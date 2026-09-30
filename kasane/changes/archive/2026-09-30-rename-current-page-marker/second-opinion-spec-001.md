# セカンドオピニオン: rename-current-page-marker (spec-001)
**相方**: codex / **label**: so-spec-rename-current-page-marker / **日付**: 2026-09-30 / **対象**: 提案一式 (kasane/changes/rename-current-page-marker/ の proposal.md・specs/ios-native・specs/android-native・specs/samples・tasks.md)
---
# 提案レビュー: rename-current-page-marker

**判定: NEEDS_DISCUSSION**  
静的レビューです。ビルド・テストは実行していません。実装前に、次の受け入れ方法を提案側で確定する必要があります。

### 🟠 Major — iOS のテスト件数比較が対象スイートの欠落を検出できない

**該当箇所:** `kasane/changes/rename-current-page-marker/tasks.md:10`  
**問題点:** 改名前に数えるのは `DialogCurrentPageSwiftUITests` の宣言数ですが、確認に指定された Swift Testing の件数行は全スイートの合計です。対象スイートのテストが減っても、合計件数を基準値と比較するだけでは `specs/ios-native/spec.md:26` を判定できません。  
**推奨修正:** テスト結果から対象スイートの実行済みテスト名を列挙し、改名前の各テストが改名後も実行されたことを受け入れ条件にしてください。対象スイートを丸ごと外せば、この確認が失敗する形にする必要があります。

### 🟠 Major — 診断文言と Compose の表示名の更新を判定できない

**該当箇所:** `kasane/changes/rename-current-page-marker/specs/ios-native/spec.md:16`、`specs/android-native/spec.md:16`  
**問題点:** 両仕様は診断文言の追随を、Android は `InspectorInfo.name` の追随も要求します。一方、Scenario は旧名の残存検索だけです。文言や表示名を削除したり別の名前にしたりしても、検索・公開面コンパイル・既存の挙動テストは通ります。  
**推奨修正:** 対象の診断箇所に新名が現れ、文型と英語が保たれること、`InspectorInfo.name` が `markAsDialogCurrentPage` に一致することを明示的な受け入れ条件にしてください。各箇所を削除した場合に失敗する確認が必要です。

改名の対象範囲、4 ルートの Sample、蒸留への申し送りに矛盾は見つかりませんでした。上記は実装中に凍結された spec を補う形では解消できないため、提案段階での修正を求めます。

## 突き合わせ結果

突き合わせの相手はホスト側の自己レビュー (ksn-propose Step 8、2 周・新たな問題なし)。相方の 2 件はどちらもホスト側が見逃していたもの。

| # | 指摘 | 採否 | 反映 |
|---|---|---|---|
| 1 | iOS のテスト件数比較が全スイートの合計になり、対象スイートの欠落を検出できない | 採用 (相方のみ + 根拠強: tasks.md の該当箇所が特定され、Scenario を判定できない実害がある) | specs/ios-native・specs/android-native の「挙動のテストが名前の追随だけで通る」を、改名前に列挙したテスト名が 1 件も欠けずに成功として現れる形へ。tasks.md 0.1 (件数 → テスト名の一覧)・1.4・2.4 (テストごとの結果との突き合わせ) |
| 2 | 診断文言と Compose の表示名の追随が、旧名の残存検索だけでは判定できない (削除・別名でも通る) | 採用 (相方のみ + 根拠強: 両 spec の該当 Requirement が特定され、SHALL を判定できない) | specs/ios-native・specs/android-native に Scenario「差分は旧名から新名への置き換えだけ」を追加 (削除だけの行が無いこと、診断メッセージが同じ英文のまま新名を名指すこと、Android の表示名が新名であること)。tasks.md 1.5・2.5 |

確定 0 件 / 採用 2 件 / 降格 0 件 / 未解決 0 件。相方の判定 NEEDS_DISCUSSION は受け入れ方法の未確定を理由とするもので、上の反映で解消した (設計判断・オーナー判断を要する論点は無し)。
