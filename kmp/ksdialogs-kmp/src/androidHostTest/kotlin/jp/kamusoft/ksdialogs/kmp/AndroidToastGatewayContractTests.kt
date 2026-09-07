package jp.kamusoft.ksdialogs.kmp

import android.content.Context
import android.view.View
import jp.kamusoft.ksdialogs.kmp.support.ConfigurableTestToastViewModel
import jp.kamusoft.ksdialogs.kmp.support.PlainTestToastViewModel
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** View factory を登録しないまま使う ViewModel。解決の失敗の観察に使う。 */
private class UnregisteredToastViewModel : ToastViewModel

/**
 * Android Native ライブラリへの委譲で、引数と解決の失敗の対応が保たれることの検証。
 *
 * 表示そのもの・重なり順・消滅の管理は Native 実装の担当なので、
 * ここでは委譲面の契約だけを見る。
 */
private class RecordingNativeToast : jp.kamusoft.ksdialogs.KsToast {
    override val registry: jp.kamusoft.ksdialogs.ToastViewRegistry =
        jp.kamusoft.ksdialogs.ToastViewRegistry()

    override var style: jp.kamusoft.ksdialogs.ToastStyle = jp.kamusoft.ksdialogs.ToastStyle()

    /** 表示に渡されたメッセージを呼ばれた順に記録したもの。カスタム Toast なら null が入る。 */
    val shownMessages: MutableList<String?> = mutableListOf()

    /** 表示に渡された ViewModel を呼ばれた順に記録したもの。既定 View なら null が入る。 */
    val shownViewModels: MutableList<jp.kamusoft.ksdialogs.ToastViewModel?> = mutableListOf()

    /** 表示に渡された duration を呼ばれた順に記録したもの。指定がなければ null が入る。 */
    val shownDurations: MutableList<Int?> = mutableListOf()

    /** 表示に渡された置き場所を呼ばれた順に記録したもの。指定がなければ null が入る。 */
    val shownPlacements: MutableList<jp.kamusoft.ksdialogs.DialogPlacement?> = mutableListOf()

    override fun show(
        message: String,
        durationMs: Int?,
        placement: jp.kamusoft.ksdialogs.DialogPlacement?,
    ) {
        record(null, message, durationMs, placement)
    }

    override fun show(
        viewModel: jp.kamusoft.ksdialogs.ToastViewModel,
        durationMs: Int?,
        placement: jp.kamusoft.ksdialogs.DialogPlacement?,
    ) {
        record(viewModel, null, durationMs, placement)
    }

    /**
     * 共有コードは中身の View を供給できないため、委譲面がインライン表示を使うことはない。
     * 呼ばれたら委譲の形が変わった合図になる。
     */
    override fun <VM : jp.kamusoft.ksdialogs.ToastViewModel> show(
        viewModel: VM,
        durationMs: Int?,
        placement: jp.kamusoft.ksdialogs.DialogPlacement?,
        factory: Context.(VM) -> View,
    ): Unit = throw UnsupportedOperationException("委譲面は中身を渡さない表示だけを使う。")

    /**
     * 共有コードは ViewModel のインスタンスを自分で渡すため、委譲面が型指定の表示を使うことはない。
     * 呼ばれたら委譲の形が変わった合図になる。
     */
    override fun <VM : jp.kamusoft.ksdialogs.ToastViewModel> show(
        viewModelClass: kotlin.reflect.KClass<VM>,
        durationMs: Int?,
        placement: jp.kamusoft.ksdialogs.DialogPlacement?,
        configure: ((VM) -> Unit)?,
    ): Unit = throw UnsupportedOperationException("委譲面はインスタンスを渡す表示だけを使う。")

    private fun record(
        viewModel: jp.kamusoft.ksdialogs.ToastViewModel?,
        message: String?,
        durationMs: Int?,
        placement: jp.kamusoft.ksdialogs.DialogPlacement?,
    ) {
        shownViewModels += viewModel
        shownMessages += message
        shownDurations += durationMs
        shownPlacements += placement
    }
}

class AndroidToastGatewayContractTests {

    @Test
    fun `メッセージと duration は Native へそのまま渡る`() {
        val native = RecordingNativeToast()
        val toast: KsToast = GatewayKsToast(AndroidToastGateway(native))

        toast.show("保存しました", durationMs = 2000)

        assertContentEquals(listOf("保存しました"), native.shownMessages)
        assertContentEquals(listOf(2000), native.shownDurations)
        assertContentEquals(listOf(null), native.shownViewModels)
    }

    @Test
    fun `TS-KM-02 共有 VM は包み直されずに Native へ渡る`() {
        val native = RecordingNativeToast()
        val toast: KsToast = GatewayKsToast(AndroidToastGateway(native))
        val viewModel = PlainTestToastViewModel()

        toast.show(viewModel)

        assertSame(viewModel, native.shownViewModels.single(), "共有 VM がそのまま委譲されていません。")
    }

    @Test
    fun `TS-KM-02 共有コードで定義した ViewModel は Native の契約と同じ型になる`() {
        val viewModel: jp.kamusoft.ksdialogs.ToastViewModel = PlainTestToastViewModel()

        assertTrue(viewModel is ToastViewModel, "ViewModel 契約が Native の型と別物になりました。")
    }

    @Test
    fun `置き場所は Native の同型へ写される`() {
        val native = RecordingNativeToast()
        val toast: KsToast = GatewayKsToast(AndroidToastGateway(native))

        toast.show(
            "保存しました",
            placement = DialogPlacement(verticalAlignment = DialogAlignment.START, offsetY = 24.0),
        )

        assertEquals(
            jp.kamusoft.ksdialogs.DialogPlacement(
                verticalAlignment = jp.kamusoft.ksdialogs.DialogAlignment.START,
                offsetY = 24.0,
            ),
            native.shownPlacements.single(),
        )
    }

    @Test
    fun `PB-KT-09 型を渡す show の VM が Native のインスタンス渡し show へ渡る`() {
        val native = RecordingNativeToast()
        val toast = GatewayKsToast(AndroidToastGateway(native))
        toast.registry.registerViewModel(ConfigurableTestToastViewModel::class) {
            ConfigurableTestToastViewModel()
        }

        toast.show(ConfigurableTestToastViewModel::class, durationMs = 2000) { viewModel ->
            viewModel.message = "configure で入れた文言"
        }

        // Native の型指定 show が使われていれば、この double は UnsupportedOperationException を投げる
        val shown = assertIs<ConfigurableTestToastViewModel>(native.shownViewModels.single())
        assertEquals("configure で入れた文言", shown.message, "configure 済みの VM が Native へ渡りませんでした。")
        assertContentEquals(listOf(2000), native.shownDurations)
    }

    /**
     * 共有コードの ViewModel のクラスがそのまま Native レジストリの解決キーになることを、
     * 未登録の型で起きる構成エラーが**その共有クラス名**を名指すことで見る。
     *
     * 解決に成功した先の実表示は Android の実機テストが担う (この置き場は端末の時計を持たない)。
     */
    @Test
    fun `TS-KM-02 共有コードの ViewModel のクラスが Native レジストリの解決キーになる`() {
        val toast: KsToast = GatewayKsToast(AndroidToastGateway(jp.kamusoft.ksdialogs.Toast()))

        val unregistered = assertFailsWith<DialogException> {
            toast.show(UnregisteredToastViewModel())
        }

        assertIs<jp.kamusoft.ksdialogs.DialogException.ViewFactoryNotRegistered>(
            unregistered.cause,
            "Native ライブラリの失敗の理由が共有コードへ届きませんでした。",
        )
        assertTrue(
            unregistered.message?.contains(UnregisteredToastViewModel::class.simpleName!!) == true,
            "解決キーが共有コードで定義したクラスになっていません: ${unregistered.message}",
        )
    }
}
