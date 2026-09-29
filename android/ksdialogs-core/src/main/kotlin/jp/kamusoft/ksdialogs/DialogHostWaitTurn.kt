package jp.kamusoft.ksdialogs

/** 提示先を待った show の番の結末。 */
internal sealed interface DialogHostWaitTurn {
    /**
     * 提示してよい。
     *
     * 列から明けた show は、提示が終わるまで列の番 ([slot]) を持つ。提示が終わったら
     * [DialogHostWaitSlot.finish] で番を返し、次の 1 枚を明けさせる。待たずに提示へ進んだ show は
     * 番を持たない (null)。
     */
    class Ready(val slot: DialogHostWaitSlot?) : DialogHostWaitTurn

    /** 待っている間に結果が確定した (VM の報告)。中身を作らずに終える。 */
    data object Settled : DialogHostWaitTurn
}
