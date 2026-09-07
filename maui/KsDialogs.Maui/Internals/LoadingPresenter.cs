using System;
using Microsoft.Maui.Controls;

namespace KsDialogs;

/// <summary>
/// カスタム Loading の表示 1 回分の解決。中身の生成は委譲面が提示先を確保した後に行う。
/// </summary>
/// <remarks>
/// 未登録 (Loading レジストリに紐付けが無い) と値型の ViewModel は構成ミスとして失敗させ、
/// 表示にもスコープ形の処理にも進まない (fail-fast)。提示環境の不在はここでは判定しない —
/// Loading では表示が成立しなくても処理は実行されるため、委譲面の側の関心になる。
/// </remarks>
internal static class LoadingPresenter
{
    /// <summary>レジストリから解決した factory で、その表示の要求を組み立てる。</summary>
    /// <param name="viewModel">表示に渡された ViewModel。</param>
    /// <param name="registry">解決に使うレジストリ。</param>
    /// <param name="placement">表示 API の引数で渡された置き場所。</param>
    /// <returns>委譲面へ渡す表示の要求。</returns>
    public static LoadingPresentationRequest Resolve(
        object viewModel,
        LoadingViewRegistry registry,
        DialogPlacement? placement)
    {
        Type viewModelType = RequireReferenceType(viewModel);
        LoadingViewFactory factory = registry.Factory(viewModelType)
            ?? throw new DialogException.ViewFactoryNotRegistered(DialogResolution.TypeName(viewModelType));

        return Compose(viewModel, factory, placement);
    }

    /// <summary>型指定 show / 型指定 start の 2 スロットを、呼び出し時点のスナップショットで解決する。</summary>
    /// <remarks>
    /// どちらのスロットも構成ミスとして区別して失敗させる。一括解決 (fallback resolver) は
    /// Loading には適用せず、明示登録と 1 行登録糖衣だけで解決する (core/ADR-0035)。
    /// </remarks>
    /// <param name="registry">解決に使うレジストリ。</param>
    /// <param name="viewModelType">解決したい ViewModel の型。</param>
    /// <returns>ViewModel factory と View factory の組。</returns>
    public static (LoadingViewModelFactory ViewModelFactory, LoadingViewFactory ViewFactory) ResolveTyped(
        LoadingViewRegistry registry,
        Type viewModelType)
    {
        LoadingRegistryEntry? entry = registry.Entry(viewModelType);
        LoadingViewModelFactory viewModelFactory = entry?.ViewModelFactory
            ?? throw new DialogException.ViewModelFactoryNotRegistered(DialogResolution.TypeName(viewModelType));
        LoadingViewFactory viewFactory = entry?.ViewFactory
            ?? throw new DialogException.ViewFactoryNotRegistered(DialogResolution.TypeName(viewModelType));

        return (viewModelFactory, viewFactory);
    }

    /// <summary>
    /// 型指定 show で生成・configure を終えた ViewModel から、その表示の要求を組み立てる。
    /// </summary>
    /// <remarks>
    /// 進捗の転送先になるのは生成した ViewModel そのもので、インスタンス渡し show と同じ経路に載る。
    /// </remarks>
    /// <param name="viewModel">生成して configure を終えた ViewModel。</param>
    /// <param name="factory">呼び出し時点で解決した型消去済みの View factory。</param>
    /// <param name="placement">表示 API の引数で渡された置き場所。</param>
    /// <returns>委譲面へ渡す表示の要求。</returns>
    public static LoadingPresentationRequest ComposeTyped(
        object viewModel,
        LoadingViewFactory factory,
        DialogPlacement? placement) =>
        Compose(viewModel, factory, placement);

    /// <summary>その場で渡された factory で、その表示の要求を組み立てる (core/ADR-0013)。</summary>
    /// <remarks>レジストリは参照も更新もしないため、この表示は既存の登録に干渉しない。</remarks>
    /// <param name="viewModel">表示に渡された ViewModel。</param>
    /// <param name="factory">その場で使う型消去済みの factory。</param>
    /// <param name="placement">表示 API の引数で渡された置き場所。</param>
    /// <returns>委譲面へ渡す表示の要求。</returns>
    public static LoadingPresentationRequest ResolveInline(
        object viewModel,
        LoadingViewFactory factory,
        DialogPlacement? placement)
    {
        RequireReferenceType(viewModel);
        return Compose(viewModel, factory, placement);
    }

    /// <summary>既定ローディングの表示の要求を組み立てる。</summary>
    /// <param name="message">表示するメッセージ。指定なしは <see langword="null"/>。</param>
    /// <param name="placement">表示 API の引数で渡された置き場所。</param>
    /// <returns>委譲面へ渡す表示の要求。</returns>
    public static LoadingPresentationRequest Builtin(string? message, DialogPlacement? placement) =>
        new(null, message, placement, null);

    /// <summary>中身の生成と進捗の転送先を束ねる。</summary>
    /// <remarks>
    /// 進捗の転送先になるのは <see cref="ILoadingProgressReceiver"/> を実装した ViewModel だけで、
    /// 実装しない ViewModel では転送そのものが起きない (誤りにもならない)。
    /// </remarks>
    private static LoadingPresentationRequest Compose(
        object viewModel,
        LoadingViewFactory factory,
        DialogPlacement? placement) =>
        new(
            () => factory(viewModel),
            null,
            placement,
            viewModel as ILoadingProgressReceiver);

    /// <summary>値型の ViewModel を構成ミスとして弾き、その型を返す。</summary>
    /// <remarks>
    /// 登録と 1 行登録糖衣は <c>class</c> 制約で弾けるが、interface で受ける表示の入口は
    /// コンパイル時に拒否できないため、ここで構成ミスとして失敗させる (core/ADR-0018)。
    /// </remarks>
    /// <param name="viewModel">表示に渡された ViewModel。</param>
    /// <returns>その ViewModel の実型。</returns>
    private static Type RequireReferenceType(object viewModel)
    {
        Type viewModelType = viewModel.GetType();
        return viewModelType.IsValueType
            ? throw new DialogException.ValueTypeViewModel(DialogResolution.TypeName(viewModelType))
            : viewModelType;
    }
}
