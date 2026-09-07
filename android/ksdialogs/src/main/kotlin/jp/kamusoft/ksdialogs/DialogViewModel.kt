package jp.kamusoft.ksdialogs

/**
 * ダイアログの ViewModel が準拠する契約。
 *
 * 結果型は呼び出し側ではなく ViewModel 自身が [R] として宣言し、
 * show の戻り値と結果報告部品 ([DialogNotifier]) の型はここから導出される。
 * 宣言と異なる結果型で受け取ったり報告したりする書き方はコンパイルできない (core/ADR-0003)。
 *
 * ViewModel は結果と状態の運び手であり、大きさや置き場所といった UI の関心は持たない。
 * それらのメタ属性 ([DialogOptions] / [DialogPlacement]) は中身の View への添付と
 * show の引数で供給する (core/ADR-0015)。
 *
 * 準拠できるのは参照型だけである。表示中の結果報告口 ([notifier]) をインスタンスの同一性で引くため、
 * boxing のたびに同一性が失われる value class は扱えない (core/ADR-0018)。
 * Kotlin ではこの制限をコンパイル時に表現できないため、value class の ViewModel は
 * 登録の時点と、インライン factory を含むすべての show の提示時に
 * [DialogException.ValueClassViewModel] として拒否する。
 *
 * @param R この ViewModel が宣言する結果値の型
 */
public interface DialogViewModel<R>

/**
 * 表示中のこの ViewModel に紐付いた結果報告口 (core/ADR-0018)。
 *
 * show が中身を生成する直前に紐付き、結果が呼び出し元へ渡る前に外れる。
 * したがって show の前と終わったあとは null で、表示中だけ値を返す。
 * 型は ViewModel が宣言した結果型に固定され、factory の引数で渡される報告口と同じ配送先を指す。
 *
 * 紐付けはインスタンスの同一性で引くため、等価な別インスタンスの報告口は返らない。
 *
 * ```
 * class ConfirmViewModel : SimpleDialogViewModel {
 *     fun onOkTapped() { notifier?.complete(true) }
 * }
 * ```
 */
public val <R> DialogViewModel<R>.notifier: DialogNotifier<R>?
    get() = DialogNotifierBindings.resultChannel(this)?.let { DialogNotifier(it) }
