package jp.kamusoft.ksdialogs

import android.app.Activity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.kamusoft.ksdialogs.support.DialogLayoutTestActivity
import jp.kamusoft.ksdialogs.support.FixedContentSizeView
import jp.kamusoft.ksdialogs.support.InstrumentedDialogWaiting
import jp.kamusoft.ksdialogs.support.LoadingTestGate
import jp.kamusoft.ksdialogs.support.LoadingTestHarness
import jp.kamusoft.ksdialogs.support.LoadingTestViewModel
import jp.kamusoft.ksdialogs.support.ProgressReceivingLoadingTestViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/**
 * スコープ形の進捗通知 (core/ADR-0023・0024) を確かめる。iOS Native の LoadingProgressTests のミラー。
 *
 * 値は 0〜1 にクランプされ、非有限値は無視される。表示は最新の報告が勝ち、
 * 既定ローディングではフォーマット関数の結果が、カスタム View では進捗受け口が受け取る。
 */
@RunWith(AndroidJUnit4::class)
class LoadingProgressTests {

    @get:Rule
    val activityRule: ActivityScenarioRule<DialogLayoutTestActivity> =
        ActivityScenarioRule(DialogLayoutTestActivity::class.java)

    @Test
    fun LD_PR_01_進捗報告で既定ローディングの表示が更新される() = runBlocking<Unit> {
        val harness = newHarness()
        val gate = LoadingTestGate()
        val report = AtomicReference<(Double) -> Unit>()

        coroutineScope {
            val scope = async {
                harness.loading.start(message = "読み込み中") { reporter ->
                    report.set(reporter)
                    gate.await()
                }
            }
            assertTrue(harness.waitUntilPresenting())
            assertTrue(InstrumentedDialogWaiting.waitUntil { report.get() != null })

            report.get()(0.45)

            assertTrue(
                InstrumentedDialogWaiting.waitUntil { harness.builtinText == "読み込み中\n45%" },
            )

            gate.open()
            scope.await()
        }
    }

    @Test
    fun LD_PR_02_未報告のあいだはメッセージのみが表示される() = runBlocking<Unit> {
        val harness = newHarness()
        val gate = LoadingTestGate()

        coroutineScope {
            val scope = async {
                harness.loading.start(message = "読み込み中") { _ -> gate.await() }
            }
            assertTrue(harness.waitUntilPresenting())

            assertEquals(
                "既定のフォーマットは進捗なしならメッセージだけを返す",
                "読み込み中",
                harness.builtinText,
            )

            gate.open()
            scope.await()
        }
    }

    @Test
    fun LD_PR_03_合流中は最新の報告が表示される() = runBlocking<Unit> {
        val harness = newHarness()
        val firstGate = LoadingTestGate()
        val secondGate = LoadingTestGate()
        val firstReport = AtomicReference<(Double) -> Unit>()
        val secondReport = AtomicReference<(Double) -> Unit>()

        coroutineScope {
            val first = async {
                harness.loading.start(message = "読み込み中") { reporter ->
                    firstReport.set(reporter)
                    firstGate.await()
                }
            }
            assertTrue(harness.waitUntilPresenting())
            val second = async {
                harness.loading.start { reporter ->
                    secondReport.set(reporter)
                    secondGate.await()
                }
            }
            assertTrue(InstrumentedDialogWaiting.waitUntil { harness.coalescedUseCount == 2 })
            assertTrue(
                InstrumentedDialogWaiting.waitUntil {
                    firstReport.get() != null && secondReport.get() != null
                },
            )

            firstReport.get()(0.2)
            assertTrue(
                InstrumentedDialogWaiting.waitUntil { harness.builtinText == "読み込み中\n20%" },
            )

            secondReport.get()(0.7)
            assertTrue(
                InstrumentedDialogWaiting.waitUntil { harness.builtinText == "読み込み中\n70%" },
            )

            firstReport.get()(0.3)
            assertTrue(
                InstrumentedDialogWaiting.waitUntil { harness.builtinText == "読み込み中\n30%" },
            )

            firstGate.open()
            first.await()
            secondGate.open()
            second.await()
        }
    }

    @Test
    fun LD_PR_04_範囲外_非有限の報告値の扱い() = runBlocking<Unit> {
        val harness = newHarness()
        val gate = LoadingTestGate()
        val report = AtomicReference<(Double) -> Unit>()

        coroutineScope {
            val scope = async {
                harness.loading.start(message = "読み込み中") { reporter ->
                    report.set(reporter)
                    gate.await()
                }
            }
            assertTrue(harness.waitUntilPresenting())
            assertTrue(InstrumentedDialogWaiting.waitUntil { report.get() != null })

            report.get()(1.4)
            assertTrue(
                InstrumentedDialogWaiting.waitUntil { harness.builtinText == "読み込み中\n100%" },
            )

            report.get()(-0.5)
            assertTrue(
                InstrumentedDialogWaiting.waitUntil { harness.builtinText == "読み込み中\n0%" },
            )

            // 非有限値は報告そのものを無視するので、直前の表示が保たれる。
            // 受理は UI スレッドへ移して行われるため、届く可能性のある猶予を与えてから観察する
            report.get()(Double.NaN)
            report.get()(Double.POSITIVE_INFINITY)
            InstrumentedDialogWaiting.waitUntil(NON_FINITE_REPORT_GRACE_MILLIS) {
                harness.builtinText != "読み込み中\n0%"
            }
            assertEquals("読み込み中\n0%", harness.builtinText)

            gate.open()
            scope.await()
        }
    }

    @Test
    fun LD_PR_05_進捗受け口を実装した_VM_のカスタム_View_に進捗が転送される() = runBlocking<Unit> {
        val harness = newHarness()
        harness.registry.register(ProgressReceivingLoadingTestViewModel::class) { _ ->
            FixedContentSizeView(this, CONTENT_WIDTH_PIXELS, CONTENT_HEIGHT_PIXELS)
        }
        val viewModel = ProgressReceivingLoadingTestViewModel()
        val gate = LoadingTestGate()
        val report = AtomicReference<(Double) -> Unit>()

        coroutineScope {
            val scope = async {
                harness.loading.start(viewModel) { reporter ->
                    report.set(reporter)
                    gate.await()
                }
            }
            assertTrue(harness.waitUntilPresenting())
            assertTrue(InstrumentedDialogWaiting.waitUntil { report.get() != null })

            report.get()(0.25)
            assertTrue(
                InstrumentedDialogWaiting.waitUntil { viewModel.receivedProgress == listOf(0.25) },
            )

            // 転送されるのはクランプ後の値で、非有限値は転送されない
            report.get()(2.0)
            assertTrue(
                InstrumentedDialogWaiting.waitUntil {
                    viewModel.receivedProgress == listOf(0.25, 1.0)
                },
            )
            report.get()(Double.NaN)
            InstrumentedDialogWaiting.waitUntil(NON_FINITE_REPORT_GRACE_MILLIS) {
                viewModel.receivedProgress.size > 2
            }
            assertEquals(listOf(0.25, 1.0), viewModel.receivedProgress)

            gate.open()
            scope.await()
        }
    }

    @Test
    fun LD_PR_06_受け口未実装の_VM_では転送されない() = runBlocking<Unit> {
        val harness = newHarness()
        harness.registry.register(LoadingTestViewModel::class) { _ ->
            FixedContentSizeView(this, CONTENT_WIDTH_PIXELS, CONTENT_HEIGHT_PIXELS)
        }
        val gate = LoadingTestGate()
        val report = AtomicReference<(Double) -> Unit>()

        coroutineScope {
            val scope = async {
                harness.loading.start(LoadingTestViewModel()) { reporter ->
                    report.set(reporter)
                    reporter(0.4)
                    gate.await()
                    "完了"
                }
            }
            assertTrue(harness.waitUntilPresenting())
            assertTrue(InstrumentedDialogWaiting.waitUntil { report.get() != null })

            report.get()(0.8)
            InstrumentedDialogWaiting.waitUntil(NON_FINITE_REPORT_GRACE_MILLIS) {
                !harness.isPresenting
            }
            assertTrue("報告は誤りにならず、表示も処理も続く", harness.isPresenting)

            gate.open()
            assertEquals("完了", scope.await())
        }
    }

    /**
     * 報告と終了の前後関係を固定する回帰ガード。
     *
     * 発行済みの報告が受理される前に処理の終了が追い越すと、最後の進捗が表示に届かないまま
     * 表示が畳まれる。この順序は報告の受理と終了が同じ UI スレッドの列に受理順で載ることに
     * 依存しており、載せ方を変えると静かに崩れる。iOS Native の同名テストのミラー。
     */
    @Test
    fun 報告の直後に処理が戻っても最終進捗が終了に追い越されない() = runBlocking<Unit> {
        val harness = newHarness()
        harness.registry.register(ProgressReceivingLoadingTestViewModel::class) { _ ->
            FixedContentSizeView(this, CONTENT_WIDTH_PIXELS, CONTENT_HEIGHT_PIXELS)
        }

        // 報告も終了も UI スレッドから呼ばれる形で見る。受理の載せ方が食い違うと、
        // 一方が即座に走り他方が列に並ぶことで終了が報告を追い越す。
        // 順序が崩れても間欠的にしか壊れないため、繰り返して安定を見る
        withContext(Dispatchers.Main) {
            repeat(FINAL_REPORT_ATTEMPTS) { attempt ->
                val viewModel = ProgressReceivingLoadingTestViewModel()

                harness.loading.start(viewModel) { report ->
                    report(0.5)
                    report(1.0)
                }

                assertEquals(
                    "${attempt + 1} 回目: 発行済みの報告は受理順のまま、終了より先に受け口へ届く",
                    listOf(0.5, 1.0),
                    viewModel.receivedProgress,
                )
            }
        }
    }

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

        /** 無視されるはずの報告が届き得る猶予 (ミリ秒)。 */
        const val NON_FINITE_REPORT_GRACE_MILLIS = 200L

        /** 報告と終了の前後関係を見る反復回数。 */
        const val FINAL_REPORT_ATTEMPTS = 12
    }
}
