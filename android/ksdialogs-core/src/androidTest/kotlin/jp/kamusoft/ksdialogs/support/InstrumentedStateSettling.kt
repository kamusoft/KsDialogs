package jp.kamusoft.ksdialogs.support

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.SystemClock
import android.view.View
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.delay
import org.junit.Assert.assertTrue
import java.util.concurrent.atomic.AtomicReference

/**
 * 状態遷移の観測を「終端状態の合意」で待ち合わせる共通の待ち。
 *
 * 遷移の途中には、目的の終端状態と見分けのつかない一瞬が現れる — 要求だけが先に切り替わる窓、
 * 器が取り付いてから中身が載るまでの隙、アニメーションが折り返す瞬間。その一瞬を読んで先へ進むと、
 * 続く操作が「もう終わっている」と判定されて捨てられ、待ちが無音のまま時間切れになる。
 *
 * ここでは終端の判定を、呼び出し側が渡す述語 (複数の観測の合意と、遷移が進行していないこと) が
 * **一定時間続けて成り立つ**ことに限定する。1 回の読みで判定しないので、上記の一瞬では成立しない。
 */
internal object InstrumentedStateSettling {

    /** 落ち着きを認めるまでに述語が成り立ち続ける時間 (ミリ秒)。60Hz の 3 フレーム弱に当たる。 */
    const val DEFAULT_STABLE_MILLIS = 48L

    /** 述語を読み直す間隔 (ミリ秒)。 */
    private const val POLLING_INTERVAL_MILLIS = 8L

    /** ウィンドウが入力の宛先になるのを待つ上限 (ミリ秒)。 */
    private const val WINDOW_FOCUS_TIMEOUT_MILLIS = 10_000L

    /**
     * [settled] が [stableMillis] の間続けて成り立つまで待つ。時間切れになったら false を返す。
     *
     * 途中で 1 度でも偽に振れたら、そこから数え直す。
     */
    suspend fun awaitSettled(
        timeoutMillis: Long,
        stableMillis: Long = DEFAULT_STABLE_MILLIS,
        settled: () -> Boolean,
    ): Boolean {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        var heldSince: Long? = null
        while (true) {
            val now = SystemClock.uptimeMillis()
            if (settled()) {
                val since = heldSince ?: now.also { heldSince = it }
                if (now - since >= stableMillis) return true
            } else {
                heldSince = null
            }
            if (now >= deadline) return false
            delay(POLLING_INTERVAL_MILLIS)
        }
    }

    /**
     * その View が載っているウィンドウが入力の宛先として落ち着くまで待ち、
     * 落ち着かなければ観測履歴を添えて落とす。
     *
     * ウィンドウは画面へ追加されてすぐ入力の宛先になるわけではない。レイアウトが終わっていても
     * 宛先の切り替わりより前にタップを注入すると、背後のウィンドウがそれを受け取る
     * (「前面が受け取る」「背後へ透過しない」のどちらの判定も、そこで裏返る)。
     *
     * 判定は [WindowState.isInputTarget] の合意で行い、成り立ち続けることを [awaitSettled] が見る。
     * 時間切れになった回に宛先がどこにあったかは事後には読めないので、観測が変わるたびに
     * [StateHistory] へ積み、失敗の説明文に添える。
     */
    suspend fun assertWindowFocused(
        view: View,
        reason: String,
        timeoutMillis: Long = WINDOW_FOCUS_TIMEOUT_MILLIS,
    ) {
        val history = StateHistory()
        val settled = awaitSettled(timeoutMillis) {
            val state = readWindowState(view)
            history.recordChange(state.toString())
            state.isInputTarget
        }
        assertTrue(
            "$reason\n" +
                "期待した状態: ウィンドウが取り付いて可視で、入力の宛先がそこにある" +
                " (待ちの上限 $timeoutMillis ms)\n" +
                "観測履歴 (数値は観測開始からの経過ミリ秒):\n${history.format()}",
            settled,
        )
    }

    /** ウィンドウの今の状態を main で読む。 */
    private fun readWindowState(view: View): WindowState {
        val value = AtomicReference<WindowState>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val decor = activityDecorOf(view)
            value.set(
                WindowState(
                    attached = view.isAttachedToWindow,
                    windowVisible = view.windowVisibility == View.VISIBLE,
                    laidOut = view.isShown && view.width > 0 && view.height > 0,
                    hasWindowFocus = view.hasWindowFocus(),
                    rootHasWindowFocus = view.rootView.hasWindowFocus(),
                    otherWindowFocused =
                        if (decor == null || decor.rootView === view.rootView) {
                            null
                        } else {
                            decor.hasWindowFocus()
                        },
                ),
            )
        }
        return requireNotNull(value.get())
    }

    /** その View を載せている Activity の decorView (辿れなければ null)。 */
    private fun activityDecorOf(view: View): View? {
        var context: Context? = view.context
        while (context != null) {
            if (context is Activity) return context.window?.decorView
            context = (context as? ContextWrapper)?.baseContext
        }
        return null
    }

    /**
     * ウィンドウが入力の宛先になっているかを読むための観測の一時点。
     *
     * 独立した読みを 3 系統ぶん持つ。取り付けと可視とレイアウトは**ウィンドウが入力の受け手として
     * 成立しているか**を、フォーカスの旗は**システムがそこを宛先に選んだか**を、
     * もう一方のウィンドウの旗は**宛先が実際に移り終えたか**を見る。
     *
     * [hasWindowFocus] と [rootHasWindowFocus] は取り付いている限り同じ値になる
     * (同じ attach info から配られる) ため、この 2 つは互いの独立な裏付けにはならない。
     * 食い違うのは View が木から外れている間だけで、そこを弾くために両方を読む。
     */
    data class WindowState(
        val attached: Boolean,
        val windowVisible: Boolean,
        val laidOut: Boolean,
        val hasWindowFocus: Boolean,
        val rootHasWindowFocus: Boolean,
        val otherWindowFocused: Boolean?,
    ) {

        /**
         * ウィンドウが入力の宛先として成立しているか。
         *
         * 別のウィンドウ (この View を載せている Activity 自身の画面) が読めるときは、
         * そちらが宛先を手放していることも求める — 旗が届いた直後に読んで先へ進むと、
         * 受け渡しの途中で注入したタップが元のウィンドウへ落ちる経路が残るため。
         */
        val isInputTarget: Boolean
            get() = attached && windowVisible && laidOut &&
                hasWindowFocus && rootHasWindowFocus && otherWindowFocused != true

        override fun toString(): String =
            "attached=$attached windowVisible=$windowVisible laidOut=$laidOut" +
                " hasWindowFocus=$hasWindowFocus rootHasWindowFocus=$rootHasWindowFocus" +
                " otherWindowFocused=$otherWindowFocused"
    }
}

/**
 * 単調時計付きの観測履歴。時間切れになった回に何が起きていたかを説明文へ添えるために使う。
 *
 * 事後には読めない経過 (要求の順序・アニメーションの出入り・見えの遷移) を、起きた時点で積む。
 * 上限に達したら**中ほどを捨てて先頭と末尾を残す** — 読みたいのは始まりの経緯と時間切れの直前で、
 * 履歴が上限に届くのは状態が細かく振れている回、つまり最も読みたい回だから。
 */
internal class StateHistory(
    private val headLimit: Int = DEFAULT_HEAD_LIMIT,
    private val tailLimit: Int = DEFAULT_TAIL_LIMIT,
) {

    private val lock = Any()
    private val startedAt = SystemClock.uptimeMillis()
    private val head = ArrayList<String>()
    private val tail = ArrayDeque<String>()
    private var dropped = 0

    /** 直前に積んだ状態の文字列。同じ状態が続く間は積まない。 */
    private var lastState: String? = null

    /** 出来事を単調時計付きで積む。 */
    fun record(label: String) {
        synchronized(lock) {
            val line = "+${SystemClock.uptimeMillis() - startedAt}ms $label"
            if (head.size < headLimit) {
                head.add(line)
                return
            }
            tail.addLast(line)
            while (tail.size > tailLimit) {
                tail.removeFirst()
                dropped++
            }
        }
    }

    /** 状態を積む。直前と同じ内容なら積まない (変化した時点だけが履歴に残る)。 */
    fun recordChange(state: String) {
        synchronized(lock) {
            if (state == lastState) return
            lastState = state
        }
        record(state)
    }

    /** 積んだ履歴を古い順に並べた本文。捨てた区間があればその旨を挟む。 */
    fun format(): String = synchronized(lock) {
        val lines = ArrayList(head)
        if (dropped > 0) {
            lines.add("(履歴の中ほど $dropped 件は上限のため捨てた)")
        }
        lines.addAll(tail)
        lines.joinToString("\n")
    }

    private companion object {
        /** 始まりの経緯として残す件数。 */
        const val DEFAULT_HEAD_LIMIT = 100

        /** 時間切れの直前として残す件数。 */
        const val DEFAULT_TAIL_LIMIT = 400
    }
}
