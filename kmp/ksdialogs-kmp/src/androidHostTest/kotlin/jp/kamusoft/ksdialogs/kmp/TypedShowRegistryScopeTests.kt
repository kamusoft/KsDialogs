package jp.kamusoft.ksdialogs.kmp

import jp.kamusoft.ksdialogs.kmp.support.ConfigurableTestDialogViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * 型を渡す表示の ViewModel factory を、共有コードのレジストリだけが持つことの確認。
 *
 * Android では共有コードの ViewModel 契約が Native ライブラリの契約そのものなので、
 * Native 側のレジストリにも同じ型で ViewModel factory を登録できてしまう。
 * 共有コードの表示がそれを引かないこと (登録先を取り違えたら黙って通らずに失敗すること) を固定する。
 *
 * Native 側のレジストリはプロセス全体で 1 個で登録解除の口が無いため、ここで登録する ViewModel 型は
 * このファイル専用のもの (NativeOnly*) に分け、他のテストが同じ型を Native レジストリで見ないようにする。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TypedShowRegistryScopeTests {

    @Test
    fun `共有コードの ViewModel のクラス参照は Native の型のキーと等しくなる`() {
        val shared: kotlin.reflect.KClass<out jp.kamusoft.ksdialogs.DialogViewModel<Boolean>> =
            ConfigurableTestDialogViewModel::class

        // クラス参照は取得のたびに別のラッパーになりうるため、レジストリのキーとしての等価性で見る
        assertEquals(
            ConfigurableTestDialogViewModel::class,
            shared,
            "共有 VM のクラス参照が Native の型と別物になりました。",
        )
    }

    @Test
    fun `PB-KT-11 Native 側に登録した VM factory は共有コードの型を渡す show から見えない`() = runTest {
        jp.kamusoft.ksdialogs.DialogViewRegistry.shared
            .registerViewModel(NativeOnlyDialogViewModel::class) { NativeOnlyDialogViewModel() }

        val failure = assertFailsWith<DialogException> {
            Dialog.instance.show(NativeOnlyDialogViewModel::class)
        }

        assertEquals(
            "No ViewModel factory is registered for ViewModel type NativeOnlyDialogViewModel.",
            failure.message,
        )
        // Native 側の失敗を載せ替えたものではなく、共有コードのレジストリが出した失敗であること
        assertNull(failure.cause, "共有コードのレジストリではなく Native の解決が使われました。")
    }

    @Test
    fun `PB-KT-11 Native 側に登録した Loading の VM factory は共有コードから見えない`() = runTest {
        jp.kamusoft.ksdialogs.LoadingViewRegistry.shared
            .registerViewModel(NativeOnlyLoadingViewModel::class) { NativeOnlyLoadingViewModel() }

        val showFailure = assertFailsWith<DialogException> {
            Loading.instance.show(NativeOnlyLoadingViewModel::class)
        }
        val startFailure = assertFailsWith<DialogException> {
            Loading.instance.start(NativeOnlyLoadingViewModel::class) { "実行された" }
        }

        assertEquals(
            "No ViewModel factory is registered for ViewModel type NativeOnlyLoadingViewModel.",
            showFailure.message,
        )
        assertEquals(showFailure.message, startFailure.message)
        assertNull(showFailure.cause, "共有コードのレジストリではなく Native の解決が使われました。")
    }

    @Test
    fun `PB-KT-11 Native 側に登録した Toast の VM factory は共有コードから見えない`() {
        jp.kamusoft.ksdialogs.ToastViewRegistry.shared
            .registerViewModel(NativeOnlyToastViewModel::class) { NativeOnlyToastViewModel() }

        val failure = assertFailsWith<DialogException> {
            Toast.instance.show(NativeOnlyToastViewModel::class)
        }

        assertEquals(
            "No ViewModel factory is registered for ViewModel type NativeOnlyToastViewModel.",
            failure.message,
        )
        assertNull(failure.cause, "共有コードのレジストリではなく Native の解決が使われました。")
    }
}

/** Native 側レジストリにだけ登録する検証専用のダイアログ ViewModel。このファイル以外では使わない。 */
private class NativeOnlyDialogViewModel : DialogViewModel<Boolean>

/** Native 側レジストリにだけ登録する検証専用の Loading ViewModel。このファイル以外では使わない。 */
private class NativeOnlyLoadingViewModel : LoadingViewModel

/** Native 側レジストリにだけ登録する検証専用の Toast ViewModel。このファイル以外では使わない。 */
private class NativeOnlyToastViewModel : ToastViewModel
