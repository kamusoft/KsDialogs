package jp.kamusoft.ksdialogs

/**
 * 結果の型消去表現。
 *
 * 公開 API の [DialogResult] と違い結果値の型を持たず、レジストリ・提示層が共通に使う輸送形。
 * 値の実体は [DialogNotifier] が宣言結果型で受け取ったものをそのまま運ぶ。
 */
internal sealed interface DialogOutcome {
    /** 結果値つきの完了。 */
    class Completed(val value: Any?) : DialogOutcome

    /** 結果値を持たないキャンセル。 */
    data object Cancelled : DialogOutcome
}
