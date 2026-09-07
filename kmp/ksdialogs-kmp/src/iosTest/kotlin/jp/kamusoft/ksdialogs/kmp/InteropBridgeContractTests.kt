package jp.kamusoft.ksdialogs.kmp

import jp.kamusoft.ksdialogs.kmp.support.runPumpingMainLoop
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.interpretObjCPointer
import kotlinx.cinterop.objcPtr
import kotlinx.cinterop.toKString
import objcnames.classes.UIView as InteropUIView
import platform.UIKit.UIView
import platform.objc.class_getName
import platform.objc.object_getClass
import swiftPMImport.jp.kamusoft.ksdialogs.kmp.KSDInteropDialogBridge
import swiftPMImport.jp.kamusoft.ksdialogs.kmp.KSDInteropDialogResult
import swiftPMImport.jp.kamusoft.ksdialogs.kmp.KSDInteropDialogResultKindError
import swiftPMImport.jp.kamusoft.ksdialogs.kmp.KSDInteropDialogResultType
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** 互換面への委譲を確かめるための ViewModel。View factory を登録して使う。 */
private class RegisteredProbeViewModel : DialogViewModel<Boolean>

/** View factory を登録せずに使う ViewModel。 */
private class UnregisteredProbeViewModel : DialogViewModel<Boolean>

/**
 * 共有コードの ViewModel が iOS Native ライブラリのレジストリのキーとして通用するかを実測する。
 *
 * 提示先の画面を持たないテストランナーではダイアログの表示まで到達しないため、
 * 「View factory の解決に成功したか」を、解決の後に起きる提示先不在の失敗と、
 * 解決に失敗したときの未登録の失敗の区別で判定する (互換面の失敗は判別と説明文で返る)。
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class InteropBridgeContractTests {
    private val bridge = KSDInteropDialogBridge.sharedBridge()

    /** factory が生成した View の実体。互換面へ渡した handle が指す先を生かしておくために保持する。 */
    private val createdProbeViews = mutableListOf<UIView>()

    @Test
    fun `共有コードの ViewModel は ObjC クラスとして見える`() {
        val first = RegisteredProbeViewModel()
        val second = RegisteredProbeViewModel()

        val firstName = class_getName(assertNotNull(object_getClass(first)))?.toKString()
        val secondName = class_getName(assertNotNull(object_getClass(second)))?.toKString()

        assertNotNull(firstName, "ObjC クラス名を取得できませんでした。")
        assertEquals(firstName, secondName, "同じ Kotlin クラスの実例が別々の ObjC クラスになりました。")
        println("[実測] 共有コード ViewModel の ObjC クラス名: $firstName")
    }

    @Test
    fun `互換面へ渡す View factory は実体のある View を返す`() {
        val handle = newProbeViewHandle()

        val className = class_getName(assertNotNull(object_getClass(handle)))?.toKString()

        assertEquals("UIView", className, "factory の戻り値が View の実体を指していません。")
    }

    @Test
    fun `登録時に渡した宣言結果型が合う値と合わない値を境界越しに判別する`() {
        val resultType = booleanResultType()

        assertTrue(resultType.acceptsValue(true), "宣言結果型と同じ型の値が受理されませんでした。")
        assertTrue(resultType.acceptsValue(false), "宣言結果型と同じ型の値が受理されませんでした。")
        assertFalse(resultType.acceptsValue("文字列"), "宣言結果型と異なる型の値が受理されました。")
        assertFalse(resultType.acceptsValue(1), "宣言結果型と異なる型の値が受理されました。")
    }

    @Test
    fun `DM-KM-01 Swift 側登録の View factory が共有コードの ViewModel で解決される`() {
        registerProbeViewFactory()

        val registered = showThroughBridge(RegisteredProbeViewModel())
        val unregistered = showThroughBridge(UnregisteredProbeViewModel())

        assertEquals(KSDInteropDialogResultKindError, registered.kind)
        assertEquals(KSDInteropDialogResultKindError, unregistered.kind)

        val registeredFailure = assertNotNull(registered.error).localizedDescription
        val unregisteredFailure = assertNotNull(unregistered.error).localizedDescription
        println("[実測] 登録済み ViewModel の失敗: $registeredFailure")
        println("[実測] 未登録 ViewModel の失敗: $unregisteredFailure")

        assertTrue(
            unregisteredFailure.contains("No View factory is registered for ViewModel type"),
            "未登録の ViewModel が未登録として扱われませんでした: $unregisteredFailure",
        )
        assertTrue(
            registeredFailure.contains("No screen is available to present the Dialog."),
            "登録済みの ViewModel の解決が未登録扱いになりました: $registeredFailure",
        )
    }

    @Test
    fun `DM-KM-03 既定エントリの show は互換面と同じレジストリを引く`() {
        registerProbeViewFactory()

        val failure = runPumpingMainLoop {
            runCatching { Dialog.instance.show(RegisteredProbeViewModel()) }
        }.exceptionOrNull()

        val exception = assertNotNull(failure, "提示先が無いのに結果が返りました。")
        assertTrue(
            exception.message?.contains("No screen is available to present the Dialog.") == true,
            "互換面で登録した factory が既定エントリの show から引けませんでした: ${exception.message}",
        )
    }

    @Test
    fun `DM-KM-03 未登録の ViewModel の show は結果を返さずに失敗する`() {
        val failure = runPumpingMainLoop {
            runCatching { Dialog.instance.show(UnregisteredProbeViewModel()) }
        }.exceptionOrNull()

        val exception = assertNotNull(failure, "未登録の ViewModel で結果が返りました。")
        assertTrue(exception is DialogException, "構成エラーが $exception として届きました。")
        println("[実測] 共有コードへ届いた構成エラー: ${exception.message}")
        assertTrue(
            exception.message?.contains("No View factory is registered for ViewModel type") == true,
            "Native の未登録の説明が共有コードの DialogException へ素通しで届きませんでした: ${exception.message}",
        )
    }

    @Test
    fun `PB-KT-09 型を渡す show の VM が互換面のレジストリで解決される`() {
        registerProbeViewFactory()
        var created: RegisteredProbeViewModel? = null
        Dialog.instance.registry.registerViewModel(RegisteredProbeViewModel::class) {
            RegisteredProbeViewModel().also { created = it }
        }

        val failure = runPumpingMainLoop {
            runCatching { Dialog.instance.show(RegisteredProbeViewModel::class) }
        }.exceptionOrNull()

        assertNotNull(created, "登録した ViewModel factory が呼ばれませんでした。")
        val exception = assertNotNull(failure, "提示先が無いのに結果が返りました。")
        assertTrue(
            exception.message?.contains("No screen is available to present the Dialog.") == true,
            "生成した VM が Swift 側レジストリで解決されませんでした: ${exception.message}",
        )
    }

    /**
     * 検証用 ViewModel の View factory を互換面へ登録する。
     *
     * 互換面の factory は View を必ず1つ返す約束なので、呼ばれても成立する実体を毎回新規に返す。
     * 互換面の取り込みは ObjC の型を UIKit のものとは別に宣言するため、
     * 生成した View は同じ ObjC オブジェクトを指す handle に読み替えて渡し、
     * 実体の生存期間は [createdProbeViews] が握る。
     */
    private fun registerProbeViewFactory() {
        bridge.registerViewFactoryForViewModelClass(
            viewModelClass = assertNotNull(object_getClass(RegisteredProbeViewModel())),
            resultType = booleanResultType(),
            factory = { _, _ -> newProbeViewHandle() },
        )
    }

    /** 検証用 ViewModel が宣言する結果型 (Boolean) を互換面の表現で作る。 */
    private fun booleanResultType(): KSDInteropDialogResultType =
        KSDInteropDialogResultType(name = "Boolean", matcher = { it is Boolean })

    /** 互換面へ渡す View を1つ生成する。実体は [createdProbeViews] が保持し続ける。 */
    private fun newProbeViewHandle(): InteropUIView {
        val view = UIView()
        createdProbeViews += view
        return interpretObjCPointer(view.objcPtr())
    }

    private fun showThroughBridge(viewModel: DialogViewModel<*>): KSDInteropDialogResult =
        assertNotNull(
            runPumpingMainLoop {
                suspendCoroutine { continuation ->
                    bridge.showViewModel(viewModel, placement = null) { continuation.resume(it) }
                }
            },
            "互換面から結果が返りませんでした。",
        )
}
