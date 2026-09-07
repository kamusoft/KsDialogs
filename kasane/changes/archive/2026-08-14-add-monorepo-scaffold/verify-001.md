# 検証結果: add-monorepo-scaffold (001 回目)

**日付**: 2026-08-14
**判定**: VALID

デルタスペック `specs/repo-scaffold/spec.md` の全 7 Requirement / 8 Scenario について実装とテストの対応を確認した。❌ は 0 件。

## 対応表

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| **Req: iOS ビルドルートの疎通** | `ios/Package.swift:6-30` (product / target `KsDialogs`)、`ios/Sources/KsDialogs/BuildProbe.swift` | `ios/Tests/KsDialogsTests/BuildProbeTests.swift` | ✅ 一致 |
| └ Scenario: swift test が通る | `ios/Package.swift:22-29` (target + testTarget) | `ios/Tests/KsDialogsTests/BuildProbeTests.swift:5-8` (Swift Testing 1本) | ✅ 一致 (本検証で `swift test` 再実行 — 1 test passed) |
| └ Scenario: iOS Simulator 向けビルドが通る | `ios/Package.swift:9-11` (`platforms: [.iOS(.v17)]`) | ビルド自体が検証手段 (テストコードなし) | ✅ 一致 (本検証で `xcodebuild build -destination 'generic/platform=iOS Simulator'` 再実行 — BUILD SUCCEEDED) |
| **Req: Android ビルドルートの疎通** | `android/settings.gradle.kts:34` (`include(":ksdialogs")`)、`android/ksdialogs/build.gradle.kts:15-22` (com.android.library / namespace / minSdk) | `android/ksdialogs/src/test/kotlin/jp/kamusoft/ksdialogs/BuildProbeTest.kt` | ✅ 一致 |
| └ Scenario: gradle build が通る | `android/ksdialogs/build.gradle.kts:1-45`、`android/gradle/libs.versions.toml` (AGP 9.3.0 / Kotlin 2.4.10 / minSdk 24)、`android/gradle/wrapper/gradle-wrapper.properties` (Gradle 9.7.0) | `BuildProbeTest.kt:8-13` (JUnit 5 / `useJUnitPlatform()` は build.gradle.kts:29-33) | ✅ 一致 (再実行は Android SDK 未設定のため不可。`android/ksdialogs/build/test-results/testDebugUnitTest/TEST-jp.kamusoft.ksdialogs.BuildProbeTest.xml` が `tests="1" failures="0" errors="0"`、`build/outputs/aar/ksdialogs-{debug,release}.aar` 生成済み) |
| **Req: KMP ビルドルートの疎通と composite 接続** | `kmp/settings.gradle.kts:31` (`includeBuild("../android")`)、`kmp/ksdialogs-kmp/build.gradle.kts:16-58` (androidTarget + iosArm64 / iosSimulatorArm64 / iosX64) | `kmp/ksdialogs-kmp/src/commonTest/.../BuildProbeTest.kt` | ✅ 一致 |
| └ Scenario: composite 接続込みでビルドが通る | `kmp/settings.gradle.kts:31`、`kmp/ksdialogs-kmp/build.gradle.kts:52-55` (`implementation("jp.kamusoft:ksdialogs:0.1.0")`)、`kmp/ksdialogs-kmp/src/androidMain/kotlin/jp/kamusoft/ksdialogs/kmp/NativeBridgeProbe.kt` (Native 側シンボルを参照しコンパイル時に置換成立を示す) | 置換成立はコンパイルが検証手段 | ✅ 一致 (`kmp/ksdialogs-kmp/build/classes/kotlin/{android,iosArm64,iosSimulatorArm64,iosX64}` と `build/libs/ksdialogs-kmp-*.jar` が生成済み。Maven に `jp.kamusoft:ksdialogs` の公開は存在しないため、解決成立自体が includeBuild 経由である証拠) |
| └ Scenario: commonTest が Android と iOS シミュレータの両方で回る | `kmp/ksdialogs-kmp/build.gradle.kts:23-26` (host test 有効化)、`:56-58` (kotlin.test) | `kmp/ksdialogs-kmp/src/commonTest/kotlin/jp/kamusoft/ksdialogs/kmp/BuildProbeTest.kt:7-12` (kotlin.test 1本) | ✅ 一致 (`build/test-results/testAndroidHostTest/*.xml` と `build/test-results/iosSimulatorArm64Test/*.xml` がともに `tests="1" failures="0"`。iosX64Test は Apple Silicon 上で SKIPPED、Scenario の要求対象外) |
| **Req: MAUI ビルドルートの疎通** | `maui/KsDialogs.slnx`、`maui/KsDialogs.Maui/KsDialogs.Maui.csproj:4` (`net10.0;net10.0-ios;net10.0-android`) | `maui/KsDialogs.Maui.Tests/BuildProbeTests.cs` | ✅ 一致 |
| └ Scenario: dotnet build / test が通る | `maui/KsDialogs.Maui/KsDialogs.Maui.csproj`、`maui/KsDialogs.Maui.Tests/KsDialogs.Maui.Tests.csproj:4` (net10.0 テストプロジェクト、NUnit 4.6.1) | `maui/KsDialogs.Maui.Tests/BuildProbeTests.cs:10-14` (NUnit 1本) | ✅ 一致 (本検証で `dotnet test` 再実行 — net10.0 / net10.0-ios / net10.0-android の3 TFM ビルド成功、テスト 1 合格 0 失敗) |
| **Req: 公開識別子の適用** | 下記4行に分解 | 各ルートのスモークテストが自分の識別子を assert | ✅ 一致 |
| └ iOS: モジュール / SwiftPM パッケージ名 `KsDialogs` | `ios/Package.swift:7` (`name: "KsDialogs"`)、`:16-18` (library `KsDialogs`)、`:23-24` (target `KsDialogs`) | `BuildProbeTests.swift:8` (`BuildProbe.moduleName == "KsDialogs"`) | ✅ 一致 |
| └ Android: パッケージ `jp.kamusoft.ksdialogs` / Maven `jp.kamusoft:ksdialogs` | `android/ksdialogs/build.gradle.kts:11` (`group = "jp.kamusoft"`)、`:16` (`namespace`)、`android/settings.gradle.kts:34` (project 名 = artifactId `ksdialogs`)、ソースツリー `src/main/kotlin/jp/kamusoft/ksdialogs/` | `BuildProbeTest.kt:12` (`PACKAGE_NAME` を assert) | ✅ 一致 (生成 aar 名 `ksdialogs-{debug,release}.aar` も一致) |
| └ KMP: パッケージ `jp.kamusoft.ksdialogs.kmp` / Maven `jp.kamusoft:ksdialogs-kmp` | `kmp/ksdialogs-kmp/build.gradle.kts:12` (`group`)、`:19` (`namespace`)、`kmp/settings.gradle.kts:33` (project 名 = artifactId `ksdialogs-kmp`)、ソースツリー `.../jp/kamusoft/ksdialogs/kmp/` | `BuildProbeTest.kt:10` (`PACKAGE_NAME` を assert) | ✅ 一致 (生成物 `ksdialogs-kmp-0.1.0.jar` / `ksdialogs-kmp-iosarm64-0.1.0-metadata.jar` 等も一致) |
| └ MAUI: namespace `KsDialogs` / NuGet ID `KsDialogs.Maui` | `maui/KsDialogs.Maui/KsDialogs.Maui.csproj:12-13` (`RootNamespace` / `PackageId`)、`maui/KsDialogs.Maui/BuildProbe.cs:1` (`namespace KsDialogs;`) | `BuildProbeTests.cs:13` (`NamespaceName` を assert) | ✅ 一致 |
| └ Scenario: 識別子が写像表と一致する | 上記4行すべて | — | ✅ 一致 (cross/ADR-0005 の表と全項目突き合わせ済み) |
| **Req: 最低対象 OS の宣言** | 下記4行に分解 | — | ✅ 一致 |
| └ ios: `.iOS(.v17)` | `ios/Package.swift:9-11` | — | ✅ 一致 |
| └ android: minSdk 24 | `android/gradle/libs.versions.toml:8` (`android-minSdk = "24"`)、`android/ksdialogs/build.gradle.kts:21` | — | ✅ 一致 |
| └ kmp: androidTarget minSdk 24 + iOS deployment target 17 | `kmp/ksdialogs-kmp/build.gradle.kts:21` (minSdk)、`:34-49` (`-Xoverride-konan-properties` で `osVersionMin.ios_{arm64,simulator_arm64,x64}=17.0`) | — | ✅ 一致 (`build/bin/iosSimulatorArm64/debugTest/test.kexe` の `LC_BUILD_VERSION` が `minos 17.0` で実測確認。宣言の耐久性は review-001 の Minor 指摘を参照) |
| └ maui: SupportedOSPlatformVersion (ios 17.0 / android 24.0) | `maui/KsDialogs.Maui/KsDialogs.Maui.csproj:18-25` | — | ✅ 一致 |
| └ Scenario: 宣言値が ADR と一致する | 上記4行すべて | — | ✅ 一致 (cross/ADR-0002 の iOS 17 / minSdk 24 と一致) |
| **Req: リポジトリ初期整備** | `README.md` (名前 / コンセプト / Status / 4ビルドルートの表)、`.gitignore` (ルート1本に4形態分を集約)、`LICENSE` (MIT, Copyright (c) 2026 kamusoft) | — | ✅ 一致 |
| └ Scenario: ルートに共通ビルドファイルがない | ルート直下は `.claude` / `.git` / `.gitignore` / `AGENTS.md` / `CLAUDE.md` / `LICENSE` / `README.md` / `android` / `ios` / `kasane` / `kmp` / `maui` / `scripts` のみ | — | ✅ 一致 (`Package.swift` / `settings.gradle.kts` / `*.slnx` はいずれも各ビルドルート配下にのみ存在) |

## 追加検査

### tasks.md の突き合わせ

- チェック済み 15 件はすべて対応表の実装・成果物で裏付けられ、**虚偽チェックなし**。
- **未完了 1 件**: `2.3 swift-tools-version を実物で確定し、結果を cross/ADR-0002 へ追記して残課題を閉じる`。正直に未チェックのまま残されている。実物の `ios/Package.swift:1` は `// swift-tools-version: 6.3` で確定しており (cross/ADR-0002 の推測値 6.2 とは異なる)、ビルド・テストは成功しているため Requirement / Scenario の充足には影響しない。残るのは cross/ADR-0002 側の追記 (kasane/ 文書の更新) のみで、オーナー判断待ちとして本検証の対象外。**アーカイブ前にこのタスクを閉じる必要がある**点だけ記録しておく。

### 逆流検査 (足場アーティファクトの書き換え)

本 change は `kasane/changes/add-monorepo-scaffold/` 一式が未コミット (untracked) のため、git 上の差分では逆流を判定できない。代替として更新時刻を確認した:

| ファイル | 更新時刻 |
|---|---|
| `proposal.md` | 2026-08-14 13:43:50 |
| `specs/repo-scaffold/spec.md` | 2026-08-14 13:44:02 |
| `tasks.md` | 2026-08-14 15:19:57 |
| 実装ファイル (最初: `.gitignore`) | 2026-08-14 15:04:47 |
| 実装ファイル (最後: `maui/.../KsDialogs.Maui.csproj`) | 2026-08-14 15:16:19 |

proposal / spec は実装開始 (15:04) より前で止まっており、**逆流なし**。tasks.md のみ実装後 (15:19) に更新されているが、これはチェックボックスの進捗反映であり足場の書き換えではない。

### 未記録乖離

対応表に ❌ がないため、未記録乖離は 0 件。`deviation.md` は存在せず、記録された合意済み差分もゼロで整合している。

### UI 変更

本 change は UI アーティファクト (`ui/`) を持たない。適用対象外。

### テストの実行確認

| ビルドルート | 実行方法 | 結果 |
|---|---|---|
| ios/ | 本検証で `swift test` を実行 | 1 test passed |
| ios/ | 本検証で `xcodebuild build` (generic/platform=iOS Simulator) を実行 | BUILD SUCCEEDED |
| maui/ | 本検証で `dotnet test` を実行 | 3 TFM ビルド成功 / 合格 1・失敗 0 |
| android/ | 再実行不可 (検証環境に `ANDROID_HOME` / `local.properties` が無く SDK 未解決) | 残存テスト結果 XML で `tests="1" failures="0" errors="0"` を確認 |
| kmp/ | 同上 | `testAndroidHostTest` / `iosSimulatorArm64Test` の XML がともに `tests="1" failures="0"` |

android / kmp を検証者自身の手で再実行できていない点は明記しておく。ただし残存する成果物 (aar / klib / metadata jar / テスト結果 XML) は実装時刻のビルドが実際に完走したことを示しており、失敗を隠した痕跡はない。Android SDK が入った環境で再実行できる状態にするには、README の記載どおり `ANDROID_HOME` を設定するか各ビルドルートに `local.properties` を置く。

## 判定

**VALID** — 全 Requirement / Scenario が「✅ 一致」。虚偽チェックなし、逆流なし、未記録乖離なし、テストは全件成功。

残課題として task 2.3 (cross/ADR-0002 への swift-tools-version 6.3 の追記) が未完了である。デルタスペックの充足には影響しないが、蒸留・アーカイブの前に閉じること。
