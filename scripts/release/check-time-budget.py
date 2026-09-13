#!/usr/bin/env python3
"""リリースの待ちを抱える job が、自分の実行時間上限に収まるかの検査。

publish と wait-for-registries の 2 系統を別々に検査する。どちらも「待ちの上限はスクリプト
側の定数、job の打ち切りは workflow 側の値」という二重管理になっており、片方を延ばしたときに
もう片方が置いていかれても平時の実行は緑のまま進む。この検査が両者を読んで突き合わせる。

publish の内訳は次のとおり (分。各項は切り上げる)。

  公開待ちの上限         central-portal.sh の公開待ちの上限 (2 枠まとめて 1 本)
  最後の照会             照会 1 回あたりの応答上限 x 1 待ち
  巡回間隔の端数         巡回間隔 x 1 待ち
  本体処理               成果物の取得・署名つき再ビルドと比較・upload・NuGet への push

検証の決着待ち (枠ごとの上限) と待機外の照会は予算に含めない。Maven Central の枠が 2 つ
あるため最悪ケースを全部収容すると実行環境の job 実行時間の上限に対して余裕がなくなること、
検証は枠ごとに短時間で決着するのが常態であること、deployment ID が upload の直後に保存されて
次の試行が引き継ぐため打ち切りが不可逆な損失を生まないことによる。上限を超えた場合は job の
打ち切りで再実行に回す。

反映待ち (wait-for-registries) 側の内訳は次のとおり。

  待機の上限         wait-for-registries.sh の上限の既定値
  期限を跨げる照会   応答上限 x 1 件 (期限直前に始まった 1 件だけが期限を越えられる)
  job の前後         checkout と runner の起動

どちらの系統も、照会の接続上限を応答上限とは別のリテラルで持つ。接続上限は応答上限の内側に
収まるため合計には算入しないが、接続上限だけを応答上限より大きくした退化は応答上限を読むだけの
予算の検査を素通りするので、大小関係を独立の検査として見る。

こちらは、対象ごとに保持している分類 (反映済み / 未反映 / 判定不能 / 未照会) を出力するのが
待機スクリプトの失敗する瞬間であるという事情が効く。job の打ち切りが先に来ると、待ちの結果を
読み分ける材料がその出力ごと消え、レジストリが遅いのか壊れているのかが分からないまま終わる。

使い方:
  python3 scripts/release/check-time-budget.py
  python3 scripts/release/check-time-budget.py --release-yml PATH --central-portal PATH \
      --wait-for-registries PATH
  python3 scripts/release/check-time-budget.py --selftest
"""

from __future__ import annotations

import argparse
import math
import os
import re
import subprocess
import sys

# publish が抱える待ちの本数。公開待ち 1 本だけを予算に数える (検証の決着待ちは予算外)。
PUBLISH_WAITS = 1

# 待ち以外に publish が使う時間 (分)。成果物の取得・署名つき再ビルドと比較・upload・
# nuget.org への push の実測に対する見積もり。
BODY_WORK_MINUTES = 16

# 反映待ちの job が待ち以外に使う時間 (分)。checkout と runner の起動、待ちに入る前後の
# 出力ぶん。この job は checkout 1 つと待機スクリプトの実行しか持たないため小さく取る。
REGISTRY_JOB_OVERHEAD_MINUTES = 5

# 反映待ちで期限を跨げる照会の件数。待機は残り時間が正のときだけ次の照会を始め、対象 1 件
# ごとに判定し直す。したがって期限を跨いで走り続けられるのは、期限直前に始まった 1 件だけに
# なる。待機側がこの構造 (照会ごとの期限判定) をやめたら、この式も変わる。
REGISTRY_OVERRUNNING_REQUESTS = 1


def repo_root() -> str:
    try:
        out = subprocess.run(
            ["git", "rev-parse", "--show-toplevel"],
            capture_output=True, text=True, check=True,
        ).stdout.strip()
        return out or os.getcwd()
    except Exception:
        return os.getcwd()


def read(path: str) -> str:
    with open(path, encoding="utf-8") as handle:
        return handle.read()


def single_value(text: str, pattern: str, what: str, errors: list[str]) -> int | None:
    """pattern に一致する値がすべて同じなら、その値を返す。

    1 件も無ければ「読み取れない」、複数に割れていれば「値が割れている」で検査を失敗させる。
    読めないまま通過すると、定数を読み取れない形へ退化しても平時は緑のままになる。
    """
    found = {int(match) for match in re.findall(pattern, text)}
    if not found:
        errors.append(f"{what} を読み取れない (探した形: {pattern})")
        return None
    if len(found) > 1:
        errors.append(f"{what} の値が割れている: {sorted(found)}")
        return None
    return found.pop()


def job_timeout_minutes(text: str, job: str, errors: list[str]) -> int | None:
    """指定した job の timeout-minutes を読む。

    job は 2 桁字下げのキーで始まる。別の job の同名の値を拾わないよう、目的の job の範囲に
    入っている間だけ見る。
    """
    inside = False
    for line in text.splitlines():
        if re.match(r"^  [A-Za-z][\w-]*:\s*$", line):
            inside = line.strip() == f"{job}:"
            continue
        if inside:
            match = re.match(r"^    timeout-minutes:\s*(\d+)\s*$", line)
            if match:
                return int(match.group(1))
    errors.append(f"{job} job の timeout-minutes を読み取れない")
    return None


def to_minutes(seconds: int) -> int:
    return math.ceil(seconds / 60)


def report(rows: list[tuple[str, str, int]], total: int, timeout_minutes: int,
           what: str) -> bool:
    """内訳と合計を出し、実行時間上限に収まっていれば True を返す。"""
    for name, detail, minutes in rows:
        print(f"  {name}: {minutes} 分 ({detail})")
    print(f"  合計: {total} 分")
    print(f"  {what} の上限: {timeout_minutes} 分")

    if total > timeout_minutes:
        print(
            f"::error::{what} の実行時間上限が待ちの予算を収容していない "
            f"(必要 {total} 分 > timeout-minutes {timeout_minutes} 分)",
            file=sys.stderr,
        )
        return False

    print(f"  余裕: {timeout_minutes - total} 分")
    return True


def check_publish(workflow: str, portal: str) -> bool:
    errors: list[str] = []
    timeout_minutes = job_timeout_minutes(workflow, "publish", errors)
    published_timeout = single_value(
        portal, r"KSR_PUBLISHED_TIMEOUT_SECONDS:-(\d+)", "公開待ちの上限", errors)
    interval = single_value(
        portal, r"KSR_POLL_INTERVAL_SECONDS:-(\d+)", "巡回間隔", errors)
    max_time = single_value(
        portal, r"--max-time (\d+)", "照会 1 回あたりの応答上限", errors)

    if errors:
        for message in errors:
            print(f"::error::{message}", file=sys.stderr)
        return False

    assert timeout_minutes is not None and published_timeout is not None
    assert interval is not None and max_time is not None

    rows = [
        (
            "公開待ちの上限",
            f"{published_timeout} 秒 x {PUBLISH_WAITS} 本 (2 枠まとめて)",
            to_minutes(published_timeout * PUBLISH_WAITS),
        ),
        (
            "最後の照会",
            f"応答上限 {max_time} 秒 x {PUBLISH_WAITS} 待ち",
            to_minutes(max_time * PUBLISH_WAITS),
        ),
        (
            "巡回間隔の端数",
            f"巡回間隔 {interval} 秒 x {PUBLISH_WAITS} 待ち",
            to_minutes(interval * PUBLISH_WAITS),
        ),
        (
            "本体処理",
            "成果物の取得・署名つき再ビルドと比較・upload・NuGet への push",
            BODY_WORK_MINUTES,
        ),
    ]

    total = sum(row[2] for row in rows)
    return report(rows, total, timeout_minutes, "publish")


def check_registry_wait(workflow: str, waiter: str) -> bool:
    """反映待ち job が、待機スクリプトの上界を収容しているかを検査する。

    上界は「待機の上限 + 期限を跨げる照会 1 件ぶんの応答上限」。照会は残り時間が正のときだけ
    始まり対象 1 件ごとに判定し直すので、期限を越えて走り続けられるのは期限直前に始まった
    1 件に限られる。
    """
    errors: list[str] = []
    timeout_minutes = job_timeout_minutes(workflow, "wait-for-registries", errors)
    poll_timeout = single_value(
        waiter, r"KSR_POLL_TIMEOUT_SECONDS:-(\d+)", "反映待ちの上限", errors)
    max_time = single_value(
        waiter, r"HTTP_MAX_TIME_SECONDS=(\d+)", "反映待ちの照会 1 回あたりの応答上限", errors)

    if errors:
        for message in errors:
            print(f"::error::{message}", file=sys.stderr)
        return False

    assert timeout_minutes is not None and poll_timeout is not None and max_time is not None

    rows = [
        ("待機の上限", f"{poll_timeout} 秒", to_minutes(poll_timeout)),
        (
            "期限を跨げる照会",
            f"応答上限 {max_time} 秒 x {REGISTRY_OVERRUNNING_REQUESTS} 件",
            to_minutes(max_time * REGISTRY_OVERRUNNING_REQUESTS),
        ),
        ("job の前後", "checkout と runner の起動", REGISTRY_JOB_OVERHEAD_MINUTES),
    ]

    total = sum(row[2] for row in rows)
    return report(rows, total, timeout_minutes, "wait-for-registries")


def check_connect_timeout(text: str, what: str, max_time_pattern: str) -> bool:
    """接続上限が応答上限の内側に収まっているかを検査する。

    接続上限は応答上限とは別のリテラルで持つため、応答上限だけを読む予算の検査では、
    接続上限だけを大きくした退化が無音で通る。接続は応答の内側で起きるので合計には
    算入せず、大小関係だけを独立に見る。
    """
    errors: list[str] = []
    connect = single_value(
        text, r"--connect-timeout (\d+)", f"{what}の接続上限", errors)
    max_time = single_value(text, max_time_pattern, f"{what}の応答上限", errors)

    if errors:
        for message in errors:
            print(f"::error::{message}", file=sys.stderr)
        return False

    assert connect is not None and max_time is not None
    print(f"  {what}: 接続上限 {connect} 秒 / 応答上限 {max_time} 秒")

    if connect > max_time:
        print(
            f"::error::{what}の接続上限が応答上限を超えている "
            f"(接続 {connect} 秒 > 応答 {max_time} 秒)",
            file=sys.stderr,
        )
        return False

    return True


def check(release_yml: str, central_portal: str, wait_for_registries: str) -> int:
    workflow = read(release_yml)
    portal = read(central_portal)
    waiter = read(wait_for_registries)

    print("[publish]")
    published = check_publish(workflow, portal)
    print("[wait-for-registries]")
    waited = check_registry_wait(workflow, waiter)
    print("[接続上限と応答上限の整合]")
    consistent = check_connect_timeout(portal, "publish の照会", r"--max-time (\d+)")
    consistent &= check_connect_timeout(
        waiter, "反映待ちの照会", r"HTTP_MAX_TIME_SECONDS=(\d+)")
    return 0 if published and waited and consistent else 1


def selftest() -> int:
    """実物の定数を土台に、悪化させた入力で検査が落ちることを確かめる。

    この検査は「収まっていること」しか出力しないため、読み取りが空振りする形へ退化しても
    平時は緑のままになる。検出力そのものをここで確かめる。
    """
    import tempfile

    root = repo_root()
    release_yml = os.path.join(root, ".github", "workflows", "release.yml")
    central_portal = os.path.join(root, "scripts", "release", "central-portal.sh")
    waiter = os.path.join(root, "scripts", "release", "wait-for-registries.sh")
    sources = {
        "release": read(release_yml),
        "portal": read(central_portal),
        "waiter": read(waiter),
    }

    def mutated(key: str, old: str, new: str) -> dict[str, str]:
        """1 箇所だけ書き換えた入力一式を返す。置換できなければ検査自体を失敗にする。"""
        copy = dict(sources)
        if old not in copy[key]:
            raise AssertionError(f"自己テストの土台が変わっている: {old}")
        copy[key] = copy[key].replace(old, new)
        return copy

    cases: list[tuple[str, dict[str, str], int]] = [
        ("現在の定数では両系統とも収まる", dict(sources), 0),
        # publish 側
        (
            "公開待ちの上限を延ばすと落ちる",
            mutated("portal", "KSR_PUBLISHED_TIMEOUT_SECONDS:-5400",
                    "KSR_PUBLISHED_TIMEOUT_SECONDS:-9000"),
            1,
        ),
        (
            "publish の照会の応答上限を延ばすと落ちる",
            mutated("portal", "--max-time 300", "--max-time 3000"),
            1,
        ),
        (
            "publish の巡回間隔を延ばすと落ちる",
            mutated("portal", "KSR_POLL_INTERVAL_SECONDS:-30",
                    "KSR_POLL_INTERVAL_SECONDS:-3000"),
            1,
        ),
        (
            "publish の timeout-minutes を縮めると落ちる",
            mutated("release", "    timeout-minutes: 150", "    timeout-minutes: 100"),
            1,
        ),
        # wait-for-registries 側
        (
            "反映待ちの上限を延ばすと落ちる",
            mutated("waiter", "KSR_POLL_TIMEOUT_SECONDS:-2700",
                    "KSR_POLL_TIMEOUT_SECONDS:-3600"),
            1,
        ),
        (
            "反映待ちの応答上限を延ばすと落ちる",
            mutated("waiter", "HTTP_MAX_TIME_SECONDS=120", "HTTP_MAX_TIME_SECONDS=900"),
            1,
        ),
        (
            "反映待ちの timeout-minutes を縮めると落ちる",
            mutated("release", "    timeout-minutes: 60", "    timeout-minutes: 45"),
            1,
        ),
        # 接続上限と応答上限の整合
        (
            "publish の接続上限が応答上限を超えると落ちる",
            mutated("portal", "--connect-timeout 30 --max-time 300",
                    "--connect-timeout 600 --max-time 300"),
            1,
        ),
        (
            "反映待ちの接続上限が応答上限を超えると落ちる",
            mutated("waiter", "--connect-timeout 30", "--connect-timeout 600"),
            1,
        ),
        (
            "反映待ちの接続上限を読み取れなければ落ちる",
            mutated("waiter", "--connect-timeout 30",
                    '--connect-timeout "${KSR_CONNECT_TIMEOUT_SECONDS:-x}"'),
            1,
        ),
        # 読み取りが空振りする形への退化
        (
            "publish の定数を読み取れなければ落ちる",
            mutated("portal", "KSR_PUBLISHED_TIMEOUT_SECONDS:-5400",
                    "KSR_PUBLISHED_TIMEOUT_SECONDS-5400"),
            1,
        ),
        (
            "反映待ちの応答上限を読み取れなければ落ちる",
            mutated("waiter", "HTTP_MAX_TIME_SECONDS=120", "HTTP_MAX_TIME_SECONDS=$(echo 120)"),
            1,
        ),
        (
            "publish の timeout-minutes を読み取れなければ落ちる",
            mutated("release", "    timeout-minutes: 150",
                    "    timeout-minutes: ${{ env.KS_PUBLISH_TIMEOUT }}"),
            1,
        ),
        (
            "同じ定数の値が割れていれば落ちる",
            mutated("waiter", 'local timeout="${KSR_POLL_TIMEOUT_SECONDS:-2700}"',
                    'local timeout="${KSR_POLL_TIMEOUT_SECONDS:-2700}"\n'
                    '    : "${KSR_POLL_TIMEOUT_SECONDS:-1234}"'),
            1,
        ),
    ]

    failures = 0
    with tempfile.TemporaryDirectory() as work:
        for index, (name, source, expected) in enumerate(cases):
            paths = {}
            for key, content in source.items():
                paths[key] = os.path.join(work, f"{key}-{index}")
                with open(paths[key], "w", encoding="utf-8") as handle:
                    handle.write(content)
            # 出力そのものは検証対象ではないので、終了コードだけを見る。
            stdout, stderr = sys.stdout, sys.stderr
            with open(os.devnull, "w", encoding="utf-8") as quiet:
                sys.stdout = sys.stderr = quiet
                try:
                    actual = check(paths["release"], paths["portal"], paths["waiter"])
                finally:
                    sys.stdout, sys.stderr = stdout, stderr
            if actual == expected:
                print(f"  OK   {name}")
            else:
                print(f"  NG   {name} (exit {actual} / 期待 {expected})")
                failures += 1

    print(f"{len(cases)} 件 / 失敗なし" if failures == 0 else f"失敗 {failures} 件")
    return 0 if failures == 0 else 1


def main(argv: list[str]) -> int:
    root = repo_root()
    parser = argparse.ArgumentParser(add_help=True)
    parser.add_argument(
        "--release-yml",
        default=os.path.join(root, ".github", "workflows", "release.yml"),
    )
    parser.add_argument(
        "--central-portal",
        default=os.path.join(root, "scripts", "release", "central-portal.sh"),
    )
    parser.add_argument(
        "--wait-for-registries",
        default=os.path.join(root, "scripts", "release", "wait-for-registries.sh"),
    )
    parser.add_argument("--selftest", action="store_true")
    args = parser.parse_args(argv)
    if args.selftest:
        return selftest()
    return check(args.release_yml, args.central_portal, args.wait_for_registries)


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
