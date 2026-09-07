using System;

namespace KsDialogs;

/// <summary>
/// カスタム Toast の表示 1 回分の解決。中身の生成は委譲面が提示先を確保した後に行う。
/// </summary>
/// <remarks>
/// 未登録 (Toast レジストリに紐付けが無い) と値型の ViewModel は構成ミスとして呼び出し時点で
/// 同期に失敗させ、表示には進まない (fail-fast)。受理より後の失敗はその表示 1 枚の破棄になり、
/// 呼び出し元へは返らない (core/ADR-0031)。
/// </remarks>
internal static class ToastPresenter
{
    /// <summary>レジストリから解決した factory で、その表示の要求を組み立てる。</summary>
    /// <param name="viewModel">表示に渡された ViewModel。</param>
    /// <param name="registry">解決に使うレジストリ。</param>
    /// <param name="durationMilliseconds">表示するミリ秒。指定なしは <see langword="null"/>。</param>
    /// <param name="placement">表示 API の引数で渡された置き場所。</param>
    /// <returns>委譲面へ渡す表示の要求。</returns>
    public static ToastPresentationRequest Resolve(
        object viewModel,
        ToastViewRegistry registry,
        int? durationMilliseconds,
        DialogPlacement? placement)
    {
        Type viewModelType = RequireReferenceType(viewModel);
        ToastViewFactory factory = registry.Factory(viewModelType)
            ?? throw new DialogException.ViewFactoryNotRegistered(DialogResolution.TypeName(viewModelType));

        return Compose(viewModel, factory, durationMilliseconds, placement);
    }

    /// <summary>型指定 show の 2 スロットを解決し、その表示の要求を組み立てる (core/ADR-0035)。</summary>
    /// <remarks>
    /// 解決は呼び出し時点のスナップショットで行い、どちらのスロットの未登録も構成ミスとして
    /// 呼び出し時点で同期に失敗させる (一括解決 (fallback resolver) は Toast には適用しない)。
    /// ViewModel の生成と configure は中身の生成と同じ受理後の UI スレッドで行うため、そこでの失敗は
    /// 呼び出し元へ返らず、中身を作れなかった表示 1 枚の破棄になる (core/ADR-0031・0033)。
    /// </remarks>
    /// <typeparam name="TViewModel">表示する ViewModel の型。</typeparam>
    /// <param name="registry">解決に使うレジストリ。</param>
    /// <param name="configure">生成した ViewModel の状態を整える処理。省略時は <see langword="null"/>。</param>
    /// <param name="durationMilliseconds">表示するミリ秒。指定なしは <see langword="null"/>。</param>
    /// <param name="placement">表示 API の引数で渡された置き場所。</param>
    /// <returns>委譲面へ渡す表示の要求。</returns>
    public static ToastPresentationRequest ResolveTyped<TViewModel>(
        ToastViewRegistry registry,
        Action<TViewModel>? configure,
        int? durationMilliseconds,
        DialogPlacement? placement)
        where TViewModel : class
    {
        Type viewModelType = typeof(TViewModel);
        ToastRegistryEntry? entry = registry.Entry(viewModelType);
        ToastViewModelFactory viewModelFactory = entry?.ViewModelFactory
            ?? throw new DialogException.ViewModelFactoryNotRegistered(DialogResolution.TypeName(viewModelType));
        ToastViewFactory factory = entry?.ViewFactory
            ?? throw new DialogException.ViewFactoryNotRegistered(DialogResolution.TypeName(viewModelType));

        return new ToastPresentationRequest(
            () =>
            {
                // ViewModel factory は型キーと対で登録されるため、生成物の型は常に一致する
                TViewModel created = (TViewModel)viewModelFactory();
                configure?.Invoke(created);
                return factory(created);
            },
            null,
            durationMilliseconds,
            placement);
    }

    /// <summary>その場で渡された factory で、その表示の要求を組み立てる (core/ADR-0013)。</summary>
    /// <remarks>レジストリは参照も更新もしないため、この表示は既存の登録に干渉しない。</remarks>
    /// <param name="viewModel">表示に渡された ViewModel。</param>
    /// <param name="factory">その場で使う型消去済みの factory。</param>
    /// <param name="durationMilliseconds">表示するミリ秒。指定なしは <see langword="null"/>。</param>
    /// <param name="placement">表示 API の引数で渡された置き場所。</param>
    /// <returns>委譲面へ渡す表示の要求。</returns>
    public static ToastPresentationRequest ResolveInline(
        object viewModel,
        ToastViewFactory factory,
        int? durationMilliseconds,
        DialogPlacement? placement)
    {
        RequireReferenceType(viewModel);
        return Compose(viewModel, factory, durationMilliseconds, placement);
    }

    /// <summary>デフォルト View の表示の要求を組み立てる。</summary>
    /// <param name="message">表示するメッセージ。</param>
    /// <param name="durationMilliseconds">表示するミリ秒。指定なしは <see langword="null"/>。</param>
    /// <param name="placement">表示 API の引数で渡された置き場所。</param>
    /// <returns>委譲面へ渡す表示の要求。</returns>
    public static ToastPresentationRequest Builtin(
        string message,
        int? durationMilliseconds,
        DialogPlacement? placement) =>
        new(null, message, durationMilliseconds, placement);

    /// <summary>中身の生成を要求へ束ねる。</summary>
    private static ToastPresentationRequest Compose(
        object viewModel,
        ToastViewFactory factory,
        int? durationMilliseconds,
        DialogPlacement? placement) =>
        new(() => factory(viewModel), null, durationMilliseconds, placement);

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
