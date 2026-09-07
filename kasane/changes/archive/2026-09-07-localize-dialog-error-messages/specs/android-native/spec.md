# android-native デルタ (localize-dialog-error-messages)

Android Native がライブラリの外へ出す診断文言 (失敗型のメッセージ・警告ログ) を英語固定にする (cross/ADR-0015)。公開契約 (例外型・引数・throw 条件) は変えない。文言は本書の対応表が唯一の源。文言そのものは互換契約ではない (契約は例外型・throw 条件。完全一致 Scenario は今回の置き換えの受け入れ基準)。実現経路: `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogException.kt` の各サブクラスが基底へ渡す文字列、`DialogLayoutHost.kt` / `DialogTransitionRunner.kt` / `ToastCoordinator.kt` / `LoadingCoordinator.kt` の `Log.w` の文字列リテラルの置き換え。

## ADDED Requirements

### Requirement: Android の失敗型メッセージは英語固定

`DialogException` の各サブクラスの `message` は、次の英語文言とする (SHALL)。`{T}` は現行と同じ値 (ViewModel 型名の文字列) を埋め込む。iOS と同じ状況の文言は ios-native デルタと同じ英語にする。

| 例外 | 現行 (ja) | 変更後 (en) |
|---|---|---|
| `DialogException.ViewFactoryNotRegistered` | `ViewModel 型 {T} の View factory が登録されていません。` | `No View factory is registered for ViewModel type {T}.` |
| `DialogException.ViewModelFactoryNotRegistered` | `ViewModel 型 {T} の ViewModel factory が登録されていません。` | `No ViewModel factory is registered for ViewModel type {T}.` |
| `DialogException.ViewModelAlreadyShowing` | `ViewModel 型 {T} のこのインスタンスは既に表示中です。` | `This ViewModel instance of type {T} is already being shown.` |
| `DialogException.ValueClassViewModel` | `ViewModel 型 {T} は value class のため ViewModel として扱えません。` | `ViewModel type {T} is a value class and cannot be used as a ViewModel.` |
| `DialogException.PresentationHostUnavailable` | `ダイアログを提示できる画面がありません。` | `No screen is available to present the Dialog.` |

#### Scenario: [DM-AN-01] DialogException の全サブクラスが対応表の英語文言を持つ
- **GIVEN** `DialogException` の 5 サブクラスそれぞれに型名を与えたインスタンス
- **WHEN** `message` を読む
- **THEN** 対応表の英語文言に型名を埋め込んだ文字列と完全一致する

### Requirement: Android の警告ログは英語固定

提示機構が出す `Log.w` の文言は、次の英語文言とする (SHALL)。`{phase}` は現行と同じ値 (`presentation` / `dismissal`)、`{N}` は収束パスの上限値を埋め込む。原因の例外は現行どおり `Log.w` の throwable 引数で渡し、本文には含めない。iOS と対になる警告 (フック失敗・フック未完了・duration 不正・Toast の中身生成失敗) は本文テンプレートを ios-native デルタと同じ英語にする。収束警告は `DialogLayoutHost` が Dialog / Loading / Toast の器で共用され機能名を持たないため、機能名を含めない文言にする (iOS の機能別文言と同一化しない)。

| 箇所 | 現行 (ja) | 変更後 (en) |
|---|---|---|
| `DialogLayoutHost` 収束せず (Dialog / Loading / Toast 共用) | `ダイアログの添付が収束しないため、{N} 回目のレイアウトの値で固定します。` | `The attachments did not converge; using the values from layout pass {N}.` |
| `DialogTransitionRunner` フック失敗 | `ダイアログの{phase}フックが失敗しました。` | `The Dialog {phase} hook failed.` |
| `DialogTransitionRunner` フック未完了 | `ダイアログの{phase}フックが完了しません。フックの完了は利用者の責務です。` | `The Dialog {phase} hook did not complete. Completing the hook is the caller's responsibility.` |
| `ToastCoordinator` 中身生成失敗 | `Toast の中身を作れませんでした。この表示を破棄します。` | `Could not create the Toast content. This presentation is discarded.` |
| `ToastCoordinator` duration 不正 (指定値) | `Toast の duration は正の整数で指定します。既定の duration で表示します。` | `Toast duration must be a positive integer. Showing with the default duration.` |
| `ToastCoordinator` duration 不正 (ToastStyle 既定) | `ToastStyle の既定 duration が正の整数ではありません。内蔵の既定値で表示します。` | `The default duration of ToastStyle is not a positive integer. Showing with the built-in default.` |
| `LoadingCoordinator` 中身生成失敗 | `ローディングの中身を作れませんでした。表示は行われません。` | `Could not create the Loading content. Nothing is presented.` |

#### Scenario: [DM-AN-02] Android のライブラリ本体に日本語の文字列リテラルが残らない
- **GIVEN** `android/ksdialogs/src/main/` と `android/ksdialogs-compose/src/main/` 配下の Kotlin ソース (コメント行を除く)
- **WHEN** 日本語を含む文字列リテラルを検索する
- **THEN** 該当行は 0 件で、上の 2 表の英語文言が実装のリテラルと一致している (静的 grep とレビューで受け入れる Scenario。`scripts/scenario-id-coverage.py` の除外表に理由付きで登録し、検証コマンドと結果を verification に記録する)
