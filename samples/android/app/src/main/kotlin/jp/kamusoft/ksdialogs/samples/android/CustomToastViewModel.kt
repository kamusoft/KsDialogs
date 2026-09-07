package jp.kamusoft.ksdialogs.samples.android

import jp.kamusoft.ksdialogs.ToastViewModel

/**
 * 登録経路のカスタム Toast の ViewModel。
 *
 * Toast は結果も進捗も持たないため、このクラスはデータの運搬とレジストリの型キーだけを担う。
 * 実体はレジストリの ViewModel factory が引数なしで作り、メッセージは表示直前の configure が入れる。
 */
internal class CustomToastViewModel : ToastViewModel {
    /** Toast に表示するメッセージ。表示の直前に設定する。 */
    var message: String = ""
}
