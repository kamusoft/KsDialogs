package jp.kamusoft.ksdialogs

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import jp.kamusoft.ksdialogs.support.ConfigurableLoadingTestViewModel
import jp.kamusoft.ksdialogs.support.DialogLayoutTestActivity
import jp.kamusoft.ksdialogs.support.FixedContentSizeView
import jp.kamusoft.ksdialogs.support.InstrumentedDialogWaiting
import jp.kamusoft.ksdialogs.support.LoadingLayoutObservation
import jp.kamusoft.ksdialogs.support.LoadingTestHarness
import jp.kamusoft.ksdialogs.support.LoadingTypedShowRecorder
import jp.kamusoft.ksdialogs.support.SecondaryConfigurableLoadingTestViewModel
import jp.kamusoft.ksdialogs.support.ValueClassLoadingTestViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * ViewModel の型を渡して表示する Loading の経路 (core/ADR-0035) を確かめる。
 * iOS Native の LoadingTypedShowTests のミラー。
 *
 * レジストリは View factory と ViewModel factory の2スロットを持ち、
 * 表示は「ViewModel 生成 → configure の完了 → 進捗の受け口の紐付け → 中身の生成 → 提示」の順で進む。
 * 生成と configure の失敗は提示に進まず呼び出し元へ伝わる。
 */
@RunWith(AndroidJUnit4::class)
class LoadingTypedShowTests {

    @get:Rule
    val activityRule: ActivityScenarioRule<DialogLayoutTestActivity> =
        ActivityScenarioRule(DialogLayoutTestActivity::class.java)

    /** configure と ViewModel factory が投げる、テスト専用の失敗。 */
    private class TypedShowFailure(message: String) : RuntimeException(message)

    // MARK: - レジストリの2スロット

    @Test
    fun LD_TY_01_VM_factory_の再登録は_View_factory_を保持する() = runBlocking<Unit> {
        val harness = newHarness()
        val recorder = LoadingTypedShowRecorder()
        registerBothSlots(harness, recorder, factoryTitle = "最初の VM")

        // ViewModel factory だけを登録し直す。View factory のスロットは触らない
        harness.registry.registerViewModel(ConfigurableLoadingTestViewModel::class) {
            ConfigurableLoadingTestViewModel("後の VM").also(recorder::recordCreation)
        }

        harness.loading.show(ConfigurableLoadingTestViewModel::class)

        assertEquals("新しい VM factory の生成物が使われる", listOf("後の VM"), recorder.observedTitles)
        assertEquals("View factory は登録時のものが使われる", 1, recorder.viewCreationCount)
        assertSame(recorder.lastView, harness.contentView)
        harness.loading.hide()

        // 逆向き。View factory だけを登録し直しても ViewModel factory は残る
        val secondRecorder = LoadingTypedShowRecorder()
        harness.registry.register(ConfigurableLoadingTestViewModel::class) { viewModel ->
            newContentView().also { secondRecorder.recordView(viewModel.title, it) }
        }

        harness.loading.show(ConfigurableLoadingTestViewModel::class)

        assertEquals("ViewModel factory は保持されている", listOf("後の VM"), secondRecorder.observedTitles)
        harness.loading.hide()
    }

    @Test
    fun LD_TY_02_表示中の再登録は出ている_Loading_に影響しない() = runBlocking<Unit> {
        val harness = newHarness()
        val recorder = LoadingTypedShowRecorder()
        registerBothSlots(harness, recorder, factoryTitle = "最初の VM")
        val laterRecorder = LoadingTypedShowRecorder()

        val presentedView = AtomicReference<android.view.View>()
        harness.loading.start(ConfigurableLoadingTestViewModel::class) { report ->
            presentedView.set(harness.contentView)
            // 表示中に両スロットを登録し直す
            registerBothSlots(harness, laterRecorder, factoryTitle = "後の VM")
            report(PROGRESS_VALUE)
            awaitProgress(displayedViewModel(recorder))
        }

        assertSame("表示中の中身は変わらない", presentedView.get(), recorder.lastView)
        assertEquals(
            "進捗の転送先も変わらない",
            listOf(PROGRESS_VALUE),
            displayedViewModel(recorder).receivedProgress,
        )
        assertEquals("再登録後の factory は使われない", 0, laterRecorder.viewCreationCount)

        harness.loading.show(ConfigurableLoadingTestViewModel::class)

        assertEquals("次の表示から新しい登録が使われる", listOf("後の VM"), laterRecorder.observedTitles)
        harness.loading.hide()
    }

    // MARK: - 型を渡す表示

    @Test
    fun LD_TY_03_型指定_show_が生成から_configure_表示の一連で動く() = runBlocking<Unit> {
        val harness = newHarness()
        val recorder = LoadingTypedShowRecorder()
        registerBothSlots(harness, recorder)

        harness.loading.show(ConfigurableLoadingTestViewModel::class) { viewModel ->
            viewModel.title = "configure で設定"
        }

        assertEquals("configure の設定が中身の生成から読める", listOf("configure で設定"), recorder.observedTitles)
        assertSame(recorder.lastView, harness.contentView)
        assertEquals("合流1件を開始する", 1, harness.coalescedUseCount)

        harness.loading.hide()
    }

    @Test
    fun LD_TY_04_非同期_configure_の完了まで_View_生成が始まらない() = runBlocking<Unit> {
        val harness = newHarness()
        val recorder = LoadingTypedShowRecorder()
        registerBothSlots(harness, recorder)
        val gate = CompletableDeferred<Unit>()
        val configureStarted = CompletableDeferred<Unit>()

        coroutineScope {
            val showTask = async {
                harness.loading.show(ConfigurableLoadingTestViewModel::class) { viewModel ->
                    configureStarted.complete(Unit)
                    gate.await()
                    viewModel.title = "門を通ってから設定"
                }
            }
            configureStarted.await()

            assertEquals("configure 完了前は中身が作られない", 0, recorder.viewCreationCount)
            assertFalse("表示も始まらない", harness.isPresenting)

            gate.complete(Unit)
            showTask.await()
        }

        assertEquals(listOf("門を通ってから設定"), recorder.observedTitles)
        assertTrue(harness.waitUntilPresenting())
        harness.loading.hide()
    }

    @Test
    fun LD_TY_05_VM_factory_未登録の型指定_show_は構成ミスとして失敗する() = runBlocking<Unit> {
        val harness = newHarness()
        val recorder = LoadingTypedShowRecorder()
        registerViewFactory(harness, recorder)

        val failure = runCatching {
            harness.loading.show(ConfigurableLoadingTestViewModel::class)
        }.exceptionOrNull()

        assertTrue(
            "ViewModel factory 未登録の構成ミスとして失敗する",
            failure is DialogException.ViewModelFactoryNotRegistered,
        )
        assertEquals(0, recorder.viewCreationCount)
        assertFalse("表示されない", harness.isPresenting)
        assertEquals("合流数も変わらない", 0, harness.coalescedUseCount)
    }

    @Test
    fun LD_TY_06_configure_の失敗は提示に進まず伝播する() = runBlocking<Unit> {
        val harness = newHarness()
        val recorder = LoadingTypedShowRecorder()
        registerBothSlots(harness, recorder)

        val failure = runCatching {
            harness.loading.show(ConfigurableLoadingTestViewModel::class) {
                throw TypedShowFailure("configure の失敗")
            }
        }.exceptionOrNull()

        assertTrue("configure の失敗がそのまま伝播する", failure is TypedShowFailure)
        assertEquals("中身は作られない", 0, recorder.viewCreationCount)
        assertFalse("表示されない", harness.isPresenting)
        assertEquals(0, harness.coalescedUseCount)
    }

    @Test
    fun LD_TY_07_configure_省略の型指定_show_は生成物をそのまま表示する() = runBlocking<Unit> {
        val harness = newHarness()
        val recorder = LoadingTypedShowRecorder()
        registerBothSlots(harness, recorder, factoryTitle = "factory の既定")

        harness.loading.show(ConfigurableLoadingTestViewModel::class)

        assertEquals(listOf("factory の既定"), recorder.observedTitles)
        assertSame(recorder.lastView, harness.contentView)
        harness.loading.hide()
    }

    @Test
    fun LD_TY_08_型指定_show_で生成した_VM_にも進捗が転送される() = runBlocking<Unit> {
        val harness = newHarness()
        val recorder = LoadingTypedShowRecorder()
        registerBothSlots(harness, recorder)

        harness.loading.start(ConfigurableLoadingTestViewModel::class) { report ->
            report(PROGRESS_VALUE)
            awaitProgress(displayedViewModel(recorder))
        }

        assertEquals(
            "生成された VM の受け口へ届く",
            listOf(PROGRESS_VALUE),
            displayedViewModel(recorder).receivedProgress,
        )
    }

    // MARK: - 型を渡すスコープ形

    @Test
    fun LD_TY_09_型指定_start_が戻り値を返し合流1件を対で数える() = runBlocking<Unit> {
        val harness = newHarness()
        val recorder = LoadingTypedShowRecorder()
        registerBothSlots(harness, recorder)

        val value = harness.loading.start(ConfigurableLoadingTestViewModel::class) { _ ->
            assertTrue("処理中は表示されている", harness.isPresenting)
            assertEquals(1, harness.coalescedUseCount)
            SCOPE_RESULT
        }

        assertEquals("処理の戻り値がそのまま返る", SCOPE_RESULT, value)
        assertEquals("終了で合流1件が減る", 0, harness.coalescedUseCount)
        assertFalse(harness.isPresenting)
    }

    @Test
    fun LD_TY_10_型指定_start_の_VM_factory_未登録は処理を実行しない() = runBlocking<Unit> {
        val harness = newHarness()
        val recorder = LoadingTypedShowRecorder()
        registerViewFactory(harness, recorder)
        val actionRuns = AtomicInteger(0)

        val failure = runCatching {
            harness.loading.start(ConfigurableLoadingTestViewModel::class) { _ ->
                actionRuns.incrementAndGet()
            }
        }.exceptionOrNull()

        assertTrue(failure is DialogException.ViewModelFactoryNotRegistered)
        assertEquals("処理ブロックは実行されない", 0, actionRuns.get())
        assertFalse(harness.isPresenting)
        assertEquals(0, harness.coalescedUseCount)
    }

    @Test
    fun LD_TY_11_型指定_show_の置き場所引数が提示に渡る() = runBlocking<Unit> {
        val harness = newHarness()
        val recorder = LoadingTypedShowRecorder()
        registerBothSlots(harness, recorder)

        harness.loading.show(ConfigurableLoadingTestViewModel::class)
        val centered = settledContentRect(harness)
        harness.loading.hide()

        harness.loading.show(
            ConfigurableLoadingTestViewModel::class,
            placement = DialogPlacement(
                horizontalAlignment = DialogAlignment.START,
                verticalAlignment = DialogAlignment.START,
                offsetX = OFFSET_DP,
                offsetY = OFFSET_DP,
            ),
        )
        val placed = settledContentRect(harness)
        harness.loading.hide()

        assertNear("x が placement 引数どおりでない", toPixels(OFFSET_DP), placed.left)
        assertNear("y が placement 引数どおりでない", toPixels(OFFSET_DP), placed.top)
        assertNotEquals(centered.left, placed.left)
        assertNotEquals(centered.top, placed.top)
    }

    @Test
    fun LD_TY_12_型指定_show_は呼び出し時点のエントリで_View_まで作る() = runBlocking<Unit> {
        val harness = newHarness()
        val firstRecorder = LoadingTypedShowRecorder()
        registerBothSlots(harness, firstRecorder, factoryTitle = "最初の VM")
        val secondRecorder = LoadingTypedShowRecorder()
        val gate = CompletableDeferred<Unit>()
        val configureStarted = CompletableDeferred<Unit>()

        coroutineScope {
            val showTask = async {
                harness.loading.show(ConfigurableLoadingTestViewModel::class) { _ ->
                    configureStarted.complete(Unit)
                    gate.await()
                }
            }
            configureStarted.await()
            registerBothSlots(harness, secondRecorder, factoryTitle = "後の VM")
            gate.complete(Unit)
            showTask.await()
        }

        assertEquals(
            "最初に取得した VM factory と View factory の組で表示される",
            listOf("最初の VM"),
            firstRecorder.observedTitles,
        )
        assertEquals("再登録後の VM factory は使われない", 0, secondRecorder.createdViewModels.size)
        assertEquals("再登録後の View factory も使われない", 0, secondRecorder.viewCreationCount)
        harness.loading.hide()
    }

    @Test
    fun LD_TY_13_VM_factory_の失敗は提示に進まず伝播する() = runBlocking<Unit> {
        val harness = newHarness()
        val recorder = LoadingTypedShowRecorder()
        registerViewFactory(harness, recorder)
        val configureRuns = AtomicInteger(0)
        harness.registry.registerViewModel(ConfigurableLoadingTestViewModel::class) {
            throw TypedShowFailure("ViewModel factory の失敗")
        }

        val failure = runCatching {
            harness.loading.show(ConfigurableLoadingTestViewModel::class) {
                configureRuns.incrementAndGet()
            }
        }.exceptionOrNull()

        assertTrue("ViewModel factory の失敗がそのまま伝播する", failure is TypedShowFailure)
        assertEquals("configure は呼ばれない", 0, configureRuns.get())
        assertEquals("中身も作られない", 0, recorder.viewCreationCount)
        assertFalse(harness.isPresenting)
        assertEquals(0, harness.coalescedUseCount)
    }

    // MARK: - 合流

    @Test
    fun LD_TY_14_表示中の型指定_start_は既存の表示に合流する() = runBlocking<Unit> {
        val harness = newHarness()
        val firstRecorder = LoadingTypedShowRecorder()
        registerBothSlots(harness, firstRecorder, factoryTitle = "最初の VM")
        val secondRecorder = LoadingTypedShowRecorder()
        registerSecondaryBothSlots(harness, secondRecorder)

        // 先に型指定 show で1件目を表示しておく
        harness.loading.show(ConfigurableLoadingTestViewModel::class)
        val presentedView = harness.contentView

        harness.loading.start(
            SecondaryConfigurableLoadingTestViewModel::class,
            configure = { viewModel -> viewModel.title = "合流側の configure" },
        ) { report ->
            assertEquals("合流数が1増える", 2, harness.coalescedUseCount)
            assertSame("中身は最初の開始のまま", presentedView, harness.contentView)
            report(PROGRESS_VALUE)
            awaitProgress(displayedViewModel(firstRecorder))
        }

        assertEquals("処理の終了で合流数が1減る", 1, harness.coalescedUseCount)
        assertEquals(
            "合流側でも VM factory と configure は実行される",
            listOf("合流側の configure"),
            secondRecorder.createdViewModels.map { (it as SecondaryConfigurableLoadingTestViewModel).title },
        )
        assertEquals("合流側の View factory は呼ばれない", 0, secondRecorder.viewCreationCount)
        assertEquals(
            "進捗は表示中のコンテンツへ届く",
            listOf(PROGRESS_VALUE),
            displayedViewModel(firstRecorder).receivedProgress,
        )

        harness.loading.hide()
    }

    @Test
    fun LD_TY_15_非同期_configure_の間に別の開始が表示を確定すると合流側になる() = runBlocking<Unit> {
        val harness = newHarness()
        val typedRecorder = LoadingTypedShowRecorder()
        registerBothSlots(harness, typedRecorder)
        val instanceRecorder = LoadingTypedShowRecorder()
        harness.registry.register(SecondaryConfigurableLoadingTestViewModel::class) { viewModel ->
            newContentView().also { instanceRecorder.recordView(viewModel.title, it) }
        }
        val gate = CompletableDeferred<Unit>()
        val configureStarted = CompletableDeferred<Unit>()

        coroutineScope {
            val showTask = async {
                harness.loading.show(ConfigurableLoadingTestViewModel::class) { _ ->
                    configureStarted.complete(Unit)
                    gate.await()
                }
            }
            configureStarted.await()

            // configure の完了前に、別の開始が表示を確定させる
            harness.loading.show(SecondaryConfigurableLoadingTestViewModel("インスタンス渡し"))
            assertEquals(1, harness.coalescedUseCount)

            gate.complete(Unit)
            showTask.await()
        }

        assertEquals("型指定 show 側は合流側になる", 2, harness.coalescedUseCount)
        assertEquals("型指定側の View factory は呼ばれない", 0, typedRecorder.viewCreationCount)
        assertEquals(
            "中身は先に開始した show のもの",
            listOf("インスタンス渡し"),
            instanceRecorder.observedTitles,
        )
        assertSame(instanceRecorder.lastView, harness.contentView)

        harness.loading.hide()
    }

    // MARK: - 参照型限定

    @Test
    fun LD_YA_03_value_class_の_VM_は登録と型指定_show_start_で拒否される() = runBlocking<Unit> {
        val harness = newHarness()

        val registrationFailure = runCatching {
            harness.registry.registerViewModel(ValueClassLoadingTestViewModel::class) {
                ValueClassLoadingTestViewModel("値型")
            }
        }.exceptionOrNull()
        assertTrue(registrationFailure is DialogException.ValueClassViewModel)

        val showFailure = runCatching {
            harness.loading.show(ValueClassLoadingTestViewModel::class)
        }.exceptionOrNull()
        assertTrue(showFailure is DialogException.ValueClassViewModel)

        val actionRuns = AtomicInteger(0)
        val startFailure = runCatching {
            harness.loading.start(ValueClassLoadingTestViewModel::class) { _ ->
                actionRuns.incrementAndGet()
            }
        }.exceptionOrNull()
        assertTrue(startFailure is DialogException.ValueClassViewModel)

        assertEquals("処理ブロックは実行されない", 0, actionRuns.get())
        assertFalse(harness.isPresenting)
    }

    // MARK: - 補助

    /** View factory と ViewModel factory の両方を登録する。 */
    private fun registerBothSlots(
        harness: LoadingTestHarness,
        recorder: LoadingTypedShowRecorder,
        factoryTitle: String = "factory の既定",
    ) {
        harness.registry.registerViewModel(ConfigurableLoadingTestViewModel::class) {
            ConfigurableLoadingTestViewModel(factoryTitle).also(recorder::recordCreation)
        }
        harness.registry.register(ConfigurableLoadingTestViewModel::class) { viewModel ->
            newContentView().also { recorder.recordView(viewModel.title, it) }
        }
    }

    /** 合流の相手に使う、もう1つの型の両スロットを登録する。 */
    private fun registerSecondaryBothSlots(
        harness: LoadingTestHarness,
        recorder: LoadingTypedShowRecorder,
    ) {
        harness.registry.registerViewModel(SecondaryConfigurableLoadingTestViewModel::class) {
            SecondaryConfigurableLoadingTestViewModel().also(recorder::recordCreation)
        }
        harness.registry.register(SecondaryConfigurableLoadingTestViewModel::class) { viewModel ->
            newContentView().also { recorder.recordView(viewModel.title, it) }
        }
    }

    /** View factory だけを登録する (ViewModel factory 未登録の状況を作る)。 */
    private fun registerViewFactory(
        harness: LoadingTestHarness,
        recorder: LoadingTypedShowRecorder,
    ) {
        harness.registry.register(ConfigurableLoadingTestViewModel::class) { viewModel ->
            newContentView().also { recorder.recordView(viewModel.title, it) }
        }
    }

    /** ViewModel factory が最初に作った ViewModel。進捗の転送先の確認に使う。 */
    private fun displayedViewModel(recorder: LoadingTypedShowRecorder): ConfigurableLoadingTestViewModel =
        recorder.createdViewModels.first() as ConfigurableLoadingTestViewModel

    /** 進捗が受け口へ届くまで待つ。転送は UI スレッドへ渡ってから行われる。 */
    private suspend fun awaitProgress(viewModel: ConfigurableLoadingTestViewModel) {
        assertTrue(
            "進捗が受け口へ届かなかった",
            InstrumentedDialogWaiting.waitUntil { viewModel.receivedProgress.isNotEmpty() },
        )
    }

    /** 位置の期待値を端末に依存させないための、内容サイズ固定の中身。 */
    private fun Context.newContentView(): FixedContentSizeView =
        FixedContentSizeView(this, CONTENT_SIZE_PIXELS, CONTENT_SIZE_PIXELS).apply {
            ksDialogOptions = WINDOW_AREA_WITHOUT_MARGIN
        }

    /** 実効値の固定とレイアウトを待ってから、中身の外形 (器の面の座標) を読む。 */
    private fun settledContentRect(harness: LoadingTestHarness): Rect {
        val container = requireNotNull(harness.container)
        LoadingLayoutObservation.awaitSettled(container)
        return LoadingLayoutObservation.contentRect(container)
    }

    private fun newHarness(): LoadingTestHarness {
        val harness = AtomicReference<LoadingTestHarness>()
        activityRule.scenario.onActivity { activity: Activity ->
            harness.set(LoadingTestHarness(activity))
        }
        return requireNotNull(harness.get())
    }

    /** 画素密度の丸めを吸収する許容差で一致を確かめる。 */
    private fun assertNear(message: String, expected: Float, actual: Int) {
        assertTrue(
            "$message (期待 ${expected.roundToInt()} 実測 $actual)",
            abs(expected - actual) <= TOLERANCE_PIXELS,
        )
    }

    private fun toPixels(dp: Double): Float {
        val density = LoadingLayoutObservation.readOnMain {
            InstrumentationRegistry.getInstrumentation()
                .targetContext.resources.displayMetrics.density
        }
        return (dp * density).toFloat()
    }

    private companion object {
        /** カスタム Loading View の内容サイズ (px)。 */
        const val CONTENT_SIZE_PIXELS = 200

        /** 基準領域をウィンドウ全体・余白 0 にした属性。端末の画面サイズに依らない期待値にするため。 */
        val WINDOW_AREA_WITHOUT_MARGIN = DialogOptions(
            layoutArea = DialogLayoutArea.WINDOW,
            dialogMargin = DialogEdgeInsets.ZERO,
        )

        /** 置き場所の確認に使うずらし幅 (dp)。 */
        const val OFFSET_DP = 12.0

        /** 位置の比較で許す差 (px)。 */
        const val TOLERANCE_PIXELS = 2

        /** 転送を確かめるために報告する進捗値。 */
        const val PROGRESS_VALUE = 0.6

        /** スコープ形の戻り値。 */
        const val SCOPE_RESULT = "処理の戻り値"
    }
}
