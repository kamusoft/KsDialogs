# maui-nuget-distribution デルタスペック

## ADDED Requirements

### Requirement: パッケージの共通メタデータと版の宣言元

`maui/` 配下の全プロジェクトは `maui/Directory.Build.props` の共通メタデータ (Authors `kamusoft`、Company `kamusoft LLC`、Copyright `Copyright (c) kamusoft`、MIT の license expression、リポジトリ URL `https://github.com/kamusoft/KsDialogs`、Version 既定値 `0.0.0-dev`、SourceLink、symbol package `snupkg`、アイコン) を継承する SHALL。pack の対象は facade と binding 2 件のみで、テストと API 形状検査のプロジェクトは pack 対象にならない SHALL。csproj に `Version` の直書きは残らない SHALL。`Microsoft.Maui.Controls` の版は `maui/Directory.Packages.props` で workload set 同梱の版 (10.0.20) に宣言され、`samples/maui` の `MauiVersion` も同じ版である SHALL。

#### Scenario: nuspec のメタデータと Version の既定・注入

- **GIVEN** props を導入した `maui/`
- **WHEN** facade と binding 2 件を `-p:Version` なしで pack し、次に `-p:Version=0.1.0-alpha.1` で pack する
- **THEN** 3 パッケージとも前者の nuspec は version `0.0.0-dev`、後者は `0.1.0-alpha.1` になり、いずれも authors `kamusoft`・license expression `MIT`・repository URL・projectUrl・icon を含み、`.snupkg` が併せて生成される。readme は facade の nuspec にだけ存在する

#### Scenario: pack 対象の限定

- **GIVEN** props を導入した `maui/`
- **WHEN** `KsDialogs.Maui.Tests` と `KsDialogs.Maui.ApiSurfaceCheck` に対して pack を実行する
- **THEN** いずれも nupkg を生成しない

#### Scenario: MAUI 本体の版の引き下げ後のビルドとテスト

- **GIVEN** `Microsoft.Maui.Controls` を 10.0.20 にした `maui/` と `samples/maui`
- **WHEN** facade を 3 TFM でビルドし、`KsDialogs.Maui.Tests` を実行し、Sample を両 OS 向けにビルドする
- **THEN** `project.assets.json` で `Microsoft.Maui.Controls` が 10.0.20 に解決し、ビルドはすべて成功し、テストは変更前と同じ件数が実行されすべて成功する

### Requirement: 3 パッケージの構成と内容

facade は Package ID `KsDialogs.Maui` として pack でき、TFM group ごとの依存に `Microsoft.Maui.Controls` (下限 10.0.20) と、platform TFM でのみ対応する binding パッケージ (`KsDialogs.Binding.iOS` / `KsDialogs.Binding.Android`) を同じ version で含む SHALL。facade のパッケージに binding のアセンブリや Gradle / Xcode 由来の native 成果物は含まれない SHALL。Android binding は Gradle 由来の aar 2 本 (互換面 + Android 本体) と Kotlin 系の実行時依存を持ち、.NET Android SDK が生成する自 assembly 用 aar は facade / Android binding の nupkg に含まれない SHALL。iOS binding は device と simulator 両スライスを含む xcframework の binding resource package を持つ SHALL。binding の Description は facade から推移的に参照されるものであり直接参照しない旨を英語で含み、facade の Description は AiForms.Maui.Dialogs の後継であることに触れる SHALL。

#### Scenario: 3 パッケージのローカル pack

- **GIVEN** pack 設定を導入した `maui/`
- **WHEN** binding 2 件と facade を Release で同じ Version を指定して pack する
- **THEN** 3 つの nupkg が生成され、facade の nuspec は `net10.0` に binding 依存を持たず、`net10.0-android*` に `KsDialogs.Binding.Android`、`net10.0-ios*` に `KsDialogs.Binding.iOS` を同じ version で持ち、facade の nupkg に Gradle 由来の `.aar` / `.xcframework` / binding の dll / 自 assembly 用 `KsDialogs.Maui.aar` が含まれない

#### Scenario: binding パッケージの同梱物と説明

- **GIVEN** 上記の nupkg
- **WHEN** binding 2 件の nuspec と同梱物を検査する
- **THEN** Android binding の `lib/` に Gradle 由来の aar 2 本だけがあり (自 assembly 用 `KsDialogs.Binding.Android.aar` は無い) nuspec の依存に Kotlin 系が並び、iOS binding の resource package 内 xcframework に `ios-arm64` と `ios-arm64_x86_64-simulator` の両スライスがあり、両 nuspec の description に "do not reference this package directly" が含まれ、facade の description に "AiForms.Maui.Dialogs" が含まれる

### Requirement: 消費者からの導入

利用者は facade パッケージ 1 件の `PackageReference` だけで、binding と Kotlin 系の依存を推移的に得られる SHALL。同じ workload set の利用者は `Microsoft.Maui.Controls` の版を書かずに (テンプレート既定のまま) 導入でき、要件 (最低 OS 版) を満たす利用者アプリは NU1605 / NU1608 / NU1107 のない restore と両 OS の Release ビルドが成立し、パッケージ経由でダイアログを表示できる SHALL。消費者検証は本リポジトリと同じ SDK / workload (`global.json`) で、隔離した packages path とローカルフィード (テンプレート依存のため nuget.org を併記) を取得元にして行い、3 パッケージの取得元がローカルフィードであることを証跡に残す SHALL。

#### Scenario: ローカルフィードからの restore と Release ビルド

- **GIVEN** 3 パッケージを置いたローカルフォルダフィードと、本リポジトリの `global.json` を複製して SDK を固定し、隔離した packages path を使う、`Microsoft.Maui.Controls` の版を書かず Android 24 / iOS 17.0 を設定した素の MAUI アプリ
- **WHEN** `KsDialogs.Maui` の PackageReference を 1 行足して restore し、facade の公開型を参照した状態で Android と iOS (simulator) を Release でビルドする
- **THEN** restore の警告が 0 件で `Microsoft.Maui.Controls` が 10.0.20 に解決し、binding 2 件が facade と同じ version でローカルフィードから推移的に解決され、両 OS の Release ビルドが成功して成果物に facade と binding のアセンブリが残り、利用者側で `XA4301` が出ない

#### Scenario: パッケージ経由の起動確認

- **GIVEN** 上記の消費者アプリに 1 行登録 (`RegisterForDialog`) とダイアログを表示する操作を足したもの
- **WHEN** Android エミュレータと iOS Simulator で起動してダイアログを表示し、閉じる
- **THEN** 両 OS でダイアログが表示され、閉じると show の結果が届く

### Requirement: 最低 OS 版のビルド時ガード

facade パッケージは利用者のビルドに同梱される MSBuild 資産 (`buildTransitive/`) を持ち、platform TFM のビルドで利用者アプリの `SupportedOSPlatformVersion` が要件 (Android 24 / iOS 17.0、cross/ADR-0002) 未満または未設定なら、診断 ID `KSDLG0001` で要件と設定方法を示すエラーを出してビルドを止める SHALL。要件を満たす場合は何も出力しない SHALL。要件の数値はこの同梱資産が単一の宣言元であり、リポジトリ内の facade・binding 2 件の `SupportedOSPlatformVersion` も同じ宣言元から導かれる SHALL。

#### Scenario: 要件未満の利用者アプリ

- **GIVEN** ローカルフィードの facade を参照し、Android の `SupportedOSPlatformVersion` を 21、iOS を 15.0 に設定した利用者アプリ
- **WHEN** それぞれの platform TFM でビルドする
- **THEN** Android は manifest merger のエラーより先に、iOS はビルド成功に至る前に、`KSDLG0001` で KsDialogs の要件 (Android 24 / iOS 17.0) と `SupportedOSPlatformVersion` の設定を促す文面のエラーで失敗する

#### Scenario: 要件を満たす利用者アプリとリポジトリ内のビルド

- **GIVEN** 要件を満たす設定の利用者アプリと、リポジトリ内の facade / binding 2 件 / Sample
- **WHEN** それぞれをビルドする
- **THEN** ガード由来のエラー・警告は出ず、ビルドが成功する

#### Scenario: 非 platform TFM と outer build ではガードが動かない

- **GIVEN** ローカルフィードの facade を参照する `net10.0` のみのクラスライブラリと、`net10.0;net10.0-android;net10.0-ios` の複数 TFM プロジェクト (最低 OS 版は未設定)
- **WHEN** クラスライブラリをビルドし、複数 TFM プロジェクトの outer build (TFM 指定なし) と `net10.0` の inner build を実行する
- **THEN** ガード由来の診断は出ず、複数 TFM プロジェクトでは platform の inner build に入って初めて要件未満のエラーが出る

#### Scenario: 要件の宣言元の一致

- **GIVEN** 同梱資産 (`buildTransitive/`) の要件の数値
- **WHEN** facade (net10.0-android / net10.0-ios) と binding 2 件について TFM ごとに `SupportedOSPlatformVersion` の評価値を取得する
- **THEN** Android は 24 (SDK の正規化で `24.0`)、iOS は 17.0 と一致し、`maui/` 配下の csproj に数値の直書きが残っていない

### Requirement: package README の表示

facade パッケージはルート `README.md` を package README として同梱する SHALL。`README.md` / `README_ja.md` のリンク参照は public リポジトリ上の絶対 URL であり相対パス参照が残らず、nuget.org と GitHub の両方で辿れる SHALL。

#### Scenario: README の同梱とリンク参照

- **GIVEN** pack 設定を導入した facade
- **WHEN** facade を pack し、`README.md` / `README_ja.md` の画像とリンクの参照を検査する
- **THEN** nupkg のルートに `README.md` があり nuspec の readme がそれを指し、両 README の画像とリンクの参照がすべて `https://raw.githubusercontent.com/kamusoft/KsDialogs/develop/` または `https://github.com/kamusoft/KsDialogs/` 配下の絶対 URL で相対パス参照が残っておらず、各 URL は取得に成功する

### Requirement: MAUI iOS 面の Sample 通し

`samples/maui` の Sample は iOS Simulator でデモ駆動モードにより全デモ項目 (Model Dialog を含む) を通せ、各項目の観察可能な結果が Android 面の既存証跡と同じ形で得られる SHALL。

#### Scenario: iOS 面の全デモ項目

- **GIVEN** `Microsoft.Maui.Controls` 10.0.20 でビルドした Sample と iOS Simulator
- **WHEN** handbook `cross/sample-parity.md` の安定デモ ID を順に起動引数で渡して起動する
- **THEN** 全デモ項目が表示され、結果表示の変化まで観察でき、証跡が evidence/ に残る

### Requirement: MAUI 本体の版の追随ルール

handbook `cross/local-development-setup.md` は、`global.json` の workload set を上げるときに `maui/Directory.Packages.props` の `Microsoft.Maui.Controls` と Sample の `MauiVersion` を workload set 同梱の版に合わせる 1 ルールを含む SHALL。

#### Scenario: 規範文書の記載

- **GIVEN** 改訂後の handbook
- **WHEN** 「.NET SDK と MAUI ワークロード」の節を読む
- **THEN** props と Sample の版を workload 同梱版に合わせるルールと、同梱版の確かめ方 (`$(MauiVersion)` の既定値) が書かれている
