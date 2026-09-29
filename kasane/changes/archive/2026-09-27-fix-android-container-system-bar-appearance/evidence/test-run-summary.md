# テスト実行の件数 (2026-09-27、修正サイクル 3 の後。iOS はその後の iOS 修正サイクルの後)

handbook cross/test-execution.md に従った全件実行の結果。呼び方は「API 31 と API 35 で全件実行、API 29 は対象外」。API 29 でだけ判定できる `DialogSystemBarsTests.PB_SB_04_旧経路でも非表示状態が維持される` は未実行 (各 API で skip)。

端末はこの change 専用に作成したもの (deviation.md の 1 項目め)。

| ビルドルート | 実行環境 | 件数 |
|---|---|---|
| android/ `./gradlew test --rerun-tasks` | JVM | 75 tests / 0 failures |
| android/ instrumented `:ksdialogs-core` | API 35 (ksn_sbfix_api35) | 361 tests / 0 failures / 1 skipped (PB_SB_04) |
| android/ instrumented `:ksdialogs` | API 35 (ksn_sbfix_api35) | 52 tests / 0 failures |
| android/ instrumented `:ksdialogs-core` | API 31 (ksn_sbfix_api31) | 361 tests / 0 failures / 3 skipped (PB_SB_04、API 35 以上に絞った PB_SB_09 のテーマのテスト 2 本) |
| android/ instrumented `:ksdialogs` | API 31 (ksn_sbfix_api31) | 52 tests / 0 failures |
| android/ instrumented システムバー関連 5 クラスのみ | API 36 (ksn_sbfix_api36、CI と同じ API) | 21 tests × 3 回連続 / 0 failures |
| ios/ `xcodebuild test -scheme KsDialogs` | iOS 26.5 シミュレータ (ksn-sbfix-ios) | Swift Testing 321 tests / 0 failures (XCTest は Executed 0) |
| kmp/ `./gradlew allTests --rerun-tasks` | iosSimulatorArm64Test | 85 tests / 0 failures (iOS 修正サイクルの後にも再実行) |
| kmp/ `./gradlew allTests --rerun-tasks` | testAndroidHostTest | 81 tests / 0 failures |
| maui/ `dotnet test` | net10.0 | 197 tests / 0 failures |
| maui/android/native/ `:ksdialogs-maui-bridge:test --rerun-tasks` | JVM | 38 tests / 0 failures |
| maui/macios/native/ `xcodebuild test` | iOS 26.5 シミュレータ (ksn-sbfix-ios) | Swift Testing 9 tests / 0 failures (iOS の本体に Toast の後始末の入口を足した後に再実行) |

追加したテストは結果に現れている: Android は PB_SB_08〜11・PB_SB_09 のテーマのテスト 2 本・LD_SB_01 (×2)・LD_SB_02・TS_SB_01 (×2)・TS_SB_02、iOS は PB_SB_08・LD_SB_01・LD_SB_02・TS_SB_01・TS_SB_02。
