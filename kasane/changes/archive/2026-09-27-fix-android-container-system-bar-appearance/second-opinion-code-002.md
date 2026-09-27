# セカンドオピニオン: fix-android-container-system-bar-appearance (code-002)
**相方**: codex / **label**: so-code-002-fix-android-container-system-bar-appearance / **日付**: 2026-09-27 / **対象**: HEAD (d1396b0) に対する未コミットの作業ツリー (修正サイクル後)
---
前回の **Toast の表示期限が観測時間より短い** という Major 指摘は解消されています。[Android のテスト](android/ksdialogs-core/src/androidTest/kotlin/jp/kamusoft/ksdialogs/ToastSystemBarsTests.kt:275) は待機・観測時間を見込んだ 62 秒、[iOS のテスト](ios/Tests/KsDialogsTests/ToastStatusBarAppearanceTests.swift:17) は待機上限の合計 10 秒に対して 30 秒を設定し、両方とも判定時に表示継続を確認しています。

### 指摘事項

**Minor — Android テストの撤去後も Toast の計時が続く**  
**該当箇所**: `android/ksdialogs-core/src/androidTest/kotlin/jp/kamusoft/ksdialogs/ToastSystemBarsTests.kt:246-249`  
**問題点**: `changeHost(null)` は器を外しますが、表示自体は期限まで保持されます。延長した 62 秒の間、終了済みテストの Activity を中身の View 経由で保持し得ます。  
**推奨修正**: テスト用の終了処理で表示とタイマーも破棄し、`displayCount == 0` を確認してください。

指定された範囲の静的レビューでは、ほかに Critical / Major は見つかりませんでした。ビルド・テストは実行しておらず、Android JVM・KMP・MAUI と互換面の再実行結果は判定材料に含めていません。

**判定: APPROVED**


## 突き合わせ結果

ホスト側レビュー: review-002.md (APPROVED / 一致検証 VALID、Minor 1: API 35 以上で旧来のフラグの代入を取りこぼす → オーナー判断で修正)

| 指摘 | 出典 | 採否 | 理由 |
|---|---|---|---|
| 前回の Toast 表示期限の競合が解消 | 相方 | 確定 | ホスト側も解消を確認 |
| Android テストの撤去後も Toast の表示と計時が 62 秒残り、終了済みテストの Activity を握り得る (Minor) | 相方のみ | **採用** | 該当箇所と実害 (延長した期限の間の保持) が特定されている。修正サイクル 3 で対応 |

件数: 確定 1 / 採用 1 / 降格 0 / 未解決 0
