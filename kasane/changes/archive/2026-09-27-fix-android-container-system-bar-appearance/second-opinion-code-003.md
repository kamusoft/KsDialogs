# セカンドオピニオン: fix-android-container-system-bar-appearance (code-003)
**相方**: codex / **label**: so-code-003-fix-android-container-system-bar-appearance / **日付**: 2026-09-27 / **対象**: HEAD (d1396b0) に対する未コミットの作業ツリー (修正サイクル 3 の後)
---
12:- Minor: ToastSystemBarsTests.kt で changeHost(null) は器を外すが、表示とタイマーは延長した 62 秒の期限まで残り、終了済みテストの Activity を中身の View 経由で保持し得る。テストの終了処理で表示とタイマーも破棄し displayCount == 0 を確認すべき
16:  - Android instrumented (専用 AVD): API 35 :ksdialogs-core 361 tests / 0 failures / 1 skipped (PB_SB_04、API 29 専用)、:ksdialogs 52 / 0。API 31 :ksdialogs-core 361 / 0 / 3 skipped (PB_SB_04 と API 35 以上に絞ったテスト 2 件)、:ksdialogs 52 / 0。API 36 (CI と同じ API) でシステムバー関連 5 クラス 21 件 × 3 回連続成功
前回の指摘は解消されています。[ToastSystemBarsTests.kt](android/ksdialogs-core/src/androidTest/kotlin/jp/kamusoft/ksdialogs/ToastSystemBarsTests.kt:250) は `finally` で表示とタイマーを破棄し、`displayCount == 0` を確認しています。

### 🟡 Minor: iOS Toast テストの終了処理が表示を残す

**該当箇所**: [ToastStatusBarAppearanceTests.swift](ios/Tests/KsDialogsTests/ToastStatusBarAppearanceTests.swift:82)  
**問題点**: 器だけを `dismiss()` しており、coordinator の表示件数と期限タスクは残ります。タスクは表示と中身を最大30秒保持します。  
**推奨修正**: 観測後と失敗時に coordinator 側の表示・計時を破棄し、`displayCount == 0` を確認してください。

その他、確認した差分に Critical / Major は見つかりませんでした。ビルド・テストは依頼どおり再実行せず、提示された実行結果を前提に判定しました。

**判定: APPROVED**


## 突き合わせ結果

ホスト側レビュー: review-003.md (APPROVED / 一致検証 VALID、Suggestion 1: 受け入れる取りこぼしを仕組みの形で書く → deviation.md に反映、ADR-0039 の Consequences は蒸留へ申し送り)

| 指摘 | 出典 | 採否 | 理由 |
|---|---|---|---|
| Android テストの Toast 片付けの解消 | 相方 | 確定 | ホスト側も解消を確認 |
| iOS Toast テストの終了処理が coordinator の表示と期限タスクを最大 30 秒残す (Minor) | 相方のみ | **採用** (オーナー判断で修正、2026-09-27) | Android で直した指摘の iOS 側の姉妹面で根拠はある。直すには iOS 本体 (coordinator) に後始末の口を足す変更が要り、レビュー上限 (3 周) に達していたため、オーナーに諮って修正を選んだ (上限を超える 4 周目のレビューを含めて了承) |

件数: 確定 1 / 採用 1 / 降格 0 / 未解決 0
