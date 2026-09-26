package jp.kamusoft.ksdialogs.support

import android.app.Activity
import android.graphics.Rect
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import jp.kamusoft.ksdialogs.ActivityDialogPresentationSurface
import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.DialogAlignment
import jp.kamusoft.ksdialogs.DialogLayoutArea
import jp.kamusoft.ksdialogs.DialogLayoutHost
import jp.kamusoft.ksdialogs.DialogNotifier
import jp.kamusoft.ksdialogs.DialogOptions
import jp.kamusoft.ksdialogs.DialogPlacement
import jp.kamusoft.ksdialogs.DialogResult
import jp.kamusoft.ksdialogs.DialogViewRegistry
import jp.kamusoft.ksdialogs.windowVisibleAreaInsets
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 実際の show でダイアログを出し、表示中のページを基準にした配置を画面座標で観察する。
 *
 * 器 (別ウィンドウ) とページ (Activity のウィンドウ) は原点が違い得るため、観察はすべて画面座標で行う。
 * 期待値は「ページの矩形と器のウィンドウの可視領域の共通部分」から、その場で導く。
 */
internal object DialogCurrentPageStage {

    /** 提示と状態変化を待つ上限 (ミリ秒)。 */
    const val TIMEOUT_MILLIS: Long = 10_000L

    /** 画素の丸めを吸収する許容差 (px)。 */
    const val TOLERANCE_PIXELS: Int = 2

    /** 中身が要求する内容サイズ (dp)。 */
    const val CONTENT_WIDTH_DP: Int = 200
    const val CONTENT_HEIGHT_DP: Int = 120

    /** 下端・右端へ寄せる配置。 */
    val END_END: DialogPlacement = DialogPlacement(
        horizontalAlignment = DialogAlignment.END,
        verticalAlignment = DialogAlignment.END,
    )

    /** 上端・左端へ寄せる配置。 */
    val START_START: DialogPlacement = DialogPlacement(
        horizontalAlignment = DialogAlignment.START,
        verticalAlignment = DialogAlignment.START,
    )

    /**
     * 固定サイズの中身に属性を添付してダイアログを 1 枚出し、実効値が固まるまで待つ。
     *
     * @param scope show を走らせる文脈。観察が済んだら [ShownCurrentPageDialog.close] で閉じる
     * @param layoutArea 添付する基準領域
     * @param placement 添付する配置
     */
    suspend fun show(
        scope: CoroutineScope,
        layoutArea: DialogLayoutArea,
        placement: DialogPlacement,
    ): ShownCurrentPageDialog {
        val dialogs = Dialog(DialogViewRegistry(), ActivityDialogPresentationSurface())
        val presented = CompletableDeferred<Pair<View, DialogNotifier<Boolean>>>()
        val showTask = scope.async {
            dialogs.show(CurrentPageTestDialogViewModel(), placement = null) { _, notifier ->
                val density = resources.displayMetrics.density
                FixedContentSizeView(
                    context = this,
                    contentWidth = (CONTENT_WIDTH_DP * density).roundToInt(),
                    contentHeight = (CONTENT_HEIGHT_DP * density).roundToInt(),
                ).attach(DialogOptions(layoutArea = layoutArea), placement).also {
                    presented.complete(it to notifier)
                }
            }
        }
        val (contentView, notifier) = withTimeout(TIMEOUT_MILLIS) { presented.await() }
        val shown = ShownCurrentPageDialog(contentView, notifier, showTask)
        shown.awaitFrozen()
        return shown
    }

    /** View の外形を画面座標 (px) で読む。 */
    fun <A : Activity> screenRectOf(scenario: ActivityScenario<A>, view: () -> View): Rect {
        val rect = AtomicReference<Rect>()
        scenario.onActivity { rect.set(view().screenRect()) }
        return requireNotNull(rect.get())
    }

    /** 1dp あたりの px 数。 */
    val density: Float
        get() = InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density

    /** dp を px に直す。 */
    fun toPixels(dp: Double): Int = (dp * density).roundToInt()
}

/** 表示中のダイアログ 1 枚。 */
internal class ShownCurrentPageDialog(
    /** 中身の View。 */
    val contentView: View,
    private val notifier: DialogNotifier<Boolean>,
    private val showTask: Deferred<DialogResult<Boolean>>,
) {
    /** 器の面。中身の祖先を辿って見つける。 */
    val host: DialogLayoutHost by lazy {
        var current = contentView.parent
        while (current != null && current !is DialogLayoutHost) {
            current = current.parent
        }
        requireNotNull(current as? DialogLayoutHost) { "器の面が見つからない" }
    }

    /** 中身の外形 (画面座標、px)。 */
    fun contentRect(): Rect = onMain { contentView.screenRect() }

    /** 器の面の可視領域 (画面座標、px)。システムバーを除いた領域。 */
    fun visibleAreaRect(): Rect = onMain {
        val hostRect = host.screenRect()
        val insets = windowVisibleAreaInsets(host)
        Rect(
            hostRect.left + insets.left.roundToInt(),
            hostRect.top + insets.top.roundToInt(),
            hostRect.right - insets.right.roundToInt(),
            hostRect.bottom - insets.bottom.roundToInt(),
        )
    }

    /** 器の面の外形 (画面座標、px)。 */
    fun hostRect(): Rect = onMain { host.screenRect() }

    /** ページが得られずに出した診断。 */
    fun diagnostics(): List<String> = onMain { host.currentPageDiagnostics.toList() }

    /** 実効値が固まり、レイアウトが済むまで待つ。固まらなければ観測履歴を添えて落とす。 */
    suspend fun awaitFrozen() {
        val history = StateHistory()
        val settled = awaitObservedSettled(history) { host.isLayoutSnapshotFrozen && isLayoutSettled() }
        check(settled) { timeoutMessage("ダイアログの実効値が固まらなかった", history) }
    }

    /**
     * 中身の外形が条件を満たして落ち着くまで待ち、落ち着かなければ観測履歴を添えて落とす。
     *
     * 回転のように器とページが別々に置き直される変化では、両方が落ち着くまで待ってから確かめる。
     *
     * @param reason 落ち着かなかったときの説明文の書き出し
     * @param condition 中身の外形 (画面座標、px) が満たすべき条件。UI スレッドで評価する
     */
    suspend fun assertContentRectSettles(reason: String, condition: (Rect) -> Boolean) {
        val history = StateHistory()
        val settled = awaitObservedSettled(history) { isLayoutSettled() && condition(contentView.screenRect()) }
        assertTrue(timeoutMessage(reason, history), settled)
    }

    /**
     * [settled] が成り立ち続けるまで共通の待ち ([InstrumentedStateSettling.awaitSettled]) で待つ。
     *
     * 読むたびに器と中身の観測を [history] へ積む (変化したときだけ)。時間切れの回に何が起きていたかは
     * 事後には読めないため。
     */
    private suspend fun awaitObservedSettled(history: StateHistory, settled: () -> Boolean): Boolean =
        InstrumentedStateSettling.awaitSettled(DialogCurrentPageStage.TIMEOUT_MILLIS) {
            onMain {
                val result = settled()
                history.recordChange("${observation()} satisfied=$result")
                result
            }
        }

    /** 器と中身の今の状態を 1 行にまとめる。UI スレッドで呼ぶ。 */
    private fun observation(): String =
        "content=${contentView.screenRect().toShortString()}" +
            " host=${host.screenRect().toShortString()}" +
            " frozen=${host.isLayoutSnapshotFrozen}" +
            " hostLaidOut=${host.isLaidOut} hostLayoutRequested=${host.isLayoutRequested}" +
            " contentLaidOut=${contentView.isLaidOut} contentLayoutRequested=${contentView.isLayoutRequested}"

    /** 時間切れの説明文。待ちの上限と観測履歴を添える。 */
    private fun timeoutMessage(reason: String, history: StateHistory): String =
        "$reason (待ちの上限 ${DialogCurrentPageStage.TIMEOUT_MILLIS} ms)\n" +
            "観測履歴 (数値は観測開始からの経過ミリ秒):\n${history.format()}"

    /**
     * 器のレイアウトが済み、次のレイアウトも要求されていないか。
     *
     * 器は置き直しを次のフレームへ要求することがあり、UI スレッドが空いた時点でもまだ前の配置のことがある。
     */
    private fun isLayoutSettled(): Boolean =
        host.isLaidOut && contentView.isLaidOut && !host.isLayoutRequested && !contentView.isLayoutRequested

    /** 結果を報告して閉じ、show が返した結果を返す。 */
    suspend fun close(): DialogResult<Boolean> {
        onMain { notifier.complete(true) }
        return withTimeout(DialogCurrentPageStage.TIMEOUT_MILLIS) { showTask.await() }
    }

    private fun <T> onMain(block: () -> T): T {
        val result = AtomicReference<Any?>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync { result.set(block()) }
        @Suppress("UNCHECKED_CAST")
        return result.get() as T
    }
}

/** 表示中のページの配置を確かめるダイアログの ViewModel。状態を持たない。 */
internal class CurrentPageTestDialogViewModel : jp.kamusoft.ksdialogs.DialogViewModel<Boolean>

/** View の外形を画面座標 (px) で返す。 */
internal fun View.screenRect(): Rect {
    val location = IntArray(2)
    getLocationOnScreen(location)
    return Rect(location[0], location[1], location[0] + width, location[1] + height)
}

/** 2 つの値が画素の丸めの範囲で一致することを確かめる。 */
internal fun assertPixelsNear(message: String, expected: Int, actual: Int) {
    assertTrue(
        "$message: 期待 $expected px / 実測 $actual px",
        abs(expected - actual) <= DialogCurrentPageStage.TOLERANCE_PIXELS,
    )
}
