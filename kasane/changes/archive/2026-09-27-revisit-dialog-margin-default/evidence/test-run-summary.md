# テスト実行の記録: revisit-dialog-margin-default

`kasane/handbook/cross/test-execution.md` の全件実行コマンドで回した件数 (2026-09-27)。実装の完了時 (tasks 4.6) と、独立レビュー (review-001) での再実行の 2 回分。

## 実装完了時 (tasks 4.6)

| ルート | 件数 | 結果 |
|---|---|---|
| ios/ (`xcodebuild test`、新規作成の iPhone 17 Simulator) | Swift Testing 321 | 全件成功 (XCTest 側は 0 件) |
| android/ unit (`./gradlew test --rerun-tasks`) | 75 | 失敗 0 |
| android/ instrumented `:ksdialogs-core` (API 31 emulator) | 356 | 失敗 5・skip 1 (下記) |
| android/ instrumented `:ksdialogs` (Compose、API 31 emulator) | 52 | 失敗 0 |
| kmp/ (`./gradlew allTests --rerun-tasks`) | 166 (iosSimulatorArm64Test 85 + testAndroidHostTest 81) | 失敗 0 |
| maui/android/native/ (`:ksdialogs-maui-bridge:test`) | 38 | 失敗 0 |
| maui/macios/native/ (bridge の `xcodebuild test`) | Swift Testing 9 | 全件成功 |
| maui/ (`dotnet test`) | 197 | 全件合格 |

- PlacementHost は `dotnet test` の対象外のため、net10.0-ios / net10.0-android のビルドのみ確認した
- instrumented の失敗 5 件 (`DialogTransparentOverlayTests` 2 件・`ToastMultiDisplayTests` TS_MX_01 / TS_MX_05・`ToastSystemInputTests.Toast_表示中でも戻るとホームが通る`) は UiAutomation の接続例外 (`already registered!` / `Not connected!`) で判定まで届いていない。変更前のコード (HEAD を別の場所へ書き出したもの) でも同じ例外で落ちる。他のツールが UiAutomation を握っている共用端末に固有の環境要因 (add-page-layout-area の証跡で確定済み)。下のレビュー時の再実行 (専用 AVD) では 0 件
- この change で足したテスト (TS-AT-04〜07、既定ローディング・カスタム Loading・カスタム Toast の末尾寄せの検査) は iOS / Android の結果に現れている。余白の添付を一時的に外すと、iOS は TS-AT-01・04・05・06、Android は TS-AT-04・05・06 が失敗することを確かめた (確認後に元へ戻した)
- `scripts/scenario-id-coverage.py` (通常・`--require-mirror`): 未網羅なし

## 独立レビューでの再実行 (review-001)

| ルート | 件数 | 結果 |
|---|---|---|
| ios/ | 321 | 3 件が負荷時の時間切れ (差分外の Toast スイート)。単独の再実行で 23/23 成功。変更したテストはすべて成功 |
| android/ unit | 75 | 失敗 0 |
| android/ instrumented `:ksdialogs-core` (新規起動の API 35 AVD) | 356 | 失敗 0・skip 1 |
| android/ instrumented `:ksdialogs` (Compose) | 52 | 1 回目 52 tests / 2 failures (起動直後のランチャーの ANR ダイアログが前面にあり入力の注入が失敗)、ANR を閉じた 2 回目 52 tests / 1 failure (`ComposeCurrentPageTests` の最初の 1 件が配置の落ち着く前に 10 秒で時間切れ。1 回目では成功)。`ComposeCurrentPageTests` の単独の再実行で 7 tests / 0 failures |
| kmp/ | 85 + 81 | 失敗 0 |
| maui/ `dotnet test` | 197 | 失敗 0 |
| maui/ bridge Android / iOS | 38 / 9 | 失敗 0 |

- `scripts/scenario-id-coverage.py`: 未網羅なし

## Sample (tasks 5)

Sample にテストは無い。4 ルートのビルド (iOS / KMP iOS の `xcodebuild`、`samples/android` と `samples/kmp` の `:app:assembleDebug` / `:androidApp:assembleDebug`、MAUI の net10.0-ios / net10.0-android) が通ることと、視覚照合・読み上げ (`ui/verification/`) で確認した。
