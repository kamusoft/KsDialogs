package jp.kamusoft.ksdialogs.samples.android

import jp.kamusoft.ksdialogs.SimpleDialogViewModel

/**
 * Inline Dialog の ViewModel。
 *
 * 結果型を書かずに済む顔で宣言しているため、結果は真偽値になる (core/ADR-0012)。
 * レジストリには登録せず表示のたびに中身を直接渡すため、このクラスが登録キーになることはない。
 *
 * @property message ダイアログに表示するメッセージ
 */
internal class InlineDialogViewModel(val message: String) : SimpleDialogViewModel
