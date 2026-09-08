package jp.kamusoft.ksdialogs

import android.content.Context
import android.view.View
import kotlin.reflect.KClass

/**
 * ダイアログ表示の契約。
 *
 * 既定 singleton エントリ ([Dialog.instance]) と DI 注入のどちらからでも同じ契約で呼び出せる (core/ADR-0002)。
 */
public interface KsDialog {
    /** ViewModel 型と View factory の紐付け。全ての入口が同じレジストリを共有する。 */
    public val registry: DialogViewRegistry

    /**
     * ViewModel を渡してダイアログを表示し、結果を待つ。
     *
     * 結果型は ViewModel の宣言から導出され、completed(結果値) か cancelled のどちらかをちょうど1回返す。
     * 提示先の指定は不要で、任意のスレッドから呼び出せる。
     * 構成エラー (未登録の ViewModel 型・提示先不在) では結果を返さずに [DialogException] を投げる。
     *
     * @param placement この呼び出しでの置き場所。渡すと中身の View に添付された [DialogPlacement] を
     *   オブジェクトまるごと置換する (core/ADR-0015)。省略すれば添付、添付もなければ契約の既定値が使われる。
     *   静的メタ属性 ([DialogOptions]) は View の性質なので、show からは供給できない
     */
    public suspend fun <R> show(
        viewModel: DialogViewModel<R>,
        placement: DialogPlacement? = null,
    ): DialogResult<R>

    /**
     * 登録せずに、その場で渡した factory の中身を表示して結果を待つ (core/ADR-0013)。
     *
     * factory の形も結果の返し方も登録経路とまったく同じで、[placement] の意味も変わらない。
     * **レジストリの状態は一切変わらない** — 一時的にも登録せず、その解除も起こらない。
     * よって同じ ViewModel 型の登録があってもそれは使われず、登録内容もこの呼び出しの前後で変わらない。
     * 同じ型のインライン表示を並行させても、factory・[DialogNotifier]・結果はそれぞれ独立する。
     *
     * インライン表示したからといってその ViewModel 型が登録済みになるわけではないため、
     * あとから [show] をレジストリ経由で呼べば未登録の [DialogException] になる。
     *
     * @param viewModel 表示する ViewModel
     * @param placement この呼び出しでの置き場所。渡すと中身の View に添付された [DialogPlacement] を
     *   オブジェクトまるごと置換する (core/ADR-0015)
     * @param factory 中身の View を生成する関数。レシーバは提示先画面の [Context]
     */
    public suspend fun <R, VM : DialogViewModel<R>> show(
        viewModel: VM,
        placement: DialogPlacement? = null,
        factory: Context.(VM, DialogNotifier<R>) -> View,
    ): DialogResult<R>

    /**
     * ViewModel の**型**を渡してダイアログを表示し、結果を待つ (core/ADR-0021)。
     *
     * ViewModel はレジストリに登録された ViewModel factory ([DialogViewRegistry.registerViewModel]) が作る。
     * 実行順序は「ViewModel 生成 → configure の完了 → 中身の生成 → 提示」で固定されており、
     * configure が設定した状態は中身の初期化から必ず読める。
     * 生成と configure は UI スレッド (Main dispatcher) で実行される。
     *
     * ViewModel factory が未登録の場合と、生成・configure が失敗した場合 (キャンセルを含む) は、
     * 結果を返さずにその失敗を投げる — 提示には進まず、cancelled 等の結果には化けない。
     * 結果報告口の供給・結果型の復元・[placement] の意味はインスタンス渡し [show] と同じである。
     *
     * @param viewModelClass 表示する ViewModel のクラス参照
     * @param placement この呼び出しでの置き場所。意味はインスタンス渡し [show] と同じ
     * @param configure 生成した ViewModel の状態を整える関数。中断関数として書ける
     */
    public suspend fun <R, VM : DialogViewModel<R>> show(
        viewModelClass: KClass<VM>,
        placement: DialogPlacement? = null,
        configure: (suspend (VM) -> Unit)? = null,
    ): DialogResult<R>
}
