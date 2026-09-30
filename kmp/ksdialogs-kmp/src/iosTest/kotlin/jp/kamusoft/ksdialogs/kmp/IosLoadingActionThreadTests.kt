package jp.kamusoft.ksdialogs.kmp

import jp.kamusoft.ksdialogs.kmp.support.runPumpingMainLoop
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.interpretObjCPointer
import kotlinx.cinterop.objcPtr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSThread
import platform.UIKit.UIView
import platform.objc.object_getClass
import swiftPMImport.jp.kamusoft.ksdialogs.kmp.KSDInteropLoadingBridge
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** 進捗の受け口を実装した ViewModel。受け取った値を呼ばれた順に記録する。 */
private class ThreadProbeLoadingViewModel : LoadingViewModel, LoadingProgressReceiver {
    /** 受け取った進捗を呼ばれた順に並べたもの。 */
    val receivedProgress: MutableList<Double> = mutableListOf()

    override fun onProgress(progress: Double) {
        receivedProgress += progress
    }
}

/**
 * iOS の委譲面が、スコープ形の処理を指定どおりのスレッドで始めることの実測。
 *
 * 互換面は本物 (Swift の共有インスタンス) を使う。委譲面の切り替えと互換面の受理の順序が
 * 組み合わさったときに、報告と終了の順序が崩れないことも合わせて確かめる。
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosLoadingActionThreadTests {
    private val bridge = KSDInteropLoadingBridge.sharedBridge()
    private val loading: KsLoading = GatewayKsLoading(IosLoadingGateway(bridge))

    /** factory が生成した View の実体。互換面へ渡した中身を生かしておくために保持する。 */
    private val createdProbeViews = mutableListOf<UIView>()

    @Test
    fun `LD-HK-03 UI スレッド外から指定なしで始めると処理は UI スレッドで始まる`() = runPumpingMainLoop {
        registerProbeViewFactory()
        assertFalse(NSThread.isMainThread, "前提: 呼び出し元が UI スレッド外になっていません。")

        try {
            val builtin = loading.start(message = "読み込み中") { NSThread.isMainThread }
            val custom = loading.start(ThreadProbeLoadingViewModel()) { NSThread.isMainThread }

            assertTrue(builtin, "既定ローディングのスコープ形で、処理が UI スレッドで始まりませんでした。")
            assertTrue(custom, "カスタム Loading のスコープ形で、処理が UI スレッドで始まりませんでした。")
        } finally {
            loading.hide()
        }
    }

    @Test
    fun `LD-HK-04 UI スレッドから BACKGROUND で始めると処理は UI スレッド外で始まる`() = runPumpingMainLoop {
        registerProbeViewFactory()

        try {
            val (builtin, custom) = withContext(Dispatchers.Main) {
                assertTrue(NSThread.isMainThread, "前提: 呼び出し元が UI スレッドになっていません。")
                val builtin = loading.start(
                    message = "読み込み中",
                    actionThread = LoadingActionThread.BACKGROUND,
                ) { NSThread.isMainThread }
                val custom = loading.start(
                    ThreadProbeLoadingViewModel(),
                    actionThread = LoadingActionThread.BACKGROUND,
                ) { NSThread.isMainThread }
                builtin to custom
            }

            assertFalse(builtin, "既定ローディングのスコープ形で、処理が UI スレッドで始まりました。")
            assertFalse(custom, "カスタム Loading のスコープ形で、処理が UI スレッドで始まりました。")
        } finally {
            loading.hide()
        }
    }

    @Test
    fun `LD-HK-03 UI スレッドから指定なしで始めても処理は UI スレッドで始まる`() = runPumpingMainLoop {
        try {
            val startedOnMain = withContext(Dispatchers.Main) {
                loading.start(message = "読み込み中") { NSThread.isMainThread }
            }

            assertTrue(startedOnMain, "UI スレッドからの呼び出しで、処理が UI スレッドで始まりませんでした。")
        } finally {
            loading.hide()
        }
    }

    @Test
    fun `LD-HK-04 UI スレッド外から BACKGROUND で始めても処理は UI スレッド外で始まる`() = runPumpingMainLoop {
        try {
            val startedOnMain = loading.start(
                message = "読み込み中",
                actionThread = LoadingActionThread.BACKGROUND,
            ) { NSThread.isMainThread }

            assertFalse(startedOnMain, "UI スレッド外からの呼び出しで、処理が UI スレッドで始まりました。")
        } finally {
            loading.hide()
        }
    }

    @Test
    fun `BACKGROUND で始めて順に報告しすぐ戻っても最後の報告が終了に追い越されない`() =
        runPumpingMainLoop {
            registerProbeViewFactory()
            val expected = (1..10).map { it / 10.0 }

            // 1 回だけでは順序の崩れが偶然表に出ないことがあるため、同じ形を繰り返して確かめる
            repeat(REPETITIONS) { round ->
                val viewModel = ThreadProbeLoadingViewModel()
                try {
                    loading.start(viewModel, actionThread = LoadingActionThread.BACKGROUND) { report ->
                        expected.forEach(report)
                    }
                } finally {
                    loading.hide()
                }

                assertContentEquals(
                    expected,
                    viewModel.receivedProgress,
                    "${round + 1} 回目: 報告した進捗が、報告した順にすべて受け口へ届きませんでした。",
                )
            }
        }

    @Test
    fun `既定の指定で処理の中から UI スレッド外で報告してすぐ戻っても最後の報告が終了に追い越されない`() =
        runPumpingMainLoop {
            registerProbeViewFactory()
            val expected = (1..10).map { it / 10.0 }

            repeat(REPETITIONS) { round ->
                val viewModel = ThreadProbeLoadingViewModel()
                try {
                    loading.start(viewModel) { report ->
                        withContext(Dispatchers.Default) { expected.forEach(report) }
                    }
                } finally {
                    loading.hide()
                }

                assertContentEquals(
                    expected,
                    viewModel.receivedProgress,
                    "${round + 1} 回目: 報告した進捗が、報告した順にすべて受け口へ届きませんでした。",
                )
            }
        }

    @Test
    fun `どちらの指定でも処理の戻り値がそのまま返る`() = runPumpingMainLoop {
        try {
            val onMain = loading.start(message = "読み込み中") { "UI スレッド" }
            val offMain = loading.start(message = "読み込み中", actionThread = LoadingActionThread.BACKGROUND) {
                "UI スレッド外"
            }

            assertEquals("UI スレッド", onMain)
            assertEquals("UI スレッド外", offMain)
        } finally {
            loading.hide()
        }
    }

    /**
     * 共有コードの ViewModel のクラスをキーに、互換面へ View factory を登録する。
     *
     * 互換面の取り込みは ObjC の型を UIKit のものとは別に宣言するため、生成した View は
     * 同じ ObjC オブジェクトを指す handle に読み替えて渡し、実体の生存期間は [createdProbeViews] が握る。
     */
    private fun registerProbeViewFactory() {
        bridge.registerViewFactoryForViewModelClass(
            viewModelClass = assertNotNull(object_getClass(ThreadProbeLoadingViewModel())),
            factory = { _ ->
                val view = UIView()
                createdProbeViews += view
                interpretObjCPointer(view.objcPtr())
            },
        )
    }

    private companion object {
        /** 順序の検証で同じ形を繰り返す回数。 */
        const val REPETITIONS: Int = 20
    }
}
