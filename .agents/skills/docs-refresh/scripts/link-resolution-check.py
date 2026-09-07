#!/usr/bin/env python3
"""内部リンク解決の検査。

目的:
  検査対象ファイルに書かれた markdown リンクのうち、外部 URL 以外の
  相対リンクがリポジトリ内の実在ファイルへ解決することを確かめる。

入力:
  1 行 1 パスの検査対象一覧 (targets-list.py の出力)。
    ファイルパスは環境変数 DOCS_REFRESH_TARGETS で指定でき、
    未指定時の既定は /tmp/docs-refresh-targets.txt。
  カレントディレクトリはリポジトリルート。

出力:
  標準出力に未解決リンク・欠落ファイルの行。無ければ
  "All internal links resolve"。対象一覧が空のときは検査せずその旨を出す
  (0 件を適合と誤読しないため)。
"""

import os, re
targets_path = os.environ.get("DOCS_REFRESH_TARGETS", "/tmp/docs-refresh-targets.txt")
targets = [l.strip() for l in open(targets_path) if l.strip()]
if not targets:
    print("検査対象が空です (manifest の targets / readmes を確認してから再実行)")
    raise SystemExit(0)
issues = []
for path in targets:
    if not os.path.exists(path):
        issues.append(f"  {path}: MISSING")
        continue
    with open(path, encoding="utf-8") as fh:
        content = fh.read()
    for m in re.finditer(r'\[([^\]]+)\]\(([^)#\s]+)(?:#[^)]*)?\)', content):
        target = m.group(2)
        if target.startswith(("http://", "https://", "mailto:", "file://")):
            continue
        base = os.path.dirname(path) or "."
        resolved = os.path.normpath(os.path.join(base, target))
        if not os.path.exists(resolved):
            issues.append(f"  {path}: [{m.group(1)}]({target}) -> {resolved} MISSING")
print("\n".join(issues) if issues else "All internal links resolve")
