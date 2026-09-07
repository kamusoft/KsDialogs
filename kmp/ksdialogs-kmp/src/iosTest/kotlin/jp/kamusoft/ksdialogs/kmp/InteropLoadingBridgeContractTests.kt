package jp.kamusoft.ksdialogs.kmp

import jp.kamusoft.ksdialogs.kmp.support.runPumpingMainLoop
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.interpretObjCPointer
import kotlinx.cinterop.objcPtr
import platform.UIKit.UIView
import platform.objc.object_getClass
import swiftPMImport.jp.kamusoft.ksdialogs.kmp.KSDInteropLoadingBridge
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** 互換面への委譲を確かめるための ViewModel。View factory を登録して使う。 */
private class RegisteredLoadingProbeViewModel : LoadingViewModel, LoadingProgressReceiver {
    /** 受け取った進捗を呼ばれた順に並べたもの。 */
    val receivedProgress: MutableList<Double> = mutableListOf()

    override fun onProgress(progress: Double) {
        receivedProgress += progress
    }
}

/** View factory を登録せずに使う ViewModel。 */
private class UnregisteredLoadingProbeViewModel : LoadingViewModel

/**
 * 共有コードからのローディング呼び出しが、iOS Native ライブラリの互換面を通って
 * 状態の正へ届くかを実測する。
 *
 * 提示先の画面を持たないテストランナーでは器の取り付けまで到達しないが、合流状態と進捗の転送は
 * 提示先の有無によらず成立する — 実際に画面へ出ることは ios/ 側のテストと Sample が担う。
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class InteropLoadingBridgeContractTests {
    private val bridge = KSDInteropLoadingBridge.sharedBridge()
    private val loading: KsLoading = GatewayKsLoading(IosLoadingGateway(bridge))

    /** factory が生成した View の実体。互換面へ渡した中身を生かしておくために保持する。 */
    private val createdProbeViews = mutableListOf<UIView>()

    @Test
    fun `LD-KM-01 共有コードの start が処理を走らせて戻り値を返す`() = runPumpingMainLoop {
        try {
            val received = loading.start(message = "読み込み中") { report ->
                report(0.5)
                "完了"
            }

            assertEquals("完了", received, "処理の戻り値が共有コードへ届きませんでした。")
        } finally {
            loading.hide()
        }
    }

    @Test
    fun `LD-KM-01 処理が失敗しても合流1件は終了し失敗が呼び出し元へ伝播する`() = runPumpingMainLoop {
        assertFailsWith<IllegalStateException> {
            loading.start<Unit> { error("処理の失敗") }
        }
        // 終了が数えられていれば、この時点で表示は残っていない (残っていれば次の開始が新世代にならない)
        loading.hide()
    }

    @Test
    fun `LD-KM-03 共有 VM のカスタム Loading が解決され進捗が受け口へ届く`() = runPumpingMainLoop {
        registerProbeViewFactory()
        val viewModel = RegisteredLoadingProbeViewModel()

        try {
            loading.start(viewModel) { report ->
                report(0.25)
                report(1.5)
            }
        } finally {
            loading.hide()
        }

        assertContentEquals(
            listOf(0.25, 1.0),
            viewModel.receivedProgress,
            "共有 VM の受け口へ丸めた後の進捗が届きませんでした。",
        )
    }

    @Test
    fun `LD-KM-03 未登録の共有 VM は構成エラーになり処理は実行されない`() = runPumpingMainLoop {
        var executed = false

        val failure = assertFailsWith<DialogException> {
            loading.start(UnregisteredLoadingProbeViewModel()) { executed = true }
        }

        assertTrue(!executed, "構成ミスの表示で処理が実行されました。")
        assertTrue(
            failure.message?.isNotEmpty() == true,
            "互換面の失敗の理由が共有コードへ届きませんでした。",
        )
    }

    @Test
    fun `メッセージ更新と閉じるは互換面を通っても戻ってくる`() = runPumpingMainLoop {
        loading.show(message = "読み込み中")
        loading.setMessage("残り少し")
        loading.hide()
    }

    @Test
    fun `PB-KT-09 型を渡す表示の VM が互換面のレジストリで解決される`() = runPumpingMainLoop {
        registerProbeViewFactory()
        var created: RegisteredLoadingProbeViewModel? = null
        loading.registry.registerViewModel(RegisteredLoadingProbeViewModel::class) {
            RegisteredLoadingProbeViewModel().also { created = it }
        }

        try {
            loading.start(RegisteredLoadingProbeViewModel::class) { report -> report(0.25) }
        } finally {
            loading.hide()
        }

        val viewModel = assertNotNull(created, "登録した ViewModel factory が呼ばれませんでした。")
        assertContentEquals(
            listOf(0.25),
            viewModel.receivedProgress,
            "生成した VM が Swift 側レジストリで解決されませんでした (進捗が届いていません)。",
        )
    }

    /**
     * 共有コードの ViewModel のクラスをキーに、互換面へ View factory を登録する。
     *
     * 互換面の factory は View を必ず1つ返す約束なので、呼ばれても成立する実体を毎回新規に返す。
     * 互換面の取り込みは ObjC の型を UIKit のものとは別に宣言するため、
     * 生成した View は同じ ObjC オブジェクトを指す handle に読み替えて渡し、
     * 実体の生存期間は [createdProbeViews] が握る。
     */
    private fun registerProbeViewFactory() {
        bridge.registerViewFactoryForViewModelClass(
            viewModelClass = assertNotNull(object_getClass(RegisteredLoadingProbeViewModel())),
            factory = { _ ->
                val view = UIView()
                createdProbeViews += view
                interpretObjCPointer(view.objcPtr())
            },
        )
    }
}
