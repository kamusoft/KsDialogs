package jp.kamusoft.ksdialogs.kmp

import jp.kamusoft.ksdialogs.kmp.support.cancelWhileWaitingForHost
import jp.kamusoft.ksdialogs.kmp.support.pumpMainLoopUntil
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
import swiftPMImport.jp.kamusoft.ksdialogs.kmp.KSDInteropDialogResultKindCancelled
import swiftPMImport.jp.kamusoft.ksdialogs.kmp.KSDInteropDialogResultKindError
import swiftPMImport.jp.kamusoft.ksdialogs.kmp.KSDInteropDialogResultType
import kotlin.concurrent.AtomicInt
import kotlin.concurrent.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIsNot
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

/** 互換面への委譲を確かめるための ViewModel。View factory を登録して使う。 */
private class RegisteredProbeViewModel : DialogViewModel<Boolean>

/** View factory を登録せずに使う ViewModel。 */
private class UnregisteredProbeViewModel : DialogViewModel<Boolean>

/**
 * 共有コードの ViewModel が iOS Native ライブラリのレジストリのキーとして通用するかを実測する。
 *
 * 提示先の画面を持たないテストランナーではダイアログの表示まで到達しない。
 * 「View factory の解決に成功したか」は、解決に成功したときの提示先の出現の待ち (打ち切ると cancelled で終わる) と、
 * 解決に失敗したときのその場の未登録の失敗の区別で判定する (互換面の失敗は判別と説明文で返る)。
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

        val unregistered = showThroughBridge(UnregisteredProbeViewModel())
        assertEquals(KSDInteropDialogResultKindError, unregistered.kind)
        val unregisteredFailure = assertNotNull(unregistered.error).localizedDescription
        println("[実測] 未登録 ViewModel の失敗: $unregisteredFailure")
        assertTrue(
            unregisteredFailure.contains("No View factory is registered for ViewModel type"),
            "未登録の ViewModel が未登録として扱われませんでした: $unregisteredFailure",
        )

        // 登録済みの側は解決に成功すると失敗せずに提示先を待つ。結果が届かないことを確かめてから、
        // 互換面のハンドルで打ち切り、cancelled で終わることを確かめる
        val delivered = AtomicReference<KSDInteropDialogResult?>(null)
        val handle = bridge.showViewModel(RegisteredProbeViewModel(), placement = null) { delivered.value = it }
        pumpMainLoopUntil(timeout = WAITING_PERIOD) { delivered.value != null }
        assertNull(
            delivered.value?.let { it.error?.localizedDescription ?: "kind=${it.kind}" },
            "登録済みの ViewModel の show が提示先を待たずに結果を返しました。",
        )

        handle.cancel()

        assertTrue(pumpMainLoopUntil { delivered.value != null }, "打ち切ったあとも互換面から結果が返りませんでした。")
        assertEquals(
            KSDInteropDialogResultKindCancelled,
            assertNotNull(delivered.value).kind,
            "待っている間の打ち切りが cancelled で終わりませんでした。",
        )
        assertTrue(createdProbeViews.isEmpty(), "待っている間に打ち切った Dialog の中身が作られました。")
    }

    @Test
    fun `DM-KM-03 既定エントリの show は互換面と同じレジストリを引く`() {
        registerProbeViewFactory()

        // 未登録の失敗にならず提示先を待つことが、互換面で登録した factory を引けた印になる
        val cancellation = cancelWhileWaitingForHost {
            Dialog.instance.show(RegisteredProbeViewModel())
        }

        assertIsNot<DialogException>(cancellation, "打ち切りが構成エラーに化けました。")
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

        // 未登録の失敗にならず提示先を待つことが、生成した VM が Swift 側レジストリで解決された印になる
        cancelWhileWaitingForHost {
            Dialog.instance.show(RegisteredProbeViewModel::class)
        }

        assertNotNull(created, "登録した ViewModel factory が呼ばれませんでした。")
    }

    @Test
    fun `PB-KC-04 待っている Dialog を共有コードで打ち切ると表示されずにキャンセルが伝播する`() {
        registerProbeViewFactory()
        val surface = CancellationRecordingShowSurface(InteropDialogShowSurface(bridge))
        val dialogs = GatewayKsDialog(IosDialogGateway(surface))

        val cancellation = cancelWhileWaitingForHost {
            dialogs.show(RegisteredProbeViewModel())
        }

        assertIsNot<DialogException>(cancellation, "打ち切りが構成エラーに化けました。")
        assertEquals(1, surface.cancelCount.value, "互換面のハンドルの打ち切りがちょうど1回呼ばれていません。")
        assertTrue(
            pumpMainLoopUntil { surface.delivered.value != null },
            "打ち切ったあとも互換面から結果が返りませんでした。",
        )
        assertEquals(
            DialogOutcome.Cancelled,
            surface.delivered.value?.getOrNull(),
            "互換面の結果が cancelled で確定しませんでした: ${surface.delivered.value}",
        )
        assertTrue(createdProbeViews.isEmpty(), "待っている間に打ち切った Dialog の中身が作られました。")
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

    private companion object {
        /** 未登録の失敗が十分に届く時間。この間に結果が返らなければ、提示先を待っているとみなす。 */
        val WAITING_PERIOD = 500.milliseconds
    }
}

/**
 * 互換面への委譲をそのまま通しつつ、取り消しの呼び出しと互換面から届いた結果を記録する表示面。
 *
 * 呼び出し元のコルーチンの打ち切りが互換面のハンドルまで届いたかを、実際の互換面を使ったまま観察するために使う。
 */
private class CancellationRecordingShowSurface(
    private val delegate: IosDialogShowSurface,
) : IosDialogShowSurface {
    /** 取り消しが呼ばれた回数。 */
    val cancelCount = AtomicInt(0)

    /** 互換面から届いた結果。届くまでは null。 */
    val delivered = AtomicReference<Result<DialogOutcome>?>(null)

    override fun show(
        viewModel: DialogViewModel<*>,
        placement: DialogPlacement?,
        completion: (Result<DialogOutcome>) -> Unit,
    ): IosDialogShowCancellation {
        val cancellation = delegate.show(viewModel, placement) { result ->
            delivered.value = result
            completion(result)
        }
        return IosDialogShowCancellation {
            cancelCount.incrementAndGet()
            cancellation.cancel()
        }
    }
}
