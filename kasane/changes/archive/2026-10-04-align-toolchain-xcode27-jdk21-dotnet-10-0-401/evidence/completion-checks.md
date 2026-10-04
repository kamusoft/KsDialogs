# 完了条件の実測結果 (実装、2026-10-04)

環境: Xcode 27.0 (27A266a) / iOS 27.0 Simulator (作業専用に作った iPhone 17。実測後に削除)、JDK 21.0.12.1、.NET SDK 10.0.401 / workload set 10.0.401.1、Kotlin 2.4.20。リポジトリ本体の作業ツリーで実測した。ログの全文は手元保管。

## テスト

| 実行 | 結果 |
|---|---|
| `android/` の `./gradlew test --rerun-tasks` | 109 tests / 0 failures (結果 XML の合算) |
| `kmp/` の `./gradlew allTests compileCommonMainKotlinMetadata compileIosMainKotlinMetadata --rerun-tasks` | `iosSimulatorArm64Test` 86 tests / 0 failures、`testAndroidHostTest` 83 tests / 0 failures。Swift パッケージの取り込み (`convertSyntheticImportProjectIntoDefFileIphonesimulator` / `Iphoneos`、`:api-surface-check` の同名タスク) が成功 |
| `maui/android/native/` の `./gradlew :ksdialogs-maui-bridge:test --rerun-tasks` | 41 tests / 0 failures |
| `ios/` の `xcodebuild test` (`-parallel-testing-enabled NO -collect-test-diagnostics never`) | `Test run with 372 tests in 61 suites passed` / `Executed 0 tests` (XCTest 側) |
| `maui/` の `dotnet test` | 合計 206 / 失敗 0 |
| `maui/macios/native/` の `xcodebuild test` (同じ 2 指定) | `Test run with 17 tests in 7 suites passed` / `Executed 0 tests` |

## ビルド

| 対象 | 結果 |
|---|---|
| iOS binding (Release) | 成功、警告 0 |
| Android binding (Release) | 成功、警告 30 (BG8605 ほか binding 生成器の警告。変更前の CI の同じ step も 30 件で、種類別の件数も同じ) |
| facade `net10.0-ios` / `net10.0-android` (Release) | 成功、警告 0 |
| Sample: `samples/ios` | BUILD SUCCEEDED |
| Sample: `samples/android` (`:app:assembleDebug`) | BUILD SUCCESSFUL |
| Sample: `samples/maui` (`net10.0-ios` / `net10.0-android`) | どちらも成功、警告 0 |
| Sample: `samples/kmp` (`:androidApp:assembleDebug`) | BUILD SUCCESSFUL |
| Sample: `samples/kmp/iosApp` | 1 回目は失敗、2 回目は BUILD SUCCEEDED (下の「合成 package の差分」) |
| 消費者検証 4 本 (`verification/<platform>/build-consumer.sh`、dry-run) | 4 本とも exit 0。MAUI は XA4301 検出なし |

MAUI 本体は 10.0.20 のまま。版を書かない消費者検証アプリは `Microsoft.Maui.Controls` 10.0.110 (workload set 同梱版) を解決してビルドが通った。

## 配布物の対象

| 確かめたこと | 結果 |
|---|---|
| 生成 class のバイトコード版 | `android/` 3 モジュール・`maui/android/native/` の橋渡し・`samples/android` の全 class が major version 55。release aar 3 本 (`ksdialogs-core` / `ksdialogs` / `ksdialogs-maui-bridge`) の `classes.jar` も 55 |
| klib の manifest | 消費者検証が発行した `ksdialogs-kmp` の iosArm64 / iosSimulatorArm64 / iosX64 の klib と cinterop klib が `abi_version=2.4.0` (`metadata_version=2.4.0`、`compiler_version=2.4.20`) |
| nupkg の TFM group | `KsDialogs.Maui`: `net10.0` / `net10.0-android36.0` / `net10.0-ios26.0`。`KsDialogs.Binding.Android`: `net10.0-android36.0`。`KsDialogs.Binding.iOS`: `net10.0-ios26.0`。変更前の記述 (`kasane/concepts/cross/architecture/distribution-artifacts.md`) と同じ |
| nupkg の依存 | `Microsoft.Maui.Controls` 10.0.20、`Xamarin.Kotlin.StdLib` 2.4.0.1、`Xamarin.KotlinX.Coroutines.Android` 1.11.0.1 |
| 自 assembly 用 aar | `KsDialogs.Binding.Android` の nupkg の aar は `ksdialogs-core-release.aar` と `ksdialogs-maui-bridge-release.aar` の 2 件だけ。自 assembly 用 aar は Release 出力にも生成されておらず、除去の後処理は動く機会が無かった (変更前と同じ「発生していない」状態) |

## 合成 package の差分 (Kotlin 2.4.20)

Kotlin 2.4.20 は、KMP の合成 Swift package の `include/` に空の `module.modulemap` を新しく書く。追跡済みの合成 package に、未追跡のファイルが 6 件できた。

- `kmp/.swiftpm-locks/default/swiftImport/` 配下に 4 件 (`kmp/` の `allTests` で生成。ビルドは 1 回目から通る)
- `samples/kmp/iosApp/KotlinMultiplatformLinkedPackage/` 配下に 2 件

`samples/kmp/iosApp` の 1 回目のビルドは、生成タスクが差分を検出して止めた:

```
> Task :shared:generateSyntheticLinkageSwiftPMImportProjectForEmbedAndSignLinkage FAILED
error: Synthetic project regenerated
error: Please go to File -> Package -> Resolve Package Versions in Xcode
error: Synthetic linkage package files changed during the build:
error:   + Sources/KotlinMultiplatformLinkedPackage/include/module.modulemap (added)
error:   + subpackages/_ksdialogs_kmp/Sources/_ksdialogs_kmp/include/module.modulemap (added)
```

ファイルができた後の 2 回目は通る。`verification/` には差分が出ていない。

## 未検証

- develop への push での lint と 5 形態の本体検証 (`xcode-27` イメージでの Xcode 選択を含む)
- 消費者検証とリリースの CI 経路 (`main` 宛ての pull request とリリースの dry-run でしか走らない)
- Android の instrumented テスト、負のコンパイル検証、Sample と実配置テストホストの起動確認

## develop への push での CI (2026-10-04)

コミット `4449c6a` を develop へ push した CI (https://github.com/kamusoft/KsDialogs/actions/runs/37167099986) の結果。

| job | 結果 |
|---|---|
| lint | 成功 |
| ios / verify | 成功。`xcode-27` イメージで Xcode 27.0 (27A266a) が選ばれ、372 tests in 61 suites passed |
| android / verify | 成功 |
| android-instrumented / verify | 成功 (API 36。手元では回していなかった分) |
| kmp / verify | 成功。Xcode 27.0 (27A266a)。新規 checkout で Swift パッケージの取り込みが通った |
| maui / verify | 成功。Xcode 27.0 (27A266a)。iOS 橋渡しは 17 tests in 7 suites passed |
| consumer-ios / consumer-android / consumer-maui / consumer-kmp | skipped (pull request でだけ走る) |

消費者検証とリリースの経路は、`main` 宛ての pull request とリリースの dry-run まで未検証のまま。
