package jp.kamusoft.ksdialogs.kmp

import jp.kamusoft.ksdialogs.kmp.support.runPumpingMainLoop
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.interpretObjCPointer
import kotlinx.cinterop.objcPtr
import kotlinx.coroutines.delay
import platform.UIKit.UIView
import platform.objc.object_getClass
import swiftPMImport.jp.kamusoft.ksdialogs.kmp.KSDInteropToastBridge
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/** 互換面への委譲を確かめるための ViewModel。View factory を登録して使う。 */
private class RegisteredToastProbeViewModel : ToastViewModel

/** View factory を登録せずに使う ViewModel。 */
private class UnregisteredToastProbeViewModel : ToastViewModel

/**
 * 共有コードからの Toast 呼び出しが、iOS Native ライブラリの互換面を通って
 * 状態の正へ届くかを実測する。
 *
 * 提示先の画面を持たないテストランナーでは器の取り付けまで到達しないが、中身の解決 (型キーによる
 * View factory の引き当てと生成) は提示先の有無によらず行われる — 実際に画面へ出ることは
 * ios/ 側のテストと Sample が担う。
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class InteropToastBridgeContractTests {
    private val bridge = KSDInteropToastBridge.sharedBridge()
    private val toast: KsToast = GatewayKsToast(IosToastGateway(bridge))

    /** factory が生成した View の実体。互換面へ渡した中身を生かしておくために保持する。 */
    private val createdProbeViews = mutableListOf<UIView>()

    /** factory が受け取った ViewModel を呼ばれた順に並べたもの。 */
    private val resolvedViewModels = mutableListOf<Any?>()

    @Test
    fun `TS-KM-02 共有 VM の型キーが OS 側登録の factory を引き当てる`() = runPumpingMainLoop {
        registerProbeViewFactory()
        val viewModel = RegisteredToastProbeViewModel()

        toast.show(viewModel, durationMs = 50)

        awaitUntil("登録した factory が共有 VM で引き当てられませんでした。") {
            resolvedViewModels.isNotEmpty()
        }
        assertSame(
            viewModel,
            resolvedViewModels.single(),
            "factory へ渡ったのが共有 VM そのものではありません。",
        )
    }

    @Test
    fun `TS-KM-02 未登録の共有 VM は構成エラーになり表示は行われない`() = runPumpingMainLoop {
        val failure = assertFailsWith<DialogException> {
            toast.show(UnregisteredToastProbeViewModel())
        }

        assertTrue(
            failure.message?.isNotEmpty() == true,
            "互換面の失敗の理由が共有コードへ届きませんでした。",
        )
    }

    @Test
    fun `メッセージ表示は互換面を通っても失敗せずに戻る`() = runPumpingMainLoop {
        toast.show("保存しました", durationMs = 50)
        toast.show("保存しました", placement = DialogPlacement(offsetY = 24.0))
    }

    @Test
    fun `PB-KT-09 型を渡す show の VM が互換面のレジストリで解決される`() = runPumpingMainLoop {
        registerProbeViewFactory()
        var created: RegisteredToastProbeViewModel? = null
        toast.registry.registerViewModel(RegisteredToastProbeViewModel::class) {
            RegisteredToastProbeViewModel().also { created = it }
        }

        toast.show(RegisteredToastProbeViewModel::class, durationMs = 50)

        awaitUntil("生成した VM が Swift 側レジストリで解決されませんでした。") {
            resolvedViewModels.isNotEmpty()
        }
        assertSame(
            assertNotNull(created, "登録した ViewModel factory が呼ばれませんでした。"),
            resolvedViewModels.single(),
            "factory へ渡ったのが生成した VM そのものではありません。",
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
            viewModelClass = assertNotNull(object_getClass(RegisteredToastProbeViewModel())),
            factory = { viewModel ->
                resolvedViewModels += viewModel
                val view = UIView()
                createdProbeViews += view
                interpretObjCPointer(view.objcPtr())
            },
        )
    }

    /** 受理は UI スレッドへ積まれてから進むため、条件が満たされるまで待つ。 */
    private suspend fun awaitUntil(
        message: String,
        timeout: Duration = 5.seconds,
        condition: () -> Boolean,
    ) {
        val started = TimeSource.Monotonic.markNow()
        while (!condition() && started.elapsedNow() < timeout) {
            delay(10.milliseconds)
        }
        assertTrue(condition(), message)
    }
}
