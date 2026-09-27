# セカンドオピニオン: fix-android-container-system-bar-appearance (code-004)
**相方**: codex / **label**: so-code-004-fix-android-container-system-bar-appearance / **日付**: 2026-09-27 / **対象**: HEAD (d1396b0) に対する未コミットの作業ツリー (iOS 修正サイクルの後)
---
1:# 独立コードレビュー依頼 (iOS 修正サイクルの後の再レビュー)
19:  - iOS (`xcodebuild test -scheme KsDialogs`): 321 tests / 0 failures (iOS 修正の後)
20:  - MAUI 互換面 iOS: 9 / 0、kmp allTests: 85 + 81 / 0 (iOS 修正の後)
### 指摘事項

**Minor — 失敗時の後始末を確認していない**  
該当箇所: `ios/Tests/KsDialogsTests/ToastStatusBarAppearanceTests.swift:64`

前回指摘した器・表示・期限タスクの破棄は `discardAll()` で対応されています。ただし器の取り付け待ちが失敗して `#require` が例外を投げた場合、`defer` は破棄を行うだけで、`displayCount == 0` を確認しません。失敗経路でも破棄直後の件数を検証してください。

差分全体と未追跡ファイルを静的に確認しました。ビルド・テストは実行せず、提示された実行結果を確認しました。Critical・Major の指摘はありません。

**判定: APPROVED**


## 突き合わせ結果

ホスト側レビュー: review-004.md (APPROVED / 一致検証 VALID、新しい指摘 0 件)

| 指摘 | 出典 | 採否 | 理由 |
|---|---|---|---|
| iOS Toast テストの表示・計時の破棄の解消 | 相方 | 確定 | ホスト側も解消を確認 (1 行と discardAll() を外すとそれぞれ確認が落ちることまで照合) |
| 取り付け待ちの `#require` で打ち切られた失敗経路で、`defer` の破棄の後に `displayCount == 0` を確かめていない (Minor) | 相方のみ | **降格** | その経路ではテストは既に失敗しており、破棄後の件数を確かめても検出できる退行は増えない。破棄そのものは `defer` で行われ、成功経路では件数と器の取り外しを確かめている |

件数: 確定 1 / 採用 0 / 降格 1 / 未解決 0
