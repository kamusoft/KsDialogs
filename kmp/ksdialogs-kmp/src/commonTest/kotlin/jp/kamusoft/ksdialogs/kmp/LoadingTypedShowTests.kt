package jp.kamusoft.ksdialogs.kmp

import jp.kamusoft.ksdialogs.kmp.support.ConfigurableTestLoadingViewModel
import jp.kamusoft.ksdialogs.kmp.support.DerivedTestLoadingViewModel
import jp.kamusoft.ksdialogs.kmp.support.ExecutionMarkingDispatcher
import jp.kamusoft.ksdialogs.kmp.support.TestLoadingGateway
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.coroutineContext
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * ViewModel の型を渡すローディング表示の検証。
 *
 * 本番の前段 ([GatewayKsLoading]) に委譲面の差し替えを挿し、
 * 生成した ViewModel が実例を渡す表示と同じ経路へ流れることを見る。
 */
class LoadingTypedShowTests {

    @Test
    fun `PB-KT-01 既定エントリと契約 interface 経由で VM factory のレジストリを共有する`() = runTest {
        val gateway = TestLoadingGateway()
        val entry = GatewayKsLoading(gateway)
        val contract: KsLoading = entry

        assertSame(entry.registry, contract.registry, "入口ごとに別のレジストリになりました。")
        entry.registry.registerViewModel(ConfigurableTestLoadingViewModel::class) {
            ConfigurableTestLoadingViewModel(message = "登録した factory の産物")
        }

        contract.show(ConfigurableTestLoadingViewModel::class)

        val shown = gateway.shownViewModels.single() as ConfigurableTestLoadingViewModel
        assertEquals("登録した factory の産物", shown.message, "登録した factory が使われませんでした。")
    }

    @Test
    fun `LD-KT-02 型を渡す show と start が実例を渡す経路へ流れる`() = runTest {
        val gateway = TestLoadingGateway()
        val loading = GatewayKsLoading(gateway)
        loading.registry.registerViewModel(ConfigurableTestLoadingViewModel::class) {
            ConfigurableTestLoadingViewModel()
        }

        loading.show(
            ConfigurableTestLoadingViewModel::class,
            placement = DialogPlacement(verticalAlignment = DialogAlignment.END, offsetY = 8.0),
        ) { viewModel -> viewModel.message = "表示の configure" }
        val received = loading.start(
            ConfigurableTestLoadingViewModel::class,
            placement = DialogPlacement(offsetX = 4.0),
            configure = { viewModel -> viewModel.message = "スコープ形の configure" },
        ) { report ->
            report(0.5)
            "完了"
        }

        assertEquals("完了", received, "処理の戻り値がそのまま返りませんでした。")
        assertEquals(1, gateway.scopedStartCount, "スコープ形の委譲が行われませんでした。")
        assertContentEquals(
            listOf("表示の configure", "スコープ形の configure"),
            gateway.shownViewModels.map { (it as ConfigurableTestLoadingViewModel).message },
        )
        assertContentEquals(
            listOf(
                DialogPlacement(verticalAlignment = DialogAlignment.END, offsetY = 8.0),
                DialogPlacement(offsetX = 4.0),
            ),
            gateway.shownPlacements,
        )
    }

    @Test
    fun `PB-KT-05 VM factory 未登録の型を渡す表示は構成ミスとして失敗する`() = runTest {
        val gateway = TestLoadingGateway()
        val loading = GatewayKsLoading(gateway)

        val showFailure = assertFailsWith<DialogException> {
            loading.show(ConfigurableTestLoadingViewModel::class)
        }
        val startFailure = assertFailsWith<DialogException> {
            loading.start(ConfigurableTestLoadingViewModel::class) { "実行された" }
        }

        assertTrue(
            showFailure.message?.contains("No ViewModel factory is registered") == true,
            "未登録が構成ミスとして説明されませんでした: ${showFailure.message}",
        )
        assertTrue(
            startFailure.message?.contains("No ViewModel factory is registered") == true,
            "未登録が構成ミスとして説明されませんでした: ${startFailure.message}",
        )
        assertTrue(gateway.shownViewModels.isEmpty(), "未登録なのに委譲面が呼ばれました。")
        assertEquals(0, gateway.scopedStartCount, "未登録なのに処理が実行されました。")
    }

    @Test
    fun `PB-KT-06 VM factory と configure の失敗は表示に進まず伝播する`() = runTest {
        val gateway = TestLoadingGateway()
        val loading = GatewayKsLoading(gateway)
        var actionRan = false
        loading.registry.registerViewModel(ConfigurableTestLoadingViewModel::class) {
            error("factory の失敗")
        }

        val showFactoryFailure = assertFailsWith<IllegalStateException> {
            loading.show(ConfigurableTestLoadingViewModel::class)
        }
        val startFactoryFailure = assertFailsWith<IllegalStateException> {
            loading.start(ConfigurableTestLoadingViewModel::class) { actionRan = true }
        }

        loading.registry.registerViewModel(ConfigurableTestLoadingViewModel::class) {
            ConfigurableTestLoadingViewModel()
        }
        val showConfigureFailure = assertFailsWith<IllegalStateException> {
            loading.show(ConfigurableTestLoadingViewModel::class) { error("configure の失敗") }
        }
        val startConfigureFailure = assertFailsWith<IllegalStateException> {
            loading.start(
                ConfigurableTestLoadingViewModel::class,
                configure = { error("configure の失敗") },
            ) { actionRan = true }
        }

        assertEquals("factory の失敗", showFactoryFailure.message)
        assertEquals("factory の失敗", startFactoryFailure.message)
        assertEquals("configure の失敗", showConfigureFailure.message)
        assertEquals("configure の失敗", startConfigureFailure.message)
        assertTrue(gateway.shownViewModels.isEmpty(), "前段が失敗したのに委譲面が呼ばれました。")
        assertEquals(0, gateway.scopedStartCount, "前段が失敗したのにスコープ形の委譲が行われました。")
        assertTrue(!actionRan, "前段が失敗したのに処理が実行されました。")
    }

    @Test
    fun `PB-KT-07 VM factory と configure は呼び出し元の文脈で実行される`() = runTest {
        val gateway = TestLoadingGateway()
        val loading = GatewayKsLoading(gateway)
        val dispatcher = ExecutionMarkingDispatcher()
        val createdInsideCallerBlock = mutableListOf<Boolean>()
        val configuredInsideCallerBlock = mutableListOf<Boolean>()
        val configureInterceptors = mutableListOf<ContinuationInterceptor?>()
        loading.registry.registerViewModel(ConfigurableTestLoadingViewModel::class) {
            createdInsideCallerBlock += dispatcher.isRunningBlock
            ConfigurableTestLoadingViewModel()
        }

        withContext(dispatcher) {
            loading.show(ConfigurableTestLoadingViewModel::class) {
                configuredInsideCallerBlock += dispatcher.isRunningBlock
                configureInterceptors += coroutineContext[ContinuationInterceptor]
            }
            loading.start(
                ConfigurableTestLoadingViewModel::class,
                configure = {
                    configuredInsideCallerBlock += dispatcher.isRunningBlock
                    configureInterceptors += coroutineContext[ContinuationInterceptor]
                },
            ) { "完了" }
        }

        assertContentEquals(
            listOf(true, true),
            createdInsideCallerBlock,
            "VM factory が呼び出し元の実行区間の外で実行されました (show / start の順)。",
        )
        assertContentEquals(
            listOf(true, true),
            configuredInsideCallerBlock,
            "configure が呼び出し元の実行区間の外で実行されました (show / start の順)。",
        )
        assertContentEquals(
            listOf(dispatcher, dispatcher),
            configureInterceptors,
            "configure が呼び出し元と違う文脈で実行されました。",
        )
    }

    @Test
    fun `PB-KT-13 VM factory の生成物の型が登録キーと違えば型不一致として失敗する`() = runTest {
        val gateway = TestLoadingGateway()
        val loading = GatewayKsLoading(gateway)
        loading.registry.registerViewModel(ConfigurableTestLoadingViewModel::class) {
            DerivedTestLoadingViewModel()
        }

        val failure = assertFailsWith<DialogException> {
            loading.show(ConfigurableTestLoadingViewModel::class)
        }

        assertTrue(
            failure.message?.contains("DerivedTestLoadingViewModel") == true,
            "型不一致の説明に生成物の型が出ませんでした: ${failure.message}",
        )
        assertTrue(gateway.shownViewModels.isEmpty(), "型不一致なのに委譲面が呼ばれました。")
    }
}
