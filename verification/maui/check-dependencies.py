#!/usr/bin/env python3
"""MAUI 消費者が解決した KsDialogs パッケージの版・取得元・アセットを検査する。

restore の結果から次の 3 点を確かめる。

1. facade (KsDialogs.Maui) と binding 2 件 (KsDialogs.Binding.iOS /
   KsDialogs.Binding.Android) の解決版が、要求した version と一致すること
2. 3 件の取得元が、指定した参照先であること
3. platform TFM (net10.0-android / net10.0-ios) の解決結果に、その platform の binding の
   アセンブリが platform 固有アセットとして入っていること

版は `project.assets.json` の targets から読む。取得元は展開先の
`<id>/<version>/.nupkg.metadata` の source から読む。`project.assets.json` の
restore.sources は「構成したソースの一覧」であって実際の取得元ではないため使わない。
packageSourceMapping は展開済みのパッケージには働かないので、取得元の確認は
パッケージ単位のこの記録でしか行えない。

3 を見るのは、消費者の TargetPlatformVersion がパッケージの API 版付き TFM を下回ると、
警告なく platform 中立アセットへフォールバックして binding が入らないためである。
版の一致だけでは binding が効いていることを示せない。

使い方:
    check-dependencies.py --assets <project.assets.json>
                          --packages <展開先ディレクトリ>
                          --expected-version <version>
                          --expected-source <フォルダフィードのパス、または URL>
    check-dependencies.py --selftest
"""

import argparse
import json
import os
import re
import sys

# facade と、platform TFM の依存として推移的に入る binding 2 件。
FACADE = "KsDialogs.Maui"
BINDINGS = ("KsDialogs.Binding.iOS", "KsDialogs.Binding.Android")
REQUIRED = (FACADE,) + BINDINGS

# platform ごとに、その platform の target へ入っているべき binding。
PLATFORM_BINDINGS = {
    "android": "KsDialogs.Binding.Android",
    "ios": "KsDialogs.Binding.iOS",
}

# target framework 名から platform を取る (`net10.0-android` -> android)。
TARGET_PLATFORM_RE = re.compile(r"^net\d+\.\d+-(?P<platform>[a-z]+)")

# アセットのパス (`lib/net10.0-android36.0/X.dll`) から TFM フォルダを取る。
ASSET_TFM_RE = re.compile(r"^(?:lib|ref|runtimes/[^/]+/lib)/(?P<tfm>[^/]+)/")

# platform 固有アセットの TFM (API 版付き)。`net10.0` や `netstandard2.0` は中立アセット。
def platform_asset_re(platform):
    return re.compile(r"^net\d+\.\d+-" + platform + r"\d+\.\d+$")


def read_resolved_versions(assets):
    """assets から KsDialogs 系パッケージの解決版を集める。

    戻り値は {パッケージ ID: {version, ...}}。同じ ID が複数の TFM に現れるため、
    版が TFM ごとに割れていないことも呼び出し側で判定できるよう集合で返す。
    """
    resolved = {}
    for libraries in assets.get("targets", {}).values():
        for key in libraries:
            package_id, _, version = key.partition("/")
            if package_id in REQUIRED:
                resolved.setdefault(package_id, set()).add(version)
    return resolved


def read_source(packages_path, package_id, version):
    """展開先の .nupkg.metadata から取得元を読む。無ければ None を返す。"""
    metadata_path = os.path.join(
        packages_path, package_id.lower(), version.lower(), ".nupkg.metadata"
    )
    if not os.path.isfile(metadata_path):
        return None
    with open(metadata_path, encoding="utf-8") as f:
        return json.load(f).get("source")


def check_platform_assets(assets):
    """platform TFM の target に binding が platform 固有アセットとして入っているか見る。

    戻り値は (行の一覧, 失敗の一覧)。
    """
    rows = []
    failures = []
    seen_platforms = set()

    for target_name, libraries in assets.get("targets", {}).items():
        framework = target_name.split("/", 1)[0]
        matched = TARGET_PLATFORM_RE.match(framework)
        if matched is None:
            continue
        platform = matched.group("platform")
        binding = PLATFORM_BINDINGS.get(platform)
        if binding is None:
            continue
        seen_platforms.add(platform)

        entries = [
            (key, value) for key, value in libraries.items() if key.partition("/")[0] == binding
        ]
        if not entries:
            failures.append(f"{target_name} の解決結果に {binding} が無い")
            rows.append((target_name, binding, "(未解決)"))
            continue

        expected = platform_asset_re(platform)
        for key, library in entries:
            paths = [
                path
                for group in ("compile", "runtime")
                for path in (library.get(group) or {})
            ]
            specific = []
            neutral = []
            for path in paths:
                if path.endswith("_._"):
                    neutral.append(path)
                    continue
                tfm = ASSET_TFM_RE.match(path)
                if tfm is not None and expected.match(tfm.group("tfm")):
                    specific.append(path)
                else:
                    neutral.append(path)
            rows.append(
                (target_name, key, specific[0] if specific else (neutral[0] if neutral else "(アセットなし)"))
            )
            if not specific:
                failures.append(
                    f"{target_name} の {key} が platform 固有アセットとして入っていない "
                    f"(見つかったアセット: {', '.join(neutral) or 'なし'})"
                )

    for platform in PLATFORM_BINDINGS:
        if platform not in seen_platforms:
            failures.append(f"{platform} の platform TFM の target が解決結果に無い")

    return rows, failures


def check(assets_path, packages_path, expected_version, expected_source, out=sys.stdout, err=sys.stderr):
    with open(assets_path, encoding="utf-8") as f:
        assets = json.load(f)

    resolved = read_resolved_versions(assets)

    failures = []
    rows = []
    for package_id in REQUIRED:
        versions = sorted(resolved.get(package_id, ()))
        if not versions:
            failures.append(f"{package_id} が解決されていない")
            rows.append((package_id, "(未解決)", "(なし)"))
            continue
        if len(versions) > 1:
            failures.append(
                f"{package_id} の解決版が TFM ごとに割れている: {', '.join(versions)}"
            )
        for version in versions:
            source = read_source(packages_path, package_id, version)
            rows.append((package_id, version, source or "(取得元の記録なし)"))
            if version != expected_version:
                failures.append(
                    f"{package_id} の解決版が要求と異なる: "
                    f"要求 {expected_version} / 解決 {version}"
                )
            if source is None:
                failures.append(f"{package_id} {version} の取得元の記録がない")
            elif source != expected_source:
                failures.append(
                    f"{package_id} {version} の取得元が参照先と異なる: "
                    f"期待 {expected_source} / 実際 {source}"
                )

    asset_rows, asset_failures = check_platform_assets(assets)
    failures.extend(asset_failures)

    width = max(len(row[0]) for row in rows)
    print(
        "\n".join(
            f"{package_id.ljust(width)}  {version}  <- {source}"
            for package_id, version, source in rows
        ),
        file=out,
    )
    if asset_rows:
        print("", file=out)
        print("platform TFM のアセット", file=out)
        for target_name, key, path in asset_rows:
            print(f"  {target_name}  {key}  {path}", file=out)

    if failures:
        print("", file=err)
        for failure in failures:
            print(f"エラー: {failure}", file=err)
        return 1

    print(
        f"facade と binding 2 件が {expected_version} で一致し、取得元も参照先と一致し、"
        "binding は platform 固有アセットとして入っている",
        file=out,
    )
    return 0


def selftest_assets(version="1.2.3", android_asset="lib/net10.0-android36.0/KsDialogs.Binding.Android.dll"):
    """検査に掛ける最小の project.assets.json を組み立てる。"""

    def library(path):
        return {"type": "package", "compile": {path: {}}, "runtime": {path: {}}}

    return {
        "targets": {
            "net10.0-android": {
                f"KsDialogs.Maui/{version}": library("lib/net10.0-android36.0/KsDialogs.Maui.dll"),
                f"KsDialogs.Binding.Android/{version}": library(android_asset),
            },
            "net10.0-ios": {
                f"KsDialogs.Maui/{version}": library("lib/net10.0-ios26.0/KsDialogs.Maui.dll"),
                f"KsDialogs.Binding.iOS/{version}": library("lib/net10.0-ios26.0/KsDialogs.Binding.iOS.dll"),
            },
        }
    }


def selftest():
    import io
    import tempfile

    failures = 0

    def report(ok, name, detail=""):
        nonlocal failures
        failures += 0 if ok else 1
        # 詳細は失敗したときだけ出す (通ったときの出力で結果が埋もれないようにする)。
        print(f"  {'OK  ' if ok else 'NG  '} {name}{f' ({detail})' if detail and not ok else ''}")

    version = "1.2.3"
    source = "/feed"

    with tempfile.TemporaryDirectory() as tmp:
        packages = os.path.join(tmp, "packages")

        def write_packages(sources):
            for package_id, package_source in sources.items():
                directory = os.path.join(packages, package_id.lower(), version)
                os.makedirs(directory, exist_ok=True)
                with open(os.path.join(directory, ".nupkg.metadata"), "w", encoding="utf-8") as f:
                    json.dump({"version": 2, "source": package_source}, f)

        def run(assets, expected_version=version, expected_source=source):
            path = os.path.join(tmp, "project.assets.json")
            with open(path, "w", encoding="utf-8") as f:
                json.dump(assets, f)
            out, err = io.StringIO(), io.StringIO()
            code = check(path, packages, expected_version, expected_source, out=out, err=err)
            return code, out.getvalue() + err.getvalue()

        write_packages({package_id: source for package_id in REQUIRED})

        print("[正の入力]")
        code, output = run(selftest_assets(version))
        report(code == 0, "版・取得元・アセットが揃っていれば exit 0", output.strip())

        print("[負の入力]")
        broken = selftest_assets(version)
        broken["targets"]["net10.0-ios"]["KsDialogs.Binding.iOS/9.9.9"] = broken["targets"][
            "net10.0-ios"
        ].pop(f"KsDialogs.Binding.iOS/{version}")
        code, output = run(broken)
        report(code == 1, "binding の版が facade と違えば exit 1")
        report("解決版が要求と異なる" in output, "版の不一致が理由として出る", output.strip())

        code, output = run(selftest_assets(version), expected_source="https://api.nuget.org/v3/index.json")
        report(code == 1, "取得元が参照先と違えば exit 1")
        report("取得元が参照先と異なる" in output, "取得元の不一致が理由として出る")

        code, output = run(selftest_assets(version, android_asset="lib/net10.0/KsDialogs.Binding.Android.dll"))
        report(code == 1, "platform 中立アセットへのフォールバックで exit 1")
        report(
            "platform 固有アセットとして入っていない" in output,
            "フォールバックが理由として出る",
            output.strip(),
        )

        missing = selftest_assets(version)
        del missing["targets"]["net10.0-android"][f"KsDialogs.Binding.Android/{version}"]
        code, output = run(missing)
        report(code == 1, "binding が target に無ければ exit 1")
        report("KsDialogs.Binding.Android が無い" in output, "不在が理由として出る")

        without_ios = selftest_assets(version)
        del without_ios["targets"]["net10.0-ios"]
        code, output = run(without_ios)
        report(code == 1, "platform TFM の target が欠ければ exit 1")

    print("失敗なし" if failures == 0 else f"失敗 {failures} 件")
    return 0 if failures == 0 else 1


def main(argv):
    if "--selftest" in argv:
        return selftest()

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--assets", required=True)
    parser.add_argument("--packages", required=True)
    parser.add_argument("--expected-version", required=True)
    parser.add_argument("--expected-source", required=True)
    args = parser.parse_args(argv)

    return check(args.assets, args.packages, args.expected_version, args.expected_source)


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
