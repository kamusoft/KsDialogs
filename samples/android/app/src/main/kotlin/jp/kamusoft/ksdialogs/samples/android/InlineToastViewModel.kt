package jp.kamusoft.ksdialogs.samples.android

import jp.kamusoft.ksdialogs.ToastViewModel

/**
 * インライン経路のカスタム Toast の ViewModel。
 *
 * 中身はこの場で渡すため、このクラスはレジストリに登録しない (core/ADR-0013)。
 *
 * @property message Toast に表示するメッセージ
 */
internal class InlineToastViewModel(val message: String) : ToastViewModel
