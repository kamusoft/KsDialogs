using System;
using Microsoft.Maui.Controls;

namespace KsDialogs;

/// <summary>
/// ViewModel 型から View factory と ViewModel factory を解決する順序 (maui/ADR-0005)。
/// </summary>
/// <remarks>
/// 順序は「明示レジストリ → fallback resolver → 構成ミスとして失敗」で、明示登録が常に優先される。
/// 判定は View / ViewModel のスロットごとに独立して働くため、片方だけを明示登録して
/// もう片方を fallback に任せる構成が成立する。
/// </remarks>
internal static class DialogResolution
{
    /// <summary>その ViewModel 型の View factory を解決する。</summary>
    /// <param name="registry">解決に使うレジストリ。</param>
    /// <param name="entry">呼び出し時点のエントリのスナップショット。未登録なら <see langword="null"/>。</param>
    /// <param name="viewModelType">解決したい ViewModel の型。</param>
    /// <returns>提示層へ渡せる型消去された View factory。</returns>
    public static DialogViewFactory ResolveViewFactory(
        DialogViewRegistry registry,
        DialogRegistryEntry? entry,
        Type viewModelType)
    {
        if (entry?.ViewFactory is { } registered)
        {
            return registered;
        }

        if (registry.Fallbacks?.View is { } fallback)
        {
            // fallback が生成した View は明示登録の factory を通らないため、
            // ViewModel との結び付け (BindingContext) はライブラリが受け持つ
            return (viewModel, _) =>
            {
                View view = fallback(viewModelType, DialogServiceProvider.Require())
                    ?? throw new DialogException.ViewFactoryNotRegistered(TypeName(viewModelType));
                view.BindingContext = viewModel;
                return view;
            };
        }

        throw new DialogException.ViewFactoryNotRegistered(TypeName(viewModelType));
    }

    /// <summary>その ViewModel 型の ViewModel factory を解決する。</summary>
    /// <param name="registry">解決に使うレジストリ。</param>
    /// <param name="entry">呼び出し時点のエントリのスナップショット。未登録なら <see langword="null"/>。</param>
    /// <param name="viewModelType">解決したい ViewModel の型。</param>
    /// <returns>型消去された ViewModel factory。</returns>
    public static DialogViewModelFactory ResolveViewModelFactory(
        DialogViewRegistry registry,
        DialogRegistryEntry? entry,
        Type viewModelType)
    {
        if (entry?.ViewModelFactory is { } registered)
        {
            return registered;
        }

        if (registry.Fallbacks?.ViewModel is { } fallback)
        {
            return () => fallback(viewModelType, DialogServiceProvider.Require())
                ?? throw new DialogException.ViewModelFactoryNotRegistered(TypeName(viewModelType));
        }

        throw new DialogException.ViewModelFactoryNotRegistered(TypeName(viewModelType));
    }

    /// <summary>失敗メッセージに載せる ViewModel の型名。</summary>
    /// <param name="viewModelType">対象の型。</param>
    /// <returns>名前空間つきの型名。取れなければ短い型名。</returns>
    public static string TypeName(Type viewModelType) => viewModelType.FullName ?? viewModelType.Name;
}
