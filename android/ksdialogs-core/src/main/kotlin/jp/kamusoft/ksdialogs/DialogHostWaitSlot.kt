package jp.kamusoft.ksdialogs

/**
 * 提示先を待つ列から明けた 1 枚が持つ、提示の番。
 *
 * 番を持っている間、列の次の 1 枚は明けない。提示が終わった (器のウィンドウを追加した) ときか、
 * 提示に進まずに show が終わったときに [finish] で返す。UI スレッドから呼ぶ。
 */
internal class DialogHostWaitSlot(queue: DialogHostWaitQueue) {
    private var queue: DialogHostWaitQueue? = queue

    /** 番を返し、提示先があれば列の次の 1 枚を明けさせる。何度呼んでもよく、2 回目以降は何もしない。 */
    fun finish() {
        val owner = queue ?: return
        queue = null
        owner.slotDidFinish(this, advances = true)
    }

    /** 次の 1 枚を明けさせずに番を返す。明けた直後に提示先が消えていて、列の先頭へ戻るときに使う。 */
    fun relinquish() {
        val owner = queue ?: return
        queue = null
        owner.slotDidFinish(this, advances = false)
    }
}
