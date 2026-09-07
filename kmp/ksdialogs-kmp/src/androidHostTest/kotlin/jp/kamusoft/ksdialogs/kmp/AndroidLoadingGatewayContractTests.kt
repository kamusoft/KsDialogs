package jp.kamusoft.ksdialogs.kmp

import android.content.Context
import android.view.View
import jp.kamusoft.ksdialogs.kmp.support.ConfigurableTestLoadingViewModel
import jp.kamusoft.ksdialogs.kmp.support.PlainTestLoadingViewModel
import jp.kamusoft.ksdialogs.kmp.support.ProgressReceivingTestLoadingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Android Native ライブラリへの委譲で、引数・進捗・戻り値の対応が保たれることの検証。
 *
 * ローディングの表示そのものと合流の数え方は Native 実装の担当なので、
 * ここでは委譲面の契約だけを見る。
 *
 * @param scope スコープ形の呼び出しで Native ライブラリ側が行う処理の走らせ方
 */
private class RecordingNativeLoading(
    private val scope: suspend (suspend ((Double) -> Unit) -> Any?) -> Any? = { action -> action {} },
) : jp.kamusoft.ksdialogs.KsLoading {
    override val registry: jp.kamusoft.ksdialogs.LoadingViewRegistry =
        jp.kamusoft.ksdialogs.LoadingViewRegistry.shared

    override var style: jp.kamusoft.ksdialogs.LoadingStyle = jp.kamusoft.ksdialogs.LoadingStyle()

    override var options: jp.kamusoft.ksdialogs.DialogOptions = jp.kamusoft.ksdialogs.DialogOptions()

    /** 表示に渡されたメッセージを呼ばれた順に記録したもの。 */
    val shownMessages: MutableList<String?> = mutableListOf()

    /** 表示に渡された置き場所を呼ばれた順に記録したもの。指定がなければ null が入る。 */
    val shownPlacements: MutableList<jp.kamusoft.ksdialogs.DialogPlacement?> = mutableListOf()

    /** 表示に渡された ViewModel を呼ばれた順に記録したもの。既定ローディングなら null が入る。 */
    val shownViewModels: MutableList<jp.kamusoft.ksdialogs.LoadingViewModel?> = mutableListOf()

    /** [hide] が呼ばれた回数。 */
    var hideCount: Int = 0
        private set

    /** 更新されたメッセージを呼ばれた順に記録したもの。 */
    val updatedMessages: MutableList<String?> = mutableListOf()

    override suspend fun show(message: String?, placement: jp.kamusoft.ksdialogs.DialogPlacement?) {
        record(null, message, placement)
    }

    override suspend fun show(
        viewModel: jp.kamusoft.ksdialogs.LoadingViewModel,
        placement: jp.kamusoft.ksdialogs.DialogPlacement?,
    ) {
        record(viewModel, null, placement)
    }

    override suspend fun hide() {
        hideCount += 1
    }

    override suspend fun setMessage(message: String?) {
        updatedMessages += message
    }

    override suspend fun <T> start(
        message: String?,
        placement: jp.kamusoft.ksdialogs.DialogPlacement?,
        action: suspend ((Double) -> Unit) -> T,
    ): T {
        record(null, message, placement)
        @Suppress("UNCHECKED_CAST")
        return scope(action as suspend ((Double) -> Unit) -> Any?) as T
    }

    override suspend fun <T> start(
        viewModel: jp.kamusoft.ksdialogs.LoadingViewModel,
        placement: jp.kamusoft.ksdialogs.DialogPlacement?,
        action: suspend ((Double) -> Unit) -> T,
    ): T {
        record(viewModel, null, placement)
        @Suppress("UNCHECKED_CAST")
        return scope(action as suspend ((Double) -> Unit) -> Any?) as T
    }

    /**
     * 共有コードは中身の View を供給できないため、委譲面がインライン表示を使うことはない。
     * 呼ばれたら委譲の形が変わった合図になる。
     */
    override suspend fun <VM : jp.kamusoft.ksdialogs.LoadingViewModel> show(
        viewModel: VM,
        placement: jp.kamusoft.ksdialogs.DialogPlacement?,
        factory: Context.(VM) -> View,
    ): Unit = throw UnsupportedOperationException("委譲面は中身を渡さない表示だけを使う。")

    /** 同上。共有コードからはインライン表示のスコープ形も使わない。 */
    override suspend fun <VM : jp.kamusoft.ksdialogs.LoadingViewModel, T> start(
        viewModel: VM,
        placement: jp.kamusoft.ksdialogs.DialogPlacement?,
        factory: Context.(VM) -> View,
        action: suspend ((Double) -> Unit) -> T,
    ): T = throw UnsupportedOperationException("委譲面は中身を渡さない表示だけを使う。")

    /**
     * 共有コードは ViewModel のインスタンスを自分で渡すため、委譲面が型指定の表示を使うことはない。
     * 呼ばれたら委譲の形が変わった合図になる。
     */
    override suspend fun <VM : jp.kamusoft.ksdialogs.LoadingViewModel> show(
        viewModelClass: kotlin.reflect.KClass<VM>,
        placement: jp.kamusoft.ksdialogs.DialogPlacement?,
        configure: (suspend (VM) -> Unit)?,
    ): Unit = throw UnsupportedOperationException("委譲面はインスタンスを渡す表示だけを使う。")

    /** 同上。共有コードからは型指定のスコープ形も使わない。 */
    override suspend fun <VM : jp.kamusoft.ksdialogs.LoadingViewModel, T> start(
        viewModelClass: kotlin.reflect.KClass<VM>,
        placement: jp.kamusoft.ksdialogs.DialogPlacement?,
        configure: (suspend (VM) -> Unit)?,
        action: suspend ((Double) -> Unit) -> T,
    ): T = throw UnsupportedOperationException("委譲面はインスタンスを渡す表示だけを使う。")

    private fun record(
        viewModel: jp.kamusoft.ksdialogs.LoadingViewModel?,
        message: String?,
        placement: jp.kamusoft.ksdialogs.DialogPlacement?,
    ) {
        shownViewModels += viewModel
        shownMessages += message
        shownPlacements += placement
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidLoadingGatewayContractTests {

    @Test
    fun `LD-KM-01 共有コードの start が Native の start へ渡り戻り値が返る`() = runTest {
        val native = RecordingNativeLoading { action -> action { } }
        val loading: KsLoading = GatewayKsLoading(AndroidLoadingGateway(native))

        val received = loading.start(message = "読み込み中") { report ->
            report(0.5)
            "完了"
        }

        assertEquals("完了", received, "処理の戻り値が共有コードへ届きませんでした。")
        assertContentEquals(listOf("読み込み中"), native.shownMessages)
        assertContentEquals(listOf(null), native.shownViewModels)
    }

    @Test
    fun `LD-KM-03 共有 VM は包み直されずに Native へ渡る`() = runTest {
        val native = RecordingNativeLoading()
        val loading: KsLoading = GatewayKsLoading(AndroidLoadingGateway(native))
        val viewModel = ProgressReceivingTestLoadingViewModel()

        loading.show(viewModel)

        assertSame(viewModel, native.shownViewModels.single(), "共有 VM がそのまま委譲されていません。")
    }

    @Test
    fun `LD-KM-03 共有コードで定義した ViewModel は Native の契約と同じ型になる`() {
        val viewModel: jp.kamusoft.ksdialogs.LoadingViewModel = PlainTestLoadingViewModel()
        val receiver: jp.kamusoft.ksdialogs.LoadingProgressReceiver =
            ProgressReceivingTestLoadingViewModel()

        assertTrue(viewModel is LoadingViewModel, "ViewModel 契約が Native の型と別物になりました。")
        assertTrue(receiver is LoadingProgressReceiver, "進捗の受け口が Native の面と別物になりました。")
    }

    @Test
    fun `置き場所は Native の同型へ写される`() = runTest {
        val native = RecordingNativeLoading()
        val loading: KsLoading = GatewayKsLoading(AndroidLoadingGateway(native))

        loading.show(placement = DialogPlacement(horizontalAlignment = DialogAlignment.START, offsetX = 12.0))

        assertEquals(
            jp.kamusoft.ksdialogs.DialogPlacement(
                horizontalAlignment = jp.kamusoft.ksdialogs.DialogAlignment.START,
                offsetX = 12.0,
            ),
            native.shownPlacements.single(),
        )
    }

    @Test
    fun `メッセージ更新と閉じるは Native へそのまま渡る`() = runTest {
        val native = RecordingNativeLoading()
        val loading: KsLoading = GatewayKsLoading(AndroidLoadingGateway(native))

        loading.setMessage("残り少し")
        loading.hide()

        assertContentEquals(listOf("残り少し"), native.updatedMessages)
        assertEquals(1, native.hideCount)
    }

    @Test
    fun `PB-KT-09 型を渡す表示の VM が Native のインスタンス渡し経路へ渡る`() = runTest {
        val native = RecordingNativeLoading()
        val loading = GatewayKsLoading(AndroidLoadingGateway(native))
        loading.registry.registerViewModel(ConfigurableTestLoadingViewModel::class) {
            ConfigurableTestLoadingViewModel()
        }

        loading.show(ConfigurableTestLoadingViewModel::class) { viewModel ->
            viewModel.message = "表示の configure"
        }
        val received = loading.start(
            ConfigurableTestLoadingViewModel::class,
            configure = { viewModel -> viewModel.message = "スコープ形の configure" },
        ) { "完了" }

        // Native の型指定の表示が使われていれば、この double は UnsupportedOperationException を投げる
        assertEquals("完了", received, "処理の戻り値がそのまま返りませんでした。")
        assertContentEquals(
            listOf("表示の configure", "スコープ形の configure"),
            native.shownViewModels.map { (it as ConfigurableTestLoadingViewModel).message },
        )
    }

    @Test
    fun `LD-KM-03 共有コードの ViewModel が Native レジストリのキーとして通用する`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            jp.kamusoft.ksdialogs.LoadingViewRegistry.shared
                .register(PlainTestLoadingViewModel::class) { View(this) }

            // 提示先の画面を持たないテストでは表示まで到達しないが、合流状態は成立する。
            // View factory の解決に成功したかは、未登録の型で起きる構成エラーとの違いで判定する
            Loading.instance.show(PlainTestLoadingViewModel())
            val unregistered = assertFailsWith<DialogException> {
                Loading.instance.show(ProgressReceivingTestLoadingViewModel())
            }

            assertIs<jp.kamusoft.ksdialogs.DialogException.ViewFactoryNotRegistered>(
                unregistered.cause,
                "Native ライブラリの失敗の理由が共有コードへ届きませんでした。",
            )
        } finally {
            Loading.instance.hide()
            Dispatchers.resetMain()
        }
    }
}
