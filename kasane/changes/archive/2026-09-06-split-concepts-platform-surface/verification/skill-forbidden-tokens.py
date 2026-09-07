#!/usr/bin/env python3
"""禁止トークンの負の検査 (task 6.1) — 再生成後の Skill と検査候補に他 platform 名が無いことを見る。

前提: カレントディレクトリはリポジトリルート (docs-refresh の scripts と同じ約束)。
標準ライブラリのみを使う。

2 面の検査 (design.md Decision 7 / specs user-skills-manifest の 2 Scenario):

  skills    再生成後の Skill 範囲 (skills/{en,ja}/<skill>/** の全ファイル) の全文から
            トークンを抽出し、当該 platform の禁止集合と完全一致で突き合わせる。
            突き合わせ単位は verification/baseline.md 末尾「Skill 側の負の検査 (task 6.1) の
            突き合わせ単位」に従い、本文のバッククォート span と fenced code block の両方から
            api-coverage-check.py と同じ識別子規則 (`[A-Za-z_][A-Za-z0-9_.]*` に末尾 `()` を許す)
            で単語境界で切って拾う。

  candidates  api-coverage-check.py の報告行 ("  <skill> <- <concept>: <トークン列>") の
            候補トークンを、同じ platform の禁止集合と突き合わせる。

    python3 kasane/changes/<change>/verification/skill-forbidden-tokens.py

一致が 1 件でもあれば exit 1。仕分けは task 6.2 に従い、禁止集合から外して通すことはしない。
"""

import json
import os
import re
import subprocess
import sys

SKILLS_ROOT = "skills"
FIXTURE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "forbidden-tokens.json")
COVERAGE = ".agents/skills/docs-refresh/scripts/api-coverage-check.py"
LANGS = ["en", "ja"]

# Skill ディレクトリ名 -> 禁止集合のキー
SKILL_PLATFORM = {
    "ksdialogs-ios": "ios",
    "ksdialogs-android": "android",
    "ksdialogs-maui": "maui",
    "ksdialogs-kmp": "kmp",
    "ksdialogs-aiforms-migration": "aiforms-migration",
}

# api-coverage-check.py と同じ識別子規則。単語境界 (直前が識別子文字・ドットでない) で切る。
TOKEN = re.compile(r"(?<![A-Za-z0-9_.])([A-Za-z_][A-Za-z0-9_.]*(?:\(\))?)")
WHOLE_TOKEN = re.compile(r"[A-Za-z_][A-Za-z0-9_.]*(?:\(\))?")
FENCE = re.compile(r"^[ \t]*(`{3,}|~{3,})")
INLINE_SPAN = re.compile(r"`+([^`\n]+)`+")
COVERAGE_LINE = re.compile(r"^\s+(\S+) <- (\S+): (.+)$")


def read(path):
    with open(path, encoding="utf-8") as fh:
        return fh.read()


def normalize(raw):
    """末尾の `()` を保ったまま、区切りとして付いたドットを落とす。"""
    suffix = "()" if raw.endswith("()") else ""
    core = raw[:-2] if suffix else raw
    core = core.rstrip(".")
    return core + suffix if core else ""


def tokens_with_lines(path):
    """ファイルから (トークン, 行番号) を重複なしで拾う (同じトークンは初出行で代表する)。

    本文のバッククォート span は api-coverage-check.py と同じ規則で、span の中身**全体**が
    識別子のものだけを 1 トークンとして採る (`ksdialogs-maui` のような span は識別子ではない)。
    fenced code block はバッククォートの区切りが無いので、単語境界で切って拾う。

    戻り値は トークン -> (行番号, 出所) で、出所は "span" (バッククォート) か "code" (コード例)。
    """
    found = {}
    fence = None

    def add(token, lineno, origin):
        token = normalize(token)
        if token and token not in found:
            found[token] = (lineno, origin)

    for lineno, line in enumerate(read(path).splitlines(), start=1):
        opener = FENCE.match(line)
        if fence is None:
            if opener:
                fence = opener.group(1)[0] * 3
                continue
            for span in INLINE_SPAN.finditer(line):
                matched = WHOLE_TOKEN.fullmatch(span.group(1))
                if matched:
                    add(matched.group(0), lineno, "span")
        else:
            if opener and opener.group(1)[0] * 3 == fence:
                fence = None
                continue
            for raw in TOKEN.findall(line):
                add(raw, lineno, "code")
    return found


def skill_files(skill):
    for lang in LANGS:
        root = os.path.join(SKILLS_ROOT, lang, skill)
        for base, _dirs, names in os.walk(root):
            for name in sorted(names):
                yield os.path.join(base, name)


def check_skills(fixture):
    """検査 1: Skill 本文への負の検査。(見出し, 一致行) を返す。"""
    results = []
    for skill in sorted(SKILL_PLATFORM):
        banned = set(fixture[SKILL_PLATFORM[skill]]["tokens"])
        hits = []
        for path in sorted(skill_files(skill)):
            for token, (lineno, origin) in sorted(tokens_with_lines(path).items()):
                if token in banned:
                    hits.append(f"{path}:{lineno}: {token} ({origin})")
        results.append((skill, hits))
    return results


def coverage_lines():
    out = subprocess.run([sys.executable, COVERAGE], capture_output=True, text=True,
                         check=True).stdout
    return out.rstrip("\n").splitlines()


def check_candidates(fixture, lines):
    """検査 2: api-coverage-check.py の候補への負の検査。(見出し, 一致行) を返す。"""
    results = {skill: [] for skill in SKILL_PLATFORM}
    for line in lines:
        matched = COVERAGE_LINE.match(line)
        if not matched:
            continue
        skill, src, toks = matched.groups()
        if skill not in SKILL_PLATFORM:
            continue
        banned = set(fixture[SKILL_PLATFORM[skill]]["tokens"])
        for token in (t.strip() for t in toks.split(",")):
            if normalize(token) in banned:
                results[skill].append(f"{src}: {token}")
    return [(skill, results[skill]) for skill in sorted(results)]


def report(title, results):
    print(title)
    total = 0
    for skill, hits in results:
        total += len(hits)
        print(f"  {skill}: {len(hits)} 件" + ("" if hits else " (0 件)"))
        for hit in hits:
            print(f"    {hit}")
    print(f"  合計: {total} 件")
    return total


def main():
    fixture = json.loads(read(FIXTURE))
    lines = coverage_lines()

    print("$ python3 .agents/skills/docs-refresh/scripts/api-coverage-check.py")
    print("\n".join(lines))
    print(f"  (報告 {len(lines)} 行)")
    print()

    total = report("[1] Skill 本文への負の検査 (skills/{en,ja}/<skill>/** の全ファイル)",
                   check_skills(fixture))
    print()
    total += report("[2] 検査候補への負の検査 (api-coverage-check.py の候補トークン)",
                    check_candidates(fixture, lines))
    print()
    print("負の検査: 一致 0 件" if total == 0 else f"負の検査: 一致 {total} 件 — task 6.2 の仕分けへ")
    return 0 if total == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
