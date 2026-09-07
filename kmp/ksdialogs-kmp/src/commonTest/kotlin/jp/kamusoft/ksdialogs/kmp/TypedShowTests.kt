package jp.kamusoft.ksdialogs.kmp

import jp.kamusoft.ksdialogs.kmp.support.ConfigurableTestDialogViewModel
import jp.kamusoft.ksdialogs.kmp.support.DerivedTestDialogViewModel
import jp.kamusoft.ksdialogs.kmp.support.ExecutionMarkingDispatcher
import jp.kamusoft.ksdialogs.kmp.support.TestDialogGateway
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * ViewModel の型を渡すダイアログ表示の検証。
 *
 * 本番の前段 ([GatewayKsDialog]) に委譲面の差し替えを挿し、
 * ViewModel factory の解決・configure の適用・失敗の伝播を見る。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TypedShowTests {

    @Test
    fun `PB-KT-01 既定エントリと契約 interface 経由で VM factory のレジストリを共有する`() = runTest {
        val gateway = TestDialogGateway.completing(true)
        val entry = GatewayKsDialog(gateway)
        val contract: KsDialog = entry

        assertSame(entry.registry, contract.registry, "入口ごとに別のレジストリになりました。")
        entry.registry.registerViewModel(ConfigurableTestDialogViewModel::class) {
            ConfigurableTestDialogViewModel(message = "登録した factory の産物")
        }

        contract.show(ConfigurableTestDialogViewModel::class)

        val shown = gateway.presentedViewModels.single() as ConfigurableTestDialogViewModel
        assertEquals("登録した factory の産物", shown.message, "登録した factory が使われませんでした。")
    }

    @Test
    fun `PB-KT-01 既定エントリのレジストリはどの入口から取っても同じものになる`() {
        val dialogContract: KsDialog = Dialog.instance
        val loadingContract: KsLoading = Loading.instance
        val toastContract: KsToast = Toast.instance

        assertSame(Dialog.instance.registry, dialogContract.registry)
        assertSame(Loading.instance.registry, loadingContract.registry)
        assertSame(Toast.instance.registry, toastContract.registry)
    }

    @Test
    fun `PB-KT-02 VM factory の再登録は後勝ちで表示中の show には影響しない`() = runTest {
        val gateway = TestDialogGateway.completing(true)
        val dialogs = GatewayKsDialog(gateway)
        val configureGate = CompletableDeferred<Unit>()
        dialogs.registry.registerViewModel(ConfigurableTestDialogViewModel::class) {
            ConfigurableTestDialogViewModel(message = "最初の factory")
        }

        val inFlight = launch {
            dialogs.show(ConfigurableTestDialogViewModel::class) { configureGate.await() }
        }
        runCurrent()
        dialogs.registry.registerViewModel(ConfigurableTestDialogViewModel::class) {
            ConfigurableTestDialogViewModel(message = "後から登録した factory")
        }
        configureGate.complete(Unit)
        inFlight.join()
        dialogs.show(ConfigurableTestDialogViewModel::class)

        val messages = gateway.presentedViewModels.map { (it as ConfigurableTestDialogViewModel).message }
        assertEquals(listOf("最初の factory", "後から登録した factory"), messages)
    }

    @Test
    fun `PB-KT-03 型を渡す show が生成 configure 結果の一連で動く`() = runTest {
        val gateway = TestDialogGateway.completing(true)
        val dialogs = GatewayKsDialog(gateway)
        dialogs.registry.registerViewModel(ConfigurableTestDialogViewModel::class) {
            ConfigurableTestDialogViewModel()
        }

        val result = dialogs.show(
            ConfigurableTestDialogViewModel::class,
            placement = DialogPlacement(horizontalAlignment = DialogAlignment.START, offsetX = 12.0),
        ) { viewModel ->
            viewModel.message = "configure で入れた文言"
        }

        val shown = gateway.presentedViewModels.single() as ConfigurableTestDialogViewModel
        assertEquals("configure で入れた文言", shown.message, "configure の状態が委譲面へ届きませんでした。")
        assertEquals(
            DialogPlacement(horizontalAlignment = DialogAlignment.START, offsetX = 12.0),
            gateway.presentedPlacements.single(),
        )
        assertEquals(DialogResult.Completed(true), result, "宣言結果型の完了が返りませんでした。")
    }

    @Test
    fun `PB-KT-04 非同期 configure の完了まで委譲面に渡らない`() = runTest {
        val gateway = TestDialogGateway.completing(true)
        val dialogs = GatewayKsDialog(gateway)
        val configureGate = CompletableDeferred<Unit>()
        dialogs.registry.registerViewModel(ConfigurableTestDialogViewModel::class) {
            ConfigurableTestDialogViewModel()
        }

        val inFlight = launch {
            dialogs.show(ConfigurableTestDialogViewModel::class) { configureGate.await() }
        }
        runCurrent()

        assertTrue(gateway.presentedViewModels.isEmpty(), "configure の完了前に委譲面が呼ばれました。")

        configureGate.complete(Unit)
        inFlight.join()

        assertEquals(1, gateway.presentedViewModels.size, "configure の完了後に委譲面が呼ばれませんでした。")
    }

    @Test
    fun `PB-KT-05 VM factory 未登録の型を渡す show は構成ミスとして失敗する`() = runTest {
        val gateway = TestDialogGateway.completing(true)
        val dialogs = GatewayKsDialog(gateway)

        val failure = assertFailsWith<DialogException> {
            dialogs.show(ConfigurableTestDialogViewModel::class)
        }

        assertTrue(
            failure.message?.contains("No ViewModel factory is registered") == true,
            "未登録が構成ミスとして説明されませんでした: ${failure.message}",
        )
        assertTrue(gateway.presentedViewModels.isEmpty(), "未登録なのに委譲面が呼ばれました。")
    }

    @Test
    fun `PB-KT-06 VM factory の失敗は提示に進まず伝播する`() = runTest {
        val gateway = TestDialogGateway.completing(true)
        val dialogs = GatewayKsDialog(gateway)
        dialogs.registry.registerViewModel(ConfigurableTestDialogViewModel::class) {
            error("factory の失敗")
        }

        val failure = assertFailsWith<IllegalStateException> {
            dialogs.show(ConfigurableTestDialogViewModel::class)
        }

        assertEquals("factory の失敗", failure.message)
        assertTrue(gateway.presentedViewModels.isEmpty(), "factory が失敗したのに委譲面が呼ばれました。")
    }

    @Test
    fun `PB-KT-06 configure の失敗とキャンセルは提示に進まず伝播する`() = runTest {
        val gateway = TestDialogGateway.completing(true)
        val dialogs = GatewayKsDialog(gateway)
        dialogs.registry.registerViewModel(ConfigurableTestDialogViewModel::class) {
            ConfigurableTestDialogViewModel()
        }

        val failure = assertFailsWith<IllegalStateException> {
            dialogs.show(ConfigurableTestDialogViewModel::class) { error("configure の失敗") }
        }
        val cancellation = assertFailsWith<CancellationException> {
            dialogs.show(ConfigurableTestDialogViewModel::class) {
                throw CancellationException("configure のキャンセル")
            }
        }

        assertEquals("configure の失敗", failure.message)
        assertEquals("configure のキャンセル", cancellation.message)
        assertTrue(gateway.presentedViewModels.isEmpty(), "configure が失敗したのに委譲面が呼ばれました。")
    }

    @Test
    fun `PB-KT-07 VM factory と configure は呼び出し元の文脈で実行される`() = runTest {
        val gateway = TestDialogGateway.completing(true)
        val dialogs = GatewayKsDialog(gateway)
        val dispatcher = ExecutionMarkingDispatcher()
        var createdInsideCallerBlock = false
        var configuredInsideCallerBlock = false
        var configureInterceptor: ContinuationInterceptor? = null
        dialogs.registry.registerViewModel(ConfigurableTestDialogViewModel::class) {
            createdInsideCallerBlock = dispatcher.isRunningBlock
            ConfigurableTestDialogViewModel()
        }

        withContext(dispatcher) {
            dialogs.show(ConfigurableTestDialogViewModel::class) {
                configuredInsideCallerBlock = dispatcher.isRunningBlock
                configureInterceptor = coroutineContext[ContinuationInterceptor]
            }
        }

        assertTrue(createdInsideCallerBlock, "VM factory が呼び出し元の実行区間の外で実行されました。")
        assertTrue(configuredInsideCallerBlock, "configure が呼び出し元の実行区間の外で実行されました。")
        assertSame(dispatcher, configureInterceptor, "configure が呼び出し元と違う文脈で実行されました。")
    }

    @Test
    fun `PB-KT-08 configure 省略の型を渡す show は VM factory の生成物をそのまま渡す`() = runTest {
        val gateway = TestDialogGateway.completing(true)
        val dialogs = GatewayKsDialog(gateway)
        val created = ConfigurableTestDialogViewModel(message = "生成物そのまま")
        dialogs.registry.registerViewModel(ConfigurableTestDialogViewModel::class) { created }

        dialogs.show(ConfigurableTestDialogViewModel::class)

        assertSame(created, gateway.presentedViewModels.single(), "生成物と別のものが委譲されました。")
    }

    @Test
    fun `PB-KT-12 並行する再登録の途中でも解決は原子的なスナップショットになる`() = runTest {
        val gateway = TestDialogGateway.completing(true)
        val dialogs = GatewayKsDialog(gateway)
        val showCount = 200
        // 両側を同時に走り出させ、互いに相手が動き出したことを見てから残りを回す
        // (先に片方が終わってしまう逐次実行では、この待ち合わせを抜けられない)
        val startGate = CompletableDeferred<Unit>()
        val reregisteringInProgress = CompletableDeferred<Unit>()
        val resolvingInProgress = CompletableDeferred<Unit>()
        dialogs.registry.registerViewModel(ConfigurableTestDialogViewModel::class) {
            ConfigurableTestDialogViewModel(message = "登録 0", generation = 0)
        }

        withContext(Dispatchers.Default) {
            val reregistering = launch {
                startGate.await()
                repeat(showCount) { index ->
                    val generation = index + 1
                    dialogs.registry.registerViewModel(ConfigurableTestDialogViewModel::class) {
                        ConfigurableTestDialogViewModel(message = "登録 $generation", generation = generation)
                    }
                    reregisteringInProgress.complete(Unit)
                    if (index == 0) resolvingInProgress.await()
                }
            }
            val resolving = launch {
                startGate.await()
                repeat(showCount) { index ->
                    // 未登録として失敗すれば例外でここを抜ける (解決が再登録の途中を見た合図になる)
                    dialogs.show(ConfigurableTestDialogViewModel::class)
                    resolvingInProgress.complete(Unit)
                    if (index == 0) reregisteringInProgress.await()
                }
            }
            startGate.complete(Unit)
            reregistering.join()
            resolving.join()
        }

        assertEquals(showCount, gateway.presentedViewModels.size, "解決に失敗した show がありました。")
        val presented = gateway.presentedViewModels.map { it as ConfigurableTestDialogViewModel }
        assertTrue(
            presented.all { it.message == "登録 ${it.generation}" && it.generation in 0..showCount },
            "1つの登録単位に対応しない ViewModel が渡りました: " +
                presented.filterNot { it.message == "登録 ${it.generation}" }.map { it.message },
        )
    }

    @Test
    fun `PB-KT-13 VM factory の生成物の型が登録キーと違えば型不一致として失敗する`() = runTest {
        val gateway = TestDialogGateway.completing(true)
        val dialogs = GatewayKsDialog(gateway)
        dialogs.registry.registerViewModel(ConfigurableTestDialogViewModel::class) {
            DerivedTestDialogViewModel()
        }

        val failure = assertFailsWith<DialogException> {
            dialogs.show(ConfigurableTestDialogViewModel::class)
        }

        assertTrue(
            failure.message?.contains("DerivedTestDialogViewModel") == true,
            "型不一致の説明に生成物の型が出ませんでした: ${failure.message}",
        )
        assertTrue(gateway.presentedViewModels.isEmpty(), "型不一致なのに委譲面が呼ばれました。")
    }
}
