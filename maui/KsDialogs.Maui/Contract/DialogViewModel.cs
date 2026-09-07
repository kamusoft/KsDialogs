namespace KsDialogs;

/// <summary>
/// ダイアログの ViewModel が準拠する契約。
/// </summary>
/// <remarks>
/// 結果型は呼び出し側ではなく ViewModel 自身が <typeparamref name="TResult"/> として宣言し、
/// show の戻り値と結果報告部品 (<see cref="DialogNotifier{TResult}"/>) の型はここから導出される。
/// 宣言と異なる結果型で受け取ったり報告したりする書き方はコンパイルできない (core/ADR-0003)。
/// <para>
/// ViewModel は結果と状態の運び手であり、大きさや置き場所といった UI の関心は持たない。
/// それらのメタ属性は中身 (View) への添付 (<c>ksd:Dialog.*</c>) と show の placement 引数で供給する
/// (core/ADR-0015)。
/// </para>
/// </remarks>
/// <typeparam name="TResult">この ViewModel が宣言する結果値の型。</typeparam>
public interface IDialogViewModel<TResult>
{
}

/// <summary>
/// 真偽値の結果を返す ViewModel の顔 (core/ADR-0012)。
/// </summary>
/// <remarks>
/// ダイアログの結果は「OK か否か」の真偽値であることが大半なので、その場合は結果型を書かずに
/// <c>class ConfirmViewModel : IDialogViewModel</c> と宣言できる。
/// この顔で宣言した ViewModel は ViewModel 型だけを型引数に取る
/// <see cref="DialogViewRegistry.Register{TViewModel}(System.Func{TViewModel, DialogNotifier{bool}, Microsoft.Maui.Controls.View})"/>
/// で登録でき、show は
/// <c>DialogResult&lt;bool&gt;</c> を返す。
/// 結果型を明示する <see cref="IDialogViewModel{TResult}"/> の形はそのまま併存する。
/// </remarks>
public interface IDialogViewModel : IDialogViewModel<bool>
{
}
