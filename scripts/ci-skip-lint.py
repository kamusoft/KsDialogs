#!/usr/bin/env python3
"""CI 限定スキップの印がオーナー承認の許可リストに載っているかを検査する。

CI でだけ実行しないテストの印 (Android instrumented の `@SkipOnCi`、Swift の trait `.skipOnCi`) は、
オーナーが `kasane/config.yaml` の `lint.ci-skip.allow` に列挙することで承認される。本検査は
コード側の印と許可リストを突き合わせ、承認の無い印を落とす (cross/ADR-0021)。規約は handbook cross の
「状態遷移の観測と CI 限定スキップ」。

使い方:
  python3 scripts/ci-skip-lint.py             # 検査 (承認の無い印・理由の不足があれば exit 1)
  python3 scripts/ci-skip-lint.py --list      # 検出した印と解決したテスト識別子を並べる
  python3 scripts/ci-skip-lint.py --selftest  # 一時ディレクトリに違反例と正例を作って検出を確認

判定は 6 つ:

- 許可リストに無い印                          → 違反 (exit 1)
- 理由の記述が切り分けの 3 要素を欠く         → 違反 (exit 1)
- 正規の印以外の手段で実行を止めている        → 違反 (exit 1)
- 許可リストの項目が承認の記録を欠く          → 違反 (exit 1)
- 許可リストにあるがコードに印が無い          → 警告 (棚卸し対象。exit 0)
- `review-by` を過ぎた許可                    → 警告 (再評価の期限切れ。exit 0)

理由の 3 要素は「手元の反復で通ること (回数の数字)」「CI での落ち方 (assertion・時間切れ等)」
「重要な機能のテストでないと判断した理由」で、単語の存在で機械判定する。判定は厳密ではなく、
空の理由と一言だけの理由 (「flaky」「たまに落ちる」) を弾くのが目的で、記述の質はレビューが見る。

**正規の印以外の無効化手段**とは、承認手順を通らずにテストを実行から外せる書き方を指す
(Kotlin の `@Ignore`、Swift の素の `.disabled(...)` / `.enabled(if:)` / `XCTSkip` 系、
NUnit の `[Ignore(...)]` / `Assert.Ignore(...)` / `Assert.Inconclusive(...)`)。
承認された印より安く痕跡も残らないため、許可リストとの突き合わせを持たず一律で違反にする。
検査は CI が回すテストルートすべて (SCAN_ROOTS) に掛ける — 正規の印を置ける 3 つと、
印の仕組みを持たない 4 つ (Android のローカル単体テスト・kmp・MAUI の NUnit・MAUI Android 橋渡しの
Kotlin JVM テスト) の両方。
前提条件の表現 (JUnit の `Assume` / `assumeTrue`、Swift の `#require` による早期打ち切り) は
「その環境では検証が成立しない」ことの宣言であり、赤を消す出口ではないので対象にしない。

照合はコードの本文だけを見る。行コメント・ブロックコメント・文字列リテラルの中身は落としてから
当てるので、規約の引用や説明文に書かれた語は拾わない。Swift の `.disabled(...)` / `.enabled(if:)`
は `@Test(...)` / `@Suite(...)` の引数並び (trait を書く場所) にある分だけを見る — 同じ綴りが
SwiftUI の修飾子として正当に現れるため。

テスト識別子は印の直後にある宣言から解決する (`<型名>.<関数名>`、型に付いた印は `<型名>`)。
`allow` の `test` は完全一致か、型名を省いた関数名の一致で対応づける。書く値は `--list` の
出力に並ぶ識別子をそのまま使う (入れ子の型では見た目の suite 名と食い違うため)。
"""

from __future__ import annotations

import argparse
import datetime
import importlib.util
import os
import re
import subprocess
import sys
import tempfile


def _load_sibling():
    path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "local-path-lint.py")
    spec = importlib.util.spec_from_file_location("local_path_lint", path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


LP = _load_sibling()

# 印と無効化手段を探す範囲。CI が回すテストルートすべてに対応する。
#
# `marker` は正規の印 (置ける仕組みが無いルートは None)、`path_marker` はそのルートで
# テストと見なすパスの目印 (ライブラリ本体や生成物を巻き込まないための絞り) で、
# リポジトリ相対パスの先頭に `/` を足した文字列に対する正規表現。
#
# 正規の印を置けるのは実行機の揺らぎを持つ 3 ルート (instrumented / Simulator) だけだが、
# **無効化手段の検査はどのルートにも要る** — 印を置く場所が無いことは、そこで `@Ignore` を
# 使ってよい理由にならない。印の仕組みを持たないルートは marker を None にして、
# 無効化手段の検査だけを掛ける。
SCAN_ROOTS = [
    # 正規の印を置けるルート (印と無効化手段の両方を見る)
    ("android-instrumented", os.path.join("android"), ".kt", "@SkipOnCi(", r"/src/androidTest/"),
    ("ios", os.path.join("ios", "Tests"), ".swift", ".skipOnCi(", ""),
    ("maui-bridge", os.path.join("maui", "macios", "native"), ".swift", ".skipOnCi(",
     r"/KsDialogsMauiBridgeTests/"),
    # 印の仕組みを持たないルート (無効化手段だけを見る)
    ("android-unit", os.path.join("android"), ".kt", None, r"/src/test/"),
    ("kmp", os.path.join("kmp"), ".kt", None, r"/src/[A-Za-z]*[Tt]est/"),
    ("maui-nunit", os.path.join("maui"), ".cs", None, r"/[^/]*Tests[^/]*/"),
    # MAUI Android 橋渡しの Kotlin JVM テスト (verify-maui.yml が gradle test で回す)
    ("maui-android-bridge", os.path.join("maui", "android", "native"), ".kt", None, r"/src/test/"),
]

# 正規の印を組み立てているヘルパ自身。定義行は無効化手段の検出から外す
SKIP_HELPER_FILES = {
    "android/ksdialogs-core/src/androidTest/kotlin/jp/kamusoft/ksdialogs/support/SkipOnCi.kt",
    "ios/Tests/KsDialogsTests/Support/DialogTestCiSkip.swift",
    "maui/macios/native/KsDialogsMauiBridgeTests/Support/BridgeTestCiSkip.swift",
}

# 承認手順を通らずに実行を止められる書き方。見つけたら一律で違反にする。
#
# 3 つ目の要素は照合する場所の限定で、`SWIFT_TRAIT_SCOPE` は「`@Test` / `@Suite` の
# 引数並びにある行だけ」を指す (None はその拡張子のどこでも照合する)。
SWIFT_TRAIT_SCOPE = "swift-trait"

BANNED_SKIPS = {
    ".kt": [
        (re.compile(r"(?<![\w.])@(?:org\.junit\.|kotlin\.test\.)?Ignore\b"), "@Ignore", None),
    ],
    ".swift": [
        (re.compile(r"\.disabled\s*\("), ".disabled(...)", SWIFT_TRAIT_SCOPE),
        (re.compile(r"\.enabled\s*\(\s*if\s*:"), ".enabled(if:)", SWIFT_TRAIT_SCOPE),
        (re.compile(r"\bXCTSkip(?:If|Unless)?\s*\("), "XCTSkip 系", None),
        (re.compile(r"\bthrow\s+XCTSkip\b"), "XCTSkip 系", None),
    ],
    ".cs": [
        (re.compile(r"\[\s*Ignore\s*\("), "[Ignore(...)]", None),
        # `[Explicit]` は明示指定時だけ走る印で、CI の既定実行では skip と同じに働く。
        (re.compile(r"\[\s*Explicit\b"), "[Explicit]", None),
        (re.compile(r"\bAssert\.Ignore\s*\("), "Assert.Ignore(...)", None),
        (re.compile(r"\bAssert\.Inconclusive\s*\("), "Assert.Inconclusive(...)", None),
    ],
}

# 宣言の検出。関数に付いた印はその関数、型に付いた印はその型ごとの skip になる
KT_FUNC = re.compile(r"\bfun\s+([^\s(]+)\s*\(")
KT_TYPE = re.compile(r"\b(?:class|object)\s+([A-Za-z_]\w*)")
SWIFT_FUNC = re.compile(r"\bfunc\s+([^\s(<]+)")
SWIFT_TYPE = re.compile(r"\b(?:struct|enum|actor|final\s+class|class)\s+([A-Za-z_]\w*)")
STRING_LITERAL = re.compile(r'"((?:[^"\\]|\\.)*)"')

# 印の直後を宣言を探して見る行数の上限 (注釈・trait が複数行に折れる場合を吸収する)
LOOKAHEAD_LINES = 12

# 理由に求める 3 要素。値は「その要素が書かれている」と読む手掛かりの語
REASON_REPEAT = re.compile(r"\d+\s*(?:回|周|度)")
REASON_FAILURE_WORDS = [
    "assertion", "アサーション", "時間切れ", "タイムアウト", "timeout",
    "失敗", "落ちる", "落ち方", "時間超過",
]
REASON_IMPORTANCE_WORDS = ["重要", "契約", "保証", "前提", "利用者が依存"]
REASON_MIN_CHARS = 40


class Mark:
    """コード上で見つかった 1 件の印。"""

    def __init__(self, rel: str, line_no: int, test: str, reason: str):
        self.rel = rel
        self.line_no = line_no
        self.test = test
        self.reason = reason

    @property
    def member(self) -> str:
        """型名を省いたテスト名 (許可リストとの緩い対応づけに使う)。"""
        return self.test.rsplit(".", 1)[-1]

    @property
    def where(self) -> str:
        return f"{self.rel}:{self.line_no}"


# ---------- 許可リストの読み込み ----------

def load_allow(root: str) -> list[dict]:
    """`kasane/config.yaml` の `lint.ci-skip.allow` を読む。

    項目は `- test: ...` に続けて `mechanism` / `approved` / `review-by` を並べたマップの列で、
    兄弟 lint が共有する最小 YAML 読み (LP.load_config) はマップの列に対応しないため、この節だけ
    自前で読む。インデントの深さで節の終わりを判定する。
    """
    cfg = os.path.join(root, "kasane", "config.yaml")
    if not os.path.isfile(cfg):
        return []
    with open(cfg, encoding="utf-8") as f:
        lines = f.read().splitlines()

    entries: list[dict] = []
    stack: list[str] = []          # 現在位置の親キー (インデントの浅い順)
    indents: list[int] = []
    in_allow = False
    allow_indent = -1
    current: dict | None = None

    for raw in lines:
        line = raw.split("#", 1)[0].rstrip() if not _in_quotes_hash(raw) else raw.rstrip()
        if not line.strip():
            continue
        indent = len(line) - len(line.lstrip(" "))
        body = line.strip()

        if in_allow:
            if body.startswith("- ") and indent > allow_indent:
                current = {}
                entries.append(current)
                body = body[2:].strip()
            elif indent <= allow_indent:
                in_allow = False
                current = None
            if in_allow and current is not None and ":" in body:
                key, _, val = body.partition(":")
                current[key.strip()] = _unquote(val.strip())
            if in_allow:
                continue

        if body.startswith("- "):
            continue
        if ":" not in body:
            continue
        key = body.partition(":")[0].strip()
        while indents and indents[-1] >= indent:
            indents.pop()
            stack.pop()
        stack.append(key)
        indents.append(indent)
        if stack[-3:] == ["lint", "ci-skip", "allow"]:
            in_allow = True
            allow_indent = indent
            current = None

    return entries


def _in_quotes_hash(raw: str) -> bool:
    """行内の `#` が引用符の中にあるか (雑な判定。allow の値に `#` は使わない)。"""
    before = raw.split("#", 1)[0]
    return before.count('"') % 2 == 1


def _unquote(value: str) -> str:
    if len(value) >= 2 and value[0] == value[-1] and value[0] in "'\"":
        return value[1:-1]
    return value


# ---------- 印の収集 ----------

def _repository_files(root: str) -> list[str] | None:
    """git が把握しているファイル (追跡分と、無視されていない未追跡分) のリポジトリ相対パス。

    追跡外のビルド生成物 (`DerivedData/` 等) にテストソースの写しが入っても歩かないため、
    走査の起点を git に合わせる。git の管理下でなければ None を返し、呼び出し側が実体の走査へ落ちる。
    """
    try:
        proc = subprocess.run(
            ["git", "ls-files", "-z", "--cached", "--others", "--exclude-standard"],
            cwd=root, capture_output=True, text=True, check=True,
        )
    except Exception:
        return None
    return [p for p in proc.stdout.split("\0") if p]


def source_files(root: str) -> list[tuple[str, str, str | None]]:
    """走査対象のファイルを (リポジトリ相対パス, 拡張子, 探す印) で返す。

    印を置ける仕組みの無いルートでは 3 つ目が None になり、無効化手段の検査だけが掛かる。
    """
    tracked = _repository_files(root)
    found: list[tuple[str, str, str | None]] = []
    for _name, rel_root, ext, marker, path_marker in SCAN_ROOTS:
        rel_root = rel_root.replace(os.sep, "/")
        matches_path = re.compile(path_marker).search if path_marker else None
        if tracked is None:
            candidates = _walk_files(root, rel_root, ext)
        else:
            candidates = [
                p for p in tracked
                if p.endswith(ext) and (p == rel_root or p.startswith(rel_root + "/"))
            ]
        for rel in sorted(candidates):
            if matches_path and not matches_path("/" + rel):
                continue
            found.append((rel, ext, marker))
    return list(dict.fromkeys(found))


def _walk_files(root: str, rel_root: str, ext: str) -> list[str]:
    """git の管理下でないとき (自己テスト等) の実体走査。"""
    base = os.path.join(root, rel_root)
    if not os.path.isdir(base):
        return []
    found: list[str] = []
    for dirpath, dirnames, filenames in os.walk(base):
        dirnames[:] = sorted(
            d for d in dirnames if d not in (".git", "build", "obj", "bin", "DerivedData")
        )
        for fn in sorted(filenames):
            if fn.endswith(ext):
                found.append(LP.normalize_rel(os.path.join(dirpath, fn), root))
    return found


def collect_marks(root: str) -> list[Mark]:
    marks: list[Mark] = []
    for rel, ext, marker in source_files(root):
        if marker is None:
            continue
        with open(os.path.join(root, rel), encoding="utf-8") as f:
            lines = f.read().splitlines()
        if not any(marker in line for line in lines):
            continue
        for i, line in enumerate(lines):
            if marker not in line:
                continue
            region = "\n".join(lines[i:i + LOOKAHEAD_LINES])
            reason = _reason_from(region, marker)
            test = _resolve_test(lines, i, ext)
            marks.append(Mark(rel, i + 1, test, reason))
    return marks


def collect_bans(root: str) -> list[str]:
    """正規の印以外の手段で実行を止めている箇所 (説明付き) を集める。"""
    hits: list[str] = []
    for rel, ext, _marker in source_files(root):
        if rel in SKIP_HELPER_FILES:
            continue
        patterns = BANNED_SKIPS.get(ext, [])
        if not patterns:
            continue
        with open(os.path.join(root, rel), encoding="utf-8") as f:
            body_lines = _strip_comments(f.read().splitlines())
        trait_lines = _swift_trait_lines(body_lines) if ext == ".swift" else set()
        for i, body in enumerate(body_lines):
            for pattern, label, scope in patterns:
                if scope == SWIFT_TRAIT_SCOPE and i not in trait_lines:
                    continue
                if pattern.search(body):
                    hits.append(f"{rel}:{i + 1}: {label} — 承認手順を通らない skip")
                    break
    return hits


def _strip_comments(lines: list[str]) -> list[str]:
    """コメントと文字列リテラルの中身を落とした本文を、行数を保ったまま返す。

    説明文の中の書き方 (規約の引用・KDoc での言及) を違反として拾わないための前処理。
    行コメントとブロックコメント (Kotlin / Swift の入れ子を含む) を落とし、文字列は
    引用符だけ残して中身を空にする — 検出語は文字列の外に現れるので、これで綴りは残る。

    複数行にまたがる文字列 (raw string) までは追わない。中に検出語が現れれば誤検出になるが、
    その形は行単位の抑止を用意するより、書き方を直すほうが読み手に分かりやすい。
    """
    stripped: list[str] = []
    depth = 0
    for line in lines:
        out: list[str] = []
        quote: str | None = None
        i = 0
        while i < len(line):
            if depth > 0:
                if line.startswith("/*", i):
                    depth += 1
                    i += 2
                elif line.startswith("*/", i):
                    depth -= 1
                    i += 2
                else:
                    i += 1
                continue
            ch = line[i]
            if quote is not None:
                if ch == "\\":
                    i += 2
                    continue
                if ch == quote:
                    quote = None
                    out.append(ch)
                i += 1
                continue
            if ch == "'" or ch == '"':
                quote = ch
                out.append(ch)
                i += 1
                continue
            if line.startswith("//", i):
                break
            if line.startswith("/*", i):
                depth += 1
                i += 2
                continue
            out.append(ch)
            i += 1
        stripped.append("".join(out))
    return stripped


def _swift_trait_lines(lines: list[str]) -> set[int]:
    """`@Test(...)` / `@Suite(...)` の引数並びに当たる行の番号 (0 起点)。

    Swift Testing の trait はここにしか書けない。同じ綴り (`.disabled(...)`) は SwiftUI の
    修飾子としても正当に現れるため、無効化手段の照合はこの範囲に限る。
    """
    marked: set[int] = set()
    index = 0
    while index < len(lines):
        head = lines[index].lstrip()
        if head.startswith("@Test") or head.startswith("@Suite"):
            marked.add(index)
            depth = lines[index].count("(") - lines[index].count(")")
            last = index
            while depth > 0 and last + 1 < len(lines):
                last += 1
                marked.add(last)
                depth += lines[last].count("(") - lines[last].count(")")
            index = last + 1
            continue
        index += 1
    return marked


def _reason_from(region: str, marker: str) -> str:
    """印に渡された理由の文字列。印の位置から先の最初の文字列リテラルを取る。"""
    at = region.find(marker)
    m = STRING_LITERAL.search(region, at + len(marker) - 1)
    return m.group(1) if m else ""


def _resolve_test(lines: list[str], index: int, ext: str) -> str:
    """印の直後の宣言からテスト識別子を組み立てる。"""
    func_re, type_re = (KT_FUNC, KT_TYPE) if ext == ".kt" else (SWIFT_FUNC, SWIFT_TYPE)
    for line in lines[index:index + LOOKAHEAD_LINES]:
        type_hit = type_re.search(line)
        func_hit = func_re.search(line)
        if type_hit and not func_hit:
            return type_hit.group(1)
        if func_hit:
            owner = _enclosing_type(lines, index, type_re)
            name = func_hit.group(1).strip("`")
            return f"{owner}.{name}" if owner else name
    return "(宣言を解決できない印)"


def _enclosing_type(lines: list[str], index: int, type_re: re.Pattern) -> str:
    """印より前にある直近の型宣言 (入れ子の内側が勝つ)。"""
    for line in reversed(lines[:index]):
        hit = type_re.search(line)
        if hit:
            return hit.group(1)
    return ""


# ---------- 判定 ----------

def reason_gaps(reason: str) -> list[str]:
    """理由に欠けている要素の一覧 (空なら足りている)。"""
    gaps: list[str] = []
    text = reason.strip()
    if not text:
        return ["理由が空"]
    if len(text) < REASON_MIN_CHARS:
        gaps.append(f"理由が短すぎる ({len(text)} 字。切り分けの 3 要素が書けない長さ)")
    if not REASON_REPEAT.search(text):
        gaps.append("手元の反復で通ることの回数 (「20 回」等の数字) が無い")
    if not any(w in text for w in REASON_FAILURE_WORDS):
        gaps.append("CI での落ち方 (assertion・時間切れ等) が無い")
    if not any(w in text for w in REASON_IMPORTANCE_WORDS):
        gaps.append("重要な機能のテストでないと判断した理由が無い")
    return gaps


def match_allow(mark: Mark, allow: list[dict]) -> dict | None:
    for entry in allow:
        test = str(entry.get("test", "")).strip()
        if not test:
            continue
        if test == mark.test or test == mark.member:
            return entry
    return None


def allow_gaps(entry: dict, today: datetime.date) -> list[str]:
    """許可リストの 1 項目に欠けている承認の記録 (空なら足りている)。

    誰が書いたかは機械では見分けられないので、機械で見られる範囲 — 承認日が実在して未来でないこと、
    失敗の機構が書かれていること — だけを検査する。書き手の照合はレビューが行う。
    """
    gaps: list[str] = []
    if not str(entry.get("mechanism", "")).strip():
        gaps.append("mechanism (失敗の機構) が空")
    raw = str(entry.get("approved", "")).strip()
    if not raw:
        gaps.append("approved (オーナーが承認した日) が無い")
        return gaps
    try:
        approved = datetime.date.fromisoformat(raw)
    except ValueError:
        gaps.append(f"approved が日付として読めない ({raw})")
        return gaps
    if approved > today:
        gaps.append(f"approved が未来日 ({raw})")
    return gaps


def expired(entry: dict, today: datetime.date) -> bool:
    raw = str(entry.get("review-by", "")).strip()
    if not raw:
        return False
    try:
        return datetime.date.fromisoformat(raw) < today
    except ValueError:
        return False


def evaluate(
    marks: list[Mark],
    allow: list[dict],
    today: datetime.date,
    bans: list[str] | None = None,
):
    """(違反, 警告) を組み立てる。"""
    violations: list[str] = []
    warnings: list[str] = []
    used: set[int] = set()

    violations.extend(bans or [])

    for mark in marks:
        entry = match_allow(mark, allow)
        if entry is None:
            violations.append(
                f"{mark.where}: {mark.test} — オーナー承認 (config の lint.ci-skip.allow) が無い skip"
            )
            continue
        used.add(id(entry))
        for gap in reason_gaps(mark.reason):
            violations.append(f"{mark.where}: {mark.test} — 理由の記述が不足: {gap}")

    for entry in allow:
        test = str(entry.get("test", "")).strip() or "(test 未記入)"
        for gap in allow_gaps(entry, today):
            violations.append(f"{test} — 許可リストの項目に承認の記録が足りない: {gap}")
        if id(entry) not in used:
            warnings.append(f"{test} — 許可リストにあるがコードに印が無い (棚卸し対象)")
        elif expired(entry, today):
            warnings.append(
                f"{test} — review-by ({entry.get('review-by')}) を過ぎている (外せないか再評価する)"
            )

    return violations, warnings


# ---------- 実行 ----------

def run(root: str, show_list: bool, today: datetime.date) -> int:
    allow = load_allow(root)
    marks = collect_marks(root)
    bans = collect_bans(root)
    violations, warnings = evaluate(marks, allow, today, bans)

    print("[CI 限定スキップの検査]")
    print(f"印: {len(marks)} 件 / 許可リスト: {len(allow)} 件")

    if show_list:
        print("\n検出した印 (許可リストの test にはこの識別子をそのまま書く):")
        for mark in marks:
            print(f"  {mark.where}  {mark.test}")
            print(f"    理由: {mark.reason or '(なし)'}")
        if not marks:
            print("  (なし)")

    if warnings:
        print("\n警告:")
        for w in warnings:
            print(f"  {w}")
            print(f"::warning::CI 限定スキップ: {w}")

    if violations:
        print("\n違反:")
        for v in violations:
            print(f"  {v}")
        print(
            "\nskip はオーナーが config の lint.ci-skip.allow に列挙することで承認される。"
            "切り分けの実測を残して判断を仰ぎ、承認された後に印を付けること。"
            "\n正規の印 (@SkipOnCi / .skipOnCi) 以外の手段で CI の実行から外さないこと。"
        )
        print(f"\n結果: 違反 {len(violations)} 件")
        return 1

    print("\n結果: 違反なし")
    return 0


# ---------- 自己テスト ----------

_GOOD_REASON = (
    "手元の API 36 エミュレータで 30 回反復して全て成功する。CI では入力の宛先を待つ "
    "assertion が時間切れになる。前提の成立確認であって公開契約の保証を固定するテストではない"
)

SELFTEST_CONFIG = """version: "0.1"

lint:
  exclude: []
  ci-skip:
    allow:
      - test: SampleInstrumentedTests.approved_case
        mechanism: 入力の宛先の切り替わりが実行機の負荷で遅れる
        approved: 2026-09-09
        review-by: 2026-12-08
      - test: StaleTests.gone_case
        mechanism: すでに外した印
        approved: 2026-06-01
        review-by: 2026-08-30
      - test: BridgeSuite.expired_case
        mechanism: 提示の待ち合わせが並列スイートで飢餓になる
        approved: 2026-01-05
        review-by: 2026-04-05
      - test: UnapprovedByOwnerTests.self_written_case
        mechanism:
        approved: 2099-01-01
        review-by: 2099-04-01
  identity:
    scope: [kasane]
"""


def _write(path: str, text: str) -> None:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)


def run_selftest() -> int:
    failures = 0

    def check(ok: bool, name: str) -> None:
        nonlocal failures
        if not ok:
            failures += 1
        print(f"  {'OK  ' if ok else 'NG  '} {name}")

    with tempfile.TemporaryDirectory() as root:
        _write(os.path.join(root, "kasane", "config.yaml"), SELFTEST_CONFIG)
        _write(
            os.path.join(root, "android", "core", "src", "androidTest", "kotlin", "Sample.kt"),
            "package sample\n\n"
            "class SampleInstrumentedTests {\n"
            "    @Test\n"
            f'    @SkipOnCi(reason = "{_GOOD_REASON}")\n'
            "    fun approved_case() {\n    }\n\n"
            "    @Test\n"
            '    @SkipOnCi(reason = "たまに落ちるので")\n'
            "    fun thin_reason_case() {\n    }\n"
            "}\n",
        )
        _write(
            os.path.join(root, "android", "core", "src", "test", "kotlin", "Local.kt"),
            "class LocalTests {\n"
            '    @SkipOnCi(reason = "対象外の置き場")\n'
            "    fun local_case() {\n    }\n"
            "}\n",
        )
        _write(
            os.path.join(root, "ios", "Tests", "Suite.swift"),
            "struct BridgeSuite {\n"
            f'    @Test("expired", .skipOnCi("{_GOOD_REASON}"))\n'
            "    func expired_case() async throws {\n    }\n\n"
            f'    @Test("unapproved", .skipOnCi("{_GOOD_REASON}"))\n'
            "    func unapproved_case() async throws {\n    }\n"
            "}\n",
        )

        # 正規の印を通らない無効化手段 (違反) と、skip ではない前提条件の表現 (対象外)
        _write(
            os.path.join(root, "android", "core", "src", "androidTest", "kotlin", "Banned.kt"),
            "class BannedTests {\n"
            "    @Test\n"
            "    @Ignore(\"面倒なので止める\")\n"
            "    fun ignored_case() {\n    }\n\n"
            "    @Test\n"
            "    fun assumed_case() {\n"
            "        assumeTrue(Build.VERSION.SDK_INT >= 30)\n"
            "    }\n"
            "}\n",
        )
        _write(
            os.path.join(root, "ios", "Tests", "Banned.swift"),
            "struct BannedSuite {\n"
            '    @Test("素の disable", .disabled("面倒なので CI で止める"))\n'
            "    func disabled_case() async throws {\n    }\n\n"
            "    func skipped_case() throws {\n"
            "        try XCTSkipIf(true, \"CI では見ない\")\n"
            "    }\n\n"
            '    @Test("条件付きの有効", .enabled(if: ProcessInfo.processInfo.arguments.isEmpty))\n'
            "    func conditional_case() async throws {\n    }\n"
            "}\n",
        )
        # 印の仕組みを持たないルート。無効化手段だけが検査される
        _write(
            os.path.join(root, "android", "core", "src", "test", "kotlin", "LocalBanned.kt"),
            "class LocalUnitTests {\n"
            "    @Ignore(\"ローカル単体テストでも承認手順は要る\")\n"
            "    fun ignored_case() {\n    }\n"
            "}\n",
        )
        _write(
            os.path.join(root, "kmp", "mod", "src", "commonTest", "kotlin", "KmpSample.kt"),
            "import kotlin.test.Ignore\n\n"
            "class KmpTests {\n"
            "    @Ignore\n"
            "    fun ignored_case() {\n    }\n"
            "}\n",
        )
        _write(
            os.path.join(root, "maui", "KsDialogs.Maui.Tests", "NUnitSample.cs"),
            "public class NUnitTests\n{\n"
            "    [Test]\n"
            "    [Ignore(\"面倒なので止める\")]\n"
            "    public void IgnoredCase()\n    {\n    }\n\n"
            "    [Test]\n"
            "    public void InconclusiveCase()\n    {\n"
            "        Assert.Inconclusive(\"CI では見ない\");\n"
            "    }\n}\n",
        )

        # 説明文の中の言及と、trait ではない同名の修飾子 (どちらも対象外)
        _write(
            os.path.join(root, "android", "core", "src", "androidTest", "kotlin", "Commented.kt"),
            "/**\n"
            " * 承認手順を通らない書き方 (@Ignore) を使わない理由の説明。\n"
            " */\n"
            "class CommentedTests {\n"
            "    // @Ignore は使わずに @SkipOnCi を通す\n"
            "    fun documented_case() {\n"
            '        val note = "@Ignore と書かれた文字列"\n'
            "    }\n"
            "}\n",
        )
        _write(
            os.path.join(root, "ios", "Tests", "SwiftUIModifier.swift"),
            "import SwiftUI\n"
            "import Testing\n\n"
            "struct ModifierSuite {\n"
            '    @Test("無効状態のコントロール")\n'
            "    func disabledControl_case() async throws {\n"
            "        let view = Button(\"OK\") {}\n"
            "            .disabled(true)\n"
            "        /* trait の .disabled(...) とは別物 */\n"
            "        _ = view\n"
            "    }\n"
            "}\n",
        )

        # 正規の印を組み立てるヘルパ自身は、定義に disable を含んでいても対象外
        _write(
            os.path.join(
                root, "maui", "macios", "native", "KsDialogsMauiBridgeTests",
                "Support", "BridgeTestCiSkip.swift",
            ),
            "extension Trait where Self == ConditionTrait {\n"
            "    static func skipOnCi(_ reason: String) -> Self {\n"
            "        .disabled(if: true, Comment(rawValue: reason))\n"
            "    }\n"
            "}\n",
        )

        allow = load_allow(root)
        check(len(allow) == 4, f"許可リストを 4 件読む (実際 {len(allow)} 件)")
        check(
            allow and allow[0].get("review-by") == "2026-12-08",
            "許可リストの review-by を読む",
        )

        marks = collect_marks(root)
        found = {m.test for m in marks}
        check(
            found == {
                "SampleInstrumentedTests.approved_case",
                "SampleInstrumentedTests.thin_reason_case",
                "BridgeSuite.expired_case",
                "BridgeSuite.unapproved_case",
            },
            f"instrumented と Swift の印だけを解決する (実際 {sorted(found)})",
        )
        check(
            all("src/test/" not in m.rel for m in marks),
            "instrumented 以外の Android テストは対象外",
        )

        bans = collect_bans(root)
        check(
            any("Banned.kt:3" in b and "@Ignore" in b for b in bans),
            f"@Ignore を承認手順を通らない skip として検出する (実際 {bans})",
        )
        check(
            any("Banned.swift:2" in b and ".disabled" in b for b in bans),
            "素の .disabled(...) を検出する",
        )
        check(
            any("Banned.swift:7" in b and "XCTSkip" in b for b in bans),
            "XCTSkip 系を検出する",
        )
        check(
            any("Banned.swift:10" in b and ".enabled(if:)" in b for b in bans),
            f"trait の .enabled(if:) を検出する (実際 {[b for b in bans if 'Banned.swift' in b]})",
        )
        check(
            not any("assumed_case" in b or ":8" in b.split(": ", 1)[0] for b in bans),
            "前提条件の表現 (assumeTrue) は検出しない",
        )
        check(
            not any("BridgeTestCiSkip" in b for b in bans),
            "正規の印を組み立てるヘルパ自身は検出しない",
        )
        check(
            any("LocalBanned.kt" in b and "@Ignore" in b for b in bans),
            "印の置けない Android ローカル単体テストでも @Ignore は検出する",
        )
        check(
            any("KmpSample.kt" in b and "@Ignore" in b for b in bans),
            "kmp のテストソースセットでも @Ignore は検出する",
        )
        check(
            any("NUnitSample.cs:4" in b and "Ignore" in b for b in bans),
            f"NUnit の [Ignore(...)] を検出する (実際 {[b for b in bans if '.cs' in b]})",
        )
        check(
            any("NUnitSample.cs:12" in b and "Inconclusive" in b for b in bans),
            "NUnit の Assert.Inconclusive(...) を検出する",
        )
        check(
            not any("Commented.kt" in b for b in bans),
            "コメントと文字列の中の @Ignore は検出しない",
        )
        check(
            not any("SwiftUIModifier.swift" in b for b in bans),
            "trait ではない SwiftUI の .disabled(...) は検出しない",
        )

        violations, warnings = evaluate(marks, allow, datetime.date(2026, 9, 9), bans)
        check(
            all(b in violations for b in bans),
            "承認手順を通らない skip を違反にする",
        )
        check(
            any("self_written_case" in v and "mechanism" in v for v in violations),
            "mechanism が空の許可を違反にする",
        )
        check(
            any("self_written_case" in v and "未来日" in v for v in violations),
            "approved が未来日の許可を違反にする",
        )
        check(
            any("thin_reason_case" in v for v in violations),
            "一言だけの理由を違反にする",
        )
        check(
            any("unapproved_case" in v and "承認" in v for v in violations),
            "許可リストに無い印を違反にする",
        )
        check(
            not any("SampleInstrumentedTests.approved_case" in v for v in violations),
            "承認済みで理由の揃った印は違反にしない",
        )
        check(
            any("StaleTests.gone_case" in w for w in warnings),
            "コードに印の無い許可を警告にする",
        )
        check(
            any("BridgeSuite.expired_case" in w and "review-by" in w for w in warnings),
            "review-by を過ぎた許可を警告にする",
        )
        check(
            not reason_gaps(_GOOD_REASON),
            "3 要素の揃った理由は不足なしと判定する",
        )
        check(
            len(reason_gaps("")) == 1,
            "空の理由を違反にする",
        )

    print(f"\n自己テスト: {'全件 OK' if not failures else f'{failures} 件 NG'}")
    return failures


def main() -> int:
    parser = argparse.ArgumentParser(
        description="CI 限定スキップの印とオーナー承認の許可リストを突き合わせる"
    )
    parser.add_argument("--list", action="store_true", help="検出した印と理由を並べる")
    parser.add_argument("--selftest", action="store_true", help="検出ロジックの自己テストを行う")
    args = parser.parse_args()

    if args.selftest:
        return 1 if run_selftest() else 0
    return run(LP.repo_root(), args.list, datetime.date.today())


if __name__ == "__main__":
    sys.exit(main())
