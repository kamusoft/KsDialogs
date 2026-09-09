package jp.kamusoft.ksdialogs.support

import android.app.Activity
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsAnimation
import android.view.inputmethod.InputMethodManager
import androidx.annotation.RequiresApi
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import java.util.concurrent.atomic.AtomicReference

/**
 * ソフトキーボードの出し入れを、終端状態の合意まで待ってから読む観測。
 *
 * IME の出し入れは、アプリ側が持つ要求可視性と、IME が実際に占める枠と、出入りのアニメーションが
 * 別々に動く。要求可視性だけを読むと、表示アニメーションの最中に出した非表示要求が保留され、
 * その保留が適用される数十ミリ秒だけ要求が非表示へ振れる窓を「引っ込んだ」と読み違える。
 * 読み違えたまま先へ進むと、続く非表示要求が「要求上は既に非表示」として捨てられ、
 * 待ちが無音のまま時間切れになる。
 *
 * 見えは [State] にまとめて読み、落ち着きの判定は [State.isSettled] に置く。
 * 時間切れになった回に何が起きていたかは事後には読めないので、状態が変わるたびに
 * 単調時計 ([SystemClock.uptimeMillis]) 基準の [StateHistory] へ積み、失敗時のメッセージに添える。
 *
 * 判定できるのは API 30 以降 — それ以前は IME の見えを直接読む口が無い。
 */
@RequiresApi(Build.VERSION_CODES.R)
internal class ImeSettleWaiting(
    private val activity: Activity,
    private val inputField: View,
) {

    private val history = StateHistory()

    /**
     * 走行中の IME のアニメーションと、走行を認識した時刻。
     *
     * 同一性で数えるので開始の通知が重なっても二重に数えない。終了の通知が届かない回
     * (取り付けの前後をまたいだ走行・組み立たなかった走行) に集合が空にならず、
     * 判定が恒久的に落ち着かなくなるのを防ぐため、時刻で古いものを外す ([animatingNow])。
     */
    private val runningAnimations = mutableMapOf<WindowInsetsAnimation, Long>()

    /**
     * アニメーションの走行を追う口を画面に取り付ける。
     *
     * 配送方式は subtree へ流し続ける方を選び、[WindowInsetsAnimation.Callback.onProgress] は
     * 受け取った insets をそのまま返す — 観測を足したことで画面に配られる insets の中身や
     * 配送先を変えないため。ただし**コールバックが登録されていること自体**は framework から
     * 見えており (走行を組み立てるときに登録の有無を問い合わせる)、IME の出入りの時間軸は
     * 観測の有無で変わりうる。観測を付けた版と付けない版の所要時間を比べる用途には使えない。
     */
    fun attach() = onMain {
        val callback = object : WindowInsetsAnimation.Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE) {
            override fun onPrepare(animation: WindowInsetsAnimation) {
                if (animation.isIme()) {
                    markRunning(animation)
                    history.record("IME のアニメーションの準備")
                }
            }

            override fun onStart(
                animation: WindowInsetsAnimation,
                bounds: WindowInsetsAnimation.Bounds,
            ): WindowInsetsAnimation.Bounds {
                if (animation.isIme()) {
                    markRunning(animation)
                    history.record("IME のアニメーションの開始")
                }
                return bounds
            }

            override fun onProgress(
                insets: WindowInsets,
                running: MutableList<WindowInsetsAnimation>,
            ): WindowInsets = insets

            override fun onEnd(animation: WindowInsetsAnimation) {
                if (animation.isIme()) {
                    runningAnimations.remove(animation)
                    history.record("IME のアニメーションの終了")
                }
            }
        }
        activity.window.decorView.setWindowInsetsAnimationCallback(callback)
        history.record("観測を開始")
    }

    /** 取り付けた口を外す。 */
    fun detach() = onMain {
        activity.window.decorView.setWindowInsetsAnimationCallback(null)
        runningAnimations.clear()
    }

    /** 出来事を履歴へ積む。 */
    fun record(label: String) = history.record(label)

    /** IME の今の見えを読む。読みは main で行い、直前から変わっていれば履歴へも積む。 */
    fun readState(): State = onMain {
        val decorView = activity.window.decorView
        val insets = decorView.rootWindowInsets
        val state = State(
            hasInsets = insets != null,
            visible = insets != null && insets.isVisible(WindowInsets.Type.ime()),
            bottom = insets?.getInsets(WindowInsets.Type.ime())?.bottom ?: -1,
            animating = animatingNow(),
            viewFocus = inputField.hasFocus(),
            windowFocus = decorView.hasWindowFocus(),
            windowToken = inputField.windowToken?.toString() ?: "なし",
        )
        history.recordChange(state.toString())
        state
    }

    /** ソフトキーボードを出すよう要求する。届いたかどうかの判定はこの呼び出しでは行わない。 */
    fun requestShow() = onMain {
        inputField.requestFocus()
        val manager = activity.getSystemService(InputMethodManager::class.java)
        val accepted = manager.showSoftInput(inputField, 0)
        history.record("showSoftInput を呼んだ (戻り値 $accepted)")
    }

    /**
     * ソフトキーボードを引っ込めるよう要求する。
     *
     * 戻り値は履歴に残すだけで判定には使わない。この呼び出しは要求の配送先を照合した後で
     * ウィンドウの insets 制御へ合流するため、要求がそこで捨てられた回も true を返す
     * (届いたかどうかの判別材料にならない)。
     */
    fun requestHide() = onMain {
        val manager = activity.getSystemService(InputMethodManager::class.java)
        val accepted = manager.hideSoftInputFromWindow(inputField.windowToken, 0)
        history.record("hideSoftInputFromWindow を呼んだ (戻り値 $accepted)")
    }

    /**
     * 最初の出し入れを始める前に、画面が落ち着いていることを確かめる。
     *
     * 画面の起動に伴ってシステム側が出す非表示要求は、テストが最初に出す表示要求と交差すると
     * その表示をレイアウト後の段階で中止させる。前のテストが IME を出したまま抜けた回も
     * ここで引っ張られるので、まず [restoreHidden] で見えを揃えてから、
     * 「IME が出ていない・アニメーションが走っていない・ウィンドウがフォーカスを持つ」が
     * 数フレーム続くまで待つ。
     */
    suspend fun assertIdleBeforeFirstRequest(reason: String) {
        restoreHidden()
        val idle = InstrumentedStateSettling.awaitSettled(IDLE_TIMEOUT_MILLIS) {
            readState().isSettled(false)
        }
        assertTrue(report(reason, false, IDLE_TIMEOUT_MILLIS), idle)
    }

    /** IME が目的の見えで落ち着くまで待ち、落ち着かなければ観測履歴を添えて落とす。 */
    suspend fun assertSettled(target: Boolean, reason: String) {
        val settled = InstrumentedStateSettling.awaitSettled(TIMEOUT_MILLIS) {
            readState().isSettled(target)
        }
        assertTrue(report(reason, target, TIMEOUT_MILLIS), settled)
    }

    /**
     * IME が出たままなら引っ込める。落ち着いたかどうかの判定はしない。
     *
     * 途中で落ちた回に IME を出したまま抜けると、次のテストが最初に起こす操作 (戻るの注入・
     * 最初の表示要求) をその IME が横取りして、1 件の失敗が 2 件の赤に化ける。
     * 非表示要求は要求可視性の側で捨てられうるので、それだけに頼らず入力欄のフォーカスと
     * ウィンドウの insets 制御からも引っ込める。
     */
    suspend fun restoreHidden() {
        onMain {
            inputField.clearFocus()
            val manager = activity.getSystemService(InputMethodManager::class.java)
            manager.hideSoftInputFromWindow(inputField.windowToken, 0)
            activity.window.insetsController?.hide(WindowInsets.Type.ime())
            history.record("後始末として IME を引っ込めた")
        }
        InstrumentedStateSettling.awaitSettled(CLEANUP_TIMEOUT_MILLIS) {
            readState().isSettled(false)
        }
    }

    /** 時間切れの説明文。期待した見えと、そこまでに観測した履歴を並べる。 */
    fun report(reason: String, target: Boolean, timeoutMillis: Long): String {
        val expectedBottom = if (target) "正" else "0"
        return "$reason\n" +
            "期待した見え: isVisible=$target / bottom=$expectedBottom / アニメーション停止" +
            " / この画面が入力の宛先" +
            " (待ちの上限 $timeoutMillis ms)\n" +
            "観測履歴 (数値は観測開始からの経過ミリ秒):\n${history.format()}"
    }

    /** 走行中として記録する。既に記録済みなら開始時刻は書き換えない。 */
    private fun markRunning(animation: WindowInsetsAnimation) {
        runningAnimations.putIfAbsent(animation, SystemClock.uptimeMillis())
    }

    /** 走行中のアニメーションがあるか。終了の通知を取りこぼした古い走行はここで外す。 */
    private fun animatingNow(): Boolean {
        val now = SystemClock.uptimeMillis()
        val stale = runningAnimations.filterValues { now - it > STALE_ANIMATION_MILLIS }.keys
        if (stale.isNotEmpty()) {
            stale.forEach { runningAnimations.remove(it) }
            history.record(
                "IME のアニメーション ${stale.size} 件を走行中から外した" +
                    " (終了の通知が $STALE_ANIMATION_MILLIS ms 以内に届かなかった)",
            )
        }
        return runningAnimations.isNotEmpty()
    }

    private fun WindowInsetsAnimation.isIme(): Boolean =
        (typeMask and WindowInsets.Type.ime()) != 0

    private fun <T> onMain(block: () -> T): T {
        val value = AtomicReference<T>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync { value.set(block()) }
        @Suppress("UNCHECKED_CAST")
        return value.get() as T
    }

    /** IME の見えの一時点。 */
    data class State(
        val hasInsets: Boolean,
        val visible: Boolean,
        val bottom: Int,
        val animating: Boolean,
        val viewFocus: Boolean,
        val windowFocus: Boolean,
        val windowToken: String,
    ) {

        /**
         * 目的の見えで落ち着いているか。
         *
         * 要になるのは **アニメーションが走っていないこと**。表示要求を出したあと走行の終了まで
         * 待ってから次の非表示要求を出すので、「表示の途中に出た非表示要求が保留される」分岐に
         * そもそも入らない。この条件を落とすと、要求可視性が一瞬だけ非表示へ振れる窓を
         * 読み違える経路が戻る。
         *
         * 枠の高さは要求可視性とほぼ同値 — 不可視の種別に対して枠は 0 で返るため、
         * `bottom > 0` は可視であることを含意する。単独では偽の合格を弾けず、
         * 「見えと枠が食い違ったまま先へ進まない」ことだけを担保する。
         *
         * [windowFocus] は上の 3 つと独立に読める観測で、**この画面が入力の宛先である間の
         * 見えを読んでいるか**を見る。宛先が別のウィンドウへ移っている間の IME の見えは
         * この画面の出し入れの結果ではないため、そこで下した判定は次の要求の前提にならない。
         */
        fun isSettled(target: Boolean): Boolean =
            hasInsets && visible == target && (bottom > 0) == target && !animating && windowFocus

        override fun toString(): String =
            "hasInsets=$hasInsets isVisible=$visible bottom=$bottom animating=$animating" +
                " viewFocus=$viewFocus windowFocus=$windowFocus token=$windowToken"
    }

    private companion object {
        /** IME の出し入れを待つ上限 (ミリ秒)。 */
        const val TIMEOUT_MILLIS = 8_000L

        /** 最初の要求を出す前の落ち着きを待つ上限 (ミリ秒)。 */
        const val IDLE_TIMEOUT_MILLIS = 8_000L

        /** 後始末で引っ込むのを待つ上限 (ミリ秒)。判定はしないので短くてよい。 */
        const val CLEANUP_TIMEOUT_MILLIS = 2_000L

        /** 終了の通知が届かない走行を走行中から外すまでの時間 (ミリ秒)。 */
        const val STALE_ANIMATION_MILLIS = 2_000L
    }
}

/**
 * IME の見えを、観測を組み立てずに揃えるための後始末。
 *
 * IME のウィンドウは画面をまたいで残るため、前のテストが出したまま抜けると次のテストの
 * 最初の操作を横取りする。IME を扱わないテストでも、システムの入力を起こす前にここを通す。
 */
internal object ImeWindowCleanup {

    private const val TIMEOUT_MILLIS = 2_000L

    /** IME が出ていれば引っ込める。判定はしない。 */
    suspend fun ensureHidden(activity: Activity, inputField: View) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            inputField.clearFocus()
            activity.getSystemService(InputMethodManager::class.java)
                .hideSoftInputFromWindow(inputField.windowToken, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.window.insetsController?.hide(WindowInsets.Type.ime())
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            // 見えを読む口が無い API レベルでは、要求を出したところまでで打ち切る
            return
        }
        InstrumentedStateSettling.awaitSettled(TIMEOUT_MILLIS) {
            val visible = AtomicReference(true)
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val insets = activity.window.decorView.rootWindowInsets
                visible.set(insets == null || insets.isVisible(WindowInsets.Type.ime()))
            }
            !visible.get()
        }
    }
}
