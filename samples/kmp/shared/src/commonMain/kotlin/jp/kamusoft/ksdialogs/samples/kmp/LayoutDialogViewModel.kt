package jp.kamusoft.ksdialogs.samples.kmp

import jp.kamusoft.ksdialogs.kmp.DialogViewModel

/**
 * Layout Dialog の ViewModel。
 *
 * 結果型は [Boolean] で、完了操作は `true` を報告する。
 * 共有コードで定義したこのクラスがそのまま各 OS のレジストリの登録キーになる。
 *
 * @property message ダイアログに表示するメッセージ
 * @property usesVisibleArea サイズと位置の計算に可視領域を使うか。
 *   静的メタ属性は各 OS の View 定義側で完結するため、View factory が作った View への添付として供給する
 */
class LayoutDialogViewModel(
    val message: String,
    val usesVisibleArea: Boolean,
) : DialogViewModel<Boolean>
