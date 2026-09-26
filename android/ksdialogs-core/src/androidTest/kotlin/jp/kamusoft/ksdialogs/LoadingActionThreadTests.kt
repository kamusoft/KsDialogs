package jp.kamusoft.ksdialogs

import android.app.Activity
import android.content.Context
import android.os.Looper
import android.view.View
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.kamusoft.ksdialogs.support.ConfigurableLoadingTestViewModel
import jp.kamusoft.ksdialogs.support.DialogLayoutTestActivity
import jp.kamusoft.ksdialogs.support.FixedContentSizeView
import jp.kamusoft.ksdialogs.support.LoadingTestHarness
import jp.kamusoft.ksdialogs.support.LoadingTestViewModel
import jp.kamusoft.ksdialogs.support.ProgressReceivingLoadingTestViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/**
 * スコープ形の処理が始まるスレッド (core/ADR-0037) を確かめる。iOS Native の LoadingActionThreadTests のミラー。
 *
 * 既定では、呼び出し元のスレッドに関係なく処理は UI スレッドで始まる。
 * [LoadingActionThread.BACKGROUND] を指定すると、呼び出し元に関係なく UI スレッド外で始まる。
 * どちらも処理の最初の文で実行スレッドを読み取り、その値を処理の戻り値として持ち帰って判定する。
 * 「UI スレッドから呼ぶ」は Main dispatcher へ移して、「UI スレッド外から呼ぶ」は Default dispatcher へ
 * 移して呼び出す。
 */
@RunWith(AndroidJUnit4::class)
class LoadingActionThreadTests {

    @get:Rule
    val activityRule: ActivityScenarioRule<DialogLayoutTestActivity> =
        ActivityScenarioRule(DialogLayoutTestActivity::class.java)

    /** 呼び出し元のスレッドと、処理の最初の文を実行したスレッドの組。 */
    private data class StartObservation(
        val calledOnMainThread: Boolean,
        val startedOnMainThread: Boolean,
    )

    /** 処理の中から投げる、テスト専用の失敗。 */
    private class ActionFailure : RuntimeException("処理の失敗")

    // MARK: - 既定ローディング

    @Test
    fun LD_TH_01_既定では_UI_スレッドから呼んでも_UI_スレッドで始まる() = runBlocking<Unit> {
        val harness = newHarness()

        val observation = withContext(Dispatchers.Main) {
            StartObservation(
                calledOnMainThread = isMainThread(),
                startedOnMainThread = harness.loading.start(message = "読み込み中") { _ -> isMainThread() },
            )
        }

        assertTrue("呼び出しは UI スレッドから行っている", observation.calledOnMainThread)
        assertTrue("処理は UI スレッドで始まる", observation.startedOnMainThread)
    }

    @Test
    fun LD_TH_02_既定では_UI_スレッド外から呼んでも_UI_スレッドで始まる() = runBlocking<Unit> {
        val harness = newHarness()

        val observation = withContext(Dispatchers.Default) {
            StartObservation(
                calledOnMainThread = isMainThread(),
                startedOnMainThread = harness.loading.start(message = "読み込み中") { _ -> isMainThread() },
            )
        }

        assertFalse("呼び出しは UI スレッド外から行っている", observation.calledOnMainThread)
        assertTrue("処理は UI スレッドで始まる", observation.startedOnMainThread)
    }

    @Test
    fun LD_TH_03_UI_スレッド外の指定では_UI_スレッドから呼んでも_UI_スレッド外で始まる() = runBlocking<Unit> {
        val harness = newHarness()

        val observation = withContext(Dispatchers.Main) {
            StartObservation(
                calledOnMainThread = isMainThread(),
                startedOnMainThread = harness.loading.start(
                    message = "読み込み中",
                    actionThread = LoadingActionThread.BACKGROUND,
                ) { _ -> isMainThread() },
            )
        }

        assertTrue("呼び出しは UI スレッドから行っている", observation.calledOnMainThread)
        assertFalse("処理は UI スレッド外で始まる", observation.startedOnMainThread)
    }

    @Test
    fun LD_TH_04_UI_スレッド外の指定では_UI_スレッド外から呼んでも_UI_スレッド外で始まる() = runBlocking<Unit> {
        val harness = newHarness()

        val observation = withContext(Dispatchers.Default) {
            StartObservation(
                calledOnMainThread = isMainThread(),
                startedOnMainThread = harness.loading.start(
                    message = "読み込み中",
                    actionThread = LoadingActionThread.BACKGROUND,
                ) { _ -> isMainThread() },
            )
        }

        assertFalse("呼び出しは UI スレッド外から行っている", observation.calledOnMainThread)
        assertFalse("処理は UI スレッド外で始まる", observation.startedOnMainThread)
    }

    // MARK: - カスタム View の入口

    @Test
    fun LD_TH_05_カスタム_View_の入口でも_既定は_UI_スレッドで始まる() = runBlocking<Unit> {
        val harness = newHarness()
        registerCustomViewModels(harness)

        val (calledOnMainThread, starts) = withContext(Dispatchers.Default) {
            isMainThread() to startCustomEntries(harness, actionThread = null)
        }

        assertFalse("呼び出しは UI スレッド外から行っている", calledOnMainThread)
        assertEquals("3 つの入口をすべて通っている", CUSTOM_ENTRY_COUNT, starts.size)
        for ((entry, startedOnMainThread) in starts) {
            assertTrue("$entry: 処理は UI スレッドで始まる", startedOnMainThread)
        }
    }

    @Test
    fun LD_TH_06_カスタム_View_の入口でも_UI_スレッド外の指定が効く() = runBlocking<Unit> {
        val harness = newHarness()
        registerCustomViewModels(harness)

        val (calledOnMainThread, starts) = withContext(Dispatchers.Main) {
            isMainThread() to startCustomEntries(harness, LoadingActionThread.BACKGROUND)
        }

        assertTrue("呼び出しは UI スレッドから行っている", calledOnMainThread)
        assertEquals("3 つの入口をすべて通っている", CUSTOM_ENTRY_COUNT, starts.size)
        for ((entry, startedOnMainThread) in starts) {
            assertFalse("$entry: 処理は UI スレッド外で始まる", startedOnMainThread)
        }
    }

    // MARK: - 既存の契約との両立

    @Test
    fun LD_TH_07_UI_スレッド外で始まった_action_からの進捗報告が届く() = runBlocking<Unit> {
        val harness = newHarness()
        harness.registry.register(ProgressReceivingLoadingTestViewModel::class) { _ ->
            FixedContentSizeView(this, CONTENT_WIDTH_PIXELS, CONTENT_HEIGHT_PIXELS)
        }

        // 報告と終了の順序は間欠的にしか崩れないため、繰り返して安定を見る
        withContext(Dispatchers.Main) {
            repeat(PROGRESS_ORDER_ATTEMPTS) { attempt ->
                val viewModel = ProgressReceivingLoadingTestViewModel()

                val startedOnMainThread = harness.loading.start(
                    viewModel,
                    actionThread = LoadingActionThread.BACKGROUND,
                ) { report ->
                    val startedOnMainThread = isMainThread()
                    report(0.25)
                    report(0.5)
                    report(1.0)
                    startedOnMainThread
                }

                assertFalse("${attempt + 1} 回目: 処理は UI スレッド外で始まっている", startedOnMainThread)
                assertEquals(
                    "${attempt + 1} 回目: 報告は報告した順に届き、最後の報告が終了に追い越されない",
                    listOf(0.25, 0.5, 1.0),
                    viewModel.receivedProgress,
                )
            }
        }
    }

    /**
     * 既定の指定 (UI スレッドで始まる) の処理が、UI スレッド外へ移って報告し、その直後に戻る経路の回帰ガード。
     *
     * UI スレッド外からの報告は受理を待つ列に積まれる。移った先の処理が素早く終わると処理は UI スレッドの列を
     * 経由せずにその場で再開し得るため、終了の受理がその場で走ると積まれた報告を追い越す。
     * 始まるスレッドの指定によらず、最後の報告が終了に追い越されないことを確かめる。
     */
    @Test
    fun 既定の指定で_UI_スレッド外から報告して直後に戻っても最終進捗が終了に追い越されない() = runBlocking<Unit> {
        val harness = newHarness()
        harness.registry.register(ProgressReceivingLoadingTestViewModel::class) { _ ->
            FixedContentSizeView(this, CONTENT_WIDTH_PIXELS, CONTENT_HEIGHT_PIXELS)
        }

        // 報告と終了の順序は間欠的にしか崩れないため、繰り返して安定を見る
        withContext(Dispatchers.Main) {
            repeat(PROGRESS_ORDER_ATTEMPTS) { attempt ->
                val viewModel = ProgressReceivingLoadingTestViewModel()

                val startedOnMainThread = harness.loading.start(viewModel) { report ->
                    val startedOnMainThread = isMainThread()
                    withContext(Dispatchers.Default) {
                        report(0.25)
                        report(0.5)
                        report(1.0)
                    }
                    startedOnMainThread
                }

                assertTrue("${attempt + 1} 回目: 処理は UI スレッドで始まっている", startedOnMainThread)
                assertEquals(
                    "${attempt + 1} 回目: 報告は報告した順に届き、最後の報告が終了に追い越されない",
                    listOf(0.25, 0.5, 1.0),
                    viewModel.receivedProgress,
                )
            }
        }
    }

    @Test
    fun LD_TH_08_どちらの指定でも_action_の失敗は伝播して表示が閉じる() = runBlocking<Unit> {
        val harness = newHarness()

        for (actionThread in LoadingActionThread.entries) {
            val failure = runCatching {
                harness.loading.start<Unit>(message = "読み込み中", actionThread = actionThread) { _ ->
                    throw ActionFailure()
                }
            }.exceptionOrNull()

            assertTrue("$actionThread: 処理の失敗がそのまま呼び出し元へ伝わる", failure is ActionFailure)
            assertEquals("$actionThread: 合流1件の終了として数えられる", 0, harness.coalescedUseCount)
            assertFalse("$actionThread: Loading は閉じる", harness.isPresenting)
        }
    }

    // MARK: - 補助

    /** カスタム View の 3 つの入口が使う登録を済ませる。 */
    private fun registerCustomViewModels(harness: LoadingTestHarness) {
        harness.registry.register(LoadingTestViewModel::class) { _ ->
            FixedContentSizeView(this, CONTENT_WIDTH_PIXELS, CONTENT_HEIGHT_PIXELS)
        }
        harness.registry.registerViewModel(ConfigurableLoadingTestViewModel::class) {
            ConfigurableLoadingTestViewModel()
        }
        harness.registry.register(ConfigurableLoadingTestViewModel::class) { _ ->
            FixedContentSizeView(this, CONTENT_WIDTH_PIXELS, CONTENT_HEIGHT_PIXELS)
        }
    }

    /**
     * カスタム View の 3 つの入口 (インスタンス渡し・インライン・型指定) を順に開始し、
     * 入口ごとに処理が UI スレッドで始まったかを返す。
     *
     * @param actionThread 渡す指定。null なら指定を省略して呼ぶ
     */
    private suspend fun startCustomEntries(
        harness: LoadingTestHarness,
        actionThread: LoadingActionThread?,
    ): Map<String, Boolean> {
        val loading = harness.loading
        val starts = linkedMapOf<String, Boolean>()
        starts["インスタンス渡し"] = if (actionThread == null) {
            loading.start(LoadingTestViewModel()) { _ -> isMainThread() }
        } else {
            loading.start(LoadingTestViewModel(), actionThread = actionThread) { _ -> isMainThread() }
        }
        val inlineFactory: Context.(LoadingTestViewModel) -> View = { _ ->
            FixedContentSizeView(this, CONTENT_WIDTH_PIXELS, CONTENT_HEIGHT_PIXELS)
        }
        starts["インライン"] = if (actionThread == null) {
            loading.start(LoadingTestViewModel(), factory = inlineFactory) { _ -> isMainThread() }
        } else {
            loading.start(
                LoadingTestViewModel(),
                factory = inlineFactory,
                actionThread = actionThread,
            ) { _ -> isMainThread() }
        }
        starts["型指定"] = if (actionThread == null) {
            loading.start(ConfigurableLoadingTestViewModel::class) { _ -> isMainThread() }
        } else {
            loading.start(
                ConfigurableLoadingTestViewModel::class,
                actionThread = actionThread,
            ) { _ -> isMainThread() }
        }
        return starts
    }

    private fun isMainThread(): Boolean = Looper.myLooper() === Looper.getMainLooper()

    private fun newHarness(): LoadingTestHarness {
        val harness = AtomicReference<LoadingTestHarness>()
        activityRule.scenario.onActivity { activity: Activity ->
            harness.set(LoadingTestHarness(activity))
        }
        return requireNotNull(harness.get())
    }

    private companion object {
        /** カスタム Loading View の内容サイズ (px)。 */
        const val CONTENT_WIDTH_PIXELS = 240
        const val CONTENT_HEIGHT_PIXELS = 160

        /** カスタム View のスコープ形の入口の数 (インスタンス渡し・インライン・型指定)。 */
        const val CUSTOM_ENTRY_COUNT = 3

        /**
         * 報告と終了の前後関係を見る反復回数。
         *
         * 追い越しは処理の完了と呼び出し元の中断の競争で起きるため、1 回あたりの発生率は低い。
         * 1 回のテスト実行で検出できるよう多めに回す。
         */
        const val PROGRESS_ORDER_ATTEMPTS = 40
    }
}
