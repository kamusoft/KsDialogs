---
scope: spec-review
kind: pain
severity: normal
count: 2
first-seen: 2026-09-30
last-seen: 2026-10-20
evidence:
  - rename-current-page-marker (改名のデルタスペックで、Requirement が「診断メッセージ・デバッグ用の表示名は新しい名前を名指す」と SHALL を置いたのに、Scenario は旧名の残存検索だけだった (文言を消しても別名にしても通る)。テストの欠落の確認も全スイートの合計件数との比較で、対象スイートが消えても通る形だった。ホストの自己レビュー 2 周は通過し、相方 second-opinion-spec-001 が Major 2 件で検出。差分が旧名→新名の置き換えだけであること・テスト名単位の突き合わせへ書き直した)
  - guard-kmp-swift-int-result-type (tasks.md 2.2 が `register` の登録の確かめ方に「その型の show が失敗になる」を認めており、`show(result: Int.self)` は登録の有無によらず失敗するので、登録が何もしない実装でも通る形だった。ホストの自己レビュー 2 周は通過し、相方 second-opinion-spec-001 が Minor で検出。登録直後の `declaredResultType` か、結果型を省略した show の登録由来の失敗に限った)
---

## ルール文

デルタスペックの自己レビューで、Requirement の SHALL を 1 つずつ取り上げ、「その SHALL を破った実装 (該当箇所の削除・別の値への置き換え・対象テストの除去) で失敗する Scenario があるか」を確かめる。不在の検索 (旧名が 0 件) や合計値の比較 (全件数が減っていない) だけの Scenario は、削除や別の値への置き換えを見逃すので、存在と中身を名指す形 (差分が置き換えだけ・対象の名前ごとの突き合わせ) に書き直す。守れたかは、各 SHALL に対して、それを破ったときに落ちる THEN が spec に書いてあることから判定する。

## 経緯

- 2026-09-30 rename-current-page-marker: 挙動の変わらない改名だったため「旧名が残らない」で十分と見なし、新しい名前が正しく入ったことを判定する Scenario を書かなかった
- 2026-10-20 guard-kmp-swift-int-result-type: 破れを見逃したのは Scenario ではなく tasks.md の確かめ方だったが型は同じ (SHALL を破った実装で落ちない確かめ)。確かめの結果が、確かめたい処理以外の経路 (指定した型だけで起きる失敗) でも同じになる形だった
