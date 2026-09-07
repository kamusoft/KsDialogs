package jp.kamusoft.ksdialogs.kmp

import kotlin.coroutines.cancellation.CancellationException
import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.HiddenFromObjC
import kotlin.reflect.KClass

/**
 * 共有コードから使うダイアログ表示の契約。
 *
 * 既定 singleton エントリ ([Dialog.instance]) と DI 注入のどちらからでも同じ契約で呼び出せる。
 * ViewModel factory の登録と、型を渡す表示の前段 (ViewModel の生成と configure) はこの共有コード側が担い、
 * 中身の View の作り方と実際の提示は各 OS の Native ライブラリが持つ。
 * テストでは Native 実装なしにこの interface を差し替えられる。
 */
public interface KsDialog {
    /** ViewModel 型と作り方の紐付け。全ての入口が同じレジストリを共有する。 */
    public val registry: DialogViewRegistry

    /**
     * ViewModel を渡してダイアログを表示し、結果を待つ。
     *
     * 結果型は ViewModel の宣言から導出され、completed(結果値) か cancelled のどちらかをちょうど1回返す。
     * 提示先の指定は不要で、任意のスレッドから呼び出せる。
     * 構成エラー (未登録の ViewModel 型・提示先不在) では結果を返さずに [DialogException] を投げる。
     * Swift から呼ぶ場合、この例外は NSError として届く。
     *
     * @param placement この呼び出しでの置き場所。渡すと中身の View に添付された置き場所を
     *   オブジェクトまるごと置換する (core/ADR-0015)。省略すれば添付、添付もなければ契約の既定値が使われる。
     *   静的メタ属性は View の性質として各 OS の View 定義側で完結するため、この面では供給しない
     */
    @Throws(DialogException::class, CancellationException::class)
    public suspend fun <R> show(
        viewModel: DialogViewModel<R>,
        placement: DialogPlacement? = null,
    ): DialogResult<R>

    /**
     * ViewModel の型を渡してダイアログを表示し、結果を待つ。
     *
     * 中身の ViewModel は [registry] に登録した ViewModel factory で作られ、[configure] の完了後に表示へ進む。
     * 生成と [configure] は呼び出し元の文脈 (呼び出し元のコルーチン文脈) でそのまま実行される。
     * 結果の意味と置き場所の扱いは ViewModel を渡す表示と同じ。
     *
     * ViewModel factory が未登録のとき、および factory が登録キーと違うクラスを返したときは、
     * 構成エラーとして [DialogException] を投げて表示を行わない。
     * ViewModel factory と [configure] が投げた例外は表示へ進まずそのまま呼び出し元へ伝わる。
     *
     * この呼び出しは共有 Kotlin コード専用で、Swift / Objective-C からは見えない。
     *
     * @param viewModelClass 表示する ViewModel のクラス参照。ViewModel factory の登録キーと同じもの
     * @param placement この呼び出しでの置き場所。省略時の扱いは ViewModel を渡す表示と同じ
     * @param configure 生成した ViewModel を表示前に整える処理。省略すれば生成物をそのまま表示する
     */
    @OptIn(ExperimentalObjCRefinement::class)
    @HiddenFromObjC
    public suspend fun <R, VM : DialogViewModel<R>> show(
        viewModelClass: KClass<VM>,
        placement: DialogPlacement? = null,
        configure: (suspend (VM) -> Unit)? = null,
    ): DialogResult<R>
}
