# ios-native デルタ (localize-dialog-error-messages)

iOS Native がライブラリの外へ出す診断文言 (失敗型のメッセージ・到達不能 init の `fatalError` 文言・警告ログ) を英語固定にする (cross/ADR-0015)。公開契約 (case 名・引数・throw 条件) は変えない。文言は本書の対応表が唯一の源で、実装・テスト・Skills はこの表の英語をそのまま使う。文言そのものは互換契約ではない (契約は case 名・throw 条件。完全一致 Scenario は今回の置き換えの受け入れ基準)。実現経路: `ios/Sources/KsDialogs/Contract/DialogError.swift` の `errorDescription`、`Kmp/KsDialogsKmpError.swift` の `errorDescription`、`Presentation/*.swift` の `fatalError` と `Logger.warning` の文字列リテラルの置き換え。

## ADDED Requirements

### Requirement: iOS の失敗型メッセージは英語固定

`DialogError` と `KsDialogsKmpError` の `errorDescription` は、次の英語文言を返す (SHALL)。`{T}` `{expected}` `{actual}` は現行と同じ値 (型名の文字列) を埋め込む。文言を端末言語で切り替えない。

| case | 現行 (ja) | 変更後 (en) |
|---|---|---|
| `DialogError.viewFactoryNotRegistered(viewModelType:)` | `ViewModel 型 {T} の View factory が登録されていません。` | `No View factory is registered for ViewModel type {T}.` |
| `DialogError.presentationHostUnavailable` | `ダイアログを提示できる画面がありません。` | `No screen is available to present the Dialog.` |
| `DialogError.viewFactoryTypeMismatch(viewModelType:)` | `登録済みの View factory が ViewModel 型 {T} を受け取れません。` | `The registered View factory cannot accept ViewModel type {T}.` |
| `DialogError.resultTypeMismatch(expected:actual:)` | `結果値の型が一致しません (期待: {expected} / 実際: {actual})。` | `The result value type does not match (expected: {expected} / actual: {actual}).` |
| `DialogError.viewModelFactoryNotRegistered(viewModelType:)` | `ViewModel 型 {T} の ViewModel factory が登録されていません。` | `No ViewModel factory is registered for ViewModel type {T}.` |
| `DialogError.viewModelFactoryTypeMismatch(viewModelType:)` | `登録済みの ViewModel factory が ViewModel 型 {T} を生成しません。` | `The registered ViewModel factory does not produce ViewModel type {T}.` |
| `DialogError.viewModelAlreadyShowing(viewModelType:)` | `ViewModel 型 {T} のこのインスタンスは既に表示中です。` | `This ViewModel instance of type {T} is already being shown.` |
| `KsDialogsKmpError.notRegistered(viewModelType:)` | `ViewModel 型 {T} の View factory が登録されていません。` | `No View factory is registered for ViewModel type {T}.` |
| `KsDialogsKmpError.resultTypeMismatch(expected:actual:)` | `結果値の型が一致しません (期待: {expected} / 実際: {actual})。` | `The result value type does not match (expected: {expected} / actual: {actual}).` |

#### Scenario: [DM-IO-01] DialogError の全 case が対応表の英語文言を返す
- **GIVEN** `DialogError` の 7 case それぞれに型名 (`{T}` / `{expected}` / `{actual}`) を与えたインスタンス
- **WHEN** `errorDescription` (`localizedDescription`) を読む
- **THEN** 対応表の英語文言に型名を埋め込んだ文字列と完全一致する

#### Scenario: [DM-IO-02] KsDialogsKmpError の全 case が対応表の英語文言を返す
- **GIVEN** `KsDialogsKmpError` の 2 case それぞれに型名を与えたインスタンス
- **WHEN** `errorDescription` を読む
- **THEN** 対応表の英語文言に型名を埋め込んだ文字列と完全一致する

### Requirement: iOS の到達不能 init の文言と警告ログは英語固定

storyboard 非対応の `init?(coder:)` が呼ぶ `fatalError` の文言と、提示機構が出す警告ログの文言は、次の英語文言とする (SHALL)。`{phase}` は現行と同じ値 (`presentation` / `dismissal`)、`{N}` は収束パスの上限値、`{error}` は元の失敗の説明を埋め込む。iOS と Android で対になる警告 (フック失敗・フック未完了・duration 不正・Toast の中身生成失敗) は、動的な値の前の**本文テンプレート**を android-native デルタの表と同じ英語にする。動的なエラー説明 `{error}` は iOS のログ慣行どおり本文へ埋め込む (Android は throwable 引数で渡すため本文には含めない)。添付値未達は iOS にしか無く、収束警告は Android 側が機能名を持たないため、この 2 つは同一化の対象外。

| 箇所 | 現行 (ja) | 変更後 (en) |
|---|---|---|
| `DialogSwiftUIContentView` / `LoadingDefaultContentView` / `ToastDefaultContentView` の `init?(coder:)` | `この View は storyboard からの生成に対応しない` | `This View does not support instantiation from a storyboard.` |
| `DialogContainerViewController` / `LoadingContainerViewController` / `ToastContainerViewController` の `init?(coder:)` | `この ViewController は storyboard からの生成に対応しない` | `This ViewController does not support instantiation from a storyboard.` |
| `ToastCoordinator` duration 不正 (指定値) | `Toast の duration は正の整数で指定します。既定の duration で表示します。` | `Toast duration must be a positive integer. Showing with the default duration.` |
| `ToastCoordinator` duration 不正 (ToastStyle 既定) | `ToastStyle の既定 duration が正の整数ではありません。内蔵の既定値で表示します。` | `The default duration of ToastStyle is not a positive integer. Showing with the built-in default.` |
| `ToastCoordinator` 中身生成失敗 | `Toast の中身を作れなかったため、この表示を破棄します: {error}` | `Could not create the Toast content. This presentation is discarded: {error}` |
| `DialogContainerViewController` 添付値未達 | `ダイアログの中身から添付値が届かないため、契約の既定値で提示します。` | `No attachment values were received from the Dialog content; presenting with the contract defaults.` |
| `DialogContainerViewController` 収束せず | `ダイアログの添付が収束しないため、{N} 回目のレイアウトの値で固定します。` | `The Dialog attachments did not converge; using the values from layout pass {N}.` |
| `LoadingContainerViewController` 添付値未達 | `ローディングの中身から添付値が届かないため、契約の既定値で提示します。` | `No attachment values were received from the Loading content; presenting with the contract defaults.` |
| `LoadingContainerViewController` 収束せず | `ローディングの添付が収束しないため、{N} 回目のレイアウトの値で固定します。` | `The Loading attachments did not converge; using the values from layout pass {N}.` |
| `ToastContainerViewController` 添付値未達 | `Toast の中身から添付値が届かないため、既定の配置で提示します。` | `No attachment values were received from the Toast content; presenting with the default placement.` |
| `ToastContainerViewController` 収束せず | `Toast の添付が収束しないため、{N} 回目のレイアウトの値で固定します。` | `The Toast attachments did not converge; using the values from layout pass {N}.` |
| `DialogTransitionRunner` フック失敗 | `ダイアログの{phase}フックが失敗しました: {error}` | `The Dialog {phase} hook failed: {error}` |
| `DialogTransitionRunner` フック未完了 | `ダイアログの{phase}フックが完了しません。フックの完了は利用者の責務です。` | `The Dialog {phase} hook did not complete. Completing the hook is the caller's responsibility.` |

#### Scenario: [DM-IO-03] iOS のライブラリ本体に日本語の文字列リテラルが残らない
- **GIVEN** `ios/Sources/` 配下の Swift ソース (コメント行を除く)
- **WHEN** 日本語 (ひらがな・カタカナ・漢字) を含む文字列リテラルを検索する
- **THEN** 該当行は 0 件で、上の 2 表の英語文言が実装のリテラルと一致している (静的 grep とレビューで受け入れる Scenario。`scripts/scenario-id-coverage.py` の除外表に理由付きで登録し、検証コマンドと結果を verification に記録する)
