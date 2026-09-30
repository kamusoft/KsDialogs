package jp.kamusoft.ksdialogs.kmp

import jp.kamusoft.ksdialogs.kmp.support.runPumpingMainLoop
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.interpretObjCPointer
import kotlinx.cinterop.objcPtr
import platform.UIKit.UIView
import platform.objc.object_getClass
import swiftPMImport.jp.kamusoft.ksdialogs.kmp.KSDInteropToastBridge
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 互換面への委譲を確かめるための ViewModel。View factory を登録して使う。 */
private class RegisteredToastProbeViewModel : ToastViewModel

/** View factory を登録せずに使う ViewModel。 */
private class UnregisteredToastProbeViewModel : ToastViewModel

/**
 * 共有コードからの Toast 呼び出しが、iOS Native ライブラリの互換面を通って
 * 状態の正へ届くかを実測する。
 *
 * 提示先の画面を持たないテストランナーでは器の取り付けまで到達せず、中身の View も作られない
 * (中身は提示先を確保してから作る)。ここでは型キーによる View factory の引き当てを、互換面の受理が
 * 同期に返す結果 (未登録ならエラー、登録済みならエラーなし) で判定する。引き当てた factory から中身が
 * 作られることと、実際に画面へ出ることは ios/ 側のテストと Sample が担う。
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class InteropToastBridgeContractTests {
    private val bridge = KSDInteropToastBridge.sharedBridge()
    private val toast: KsToast = GatewayKsToast(IosToastGateway(bridge))

    /** factory が生成した View の実体。互換面へ渡した中身を生かしておくために保持する。 */
    private val createdProbeViews = mutableListOf<UIView>()

    @Test
    fun `TS-KM-02 共有 VM の型キーが OS 側登録の factory を引き当てる`() = runPumpingMainLoop {
        registerProbeViewFactory()

        // 互換面の show は受理の時点で型キーを引き当て、未登録ならその場でエラーを返す。
        // 中身は提示先を確保してから作られるため、提示先の無いこのテストランナーでは factory は呼ばれない
        val registeredFailure =
            bridge.showViewModel(RegisteredToastProbeViewModel(), duration = null, placement = null)
        val unregisteredFailure =
            bridge.showViewModel(UnregisteredToastProbeViewModel(), duration = null, placement = null)

        assertNull(
            registeredFailure?.localizedDescription,
            "登録した factory が共有 VM の型キーで引き当てられませんでした。",
        )
        assertNotNull(unregisteredFailure, "未登録の共有 VM の受理がエラーになりませんでした。")
        // 共有コードの入口からも、登録済みの共有 VM は構成エラーにならずに戻る
        toast.show(RegisteredToastProbeViewModel(), durationMs = 50)
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

        // 生成した VM が Swift 側レジストリで引き当てられなければ、互換面の受理が構成エラーを返して
        // この呼び出しが DialogException を投げる
        toast.show(RegisteredToastProbeViewModel::class, durationMs = 50)

        assertTrue(created != null, "登録した ViewModel factory が呼ばれませんでした。")
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
            factory = { _ ->
                val view = UIView()
                createdProbeViews += view
                interpretObjCPointer(view.objcPtr())
            },
        )
    }
}
