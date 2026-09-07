package jp.kamusoft.ksdialogs

/**
 * 真偽値の結果を返す ViewModel の顔 (core/ADR-0012)。
 *
 * ダイアログの結果は「OK か否か」の真偽値であることが大半なので、
 * その場合は結果型を書かずに `class ConfirmViewModel : SimpleDialogViewModel` と宣言できる。
 * Kotlin には型引数の既定値がないため、宣言を省く代わりにこの別名を用意している。
 *
 * この顔で宣言した ViewModel でも、登録の型推論は [DialogViewModel] を直に書いた場合と同じに働く
 * (factory が受け取る [DialogNotifier] は真偽値の通知役になり、show は `DialogResult<Boolean>` を返す)。
 * 結果型を明示する `DialogViewModel<R>` の形はそのまま併存する。
 */
public typealias SimpleDialogViewModel = DialogViewModel<Boolean>
