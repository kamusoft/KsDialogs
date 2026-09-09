#!/usr/bin/env python3
"""README の最小例と消費者検証プロジェクトのソースの一致検査。

ルート README (英語) の「Minimal examples」節にある 4 つのコードブロックは、
`verification/` 配下の消費者プロジェクトがそのままコンパイルする 4 ファイルと同じ内容である。
どちらか一方だけが変わると、README に載っている例はビルドされていないことになるため、
完全一致 (末尾の改行まで含めた文字列一致) を検査する。

日本語版 README (`README_ja.md`) は対象外。英日の同期は利用者向けドキュメントの
追従更新が担う。

使い方:
  python3 scripts/readme-example-lint.py            # 検査 (違反があれば exit 1)
  python3 scripts/readme-example-lint.py --selftest # 抽出ロジックと検査の疎通確認
"""

from __future__ import annotations

import os
import re
import subprocess
import sys

# 検査する README と、最小例を集めた節の見出し。
README = "README.md"
SECTION_HEADING = "## Minimal examples"

# 対応表: (節内の小見出し, fence 言語, 対応するソースファイル)。
# 同じ fence 言語のブロックが複数の形態に載るため、小見出しと合わせた組で区別する。
MAPPING = [
    ("### iOS Native", "swift", "verification/ios/Sources/VerificationApp/ContentView.swift"),
    ("### Android Native", "kotlin", "verification/android/readme-example/Notifications.kt"),
    ("### .NET MAUI", "csharp", "verification/maui/Notifications.cs"),
    ("### Kotlin Multiplatform", "kotlin", "verification/kmp/shared/src/commonMain/kotlin/Confirm.kt"),
]

FENCE_RE = re.compile(r"^```(\w*)$")


def repo_root() -> str:
    try:
        out = subprocess.run(
            ["git", "rev-parse", "--show-toplevel"],
            capture_output=True, text=True, check=True,
        ).stdout.strip()
        return out or os.getcwd()
    except Exception:
        return os.getcwd()


def count_section_headings(markdown: str) -> int:
    """文書全体にある最小例の節の見出し数を数える。

    抽出は最初の 1 件だけを見て次の `## ` で打ち切るため、同名の節が 2 つあると
    2 つ目以降のコードブロックは検査されないまま README に残る。
    fence の中に現れる見出し風の行 (Markdown の入れ子例など) は数えない。
    """
    count = 0
    in_fence = False
    for line in markdown.splitlines():
        stripped = line.rstrip("\n")
        if stripped.startswith("```"):
            in_fence = not in_fence
            continue
        if not in_fence and stripped == SECTION_HEADING:
            count += 1
    return count


def extract_blocks(markdown: str) -> list[tuple[str, str, str]]:
    """最小例の節から (小見出し, fence 言語, 本文) を出現順に取り出す。

    節の範囲は SECTION_HEADING から次の同レベル見出し (`## `) の直前まで。
    fence の中身は行単位で読み、コードブロック内に現れる `#` 始まりの行を
    見出しと取り違えないようにする。
    """
    lines = markdown.splitlines(keepends=True)

    start = None
    for i, line in enumerate(lines):
        if line.rstrip("\n") == SECTION_HEADING:
            start = i + 1
            break
    if start is None:
        raise LookupError(f"{README} に見出しがない: {SECTION_HEADING}")

    blocks: list[tuple[str, str, str]] = []
    heading = ""
    body: list[str] | None = None
    language = ""
    for line in lines[start:]:
        stripped = line.rstrip("\n")
        if body is None:
            if stripped.startswith("## "):
                break
            if stripped.startswith("### "):
                heading = stripped
                continue
            fence = FENCE_RE.match(stripped)
            if fence:
                language = fence.group(1)
                body = []
            continue
        if stripped == "```":
            blocks.append((heading, language, "".join(body)))
            body = None
            continue
        body.append(line)

    if body is not None:
        raise LookupError(f"{README} の {SECTION_HEADING} 節に閉じていないコードブロックがある")
    return blocks


def resolve(blocks: list[tuple[str, str, str]]) -> tuple[dict[str, str], list[str]]:
    """対応表の 4 件をコードブロックへ割り当てる。

    (小見出し, fence 言語) の組に一致するブロックはちょうど 1 件であることを要求する。
    複数あると、対応表が拾わない 2 件目以降が検査されないまま README に残り、
    ビルド対象でない例が緑のまま増えていく。
    """
    resolved: dict[str, str] = {}
    problems: list[str] = []
    for heading, language, path in MAPPING:
        matched = [
            body
            for block_heading, block_language, body in blocks
            if block_heading == heading and block_language == language
        ]
        if len(matched) == 1:
            resolved[path] = matched[0]
        elif not matched:
            problems.append(
                f"{README} の {SECTION_HEADING} 節に該当のコードブロックがない: "
                f"{heading} / ```{language} (対応先 {path})"
            )
        else:
            problems.append(
                f"{README} の {SECTION_HEADING} 節に該当のコードブロックが {len(matched)} 件ある "
                f"(1 件だけであるべき): {heading} / ```{language} (対応先 {path})"
            )
    return resolved, problems


def lint(root: str) -> int:
    readme_path = os.path.join(root, README)
    if not os.path.isfile(readme_path):
        print(f"エラー: {README} が無い: {readme_path}", file=sys.stderr)
        return 1

    with open(readme_path, encoding="utf-8") as f:
        markdown = f.read()

    headings = count_section_headings(markdown)
    if headings != 1:
        print(
            f"エラー: {README} の {SECTION_HEADING} 節が {headings} 個ある "
            f"(1 個だけであるべき)",
            file=sys.stderr,
        )
        return 1

    blocks = extract_blocks(markdown)

    resolved, problems = resolve(blocks)

    for _heading, _language, path in MAPPING:
        expected = resolved.get(path)
        if expected is None:
            continue
        full = os.path.join(root, path)
        if not os.path.isfile(full):
            problems.append(f"対応するソースが無い: {path}")
            continue
        with open(full, encoding="utf-8") as f:
            actual = f.read()
        if actual != expected:
            problems.append(
                f"{README} の最小例と一致しない: {path} "
                f"(README {len(expected)} 文字 / ソース {len(actual)} 文字)"
            )

    if problems:
        for problem in problems:
            print(f"エラー: {problem}", file=sys.stderr)
        print(
            f"README の最小例と消費者検証のソースが {len(problems)} 件一致しない",
            file=sys.stderr,
        )
        return 1

    print(f"README の最小例 {len(MAPPING)} 件が消費者検証のソースと一致する")
    return 0


SELFTEST_README = """# Title

## Installation

### iOS Native

```swift
dependencies: []
```

## Minimal examples

### iOS Native

```swift
let ios = 1
```

### Android Native

```kotlin
val android = 1
```

### .NET MAUI

```csharp
var maui = 1;
```

### Kotlin Multiplatform

```kotlin
val kmp = 1
```

## Next section

```swift
let ignored = 1
```
"""


def selftest() -> int:
    import contextlib
    import io
    import tempfile

    failures = 0

    def check(ok: bool, name: str, detail: str = "") -> None:
        nonlocal failures
        failures += 0 if ok else 1
        print(f"  {'OK  ' if ok else 'NG  '} {name}{f' ({detail})' if detail else ''}")

    print("[抽出ロジック]")
    blocks = extract_blocks(SELFTEST_README)
    check(len(blocks) == 4, "最小例の節のブロックだけを取る", f"実際 {len(blocks)} 件")
    check(
        blocks[0] == ("### iOS Native", "swift", "let ios = 1\n"),
        "Installation 節の同名見出しを拾わない",
        repr(blocks[0]),
    )
    check(
        count_section_headings(SELFTEST_README) == 1,
        "最小例の節の見出しを 1 件だけ数える",
        f"実際 {count_section_headings(SELFTEST_README)} 件",
    )
    fenced_heading = SELFTEST_README.replace(
        "```swift\nlet ignored = 1\n```",
        f"```markdown\n{SECTION_HEADING}\n```",
    )
    check(
        count_section_headings(fenced_heading) == 1,
        "fence の中の見出し風の行は数えない",
        f"実際 {count_section_headings(fenced_heading)} 件",
    )
    resolved, problems = resolve(blocks)
    check(not problems, "対応表の 4 件すべてが解決する", "; ".join(problems))
    check(
        resolved["verification/android/readme-example/Notifications.kt"] == "val android = 1\n"
        and resolved["verification/kmp/shared/src/commonMain/kotlin/Confirm.kt"] == "val kmp = 1\n",
        "同じ fence 言語の 2 件は小見出しで振り分ける",
    )

    print("[検査の疎通]")
    contents = {
        "verification/ios/Sources/VerificationApp/ContentView.swift": "let ios = 1\n",
        "verification/android/readme-example/Notifications.kt": "val android = 1\n",
        "verification/maui/Notifications.cs": "var maui = 1;\n",
        "verification/kmp/shared/src/commonMain/kotlin/Confirm.kt": "val kmp = 1\n",
    }
    with tempfile.TemporaryDirectory() as tmp:
        def write(rel: str, text: str) -> None:
            path = os.path.join(tmp, rel)
            os.makedirs(os.path.dirname(path), exist_ok=True)
            with open(path, "w", encoding="utf-8") as f:
                f.write(text)

        write(README, SELFTEST_README)
        for rel, text in contents.items():
            write(rel, text)

        buf = io.StringIO()
        with contextlib.redirect_stdout(buf), contextlib.redirect_stderr(buf):
            code = lint(tmp)
        check(code == 0, "一致していれば exit 0", buf.getvalue().strip())

        # 片側だけを変えた状態は違反として検出する。
        write("verification/maui/Notifications.cs", "var maui = 2;\n")
        buf = io.StringIO()
        with contextlib.redirect_stdout(buf), contextlib.redirect_stderr(buf):
            code = lint(tmp)
        out = buf.getvalue()
        check(code == 1, "片側だけの変更で exit 1")
        check("verification/maui/Notifications.cs" in out, "不一致のファイルが出力される")
        check("Confirm.kt" not in out, "一致しているファイルは報告しない")

        # 同じ (小見出し, fence 言語) の組が 2 件あれば、どちらを検査すべきか決まらない。
        write("verification/maui/Notifications.cs", "var maui = 1;\n")
        write(README, SELFTEST_README.replace(
            "### Android Native\n\n```kotlin\nval android = 1\n```\n",
            "### Android Native\n\n```kotlin\nval android = 1\n```\n\n```kotlin\nval extra = 1\n```\n",
        ))
        buf = io.StringIO()
        with contextlib.redirect_stdout(buf), contextlib.redirect_stderr(buf):
            code = lint(tmp)
        out = buf.getvalue()
        check(code == 1, "同じ見出し・言語の重複で exit 1")
        check("2 件ある" in out, "重複した候補数が出力される", out.strip())
        write(README, SELFTEST_README)

        # 節そのものが 2 つある状態は、1 つ目が一致していても素通りさせない。
        write(README, SELFTEST_README + f"""
{SECTION_HEADING}

### iOS Native

```swift
let ios = 2
```
""")
        buf = io.StringIO()
        with contextlib.redirect_stdout(buf), contextlib.redirect_stderr(buf):
            code = lint(tmp)
        out = buf.getvalue()
        check(code == 1, "最小例の節が 2 つあれば exit 1")
        check("節が 2 個ある" in out, "節の個数が出力される", out.strip())
        write(README, SELFTEST_README)

        # 対応先のファイルが無い場合も違反にする。
        os.remove(os.path.join(tmp, "verification/maui/Notifications.cs"))
        buf = io.StringIO()
        with contextlib.redirect_stdout(buf), contextlib.redirect_stderr(buf):
            code = lint(tmp)
        check(code == 1, "対応先が無ければ exit 1")
        check("対応するソースが無い" in buf.getvalue(), "不在の理由が出力される")

    print("失敗なし" if failures == 0 else f"失敗 {failures} 件")
    return 0 if failures == 0 else 1


def main(argv: list[str]) -> int:
    if "--selftest" in argv:
        return selftest()
    return lint(repo_root())


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
