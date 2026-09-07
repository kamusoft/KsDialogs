# Swift Task キャンセルでの閉鎖 — 検証の証跡

tasks 7.2 の「Swift Task キャンセルの閉鎖」の確認記録 (2026-08-19)。
対象は kmp-facade デルタスペックの Requirement「呼び出し元キャンセルでの閉鎖 (KMP Swift 面)」。

## 確認方法と、Sample で手動確認できない理由

**Sample アプリの操作では確認できない。** 4ルートの Sample には、表示中のダイアログを待っている
呼び出し元 Task をキャンセルする操作が存在しないため。実際、iOS / KMP iOS の Sample はどちらも
デモ起動を `Task { await ... }` (Button のアクション内の非構造化 Task) で行っており、
画面遷移や離脱でこの Task が取り消されることはない
(`samples/ios/KsDialogsSample/SampleMenuScreen.swift` / `samples/kmp/iosApp/KsDialogsSampleKmp/SampleMenuScreen.swift`、
`SampleLayoutPanelScreen.swift` も同じ)。
キャンセル操作を Sample に足すことは本変更のデルタスペック (samples) の範囲外のため行っていない。

そのため、**Simulator 上で実 UIKit を使って走る自動テストの実行**をもって確認とし、
その実行ログを証跡として残す。ログでは各テストの合否が1件ずつ確認できる。

```
cd ios
xcodebuild test -scheme KsDialogs -destination 'platform=iOS Simulator,name=iPhone 17 Pro' \
  -only-testing:KsDialogsTests/KsDialogsKmpCancellationTests \
  -only-testing:KsDialogsTests/KsDialogsKmpFacadeTests
```

結果: **15 tests / 2 suites / 0 failures** (`xcodebuild-kmp-cancellation.log`)。

## Scenario との対応

Scenario「Swift Task キャンセルで閉じる」(当該ダイアログだけが閉じ、cancelled が1回だけ確定する) に
対応するテストは次の4件。実装は `ios/Tests/KsDialogsTests/KsDialogsKmpCancellationTests.swift`。

| テスト名 | 確認していること |
|---|---|
| 呼び出し元 Task のキャンセルでダイアログが閉じ cancelled が返る | 取り消しで表示が残らず、結果が cancelled になる |
| キャンセル後の結果報告は無効で、結果は1回だけ確定する | 確定後の complete / cancel が何も起こさない (ちょうど1回) |
| キャンセルは当該ダイアログだけを閉じ、他の表示中ダイアログは残る | 他の表示中ダイアログが影響を受けない |
| 提示が始まる前のキャンセルでも表示が残らない | 提示開始前の取り消しでも表示が残らない |

## 未確認として残ること

Kotlin 側の `IosDialogGateway` (KMP 共有コードから suspend で show する経路) は、
呼び出し元キャンセルでも表示が残る既存挙動のままである。本変更のデルタスペックは KMP の
**Swift 公開面**に限定されているためスコープ外だが、core 契約への適合という観点では追随の候補が残る
(→ `handoff-distill.md` に申し送り済み)。

## ファイル

| ファイル | 内容 |
|---|---|
| `xcodebuild-kmp-cancellation.log` | 上記コマンドの実行ログ (結果部分の抜粋) |
