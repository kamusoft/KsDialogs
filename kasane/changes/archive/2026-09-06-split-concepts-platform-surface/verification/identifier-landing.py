#!/usr/bin/env python3
"""公開名の着地検査 — core/api の識別子が再構成後の concept のどこにあるかを突き合わせる。

前提: カレントディレクトリはリポジトリルート (docs-refresh の scripts と同じ約束)。
標準ライブラリのみを使う。

2 モード:

  collect  baseline 時点の kasane/concepts/core/api/*.md からバッククォート識別子を
           すべて抽出し (STOP 語だけ除く)、期待する移動先の初期値つきの台帳を
           markdown の表として書き出す。初期値は機械的な推定であり、人が確認して直す。

             python3 kasane/changes/<change>/verification/identifier-landing.py collect \
               --out kasane/changes/<change>/verification/identifier-ledger.md

  check    台帳の各行について、期待する移動先の concept にその識別子が出現するかを調べる。
           全行が「着地」または「意図して落とした (理由つき)」で説明されていれば成功、
           未説明が 1 件でもあれば exit 1。--write を付けると、機械的に着地と判定できた行の
           判定欄だけを台帳に書き戻す (説明が要る行は空のまま残す)。

             python3 kasane/changes/<change>/verification/identifier-landing.py check \
               --ledger kasane/changes/<change>/verification/identifier-ledger.md

着地と数える範囲:
  - 期待する移動先のドメインのディレクトリ配下の concept にだけ出現を探す。
    別ドメインの concept に出ていても着地とは数えない (誤配置を検出するため)。
  - manifest の excluded に載る concept (利用者向け Skill の源泉から外した文書) への出現も
    着地と数えない。移動先 architecture はこの区分に当たるため、そこへ移した識別子は
    「意図して落とした (理由)」として台帳側で説明する。
"""

import argparse
import json
import os
import re
import sys

CONCEPTS_ROOT = "kasane/concepts"
SOURCE_GLOB_DIR = os.path.join(CONCEPTS_ROOT, "core", "api")
MANIFEST = "skills/.manifest.json"

# API 名網羅検査 (.agents/skills/docs-refresh/scripts/api-coverage-check.py) と同じ抽出規則。
# 数値始まりのトークンは正規表現の時点で外れるため、除くのは STOP 語だけ。
TOKEN = re.compile(r"`([A-Za-z_][A-Za-z0-9_.]*(?:\(\))?)`")
STOP = {"true", "false", "null", "nil", "None", "self", "this", "var", "val", "let",
        "public", "internal", "private", "open", "static", "enum", "class", "struct",
        "interface", "protocol", "data", "case", "import", "async", "await"}

# 移動先の名前 -> 出現を探すディレクトリ
DEST_DIRS = {
    "core": os.path.join(CONCEPTS_ROOT, "core", "api"),
    "architecture": os.path.join(CONCEPTS_ROOT, "core", "architecture"),
    "ios": os.path.join(CONCEPTS_ROOT, "ios"),
    "android": os.path.join(CONCEPTS_ROOT, "android"),
    "maui": os.path.join(CONCEPTS_ROOT, "maui"),
    "kmp": os.path.join(CONCEPTS_ROOT, "kmp"),
}
DEST_ORDER = ["core", "ios", "android", "maui", "kmp", "architecture"]
NO_DEST = "-"          # どこにも移さない (識別子表記をやめる等) 行の移動先
NOT_A_CONCEPT = {"index.md", "log.md", "rules.md"}

# 移動先の初期値を推定するための語。行の中でトークンの直前に現れた語を手掛かりにする。
PLATFORM_WORDS = [
    (re.compile(r"iOS|Swift|UIKit|SwiftUI|Apple"), "ios"),
    (re.compile(r"Android|Kotlin|Compose"), "android"),
    (re.compile(r"MAUI|C#|\.NET"), "maui"),
    (re.compile(r"KMP|commonMain|共有コード"), "kmp"),
]
ARCHITECTURE_HEADINGS = re.compile(r"共通ケース表")
HINT_WINDOW = 80       # トークンの直前を何文字さかのぼって形態名を探すか


def read(path):
    with open(path, encoding="utf-8") as fh:
        return fh.read()


def tokens_in(text):
    return [t for t in TOKEN.findall(text) if t.rstrip("()") not in STOP]


def excluded_concepts():
    """manifest の excluded (Skill の源泉から外した concept) を concepts 相対パスの集合で返す。"""
    if not os.path.exists(MANIFEST):
        return set()
    data = json.loads(read(MANIFEST))
    ex = data.get("excluded", {})
    return set(ex.keys() if isinstance(ex, dict) else ex)


def concept_files(directory, skip):
    """ディレクトリ配下の concept ファイル (index / log / rules と skip を除く *.md) を返す。"""
    found = []
    for base, _dirs, names in os.walk(directory):
        for name in sorted(names):
            if not name.endswith(".md") or name in NOT_A_CONCEPT:
                continue
            path = os.path.join(base, name)
            if os.path.relpath(path, CONCEPTS_ROOT) in skip:
                continue
            found.append(path)
    return found


# ---------------------------------------------------------------- collect

def first_cell(line):
    """表の行なら先頭セルの文字列、そうでなければ None。"""
    stripped = line.strip()
    if not stripped.startswith("|"):
        return None
    return stripped.strip("|").split("|")[0].strip()


def hints_from(text):
    return {domain for pattern, domain in PLATFORM_WORDS if pattern.search(text)}


def collect():
    """core/api の識別子を出典・移動先の初期値つきで集める。"""
    entries = {}   # 識別子 -> {"sources": [(file, heading)], "dests": set()}
    for name in sorted(os.listdir(SOURCE_GLOB_DIR)):
        if not name.endswith(".md") or name in NOT_A_CONCEPT:
            continue
        heading = ""
        for line in read(os.path.join(SOURCE_GLOB_DIR, name)).splitlines():
            if line.startswith("#"):
                heading = line.lstrip("#").strip()
            if "`" not in line:
                continue
            cell = first_cell(line)
            for match in TOKEN.finditer(line):
                token = match.group(1)
                if token.rstrip("()") in STOP:
                    continue
                entry = entries.setdefault(token, {"sources": [], "dests": set()})
                source = (name, heading)
                if source not in entry["sources"]:
                    entry["sources"].append(source)
                if ARCHITECTURE_HEADINGS.search(heading):
                    entry["dests"].add("architecture")
                    continue
                # 表の行は先頭セル (形態列) を、散文はトークン直前の窓を手掛かりにする
                window = cell if cell else line[max(0, match.start() - HINT_WINDOW):match.start()]
                entry["dests"].update(hints_from(window) or {"core"})
    return entries


LEDGER_HEADER = """# 着地台帳 (core/api の公開名 × 期待する移動先)

`kasane/concepts/core/api/*.md` のバッククォート識別子を、baseline (verification/baseline.md) の
時点ですべて抽出したもの。抽出規則は API 名網羅検査と同じで、STOP 語だけを除く
(小文字の API 名も落とさない)。表は `identifier-landing.py collect` が生成し、
「期待する移動先」の初期値 (表の形態列・節見出し・トークン直前の形態名からの機械推定) を
実装者が 1 行ずつ確認して確定させ、「備考」に確認の根拠を書いている。

- **期待する移動先**: `core` (共通概念名として core に残す) / `ios` / `android` / `maui` / `kmp` /
  `architecture` (検証機構の記述として `core/architecture/` へ移す) / `-` (どこにも移さない)。複数可
- **判定**: 再構成後に `identifier-landing.py check` で埋める。「着地 (移動先)」か
  「意図して落とした (理由)」の二択で、未説明が 0 件になるまで残す。
  `architecture` と `-` は利用者向け Skill の源泉から外れるため、着地とは数えず理由つきの説明が要る
- 再構成の過程で移動先の見立てが変わったら、移動先の列を書き換えて備考に理由を足す
  (`collect` を回し直すと確認結果ごと上書きされるので、初回の生成後は手で直す)

"""


def render_ledger(entries):
    lines = [
        LEDGER_HEADER.rstrip("\n"),
        "",
        "| 識別子 | 出典 (concept / 節) | 期待する移動先 | 判定 | 備考 |",
        "|---|---|---|---|---|",
    ]
    for token in sorted(entries, key=lambda t: (t.lower(), t)):
        entry = entries[token]
        sources = "<br>".join(f"{f} / {h}" for f, h in entry["sources"])
        dests = " ".join(d for d in DEST_ORDER if d in entry["dests"]) or NO_DEST
        lines.append(f"| `{token}` | {sources} | {dests} |  |  |")
    return "\n".join(lines) + "\n"


# ---------------------------------------------------------------- check

ROW = re.compile(r"^\|(?P<cells>.*)\|\s*$")


def parse_ledger(path):
    """台帳の表を読む。返すのは (行番号, 識別子, 移動先リスト, 判定, 生セル列) の並び。"""
    rows = []
    for number, line in enumerate(read(path).splitlines(), 1):
        match = ROW.match(line.rstrip())
        if not match:
            continue
        cells = [c.strip() for c in match.group("cells").split("|")]
        if len(cells) < 4 or set(cells[0]) <= set("-: ") or cells[0] == "識別子":
            continue
        token = cells[0].strip("`")
        dests = [d for d in cells[2].replace(",", " ").split() if d != NO_DEST]
        unknown = [d for d in dests if d not in DEST_DIRS]
        if unknown:
            raise SystemExit(f"{path}:{number}: 未知の移動先 {unknown}")
        rows.append((number, token, dests, cells[3], cells))
    return rows


def landing_index(skip):
    """移動先ごとに「その配下の concept に出現する識別子 -> ファイル群」を作る。"""
    index = {}
    for dest, directory in DEST_DIRS.items():
        found = {}
        if os.path.isdir(directory):
            for path in concept_files(directory, skip):
                for token in set(tokens_in(read(path))):
                    found.setdefault(token, []).append(os.path.relpath(path, CONCEPTS_ROOT))
        index[dest] = found
    return index


def present(index, dest, token):
    """その移動先に識別子が出現するか。`X()` は `X` の出現でも着地と数える。"""
    found = index[dest]
    if token in found:
        return found[token]
    if token.endswith("()") and token[:-2] in found:
        return found[token[:-2]]
    return []


def check(ledger_path, write):
    skip = excluded_concepts()
    index = landing_index(skip)
    excluded_index = {}
    for dest, directory in DEST_DIRS.items():
        hits = {}
        if os.path.isdir(directory):
            for base, _dirs, names in os.walk(directory):
                for name in sorted(names):
                    path = os.path.join(base, name)
                    rel = os.path.relpath(path, CONCEPTS_ROOT)
                    if not name.endswith(".md") or rel not in skip:
                        continue
                    for token in set(tokens_in(read(path))):
                        hits.setdefault(token, []).append(rel)
        excluded_index[dest] = hits

    rows = parse_ledger(ledger_path)
    landed, explained, unexplained = [], [], []
    filled = {}
    for number, token, dests, verdict, cells in rows:
        # 着地対象は Skill の源泉になる移動先だけ。architecture は源泉から外れるため数えない
        targets = [d for d in dests if d != "architecture"]
        missing = [d for d in targets if not present(index, d, token)]
        if targets and not missing:
            landed.append((token, dests))
            if not verdict:
                filled[number] = "着地 (" + " ".join(targets) + ")"
            continue
        if verdict.startswith("着地"):
            unexplained.append((number, token, f"判定は着地だが {missing} に出現しない"))
            continue
        if verdict.startswith("意図して落とした") and len(verdict) > len("意図して落とした"):
            explained.append((token, verdict))
            continue
        if missing:
            reason = f"移動先 {' '.join(missing)} の concept に出現しない"
        elif "architecture" in dests:
            reason = "利用者向け Skill の源泉から外れる移動先 (architecture) のため、理由つきの説明が要る"
        elif cells[2] == NO_DEST:
            reason = "移動先なし (どこにも移さない) のため、理由つきの説明が要る"
        else:
            reason = "移動先が未記入"
        elsewhere = sorted({d for d in DEST_DIRS if d not in dests and present(index, d, token)})
        if elsewhere:
            reason += f" (期待外の出現: {' '.join(elsewhere)})"
        in_excluded = sorted({d for d in dests if excluded_index[d].get(token)
                              or (token.endswith("()") and excluded_index[d].get(token[:-2]))})
        if in_excluded:
            reason += f" (源泉から外した concept には出現: {' '.join(in_excluded)})"
        unexplained.append((number, token, reason))

    if write and filled:
        lines = read(ledger_path).splitlines()
        for number, verdict in filled.items():
            cells = lines[number - 1].rstrip().strip("|").split("|")
            cells[3] = f" {verdict} "
            lines[number - 1] = "|" + "|".join(cells) + "|"
        with open(ledger_path, "w", encoding="utf-8") as fh:
            fh.write("\n".join(lines) + "\n")
        print(f"判定欄を {len(filled)} 行に書き戻した: {ledger_path}")

    print(f"台帳 {len(rows)} 行: 着地 {len(landed)} / 意図して落とした {len(explained)} / 未説明 {len(unexplained)}")
    for number, token, reason in unexplained:
        print(f"  未説明 {ledger_path}:{number} `{token}` — {reason}")
    return 1 if unexplained else 0


def main():
    parser = argparse.ArgumentParser(description="公開名の着地検査")
    parser.add_argument("mode", choices=["collect", "check"])
    parser.add_argument("--out", help="collect の出力先 (既定は標準出力)")
    parser.add_argument("--ledger", help="check が読む台帳のパス")
    parser.add_argument("--write", action="store_true",
                        help="check で着地と判定できた行の判定欄を台帳に書き戻す")
    args = parser.parse_args()

    if not os.path.isdir(CONCEPTS_ROOT):
        raise SystemExit(f"{CONCEPTS_ROOT} が見つからない — リポジトリルートで実行すること")

    if args.mode == "collect":
        text = render_ledger(collect())
        if args.out:
            with open(args.out, "w", encoding="utf-8") as fh:
                fh.write(text)
            print(f"台帳の表を書き出した: {args.out}")
        else:
            sys.stdout.write(text)
        return 0
    if not args.ledger:
        raise SystemExit("check には --ledger が要る")
    return check(args.ledger, args.write)


if __name__ == "__main__":
    sys.exit(main())
