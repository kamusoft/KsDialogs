package jp.kamusoft.ksdialogs.samples.kmp

import jp.kamusoft.ksdialogs.kmp.DialogViewModel

/**
 * Model Dialog の ViewModel。
 *
 * 結果型は [Boolean] で、完了操作は `true` を報告する。
 * 共有コードで定義したこのクラスがそのまま各 OS のレジストリの登録キーになる。
 *
 * 実体はレジストリの ViewModel factory が引数なしで作り、表示するメッセージは
 * 型を渡す表示の configure が入れる。
 *
 * 結果の報告口は OS ごとの Native API から供給されるため (core/ADR-0018)、
 * 共有コードのこのクラスは報告口を持たない。中身の View が表示中の自分に紐付いた報告口を引く。
 */
class ModelDialogViewModel : DialogViewModel<Boolean> {
    /** ダイアログに表示するメッセージ。表示の直前に設定する。 */
    var message: String = ""
}
