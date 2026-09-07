# repo-scaffold デルタスペック

技術セットのバージョンと最低対象 OS は cross/ADR-0002、ビルドルート構成は cross/ADR-0004、識別子は cross/ADR-0005 に従う。

## ADDED Requirements

### Requirement: iOS ビルドルートの疎通

ios/ は独立した SwiftPM ビルドルートとして、ライブラリターゲット `KsDialogs` のビルドとテストが成功する SHALL。

#### Scenario: swift test が通る

- **GIVEN** クリーンなチェックアウトと cross/ADR-0002 の Xcode / Swift 環境
- **WHEN** ios/ で `swift test` を実行する
- **THEN** `KsDialogs` のビルドと Swift Testing のスモークテスト1本が成功する

#### Scenario: iOS Simulator 向けビルドが通る

- **GIVEN** 同環境 (swift test はホスト macOS 向けビルドのため、iOS SDK 向けの疎通は別途検証する)
- **WHEN** `xcodebuild build` を destination: iOS Simulator で実行する
- **THEN** iOS SDK 向けのコンパイルが成功する

### Requirement: Android ビルドルートの疎通

android/ は独立した Gradle ビルドルートとして、Android ライブラリモジュール `:ksdialogs` のビルドとテストが成功する SHALL。

#### Scenario: gradle build が通る

- **GIVEN** クリーンなチェックアウトと cross/ADR-0002 の Gradle / AGP / Kotlin 環境
- **WHEN** android/ で `./gradlew build` を実行する
- **THEN** `:ksdialogs` (com.android.library、minSdk は cross/ADR-0002 の値) のビルドと JUnit 5 のスモークテスト1本が成功する

### Requirement: KMP ビルドルートの疎通と composite 接続

kmp/ は独立した Gradle ビルドルートとして、multiplatform モジュール `:ksdialogs-kmp` (androidTarget + iosArm64 / iosSimulatorArm64 / iosX64) のビルドが成功し、composite build (includeBuild) 経由で Android Native ライブラリ `jp.kamusoft:ksdialogs` への依存が解決される SHALL。

#### Scenario: composite 接続込みでビルドが通る

- **GIVEN** クリーンなチェックアウト (Maven リポジトリへの `jp.kamusoft:ksdialogs` の公開は存在しない)
- **WHEN** kmp/ で `./gradlew build` を実行する
- **THEN** `jp.kamusoft:ksdialogs` が includeBuild によりローカルの android/ ビルドへ置換解決され、全ターゲットのビルドが成功する

#### Scenario: commonTest が Android と iOS シミュレータの両方で回る

- **GIVEN** kmp/ の commonTest に kotlin.test のスモークテスト1本がある
- **WHEN** androidTarget のユニットテストと iosSimulatorArm64 のテストを実行する
- **THEN** 両ターゲットでテストが成功する

### Requirement: MAUI ビルドルートの疎通

maui/ は独立した .NET ビルドルートとして、`KsDialogs.Maui` クラスライブラリ (TFM: net10.0 / net10.0-ios / net10.0-android) のビルドとテストが成功する SHALL。

#### Scenario: dotnet build / test が通る

- **GIVEN** クリーンなチェックアウトと cross/ADR-0002 の .NET 環境
- **WHEN** maui/ で `dotnet build KsDialogs.slnx` と `dotnet test` を実行する
- **THEN** 全 TFM のビルドと、NUnit 4 のスモークテスト1本 (net10.0 のテストプロジェクトから参照) が成功する

### Requirement: 公開識別子の適用

各ビルドルートの成果物は cross/ADR-0005 の写像表の識別子を持つ SHALL — iOS は Swift モジュール・SwiftPM パッケージ名とも `KsDialogs` / Android は Kotlin パッケージ `jp.kamusoft.ksdialogs`・Maven 座標 `jp.kamusoft:ksdialogs` / KMP は Kotlin パッケージ `jp.kamusoft.ksdialogs.kmp`・Maven 座標 `jp.kamusoft:ksdialogs-kmp` / MAUI は namespace `KsDialogs`・NuGet ID `KsDialogs.Maui`。

#### Scenario: 識別子が写像表と一致する

- **GIVEN** 4ビルドルートのビルド設定ファイル
- **WHEN** モジュール名・パッケージ / namespace・配布座標の宣言を写像表と突き合わせる
- **THEN** すべて cross/ADR-0005 の値と一致する

### Requirement: 最低対象 OS の宣言

各ビルドルートは cross/ADR-0002 の最低対象 OS (iOS 17 / Android minSdk 24) をビルド設定で宣言する SHALL — ios/ は platforms `.iOS(.v17)` / android/ は minSdk 24 / kmp/ は androidTarget minSdk 24 + iOS deployment target 17 / maui/ は SupportedOSPlatformVersion (ios 17.0 / android 24)。

#### Scenario: 宣言値が ADR と一致する

- **GIVEN** 4ビルドルートのビルド設定ファイル
- **WHEN** 最低対象 OS の宣言値を cross/ADR-0002 と突き合わせる
- **THEN** すべて iOS 17 / minSdk 24 と一致する

### Requirement: リポジトリ初期整備

リポジトリルートは README.md (英語主・最小限: 名前・コンセプト・開発中ステータス・4ビルドルートの地図) / .gitignore (4形態分をルート1本に集約) / LICENSE (MIT, (c) kamusoft) を持ち、共通ビルドファイルを置かない SHALL。

#### Scenario: ルートに共通ビルドファイルがない

- **GIVEN** リポジトリルートのファイル一覧
- **WHEN** ビルド入口 (Package.swift / settings.gradle.kts / *.slnx 等) を探す
- **THEN** ルート直下には存在せず、各ビルドルート配下にのみ存在する
