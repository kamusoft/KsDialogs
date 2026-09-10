#!/usr/bin/env python3
"""インストール例に書かれた version の置換と検査。

貼ってそのまま使える依存宣言は、ルート README 2 枚 (英語 / 日本語) と利用者向け Skill に
ある。リリースのたびにその行を同じ version へ揃えるのがこのスクリプトの仕事で、release
workflow が MAUI の pack の前 (作業木) と publish の後 (develop の先端) に呼ぶ。

対象は次の 5 種で、行の形で見つける。

  SwiftPM        .package(url: "https://github.com/kamusoft/KsDialogs-SPM", exact: "X.Y.Z")
  Maven (core)   jp.kamusoft:ksdialogs-core:X.Y.Z
  Maven (compose) jp.kamusoft:ksdialogs:X.Y.Z
  Maven (kmp)    jp.kamusoft:ksdialogs-kmp:X.Y.Z
  NuGet          <PackageReference Include="KsDialogs.Maui" Version="X.Y.Z" />

どの種をどのファイルにいくつ期待するかは TARGET_FILES で持つ。期待していない種はそのファイル
では探さない。値は `<version>` プレースホルダでも実際の version でも受け付け、どちらも新しい
version に置き換える。

コードブロックの中だけを見る、という絞り込みはしない。KsDialogs の Skill は、貼って使える
座標を散文の手順の中にも `api("jp.kamusoft:ksdialogs-kmp:X.Y.Z")` の形で書いており、そこを
置き換え損ねると同じ文書の中で version が食い違うため。座標に version が付かない言及
(`jp.kamusoft:ksdialogs-core` のような artifact 名だけの参照) は、末尾のコロンを要求する
正規表現に掛からないので拾わない。

SwiftPM の宣言は `exact:` で書く。`from:` は prerelease の tag を解決しないため、prerelease
を配る間も貼ってそのまま動く形にならない。現状が `from:` でも `exact:` でも置換後は `exact:`
に揃え、検査モードも `exact:` を要求する。

各ファイルで期待する各種がちょうど期待した本数だけ見つかることを前提とする。多くても少なくても
どの行を書き換えるべきか決まらないので、何も書き換えずに失敗する。

使い方:
  python3 scripts/release/set-readme-version.py <version>           # 置換
  python3 scripts/release/set-readme-version.py --check <version>   # 検査 (不一致で exit 1)
  python3 scripts/release/set-readme-version.py --selftest          # 自己テスト
"""

from __future__ import annotations

import os
import re
import shutil
import subprocess
import sys
import tempfile

# 対象の種別名。エラー出力でどの行が見つからなかったかを示す文字列でもある。
SWIFTPM = "SwiftPM の依存宣言"
MAVEN_CORE = "Maven 座標 (ksdialogs-core)"
MAVEN_COMPOSE = "Maven 座標 (ksdialogs)"
MAVEN_KMP = "Maven 座標 (ksdialogs-kmp)"
NUGET = "NuGet の PackageReference"

# 配信リポジトリの Swift package を指す `url:` の行。散文中の同じ URL を拾わないよう、
# `url:` ラベル付きの形だけを見る。
SPM_URL_RE = re.compile(
    r'url:\s*"https://github\.com/kamusoft/KsDialogs-SPM(?:\.git)?"'
)
# `.package(url: ..., exact: "X")` を 1 行で書いた形。
SPM_INLINE_RE = re.compile(
    r'^(?P<prefix>.*url:\s*"https://github\.com/kamusoft/KsDialogs-SPM(?:\.git)?",\s*)'
    r'(?P<keyword>from|exact)(?P<mid>:\s*")(?P<version>[^"]*)(?P<suffix>".*)$'
)
# `url:` の次の行以降に解決方法を書いた形。
SPM_STANDALONE_RE = re.compile(
    r'^(?P<prefix>\s*)(?P<keyword>from|exact)(?P<mid>:\s*")(?P<version>[^"]*)(?P<suffix>".*)$'
)
# `url:` の行から解決方法の行を探す範囲 (`.package(` の宣言 1 つ分)。
SPM_LOOKAHEAD_LINES = 5


def maven_pattern(artifact_id: str) -> re.Pattern[str]:
    """Maven 座標の行の形。version の後ろは `"` かバッククォートで閉じる。

    コードブロックの `implementation("jp.kamusoft:X:V")` も、散文の
    `` `jp.kamusoft:X:V` `` も同じ形として拾う。
    """
    return re.compile(
        r"^(?P<prefix>.*jp\.kamusoft:" + re.escape(artifact_id) + r":)"
        r"(?P<version>[^\"`]*)(?P<suffix>[\"`].*)$"
    )


MAVEN_CORE_RE = maven_pattern("ksdialogs-core")
MAVEN_COMPOSE_RE = maven_pattern("ksdialogs")
MAVEN_KMP_RE = maven_pattern("ksdialogs-kmp")
NUGET_RE = re.compile(
    r'^(?P<prefix>.*<PackageReference Include="KsDialogs\.Maui" Version=")'
    r'(?P<version>[^"]*)(?P<suffix>".*)$'
)

PATTERNS = {
    MAVEN_CORE: MAVEN_CORE_RE,
    MAVEN_COMPOSE: MAVEN_COMPOSE_RE,
    MAVEN_KMP: MAVEN_KMP_RE,
    NUGET: NUGET_RE,
}

# 対象ファイルと、そのファイルで期待する種別ごとの本数。パスは "/" 区切りのリポジトリ相対。
TARGET_FILES: list[tuple[str, dict[str, int]]] = [
    ("README.md", {SWIFTPM: 1, MAVEN_CORE: 1, MAVEN_COMPOSE: 1, MAVEN_KMP: 1, NUGET: 1}),
    ("README_ja.md", {SWIFTPM: 1, MAVEN_CORE: 1, MAVEN_COMPOSE: 1, MAVEN_KMP: 1, NUGET: 1}),
    ("skills/en/ksdialogs-android/SKILL.md", {MAVEN_CORE: 1, MAVEN_COMPOSE: 1}),
    ("skills/ja/ksdialogs-android/SKILL.md", {MAVEN_CORE: 1, MAVEN_COMPOSE: 1}),
    # 導入節のコードブロックと、iOS ホストの手順に書かれた同じ座標の 2 行。
    ("skills/en/ksdialogs-kmp/SKILL.md", {MAVEN_KMP: 2}),
    ("skills/ja/ksdialogs-kmp/SKILL.md", {MAVEN_KMP: 2}),
    ("skills/en/ksdialogs-kmp/references/ios-host.md", {MAVEN_KMP: 1}),
    ("skills/ja/ksdialogs-kmp/references/ios-host.md", {MAVEN_KMP: 1}),
    ("skills/en/ksdialogs-kmp/references/android-host.md", {MAVEN_COMPOSE: 1}),
    ("skills/ja/ksdialogs-kmp/references/android-host.md", {MAVEN_COMPOSE: 1}),
    ("skills/en/ksdialogs-maui/SKILL.md", {NUGET: 1}),
    ("skills/ja/ksdialogs-maui/SKILL.md", {NUGET: 1}),
    ("skills/en/ksdialogs-aiforms-migration/SKILL.md", {NUGET: 1}),
    ("skills/ja/ksdialogs-aiforms-migration/SKILL.md", {NUGET: 1}),
]

# 全ファイルの期待行数の合計。検査の成功メッセージで使う。
TOTAL_TARGET_LINES = sum(sum(expected.values()) for _, expected in TARGET_FILES)


def repo_root() -> str:
    try:
        out = subprocess.run(
            ["git", "rev-parse", "--show-toplevel"],
            capture_output=True, text=True, check=True,
        ).stdout.strip()
        return out or os.getcwd()
    except Exception:
        return os.getcwd()


def local_path(root: str, relative: str) -> str:
    """"/" 区切りのリポジトリ相対パスを、この OS のパスへ組み立てる。"""
    return os.path.join(root, *relative.split("/"))


class Hit:
    """見つけた 1 行と、その行を新しい version で書き直す方法。"""

    def __init__(self, index: int, version: str, keyword: str, template: str) -> None:
        # 0 始まりの行番号
        self.index = index
        # その行が今持っている version (プレースホルダのこともある)
        self.version = version
        # SwiftPM の解決方法 (`from` / `exact`)。他の種別では空
        self.keyword = keyword
        # 新しい version を差し込む前後の文字列 (改行は含まない)
        self.template = template

    def rewrite(self, version: str) -> str:
        return self.template.replace("\0", version)


def find_swiftpm(lines: list[str]) -> list[Hit]:
    """SwiftPM の依存宣言を探す。

    1 行に畳んだ形と、`url:` の次の行以降に `exact:` を書いた形の両方を拾う。
    置換後は `exact:` に揃えるので、テンプレートには `exact` を焼き込む。
    """
    hits: list[Hit] = []
    for index, raw in enumerate(lines):
        line = raw.rstrip("\n")
        if SPM_URL_RE.search(line) is None:
            continue

        inline = SPM_INLINE_RE.match(line)
        if inline is not None:
            hits.append(Hit(
                index, inline.group("version"), inline.group("keyword"),
                f'{inline.group("prefix")}exact{inline.group("mid")}\0{inline.group("suffix")}',
            ))
            continue

        for offset in range(1, SPM_LOOKAHEAD_LINES + 1):
            if index + offset >= len(lines):
                break
            following = lines[index + offset].rstrip("\n")
            match = SPM_STANDALONE_RE.match(following)
            if match is not None:
                hits.append(Hit(
                    index + offset, match.group("version"), match.group("keyword"),
                    f'{match.group("prefix")}exact{match.group("mid")}\0{match.group("suffix")}',
                ))
                break

    return hits


def find_pattern(lines: list[str], pattern: re.Pattern[str]) -> list[Hit]:
    hits: list[Hit] = []
    for index, raw in enumerate(lines):
        match = pattern.match(raw.rstrip("\n"))
        if match is not None:
            hits.append(Hit(
                index, match.group("version"), "",
                f'{match.group("prefix")}\0{match.group("suffix")}',
            ))
    return hits


def find_targets(lines: list[str], expected: dict[str, int]) -> dict[str, list[Hit]]:
    """期待する種別だけを探す。"""
    found: dict[str, list[Hit]] = {}
    for name in expected:
        if name == SWIFTPM:
            found[name] = find_swiftpm(lines)
        else:
            found[name] = find_pattern(lines, PATTERNS[name])
    return found


Parsed = dict[str, tuple[list[str], dict[str, list[Hit]], dict[str, int]]]


def collect(root: str) -> tuple[Parsed, list[str]]:
    """全対象ファイルを読み、対象行を集める。本数が合わないものは problems に積む。"""
    parsed: Parsed = {}
    problems: list[str] = []

    for relative, expected in TARGET_FILES:
        path = local_path(root, relative)
        if not os.path.isfile(path):
            problems.append(f"{relative}: ファイルが無い")
            continue
        with open(path, encoding="utf-8") as f:
            lines = f.read().splitlines(keepends=True)
        found = find_targets(lines, expected)
        for name, count in expected.items():
            actual = len(found[name])
            if actual != count:
                problems.append(
                    f"{relative}: {name} が {actual} 行ある ({count} 行であるべき)"
                )
        parsed[relative] = (lines, found, expected)

    return parsed, problems


def replace(root: str, version: str) -> int:
    parsed, problems = collect(root)
    if problems:
        for problem in problems:
            print(f"::error::{problem}", file=sys.stderr)
        print("対象行を確定できないため置換しない", file=sys.stderr)
        return 1

    changed = 0
    for relative, (lines, found, expected) in parsed.items():
        for name in expected:
            for hit in found[name]:
                ending = "\n" if lines[hit.index].endswith("\n") else ""
                lines[hit.index] = hit.rewrite(version) + ending
                changed += 1
        with open(local_path(root, relative), "w", encoding="utf-8") as f:
            f.write("".join(lines))

    print(f"{len(TARGET_FILES)} ファイルのインストール例 {changed} 行を {version} にした")
    return 0


def check(root: str, version: str) -> int:
    parsed, problems = collect(root)

    for relative, (_lines, found, expected) in parsed.items():
        for name, count in expected.items():
            if len(found[name]) != count:
                continue
            for hit in found[name]:
                if hit.version != version:
                    problems.append(
                        f"{relative}:{hit.index + 1}: {name} の version が {version} でない "
                        f"(実際は {hit.version})"
                    )
                if name == SWIFTPM and hit.keyword != "exact":
                    problems.append(
                        f"{relative}:{hit.index + 1}: {name} は exact: で書く "
                        f"(実際は {hit.keyword}: — prerelease が解決されない)"
                    )

    if problems:
        for problem in problems:
            print(f"::error::{problem}", file=sys.stderr)
        print(f"インストール例が {version} と一致しない ({len(problems)} 件)", file=sys.stderr)
        return 1

    print(f"インストール例 {TOTAL_TARGET_LINES} 行が {version} と一致する")
    return 0


# --- 自己テスト ------------------------------------------------------------------------
#
# 一時ディレクトリに置いたコピーに対してだけ実行し、リポジトリの README と Skill は
# 読むだけで書き換えない。

SELFTEST_README = """# Title

## Installation

### iOS

```swift
dependencies: [
    .package(
        url: "https://github.com/kamusoft/KsDialogs-SPM",
        from: "<version>"
    )
]
```

For a prerelease, replace `<version>` with `X.Y.Z-beta.N`.
The artifacts `jp.kamusoft:ksdialogs-core` and `jp.kamusoft:ksdialogs` are the coordinates.

### Android

```kotlin
dependencies {
    implementation("jp.kamusoft:ksdialogs-core:0.0.1")
}
```

```kotlin
dependencies {
    implementation("jp.kamusoft:ksdialogs:0.0.1")
}
```

### .NET MAUI

```xml
<PackageReference Include="KsDialogs.Maui" Version="0.0.1" />
```

### Kotlin Multiplatform

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            api("jp.kamusoft:ksdialogs-kmp:0.0.1")
        }
    }
}
```

The iOS application also adds `https://github.com/kamusoft/KsDialogs-SPM`.
"""

SELFTEST_ANDROID_SKILL = """# Android Skill

`jp.kamusoft:ksdialogs-core` and `jp.kamusoft:ksdialogs` are the planned coordinates.

```kotlin
dependencies {
    implementation("jp.kamusoft:ksdialogs-core:<version>")
}
```

```kotlin
dependencies {
    implementation("jp.kamusoft:ksdialogs:<version>")
}
```
"""

SELFTEST_KMP_SKILL = """# KMP Skill

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            api("jp.kamusoft:ksdialogs-kmp:<version>")
        }
    }
}
```

1. Add the single `jp.kamusoft:ksdialogs-kmp:<version>` Maven dependency to the shared module.
2. Add `https://github.com/kamusoft/KsDialogs-SPM` to Xcode Package Dependencies.
"""

SELFTEST_IOS_HOST = """# iOS host integration

1. Add `api("jp.kamusoft:ksdialogs-kmp:<version>")` to the shared module's dependencies.
2. Add `https://github.com/kamusoft/KsDialogs-SPM` to Package Dependencies.
"""

SELFTEST_ANDROID_HOST = """# Android host integration

```kotlin
dependencies {
    implementation("jp.kamusoft:ksdialogs:<version>")
    implementation("androidx.compose.foundation:foundation:1.8.1")
}
```
"""

SELFTEST_NUGET_SKILL = """# MAUI Skill

```xml
<PackageReference Include="KsDialogs.Maui" Version="<version>" />
```
"""

# 期待する種別の組み合わせごとの自己テスト用テキスト。
SELFTEST_BY_TARGETS: dict[tuple[tuple[str, int], ...], str] = {
    ((SWIFTPM, 1), (MAVEN_CORE, 1), (MAVEN_COMPOSE, 1), (MAVEN_KMP, 1), (NUGET, 1)): SELFTEST_README,
    ((MAVEN_CORE, 1), (MAVEN_COMPOSE, 1)): SELFTEST_ANDROID_SKILL,
    ((MAVEN_KMP, 2),): SELFTEST_KMP_SKILL,
    ((MAVEN_KMP, 1),): SELFTEST_IOS_HOST,
    ((MAVEN_COMPOSE, 1),): SELFTEST_ANDROID_HOST,
    ((NUGET, 1),): SELFTEST_NUGET_SKILL,
}


def selftest_tree() -> dict[str, str]:
    """全対象ファイルぶんの自己テスト用テキストを、リポジトリ相対パスで返す。"""
    return {
        relative: SELFTEST_BY_TARGETS[tuple(expected.items())]
        for relative, expected in TARGET_FILES
    }


def selftest() -> int:
    import contextlib
    import io

    failures = 0

    def report(ok: bool, name: str, detail: str = "") -> None:
        nonlocal failures
        failures += 0 if ok else 1
        print(f"  {'OK  ' if ok else 'NG  '} {name}{f' ({detail})' if detail else ''}")

    def run(func, *args) -> tuple[int, str]:
        buf = io.StringIO()
        with contextlib.redirect_stdout(buf), contextlib.redirect_stderr(buf):
            code = func(*args)
        return code, buf.getvalue()

    def stage(root: str, contents: dict[str, str]) -> None:
        for relative, text in contents.items():
            path = local_path(root, relative)
            os.makedirs(os.path.dirname(path), exist_ok=True)
            with open(path, "w", encoding="utf-8") as f:
                f.write(text)

    def read_all(root: str) -> dict[str, str]:
        result = {}
        for relative, _expected in TARGET_FILES:
            with open(local_path(root, relative), encoding="utf-8") as f:
                result[relative] = f.read()
        return result

    def files_with(target: str) -> list[str]:
        return [relative for relative, expected in TARGET_FILES if target in expected]

    print("[置換]")
    with tempfile.TemporaryDirectory() as tmp:
        stage(tmp, selftest_tree())
        code, out = run(replace, tmp, "1.2.3-beta.4")
        report(code == 0, "置換が成功する", out.strip())

        after = read_all(tmp)

        report(
            all('exact: "1.2.3-beta.4"' in after[relative] for relative in files_with(SWIFTPM)),
            "SwiftPM の from: が exact: の新 version になる (README 2 枚)",
        )
        report(
            all('jp.kamusoft:ksdialogs-core:1.2.3-beta.4' in after[relative]
                for relative in files_with(MAVEN_CORE)),
            "ksdialogs-core の座標が新 version になる",
        )
        report(
            all('jp.kamusoft:ksdialogs:1.2.3-beta.4' in after[relative]
                for relative in files_with(MAVEN_COMPOSE)),
            "ksdialogs の座標が新 version になる",
        )
        report(
            all('jp.kamusoft:ksdialogs-kmp:1.2.3-beta.4' in after[relative]
                for relative in files_with(MAVEN_KMP)),
            "ksdialogs-kmp の座標が新 version になる",
        )
        report(
            all('<PackageReference Include="KsDialogs.Maui" Version="1.2.3-beta.4" />'
                in after[relative] for relative in files_with(NUGET)),
            "NuGet の Version が新 version になる",
        )
        report(
            "`jp.kamusoft:ksdialogs-kmp:1.2.3-beta.4` Maven dependency"
            in after["skills/en/ksdialogs-kmp/SKILL.md"],
            "散文の中の座標も置き換える",
        )
        report(
            'replace `<version>` with `X.Y.Z-beta.N`' in after["README.md"],
            "version を持たないプレースホルダの説明は書き換えない",
        )
        report(
            '`jp.kamusoft:ksdialogs-core` and `jp.kamusoft:ksdialogs` are the coordinates'
            in after["README.md"],
            "version の付かない座標の言及は書き換えない",
        )
        report(
            'adds `https://github.com/kamusoft/KsDialogs-SPM`' in after["README.md"],
            "散文の配信リポジトリ URL は書き換えない",
        )
        report(
            all(after[relative].count("1.2.3-beta.4") == sum(expected.values())
                for relative, expected in TARGET_FILES),
            "書き換わるのはファイルごとに期待した本数だけ",
            "; ".join(f"{r}={after[r].count('1.2.3-beta.4')}" for r, _ in TARGET_FILES),
        )

        code, out = run(check, tmp, "1.2.3-beta.4")
        report(code == 0, "置換後の検査が通る", out.strip())

        code, out = run(check, tmp, "9.9.9")
        report(code == 1, "別の version の検査は失敗する")
        report("README.md:11" in out, "README の不一致の行番号が出力される", out.strip())
        report(
            "skills/en/ksdialogs-kmp/references/ios-host.md:3" in out,
            "Skill の不一致の行番号が出力される",
            out.strip(),
        )

    print("[実値からの置換]")
    with tempfile.TemporaryDirectory() as tmp:
        stage(tmp, selftest_tree())
        run(replace, tmp, "1.0.0")
        code, out = run(replace, tmp, "2.0.0")
        report(code == 0, "実値からの置換も成功する", out.strip())
        code, out = run(check, tmp, "2.0.0")
        report(code == 0, "2 度目の置換後も検査が通る", out.strip())

    print("[期待していない種別は見ない]")
    with tempfile.TemporaryDirectory() as tmp:
        tree = selftest_tree()
        # MAUI Skill に Android 向けの座標が混ざっていても、期待する種別ではないので触らない。
        tree["skills/en/ksdialogs-maui/SKILL.md"] = SELFTEST_NUGET_SKILL + (
            "\n```kotlin\n"
            '    implementation("jp.kamusoft:ksdialogs-core:0.0.1")\n'
            "```\n"
        )
        stage(tmp, tree)
        code, out = run(replace, tmp, "1.2.3")
        report(code == 0, "期待外の種別が混ざっていても置換は成功する", out.strip())
        with open(local_path(tmp, "skills/en/ksdialogs-maui/SKILL.md"), encoding="utf-8") as f:
            maui_text = f.read()
        report(
            'jp.kamusoft:ksdialogs-core:0.0.1' in maui_text,
            "期待していない種別の行は書き換えない",
        )
        report(maui_text.count("1.2.3") == 1, "書き換わるのは期待した 1 行だけ")

    print("[該当行が確定できない場合]")
    with tempfile.TemporaryDirectory() as tmp:
        tree = selftest_tree()
        tree["README.md"] = SELFTEST_README.replace(
            '        url: "https://github.com/kamusoft/KsDialogs-SPM",\n'
            '        from: "<version>"\n',
            "        // 形の変わった宣言\n",
        )
        stage(tmp, tree)
        code, out = run(replace, tmp, "1.2.3")
        report(code == 1, "対象行が無ければ置換しない")
        report("README.md" in out and "SwiftPM" in out,
               "見つからなかったファイルと種別が出力される", out.strip())
        with open(local_path(tmp, "README_ja.md"), encoding="utf-8") as f:
            report("ksdialogs-core:0.0.1" in f.read(), "1 枚でも確定できなければ他方も書き換えない")

    with tempfile.TemporaryDirectory() as tmp:
        tree = selftest_tree()
        # KMP Skill の 2 行のうち 1 行を崩す。本数が合わないので置換しない。
        tree["skills/ja/ksdialogs-kmp/SKILL.md"] = SELFTEST_KMP_SKILL.replace(
            "1. Add the single `jp.kamusoft:ksdialogs-kmp:<version>` Maven dependency to the shared module.\n",
            "1. Add the dependency shown above.\n",
        )
        stage(tmp, tree)
        code, out = run(replace, tmp, "1.2.3")
        report(code == 1, "期待本数に足りなければ置換しない")
        report(
            "skills/ja/ksdialogs-kmp/SKILL.md" in out and "1 行ある (2 行であるべき)" in out,
            "見つかった行数と期待本数が出力される",
            out.strip(),
        )
        with open(local_path(tmp, "README.md"), encoding="utf-8") as f:
            report("ksdialogs-core:0.0.1" in f.read(), "Skill 1 枚の不備で README も書き換えない")

    with tempfile.TemporaryDirectory() as tmp:
        tree = selftest_tree()
        tree["README.md"] = SELFTEST_README.replace(
            '    implementation("jp.kamusoft:ksdialogs-core:0.0.1")\n',
            '    implementation("jp.kamusoft:ksdialogs-core:0.0.1")\n'
            '    implementation("jp.kamusoft:ksdialogs-core:0.0.1")\n',
        )
        stage(tmp, tree)
        code, out = run(replace, tmp, "1.2.3")
        report(code == 1, "対象行が期待より多ければ置換しない")
        report("2 行ある (1 行であるべき)" in out, "見つかった行数が出力される", out.strip())

    print("[SwiftPM が from: のままの場合]")
    with tempfile.TemporaryDirectory() as tmp:
        stage(tmp, selftest_tree())
        run(replace, tmp, "1.2.3-beta.4")
        # 置換直後は exact: に揃っているので、README を 1 枚だけ from: へ戻す。
        # version は一致したままなので、検査が落ちる理由は解決方法の語だけになる。
        readme = "README.md"
        path = local_path(tmp, readme)
        with open(path, encoding="utf-8") as f:
            reverted = f.read().replace('exact: "1.2.3-beta.4"', 'from: "1.2.3-beta.4"')
        with open(path, "w", encoding="utf-8") as f:
            f.write(reverted)

        code, out = run(check, tmp, "1.2.3-beta.4")
        report(code == 1, "version が一致していても from: なら検査が失敗する", out.strip())
        report(
            f"{readme}:11" in out and "exact: で書く" in out,
            "exact: を要求する指摘にファイルと行番号が出力される",
            out.strip(),
        )
        report(
            "version が 1.2.3-beta.4 でない" not in out,
            "version の不一致としては報告しない",
            out.strip(),
        )

    print("[対象ファイルが無い場合]")
    with tempfile.TemporaryDirectory() as tmp:
        tree = selftest_tree()
        absent = "skills/ja/ksdialogs-android/SKILL.md"
        del tree[absent]
        stage(tmp, tree)
        code, out = run(replace, tmp, "1.2.3")
        report(code == 1, "対象ファイルが無ければ置換しない")
        report(absent in out and "ファイルが無い" in out, "無いファイルの名前が出力される", out.strip())
        with open(local_path(tmp, "README.md"), encoding="utf-8") as f:
            report("ksdialogs-core:0.0.1" in f.read(), "1 枚が無ければ残りも書き換えない")
        code, out = run(check, tmp, "0.0.1")
        report(code == 1, "対象ファイルが無ければ検査も失敗する", out.strip())

    print("[実物のファイルに対する疎通]")
    root = repo_root()
    missing = [
        relative for relative, _expected in TARGET_FILES
        if not os.path.isfile(local_path(root, relative))
    ]
    if missing:
        report(False, "リポジトリの対象ファイルを読める", ", ".join(missing))
    else:
        with tempfile.TemporaryDirectory() as tmp:
            for relative, _expected in TARGET_FILES:
                destination = local_path(tmp, relative)
                os.makedirs(os.path.dirname(destination), exist_ok=True)
                shutil.copyfile(local_path(root, relative), destination)
            code, out = run(replace, tmp, "9.9.9-rc.7")
            report(code == 0, f"実物のファイルで {TOTAL_TARGET_LINES} 行を確定できる", out.strip())
            code, out = run(check, tmp, "9.9.9-rc.7")
            report(code == 0, "置換したコピーの検査が通る", out.strip())

    print("失敗なし" if failures == 0 else f"失敗 {failures} 件")
    return 0 if failures == 0 else 1


def usage() -> None:
    print(
        "使い方: set-readme-version.py <version> | --check <version> | --selftest",
        file=sys.stderr,
    )


def main(argv: list[str]) -> int:
    if len(argv) == 1 and argv[0] == "--selftest":
        return selftest()
    if len(argv) == 2 and argv[0] == "--check":
        version = argv[1]
    elif len(argv) == 1 and not argv[0].startswith("-"):
        version = argv[0]
    else:
        usage()
        return 2

    if not version or '"' in version:
        print(f"::error::version として使えない値: {version}", file=sys.stderr)
        return 2

    root = repo_root()
    if argv[0] == "--check":
        return check(root, version)
    return replace(root, version)


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
