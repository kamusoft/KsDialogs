package jp.kamusoft.ksdialogs

import android.graphics.Color
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.kamusoft.ksdialogs.support.DialogLayoutTestActivity
import jp.kamusoft.ksdialogs.support.DialogTransitionGate
import jp.kamusoft.ksdialogs.support.FixedContentSizeView
import jp.kamusoft.ksdialogs.support.InstrumentedDialogWaiting
import jp.kamusoft.ksdialogs.support.LoadingTestViewModel
import jp.kamusoft.ksdialogs.support.ToastLayoutObservation
import jp.kamusoft.ksdialogs.support.ToastScreenObservation
import jp.kamusoft.ksdialogs.support.ToastTestAnnouncer
import jp.kamusoft.ksdialogs.support.ToastTestHarness
import jp.kamusoft.ksdialogs.support.ToastTestPresentationSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/**
 * Toast の多重表示と表示の継続 (core/ADR-0030) を確かめる。
 * iOS Native の ToastMultiDisplayTests のミラー。
 *
 * 多重起動はすべて表示され、重なりは起動順 (後に起動したものが手前) だけで決まる。
 * 各表示は自分の duration で独立に消え、画面の入れ替わりをまたいでも表示と残り時間は続く。
 * Loading とは、起動順によらず常に Loading が手前になる。
 *
 * ウィンドウの重なり順は載っている一覧を数えても分からないため、実際に描かれた画素で判定する。
 */
@RunWith(AndroidJUnit4::class)
class ToastMultiDisplayTests {

    @get:Rule
    val activityRule: ActivityScenarioRule<DialogLayoutTestActivity> =
        ActivityScenarioRule(DialogLayoutTestActivity::class.java)

    @Test
    fun TS_MX_01_多重起動はすべて表示され起動順に重なる() = runBlocking<Unit> {
        val harness = ToastTestHarness(currentActivity())

        // 同じ文言なら3枚は同じ大きさ・同じ位置に重なるので、見えている色が手前の1枚を指す
        harness.toast.style = opaqueStyle(Color.RED)
        harness.toast.show(STACKED_MESSAGE, durationMs = LONG_DURATION_MILLIS)
        harness.toast.style = opaqueStyle(Color.GREEN)
        harness.toast.show(STACKED_MESSAGE, durationMs = LONG_DURATION_MILLIS)
        harness.toast.style = opaqueStyle(Color.BLUE)
        harness.toast.show(STACKED_MESSAGE, durationMs = LONG_DURATION_MILLIS)

        assertTrue(harness.waitUntilPresenting(count = 3))
        assertEquals("3枚すべてが表示される", 3, harness.displayCount)
        harness.containers.forEach(ToastLayoutObservation::awaitSettled)
        val frontRect = ToastLayoutObservation.contentRectOnScreen(harness.containers.last())

        val seen = ToastScreenObservation.awaitStableMeanColor(frontRect)

        assertTrue(
            "最後に起動した Toast が手前に見えない (実際: ${ToastScreenObservation.describe(seen)})",
            ToastScreenObservation.isClose(seen, Color.BLUE, COLOR_TOLERANCE),
        )
        assertTrue(harness.waitUntilEmpty())
    }

    @Test
    fun TS_MX_02_各表示は自分の_duration_で独立に消える() = runBlocking<Unit> {
        val harness = ToastTestHarness(currentActivity())

        harness.toast.show("短い", durationMs = SHORT_DURATION_MILLIS)
        harness.toast.show("長い", durationMs = LONG_DURATION_MILLIS)
        assertTrue(harness.waitUntilPresenting(count = 2))

        assertTrue(
            "短い方の duration で1枚だけ消える",
            InstrumentedDialogWaiting.waitUntil { harness.displayCount == 1 },
        )
        assertEquals("長い", harness.defaultContentViews.single().displayedText)
        assertTrue(harness.waitUntilEmpty())
    }

    @Test
    fun TS_MX_03_ページ遷移をまたいで表示が継続する() = runBlocking<Unit> {
        val harness = ToastTestHarness(currentActivity())

        harness.toast.show("遷移をまたぐ", durationMs = LONG_DURATION_MILLIS)
        assertTrue(harness.waitUntilPresenting())
        val container = harness.containers.single()
        val contentView = harness.contentViews.single()

        // 器は提示先の View 階層に参加しないため、画面の中身を入れ替えても表示は続く
        // (画面そのものの再生成は TS-AN-03 が受け持つ)
        withContext(Dispatchers.Main) {
            val activity = currentActivity()
            activity.hostContainer.removeAllViews()
            activity.hostContainer.addView(
                TextView(activity).apply { text = "次のページ" },
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                ),
            )
        }

        assertTrue("遷移後も表示され続ける", harness.isPresenting)
        assertSame("器は作り直されない", container, harness.containers.single())
        assertSame(contentView, harness.contentViews.single())
        assertTrue("duration の経過で消える", harness.waitUntilEmpty())
    }

    @Test
    fun TS_MX_04_画面が作り直されても表示が継続し残り時間は維持される() = runBlocking<Unit> {
        val harness = ToastTestHarness(currentActivity())

        harness.toast.show("継続", durationMs = MEDIUM_DURATION_MILLIS)
        assertTrue(harness.waitUntilPresenting())
        val firstContainer = harness.containers.single()
        val contentView = harness.contentViews.single()

        delay(HALFWAY_MILLIS)
        activityRule.scenario.recreate()
        withContext(Dispatchers.Main) { harness.changeHost(currentActivity()) }

        assertTrue("作り直しをまたいで表示が続く", harness.isPresenting)
        assertNotSame("器は作り直される", firstContainer, harness.containers.single())
        assertSame("中身はそのまま載せ替えられる", contentView, harness.contentViews.single())

        // 残り時間が巻き戻っていれば、ここから duration まるごと待つことになる
        assertTrue(
            "残り duration が巻き戻っている",
            InstrumentedDialogWaiting.waitUntil(REMAINING_ALLOWANCE_MILLIS) {
                harness.displayCount == 0
            },
        )
    }

    @Test
    fun TS_MX_05_Loading_は起動順によらず_Toast_より前面() = runBlocking<Unit> {
        assertLoadingCoversToast(showsLoadingFirst = true)
        assertLoadingCoversToast(showsLoadingFirst = false)
    }

    @Test
    fun Loading_の入りの演出の途中で_Toast_を出しても中身の見えが固まらない() = runBlocking<Unit> {
        val activity = currentActivity()
        val harness = ToastTestHarness(activity)
        val presentationGate = DialogTransitionGate()
        harness.loadingCoordinator.registry.register(LoadingTestViewModel::class) { _ ->
            FixedContentSizeView(activity, LOADING_CONTENT_SIZE_PX, LOADING_CONTENT_SIZE_PX).apply {
                // 途中まで進んだ入りの演出を再現する。門が開くまで中身は透明のまま止まる
                ksDialogTransition = DialogTransition(
                    presentation = { view ->
                        view.alpha = 0f
                        presentationGate.await()
                    },
                )
            }
        }

        harness.loading.show(LoadingTestViewModel())
        assertTrue(
            "入りの演出の途中にならない",
            InstrumentedDialogWaiting.waitUntil {
                harness.loadingCoordinator.presentedContainer?.containerState ==
                    DialogContainerState.PRESENTING
            },
        )
        val loadingContent = requireNotNull(harness.loadingCoordinator.presentedContentView)
        val firstContainer = requireNotNull(harness.loadingCoordinator.presentedContainer)

        // 前面化のために Loading の器が載せ替えられる
        harness.toast.show(STACKED_MESSAGE, durationMs = MEDIUM_DURATION_MILLIS)
        assertTrue(harness.waitUntilPresenting())
        assertTrue(
            "前面化の載せ替えが起きない",
            InstrumentedDialogWaiting.waitUntil {
                harness.loadingCoordinator.presentedContainer !== firstContainer
            },
        )

        assertSame(
            "中身は同じものが載せ替えられる",
            loadingContent,
            harness.loadingCoordinator.presentedContentView,
        )
        assertTrue(
            "中身が新しい器の画面に載らない",
            InstrumentedDialogWaiting.waitUntil { loadingContent.isAttachedToWindow },
        )
        assertEquals(
            "Loading の中身が入りの途中の見え (透明) のまま固まっている",
            1f,
            loadingContent.alpha,
            ALPHA_TOLERANCE,
        )

        presentationGate.open()
        harness.loading.hide()
        assertTrue(harness.waitUntilEmpty())
    }

    @Test
    fun 既定の前面化の配線でも_Loading_が_Toast_より前面に戻る() = runBlocking<Unit> {
        val activity = currentActivity()
        // 前面化の依頼口だけは差し替えず、既定の配線 (LoadingCoordinator.shared) をそのまま踏む
        val coordinator = ToastCoordinator(
            registry = ToastViewRegistry(),
            settings = ToastSettings(),
            presentationSurface = ToastTestPresentationSurface(activity),
            announcer = ToastTestAnnouncer(),
        )
        val toast: KsToast = Toast(coordinator)
        val loading: KsLoading = Loading()
        try {
            loading.show("既定の配線")
            assertTrue(
                InstrumentedDialogWaiting.waitUntil {
                    LoadingCoordinator.shared.presentedContainer?.containerState ==
                        DialogContainerState.SHOWN
                },
            )
            val firstContainer = requireNotNull(LoadingCoordinator.shared.presentedContainer)
            val loadingContent = requireNotNull(LoadingCoordinator.shared.presentedContentView)

            toast.show(STACKED_MESSAGE, durationMs = MEDIUM_DURATION_MILLIS)
            assertTrue(InstrumentedDialogWaiting.waitUntil { coordinator.isPresenting })
            assertTrue(
                "既定の配線では Loading の器が載せ直されない (前面化が届いていない)",
                InstrumentedDialogWaiting.waitUntil {
                    LoadingCoordinator.shared.presentedContainer !== firstContainer
                },
            )
            assertSame(
                "中身は同じものが載せ替えられる",
                loadingContent,
                LoadingCoordinator.shared.presentedContentView,
            )
            assertEquals(
                "載せ直しで Loading の中身の見えが失われている",
                1f,
                loadingContent.alpha,
                ALPHA_TOLERANCE,
            )
        } finally {
            loading.hide()
        }
        assertTrue(
            InstrumentedDialogWaiting.waitUntil { coordinator.displayCount == 0 },
        )
    }

    /**
     * Loading と Toast を指定の順で表示し、Toast のピルが Loading の覆いで暗くなることを確かめる。
     *
     * Loading の覆いは画面全体を占めるので、Loading が手前なら Toast のピルは覆いの分だけ暗くなる。
     *
     * @param showsLoadingFirst Loading を先に表示するか
     */
    private suspend fun assertLoadingCoversToast(showsLoadingFirst: Boolean) {
        val order = if (showsLoadingFirst) "Loading が先" else "Toast が先"
        val harness = ToastTestHarness(currentActivity())
        harness.toast.style = opaqueStyle(Color.WHITE)

        // 覆いの無い状態でのピルの見えを先に測る
        harness.toast.show(STACKED_MESSAGE, durationMs = LONG_DURATION_MILLIS)
        assertTrue(harness.waitUntilPresenting())
        val pillRect = harness.containers.single().let {
            ToastLayoutObservation.awaitSettled(it)
            ToastLayoutObservation.contentRectOnScreen(it)
        }
        val withoutLoading = ToastScreenObservation.awaitStableMeanColor(pillRect)

        if (showsLoadingFirst) {
            // Loading 表示中に出した Toast。ウィンドウの追加順では Toast が手前になるため、
            // ここで Loading が前面へ戻ることを見る
            showLoading(harness)
            showAnotherToast(harness)
        } else {
            showAnotherToast(harness)
            showLoading(harness)
        }

        val withLoading = ToastScreenObservation.awaitStableMeanColor(
            ToastLayoutObservation.contentRectOnScreen(harness.containers.last()),
        )

        assertTrue(
            "$order: Loading の覆いが Toast の手前に来ていない " +
                "(覆いなし ${ToastScreenObservation.describe(withoutLoading)} / " +
                "覆いあり ${ToastScreenObservation.describe(withLoading)})",
            ToastScreenObservation.brightness(withoutLoading) -
                ToastScreenObservation.brightness(withLoading) > DIMMED_DIFFERENCE,
        )

        harness.loading.hide()
        assertTrue(harness.waitUntilEmpty())
    }

    /** 既定ローディングを表示し、器が取り付くまで待つ。 */
    private suspend fun showLoading(harness: ToastTestHarness) {
        harness.loading.show()
        assertTrue(
            InstrumentedDialogWaiting.waitUntil { harness.loadingCoordinator.isPresenting },
        )
    }

    /** 同じ位置に重なる2枚目の Toast を表示し、取り付くまで待つ。 */
    private suspend fun showAnotherToast(harness: ToastTestHarness) {
        harness.toast.show(STACKED_MESSAGE, durationMs = LONG_DURATION_MILLIS)
        assertTrue(harness.waitUntilPresenting(count = 2))
    }

    /** 重なりを画素で見分けるための、透けない地色のスタイル。 */
    private fun opaqueStyle(color: Int): ToastStyle =
        ToastStyle(backgroundColor = color, textColor = color)

    private fun currentActivity(): DialogLayoutTestActivity {
        val activity = AtomicReference<DialogLayoutTestActivity>()
        activityRule.scenario.onActivity { activity.set(it) }
        return requireNotNull(activity.get())
    }

    private companion object {
        /** 重なりの検証で使う共通の文言。同じ大きさ・同じ位置に重ねるために揃える。 */
        const val STACKED_MESSAGE = "重なり"

        /** 待ち時間を短く保つための表示時間 (ミリ秒)。 */
        const val SHORT_DURATION_MILLIS = 400

        /** 途中で画面を作り直すために使う表示時間 (ミリ秒)。 */
        const val MEDIUM_DURATION_MILLIS = 3_000

        /** 検証の間ずっと残っていてほしい表示時間 (ミリ秒)。 */
        const val LONG_DURATION_MILLIS = 8_000

        /** 画面を作り直す前に消費させる時間 (ミリ秒)。 */
        const val HALFWAY_MILLIS = 1_500L

        /** 残り時間が維持されていれば足りる待ちの上限 (ミリ秒)。 */
        const val REMAINING_ALLOWANCE_MILLIS = 2_500L

        /** 画素の色を同じとみなす、チャンネルごとの許容差。 */
        const val COLOR_TOLERANCE = 24

        /** 覆いが敷かれたと言える明るさの差。既定の覆いは黒の 40% 不透明。 */
        const val DIMMED_DIFFERENCE = 30.0

        /** 前面化の検証で使うカスタム Loading の中身の大きさ (px)。 */
        const val LOADING_CONTENT_SIZE_PX = 200

        /** 透明度を同じとみなす許容差。 */
        const val ALPHA_TOLERANCE = 0.001f
    }
}
