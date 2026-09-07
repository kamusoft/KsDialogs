using System;
using Microsoft.Maui.Controls;

namespace KsDialogs;

/// <summary>
/// レジストリが保持する View 生成関数の型消去表現。
/// </summary>
/// <remarks>
/// ViewModel と結果チャネルを受け取り、そのダイアログの中身となる MAUI View を新規に生成する。
/// </remarks>
/// <param name="viewModel">show に渡された ViewModel。登録キーと同じ型であることは解決側が保証する。</param>
/// <param name="resultChannel">その show の結果チャネル。生成する報告口の宛先になる。</param>
/// <returns>ダイアログの中身になる MAUI View。</returns>
internal delegate View DialogViewFactory(object viewModel, DialogResultChannel resultChannel);

/// <summary>
/// 宣言結果型で型付いた factory を、提示層が扱う型消去表現へ変換する。
/// </summary>
/// <remarks>
/// 登録経路とインライン show 経路の双方がこの変換を通るため、どちらから渡した factory も
/// 提示・結果・レイアウトの同じ 1 系統に載る (core/ADR-0011)。
/// </remarks>
internal static class DialogViewFactories
{
    /// <summary>型付き factory を型消去表現に包む。</summary>
    /// <typeparam name="TViewModel">factory が受け取る ViewModel の型。</typeparam>
    /// <typeparam name="TResult">ViewModel が宣言する結果値の型。</typeparam>
    /// <param name="factory">中身の MAUI View を生成する関数。</param>
    /// <returns>提示層に渡せる型消去された factory。</returns>
    public static DialogViewFactory Erase<TViewModel, TResult>(
        Func<TViewModel, DialogNotifier<TResult>, View> factory)
        where TViewModel : class, IDialogViewModel<TResult> =>
        (viewModel, resultChannel) =>
            // 解決は viewModel の実型をキーに行われるため、ここでの型は渡された factory のものと一致する
            factory((TViewModel)viewModel, new DialogNotifier<TResult>(resultChannel));

    /// <summary>ViewModel だけを受け取る factory を型消去表現に包む (core/ADR-0018)。</summary>
    /// <remarks>
    /// 結果報告口は中身の中から <c>viewModel.Notifier</c> で取り出す。
    /// 紐付けは提示層がこの factory を呼ぶ前に済ませているため、factory 本体からも読める。
    /// </remarks>
    /// <typeparam name="TViewModel">factory が受け取る ViewModel の型。</typeparam>
    /// <param name="factory">中身の MAUI View を生成する関数。</param>
    /// <returns>提示層に渡せる型消去された factory。</returns>
    public static DialogViewFactory Erase<TViewModel>(Func<TViewModel, View> factory)
        where TViewModel : class =>
        (viewModel, _) => factory((TViewModel)viewModel);
}
