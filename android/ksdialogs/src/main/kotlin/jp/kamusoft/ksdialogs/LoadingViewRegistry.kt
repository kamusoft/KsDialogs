package jp.kamusoft.ksdialogs

import android.content.Context
import android.view.View
import kotlin.reflect.KClass

/**
 * カスタム Loading の ViewModel 型をキーに View factory と ViewModel factory を引くレジストリ。
 *
 * Dialog のレジストリ ([DialogViewRegistry]) とは独立しており、同じ ViewModel 型を
 * 両方へ別々の View で登録できる。片方の再登録は他方に影響しない。
 * 登録・解決は任意のスレッドから行える。
 *
 * 1つの ViewModel 型のエントリは View factory と ViewModel factory の2スロットからなり、
 * 再登録はスロット単位の後勝ちで、もう片方のスロットは保持される。
 */
public class LoadingViewRegistry {
    private val lock = Any()
    private val entries = mutableMapOf<KClass<*>, LoadingRegistryEntry>()

    /**
     * ViewModel 型に対する従来 View 系の factory を登録する。
     *
     * 同じ型の View factory への再登録は後勝ちで置き換え、ViewModel factory は保持される。
     * factory は表示のたびに呼ばれ、View を毎回新規に生成する。
     * factory のレシーバは、View の生成に使う提示先画面の [Context]。
     *
     * @param viewModelClass 登録キーになる ViewModel のクラス参照
     * @param factory 中身の View を生成する関数
     */
    public fun <VM : LoadingViewModel> register(
        viewModelClass: KClass<VM>,
        factory: Context.(VM) -> View,
    ) {
        register(viewModelClass, erasedLoadingViewFactory(factory))
    }

    /**
     * ViewModel 型に対する ViewModel factory を登録する。
     *
     * 型指定の表示 (クラス参照を渡す [KsLoading.show] / [KsLoading.start]) はこの factory で
     * ViewModel を作る。同じ型の ViewModel factory への再登録は後勝ちで置き換え、
     * View factory は保持される。
     * factory は表示のたびに呼ばれ、UI スレッドで実行される。
     *
     * @param viewModelClass 登録キーになる ViewModel のクラス参照
     * @param factory ViewModel を生成する関数
     */
    public fun <VM : LoadingViewModel> registerViewModel(
        viewModelClass: KClass<VM>,
        factory: () -> VM,
    ) {
        viewModelClass.requireReferenceTypeViewModel()
        val erasedFactory = LoadingViewModelFactory { factory() }
        updateEntry(viewModelClass) { it.copy(viewModelFactory = erasedFactory) }
    }

    /** 型消去された View factory を登録する。宣言的 UI 系の登録面もこの口を通る。 */
    internal fun register(viewModelClass: KClass<*>, factory: LoadingViewFactory) {
        viewModelClass.requireReferenceTypeViewModel()
        updateEntry(viewModelClass) { it.copy(viewFactory = factory) }
    }

    /** エントリの片方のスロットだけを置き換える。未登録の型なら空のエントリから作る。 */
    private fun updateEntry(
        viewModelClass: KClass<*>,
        update: (LoadingRegistryEntry) -> LoadingRegistryEntry,
    ) {
        synchronized(lock) {
            entries[viewModelClass] = update(entries[viewModelClass] ?: LoadingRegistryEntry())
        }
    }

    /**
     * キーに対応するエントリを返す。どちらのスロットも未登録なら null。
     * 返すのは呼び出し時点のスナップショットで、以後の再登録には影響されない。
     */
    internal fun entry(viewModelClass: KClass<*>): LoadingRegistryEntry? =
        synchronized(lock) { entries[viewModelClass] }

    /**
     * キーに対応する View factory を返す。未登録なら null。
     * 返すのは呼び出し時点のスナップショットで、以後の再登録には影響されない。
     */
    internal fun factory(viewModelClass: KClass<*>): LoadingViewFactory? =
        entry(viewModelClass)?.viewFactory

    public companion object {
        /** 全入口が共有する既定のレジストリ。 */
        public val shared: LoadingViewRegistry = LoadingViewRegistry()
    }
}
