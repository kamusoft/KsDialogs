package jp.kamusoft.ksdialogs

import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.kamusoft.ksdialogs.support.DialogLayoutTestActivity
import jp.kamusoft.ksdialogs.support.DialogTransitionProbe
import jp.kamusoft.ksdialogs.support.DialogTransitionProbe.Phase.DISMISSAL
import jp.kamusoft.ksdialogs.support.DialogTransitionProbe.Phase.PRESENTATION
import jp.kamusoft.ksdialogs.support.FixedContentSizeView
import jp.kamusoft.ksdialogs.support.InstrumentedDialogWaiting
import jp.kamusoft.ksdialogs.support.PlainTestDialogViewModel
import jp.kamusoft.ksdialogs.support.RecordingDialogPresentationSurface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/**
 * 多段表示の系列を、器を実際にウィンドウへ載せた状態で観察する。
 *
 * 契約ロジックだけを見る `DialogMultiDisplayTests` との違いは、器の段階と出入りの演出まで
 * 観察対象に含めることにある。Android は1枚が1つのウィンドウなので、下の段を閉じても
 * 上の段はウィンドウとして残り続ける (core/ADR-0006)。
 */
@RunWith(AndroidJUnit4::class)
class DialogMultiDisplayPresentationTests {

    @get:Rule
    val activityRule: ActivityScenarioRule<DialogLayoutTestActivity> =
        ActivityScenarioRule(DialogLayoutTestActivity::class.java)

    @Test
    fun PB_MD_04_下の段を先に閉じても上の段は残り後の報告で確定する() = runBlocking<Unit> {
        coroutineScope {
            val series = presentTwoDialogs(this)
            val bottomContainer = series.container(0)
            val topContainer = series.container(1)

            assertTrue(awaitState(topContainer, DialogContainerState.SHOWN))
            // 両段とも演出の添付が器に届いていることを、退出の観察より先に確かめる
            assertEquals(1, series.bottomProbe.callCount(PRESENTATION))
            assertEquals(1, series.topProbe.callCount(PRESENTATION))

            // 下の段の結果報告口へ先に報告する (アプリコードが下の口を保持している場合にだけ起きる状況)
            series.notifier(0).complete(false)
            assertEquals(DialogResult.Completed(false), series.awaitResult(0))

            // 下の段はライブラリ発の閉鎖なので退出の演出を通り、器が撤去される
            assertEquals(1, series.bottomProbe.callCount(DISMISSAL))
            assertEquals(DialogContainerState.REMOVED, bottomContainer.containerState)

            // 上の段は1枚が1つのウィンドウなのでそのまま残る。巻き添えの閉鎖が遅れて来ないことまで見る
            delay(NO_REACTION_WAIT_MILLIS)
            assertEquals(DialogContainerState.SHOWN, topContainer.containerState)
            assertTrue("上の段はウィンドウ上に残る", topContainer.contentView.isAttachedToWindow)
            assertEquals("上の段は退出の演出を通らない", 0, series.topProbe.callCount(DISMISSAL))
            assertEquals("確定した結果は下の段の分だけ", 1, series.settledResults.size)
            assertEquals(listOf(topContainer), series.surface.presentedContainers)

            // 上の段はその後の報告で通常どおり確定する
            series.notifier(1).complete(true)
            assertEquals(DialogResult.Completed(true), series.awaitResult(1))
            assertEquals(1, series.topProbe.callCount(DISMISSAL))
            assertEquals(DialogContainerState.REMOVED, topContainer.containerState)
        }
    }

    @Test
    fun PB_MD_05_器が画面から外れると各_show_が_cancelled_で1回だけ確定する() = runBlocking<Unit> {
        coroutineScope {
            val series = presentTwoDialogs(this)
            val bottomContainer = series.container(0)
            val topContainer = series.container(1)

            assertTrue(awaitState(topContainer, DialogContainerState.SHOWN))
            assertEquals(1, series.bottomProbe.callCount(PRESENTATION))
            assertEquals(1, series.topProbe.callCount(PRESENTATION))

            // 報告を経ずに提示先の画面ごと破棄される状況を、実際の画面の破棄で作る
            activityRule.scenario.moveToState(Lifecycle.State.DESTROYED)

            assertEquals(DialogResult.Cancelled, series.awaitResult(0))
            assertEquals(DialogResult.Cancelled, series.awaitResult(1))

            // 器の消失は演出を伴わない
            assertEquals(0, series.bottomProbe.callCount(DISMISSAL))
            assertEquals(0, series.topProbe.callCount(DISMISSAL))
            assertEquals(DialogContainerState.REMOVED, bottomContainer.containerState)
            assertEquals(DialogContainerState.REMOVED, topContainer.containerState)
            assertTrue("器は画面から外れる", series.surface.presentedContainers.isEmpty())

            // 確定後に報告しても結果は増えない (ちょうど1回)
            series.notifier(0).complete(true)
            series.notifier(1).complete(true)
            delay(NO_REACTION_WAIT_MILLIS)
            assertEquals(2, series.settledResults.size)
            assertTrue(
                "確定した結果はどちらも cancelled",
                series.settledResults.all { it == DialogResult.Cancelled },
            )
        }
    }

    // 組み立て

    /** 2枚重ねた1回分の系列と、その観測に必要な道具。 */
    private class Series(
        val surface: RecordingDialogPresentationSurface,
        val bottomProbe: DialogTransitionProbe,
        val topProbe: DialogTransitionProbe,
        private val showTasks: List<Deferred<DialogResult<Boolean>>>,
        private val notifiers: List<AtomicReference<DialogNotifier<Boolean>>>,
        private val recordedResults: MutableList<DialogResult<Boolean>>,
    ) {
        /** 下から数えて [index] 番目の段の器。 */
        fun container(index: Int): DialogContainer = surface.presentedContainers[index]

        /** 下から数えて [index] 番目の段の結果報告口。 */
        fun notifier(index: Int): DialogNotifier<Boolean> = requireNotNull(notifiers[index].get())

        /** 下から数えて [index] 番目の段の show の結果が配送されるまで待つ。 */
        suspend fun awaitResult(index: Int): DialogResult<Boolean> =
            withTimeout(RESULT_TIMEOUT_MILLIS) { showTasks[index].await() }

        /** これまでに配送された結果を、届いた順に並べたもの。 */
        val settledResults: List<DialogResult<Boolean>>
            get() = synchronized(recordedResults) { recordedResults.toList() }
    }

    /**
     * ダイアログを2枚重ねて表示し、器がウィンドウに載るまで待つ。
     *
     * 段ごとに別の観測用の道具を添付し、どちらの段の演出が動いたかを取り違えないようにする。
     */
    private suspend fun presentTwoDialogs(scope: CoroutineScope): Series {
        val registry = DialogViewRegistry()
        val surface = RecordingDialogPresentationSurface()
        val dialogs = Dialog(registry, surface)
        val bottomProbe = DialogTransitionProbe()
        val topProbe = DialogTransitionProbe()
        val notifiers = listOf(
            AtomicReference<DialogNotifier<Boolean>>(),
            AtomicReference<DialogNotifier<Boolean>>(),
        )
        val recordedResults = mutableListOf<DialogResult<Boolean>>()
        var createdCount = 0

        registry.register(PlainTestDialogViewModel::class) { _, notifier ->
            // 1枚目が下の段、2枚目が上の段になる
            val index = createdCount
            createdCount++
            notifiers[index].set(notifier)
            val probe = if (index == 0) bottomProbe else topProbe
            val size = (CONTENT_SIZE_DP * resources.displayMetrics.density).toInt()
            FixedContentSizeView(context = this, contentWidth = size, contentHeight = size).also {
                it.ksDialogTransition = DialogTransition(
                    presentation = probe.immediateHook(PRESENTATION),
                    dismissal = probe.immediateHook(DISMISSAL),
                )
            }
        }

        fun startShow(message: String): Deferred<DialogResult<Boolean>> = scope.async {
            dialogs.show(PlainTestDialogViewModel(message)).also { result ->
                synchronized(recordedResults) { recordedResults.add(result) }
            }
        }

        val bottomTask = startShow("下")
        check(awaitCondition { surface.presentedContainers.size == 1 }) { "下の段が提示されなかった" }
        val topTask = startShow("上")
        check(awaitCondition { surface.presentedContainers.size == 2 }) { "上の段が提示されなかった" }

        return Series(
            surface = surface,
            bottomProbe = bottomProbe,
            topProbe = topProbe,
            showTasks = listOf(bottomTask, topTask),
            notifiers = notifiers,
            recordedResults = recordedResults,
        )
    }

    /** 器が指定の段階になるまで待つ。 */
    private suspend fun awaitState(container: DialogContainer, state: DialogContainerState): Boolean =
        awaitCondition { container.containerState == state }

    /** 条件が満たされるまで待つ。 */
    private suspend fun awaitCondition(condition: () -> Boolean): Boolean =
        InstrumentedDialogWaiting.waitUntil(condition = condition)

    private companion object {
        /** 中身の一辺 (dp)。 */
        const val CONTENT_SIZE_DP = 160.0

        /** 結果を待つ上限 (ミリ秒)。 */
        const val RESULT_TIMEOUT_MILLIS = 15_000L

        /** 「まだ何も起こらない」ことを確かめるための待ち (ミリ秒)。 */
        const val NO_REACTION_WAIT_MILLIS = 400L
    }
}
