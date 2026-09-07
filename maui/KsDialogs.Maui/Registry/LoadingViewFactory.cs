using System;
using Microsoft.Maui.Controls;

namespace KsDialogs;

/// <summary>
/// Loading のレジストリが保持する View 生成関数の型消去表現。
/// </summary>
/// <remarks>
/// 結果報告口を持たないこと以外は <see cref="DialogViewFactory"/> と同じ役割で、
/// ViewModel を受け取り、その Loading の中身となる MAUI View を新規に生成する。
/// </remarks>
/// <param name="viewModel">表示に渡された ViewModel。登録キーと同じ型であることは解決側が保証する。</param>
/// <returns>Loading の中身になる MAUI View。</returns>
internal delegate View LoadingViewFactory(object viewModel);

/// <summary>
/// ViewModel 型で型付いた factory を、提示層が扱う型消去表現へ変換する。
/// </summary>
/// <remarks>
/// 登録経路とインライン表示経路の双方がこの変換を通るため、どちらから渡した factory も
/// 提示・レイアウトの同じ 1 系統に載る (core/ADR-0011)。
/// </remarks>
internal static class LoadingViewFactories
{
    /// <summary>型付き factory を型消去表現に包む。</summary>
    /// <typeparam name="TViewModel">factory が受け取る ViewModel の型。</typeparam>
    /// <param name="factory">中身の MAUI View を生成する関数。</param>
    /// <returns>提示層に渡せる型消去された factory。</returns>
    public static LoadingViewFactory Erase<TViewModel>(Func<TViewModel, View> factory)
        where TViewModel : class =>
        // 解決は viewModel の実型をキーに行われるため、ここでの型は渡された factory のものと一致する
        viewModel => factory((TViewModel)viewModel);
}
