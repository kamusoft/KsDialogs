# maui-nuget-distribution デルタスペック

## MODIFIED Requirements

### Requirement: 3 パッケージの構成と内容

facade は Package ID `KsDialogs.Maui` として pack でき、TFM group ごとの依存に `Microsoft.Maui.Controls` (下限 10.0.20) と、platform TFM でのみ対応する binding パッケージ (`KsDialogs.Binding.iOS` / `KsDialogs.Binding.Android`) を同じ version で含む SHALL。facade のパッケージに binding のアセンブリや Gradle / Xcode 由来の native 成果物は含まれない SHALL。facade は 3 つの TFM すべてで XML ドキュメントファイル (`KsDialogs.Maui.xml`) を生成して nupkg の `lib/<TFM>/` に同梱する SHALL。binding 2 つは XML ドキュメントを生成せず、その指定は SDK 既定に頼らず csproj に明示する SHALL。Android binding は Gradle 由来の aar 2 本 (互換面 + Android 本体) と Kotlin 系の実行時依存を持ち、.NET Android SDK が生成する自 assembly 用 aar は facade / Android binding の nupkg に含まれない SHALL。iOS binding は device と simulator 両スライスを含む xcframework の binding resource package を持つ SHALL。binding の Description は facade から推移的に参照されるものであり直接参照しない旨を英語で含み、facade の Description は AiForms.Maui.Dialogs の後継であることに触れる SHALL。

#### Scenario: 3 パッケージのローカル pack

- **GIVEN** pack 設定を導入した `maui/`
- **WHEN** binding 2 件と facade を Release で同じ Version を指定して pack する
- **THEN** 3 つの nupkg が生成され、facade の nuspec は `net10.0` に binding 依存を持たず、`net10.0-android*` に `KsDialogs.Binding.Android`、`net10.0-ios*` に `KsDialogs.Binding.iOS` を同じ version で持ち、facade の nupkg に Gradle 由来の `.aar` / `.xcframework` / binding の dll / 自 assembly 用 `KsDialogs.Maui.aar` が含まれない

#### Scenario: binding パッケージの同梱物と説明

- **GIVEN** 上記の nupkg
- **WHEN** binding 2 件の nuspec と同梱物を検査する
- **THEN** Android binding の `lib/` に Gradle 由来の aar 2 本だけがあり (自 assembly 用 `KsDialogs.Binding.Android.aar` は無い) nuspec の依存に Kotlin 系が並び、iOS binding の resource package 内 xcframework に `ios-arm64` と `ios-arm64_x86_64-simulator` の両スライスがあり、両 nuspec の description に "do not reference this package directly" が含まれ、facade の description に "AiForms.Maui.Dialogs" が含まれる

#### Scenario: XML ドキュメントは facade の 3 TFM に揃う

- **GIVEN** 上記の nupkg
- **WHEN** 3 つの nupkg の `lib/` を検査する
- **THEN** facade の `lib/net10.0/`・`lib/net10.0-ios*/`・`lib/net10.0-android*/` のそれぞれに `KsDialogs.Maui.xml` があり、binding 2 件の nupkg には `.xml` が無い

## ADDED Requirements

### Requirement: 発行版の nupkg 名の検査
release workflow は MAUI の pack 直後と nuget.org への push 直前の 2 回、3 つの nupkg が `<Package ID>.<入力 version>.nupkg` の名前で存在し、対応する snupkg が対で存在することを検査する SHALL。名前が入力 version と一致しない (開発既定値 `0.0.0-dev` を含む) 場合は失敗し、push は実行されない。

#### Scenario: 開発既定値のままの nupkg は push されない
- **GIVEN** `-p:Version=` が渡らず `0.0.0-dev` で pack された nupkg
- **WHEN** 名前の検査が実行される
- **THEN** 入力 version の nupkg が無いことを理由に失敗し、nuget.org への push は実行されない

#### Scenario: artifact の取り違えを push 前に止める
- **GIVEN** publish job が download した MAUI の artifact に、入力 version と異なる version の nupkg しか無い状態
- **WHEN** push 直前の検査が実行される
- **THEN** 失敗し、push は実行されない
