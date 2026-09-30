# Delta Spec: samples (rename-current-page-marker)

対象: レイアウトデモ (属性調整パネル) で表示中のページを名乗らせる印の呼び出し。画面・文言・挙動は変えない。

現行の呼び出し (確認先):
- iOS Native: `samples/ios/KsDialogsSample/SampleLayoutPanelScreen.swift`
- KMP (iOS): `samples/kmp/iosApp/KsDialogsSampleKmp/SampleLayoutPanelScreen.swift`
- Android Native: `samples/android/app/src/main/kotlin/jp/kamusoft/ksdialogs/samples/android/SampleLayoutPanelScreen.kt`
- KMP (Android): `samples/kmp/androidApp/src/main/kotlin/jp/kamusoft/ksdialogs/samples/kmp/android/SampleLayoutPanelScreen.kt`
- MAUI は印を使わず、MAUI 層の既定 provider に任せている (`kasane/handbook/cross/sample-parity.md` の「表示中のページの名乗り」の行)
- ビルド手順: `kasane/handbook/cross/local-development-setup.md` の「Sample のビルドと実行」

## ADDED Requirements

### Requirement: Sample は新しい名前の印を使う

iOS Native と KMP (iOS) は SwiftUI の `markAsDialogCurrentPage()` を、Android Native と KMP (Android) は Compose の `Modifier.markAsDialogCurrentPage()` を、改名前と同じ各タブの中身の枠に付けること (SHALL)。MAUI の Sample は変えないこと (SHALL)。パネルの画面・文言・ダイアログの表示結果は改名前と変えないこと (SHALL)。

#### Scenario: 印を使う 4 ルートの Sample がビルドできる
- **GIVEN** 改名後のリポジトリ
- **WHEN** `kasane/handbook/cross/local-development-setup.md` の「Sample のビルドと実行」の手順で samples/ios・samples/android・samples/kmp (Android と iOS) をビルドする
- **THEN** すべてビルドが成功する

#### Scenario: Sample の差分は印の名前だけ
- **GIVEN** 改名前後の 4 ルートの Sample
- **WHEN** 差分を見比べる
- **THEN** 変わった行は印の呼び出しと、Kotlin の import だけである

#### Scenario: 旧名が Sample に残らない
- **GIVEN** 改名後の samples/
- **WHEN** `ksDialogCurrentPage` を大文字小文字を区別して検索する
- **THEN** 0 件である
