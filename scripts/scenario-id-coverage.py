#!/usr/bin/env python3
"""デルタスペックの Scenario ID とテスト名を突合し、網羅されているかを検査する。

使い方:

    python3 scripts/scenario-id-coverage.py                       # 既定の仕様置き場とテスト置き場を突合
    python3 scripts/scenario-id-coverage.py --specs kasane/changes/add-presentation-behavior/specs
    python3 scripts/scenario-id-coverage.py --allow-missing PB-XX-01,PB-XX-02
    python3 scripts/scenario-id-coverage.py --require-mirror      # 両 Native に同じ ID があることも見る
    python3 scripts/scenario-id-coverage.py --show-locations      # ID ごとの検出箇所を並べる
    python3 scripts/scenario-id-coverage.py --selftest            # 正規化と判定の自己テスト

Scenario ID は `#### Scenario: [XX-YY-NN]` の形で仕様の見出しに書かれ、テスト側は言語ごとに
表記が割れる (Swift の表示名 `[XX-YY-NN] …` と関数名 `XX_YY_NN_…`、Kotlin のバッククォート名
`XX-YY-NN …`、C# のメソッド名 `XX_YY_NN_…`)。本検査は区切り文字の差を吸収して突合する
(挙動を Scenario テストで固定する方針は core/ADR-0016)。

テスト側で ID を数えるのは**テストの宣言** (関数・メソッドの宣言行と、その直前に続く属性・注釈) に
現れたものだけである。コメント・説明文・証跡ファイル名にしか ID が無いものは網羅と見なさない
(そうしないと、テストが1本も無くてもコメントを書くだけで網羅済みに見えてしまう)。

判定は 3 つ:

- 仕様にあってテストに無い ID → 失敗 (終了コード 1)。除外指定した ID は失敗にしない
- テストにあって仕様に無い ID → 警告 (テスト名の打ち間違いの検出。終了コードは変えない)
- 領域別の集計を表示する
"""

from __future__ import annotations

import argparse
import glob
import os
import re
import subprocess
import sys

# 仕様の既定の置き場。進行中の変更に加えてアーカイブ済みも既定で見る
# (仕様は完了後にアーカイブへ移る足場であり、移った後も ID の対応は保たれるべきものだから)
DEFAULT_SPEC_GLOBS = [
    "kasane/changes/*/specs/*/spec.md",
    "kasane/changes/archive/*/specs/*/spec.md",
]

# テストソースの既定の置き場 (ビルドルートごとのテストの置き場に対応する)
DEFAULT_TEST_GLOBS = [
    "ios/Tests",
    "android/*/src/test",
    "android/*/src/androidTest",
    "android/api-surface-check/src",
    "maui/*Tests",
    "maui/*ApiSurfaceCheck",
    "maui/android/native/*/src/test",
    "maui/macios/native/*Tests",
    "kmp/*/src/*Test",
    "kmp/api-surface-check/src",
]

# テストソースとして走査する拡張子
TEST_EXT = {".swift", ".kt", ".kts", ".java", ".cs", ".m", ".h"}

# 既定の除外 ID と理由。自動テストで受け止められない Scenario をここに置く。
# PB-SM-01〜03 / MB-SM-01〜03 は Sample アプリのデモ項目 (メニュー項目・デモ画面・調整面・
# ルートごとの登録構成) を対象とする Scenario であり、Sample の通し確認と実機証跡で
# 受け入れるため自動テストを持たない。
# CA-SA-01〜07 は Sample の撮影支援機構 (起動引数・自動再生・one-shot) を対象とする Scenario で、
# 対象が Sample アプリ自身かつ判定が起動時の画面状態であるため、4ルート通しの検証証跡で受け入れる。
# BV-MA-04〜06 は判定の対象がテストの中ではなく、ビルド成果物 (再ビルドの追随) と
# テスト標的そのものの実行可能性であるため、変更の検証記録で受け入れる。
# LD-YS-01 / TS-YS-01 は Sample の Custom Loading / Custom Toast を型指定 show で出したときの
# 通しを対象とする Scenario で、判定が Sample アプリの画面であるため実機証跡で受け入れる。
# PB-KS-01 / LD-KS-01 / TS-KS-01 は kmp ルートの Sample を共有コードの型指定 show で出したときの
# 通しを対象とする Scenario で、同じく判定が Sample アプリの画面であるため実機証跡で受け入れる。
# DM-IO-03 / DM-AN-02 / DM-KM-04 / DM-MA-05 は「ライブラリ本体に日本語の文字列リテラルが
# 残らない」ことを対象とする Scenario で、判定がソースツリー全体の静的 grep であるため、
# 検証コマンドと出力の記録で受け入れる。
# MB-MA-14 は修正前後のビルドを同じ実環境で走らせて呼び出し元へ届く例外を見る Scenario で、
# 判定が実機・エミュレータの画面であるため実機証跡で受け入れる。
DEFAULT_ALLOW_MISSING = {
    "PB-SM-01": "Sample アプリのデモ項目。Sample 通しの実機証跡で受け入れる",
    "PB-SM-02": "Sample アプリのデモ項目。Sample 通しの実機証跡で受け入れる",
    "PB-SM-03": "Sample アプリのデモ項目。Sample 通しの実機証跡で受け入れる",
    "MB-SM-01": "Sample アプリのデモ項目。Sample 通しの実機証跡で受け入れる",
    "MB-SM-02": "Sample アプリのデモ項目。Sample 通しの実機証跡で受け入れる",
    "MB-SM-03": "Sample アプリのデモ項目。Sample 通しの実機証跡で受け入れる",
    "LD-SA-01": "Sample アプリのデモ項目。Sample 通しの実機証跡で受け入れる",
    "LD-SA-02": "Sample アプリのデモ項目。Sample 通しの実機証跡で受け入れる",
    "LD-SA-03": "Sample アプリのデモ項目。Sample 通しの実機証跡で受け入れる",
    "CA-SA-01": "Sample の撮影支援機構。4ルート通しの実機証跡で受け入れる",
    "CA-SA-02": "Sample の撮影支援機構。4ルート通しの実機証跡で受け入れる",
    "CA-SA-03": "Sample の撮影支援機構。4ルート通しの実機証跡で受け入れる",
    "CA-SA-04": "Sample の撮影支援機構。4ルート通しの実機証跡で受け入れる",
    "CA-SA-05": "Sample の撮影支援機構。4ルート通しの実機証跡で受け入れる",
    "CA-SA-06": "Sample の撮影支援機構。4ルート通しの実機証跡で受け入れる",
    "CA-SA-07": "Sample の撮影支援機構。4ルート通しの実機証跡で受け入れる",
    "TS-SA-01": "Sample アプリのデモ項目。Sample 通しの実機証跡で受け入れる",
    "TS-SA-02": "Sample アプリのデモ項目。Sample 通しの実機証跡で受け入れる",
    "TS-SA-03": "Sample アプリのデモ項目。Sample 通しの実機証跡で受け入れる",
    "TS-SA-04": "Sample アプリのデモ項目。Sample 通しの実機証跡で受け入れる",
    "TS-SA-05": "Sample アプリのデモ項目。Sample 通しの実機証跡で受け入れる",
    "TS-SA-06": "Sample アプリのデモ項目。Sample 通しの実機証跡で受け入れる",
    "LD-YS-01": "Sample アプリのデモ項目。Sample 通しの実機証跡で受け入れる",
    "TS-YS-01": "Sample アプリのデモ項目。Sample 通しの実機証跡で受け入れる",
    "PB-KS-01": "Sample アプリのデモ項目。Sample 通しの実機証跡で受け入れる",
    "LD-KS-01": "Sample アプリのデモ項目。Sample 通しの実機証跡で受け入れる",
    "TS-KS-01": "Sample アプリのデモ項目。Sample 通しの実機証跡で受け入れる",
    "BV-MA-04": "bridge 更新のビルド追随。ビルド成果物の実測記録で受け入れる",
    "BV-MA-05": "bridge 未変更時のビルドのスキップ。ビルド成果物の実測記録で受け入れる",
    "BV-MA-06": "bridge テスト標的そのものの全件実行。実行結果の記録で受け入れる",
    "DM-IO-03": "ライブラリ本体に日本語の文字列リテラルが残らないこと。静的 grep の記録で受け入れる",
    "DM-AN-02": "ライブラリ本体に日本語の文字列リテラルが残らないこと。静的 grep の記録で受け入れる",
    "DM-KM-04": "ライブラリ本体に日本語の文字列リテラルが残らないこと。静的 grep の記録で受け入れる",
    "DM-MA-05": "ライブラリ本体に日本語の文字列リテラルが残らないこと。静的 grep の記録で受け入れる",
    "MB-MA-14": "修正前後のビルドを同じ実環境で走らせる A/B 観測。実機証跡で受け入れる",
}

# 両 Native に同じ ID のテストを置く領域 (--require-mirror のときだけ検査する)。
# 領域名だけで持つと、別の機能面が同じ領域名を使ったときに巻き込んでしまうため
# (接頭辞 = 機能面、領域 = その中の観点であり、領域名は機能面をまたいで再利用される)、
# **(接頭辞, 領域) の組**で指定する。
MIRROR_AREAS = {
    # PB (presentation-behavior): 出入りの演出・多段・画面の変化
    ("PB", "TR"),
    ("PB", "MD"),
    ("PB", "WN"),
    # LD (loading): 挙動を定める領域はすべて両 Native ミラーの対象。
    # 形態別の公開面 (LD-IO / LD-AN / LD-MA / LD-KM) と Sample (LD-SA) は対象外
    ("LD", "CO"),
    ("LD", "AT"),
    ("LD", "WN"),
    ("LD", "PR"),
    ("LD", "ST"),
    ("LD", "CV"),
    ("LD", "TR"),
    # TS (toast): 挙動を定める領域はすべて両 Native ミラーの対象。
    # 形態別の公開面 (TS-IO / TS-AN / TS-MA / TS-KM) と Sample (TS-SA) は対象外
    ("TS", "CO"),
    ("TS", "NM"),
    ("TS", "MX"),
    ("TS", "AT"),
    ("TS", "TR"),
    ("TS", "AC"),
}

# ミラー検査での役割の振り分け (リポジトリルートからの先頭セグメント)
MIRROR_ROLES = {"ios": "iOS", "android": "Android"}

# Scenario ID。区切りはハイフンとアンダースコアのどちらも許し、正規化でハイフンへ寄せる。
# 前後は識別子の途中を拾わないよう境界を見る (末尾は関数名の `_` が続くため数字だけを弾く)。
ID_PATTERN = re.compile(
    r"(?<![A-Za-z0-9_])([A-Z]{2,5})[-_]([A-Z]{2,5})[-_]([0-9]{2,3})(?![0-9])"
)

# 仕様の Scenario 見出し
SCENARIO_HEADING_PATTERN = re.compile(r"^#{2,6}\s*Scenario:\s*(.*)$")

# テストの宣言行 (関数・メソッドの宣言) を見分けるパターン。拡張子ごとに使い分ける。
# ここに載らない拡張子は宣言を見分けられないため、ID を数えない。
DECLARATION_PATTERNS = {
    ".swift": re.compile(r"(?<![A-Za-z0-9_])func\s"),
    ".kt": re.compile(r"(?<![A-Za-z0-9_])fun\s"),
    ".kts": re.compile(r"(?<![A-Za-z0-9_])fun\s"),
    # 型と名前が並んでから括弧が来る形 (`public async Task PB_TR_10_X(` など)
    ".cs": re.compile(r"^\s*(?:[A-Za-z_][\w<>\[\].,?]*\s+)+[A-Za-z_]\w*\s*\("),
    ".java": re.compile(r"^\s*(?:[A-Za-z_][\w<>\[\].,?]*\s+)+[A-Za-z_]\w*\s*\("),
    # Objective-C のメソッド宣言
    ".m": re.compile(r"^\s*[-+]\s*\("),
    ".h": re.compile(r"^\s*[-+]\s*\("),
}

# 行コメントの始まり。宣言の直前を遡るときの打ち切りに使う。
COMMENT_LINE_PATTERN = re.compile(r"^\s*(?://|/\*|\*|#)")

# 宣言の直前に続く属性・注釈を遡る上限行数。多行にわたる `@Test(…)` を取りこぼさない深さにする。
MAX_ANNOTATION_LOOKBACK = 12


def repo_root() -> str:
    out = subprocess.run(
        ["git", "rev-parse", "--show-toplevel"],
        capture_output=True,
        text=True,
        check=True,
    )
    return out.stdout.strip()


def normalize_id(match: re.Match[str]) -> str:
    return f"{match.group(1)}-{match.group(2)}-{match.group(3)}"


def extract_ids(text: str) -> list[str]:
    """文字列から Scenario ID を出現順に (重複ありで) 取り出す。"""
    return [normalize_id(m) for m in ID_PATTERN.finditer(text)]


def area_of(scenario_id: str) -> str:
    """ID の領域部を返す (`PB-TR-01` なら `TR`)。"""
    return scenario_id.split("-")[1]


def prefix_of(scenario_id: str) -> str:
    return scenario_id.split("-")[0]


def expand_globs(root: str, patterns: list[str]) -> list[str]:
    """リポジトリルート相対のパターンを実在パスへ展開する (順序は安定させる)。"""
    hits: list[str] = []
    for pattern in patterns:
        for path in glob.glob(os.path.join(root, pattern)):
            hits.append(path)
    return sorted(dict.fromkeys(hits))


def spec_files(root: str, patterns: list[str]) -> list[str]:
    """仕様ファイルを列挙する。ディレクトリ指定はその配下の spec.md を拾う。"""
    files: list[str] = []
    for path in expand_globs(root, patterns):
        if os.path.isdir(path):
            for dirpath, _dirnames, filenames in os.walk(path):
                for name in filenames:
                    if name == "spec.md":
                        files.append(os.path.join(dirpath, name))
        elif path.endswith(".md"):
            files.append(path)
    return sorted(dict.fromkeys(files))


def test_files(root: str, patterns: list[str]) -> list[str]:
    """テストソースを列挙する。"""
    files: list[str] = []
    for path in expand_globs(root, patterns):
        if os.path.isdir(path):
            for dirpath, _dirnames, filenames in os.walk(path):
                for name in filenames:
                    if os.path.splitext(name)[1].lower() in TEST_EXT:
                        files.append(os.path.join(dirpath, name))
        elif os.path.splitext(path)[1].lower() in TEST_EXT:
            files.append(path)
    return sorted(dict.fromkeys(files))


def display_path(root: str, path: str) -> str:
    """表示用のパス。リポジトリ外を指す指定はそのまま絶対パスで見せる。"""
    rel = os.path.relpath(path, root)
    return path if rel.startswith("..") else rel


def read_text(path: str) -> str:
    try:
        with open(path, encoding="utf-8") as f:
            return f.read()
    except (OSError, UnicodeDecodeError):
        return ""


def scan_spec_text(text: str) -> tuple[dict[str, int], list[str], list[str]]:
    """仕様1件分を走査する。

    戻り値は (見出しの ID → 行番号, ID を持たない Scenario 見出しの一覧, 本文だけに現れた ID)。
    正となるのは見出しの ID であり、本文中の相互参照は網羅の母数には数えない。
    """
    heading_ids: dict[str, int] = {}
    headings_without_id: list[str] = []
    body_ids: list[str] = []
    for lineno, line in enumerate(text.splitlines(), start=1):
        heading = SCENARIO_HEADING_PATTERN.match(line)
        if heading:
            ids = extract_ids(heading.group(1))
            if ids:
                heading_ids.setdefault(ids[0], lineno)
            else:
                headings_without_id.append(heading.group(1).strip())
            continue
        body_ids.extend(extract_ids(line))
    stray = [i for i in dict.fromkeys(body_ids) if i not in heading_ids]
    return heading_ids, headings_without_id, stray


def collect_spec_ids(
    root: str, patterns: list[str]
) -> tuple[dict[str, list[str]], list[tuple[str, str]], list[tuple[str, str]], list[str], list[str]]:
    """仕様側の ID を集める。

    戻り値は (ID → 出典 (相対パス:行) の一覧, ID 無し見出し, 本文だけの ID,
    ID を1件も持たないファイル, 走査したファイル)。

    ID を1件も持たないファイルは ID 体系の適用対象外 (体系より前に書かれた仕様) とみなし、
    ID 無し見出しの警告からも外す。ID を使っている仕様の中に ID の無い見出しが混じっている
    場合だけが、付け忘れとして意味のある警告になる。
    """
    ids: dict[str, list[str]] = {}
    without_id: list[tuple[str, str]] = []
    stray: list[tuple[str, str]] = []
    unmanaged: list[str] = []
    files = spec_files(root, patterns)
    for path in files:
        rel = display_path(root, path)
        heading_ids, headings_without_id, body_only = scan_spec_text(read_text(path))
        if not heading_ids:
            unmanaged.append(rel)
            continue
        for scenario_id, lineno in heading_ids.items():
            ids.setdefault(scenario_id, []).append(f"{rel}:{lineno}")
        for heading in headings_without_id:
            without_id.append((rel, heading))
        for scenario_id in body_only:
            stray.append((rel, scenario_id))
    return ids, without_id, stray, unmanaged, [display_path(root, p) for p in files]


def extract_declaration_ids(text: str, ext: str) -> list[str]:
    """テストの宣言に現れる Scenario ID を取り出す (重複は除く)。

    数えるのは次の 2 か所だけで、コメント・本文中の文字列・証跡ファイル名は数えない。

    - 関数・メソッドの宣言行そのもの (`func PB_TR_01_…` / ``fun `PB-KC-01 …` `` / `Task PB_MA_01_…(`)
    - その宣言に続いている属性・注釈 (`@Test("[PB-TR-01] …")` / `[Description("[PB-MA-01] …")]`)。
      多行にわたる属性も拾えるよう、宣言行から空行・コメント・ブロックの境目に当たるまで遡る
    """
    declaration = DECLARATION_PATTERNS.get(ext.lower())
    if declaration is None:
        return []
    lines = text.splitlines()
    found: list[str] = []
    for index, line in enumerate(lines):
        if COMMENT_LINE_PATTERN.match(line) or not declaration.search(line):
            continue
        found.extend(extract_ids(line))
        # 宣言の直前に続く属性・注釈だけを遡って見る。
        # コメント・空行・ブロックの境目に当たった時点で、属性の連なりは終わっている。
        for back in range(1, MAX_ANNOTATION_LOOKBACK + 1):
            previous_index = index - back
            if previous_index < 0:
                break
            previous = lines[previous_index]
            stripped = previous.strip()
            if not stripped or COMMENT_LINE_PATTERN.match(previous):
                break
            if stripped.endswith("{") or stripped.endswith("}") or stripped.endswith(";"):
                break
            found.extend(extract_ids(previous))
    return list(dict.fromkeys(found))


def collect_test_ids(root: str, patterns: list[str]) -> tuple[dict[str, list[str]], list[str]]:
    """テスト側の ID を集める (ID → 検出したファイルの相対パス一覧)。"""
    ids: dict[str, list[str]] = {}
    files = test_files(root, patterns)
    for path in files:
        rel = display_path(root, path)
        ext = os.path.splitext(path)[1]
        for scenario_id in extract_declaration_ids(read_text(path), ext):
            ids.setdefault(scenario_id, []).append(rel)
    return ids, [display_path(root, p) for p in files]


def evaluate(
    spec_ids: set[str], test_ids: set[str], allow_missing: set[str]
) -> tuple[list[str], list[str], list[str]]:
    """網羅の判定を行う。

    戻り値は (失敗となる未網羅, 仕様に無い ID (警告), 除外指定だが実際は網羅済みの ID)。
    仕様に無い ID の警告は、仕様に現れる接頭辞のものだけを対象にする
    (無関係な識別子を打ち間違い候補として拾わないため)。
    """
    known_prefixes = {prefix_of(i) for i in spec_ids}
    missing = sorted(spec_ids - test_ids - allow_missing)
    unknown = sorted(i for i in test_ids - spec_ids if prefix_of(i) in known_prefixes)
    stale_allow = sorted(allow_missing & test_ids)
    return missing, unknown, stale_allow


def mirror_role(rel_path: str) -> str | None:
    top = rel_path.split(os.sep)[0]
    return MIRROR_ROLES.get(top)


def evaluate_mirror(
    spec_ids: set[str], test_locations: dict[str, list[str]]
) -> list[tuple[str, list[str]]]:
    """両 Native に同じ ID があるべき領域について、片側しか無い ID を返す。"""
    gaps: list[tuple[str, list[str]]] = []
    for scenario_id in sorted(spec_ids):
        if (prefix_of(scenario_id), area_of(scenario_id)) not in MIRROR_AREAS:
            continue
        roles = {mirror_role(p) for p in test_locations.get(scenario_id, [])}
        found = sorted(r for r in roles if r)
        if len(found) < len(set(MIRROR_ROLES.values())):
            gaps.append((scenario_id, found))
    return gaps


def area_summary(
    spec_ids: dict[str, list[str]], test_ids: dict[str, list[str]], allow_missing: set[str]
) -> list[tuple[str, int, int, int]]:
    """領域別の (領域名, 網羅数, 母数, 除外数) を並べる。"""
    areas: dict[str, list[str]] = {}
    for scenario_id in spec_ids:
        key = f"{prefix_of(scenario_id)}-{area_of(scenario_id)}"
        areas.setdefault(key, []).append(scenario_id)
    rows = []
    for key in sorted(areas):
        members = areas[key]
        covered = sum(1 for i in members if i in test_ids)
        allowed = sum(1 for i in members if i in allow_missing and i not in test_ids)
        rows.append((key, covered, len(members), allowed))
    return rows


# 正規化と判定の自己テスト。(説明, 入力, 期待) の組で、検査そのものが無音で壊れていないかを見る
NORMALIZE_CASES = [
    ("Swift の表示名", '@Test("[PB-MD-01] 下の段を先に閉じる")', ["PB-MD-01"]),
    ("Swift の関数名", "func PB_TR_01_presentationHookRunsOnce() {}", ["PB-TR-01"]),
    ("Kotlin の関数名", "fun PB_MD_04_下の段を先に閉じる() {}", ["PB-MD-04"]),
    ("Kotlin のバッククォート名", "fun `PB-KC-01 表示中のキャンセル`() {}", ["PB-KC-01"]),
    ("C# のメソッドと属性", '[Description("[PB-MA-01] 添付")] void PB_MA_01_X() {}', ["PB-MA-01", "PB-MA-01"]),
    ("証跡ファイル名", 'capture("PB-SB-01-all-bars-hidden")', ["PB-SB-01"]),
    ("仕様の見出し", "#### Scenario: [PB-WN-03] インセットのみの変化", ["PB-WN-03"]),
    ("1 桁は ID として扱わない", "PB-MD-1", []),
    ("4 桁は ID として扱わない", "PB-MD-0123", []),
    ("識別子の途中は拾わない", "somePB_MD_01Value", []),
    ("小文字は拾わない", "pb-md-01", []),
]

# テスト宣言に限った抽出の自己テスト。(説明, 拡張子, 入力, 期待) の組。
# 「宣言に無い ID は数えない」側を厚くしてあるのは、そこが緩むと
# テストを書かずにコメントだけで網羅済みに見せられるから。
DECLARATION_CASES = [
    (
        "Swift の表示名と関数名",
        ".swift",
        '@Test("[PB-TR-01] 出現の演出")\nfunc PB_TR_01_presentationHookRunsOnce() async {}\n',
        ["PB-TR-01"],
    ),
    (
        "多行にわたる Swift の表示名",
        ".swift",
        '@Test(\n    "[PB-TR-27] 有効範囲外の duration",\n    arguments: [1]\n)\nfunc runsImmediately(value: Int) {}\n',
        ["PB-TR-27"],
    ),
    (
        "Swift のコメントだけの ID は数えない",
        ".swift",
        "/// [PB-TR-02] 退出の演出については別のテストが受け持つ。\nfunc helperForSomethingElse() {}\n",
        [],
    ),
    (
        "Kotlin の関数名",
        ".kt",
        "@Test\nfun PB_MD_04_下の段を先に閉じる() {}\n",
        ["PB-MD-04"],
    ),
    (
        "Kotlin のバッククォート名",
        ".kt",
        "@Test\nfun `PB-KC-01 表示中のキャンセル`() {}\n",
        ["PB-KC-01"],
    ),
    (
        "本文の証跡ファイル名だけの ID は数えない",
        ".kt",
        '@Test\nfun システムバーの見えを撮る() {\n    capture("PB-SB-01-all-bars-hidden")\n}\n',
        [],
    ),
    (
        "C# の属性とメソッド名",
        ".cs",
        '[Test]\n[Description("[PB-MA-01] 添付")]\npublic async Task PB_MA_01_Attaches()\n{\n}\n',
        ["PB-MA-01"],
    ),
    (
        "C# の doc コメントだけの ID は数えない",
        ".cs",
        "/// <summary>[PB-MA-02] については別のテスト。</summary>\npublic void SomethingElse()\n{\n}\n",
        [],
    ),
    (
        "宣言を見分けられない拡張子は数えない",
        ".txt",
        "func PB_TR_01_x() {}\n",
        [],
    ),
]

SPEC_SCAN_CASES = [
    (
        "見出しの ID を母数にし、本文の相互参照は数えない",
        "#### Scenario: [PB-TR-01] あ\n- **THEN** 形態ごとの規約 (PB-TR-08) で観察する\n",
        (["PB-TR-01"], [], ["PB-TR-08"]),
    ),
    (
        "ID の無い見出しを拾う",
        "#### Scenario: ID のない見出し\n",
        ([], ["ID のない見出し"], []),
    ),
]

EVALUATE_CASES = [
    (
        "未網羅・仕様に無い ID・除外の同時判定",
        {"PB-XX-01", "PB-XX-02", "PB-XX-03"},
        {"PB-XX-01", "PB-XX-99", "QQ-YY-01"},
        {"PB-XX-03"},
        (["PB-XX-02"], ["PB-XX-99"], []),
    ),
    (
        "除外指定した ID が実際には網羅されている",
        {"PB-XX-01"},
        {"PB-XX-01"},
        {"PB-XX-01"},
        ([], [], ["PB-XX-01"]),
    ),
    (
        "全件網羅なら未網羅も警告も無い",
        {"PB-XX-01", "PB-XX-02"},
        {"PB-XX-01", "PB-XX-02"},
        set(),
        ([], [], []),
    ),
]

MIRROR_CASES = [
    (
        "片側にしか無い ID を検出する",
        {"PB-TR-01", "PB-MD-01", "PB-SB-01"},
        {
            "PB-TR-01": ["ios/Tests/A.swift", "android/ksdialogs/src/test/B.kt"],
            "PB-MD-01": ["ios/Tests/A.swift"],
            "PB-SB-01": ["android/ksdialogs/src/androidTest/C.kt"],
        },
        ["PB-MD-01"],
    ),
    (
        "同じ領域名でも接頭辞が対象外なら検査しない",
        {"XX-MD-01", "LD-CO-01"},
        {
            # XX-MD-01 は片側しか無いが、(XX, MD) は対象外なので不足にしない
            "XX-MD-01": ["ios/Tests/A.swift"],
            "LD-CO-01": ["ios/Tests/A.swift"],
        },
        ["LD-CO-01"],
    ),
    (
        "LD 系の挙動領域は両 Native にあれば通る",
        {"LD-CO-01", "LD-SA-01"},
        {
            "LD-CO-01": ["ios/Tests/A.swift", "android/ksdialogs/src/test/B.kt"],
            # LD-SA (Sample) はミラー対象外
            "LD-SA-01": [],
        },
        [],
    ),
]


def run_selftest() -> int:
    failures = 0

    print("[ID の抽出と正規化]")
    for name, text, expected in NORMALIZE_CASES:
        actual = extract_ids(text)
        ok = actual == expected
        failures += 0 if ok else 1
        print(f"  {'OK  ' if ok else 'NG  '} {name} (期待 {expected} / 実際 {actual})")

    print("[テスト宣言に限った抽出]")
    for name, ext, text, expected in DECLARATION_CASES:
        actual = extract_declaration_ids(text, ext)
        ok = actual == expected
        failures += 0 if ok else 1
        print(f"  {'OK  ' if ok else 'NG  '} {name} (期待 {expected} / 実際 {actual})")

    print("[仕様の走査]")
    for name, text, expected in SPEC_SCAN_CASES:
        heading_ids, without_id, stray = scan_spec_text(text)
        actual = (list(heading_ids), without_id, stray)
        ok = actual == (list(expected[0]), expected[1], expected[2])
        failures += 0 if ok else 1
        print(f"  {'OK  ' if ok else 'NG  '} {name} (期待 {expected} / 実際 {actual})")

    print("[網羅の判定]")
    for name, spec, tests, allow, expected in EVALUATE_CASES:
        actual = evaluate(spec, tests, allow)
        ok = actual == expected
        failures += 0 if ok else 1
        print(f"  {'OK  ' if ok else 'NG  '} {name} (期待 {expected} / 実際 {actual})")

    print("[終了コードの決定]")
    for label, missing, expected in [("未網羅ありは 1", ["PB-XX-01"], 1), ("未網羅なしは 0", [], 0)]:
        actual = 1 if missing else 0
        ok = actual == expected
        failures += 0 if ok else 1
        print(f"  {'OK  ' if ok else 'NG  '} {label} (期待 {expected} / 実際 {actual})")

    print("[両 Native ミラーの判定]")
    for name, spec, locations, expected in MIRROR_CASES:
        actual = [i for i, _ in evaluate_mirror(spec, locations)]
        ok = actual == expected
        failures += 0 if ok else 1
        print(f"  {'OK  ' if ok else 'NG  '} {name} (期待 {expected} / 実際 {actual})")

    print(f"\n自己テスト: {'全件 OK' if not failures else f'{failures} 件 NG'}")
    return failures


def parse_allow_missing(values: list[str], use_defaults: bool) -> dict[str, str]:
    allow = dict(DEFAULT_ALLOW_MISSING) if use_defaults else {}
    for value in values:
        for raw in value.split(","):
            scenario_id = raw.strip().upper().replace("_", "-")
            if scenario_id:
                allow.setdefault(scenario_id, "コマンドラインでの除外指定")
    return allow


def main() -> int:
    parser = argparse.ArgumentParser(
        description="デルタスペックの Scenario ID とテスト名の網羅を突合する"
    )
    parser.add_argument(
        "--specs",
        action="append",
        default=[],
        metavar="PATH",
        help="仕様の置き場 (ディレクトリ / ファイル / glob。複数指定可。既定は進行中とアーカイブの両方)",
    )
    parser.add_argument(
        "--tests",
        action="append",
        default=[],
        metavar="PATH",
        help="テストソースの置き場 (ディレクトリ / glob。複数指定可)",
    )
    parser.add_argument(
        "--allow-missing",
        action="append",
        default=[],
        metavar="IDS",
        help="テストが無くても失敗としない ID (カンマ区切り)",
    )
    parser.add_argument(
        "--no-default-allow",
        action="store_true",
        help="既定の除外リストを使わず、指定した ID だけを除外する",
    )
    parser.add_argument(
        "--require-mirror",
        action="store_true",
        help="両 Native に同じ ID があるべき領域について、片側しか無い ID も失敗にする",
    )
    parser.add_argument(
        "--show-locations", action="store_true", help="ID ごとの検出箇所を表示する"
    )
    parser.add_argument("--selftest", action="store_true", help="正規化と判定の自己テストを行う")
    args = parser.parse_args()

    if args.selftest:
        return 1 if run_selftest() else 0

    root = repo_root()
    spec_patterns = args.specs or DEFAULT_SPEC_GLOBS
    test_patterns = args.tests or DEFAULT_TEST_GLOBS
    allow = parse_allow_missing(args.allow_missing, not args.no_default_allow)

    spec_ids, headings_without_id, stray_body_ids, unmanaged_specs, scanned_specs = (
        collect_spec_ids(root, spec_patterns)
    )
    test_ids, scanned_tests = collect_test_ids(root, test_patterns)

    if not spec_ids:
        print("[Scenario ID 網羅検査]")
        print(f"  仕様が 1 件も見つかりません (指定: {', '.join(spec_patterns)})")
        return 1

    # 除外は仕様に存在する ID にだけ意味がある (綴りの取り違えを黙って飲み込まないため)
    effective_allow = {i: why for i, why in allow.items() if i in spec_ids}
    missing, unknown, stale_allow = evaluate(
        set(spec_ids), set(test_ids), set(effective_allow)
    )

    print("[Scenario ID 網羅検査]")
    print(
        f"仕様: {len(scanned_specs) - len(unmanaged_specs)} ファイル / Scenario ID {len(spec_ids)} 件"
        + (f" (ID を持たない仕様 {len(unmanaged_specs)} ファイルは対象外)" if unmanaged_specs else "")
    )
    print(f"テスト: {len(scanned_tests)} ファイル / 検出 ID {len(test_ids)} 件")

    # 探索対象がどこも実在しないと「全部通った」と読める空振りになるため、実在しない指定は表に出す
    empty_patterns = [p for p in test_patterns if not test_files(root, [p])]
    if empty_patterns:
        print("\n警告: テストが 1 件も見つからない指定があります (置き場の変更の疑い):")
        for pattern in empty_patterns:
            print(f"  {pattern}")

    print("\n領域別の網羅:")
    total_covered = 0
    total_allowed = 0
    for key, covered, total, allowed in area_summary(spec_ids, test_ids, set(effective_allow)):
        total_covered += covered
        total_allowed += allowed
        state = "OK" if covered + allowed == total else "NG"
        note = f" (除外 {allowed} 件)" if allowed else ""
        print(f"  {state}  {key}  {covered}/{total}{note}")
    print(f"  合計: {total_covered}/{len(spec_ids)} (除外 {total_allowed} 件)")

    if effective_allow:
        print("\n除外 (自動テストの対象外):")
        for scenario_id, why in sorted(effective_allow.items()):
            print(f"  {scenario_id} — {why}")

    if args.show_locations:
        print("\nID ごとの検出箇所:")
        for scenario_id in sorted(spec_ids):
            where = test_ids.get(scenario_id)
            print(f"  {scenario_id}: {', '.join(where) if where else '(テストなし)'}")

    if headings_without_id:
        print("\n警告: ID の無い Scenario 見出し:")
        for rel, heading in headings_without_id:
            print(f"  {rel}: {heading}")

    if stray_body_ids:
        print("\n警告: 見出しに無い ID が仕様本文にあります (相互参照の綴り確認):")
        for rel, scenario_id in stray_body_ids:
            print(f"  {rel}: {scenario_id}")

    if unknown:
        print("\n警告: 仕様に無い ID がテストにあります (テスト名の打ち間違いの疑い):")
        for scenario_id in unknown:
            print(f"  {scenario_id}: {', '.join(test_ids[scenario_id])}")

    if stale_allow:
        print("\n警告: 除外指定した ID が実際には網羅されています (除外の見直し候補):")
        for scenario_id in stale_allow:
            print(f"  {scenario_id}")

    mirror_gaps: list[tuple[str, list[str]]] = []
    if args.require_mirror:
        mirror_gaps = [
            gap for gap in evaluate_mirror(set(spec_ids), test_ids) if gap[0] not in effective_allow
        ]
        print("\n両 Native ミラーの検査:")
        if mirror_gaps:
            for scenario_id, found in mirror_gaps:
                where = "・".join(found) if found else "なし"
                print(f"  NG  {scenario_id} (検出: {where})")
        else:
            print("  OK  対象領域の ID はすべて iOS / Android の双方にあります")

    if missing:
        print("\n未網羅 (仕様にあるがテストに見つからない ID):")
        for scenario_id in missing:
            print(f"  {scenario_id}: {', '.join(spec_ids[scenario_id])}")
        print(f"\n結果: 未網羅 {len(missing)} 件")
        return 1

    if mirror_gaps:
        print(f"\n結果: ミラー不足 {len(mirror_gaps)} 件")
        return 1

    print("\n結果: 未網羅なし")
    return 0


if __name__ == "__main__":
    sys.exit(main())
