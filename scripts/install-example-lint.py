#!/usr/bin/env python3
"""インストール例の契約の検査 (cross/ADR-0029)。

貼ってそのまま使える依存宣言は、ルート README 2 枚 (英語 / 日本語) と利用者向け Skill
5 本 x 2 言語 (`skills/{en,ja}/ksdialogs-*/`) の導入節にある。KMP の Skill はホスト別の
手順を `references/android-host.md` と `references/ios-host.md` に分けており、そこにも
宣言がある。これらの例は具体的な version を持たず、version の位置にはプレースホルダ
`{version}` を置く (cross/ADR-0027)。埋め忘れは依存解決の失敗として利用者に必ず露見し、
リリースのたびに書き換える必要もない。

検査する契約は次の 4 つ。

  1. 対象の各行がプレースホルダを持ち、具体的な version を持たない
  2. SwiftPM の依存宣言が `exact:` で書かれている
  3. 各ファイルに最新リリースへの案内がある
  4. 英語版と日本語版が同じ文書の集合を持ち、対応する文書が同じ種類・同じ本数の
     インストール宣言を持つ

対象は次の 5 種。Maven 座標は artifact が 3 つあり、どれが来るかはファイルによって違う。

  SwiftPM          .package(url: "https://github.com/kamusoft/KsDialogs-SPM", exact: "{version}")
  Maven (core)     implementation("jp.kamusoft:ksdialogs-core:{version}")
  Maven (compose)  implementation("jp.kamusoft:ksdialogs:{version}")
  Maven (kmp)      api("jp.kamusoft:ksdialogs-kmp:{version}")
  NuGet            <PackageReference Include="KsDialogs.Maui" Version="{version}" />

契約の検査 (1〜4) が見るのは**コードブロックの中にある宣言だけ**である。同じ字面は散文の
手順の中にも現れるが、そちらは対象にしない。対象外の文書の例示や引用まで拾うと除外規則が
増え続けるためで、この絞り込みの代償は PROSE_DECLARATIONS に書いた 4 行が機械検査の外に
残ることである。**検出 0 件はそれら 4 行まで含めて契約を満たしていることの証明にならない。**

どの種をどのファイルにいくつ期待するかは TARGET_FILES で持つ。README は 5 種すべてを 1 行ずつ
持ち、Skill は自 platform の種だけを持つ。期待していない種がそのファイルのコードブロックに
現れた場合も違反とする。

対象表だけでは、新しい文書が表に登録されないまま増えたときにその存在を検出できない。この
ため `skills/{en,ja}/` の実構成も走査し、表に無い文書・表にあるのに実在しない (または宣言を
失った) 文書・英日で食い違う文書をいずれも失敗とする。この突合だけは散文の宣言も数える —
`references/ios-host.md` のように、宣言が散文の手順にしかない文書があるためである。

使い方:
  python3 scripts/install-example-lint.py            # 検査 (違反があれば exit 1)
  python3 scripts/install-example-lint.py --selftest # 各検査項目の検出力の確認
"""

from __future__ import annotations

import os
import re
import subprocess
import sys

# version の位置に置くプレースホルダ。5 経路すべてで構文としては合法だが、
# どこでも version として解決できない。
PLACEHOLDER = "{version}"

# 最新リリースへの案内先。この URL は常にその時点の最新リリースへ解決される。
LATEST_RELEASE_URL = "https://github.com/kamusoft/KsDialogs/releases/latest"

# 対象の種別名。エラー出力でどの行が問題かを示す文字列でもある。
SWIFTPM = "SwiftPM の依存宣言"
MAVEN_CORE = "Maven 座標 (ksdialogs-core)"
MAVEN_COMPOSE = "Maven 座標 (ksdialogs)"
MAVEN_KMP = "Maven 座標 (ksdialogs-kmp)"
NUGET = "NuGet の PackageReference"
KIND_NAMES = (SWIFTPM, MAVEN_CORE, MAVEN_COMPOSE, MAVEN_KMP, NUGET)

# コードブロックの境界。開始と終了の両方がこの形。
FENCE_RE = re.compile(r"^\s*```")

# 配信リポジトリの Swift package を指す `url:` の行。散文中の同じ URL を拾わないよう、
# `url:` ラベル付きの形だけを見る。
SPM_URL_RE = re.compile(
    r'url:\s*"https://github\.com/kamusoft/KsDialogs-SPM(?:\.git)?"'
)
# `.package(url: ..., exact: "X")` を 1 行で書いた形。
SPM_INLINE_RE = re.compile(
    r'.*url:\s*"https://github\.com/kamusoft/KsDialogs-SPM(?:\.git)?",\s*'
    r'(?P<keyword>from|exact):\s*"(?P<version>[^"]*)".*$'
)
# `url:` の次の行以降に解決方法を書いた形。
SPM_STANDALONE_RE = re.compile(
    r'^\s*(?P<keyword>from|exact):\s*"(?P<version>[^"]*)".*$'
)
# `url:` の行から解決方法の行を探す範囲 (`.package(` の宣言 1 つ分)。
SPM_LOOKAHEAD_LINES = 5


def maven_pattern(artifact_id: str) -> re.Pattern[str]:
    """Maven 座標の行の形。version の後ろは `"` かバッククォートで閉じる。

    コードブロックの `implementation("jp.kamusoft:X:V")` も、散文の
    `` `jp.kamusoft:X:V` `` も同じ形として拾う。artifact 名だけの言及は末尾のコロンが
    無いので掛からない。
    """
    return re.compile(
        r".*jp\.kamusoft:" + re.escape(artifact_id) + r":"
        r'(?P<version>[^"`]*)["`].*$'
    )


PATTERNS = {
    MAVEN_CORE: maven_pattern("ksdialogs-core"),
    MAVEN_COMPOSE: maven_pattern("ksdialogs"),
    MAVEN_KMP: maven_pattern("ksdialogs-kmp"),
    NUGET: re.compile(
        r'.*<PackageReference Include="KsDialogs\.Maui" Version="(?P<version>[^"]*)".*$'
    ),
}

# 対象ファイルと、そのファイルのコードブロックで期待する種別ごとの本数。
# パスは "/" 区切りのリポジトリ相対。ここに挙げていない種別は 0 本を期待する。
TARGET_FILES: list[tuple[str, dict[str, int]]] = [
    ("README.md", {SWIFTPM: 1, MAVEN_CORE: 1, MAVEN_COMPOSE: 1, MAVEN_KMP: 1, NUGET: 1}),
    ("README_ja.md", {SWIFTPM: 1, MAVEN_CORE: 1, MAVEN_COMPOSE: 1, MAVEN_KMP: 1, NUGET: 1}),
    ("skills/en/ksdialogs-ios/SKILL.md", {SWIFTPM: 1}),
    ("skills/ja/ksdialogs-ios/SKILL.md", {SWIFTPM: 1}),
    ("skills/en/ksdialogs-android/SKILL.md", {MAVEN_CORE: 1, MAVEN_COMPOSE: 1}),
    ("skills/ja/ksdialogs-android/SKILL.md", {MAVEN_CORE: 1, MAVEN_COMPOSE: 1}),
    ("skills/en/ksdialogs-kmp/SKILL.md", {MAVEN_KMP: 1}),
    ("skills/ja/ksdialogs-kmp/SKILL.md", {MAVEN_KMP: 1}),
    ("skills/en/ksdialogs-kmp/references/android-host.md", {MAVEN_COMPOSE: 1}),
    ("skills/ja/ksdialogs-kmp/references/android-host.md", {MAVEN_COMPOSE: 1}),
    # 宣言が散文の手順にしかない文書 (PROSE_DECLARATIONS)。コードブロックには 1 行も無い。
    ("skills/en/ksdialogs-kmp/references/ios-host.md", {}),
    ("skills/ja/ksdialogs-kmp/references/ios-host.md", {}),
    ("skills/en/ksdialogs-maui/SKILL.md", {NUGET: 1}),
    ("skills/ja/ksdialogs-maui/SKILL.md", {NUGET: 1}),
    ("skills/en/ksdialogs-aiforms-migration/SKILL.md", {NUGET: 1}),
    ("skills/ja/ksdialogs-aiforms-migration/SKILL.md", {NUGET: 1}),
]

# コードブロックの外に置かれた依存宣言。契約 (プレースホルダ・最新版の案内) は同じく
# 適用されるが、走査がコードブロック内に限られるため機械検査は届かない。適合は人が読んで
# 判定する。ここに挙げるのは「検査の外に何が残っているか」を検査の側に残すためであり、
# 値や本数の検査には使わない。ただし各組がコードブロックの外に実在することだけは
# check_prose_declarations が突き合わせる — 記録が実体を失っても誰も気づかなければ、
# 「検査の限界はここまで」という記述そのものが静かに嘘になるため。
PROSE_DECLARATIONS: list[tuple[str, str]] = [
    ("skills/en/ksdialogs-kmp/SKILL.md", MAVEN_KMP),
    ("skills/ja/ksdialogs-kmp/SKILL.md", MAVEN_KMP),
    ("skills/en/ksdialogs-kmp/references/ios-host.md", MAVEN_KMP),
    ("skills/ja/ksdialogs-kmp/references/ios-host.md", MAVEN_KMP),
]

# コードブロック内で検査する行数の合計。検査の成功メッセージで使う。
TOTAL_CHECKED_LINES = sum(sum(expected.values()) for _, expected in TARGET_FILES)

# 利用者向け Skill の言語ディレクトリ。
LANGUAGES = ("en", "ja")

# 利用者向け Skill の文書のパスの形。group(1) が言語、group(2) が Skill 名、
# group(3) が Skill の中での文書の位置 ("SKILL.md" / "references/ios-host.md")。
SKILL_DOC_RE = re.compile(r"^skills/(en|ja)/([^/]+)/(.+\.md)$")


class Occurrence:
    """見つけた依存宣言 1 件。"""

    def __init__(self, index: int, version: str, keyword: str, in_code: bool) -> None:
        # 0 始まりの行番号
        self.index = index
        # その行が持っている version (プレースホルダのこともある)
        self.version = version
        # SwiftPM の解決方法 (`from` / `exact`)。他の種別では空
        self.keyword = keyword
        # コードブロックの中にあるか
        self.in_code = in_code


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


def code_block_flags(lines: list[str]) -> list[bool]:
    """各行がコードブロックの中にあるかを返す。境界の行自体は外として扱う。"""
    flags: list[bool] = []
    inside = False
    for line in lines:
        if FENCE_RE.match(line):
            inside = not inside
            flags.append(False)
            continue
        flags.append(inside)
    return flags


def find_swiftpm(lines: list[str], flags: list[bool]) -> list[Occurrence]:
    """SwiftPM の依存宣言を探す。

    1 行に畳んだ形と、`url:` の次の行以降に `exact:` を書いた形の両方を拾う。
    宣言の位置は解決方法を書いた行とする (version と `exact:` がそこにあるため)。
    """
    found: list[Occurrence] = []
    for index, raw in enumerate(lines):
        line = raw.rstrip("\n")
        if SPM_URL_RE.search(line) is None:
            continue

        inline = SPM_INLINE_RE.match(line)
        if inline is not None:
            found.append(Occurrence(
                index, inline.group("version"), inline.group("keyword"), flags[index]
            ))
            continue

        for offset in range(1, SPM_LOOKAHEAD_LINES + 1):
            if index + offset >= len(lines):
                break
            match = SPM_STANDALONE_RE.match(lines[index + offset].rstrip("\n"))
            if match is not None:
                found.append(Occurrence(
                    index + offset, match.group("version"), match.group("keyword"),
                    flags[index + offset],
                ))
                break

    return found


def scan(lines: list[str]) -> dict[str, list[Occurrence]]:
    """文書の依存宣言を種別ごとに集める。

    期待していない種別も含めて 5 種すべてを探し、コードブロックの内外も区別せずに拾う。
    絞り込みは呼び出し側が Occurrence.in_code で行う。
    """
    flags = code_block_flags(lines)
    found: dict[str, list[Occurrence]] = {SWIFTPM: find_swiftpm(lines, flags)}

    for name in (MAVEN_CORE, MAVEN_COMPOSE, MAVEN_KMP, NUGET):
        pattern = PATTERNS[name]
        hits: list[Occurrence] = []
        for index, raw in enumerate(lines):
            match = pattern.match(raw.rstrip("\n"))
            if match is not None:
                hits.append(Occurrence(index, match.group("version"), "", flags[index]))
        found[name] = hits

    return found


def read_lines(path: str) -> list[str]:
    with open(path, encoding="utf-8") as f:
        return f.read().splitlines(keepends=True)


def declaring_documents(root: str) -> dict[str, set[str]]:
    """`skills/<言語>/` にあってインストール宣言を持つ文書を言語ごとに集める。

    ここだけは散文の宣言も数える。宣言が散文の手順にしかない文書 (ios-host.md) を
    対象表と突き合わせるには、コードブロックの外まで見る必要があるため。
    戻り値の各要素は Skill 名から始まる言語ディレクトリ内の相対パス。
    """
    result: dict[str, set[str]] = {language: set() for language in LANGUAGES}
    for language in LANGUAGES:
        base = local_path(root, f"skills/{language}")
        for directory, _dirs, files in os.walk(base):
            for name in sorted(files):
                if not name.endswith(".md"):
                    continue
                path = os.path.join(directory, name)
                found = scan(read_lines(path))
                if not any(found[kind] for kind in KIND_NAMES):
                    continue
                inside = os.path.relpath(path, base).replace(os.sep, "/")
                result[language].add(inside)
    return result


def registered_documents() -> dict[str, set[str]]:
    """対象表が登録している利用者向け Skill の文書を言語ごとに集める。"""
    result: dict[str, set[str]] = {language: set() for language in LANGUAGES}
    for relative, _expected in TARGET_FILES:
        match = SKILL_DOC_RE.match(relative)
        if match is not None:
            result[match.group(1)].add(f"{match.group(2)}/{match.group(3)}")
    return result


def actual_skills(root: str) -> dict[str, set[str]]:
    """`skills/<言語>/` に実在する Skill (SKILL.md を持つディレクトリ) を集める。"""
    result: dict[str, set[str]] = {language: set() for language in LANGUAGES}
    for language in LANGUAGES:
        base = local_path(root, f"skills/{language}")
        if not os.path.isdir(base):
            continue
        for name in sorted(os.listdir(base)):
            if os.path.isfile(os.path.join(base, name, "SKILL.md")):
                result[language].add(name)
    return result


def check_structure(root: str, problems: list[str]) -> None:
    """利用者向け Skill の実構成と対象表を突き合わせる。"""
    skills = actual_skills(root)
    for name in sorted(skills["en"] - skills["ja"]):
        problems.append(f"{name}: 英語版だけに存在する Skill (日本語版が無い)")
    for name in sorted(skills["ja"] - skills["en"]):
        problems.append(f"{name}: 日本語版だけに存在する Skill (英語版が無い)")

    registered = registered_documents()
    declaring = declaring_documents(root)

    for language in LANGUAGES:
        for inside in sorted(declaring[language] - registered[language]):
            problems.append(
                f"skills/{language}/{inside}: インストール宣言を持つが検査の対象表に無い"
            )
        for inside in sorted(registered[language] - declaring[language]):
            problems.append(
                f"skills/{language}/{inside}: 対象表にあるがインストール宣言を持たない"
            )

    for inside in sorted(declaring["en"] - declaring["ja"]):
        problems.append(f"{inside}: 英語版だけがインストール宣言を持つ")
    for inside in sorted(declaring["ja"] - declaring["en"]):
        problems.append(f"{inside}: 日本語版だけがインストール宣言を持つ")


def check_files(root: str, problems: list[str]) -> dict[str, dict[str, int]]:
    """各対象ファイルの宣言と案内を検査し、種別ごとの実測本数を返す。"""
    counts: dict[str, dict[str, int]] = {}

    for relative, expected in TARGET_FILES:
        path = local_path(root, relative)
        if not os.path.isfile(path):
            problems.append(f"{relative}: ファイルが無い")
            continue
        lines = read_lines(path)
        found = scan(lines)
        counts[relative] = {
            name: len([o for o in found[name] if o.in_code]) for name in KIND_NAMES
        }

        for name in KIND_NAMES:
            occurrences = [o for o in found[name] if o.in_code]
            wanted = expected.get(name, 0)
            if wanted and len(occurrences) != wanted:
                problems.append(
                    f"{relative}: {name} がコードブロック内に {len(occurrences)} 行ある "
                    f"({wanted} 行であるべき)"
                )
            if not wanted and occurrences:
                problems.append(
                    f"{relative}: このファイルが持たないはずの {name} が "
                    f"コードブロック内に {len(occurrences)} 行ある"
                )
            for occurrence in occurrences:
                if occurrence.version != PLACEHOLDER:
                    problems.append(
                        f"{relative}:{occurrence.index + 1}: {name} の version が "
                        f"プレースホルダ {PLACEHOLDER} でない (実際は {occurrence.version})"
                    )
                if name == SWIFTPM and occurrence.keyword != "exact":
                    problems.append(
                        f"{relative}:{occurrence.index + 1}: {name} は exact: で書く "
                        f"(実際は {occurrence.keyword}: — "
                        f"上限が次のメジャーまで開き固定にならない)"
                    )

        if LATEST_RELEASE_URL not in "".join(lines):
            problems.append(
                f"{relative}: 最新リリースへの案内 ({LATEST_RELEASE_URL}) が無い"
            )

    return counts


def check_language_parity(
    counts: dict[str, dict[str, int]], problems: list[str]
) -> None:
    """英語版と日本語版の文書が同じ種類・同じ本数の宣言を持つことを検査する。"""
    by_document: dict[str, dict[str, str]] = {}
    for relative, _expected in TARGET_FILES:
        match = SKILL_DOC_RE.match(relative)
        if match is None:
            continue
        inside = f"{match.group(2)}/{match.group(3)}"
        by_document.setdefault(inside, {})[match.group(1)] = relative

    for inside in sorted(by_document):
        paths = by_document[inside]
        if set(paths) != set(LANGUAGES):
            continue
        en, ja = paths["en"], paths["ja"]
        if en not in counts or ja not in counts:
            continue
        for name in KIND_NAMES:
            if counts[en][name] != counts[ja][name]:
                problems.append(
                    f"{inside}: {name} の本数が英日で違う "
                    f"(en {counts[en][name]} 行 / ja {counts[ja][name]} 行)"
                )


def check_prose_declarations(root: str, problems: list[str]) -> None:
    """PROSE_DECLARATIONS の各組が、コードブロックの外に実在することを検査する。

    値は検査しない (走査の限界はそのまま)。この一覧が実体を伴わなくなると、成功時に出す
    「散文の宣言が N 行あり、そちらは走査の外」の断り書きが実態と食い違う。
    """
    for relative, kind in PROSE_DECLARATIONS:
        path = local_path(root, relative)
        if not os.path.isfile(path):
            problems.append(f"{relative}: 散文の宣言があるはずのファイルが無い")
            continue
        found = scan(read_lines(path))
        if not [o for o in found[kind] if not o.in_code]:
            problems.append(
                f"{relative}: {kind} の宣言がコードブロックの外に無い "
                f"(散文の宣言の一覧に載っている)"
            )


def lint(root: str) -> int:
    problems: list[str] = []
    check_structure(root, problems)
    check_prose_declarations(root, problems)
    counts = check_files(root, problems)
    check_language_parity(counts, problems)

    if problems:
        for problem in problems:
            print(f"::error::{problem}", file=sys.stderr)
        print(f"インストール例が契約を満たしていない ({len(problems)} 件)", file=sys.stderr)
        return 1

    print(
        f"{len(TARGET_FILES)} ファイルのインストール例 {TOTAL_CHECKED_LINES} 行が契約を満たす "
        f"(別に散文の宣言 {len(PROSE_DECLARATIONS)} 行があり、"
        f"そちらは走査の外 — 適合は人が読んで判定する)"
    )
    return 0


# --- 自己テスト ------------------------------------------------------------------------
#
# 一時ディレクトリに組んだ木に対してだけ実行し、リポジトリの README と Skill は
# 読まない。各検査項目について、違反を入れた木で exit 1 になることを確かめる。

SELFTEST_README = """# Title

## Installation

Replace the placeholder `{version}` with the version you want; see
https://github.com/kamusoft/KsDialogs/releases/latest for the current one.

### iOS

```swift
dependencies: [
    .package(
        url: "https://github.com/kamusoft/KsDialogs-SPM",
        exact: "{version}"
    )
]
```

To pin a prerelease, write the same version string.

### Android

```kotlin
dependencies {
    implementation("jp.kamusoft:ksdialogs-core:{version}")
}
```

```kotlin
dependencies {
    implementation("jp.kamusoft:ksdialogs:{version}")
}
```

### Kotlin Multiplatform

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            api("jp.kamusoft:ksdialogs-kmp:{version}")
        }
    }
}
```

### .NET MAUI

```xml
<ItemGroup>
  <PackageReference Include="KsDialogs.Maui" Version="{version}" />
</ItemGroup>
```
"""

SELFTEST_IOS_SKILL = """# iOS Skill

## Setup

```swift
dependencies: [
    .package(
        url: "https://github.com/kamusoft/KsDialogs-SPM",
        exact: "{version}"
    )
]
```

See https://github.com/kamusoft/KsDialogs/releases/latest for the current version.
"""

SELFTEST_ANDROID_SKILL = """# Android Skill

## Setup

```kotlin
dependencies {
    implementation("jp.kamusoft:ksdialogs-core:{version}")
}
```

```kotlin
dependencies {
    implementation("jp.kamusoft:ksdialogs:{version}")
}
```

See https://github.com/kamusoft/KsDialogs/releases/latest for the current version.
"""

# コードブロックの宣言に加えて、散文にも同じ宣言を持つ文書 (PROSE_DECLARATIONS)。
SELFTEST_KMP_SKILL = """# KMP Skill

## Setup

Add `api("jp.kamusoft:ksdialogs-kmp:{version}")` to the shared module:

```kotlin
commonMain.dependencies {
    api("jp.kamusoft:ksdialogs-kmp:{version}")
}
```

See https://github.com/kamusoft/KsDialogs/releases/latest for the current version.
"""

SELFTEST_ANDROID_HOST = """# Android host

```kotlin
dependencies {
    implementation("jp.kamusoft:ksdialogs:{version}")
}
```

See https://github.com/kamusoft/KsDialogs/releases/latest for the current version.
"""

# 宣言が散文の手順にしかない文書。コードブロックの行は 0 だが、対象表との突合には現れる。
SELFTEST_IOS_HOST = """# iOS host

1. Add `api("jp.kamusoft:ksdialogs-kmp:{version}")` to the shared module.

See https://github.com/kamusoft/KsDialogs/releases/latest for the current version.
"""

SELFTEST_NUGET_SKILL = """# MAUI Skill

## Setup

```xml
<ItemGroup>
  <PackageReference Include="KsDialogs.Maui" Version="{version}" />
</ItemGroup>
```

See https://github.com/kamusoft/KsDialogs/releases/latest for the current version.
"""


def selftest_text(expected: dict[str, int]) -> str:
    """期待する種別の組み合わせから、自己テスト用の本文を選ぶ。"""
    shape = tuple(sorted(expected.items()))
    texts: dict[tuple, str] = {
        tuple(sorted({SWIFTPM: 1, MAVEN_CORE: 1, MAVEN_COMPOSE: 1,
                      MAVEN_KMP: 1, NUGET: 1}.items())): SELFTEST_README,
        tuple(sorted({SWIFTPM: 1}.items())): SELFTEST_IOS_SKILL,
        tuple(sorted({MAVEN_CORE: 1, MAVEN_COMPOSE: 1}.items())): SELFTEST_ANDROID_SKILL,
        tuple(sorted({MAVEN_KMP: 1}.items())): SELFTEST_KMP_SKILL,
        tuple(sorted({MAVEN_COMPOSE: 1}.items())): SELFTEST_ANDROID_HOST,
        (): SELFTEST_IOS_HOST,
        tuple(sorted({NUGET: 1}.items())): SELFTEST_NUGET_SKILL,
    }
    return texts[shape]


def selftest() -> int:
    import contextlib
    import io
    import shutil
    import tempfile

    failures = 0

    def check(ok: bool, name: str, detail: str = "") -> None:
        nonlocal failures
        failures += 0 if ok else 1
        print(f"  {'OK  ' if ok else 'NG  '} {name}{f' ({detail})' if detail else ''}")

    def write(root: str, relative: str, text: str) -> None:
        path = local_path(root, relative)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8") as f:
            f.write(text)

    def build(root: str) -> None:
        """契約を満たす木を組む。"""
        for relative, expected in TARGET_FILES:
            write(root, relative, selftest_text(expected))

    def run(root: str) -> tuple[int, str]:
        buf = io.StringIO()
        with contextlib.redirect_stdout(buf), contextlib.redirect_stderr(buf):
            code = lint(root)
        return code, buf.getvalue()

    with tempfile.TemporaryDirectory() as base:
        print("[契約を満たす木]")
        root = os.path.join(base, "ok")
        build(root)
        code, out = run(root)
        check(code == 0, "違反が無ければ exit 0", out.strip())

        print("[プレースホルダ]")
        root = os.path.join(base, "concrete")
        build(root)
        write(root, "README.md", SELFTEST_README.replace(
            'exact: "{version}"', 'exact: "1.2.3"'
        ))
        code, out = run(root)
        check(code == 1, "SwiftPM への具体 version の書き戻しで exit 1")
        check("README.md:14" in out, "ファイルと行が出力される", out.strip())
        check("1.2.3" in out, "実際の値が出力される")

        root = os.path.join(base, "concrete-maven")
        build(root)
        write(root, "skills/ja/ksdialogs-kmp/SKILL.md", SELFTEST_KMP_SKILL.replace(
            "ksdialogs-kmp:{version}", "ksdialogs-kmp:1.2.3"
        ))
        code, out = run(root)
        check(code == 1, "Maven 座標への具体 version の書き戻しで exit 1")
        check("ksdialogs-kmp" in out, "artifact が分かる種別名が出力される", out.strip())

        print("[SwiftPM の解決方法]")
        root = os.path.join(base, "from")
        build(root)
        write(root, "skills/en/ksdialogs-ios/SKILL.md", SELFTEST_IOS_SKILL.replace(
            'exact: "{version}"', 'from: "{version}"'
        ))
        code, out = run(root)
        check(code == 1, "from: への書き換えで exit 1")
        check("exact: で書く" in out, "exact: を求める理由が出力される", out.strip())

        print("[最新リリースへの案内]")
        root = os.path.join(base, "no-link")
        build(root)
        write(root, "skills/ja/ksdialogs-maui/SKILL.md",
              SELFTEST_NUGET_SKILL.replace(LATEST_RELEASE_URL, "https://example.invalid/"))
        code, out = run(root)
        check(code == 1, "案内が無ければ exit 1")
        check("最新リリースへの案内" in out, "不足の理由が出力される", out.strip())

        print("[対象表と実構成の突合]")
        root = os.path.join(base, "unregistered-skill")
        build(root)
        for language in LANGUAGES:
            write(root, f"skills/{language}/ksdialogs-tvos/SKILL.md", SELFTEST_NUGET_SKILL)
        code, out = run(root)
        check(code == 1, "対象表に無い Skill で exit 1")
        check("ksdialogs-tvos/SKILL.md" in out, "未登録の文書が出力される", out.strip())

        root = os.path.join(base, "unregistered-reference")
        build(root)
        for language in LANGUAGES:
            write(root, f"skills/{language}/ksdialogs-kmp/references/desktop-host.md",
                  SELFTEST_ANDROID_HOST)
        code, out = run(root)
        check(code == 1, "対象表に無い references の文書で exit 1")
        check("references/desktop-host.md" in out,
              "SKILL.md 以外の未登録文書も出力される", out.strip())

        root = os.path.join(base, "missing")
        build(root)
        shutil.rmtree(local_path(root, "skills/ja/ksdialogs-maui"))
        code, out = run(root)
        check(code == 1, "対象表にあるのに実在しない Skill で exit 1")
        check("ファイルが無い" in out, "不在の理由が出力される", out.strip())

        root = os.path.join(base, "declaration-lost")
        build(root)
        write(root, "skills/en/ksdialogs-kmp/references/ios-host.md",
              f"# iOS host\n\nSee {LATEST_RELEASE_URL} for the current version.\n")
        code, out = run(root)
        check(code == 1, "対象表にあるのに宣言を失った文書で exit 1")
        check("インストール宣言を持たない" in out, "宣言の消失が出力される", out.strip())

        print("[英日の構成]")
        root = os.path.join(base, "lang-only")
        build(root)
        write(root, "skills/en/ksdialogs-tvos/SKILL.md", SELFTEST_NUGET_SKILL)
        code, out = run(root)
        check(code == 1, "片言語だけの Skill 追加で exit 1")
        check("英語版だけに存在する Skill" in out, "言語差の理由が出力される", out.strip())

        root = os.path.join(base, "lang-count")
        build(root)
        write(root, "skills/en/ksdialogs-android/SKILL.md",
              SELFTEST_ANDROID_SKILL + """
```kotlin
dependencies {
    implementation("jp.kamusoft:ksdialogs:{version}")
}
```
""")
        code, out = run(root)
        check(code == 1, "片言語だけの宣言追加で exit 1")
        check("本数が英日で違う" in out, "英日の本数差が出力される", out.strip())

        print("[期待していない種別]")
        root = os.path.join(base, "unexpected")
        build(root)
        write(root, "skills/en/ksdialogs-ios/SKILL.md", SELFTEST_IOS_SKILL + """
```xml
<PackageReference Include="KsDialogs.Maui" Version="{version}" />
```
""")
        code, out = run(root)
        check(code == 1, "そのファイルが持たない種別の混入で exit 1")
        check("持たないはずの" in out, "混入した種別が出力される", out.strip())

        print("[散文の宣言の一覧]")
        root = os.path.join(base, "prose-lost")
        build(root)
        write(root, "skills/en/ksdialogs-kmp/SKILL.md", SELFTEST_KMP_SKILL.replace(
            'Add `api("jp.kamusoft:ksdialogs-kmp:{version}")` to the shared module:',
            "Add the dependency to the shared module:",
        ))
        code, out = run(root)
        check(code == 1, "一覧にある散文の宣言を失うと exit 1")
        check("コードブロックの外に無い" in out, "失われた位置が出力される", out.strip())

        print("[散文は見ない]")
        root = os.path.join(base, "prose")
        build(root)
        write(root, "README.md", SELFTEST_README + """
The released version looks like `implementation("jp.kamusoft:ksdialogs-core:1.2.3")`.
""")
        code, out = run(root)
        check(code == 0, "コードブロック外の具体 version は拾わない", out.strip())

    print("失敗なし" if failures == 0 else f"失敗 {failures} 件")
    return 0 if failures == 0 else 1


def main(argv: list[str]) -> int:
    if "--selftest" in argv:
        return selftest()
    return lint(repo_root())


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
