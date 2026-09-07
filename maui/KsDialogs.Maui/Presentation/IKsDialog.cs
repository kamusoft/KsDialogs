using System;
using System.Threading.Tasks;
using Microsoft.Maui.Controls;

namespace KsDialogs;

/// <summary>
/// ダイアログ表示の契約。
/// </summary>
/// <remarks>
/// 既定 singleton エントリ (<see cref="Dialog.Instance"/>) と DI 注入のどちらからでも同じ契約で呼び出せる
/// (core/ADR-0002)。
/// </remarks>
public interface IKsDialog
{
    /// <summary>ViewModel 型と MAUI View factory の紐付け。全ての入口が同じレジストリを共有する。</summary>
    DialogViewRegistry Registry { get; }

    /// <summary>
    /// ViewModel を渡してダイアログを表示し、結果を待つ。
    /// </summary>
    /// <remarks>
    /// 結果型は ViewModel の宣言から導出され、completed(結果値) か cancelled のどちらかをちょうど 1 回返す。
    /// 提示先の指定は不要で、任意のスレッドから呼び出せる。
    /// 構成エラー (未登録の ViewModel 型・値型の ViewModel・提示先不在) では結果を返さず、
    /// <see cref="DialogException"/> で失敗した <see cref="Task"/> を返す。
    /// <para>
    /// 静的メタ属性 (背後の覆いや外側タップの扱いなど) は中身の性質なので、この面では渡せない。
    /// 中身への添付だけで供給する (core/ADR-0015)。
    /// </para>
    /// </remarks>
    /// <typeparam name="TResult">ViewModel が宣言する結果値の型。</typeparam>
    /// <param name="viewModel">表示するダイアログの ViewModel。</param>
    /// <param name="placement">
    /// この呼び出しでの置き場所。渡すと中身に添付された置き場所をまるごと置換する (core/ADR-0015)。
    /// <see langword="null"/> なら添付、添付もなければ契約の既定値が使われる。
    /// </param>
    /// <returns>completed(結果値) または cancelled。</returns>
    Task<DialogResult<TResult>> ShowAsync<TResult>(
        IDialogViewModel<TResult> viewModel,
        DialogPlacement? placement = null);

    /// <summary>
    /// 登録せずに、その場で渡した factory の中身を表示して結果を待つ (core/ADR-0013)。
    /// </summary>
    /// <remarks>
    /// factory の形も結果の返し方も登録経路とまったく同じで、<paramref name="placement"/> の意味も変わらない。
    /// <b>レジストリの状態は一切変わらない</b> — 一時的にも登録せず、その解除も起こらない。
    /// よって同じ ViewModel 型の登録があってもそれは使われず、登録内容もこの呼び出しの前後で変わらない。
    /// 同じ型のインライン表示を並行させても、factory・<see cref="DialogNotifier{TResult}"/>・結果は
    /// それぞれ独立する。
    /// <para>
    /// インライン表示したからといってその ViewModel 型が登録済みになるわけではないため、
    /// あとからレジストリ経由の
    /// <see cref="ShowAsync{TResult}(IDialogViewModel{TResult}, DialogPlacement)"/> を呼べば未登録の
    /// <see cref="DialogException"/> になる。
    /// </para>
    /// </remarks>
    /// <typeparam name="TViewModel">表示する ViewModel の型。</typeparam>
    /// <typeparam name="TResult">ViewModel が宣言する結果値の型。</typeparam>
    /// <param name="viewModel">表示するダイアログの ViewModel。</param>
    /// <param name="factory">中身の MAUI View を生成する関数。</param>
    /// <param name="placement">
    /// この呼び出しでの置き場所。渡すと中身に添付された置き場所をまるごと置換する (core/ADR-0015)。
    /// </param>
    /// <returns>completed(結果値) または cancelled。</returns>
    Task<DialogResult<TResult>> ShowAsync<TViewModel, TResult>(
        TViewModel viewModel,
        Func<TViewModel, DialogNotifier<TResult>, View> factory,
        DialogPlacement? placement = null)
        where TViewModel : class, IDialogViewModel<TResult>;

    /// <summary>
    /// 真偽値の顔で宣言した ViewModel を、登録せずにその場で表示して結果を待つ
    /// (core/ADR-0012・0013)。
    /// </summary>
    /// <remarks>
    /// 結果型を型引数に書かない省略形で、レジストリに干渉しない点も含めて挙動は
    /// <see cref="ShowAsync{TViewModel, TResult}(TViewModel, Func{TViewModel, DialogNotifier{TResult}, View}, DialogPlacement?)"/>
    /// に真偽値を渡した場合とまったく同じ。
    /// </remarks>
    /// <typeparam name="TViewModel">表示する ViewModel の型。</typeparam>
    /// <param name="viewModel">表示するダイアログの ViewModel。</param>
    /// <param name="factory">中身の MAUI View を生成する関数。</param>
    /// <param name="placement">
    /// この呼び出しでの置き場所。渡すと中身に添付された置き場所をまるごと置換する (core/ADR-0015)。
    /// </param>
    /// <returns>completed(真偽値) または cancelled。</returns>
    Task<DialogResult<bool>> ShowAsync<TViewModel>(
        TViewModel viewModel,
        Func<TViewModel, DialogNotifier<bool>, View> factory,
        DialogPlacement? placement = null)
        where TViewModel : class, IDialogViewModel;

    /// <summary>
    /// ViewModel の<b>型</b>を渡してダイアログを表示し、結果を待つ (core/ADR-0020・0021)。
    /// </summary>
    /// <remarks>
    /// ViewModel はレジストリに登録された ViewModel factory
    /// (<see cref="DialogViewRegistry.RegisterViewModel{TViewModel, TResult}"/> や
    /// 1 行登録 <c>RegisterForDialog</c>) が作る。
    /// 実行順序は「ViewModel 生成 → configure の完了 → 中身の生成 → 提示」で固定されており、
    /// configure が設定した状態は中身の初期化から必ず読める。生成と configure は UI スレッドで実行される。
    /// <para>
    /// ViewModel factory が未登録の場合と、生成・configure が失敗した場合は、結果を返さずにその失敗が
    /// 呼び出し元へ届く — 提示には進まず、cancelled 等の結果には化けない。
    /// 結果報告口の供給・結果型の復元・<paramref name="placement"/> の意味はインスタンス渡しの
    /// show と同じである。
    /// </para>
    /// </remarks>
    /// <typeparam name="TViewModel">表示する ViewModel の型。</typeparam>
    /// <typeparam name="TResult">ViewModel が宣言する結果値の型。</typeparam>
    /// <param name="configure">生成した ViewModel の状態を整える処理。省略できる。</param>
    /// <param name="placement">この呼び出しでの置き場所。意味はインスタンス渡しの show と同じ。</param>
    /// <returns>completed(結果値) または cancelled。</returns>
    Task<DialogResult<TResult>> ShowAsync<TViewModel, TResult>(
        Action<TViewModel>? configure = null,
        DialogPlacement? placement = null)
        where TViewModel : class, IDialogViewModel<TResult>;

    /// <summary>
    /// ViewModel の<b>型</b>を渡し、非同期の configure を適用してから表示する (core/ADR-0019)。
    /// </summary>
    /// <remarks>
    /// configure の完了を待ってから中身の生成に進むため、非同期に用意した状態も表示前に反映される。
    /// configure を省略できない点以外は同期版と同じ。
    /// </remarks>
    /// <typeparam name="TViewModel">表示する ViewModel の型。</typeparam>
    /// <typeparam name="TResult">ViewModel が宣言する結果値の型。</typeparam>
    /// <param name="configure">生成した ViewModel の状態を整える非同期処理。</param>
    /// <param name="placement">この呼び出しでの置き場所。意味はインスタンス渡しの show と同じ。</param>
    /// <returns>completed(結果値) または cancelled。</returns>
    Task<DialogResult<TResult>> ShowAsync<TViewModel, TResult>(
        Func<TViewModel, Task> configure,
        DialogPlacement? placement = null)
        where TViewModel : class, IDialogViewModel<TResult>;

    /// <summary>
    /// 真偽値の顔で宣言した ViewModel の<b>型</b>を渡して表示する (core/ADR-0012・0021)。
    /// </summary>
    /// <remarks>
    /// 結果型を型引数に書かない省略形で、挙動は 2 型引数の型指定 show に真偽値を渡した場合と同じ。
    /// </remarks>
    /// <typeparam name="TViewModel">表示する ViewModel の型。</typeparam>
    /// <param name="configure">生成した ViewModel の状態を整える処理。省略できる。</param>
    /// <param name="placement">この呼び出しでの置き場所。意味はインスタンス渡しの show と同じ。</param>
    /// <returns>completed(真偽値) または cancelled。</returns>
    Task<DialogResult<bool>> ShowAsync<TViewModel>(
        Action<TViewModel>? configure = null,
        DialogPlacement? placement = null)
        where TViewModel : class, IDialogViewModel;

    /// <summary>
    /// 真偽値の顔で宣言した ViewModel の<b>型</b>を渡し、非同期の configure を適用してから表示する
    /// (core/ADR-0012・0019)。
    /// </summary>
    /// <typeparam name="TViewModel">表示する ViewModel の型。</typeparam>
    /// <param name="configure">生成した ViewModel の状態を整える非同期処理。</param>
    /// <param name="placement">この呼び出しでの置き場所。意味はインスタンス渡しの show と同じ。</param>
    /// <returns>completed(真偽値) または cancelled。</returns>
    Task<DialogResult<bool>> ShowAsync<TViewModel>(
        Func<TViewModel, Task> configure,
        DialogPlacement? placement = null)
        where TViewModel : class, IDialogViewModel;
}
