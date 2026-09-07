package jp.kamusoft.ksdialogs.samples.android

import jp.kamusoft.ksdialogs.SimpleDialogViewModel

/**
 * Declarative Dialog の ViewModel。
 *
 * 結果型を書かずに済む顔で宣言しているため、結果は真偽値になる (core/ADR-0012)。
 * このクラス参照そのものがレジストリの登録キーになる。
 *
 * @property message ダイアログに表示するメッセージ
 */
internal class DeclarativeDialogViewModel(val message: String) : SimpleDialogViewModel
