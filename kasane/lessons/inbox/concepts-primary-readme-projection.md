---
scope: process
kind: pain
severity: normal
count: 1
first-seen: 2026-08-15
last-seen: 2026-08-15
evidence:
  - add-vertical-slice (蒸留で「パリティ規約の正は samples/README でよい (concepts 化は二重管理)」と提案し、オーナーが「README が concepts の写像というのが Kasane の流儀。concepts が正で README はユーザー向け抜粋。むしろ concepts 化すべき」と却下)
---

## ルール文

実装側 README と concepts に同じ知識が載る場合、正は concepts に置き、README はそこからの利用者向け抜粋 (写像) として扱う。蒸留で「README にあるから concepts 化しない」という判断をしない — 重複は写像方向 (concepts → README) の明示で解消する。

## 経緯

- 2026-08-15 add-vertical-slice: SampleTheme / パリティ規約の concepts 化を「samples/README.md が既に持つので二重管理」と見送り提案したところ、オーナーが層の主従 (concepts = 正 / README = 写像) を明示して却下。価値 lint の「再導出が容易」判定を README の存在で満たすのは誤りで、README は正典ではなく抜粋という位置づけを再確認した
