using System;
using System.Threading.Tasks;
using Microsoft.Maui.Controls;

namespace KsDialogs;

/// <summary>
/// ローディング表示の既定エントリ。
/// </summary>
/// <remarks>
/// <see cref="Instance"/> で手軽に呼び出せるほか、<c>new Loading()</c> を <see cref="IKsLoading"/> として
/// DI 注入しても同じレジストリ (<see cref="LoadingViewRegistry.Shared"/>) と同じ設定を共有する
/// (core/ADR-0002)。
/// <para>
/// この型は合流状態を持たず、すべての呼び出しを委譲面越しに Native ライブラリの coordinator へ渡す
/// (core/ADR-0024)。そのため既定 singleton と注入したインスタンスを混ぜて使っても表示は 1 つに合流する。
/// </para>
/// </remarks>
public sealed class Loading : IKsLoading
{
    // 既定エントリの生成は platform の委譲面の用意を伴うため、最初に使われるまで遅らせる
    private static readonly Lazy<Loading> s_instance = new(() => new Loading());

    private readonly LoadingSettings _settings;
    private readonly ILoadingGateway _gateway;

    /// <summary>既定のレジストリと platform の委譲面を使うインスタンスを作る。</summary>
    public Loading() : this(LoadingViewRegistry.Shared, LoadingSettings.Shared, LoadingGatewayFactory.Create())
    {
    }

    internal Loading(ILoadingGateway gateway)
        : this(LoadingViewRegistry.Shared, LoadingSettings.Shared, gateway)
    {
    }

    internal Loading(LoadingViewRegistry registry, LoadingSettings settings, ILoadingGateway gateway)
    {
        Registry = registry;
        _settings = settings;
        _gateway = gateway;
    }

    /// <summary>既定の singleton エントリ。</summary>
    public static Loading Instance => s_instance.Value;

    /// <inheritdoc/>
    public LoadingViewRegistry Registry { get; }

    /// <inheritdoc/>
    public LoadingStyle Style
    {
        get => _settings.Style;
        set
        {
            ArgumentNullException.ThrowIfNull(value);

            _settings.SetStyle(value, _gateway);
        }
    }

    /// <inheritdoc/>
    public DialogOptions Options
    {
        get => _settings.Options;
        set
        {
            ArgumentNullException.ThrowIfNull(value);

            _settings.SetOptions(value, _gateway);
        }
    }

    /// <inheritdoc/>
    public Task ShowAsync(string? message = null, DialogPlacement? placement = null) =>
        _gateway.ShowAsync(LoadingPresenter.Builtin(message, placement));

    /// <inheritdoc/>
    public Task ShowAsync(ILoadingViewModel viewModel, DialogPlacement? placement = null)
    {
        ArgumentNullException.ThrowIfNull(viewModel);

        return _gateway.ShowAsync(LoadingPresenter.Resolve(viewModel, Registry, placement));
    }

    /// <inheritdoc/>
    public Task ShowAsync<TViewModel>(
        TViewModel viewModel,
        Func<TViewModel, View> factory,
        DialogPlacement? placement = null)
        where TViewModel : class, ILoadingViewModel
    {
        ArgumentNullException.ThrowIfNull(viewModel);
        ArgumentNullException.ThrowIfNull(factory);

        return _gateway.ShowAsync(
            LoadingPresenter.ResolveInline(viewModel, LoadingViewFactories.Erase(factory), placement));
    }

    /// <inheritdoc/>
    public Task ShowAsync<TViewModel>(
        Action<TViewModel>? configure = null,
        DialogPlacement? placement = null)
        where TViewModel : class, ILoadingViewModel =>
        ShowTypedAsync(ToAsynchronous(configure), placement);

    /// <inheritdoc/>
    public Task ShowAsync<TViewModel>(
        Func<TViewModel, Task> configure,
        DialogPlacement? placement = null)
        where TViewModel : class, ILoadingViewModel
    {
        ArgumentNullException.ThrowIfNull(configure);

        return ShowTypedAsync(configure, placement);
    }

    /// <inheritdoc/>
    public Task HideAsync() => _gateway.HideAsync();

    /// <inheritdoc/>
    public void SetMessage(string? message) => _gateway.SetMessage(message);

    /// <inheritdoc/>
    public Task StartAsync(
        Func<IProgress<double>, Task> action,
        string? message = null,
        DialogPlacement? placement = null)
    {
        ArgumentNullException.ThrowIfNull(action);

        return _gateway.RunAsync(LoadingPresenter.Builtin(message, placement), action);
    }

    /// <inheritdoc/>
    public Task<T> StartAsync<T>(
        Func<IProgress<double>, Task<T>> action,
        string? message = null,
        DialogPlacement? placement = null)
    {
        ArgumentNullException.ThrowIfNull(action);

        return RunWithValueAsync(LoadingPresenter.Builtin(message, placement), action);
    }

    /// <inheritdoc/>
    public Task StartAsync(
        ILoadingViewModel viewModel,
        Func<IProgress<double>, Task> action,
        DialogPlacement? placement = null)
    {
        ArgumentNullException.ThrowIfNull(viewModel);
        ArgumentNullException.ThrowIfNull(action);

        return _gateway.RunAsync(LoadingPresenter.Resolve(viewModel, Registry, placement), action);
    }

    /// <inheritdoc/>
    public Task<T> StartAsync<T>(
        ILoadingViewModel viewModel,
        Func<IProgress<double>, Task<T>> action,
        DialogPlacement? placement = null)
    {
        ArgumentNullException.ThrowIfNull(viewModel);
        ArgumentNullException.ThrowIfNull(action);

        return RunWithValueAsync(LoadingPresenter.Resolve(viewModel, Registry, placement), action);
    }

    /// <inheritdoc/>
    public Task StartAsync<TViewModel>(
        TViewModel viewModel,
        Func<TViewModel, View> factory,
        Func<IProgress<double>, Task> action,
        DialogPlacement? placement = null)
        where TViewModel : class, ILoadingViewModel
    {
        ArgumentNullException.ThrowIfNull(viewModel);
        ArgumentNullException.ThrowIfNull(factory);
        ArgumentNullException.ThrowIfNull(action);

        return _gateway.RunAsync(
            LoadingPresenter.ResolveInline(viewModel, LoadingViewFactories.Erase(factory), placement),
            action);
    }

    /// <inheritdoc/>
    public Task<T> StartAsync<TViewModel, T>(
        TViewModel viewModel,
        Func<TViewModel, View> factory,
        Func<IProgress<double>, Task<T>> action,
        DialogPlacement? placement = null)
        where TViewModel : class, ILoadingViewModel
    {
        ArgumentNullException.ThrowIfNull(viewModel);
        ArgumentNullException.ThrowIfNull(factory);
        ArgumentNullException.ThrowIfNull(action);

        return RunWithValueAsync(
            LoadingPresenter.ResolveInline(viewModel, LoadingViewFactories.Erase(factory), placement),
            action);
    }

    /// <inheritdoc/>
    public Task StartAsync<TViewModel>(
        Func<IProgress<double>, Task> action,
        Action<TViewModel>? configure = null,
        DialogPlacement? placement = null)
        where TViewModel : class, ILoadingViewModel
    {
        ArgumentNullException.ThrowIfNull(action);

        return StartTypedAsync(action, ToAsynchronous(configure), placement);
    }

    /// <inheritdoc/>
    public Task StartAsync<TViewModel>(
        Func<IProgress<double>, Task> action,
        Func<TViewModel, Task> configure,
        DialogPlacement? placement = null)
        where TViewModel : class, ILoadingViewModel
    {
        ArgumentNullException.ThrowIfNull(action);
        ArgumentNullException.ThrowIfNull(configure);

        return StartTypedAsync(action, configure, placement);
    }

    /// <inheritdoc/>
    public Task<T> StartAsync<TViewModel, T>(
        Func<IProgress<double>, Task<T>> action,
        Action<TViewModel>? configure = null,
        DialogPlacement? placement = null)
        where TViewModel : class, ILoadingViewModel
    {
        ArgumentNullException.ThrowIfNull(action);

        return StartTypedAsync<TViewModel, T>(action, ToAsynchronous(configure), placement);
    }

    /// <inheritdoc/>
    public Task<T> StartAsync<TViewModel, T>(
        Func<IProgress<double>, Task<T>> action,
        Func<TViewModel, Task> configure,
        DialogPlacement? placement = null)
        where TViewModel : class, ILoadingViewModel
    {
        ArgumentNullException.ThrowIfNull(action);
        ArgumentNullException.ThrowIfNull(configure);

        return StartTypedAsync<TViewModel, T>(action, configure, placement);
    }

    /// <summary>型指定 show の本体。解決 → 生成 → configure → 提示の順序をここで固定する。</summary>
    /// <typeparam name="TViewModel">表示する ViewModel の型。</typeparam>
    /// <param name="configure">生成した ViewModel の状態を整える処理。省略時は <see langword="null"/>。</param>
    /// <param name="placement">show の引数で渡された置き場所。</param>
    /// <returns>開始の完了。</returns>
    private async Task ShowTypedAsync<TViewModel>(
        Func<TViewModel, Task>? configure,
        DialogPlacement? placement)
        where TViewModel : class, ILoadingViewModel
    {
        LoadingPresentationRequest request =
            await ResolveTypedAsync(configure, placement).ConfigureAwait(false);

        await _gateway.ShowAsync(request).ConfigureAwait(false);
    }

    /// <summary>型指定 start の本体。生成と configure を終えてから処理の実行へ進む。</summary>
    /// <typeparam name="TViewModel">表示する ViewModel の型。</typeparam>
    /// <param name="action">実行する処理。</param>
    /// <param name="configure">生成した ViewModel の状態を整える処理。省略時は <see langword="null"/>。</param>
    /// <param name="placement">start の引数で渡された置き場所。</param>
    /// <returns>処理と (最後の 1 件なら) 撤去の完了。</returns>
    private async Task StartTypedAsync<TViewModel>(
        Func<IProgress<double>, Task> action,
        Func<TViewModel, Task>? configure,
        DialogPlacement? placement)
        where TViewModel : class, ILoadingViewModel
    {
        LoadingPresentationRequest request =
            await ResolveTypedAsync(configure, placement).ConfigureAwait(false);

        await _gateway.RunAsync(request, action).ConfigureAwait(false);
    }

    /// <summary>値を返す型指定 start の本体。</summary>
    /// <typeparam name="TViewModel">表示する ViewModel の型。</typeparam>
    /// <typeparam name="T">処理が返す値の型。</typeparam>
    /// <param name="action">実行する処理。</param>
    /// <param name="configure">生成した ViewModel の状態を整える処理。省略時は <see langword="null"/>。</param>
    /// <param name="placement">start の引数で渡された置き場所。</param>
    /// <returns>処理の戻り値。</returns>
    private async Task<T> StartTypedAsync<TViewModel, T>(
        Func<IProgress<double>, Task<T>> action,
        Func<TViewModel, Task>? configure,
        DialogPlacement? placement)
        where TViewModel : class, ILoadingViewModel
    {
        LoadingPresentationRequest request =
            await ResolveTypedAsync(configure, placement).ConfigureAwait(false);

        return await RunWithValueAsync(request, action).ConfigureAwait(false);
    }

    /// <summary>
    /// 型指定経路の解決と生成をまとめ、インスタンス渡しと同じ形の要求まで作る。
    /// </summary>
    /// <remarks>
    /// 解決は呼び出し時点のエントリのスナップショットで行い、以後の再登録には影響されない。
    /// 生成と configure は中身の生成と同じ UI スレッドで行い、configure の完了までは提示へ進まない
    /// (core/ADR-0035)。ここでの失敗は提示にも処理にも進まず呼び出し元へ伝播する。
    /// </remarks>
    /// <typeparam name="TViewModel">表示する ViewModel の型。</typeparam>
    /// <param name="configure">生成した ViewModel の状態を整える処理。省略時は <see langword="null"/>。</param>
    /// <param name="placement">表示 API の引数で渡された置き場所。</param>
    /// <returns>委譲面へ渡す表示の要求。</returns>
    private async Task<LoadingPresentationRequest> ResolveTypedAsync<TViewModel>(
        Func<TViewModel, Task>? configure,
        DialogPlacement? placement)
        where TViewModel : class, ILoadingViewModel
    {
        (LoadingViewModelFactory viewModelFactory, LoadingViewFactory viewFactory) =
            LoadingPresenter.ResolveTyped(Registry, typeof(TViewModel));

        TViewModel viewModel = await DialogPresenter.OnUiThreadAsync(async () =>
        {
            // ViewModel factory は型キーと対で登録されるため、生成物の型は常に一致する
            TViewModel created = (TViewModel)viewModelFactory();
            if (configure is not null)
            {
                await configure(created).ConfigureAwait(false);
            }

            return created;
        }).ConfigureAwait(false);

        return LoadingPresenter.ComposeTyped(viewModel, viewFactory, placement);
    }

    /// <summary>同期の configure を、非同期の configure と同じ形にそろえる。</summary>
    /// <typeparam name="TViewModel">configure が受け取る ViewModel の型。</typeparam>
    /// <param name="configure">同期の configure。指定なしは <see langword="null"/>。</param>
    /// <returns>非同期の形にした configure。指定なしなら <see langword="null"/>。</returns>
    private static Func<TViewModel, Task>? ToAsynchronous<TViewModel>(Action<TViewModel>? configure) =>
        configure is null
            ? null
            : viewModel =>
            {
                configure(viewModel);
                return Task.CompletedTask;
            };

    /// <summary>値を返すスコープ形の本体。処理の戻り値を取り出してから撤去の完了を待つ。</summary>
    /// <remarks>
    /// 委譲面が扱うのは値を返さない処理なので、戻り値はここで受け取る。
    /// 失敗はそのまま委譲面を通って伝播するため、握り潰しは起きない。
    /// </remarks>
    /// <typeparam name="T">処理が返す値の型。</typeparam>
    /// <param name="request">その表示の中身と供給値。</param>
    /// <param name="action">実行する処理。</param>
    /// <returns>処理の戻り値。</returns>
    private async Task<T> RunWithValueAsync<T>(
        LoadingPresentationRequest request,
        Func<IProgress<double>, Task<T>> action)
    {
        T value = default!;
        await _gateway.RunAsync(request, async progress => value = await action(progress).ConfigureAwait(false))
            .ConfigureAwait(false);
        return value;
    }
}
