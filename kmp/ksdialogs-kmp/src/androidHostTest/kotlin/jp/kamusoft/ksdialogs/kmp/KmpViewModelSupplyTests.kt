package jp.kamusoft.ksdialogs.kmp

import android.view.View
import jp.kamusoft.ksdialogs.kmp.support.BooleanTestDialogViewModel
import jp.kamusoft.ksdialogs.kmp.support.StringTestDialogViewModel
import jp.kamusoft.ksdialogs.notifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * 共有コードの ViewModel に対して、Android Native の結果報告口の拡張がそのまま働くことの検証。
 *
 * Android では共有コードの ViewModel 契約が Native ライブラリの契約そのものなので、
 * 報告口を引数で受け取らない factory の登録も `viewModel.notifier` も、KMP 側の追加実装なしに使える。
 * この検証はそれがコンパイルできること・登録が共有 VM の型で解決されること・
 * 紐付けが show の外では空であることを見る。
 *
 * 報告口で報告した結果が show の呼び出し元へ届くところまでは、提示先の画面を持てるテスト
 * (Android Native の `DialogNotifierSupplyTests`) が同じ契約の型で担保する。
 * このテスト置き場では実際の提示が起きないため、そこまでは到達できない。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class KmpViewModelSupplyTests {

    @Test
    fun `MB-KM-02 共有 VM に対する Native 拡張の notifier で報告できる`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            // 中身は報告口を引数で受け取らず、Native 拡張の `notifier` で報告する
            jp.kamusoft.ksdialogs.DialogViewRegistry.shared
                .register(BooleanTestDialogViewModel::class) { viewModel ->
                    viewModel.notifier?.complete(true)
                    View(this)
                }

            val viewModel = BooleanTestDialogViewModel()
            assertNull(viewModel.notifier, "show の前から報告口が引けました。")

            // 提示先の画面を持たないテストでは表示まで到達しないため、
            // 1引数 factory が共有 VM の型で解決されたことは、解決の後に起きる提示先不在の失敗で判定する
            val registered = assertFailsWith<DialogException> {
                Dialog.instance.show(viewModel)
            }
            val unregistered = assertFailsWith<DialogException> {
                Dialog.instance.show(StringTestDialogViewModel())
            }

            assertIs<jp.kamusoft.ksdialogs.DialogException.PresentationHostUnavailable>(
                registered.cause,
                "1引数 factory の登録が共有 VM の型で解決されませんでした。",
            )
            assertIs<jp.kamusoft.ksdialogs.DialogException.ViewFactoryNotRegistered>(unregistered.cause)
            assertNull(viewModel.notifier, "show が終わったあとも紐付けが残りました。")
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `共有 VM の契約は Native ライブラリの契約と同一の型になる`() {
        val viewModel: DialogViewModel<Boolean> = BooleanTestDialogViewModel()

        // typealias なので、Native の契約型としてそのまま扱える
        val native: jp.kamusoft.ksdialogs.DialogViewModel<Boolean> = viewModel

        assertNull(native.notifier)
    }
}
