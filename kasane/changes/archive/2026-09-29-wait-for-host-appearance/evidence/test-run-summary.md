# 全形態のテスト実行の要約 (wait-for-host-appearance、tasks 9.2)

- 実行日: 2026-09-28。手順は `kasane/handbook/cross/test-execution.md`
- 端末はこの実行のために新規作成した専用のもので、実行後に削除した。起動中・既存の Simulator / AVD / 実機は使っていない
  - iOS: `xcrun simctl create` で作った Simulator (iPhone 17 / iOS 26.5)。`-destination 'id=<専用 Simulator>'` で指定
  - Android: `avdmanager create avd` で作った AVD (Pixel 6 / API 36 google_apis arm64-v8a)。adb は `-s <専用 AVD>`、Gradle は `ANDROID_SERIAL=<専用 AVD>` で絞った
- Xcode 26.5。Android の Gradle 系は逐次で回した (android/ と maui/android/native・kmp の複合ビルドを同時に走らせない)
- 失敗は 0 件。間欠的な失敗も観測していない (各実行 1 回)

## 件数

| 形態 | コマンドの形 | 件数 | 失敗 | skip |
|---|---|---|---|---|
| iOS (既定の並列) | `ios/` で `xcodebuild test -scheme KsDialogs -destination 'id=<専用 Simulator>'` | Swift Testing 343 (58 suites) + XCTest 0 = 343 | 0 | 0 |
| iOS (直列) | 同上に `-parallel-testing-enabled NO` | Swift Testing 343 (58 suites) + XCTest 0 = 343 | 0 | 0 |
| Android unit | `android/` で `./gradlew test --rerun-tasks` | 88 (`ksdialogs-core` の testDebugUnitTest) | 0 | 0 |
| Android instrumented | `android/` で `./gradlew connectedDebugAndroidTest` | 410 (`ksdialogs-core` 358 + `ksdialogs` 52) | 0 | 1 |
| KMP iosTest | `kmp/` で `./gradlew --init-script <専用 Simulator へ向ける init script> allTests --rerun-tasks` | iosSimulatorArm64Test 86 | 0 | 0 |
| KMP androidHostTest | 同上 | testAndroidHostTest 83 | 0 | 0 |
| MAUI | `maui/` で `dotnet test` | 206 (`KsDialogs.Maui.Tests`) | 0 | 0 |
| MAUI iOS ブリッジ | `maui/macios/native` で `xcodebuild test -project KsDialogsMauiBridge.xcodeproj -scheme KsDialogsMauiBridge -destination 'id=<専用 Simulator>'` | Swift Testing 14 (6 suites) + XCTest 0 = 14 | 0 | 0 |
| MAUI Android ブリッジ | `maui/android/native` で `./gradlew :ksdialogs-maui-bridge:test --rerun-tasks` | 41 | 0 | 0 |
| MAUI 実配置テストホスト iOS | `maui/KsDialogs.Maui.PlacementHost` で `dotnet build -f net10.0-ios -p:RuntimeIdentifier=iossimulator-arm64` → `xcrun simctl install` → `xcrun simctl launch --console-pty` | `KSDPLACEMENT\|iOS\|SUMMARY\|passed=16\|failed=0` | 0 | - |
| MAUI 実配置テストホスト Android | `dotnet build -f net10.0-android -p:EmbedAssembliesIntoApk=true` → `adb -s <専用 AVD> install -r` → `am start` | `KSDPLACEMENT\|Android\|SUMMARY\|passed=17\|failed=0` | 0 | - |

- Android instrumented の skip 1 件は `DialogSystemBarsTests.PB_SB_04_旧経路でも非表示状態が維持される` (API 30 以上の端末では旧経路を判定できないため自分を skip する。既知)
- KMP の init script は evidence/kmp-test-run.md と同じ形 (`iosSimulatorArm64Test` の `device` に専用 Simulator を入れる)。ビルド定義は変えていない

## 今回の Scenario ID の出現 (すべて成功)

### iOS (並列・直列とも同じ出現)

| 領域 | 出現した ID | テスト数 |
|---|---|---|
| PB-HW | PB-HW-01〜08 | 10 (PB-HW-06 が 3 本) |
| PB-HI | PB-HI-01〜04 | 4 |
| LD-HW | LD-HW-01〜07 | 7 |
| TS-HW | TS-HW-01〜03 | 3 |

### Android unit (`android/ksdialogs-core/src/test`)

| 領域 | 出現した ID | テスト数 |
|---|---|---|
| PB-HW | PB-HW-01〜08 | 8 |
| PB-HA | PB-HA-01〜03 | 3 |
| その他 | DM-AN-01 | 1 |

### Android instrumented (`android/*/src/androidTest`)

| 領域 | 出現した ID | テスト数 |
|---|---|---|
| PB-HW | PB-HW-06 (実ウィンドウ版。`DialogHostWaitPresentationTests.kt`) | 1 |
| LD-HW | LD-HW-01〜07 | 7 |
| TS-HW | TS-HW-01〜03 | 3 |
| その他 | LD-HA-02 (`ksdialogs` の `ComposeLoadingActionThreadTests.kt`) | 1 |

- PB-HW-06 は unit と instrumented の両方に 1 本ずつあり、合わせて 2 本

### KMP

- iosSimulatorArm64Test / testAndroidHostTest の合計で、DM-KM-01〜03、PB-KC-04 (両ターゲット)、PB-KT-03〜09・PB-KT-13・PB-KT-14、LD-KT-02、TS-KT-01、TS-KM-02、LD-KM-03 が出現

### MAUI

- `dotnet test`: PB-MC-02・03・04・08、PB-MH-01、MB-MA-16、DM-MA-01・02。PB-MC-01 は公開面のコンパイル検査 (`KsDialogs.Maui.ApiSurfaceCheck`) で、`dotnet test` のビルドに含まれる
- iOS ブリッジ: PB-MC-06 (2 本)、PB-MC-07、BV-MA-01〜03・07、DM-MA-03
- Android ブリッジ: PB-MC-05、PB-MC-07、MB-MA-15 (2 本)
- PB-MH-02 はテストを持たない (到達させる手段が無い防御。`scripts/scenario-id-coverage.py` の除外表に登録)

## 仕様とテストの対応の検査

- `python3 scripts/scenario-id-coverage.py --require-mirror`: 未網羅なし。両 Native ミラー (PB-HW・LD-HW・TS-HW を含む) も OK
