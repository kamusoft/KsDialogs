---
scope: process
kind: pain
severity: normal
count: 1
first-seen: 2026-09-08
last-seen: 2026-09-08
evidence:
  - add-native-distribution (実装が worktree に未コミットのまま、配信リポジトリ KsDialogs-SPM へのスナップショット同期・push をオーナーに提案した。オーナーの指摘「先に develop にマージした方が良くない？」で、既定ブランチへ commit・マージしてから同期する順に改めた)
---

## ルール文

配布物 (スナップショット・発行物・tag) をリポジトリの外へ出す手順を提案・実行する前に、その元となる実装を既定ブランチへ commit (worktree ならマージ) し、配布物の記録 (commit メッセージ・証跡) に元の commit を書けるようにする。未コミットの作業ツリーから配布物を切ると、配布物と本体の履歴が対応しなくなり、後から「どの状態から出したか」を辿れない。守れたかは、配布物側の commit メッセージまたは証跡に元リポジトリの commit hash が書かれていることで判定する。

## 経緯

- 2026-09-08 add-native-distribution: tasks 7.1 の「手動で commit・push」を worktree の未コミット状態から行う手順で提案し、オーナーが順序を正した。近縁: [[compat-claim-scope-must-match-evidence]] (主張と実証の対応を成果物に残す)
