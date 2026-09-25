package jp.kamusoft.ksdialogs.kmp

import jp.kamusoft.ksdialogs.kmp.support.ConfigurableTestLoadingViewModel
import jp.kamusoft.ksdialogs.kmp.support.PlainTestLoadingViewModel
import jp.kamusoft.ksdialogs.kmp.support.TestLoadingGateway
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals

/**
 * スコープ形に渡した「処理を始めるスレッド」の指定が、共有コードを素通りして委譲面まで届くことの検証。
 *
 * 共有コードは UI スレッドを知らず、指定を渡すだけである。実際にそのスレッドで始まることは
 * 各 OS の委譲面のテストが受け持つ。
 */
class LoadingActionThreadDelegationTests {

    @Test
    fun `LD-HK-01 3 本の入口とも指定なしでは MAIN が委譲面に届く`() = runTest {
        val gateway = TestLoadingGateway()
        val loading = GatewayKsLoading(gateway)
        loading.registry.registerViewModel(ConfigurableTestLoadingViewModel::class) {
            ConfigurableTestLoadingViewModel()
        }

        loading.start(message = "読み込み中") { }
        loading.start(PlainTestLoadingViewModel()) { }
        loading.start(ConfigurableTestLoadingViewModel::class) { }

        assertContentEquals(
            List(3) { LoadingActionThread.MAIN },
            gateway.startedActionThreads,
            "指定を省いたスコープ形で、既定の MAIN 以外が委譲面に届きました。",
        )
    }

    @Test
    fun `LD-HK-01 3 本の入口とも BACKGROUND を渡すとそのまま委譲面に届く`() = runTest {
        val gateway = TestLoadingGateway()
        val loading = GatewayKsLoading(gateway)
        loading.registry.registerViewModel(ConfigurableTestLoadingViewModel::class) {
            ConfigurableTestLoadingViewModel()
        }

        loading.start(message = "読み込み中", actionThread = LoadingActionThread.BACKGROUND) { }
        loading.start(PlainTestLoadingViewModel(), actionThread = LoadingActionThread.BACKGROUND) { }
        loading.start(
            ConfigurableTestLoadingViewModel::class,
            configure = { viewModel -> viewModel.message = "configure 済み" },
            actionThread = LoadingActionThread.BACKGROUND,
        ) { }

        assertContentEquals(
            List(3) { LoadingActionThread.BACKGROUND },
            gateway.startedActionThreads,
            "渡した BACKGROUND が委譲面に届きませんでした。",
        )
    }
}
