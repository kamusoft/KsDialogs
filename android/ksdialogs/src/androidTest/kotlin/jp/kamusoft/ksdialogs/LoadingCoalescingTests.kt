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
import jp.kamusoft.ksdialogs.support.LoadingTestViewRecorder
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Loading の公開面と合流の契約 (core/ADR-0024) を、実際にウィンドウへ載る器で確かめる。
 *
 * 表示は 1 プロセスに 1 つで、重なった利用は 1 つの表示に合流する。
 * 渡された処理は表示状態によらず必ず実行され、失敗も合流1件の終了として数える。
 */
@RunWith(AndroidJUnit4::class)
class LoadingCoalescingTests {

    @get:Rule
    val activityRule: ActivityScenarioRule<DialogLayoutTestActivity> =
        ActivityScenarioRule(DialogLayoutTestActivity::class.java)

    // MARK: - 表示の出し入れ

    @Test
    fun LD_CO_01_show_で表示され_hide_で消える() = runBlocking<Unit> {
        val harness = newHarness()

        harness.loading.show()

        assertTrue("show が戻った時点で器は取り付いている", harness.isPresenting)
        val contentView = requireNotNull(harness.contentView)
        val container = requireNotNull(harness.container)

        harness.loading.hide()

        assertFalse(harness.isPresenting)
        assertEquals(DialogContainerState.REMOVED, container.containerState)
        assertNull("中身は器と一緒に外れる", contentView.parent)
    }

    @Test
    fun LD_CO_02_スコープ形は処理完了で自動的に閉じ_処理の戻り値を返す() = runBlocking<Unit> {
        val harness = newHarness()
        val gate = LoadingTestGate()

        coroutineScope {
            val scope = async {
                harness.loading.start { _ ->
                    gate.await()
                    42
                }
            }
            assertTrue("処理の実行中は表示される", harness.waitUntilPresenting())
            gate.open()

            assertEquals(42, scope.await())
        }
        assertFalse(harness.isPresenting)
    }

    @Test
    fun LD_CO_03_重なったスコープ形は1つの表示に合流し_最後の完了で閉じる() = runBlocking<Unit> {
        val harness = newHarness()
        val firstGate = LoadingTestGate()
        val secondGate = LoadingTestGate()

        coroutineScope {
            val first = async { harness.loading.start { _ -> firstGate.await() } }
            assertTrue(harness.waitUntilPresenting())
            val firstContainer = requireNotNull(harness.container)
            val second = async { harness.loading.start { _ -> secondGate.await() } }
            assertTrue(InstrumentedDialogWaiting.waitUntil { harness.coalescedUseCount == 2 })

            assertSame("表示は1つに合流する", firstContainer, harness.container)

            firstGate.open()
            first.await()
            assertTrue("先の完了では消えない", harness.isPresenting)

            secondGate.open()
            second.await()
            assertFalse("後の完了で消える", harness.isPresenting)
        }
    }

    @Test
    fun LD_CO_04_表示中でも渡した処理は必ず実行される() = runBlocking<Unit> {
        val harness = newHarness()

        harness.loading.show()
        val didRun = harness.loading.start { _ -> true }

        assertTrue(didRun)
        assertTrue("show の合流が残るため表示は継続する", harness.isPresenting)
        harness.loading.hide()
    }

    @Test
    fun LD_CO_05_処理の失敗は合流1件の終了として数え_呼び出し元へ伝播する() = runBlocking<Unit> {
        val harness = newHarness()

        val failure = runCatching {
            harness.loading.start<Unit> { _ -> throw LoadingTestScopeException() }
        }.exceptionOrNull()

        assertTrue("失敗が呼び出し元へ伝播する", failure is LoadingTestScopeException)
        assertFalse("失敗でも閉じ漏れしない", harness.isPresenting)
        assertEquals(0, harness.coalescedUseCount)
    }

    @Test
    fun LD_CO_06_hide_は合流数によらず即閉じ_走行中の処理は継続する() = runBlocking<Unit> {
        val harness = newHarness()
        val gate = LoadingTestGate()
        val completions = AtomicInteger(0)

        coroutineScope {
            val scope = async {
                harness.loading.start { _ ->
                    gate.await()
                    completions.incrementAndGet()
                    true
                }
            }
            assertTrue(harness.waitUntilPresenting())

            harness.loading.hide()
            assertFalse(harness.isPresenting)
            assertEquals("処理はまだ走っている", 0, completions.get())

            gate.open()
            assertTrue(scope.await())
            assertEquals("処理は継続して完了する", 1, completions.get())
            assertFalse("完了によって表示が再出現しない", harness.isPresenting)
        }
    }

    // MARK: - メッセージ

    @Test
    fun LD_CO_07_メッセージは後勝ち() = runBlocking<Unit> {
        val harness = newHarness()

        harness.loading.show(message = "A")
        assertEquals("A", harness.builtinText)

        harness.loading.show(message = "B")
        assertEquals("B", harness.builtinText)
        assertEquals(2, harness.coalescedUseCount)

        harness.loading.hide()
    }

    @Test
    fun LD_CO_08_setMessage_は表示中のみ有効で合流に関与しない() = runBlocking<Unit> {
        val harness = newHarness()

        // 非表示中は何も起こらない
        harness.loading.setMessage("無視される")
        assertFalse(harness.isPresenting)
        assertEquals(0, harness.coalescedUseCount)

        harness.loading.show(message = "A")
        harness.loading.setMessage("B")

        assertEquals("B", harness.builtinText)
        assertEquals("setMessage は合流を増やさない", 1, harness.coalescedUseCount)

        // hide の対応は要らず、1件だけの hide で閉じる
        harness.loading.hide()
        assertFalse(harness.isPresenting)
    }

    // MARK: - コンテンツの決まり方

    @Test
    fun LD_CO_09_コンテンツは最初の開始が決める() = runBlocking<Unit> {
        val harness = newHarness()
        val viewRecorder = LoadingTestViewRecorder()
        harness.registry.register(LoadingTestViewModel::class) { _ ->
            FixedContentSizeView(this, CONTENT_WIDTH_PIXELS, CONTENT_HEIGHT_PIXELS)
                .also(viewRecorder::record)
        }
        val customGate = LoadingTestGate()
        val builtinGate = LoadingTestGate()

        coroutineScope {
            val custom = async {
                harness.loading.start(LoadingTestViewModel()) { _ -> customGate.await() }
            }
            assertTrue(harness.waitUntilPresenting())
            val builtin = async { harness.loading.start { _ -> builtinGate.await() } }
            assertTrue(InstrumentedDialogWaiting.waitUntil { harness.coalescedUseCount == 2 })

            assertSame("表示はカスタム View のまま合流する", viewRecorder.lastView, harness.contentView)
            assertNull(harness.builtinContentView)

            builtinGate.open()
            builtin.await()
            assertTrue(harness.isPresenting)

            customGate.open()
            custom.await()
            assertFalse("両方の終了で表示が消える", harness.isPresenting)
        }
    }

    // MARK: - 表示世代

    @Test
    fun LD_CO_10_hide_後の新しい表示は旧世代の完了で閉じない() = runBlocking<Unit> {
        val harness = newHarness()
        val gateA = LoadingTestGate()
        val gateB = LoadingTestGate()

        coroutineScope {
            val scopeA = async {
                harness.loading.start { _ ->
                    gateA.await()
                    "A"
                }
            }
            assertTrue(harness.waitUntilPresenting())
            harness.loading.hide()

            val scopeB = async {
                harness.loading.start { _ ->
                    gateB.await()
                    "B"
                }
            }
            assertTrue(InstrumentedDialogWaiting.waitUntil { harness.coalescedUseCount == 1 })
            assertTrue(harness.waitUntilPresenting())

            gateA.open()
            assertEquals("旧世代の戻り値は元の呼び出し元へ返る", "A", scopeA.await())
            assertTrue("A の完了で B の表示は閉じない", harness.isPresenting)

            gateB.open()
            assertEquals("B", scopeB.await())
            assertFalse("B の完了で閉じる", harness.isPresenting)
        }
    }

    @Test
    fun LD_CO_11_旧世代の遅延進捗は新しい表示に届かない() = runBlocking<Unit> {
        val harness = newHarness()
        val gateA = LoadingTestGate()
        val gateB = LoadingTestGate()
        val staleReport = AtomicReference<(Double) -> Unit>()

        coroutineScope {
            val scopeA = async {
                harness.loading.start(message = "A") { report ->
                    staleReport.set(report)
                    gateA.await()
                }
            }
            assertTrue(InstrumentedDialogWaiting.waitUntil { staleReport.get() != null })
            harness.loading.hide()

            val scopeB = async {
                harness.loading.start(message = "B") { report ->
                    report(0.5)
                    gateB.await()
                }
            }
            assertTrue(InstrumentedDialogWaiting.waitUntil { harness.builtinText == "B\n50%" })

            // 旧世代の A から報告する。受理は UI スレッドへ移して行われるため、
            // 届く可能性のある猶予を与えてから観察する
            staleReport.get()(0.9)
            InstrumentedDialogWaiting.waitUntil(STALE_REPORT_GRACE_MILLIS) {
                harness.builtinText != "B\n50%"
            }

            assertEquals("新世代の表示内容は変わらない", "B\n50%", harness.builtinText)
            assertEquals("旧世代の利用は新世代のカウントに入らない", 1, harness.coalescedUseCount)

            gateA.open()
            scopeA.await()
            gateB.open()
            scopeB.await()
        }
    }

    // MARK: - 完了時点

    @Test
    fun LD_CO_12_hide_と最終_start_は撤去完了後に戻る() = runBlocking<Unit> {
        val harness = newHarness()

        harness.loading.show()
        val container = requireNotNull(harness.container)
        val contentView = requireNotNull(harness.contentView)

        harness.loading.hide()

        assertEquals("戻った時点で器は撤去済み", DialogContainerState.REMOVED, container.containerState)
        assertNull("操作ブロックは解除されている", contentView.parent)

        // 合流最後の start も同じく撤去の完了まで待って戻る
        val observed = AtomicReference<LoadingContainer>()
        harness.loading.start { _ -> observed.set(requireNotNull(harness.container)) }
        assertEquals(DialogContainerState.REMOVED, requireNotNull(observed.get()).containerState)
        assertFalse(harness.isPresenting)
    }

    @Test
    fun LD_CO_13_出の途中の新しい開始は出の完了後に新世代として表示される() = runBlocking<Unit> {
        val harness = newHarness()

        harness.loading.show(message = "A")
        val firstContainer = requireNotNull(harness.container)
        // 入りの演出を終えてから閉じる。実効値が固まる前に閉じると出の演出は走らず即時に撤去され、
        // 「出の途中」という状況そのものが成立しない
        assertTrue(
            InstrumentedDialogWaiting.waitUntil {
                firstContainer.containerState == DialogContainerState.SHOWN
            },
        )

        coroutineScope {
            val hiding = async { harness.loading.hide() }
            // 出の演出が始まっていることを器の段階で確かめてから、新しい表示を開始する
            assertTrue(
                InstrumentedDialogWaiting.waitUntil {
                    firstContainer.containerState == DialogContainerState.DISMISSING
                },
            )
            harness.loading.show()
            hiding.await()
        }

        val secondContainer = requireNotNull(harness.container)
        assertNotSame("新世代として器が作り直される", firstContainer, secondContainer)
        assertEquals("旧世代は撤去されている", DialogContainerState.REMOVED, firstContainer.containerState)
        assertNull("旧世代のメッセージを引き継がない", harness.builtinText)

        harness.loading.hide()
    }

    // MARK: - 提示環境と入口

    @Test
    fun LD_CO_14_提示環境が無くても_action_は実行される() = runBlocking<Unit> {
        val harness = LoadingTestHarness(hostContext = null)

        val value = harness.loading.start { _ -> 7 }

        assertEquals("表示の不成立を理由に保留・放棄されない", 7, value)
        assertFalse(harness.isPresenting)
        assertNull(harness.contentView)
    }

    @Test
    fun LD_CO_15_異なる入口からの利用は1つの表示に合流する() = runBlocking<Unit> {
        val harness = newHarness()
        // 契約 interface から構築した別インスタンス (DI 注入で使う形)
        val injected: KsLoading = Loading(harness.coordinator)
        val gate = LoadingTestGate()

        coroutineScope {
            harness.loading.show(message = "A")
            val firstContainer = requireNotNull(harness.container)
            val scope = async { injected.start(message = "B") { _ -> gate.await() } }
            assertTrue(InstrumentedDialogWaiting.waitUntil { harness.coalescedUseCount == 2 })

            assertSame("入口ごとに別の表示は出ない", firstContainer, harness.container)
            assertEquals("B", harness.builtinText)

            gate.open()
            scope.await()
            assertTrue("最後の終了まで表示は続く", harness.isPresenting)

            injected.hide()
            assertFalse(harness.isPresenting)
        }

        // 既定の入口どうしは同じ状態の正を指す
        assertSame(Loading.instance.coordinator, Loading().coordinator)
    }

    /** 提示先を実際の画面にした harness を組み立てる。 */
    private fun newHarness(): LoadingTestHarness {
        val harness = AtomicReference<LoadingTestHarness>()
        activityRule.scenario.onActivity { activity: Activity ->
            harness.set(LoadingTestHarness(activity))
        }
        return requireNotNull(harness.get())
    }

    /** スコープ形の処理の失敗を表すための例外。 */
    private class LoadingTestScopeException : RuntimeException("テスト用の失敗")

    private companion object {
        /** カスタム Loading View の内容サイズ (px)。 */
        const val CONTENT_WIDTH_PIXELS = 240
        const val CONTENT_HEIGHT_PIXELS = 160

        /** 旧世代の報告が届き得る猶予 (ミリ秒)。 */
        const val STALE_REPORT_GRACE_MILLIS = 200L
    }
}
