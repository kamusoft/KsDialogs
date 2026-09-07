# maui-binding デルタ (localize-dialog-error-messages)

MAUI (facade と両 OS の native bridge) がライブラリの外へ出す診断文言を英語固定にする (cross/ADR-0015)。公開契約 (例外型・引数・throw 条件) は変えない。文言は本書の対応表が唯一の源。文言そのものは互換契約ではない (契約は例外型・throw 条件。完全一致 Scenario は今回の置き換えの受け入れ基準)。実現経路: `maui/KsDialogs.Maui/Contract/DialogException.cs` の各入れ子型が基底へ渡す文字列、`Internals/{DialogTransitionRunner,BridgeContentSupply,ToastGateway,LoadingGateway}.cs` と `Platforms/{Android,iOS}/Platform{Dialog,Loading}Gateway.cs` の文字列リテラル、`maui/macios/native/KsDialogsMauiBridge/MauiDialogBridgeError.swift` の `errorDescription`、`maui/android/native/ksdialogs-maui-bridge/.../MauiToastBridge.kt` の `error()` の置き換え。

## ADDED Requirements

### Requirement: MAUI の失敗型メッセージは英語固定

`DialogException` の各入れ子型の `Message` は、次の英語文言とする (SHALL)。`{T}` は現行と同じ値 (ViewModel 型名の文字列) を埋め込む。Native と同じ状況の文言は ios-native / android-native デルタと同じ英語にする。

| 例外 | 現行 (ja) | 変更後 (en) |
|---|---|---|
| `DialogException.ViewFactoryNotRegistered` | `ViewModel 型 {T} の View factory が登録されていません。` | `No View factory is registered for ViewModel type {T}.` |
| `DialogException.ViewModelFactoryNotRegistered` | `ViewModel 型 {T} の ViewModel factory が登録されていません。` | `No ViewModel factory is registered for ViewModel type {T}.` |
| `DialogException.ViewModelAlreadyShowing` | `ViewModel 型 {T} のこのインスタンスは既に表示中です。` | `This ViewModel instance of type {T} is already being shown.` |
| `DialogException.ValueTypeViewModel` | `ViewModel 型 {T} は値型のため ViewModel として扱えません。` | `ViewModel type {T} is a value type and cannot be used as a ViewModel.` |
| `DialogException.ServiceProviderUnavailable` | `アプリの IServiceProvider がまだ用意されていません。` | `The app's IServiceProvider is not available yet.` |
| `DialogException.PresentationHostUnavailable` | `ダイアログを提示できる画面がありません。` | `No screen is available to present the Dialog.` |

#### Scenario: [DM-MA-01] DialogException の全入れ子型が対応表の英語文言を持つ
- **GIVEN** `DialogException` の 6 入れ子型それぞれに型名を与えたインスタンス
- **WHEN** `Message` を読む
- **THEN** 対応表の英語文言に型名を埋め込んだ文字列と完全一致する

### Requirement: MAUI の gateway と bridge が投げる例外文言は英語固定

facade の gateway と両 OS の native bridge が `DialogException` 以外の型で投げる (または Native の説明が無いときに補う) 例外文言は、次の英語文言とする (SHALL)。

| 箇所 | 現行 (ja) | 変更後 (en) |
|---|---|---|
| `ToastGateway` (既定 View の Toast の中身を facade で作ろうとした) | `デフォルト View の Toast の中身は Native ライブラリが持ちます。` | `The Native library owns the content of the default Toast.` |
| `LoadingGateway` (既定 Loading の中身を facade で作ろうとした) | `既定ローディングの中身は Native ライブラリが持ちます。` | `The Native library owns the content of the default Loading.` |
| `Platforms/Android/PlatformDialogGateway` / `Platforms/iOS/PlatformDialogGateway` (Native の説明が null のフォールバック) | `ダイアログを提示できませんでした。` | `Could not present the Dialog.` |
| `Platforms/Android/PlatformLoadingGateway` / `Platforms/iOS/PlatformLoadingGateway` (同上) | `ローディングを表示できませんでした。` | `Could not show the Loading.` |
| `MauiDialogBridgeError.unsupportedResult` (iOS bridge) | `ダイアログの結果を判別できませんでした。` | `Could not determine the Dialog result.` |
| `MauiDialogBridgeError.contentUnavailable` (iOS bridge) | `MAUI 側が表示の中身を作れませんでした。` | `The MAUI side could not create the presentation content.` |
| `MauiToastBridge` の `error()` (Android bridge) | `MAUI 側が表示の中身を作れませんでした。` | `The MAUI side could not create the presentation content.` |

#### Scenario: [DM-MA-02] 既定の中身を facade で作ろうとすると英語文言の InvalidOperationException になる
- **GIVEN** Toast / Loading の既定 View の中身を facade 側で生成する経路
- **WHEN** その生成関数を呼ぶ
- **THEN** `InvalidOperationException` の `Message` が上表の英語文言と完全一致する

#### Scenario: [DM-MA-03] iOS bridge の Error が対応表の英語文言を返す
- **GIVEN** `MauiDialogBridgeError` の 2 case
- **WHEN** `errorDescription` を読む
- **THEN** 上表の英語文言と完全一致する

#### Scenario: [DM-MA-04] Android bridge で MAUI 側が中身を作れないと英語文言の IllegalStateException になる
- **GIVEN** MAUI 側の中身の供給が null を返す Toast の bridge
- **WHEN** 表示を要求する
- **THEN** `IllegalStateException` の `message` が上表の英語文言と完全一致する

### Requirement: MAUI の警告ログは英語固定

facade が `Trace.TraceWarning` で出す文言と、その書式に埋め込む部品は次の英語文言とする (SHALL)。`{0}` `{1}` は現行と同じ値を埋め込む。

| 箇所 | 現行 (ja) | 変更後 (en) |
|---|---|---|
| `DialogTransitionRunner` 演出失敗 (書式) | `ダイアログの演出が失敗しました。ダイアログの結果には影響しません: {0}` | `The Dialog transition failed. The Dialog result is not affected: {0}` |
| `DialogTransitionRunner` 演出失敗 (error が null のときの `{0}`) | `キャンセルされました` | `cancelled` |
| `BridgeContentSupply` 中身生成失敗 (書式) | `表示の中身を作れませんでした。{0}: {1}` | `Could not create the presentation content. {0}: {1}` |
| `BridgeContentSupply.CreateOrDiscard` の効果句 (`{0}`) | `この表示を破棄します。他の表示には影響しません` | `This presentation is discarded. Other presentations are not affected` |
| `BridgeContentSupply.CreateOrFail` の効果句 (`{0}`) | `この呼び出しは失敗として返ります` | `This call fails` |

#### Scenario: [DM-MA-05] MAUI のライブラリ本体に日本語の文字列リテラルが残らない
- **GIVEN** `maui/KsDialogs.Maui/`、`maui/macios/native/KsDialogsMauiBridge/`、`maui/android/native/ksdialogs-maui-bridge/src/main/` 配下のソース (コメント行を除く)
- **WHEN** 日本語を含む文字列リテラルを検索する
- **THEN** 該当行は 0 件で、上の 3 表の英語文言が実装のリテラルと一致している (静的 grep とレビューで受け入れる Scenario。`scripts/scenario-id-coverage.py` の除外表に理由付きで登録し、検証コマンドと結果を verification に記録する。Platform gateway のフォールバック文言は `dotnet test` の器 (facade のみ) に乗らないため自動テストの対象にしない)
