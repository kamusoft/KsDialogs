package jp.kamusoft.ksdialogs.samples.kmp.android

import jp.kamusoft.ksdialogs.SimpleDialogViewModel

/**
 * Inline Dialog の ViewModel。
 *
 * 中身をその場で渡す表示は各 OS の Native API にしかないため、この ViewModel も OS 側に置く。
 * 結果型を書かずに済む顔で宣言しているため、結果は真偽値になる (core/ADR-0012)。
 *
 * @property message ダイアログに表示するメッセージ
 */
internal class InlineDialogViewModel(val message: String) : SimpleDialogViewModel
