package jp.kamusoft.ksdialogs

import android.content.Context
import android.view.View
import kotlin.reflect.KClass

/**
 * ViewModel 型をキーに View factory と ViewModel factory を引くレジストリ (core/ADR-0004・0021)。
 *
 * キーは Kotlin のクラス参照 (`Xxx::class`) そのもので、リフレクションによる型探索は行わない。
 * 既定 singleton エントリ ([Dialog.instance]) と DI 注入で使うインスタンスは [shared] を共有するため、
 * どちらの入口から登録しても同じ紐付けが引ける。
 * 登録・解決は任意のスレッドから行える。
 *
 * 1つの ViewModel 型のエントリは View factory と ViewModel factory の2スロットからなり、
 * 再登録はスロット単位の後勝ちで、もう片方のスロットは保持される。
 */
public class DialogViewRegistry {
    private val lock = Any()
    private val entries = mutableMapOf<KClass<*>, DialogRegistryEntry>()

    /**
     * ViewModel 型に対する View factory を登録する。
     *
     * factory は show のたびに呼ばれ、View を毎回新規に生成する (core/ADR-0005)。
     * factory のレシーバは、View の生成に使う提示先画面の [Context]。
     * 受け取る [DialogNotifier] は ViewModel が宣言した結果型に固定され、show 1回ごとに新しいものが渡る。
     * 報告口を引数で受け取らず、中身から `viewModel.notifier` で引く登録の形もある。
     *
     * @param viewModelClass 登録キーになる ViewModel のクラス参照
     * @param factory 中身の View を生成する関数
     */
    public fun <R, VM : DialogViewModel<R>> register(
        viewModelClass: KClass<VM>,
        factory: Context.(VM, DialogNotifier<R>) -> View,
    ) {
        putViewFactory(viewModelClass, erasedDialogViewFactory(factory))
    }

    /**
     * ViewModel 型に対する View factory を、ViewModel だけを受け取る形で登録する (core/ADR-0018)。
     *
     * 結果報告口は中身の中から `viewModel.notifier` で取り出す。
     * 紐付けは提示層が factory を呼ぶ前に済ませているため、factory 本体からも読める。
     * View のコンストラクタが `(Context, VM)` の形なら、コンストラクタ参照をそのまま渡せる。
     *
     * @param viewModelClass 登録キーになる ViewModel のクラス参照
     * @param factory 中身の View を生成する関数
     */
    public fun <R, VM : DialogViewModel<R>> register(
        viewModelClass: KClass<VM>,
        factory: Context.(VM) -> View,
    ) {
        putViewFactory(viewModelClass, erasedDialogViewFactory(factory))
    }

    /**
     * ViewModel 型に対する ViewModel factory を登録する (core/ADR-0021)。
     *
     * 型指定 show ([KsDialog.show] のクラス参照を渡す形) はこの factory で ViewModel を作る。
     * factory は show のたびに呼ばれ、UI スレッド (Main dispatcher) で実行される。
     * View factory とはスロットが別なので、どちらを登録し直しても他方は保持される。
     *
     * @param viewModelClass 登録キーになる ViewModel のクラス参照
     * @param factory ViewModel を生成する関数
     */
    public fun <R, VM : DialogViewModel<R>> registerViewModel(
        viewModelClass: KClass<VM>,
        factory: () -> VM,
    ) {
        viewModelClass.requireReferenceTypeViewModel()
        val erasedFactory = DialogViewModelFactory { factory() }
        updateEntry(viewModelClass) { it.copy(viewModelFactory = erasedFactory) }
    }

    /** 型消去した View factory を該当スロットへ入れる。 */
    private fun putViewFactory(viewModelClass: KClass<*>, factory: DialogViewFactory) {
        viewModelClass.requireReferenceTypeViewModel()
        updateEntry(viewModelClass) { it.copy(viewFactory = factory) }
    }

    /** エントリの片方のスロットだけを置き換える。未登録の型なら空のエントリから作る。 */
    private fun updateEntry(
        viewModelClass: KClass<*>,
        update: (DialogRegistryEntry) -> DialogRegistryEntry,
    ) {
        synchronized(lock) {
            entries[viewModelClass] = update(entries[viewModelClass] ?: DialogRegistryEntry())
        }
    }

    /**
     * キーに対応するエントリを返す。どちらのスロットも未登録なら null。
     * 返すのは呼び出し時点のスナップショットで、以後の再登録には影響されない。
     */
    internal fun entry(viewModelClass: KClass<*>): DialogRegistryEntry? =
        synchronized(lock) { entries[viewModelClass] }

    /** キーに対応する View factory を返す。未登録なら null。 */
    internal fun factory(viewModelClass: KClass<*>): DialogViewFactory? = entry(viewModelClass)?.viewFactory

    public companion object {
        /** 全入口が共有する既定のレジストリ。 */
        public val shared: DialogViewRegistry = DialogViewRegistry()
    }
}
