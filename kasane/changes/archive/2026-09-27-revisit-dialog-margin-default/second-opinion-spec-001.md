# セカンドオピニオン: revisit-dialog-margin-default (spec-001)
**相方**: codex / **label**: so-spec-revisit-dialog-margin-default / **日付**: 2026-09-27 / **対象**: 提案一式 (kasane/changes/revisit-dialog-margin-default の proposal.md / specs/ / tasks.md / ui/brief.md / ui/mock/mock-a.html、関連 ADR kasane/decisions/core/0039-dialog-margin-default-zero.md)
---
## 指摘事項

### 🟠 Major: Android の既存テストが更新対象から漏れている

**該当箇所**: `tasks.md:22-24`  
**問題点**: Android の `DialogAttributeSupplyTests.kt:117-139` は、NaN の左余白が既定値 24 に戻る前提で `x=72` を期待しています。既定値を 0 にすると `x=60` になるため、このテストは失敗します。tasks は同種の iOS テストだけを明記しています。  
**推奨修正**: Android の当該テストと説明コメントも更新対象に加え、期待座標を再導出してください。

### 🟠 Major: 長文 Toast の Scenario では余白 24 の添付漏れを検出できない

**該当箇所**: `specs/dialog-contract/spec.md:70-73`、`tasks.md:25`  
**問題点**: 既定 Toast は両 Native とも、余白とは別に最大幅を表示先の 80% に制限しています（`ToastDefaultContentView.swift:91-102`、`ToastDefaultContentView.kt:86-98`）。幅 400 の画面なら、余白の添付を忘れても左右に 40 ずつ空き、Scenario の「24 内側に収まる」を満たします。  
**推奨修正**: 添付された実効余白が全辺 24 であることを直接検査するか、80% の幅制限だけでは条件を満たせない画面幅を指定してください。

### 🟡 Minor: Sample の選択肢 `48` に表示結果の受け入れ条件がない

**該当箇所**: `specs/samples/spec.md:14-22`、`ui/brief.md:21`、`tasks.md:33`  
**問題点**: 選択肢は `0 / 24 / 48` ですが、位置の Scenario と撮影対象は `0 / 24` だけです。`48` を選べても表示に 24 が渡る誤実装を、現行の受け入れ条件では判別できません。  
**推奨修正**: `48` を選んだ後、Dialog の端が基準領域から 48 内側になる Scenario を加えてください。

Toast の添付値を読む経路と、ケース表 C19・C23・C24・C26 の提案座標は、確認したコード・テストおよびレイアウト規則と整合しています。静的レビューのため、ビルド・テストは実行していません。

総合判定: **CHANGES_REQUESTED**


## 突き合わせ結果

突き合わせの相手はホスト側の自己レビュー (ksn-propose Step 8 の 2 周)。自己レビューはこの 3 件をいずれも挙げていない。

| # | 指摘 | 採否 | 根拠と反映 |
|---|---|---|---|
| 1 | Android の `DialogAttributeSupplyTests.kt` が更新対象から漏れている (Major) | 採用 | 相方のみ + 根拠強。`android/ksdialogs-core/src/androidTest/kotlin/jp/kamusoft/ksdialogs/DialogAttributeSupplyTests.kt:117-139` が左辺 NaN → 既定 24 の前提で x=72 を期待していることを確認。tasks 4.2 に追加 (x=72 → 60) |
| 2 | 長文 Toast の Scenario (TS-AT-05) では余白 24 の添付漏れを検出できない (Major) | 採用 | 相方のみ + 根拠強。デフォルト View は取り付け先の幅に対する比率で最大幅を制限している (`ios/Sources/KsDialogs/Presentation/ToastDefaultContentView.swift` の `didMoveToSuperview`、`android/ksdialogs-core/src/main/kotlin/jp/kamusoft/ksdialogs/ToastDefaultContentView.kt` の `applyMaxWidthFromHost`) ことを確認。TS-AT-05 の GIVEN を「最大幅の制限よりも余白の制約が狭くなる幅の可視領域」に改め、THEN を「ちょうど 24 内側」にした。tasks 4.5 に測り方を追記 |
| 3 | Sample の選択肢 `48` に表示結果の受け入れ条件がない (Minor) | 降格 | 相方のみ、Minor で誤実装の実害シナリオが推測の域。提案は変えない (撮影対象はモック承認の 3 状態のまま) |

確定 0 件 / 採用 2 件 / 降格 1 件 / 未解決 0 件。
