package jp.kamusoft.ksdialogs.kmp

import android.view.View
import jp.kamusoft.ksdialogs.kmp.support.BooleanTestDialogViewModel
import jp.kamusoft.ksdialogs.kmp.support.ConfigurableTestDialogViewModel
import jp.kamusoft.ksdialogs.kmp.support.StringTestDialogViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Android Native ライブラリへの委譲で、引数・結果・show 呼び出しの対応が保たれることの検証。
 *
 * ダイアログの表示そのものは Native 実装の担当なので、ここでは委譲面の契約だけを見る。
 *
 * @param respond Native ライブラリ側の応答
 */
private class RecordingNativeDialogs(
    private val respond: () -> jp.kamusoft.ksdialogs.DialogResult<*>,
) : jp.kamusoft.ksdialogs.KsDialog {
    override val registry: jp.kamusoft.ksdialogs.DialogViewRegistry =
        jp.kamusoft.ksdialogs.DialogViewRegistry.shared

    /** show に渡された ViewModel を呼ばれた順に記録したもの。 */
    val shownViewModels: MutableList<Any> = mutableListOf()

    /** show に渡された置き場所を呼ばれた順に記録したもの。指定がなければ null が入る。 */
    val shownPlacements: MutableList<jp.kamusoft.ksdialogs.DialogPlacement?> = mutableListOf()

    override suspend fun <R> show(
        viewModel: jp.kamusoft.ksdialogs.DialogViewModel<R>,
        placement: jp.kamusoft.ksdialogs.DialogPlacement?,
    ): jp.kamusoft.ksdialogs.DialogResult<R> {
        shownViewModels += viewModel
        shownPlacements += placement
        @Suppress("UNCHECKED_CAST")
        return respond() as jp.kamusoft.ksdialogs.DialogResult<R>
    }

    /**
     * 共有コードは View を供給できないため、委譲面が中身を渡す show を使うことはない。
     * 呼ばれたら委譲の形が変わった合図になる。
     */
    override suspend fun <R, VM : jp.kamusoft.ksdialogs.DialogViewModel<R>> show(
        viewModel: VM,
        placement: jp.kamusoft.ksdialogs.DialogPlacement?,
        factory: android.content.Context.(VM, jp.kamusoft.ksdialogs.DialogNotifier<R>) -> View,
    ): jp.kamusoft.ksdialogs.DialogResult<R> =
        throw UnsupportedOperationException("委譲面は中身を渡さない show だけを使う。")

    /**
     * 共有コードは ViewModel のインスタンスを自分で渡すため、委譲面が型指定 show を使うことはない。
     * 呼ばれたら委譲の形が変わった合図になる。
     */
    override suspend fun <R, VM : jp.kamusoft.ksdialogs.DialogViewModel<R>> show(
        viewModelClass: kotlin.reflect.KClass<VM>,
        placement: jp.kamusoft.ksdialogs.DialogPlacement?,
        configure: (suspend (VM) -> Unit)?,
    ): jp.kamusoft.ksdialogs.DialogResult<R> =
        throw UnsupportedOperationException("委譲面はインスタンスを渡す show だけを使う。")
}

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidDialogGatewayContractTests {

    @Test
    fun `ViewModel は包み直されずに Native ライブラリへ渡る`() = runTest {
        val native = RecordingNativeDialogs { jp.kamusoft.ksdialogs.DialogResult.Completed(true) }
        val viewModel = BooleanTestDialogViewModel()

        GatewayKsDialog(AndroidDialogGateway(native)).show(viewModel)

        assertSame(viewModel, native.shownViewModels.single(), "ViewModel が包み直されて委譲されました。")
    }

    @Test
    fun `Native ライブラリの completed が宣言結果型で返る`() = runTest {
        val native = RecordingNativeDialogs { jp.kamusoft.ksdialogs.DialogResult.Completed(true) }

        val result = GatewayKsDialog(AndroidDialogGateway(native)).show(BooleanTestDialogViewModel())

        assertEquals(DialogResult.Completed(true), result)
    }

    @Test
    fun `共有コードの placement が同じ値で Native ライブラリへ届く`() = runTest {
        val native = RecordingNativeDialogs { jp.kamusoft.ksdialogs.DialogResult.Completed(true) }

        GatewayKsDialog(AndroidDialogGateway(native)).show(
            BooleanTestDialogViewModel(),
            DialogPlacement(
                horizontalAlignment = DialogAlignment.END,
                verticalAlignment = DialogAlignment.START,
                offsetX = 12.5,
                offsetY = -8.0,
            ),
        )

        assertEquals(
            jp.kamusoft.ksdialogs.DialogPlacement(
                horizontalAlignment = jp.kamusoft.ksdialogs.DialogAlignment.END,
                verticalAlignment = jp.kamusoft.ksdialogs.DialogAlignment.START,
                offsetX = 12.5,
                offsetY = -8.0,
            ),
            native.shownPlacements.single(),
        )
    }

    @Test
    fun `配置の4値はすべて Native ライブラリの同じ配置へ写る`() {
        val mapped = DialogAlignment.entries.map { alignment ->
            DialogPlacement(horizontalAlignment = alignment).toNative().horizontalAlignment
        }

        assertEquals(
            listOf(
                jp.kamusoft.ksdialogs.DialogAlignment.START,
                jp.kamusoft.ksdialogs.DialogAlignment.CENTER,
                jp.kamusoft.ksdialogs.DialogAlignment.END,
                jp.kamusoft.ksdialogs.DialogAlignment.FILL,
            ),
            mapped,
        )
    }

    @Test
    fun `placement を省略した show は Native ライブラリへも何も渡さない`() = runTest {
        val native = RecordingNativeDialogs { jp.kamusoft.ksdialogs.DialogResult.Completed(true) }

        GatewayKsDialog(AndroidDialogGateway(native)).show(BooleanTestDialogViewModel())

        assertNull(
            native.shownPlacements.single(),
            "省略した placement が既定値のオブジェクトに化けました (View への添付が効かなくなります)。",
        )
    }

    @Test
    fun `Native ライブラリの cancelled がそのまま返る`() = runTest {
        val native = RecordingNativeDialogs { jp.kamusoft.ksdialogs.DialogResult.Cancelled }

        val result = GatewayKsDialog(AndroidDialogGateway(native)).show(BooleanTestDialogViewModel())

        assertEquals(DialogResult.Cancelled, result)
    }

    @Test
    fun `DM-KM-02 共有コードの ViewModel が Native レジストリのキーとして通用する`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            jp.kamusoft.ksdialogs.DialogViewRegistry.shared
                .register(BooleanTestDialogViewModel::class) { _, _ -> View(this) }

            // 提示先の画面を持たないテストでは表示まで到達しないため、
            // View factory の解決に成功したかは、解決の後に起きる提示先不在の失敗で判定する
            val registered = assertFailsWith<DialogException> {
                Dialog.instance.show(BooleanTestDialogViewModel())
            }
            val unregistered = assertFailsWith<DialogException> {
                Dialog.instance.show(StringTestDialogViewModel())
            }

            assertEquals("No screen is available to present the Dialog.", registered.message)
            assertIs<jp.kamusoft.ksdialogs.DialogException.PresentationHostUnavailable>(registered.cause)
            assertIs<jp.kamusoft.ksdialogs.DialogException.ViewFactoryNotRegistered>(unregistered.cause)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `PB-KT-09 型を渡す show の VM が Native のインスタンス渡し show へ渡る`() = runTest {
        val native = RecordingNativeDialogs { jp.kamusoft.ksdialogs.DialogResult.Completed(true) }
        val dialogs = GatewayKsDialog(AndroidDialogGateway(native))
        dialogs.registry.registerViewModel(ConfigurableTestDialogViewModel::class) {
            ConfigurableTestDialogViewModel()
        }

        val result = dialogs.show(ConfigurableTestDialogViewModel::class) { viewModel ->
            viewModel.message = "configure で入れた文言"
        }

        // Native の型指定 show が使われていれば、この double は UnsupportedOperationException を投げる
        val shown = assertIs<ConfigurableTestDialogViewModel>(native.shownViewModels.single())
        assertEquals("configure で入れた文言", shown.message, "configure 済みの VM が Native へ渡りませんでした。")
        assertEquals(DialogResult.Completed(true), result)
    }

    @Test
    fun `既定エントリのレジストリは Native ライブラリの共有レジストリを指す`() {
        val handle = assertIs<AndroidDialogViewRegistry>(Dialog.instance.registry)

        assertSame(jp.kamusoft.ksdialogs.DialogViewRegistry.shared, handle.native)
    }
}
