# maui-binding デルタスペック

## ADDED Requirements

### Requirement: View 生成失敗の構成ミスとしての報告

1 行登録 (`RegisterForDialog` / `RegisterForLoading` / `RegisterForToast`) でライブラリ自身が View を組み立てる経路で View を生成できなかった場合、その失敗は `DialogException.ViewCreationFailed` として表され、元の失敗を InnerException に保持する SHALL。`ViewCreationFailed` は生成しようとした View の型名 (`ViewTypeName`) と ViewModel の型名 (`ViewModelTypeName`) を公開し、利用者アプリ (非 friend アセンブリ) から型・両プロパティ・InnerException を参照できる SHALL。Dialog / Loading では show (start) がこの例外で失敗する SHALL。Toast は Show が戻り値を持たないため呼び出し元へは返さず、既存契約 (警告 + その 1 枚だけの破棄、後続の表示は継続) のまま、警告に `ViewCreationFailed` が原因として残る SHALL。利用者が書いたコード (`Register` / インライン show の factory、`UseViewFallback` の resolver) が投げた例外は包まずそのまま届く SHALL。既存の `DialogException` (`ServiceProviderUnavailable` 等) が先に立つ経路は変わらない SHALL。Dialog / Loading は iOS / Android の実機経路でも、ユニットテストの fake gateway 経路でも、呼び出し元へ届く例外型が同じである SHALL。

#### Scenario: [MB-MA-11] 依存を解決できない View は ViewCreationFailed で失敗する

- **GIVEN** コンストラクタが DI に登録されていない依存を要求する TView を `RegisterForDialog<TView, TViewModel>` で 1 行登録したアプリ
- **WHEN** その ViewModel を show する
- **THEN** show は `DialogException.ViewCreationFailed` で失敗し、`ViewTypeName` が TView の型名・`ViewModelTypeName` が TViewModel の型名、InnerException が DI の解決失敗の例外であり、View は生成も表示もされない。`RegisterForLoading` の 1 行登録でも start が同じ型で失敗する

#### Scenario: [MB-MA-12] Toast の 1 行登録の生成失敗は警告に残して 1 枚だけ破棄する

- **GIVEN** コンストラクタが DI に登録されていない依存を要求する TView を `RegisterForToast` で 1 行登録したアプリ
- **WHEN** その ViewModel を Show し、続けて別の正常な Toast を Show する
- **THEN** 最初の Show は例外を投げず、警告に `ViewCreationFailed` (InnerException 付き) が原因として記録され、その 1 枚は表示されず、後続の Toast は表示される

#### Scenario: [MB-MA-13] 利用者コードの例外は包まれない

- **GIVEN** `Register` の factory が独自の例外を投げるアプリと、`UseViewFallback` の resolver が独自の例外を投げるアプリ
- **WHEN** それぞれの ViewModel を show する
- **THEN** show はその独自の例外で失敗し、`DialogException` には包まれない (resolver が `null` を返した場合は従来どおり `ViewFactoryNotRegistered`)

#### Scenario: 公開例外面の compile 検査

- **GIVEN** 利用者と同じ側から facade を参照する API 形状検査プロジェクト (`KsDialogs.Maui.ApiSurfaceCheck`)
- **WHEN** `DialogException.ViewCreationFailed` を catch し、`ViewTypeName` / `ViewModelTypeName` / `InnerException` を読むコードをビルドする
- **THEN** ビルドが成功する

#### Scenario: [MB-MA-14] Android の実機経路でも同じ型が届く (修正前後の A/B)

- **GIVEN** Android の Sample または消費者アプリで、依存を解決できない TView を 1 行登録したもの
- **WHEN** 修正前のビルドと修正後のビルドで、同じ操作でその ViewModel を show する
- **THEN** 修正前はメッセージだけの `InvalidOperationException` (InnerException なし) で失敗することが記録され、修正後は `DialogException.ViewCreationFailed` (InnerException 付き) で失敗する。iOS でも修正後に同じ型が届く

### Requirement: Android の managed/native 境界での失敗の受け止め

Android の Dialog / Loading の中身の供給は、iOS と同じく managed 側で失敗を値 (中身なし) に変えて互換面へ返し、元の失敗を呼び出し 1 回分の預かり口に退避したうえで、互換面からの失敗の通知を受け取った時点で元の失敗を呼び出し元へそのまま投げ直す SHALL (core/ADR-0033)。互換面 (Kotlin) の Dialog / Loading の中身の供給は Toast と同じく中身なしを返せ、中身なしは既存の失敗経路 (Dialog は閉鎖通知の失敗、Loading は完了通知の失敗) に合流する SHALL。生の例外が境界を越えない SHALL。

#### Scenario: [MB-MA-15] Kotlin 互換面は中身なしを失敗経路へ合流させる

- **GIVEN** 中身なし (null) を返す供給元を渡した Dialog / Loading の互換面
- **WHEN** 提示を要求する
- **THEN** Dialog は閉鎖通知の失敗、Loading は完了通知の失敗として呼び出し側へ通知され、例外は互換面の外へ漏れない

#### Scenario: [MB-MA-16] 既存の失敗経路は変わらない

- **GIVEN** 提示先の画面が無い状態と、利用者操作でキャンセルされる状態
- **WHEN** それぞれ show する
- **THEN** 前者は `DialogException.PresentationHostUnavailable`、後者は `Cancelled` の結果として従来どおり届く

### Requirement: Android binding の生成出力

Android binding のビルドは、互換面の companion object 由来の `BG8401` 警告を出さない SHALL。互換面の `@JvmStatic` メンバー (`Shared` / `ApplyAttributes`) は outer 型の static として引き続き .NET 側から呼べる SHALL。

#### Scenario: BG8401 の不在と static メンバーの可用性

- **GIVEN** Metadata.xml を改めた Android binding
- **WHEN** binding を Release でビルドし、facade を `net10.0-android` でビルドする
- **THEN** ビルド出力に `BG8401` が含まれず、facade の `MauiDialogBridge.Shared` 等の参照が解決してビルドが成功する
