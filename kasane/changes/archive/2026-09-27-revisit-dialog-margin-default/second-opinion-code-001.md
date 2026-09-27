# セカンドオピニオン: revisit-dialog-margin-default (code-001)
**相方**: codex / **label**: so-code-revisit-dialog-margin-default / **日付**: 2026-09-27 / **対象**: HEAD (525fb32) からの作業ツリーの未コミット変更すべて (`ios/`・`android/`・`maui/`・`core/layout-spec/cases.json`・テスト・`samples/` 4 ルート・change 配下)
---
静的レビューの結果、**Critical・Major の指摘はありません**。契約既定値、既定 Toast の余白、4ルートの Sample の値の受け渡しは、仕様および記録済み deviation と整合しています。ビルド・テストは再実行せず、提示されたホスト側の結果を前提にしました。

### 指摘事項

**🟡 Minor — Sample のコメントに過去の既定値が残っている**  
**該当箇所:** `samples/ios/KsDialogsSample/SampleMarginChoice.swift:3`（Android、KMP、MAUI の同等コメントも該当）  
**問題点:** 「24 は旧既定値」という記述は、ソースコメントを現在の仕様で書く `comment-policy.md` に反します。  
**推奨修正:** 「0 は契約の既定値、24 は既定 Toast の余白」のように、現在の意味だけを記述してください。

総合判定: APPROVED

## 突き合わせ結果

ホスト側: review-001.md (APPROVED / Minor 2・Suggestion 2)、verify-001.md (VALID)。

| 指摘 | ホスト | 相方 | 採否 | 対応 |
|---|---|---|---|---|
| Sample 5 ファイルのコメント「24 は旧既定値」が履歴記述 | Minor | Minor | 確定 | 現在の意味だけの記述に修正 |
| テストの実行件数が `evidence/` に無い | Minor | — | ホスト側指摘 | `evidence/test-run-summary.md` を作成 |
| MAUI `DialogOptions.cs` で XML doc と宣言の間に行コメント | Suggestion | — | ホスト側指摘 | 行コメントを XML doc の上へ移動 |
| Android `ToastDefaultContentView` の companion を internal に広げた | Suggestion | — | ホスト側指摘 | クラス外参照なしのため private に戻す |

- 採用 (相方のみ・根拠強): 0 件 / 降格: 0 件 / 未解決: 0 件
