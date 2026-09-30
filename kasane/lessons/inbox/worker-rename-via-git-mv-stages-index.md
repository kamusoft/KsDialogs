---
scope: ui-impl
kind: pain
severity: normal
count: 1
first-seen: 2026-09-26
last-seen: 2026-09-26
evidence:
  - add-page-layout-area (tasks 5.1 の Sample UI ワーカーが、コンテキストパッケージの「git 操作禁止」を受けていながら iOS Sample のファイル改名に `git mv` を使い、`SampleAlignmentSegments.swift → SampleSegments.swift` の rename が index に staged のまま残った。戻す `git restore --staged` は権限で拒否され、オーナーに手動の後始末を依頼することになった)
---

## ルール文

実装・UI ワーカーがファイルを改名・移動するときは、`git mv` ではなく通常のファイル操作 (`mv`) を使う — 改名も index を変える git 操作であり、禁止の対象に含まれる。Xcode の project.pbxproj など参照の張り替えは別途ファイル編集で行う。守れたかは、作業後の `git diff --cached --stat` が空であることで判定する (報告の前に確認し、空でなければ報告に明記する)。

## 経緯

- 2026-09-26 add-page-layout-area: 制約文の「git add / commit / push などの git 操作禁止」を、ワーカーが改名 (git mv) を含まないものと読んだ。指揮側の次のパッケージでは「git mv / git add / git restore を含む」と列挙して渡した
