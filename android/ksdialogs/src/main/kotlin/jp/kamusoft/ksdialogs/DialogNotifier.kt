package jp.kamusoft.ksdialogs

/**
 * ダイアログ側から結果を報告する部品。
 *
 * 完了 (結果値つき) とキャンセルの2操作のみを持ち、結果値の型は ViewModel の宣言に固定される。
 * 報告はちょうど1回だけ有効で、確定後の報告は何も起こさない (core/ADR-0003)。
 * 任意のスレッドから呼び出してよい。
 *
 * show 1回につき新しいインスタンスが View factory へ渡されるため、
 * 同じ ViewModel を重ねて表示しても報告先が混ざることはない。
 *
 * @param R 報告できる結果値の型
 */
public class DialogNotifier<R> internal constructor(
    private val resultChannel: DialogResultChannel,
) {
    /** 結果値つきで完了を報告する。 */
    public fun complete(value: R) {
        resultChannel.settle(DialogOutcome.Completed(value))
    }

    /** キャンセルを報告する。 */
    public fun cancel() {
        resultChannel.settle(DialogOutcome.Cancelled)
    }
}
