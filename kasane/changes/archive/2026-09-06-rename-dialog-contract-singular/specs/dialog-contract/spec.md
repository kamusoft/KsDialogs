# Delta Spec: dialog-contract (rename-dialog-contract-singular)

対象: Dialog の表示契約の公開型名。挙動 (show / 登録 / 結果通知の意味論) は変えない。

現行の宣言 (確認先):
- iOS: `ios/Sources/KsDialogs/Presentation/KsDialogs.swift` (`public protocol KsDialogs`)、実装 `Presentation/Dialog.swift` (`Dialog: KsDialogs`)
- Android: `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/KsDialogs.kt` (`public interface KsDialogs`)、実装 `Dialog.kt`
- KMP: `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/KsDialogs.kt` (`public interface KsDialogs`)、既定エントリ `Dialog.kt` (`expect object Dialog { val instance }` — 利用者は構築できない)、委譲実装 `DialogGateway.kt` (`internal class GatewayKsDialogs`)、androidMain `AndroidDialogGateway.kt` (Android Native の契約を参照)、テスト fake `commonTest/.../support/FakeKsDialogs.kt`
- Native 契約を参照する wrapper: `maui/macios/native/KsDialogsMauiBridge/MauiDialogBridge.swift`、`maui/android/native/ksdialogs-maui-bridge/src/main/kotlin/jp/kamusoft/ksdialogs/maui/MauiDialogBridge.kt`
- 公開 API 形状の検証機構: `kasane/handbook/cross/test-execution.md` の「公開 API 形状の検証」(正の検査は既定ビルド、負の検査はフラグ式・1 フラグ 1 禁止形状)
- MAUI: `maui/KsDialogs.Maui/Presentation/IKsDialogs.cs` (`public interface IKsDialogs`)、実装 `Presentation/Dialog.cs`
- Loading / Toast の先例: `KsLoading` / `KsToast` (MAUI `IKsLoading` / `IKsToast`)

## ADDED Requirements

### Requirement: 契約型名の規則
表示系機能 (Dialog / Loading / Toast) の契約型名は、全形態で「`Ks` + 機能名の単数形」SHALL とする。MAUI は C# 慣習の `I` 接頭辞を付ける。製品名としての `KsDialogs` (モジュール名・Kotlin パッケージ・C# namespace・NuGet ID・製品名を接頭辞に持つ型) はこの規則の対象外である。

#### Scenario: 3 機能の契約名が同じ規則で読める
- **GIVEN** 各形態の公開面
- **WHEN** Dialog / Loading / Toast の契約型名を並べる
- **THEN** iOS / Android / KMP は `KsDialog` / `KsLoading` / `KsToast`、MAUI は `IKsDialog` / `IKsLoading` / `IKsToast` である

### Requirement: Dialog 契約の改名
Dialog の表示契約は iOS / Android / KMP で `KsDialog`、MAUI で `IKsDialog` SHALL とする。旧名 `KsDialogs` / `IKsDialogs` は typealias・派生 interface・deprecated 別名を含め残さない。契約のメンバ (show 系・registry) の署名と意味論は変えない。

#### Scenario: 旧名では解決できない
- **GIVEN** 改名後の各形態のライブラリと、利用者と同じ可視性の境界にある公開 API 形状の検証 (ios は非 `@testable` ファイル、android / kmp は `api-surface-check`、maui は `KsDialogs.Maui.ApiSurfaceCheck`)
- **WHEN** 旧名 `KsDialogs` (MAUI では `IKsDialogs`) を型として参照する負の検査を、形態ごとの専用フラグで 1 本ずつ実行する
- **THEN** 4 形態すべてで「型が見つからない」系の診断でコンパイルが失敗する (診断文言は実装時に handbook の負の検査表へ追記)

#### Scenario: 既定エントリと自前構築の注入が同じレジストリを共有する (iOS / Android / MAUI)
- **GIVEN** 改名後の iOS / Android / MAUI
- **WHEN** 既定エントリ (`Dialog.shared` / `Dialog.instance` / `Dialog.Instance`) を新しい契約型として扱う、または `Dialog()` を自分で作って新しい契約型として注入する
- **THEN** どちらも同じレジストリを共有し、show の結果は改名前と同じ型・値で返る (既存テストが名前の追随のみで通る)

#### Scenario: 既定エントリの注入と fake の差し替えができる (KMP)
- **GIVEN** 改名後の KMP facade (`Dialog` は `expect object` で、利用者は構築できない)
- **WHEN** `Dialog.instance` を `KsDialog` として共有コードに注入する、または `KsDialog` を実装した fake を注入する
- **THEN** 前者は各 OS の Native 実体へ委譲されて改名前と同じ型・値で結果が返り、後者は Native 実装なしにテストが完結する (既存の facade / fake テストが名前の追随のみで通る)

#### Scenario: 契約名を冠する実装と fake も追随する
- **GIVEN** KMP facade の委譲実装とテスト fake
- **WHEN** 型名とファイル名を確認する
- **THEN** `GatewayKsDialog` / `FakeKsDialog` であり、`GatewayKsDialogs` / `FakeKsDialogs` は存在しない

#### Scenario: 製品名由来の識別子は変わらない
- **GIVEN** 改名後のリポジトリ
- **WHEN** 製品名を接頭辞に持つ識別子 (`KsDialogsOptions` / `KsDialogsKmp` / `AddKsDialogs` / `KsDialogsInterop*` / `KsDialogsMauiBridge` / `KsDialogsInstaller` / `KsDialogsInitializer`) とモジュール名・パッケージ・namespace・NuGet ID を確認する
- **THEN** 改名前と同じ綴りである

#### Scenario: 4 形態のライブラリテストが通る
- **GIVEN** 改名後のリポジトリ
- **WHEN** `kasane/handbook/cross/test-execution.md` の手順で 4 ルート (ios / android / kmp / maui。maui は `dotnet test` + Android 互換面 + iOS 互換面の 3 実行) のテストを実行する
- **THEN** すべて成功し、既定ビルドに含まれる正の API 形状検査 (api-surface-check / ApiSurfaceCheck) は新名で成立する

#### Scenario: 4 Sample がビルドできる
- **GIVEN** 改名後のリポジトリ (Sample に自動テストはない)
- **WHEN** `kasane/handbook/cross/local-development-setup.md` の「Sample のビルドと実行」の手順で samples/ios / samples/android / samples/maui / samples/kmp (Android + iOS) をビルドする
- **THEN** すべてビルドが成功する
