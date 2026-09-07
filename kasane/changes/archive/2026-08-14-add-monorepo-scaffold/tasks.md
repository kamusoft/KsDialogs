# Tasks: add-monorepo-scaffold

## 1. リポジトリ初期整備

- [x] 1.1 .gitignore をルートに作成 (Xcode / Gradle / KMP / .NET の4形態分を集約) (→ Requirement: リポジトリ初期整備)
- [x] 1.2 LICENSE を作成 (MIT, (c) kamusoft) (→ Requirement: リポジトリ初期整備)
- [x] 1.3 README.md を作成 (英語主・最小限: 名前・コンセプト1段落・開発中ステータス・4ビルドルートの地図) (→ Requirement: リポジトリ初期整備)

## 2. ios/ ビルドルート

- [x] 2.1 Package.swift + ライブラリターゲット `KsDialogs` (空ソース1枚) を作成 (→ Requirement: iOS ビルドルートの疎通 / 公開識別子の適用)
- [x] 2.2 Swift Testing のスモークテスト1本を追加し `swift test` を通す (→ Scenario: swift test が通る)
- [x] 2.3 swift-tools-version を実物で確定し、結果を cross/ADR-0002 へ追記して残課題を閉じる — 確定値 6.3、実測 Xcode 26.5 での確定はオーナー承認済み (→ Requirement: iOS ビルドルートの疎通)
- [x] 2.4 `xcodebuild build` (destination: iOS Simulator) で iOS SDK 向けビルドを通す (→ Scenario: iOS Simulator 向けビルドが通る)

## 3. android/ ビルドルート

- [x] 3.1 settings.gradle.kts + バージョンカタログ (gradle/libs.versions.toml、cross/ADR-0002 の技術セット) + Gradle wrapper 一式 (gradle-wrapper.properties は cross/ADR-0002 の Gradle 版を指す) を作成 (→ Requirement: Android ビルドルートの疎通)
- [x] 3.2 `:ksdialogs` モジュール (com.android.library、パッケージ `jp.kamusoft.ksdialogs`、Maven 座標 `jp.kamusoft:ksdialogs`、minSdk 24) を作成 (→ Requirement: Android ビルドルートの疎通 / 公開識別子の適用)
- [x] 3.3 JUnit 5 のスモークテスト1本を追加し `./gradlew build` を通す (→ Scenario: gradle build が通る)

## 4. kmp/ ビルドルート

- [x] 4.1 settings.gradle.kts (includeBuild("../android")、バージョンカタログは android/ のファイルを共有参照) + Gradle wrapper 一式 (android/ と同版) を作成 (→ Requirement: KMP ビルドルートの疎通と composite 接続)
- [x] 4.2 `:ksdialogs-kmp` モジュール (androidTarget + iosArm64 / iosSimulatorArm64 / iosX64、パッケージ `jp.kamusoft.ksdialogs.kmp`、Maven 座標 `jp.kamusoft:ksdialogs-kmp`) を作成し、androidMain に `jp.kamusoft:ksdialogs` への依存を張る (→ Requirement: KMP ビルドルートの疎通と composite 接続 / 公開識別子の適用)
- [x] 4.3 kotlin.test の commonTest スモークテスト1本を追加し、androidTarget と iosSimulatorArm64 の両方でテストを通す (→ Scenario: commonTest が Android と iOS シミュレータの両方で回る)

## 5. maui/ ビルドルート

- [x] 5.1 KsDialogs.slnx + `KsDialogs.Maui` クラスライブラリ (TFM `net10.0;net10.0-ios;net10.0-android`、namespace `KsDialogs`、PackageId `KsDialogs.Maui`) を作成 (→ Requirement: MAUI ビルドルートの疎通 / 公開識別子の適用)
- [x] 5.2 `KsDialogs.Maui.Tests` (net10.0 + UseMaui、NUnit 4) のスモークテスト1本を追加し `dotnet test` を通す (→ Scenario: dotnet build / test が通る)

## 6. 全体検証

- [x] 6.1 4ビルドルートすべてでビルド + テストをクリーン環境相当で通し、識別子宣言を cross/ADR-0005 の写像表と、最低対象 OS の宣言値を cross/ADR-0002 と突き合わせる (→ Requirement: 公開識別子の適用 / 最低対象 OS の宣言 / 全 Requirement)
- [x] 6.2 ルート直下に共通ビルドファイルが無いことを確認する (→ Scenario: ルートに共通ビルドファイルがない)
