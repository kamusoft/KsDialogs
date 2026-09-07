package jp.kamusoft.ksdialogs.kmp.support

import jp.kamusoft.ksdialogs.kmp.DialogException
import jp.kamusoft.ksdialogs.kmp.DialogViewModel
import jp.kamusoft.ksdialogs.kmp.DialogViewRegistry
import jp.kamusoft.ksdialogs.kmp.LoadingViewModel
import jp.kamusoft.ksdialogs.kmp.LoadingViewRegistry
import jp.kamusoft.ksdialogs.kmp.ToastViewModel
import jp.kamusoft.ksdialogs.kmp.ToastViewRegistry
import kotlin.reflect.KClass

/**
 * 差し替え実装が自前で持つ ViewModel factory の表。
 *
 * 公開されているのは登録の口だけなので、利用者が契約を自分で実装するときは
 * このように表と生成を自分で用意する。
 */
internal class FakeViewModelFactories {
    private val factories = mutableMapOf<KClass<*>, () -> Any>()

    /** ViewModel 型に対する factory を入れる。同じ型への再登録は後勝ちになる。 */
    fun put(viewModelClass: KClass<*>, factory: () -> Any) {
        factories[viewModelClass] = factory
    }

    /** 登録済みの factory で ViewModel を作る。未登録なら構成エラーにする。 */
    fun <VM : Any> create(viewModelClass: KClass<VM>): VM {
        val factory = factories[viewModelClass]
            ?: throw DialogException("差し替え実装に ${viewModelClass.simpleName} の ViewModel factory がありません。")
        @Suppress("UNCHECKED_CAST")
        return factory() as VM
    }
}

/** 差し替え実装が持つダイアログのレジストリ。 */
internal class FakeDialogViewRegistry : DialogViewRegistry {
    /** 登録された ViewModel factory の表。 */
    val viewModelFactories: FakeViewModelFactories = FakeViewModelFactories()

    override fun <R, VM : DialogViewModel<R>> registerViewModel(
        viewModelClass: KClass<VM>,
        factory: () -> VM,
    ) {
        viewModelFactories.put(viewModelClass, factory)
    }
}

/** 差し替え実装が持つカスタム Loading のレジストリ。 */
internal class FakeLoadingViewRegistry : LoadingViewRegistry {
    /** 登録された ViewModel factory の表。 */
    val viewModelFactories: FakeViewModelFactories = FakeViewModelFactories()

    override fun <VM : LoadingViewModel> registerViewModel(
        viewModelClass: KClass<VM>,
        factory: () -> VM,
    ) {
        viewModelFactories.put(viewModelClass, factory)
    }
}

/** 差し替え実装が持つカスタム Toast のレジストリ。 */
internal class FakeToastViewRegistry : ToastViewRegistry {
    /** 登録された ViewModel factory の表。 */
    val viewModelFactories: FakeViewModelFactories = FakeViewModelFactories()

    override fun <VM : ToastViewModel> registerViewModel(
        viewModelClass: KClass<VM>,
        factory: () -> VM,
    ) {
        viewModelFactories.put(viewModelClass, factory)
    }
}
