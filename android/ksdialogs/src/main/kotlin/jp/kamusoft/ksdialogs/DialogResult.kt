package jp.kamusoft.ksdialogs

/**
 * ダイアログの結果。
 *
 * 完了 (結果値つき) とキャンセル (結果値なし) を型で区別する (core/ADR-0003)。
 *
 * @param R 結果値の型
 */
public sealed interface DialogResult<out R> {
    /**
     * ダイアログ側の完了操作で確定した結果。
     *
     * @param V 結果値の型
     */
    public data class Completed<out V>(public val value: V) : DialogResult<V>

    /** キャンセル操作・外側タップ・戻るボタンで確定した結果。結果値は持たない。 */
    public data object Cancelled : DialogResult<Nothing>
}
