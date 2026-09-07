using System;
using System.Threading.Tasks;
using Microsoft.Maui.Controls;

namespace KsDialogs;

/// <summary>
/// ダイアログ表示の既定エントリ。
/// </summary>
/// <remarks>
/// <see cref="Instance"/> で手軽に呼び出せるほか、<c>new Dialog()</c> を <see cref="IKsDialog"/> として
/// DI 注入しても同じレジストリ (<see cref="DialogViewRegistry.Shared"/>) を共有する (core/ADR-0002・0004)。
/// <para>
/// この型はダイアログの中身へメタ属性を添付する面も兼ねる (<c>ksd:Dialog.OverlayColor</c> 等)。
/// </para>
/// </remarks>
public sealed partial class Dialog : IKsDialog
{
    // 既定エントリの生成は platform の委譲面の用意を伴うため、最初に使われるまで遅らせる
    private static readonly Lazy<Dialog> s_instance = new(() => new Dialog());

    private readonly IDialogGateway _gateway;

    /// <summary>既定のレジストリと platform の委譲面を使うインスタンスを作る。</summary>
    public Dialog() : this(DialogViewRegistry.Shared, DialogGatewayFactory.Create())
    {
    }

    internal Dialog(IDialogGateway gateway) : this(DialogViewRegistry.Shared, gateway)
    {
    }

    internal Dialog(DialogViewRegistry registry, IDialogGateway gateway)
    {
        Registry = registry;
        _gateway = gateway;
    }

    /// <summary>既定の singleton エントリ。</summary>
    public static Dialog Instance => s_instance.Value;

    /// <inheritdoc/>
    public DialogViewRegistry Registry { get; }

    /// <inheritdoc/>
    public async Task<DialogResult<TResult>> ShowAsync<TResult>(
        IDialogViewModel<TResult> viewModel,
        DialogPlacement? placement = null)
    {
        ArgumentNullException.ThrowIfNull(viewModel);

        DialogOutcome outcome = await DialogPresenter
            .PresentAsync(viewModel, Registry, _gateway, placement)
            .ConfigureAwait(false);

        return ToResult<TResult>(outcome);
    }

    /// <inheritdoc/>
    public async Task<DialogResult<TResult>> ShowAsync<TViewModel, TResult>(
        TViewModel viewModel,
        Func<TViewModel, DialogNotifier<TResult>, View> factory,
        DialogPlacement? placement = null)
        where TViewModel : class, IDialogViewModel<TResult>
    {
        ArgumentNullException.ThrowIfNull(viewModel);
        ArgumentNullException.ThrowIfNull(factory);

        DialogOutcome outcome = await DialogPresenter
            .PresentAsync(viewModel, DialogViewFactories.Erase(factory), _gateway, placement)
            .ConfigureAwait(false);

        return ToResult<TResult>(outcome);
    }

    /// <inheritdoc/>
    public Task<DialogResult<bool>> ShowAsync<TViewModel>(
        TViewModel viewModel,
        Func<TViewModel, DialogNotifier<bool>, View> factory,
        DialogPlacement? placement = null)
        where TViewModel : class, IDialogViewModel =>
        ShowAsync<TViewModel, bool>(viewModel, factory, placement);

    /// <inheritdoc/>
    public Task<DialogResult<TResult>> ShowAsync<TViewModel, TResult>(
        Action<TViewModel>? configure = null,
        DialogPlacement? placement = null)
        where TViewModel : class, IDialogViewModel<TResult> =>
        ShowTypedAsync<TViewModel, TResult>(ToAsynchronous(configure), placement);

    /// <inheritdoc/>
    public Task<DialogResult<TResult>> ShowAsync<TViewModel, TResult>(
        Func<TViewModel, Task> configure,
        DialogPlacement? placement = null)
        where TViewModel : class, IDialogViewModel<TResult>
    {
        ArgumentNullException.ThrowIfNull(configure);

        return ShowTypedAsync<TViewModel, TResult>(configure, placement);
    }

    /// <inheritdoc/>
    public Task<DialogResult<bool>> ShowAsync<TViewModel>(
        Action<TViewModel>? configure = null,
        DialogPlacement? placement = null)
        where TViewModel : class, IDialogViewModel =>
        ShowTypedAsync<TViewModel, bool>(ToAsynchronous(configure), placement);

    /// <inheritdoc/>
    public Task<DialogResult<bool>> ShowAsync<TViewModel>(
        Func<TViewModel, Task> configure,
        DialogPlacement? placement = null)
        where TViewModel : class, IDialogViewModel
    {
        ArgumentNullException.ThrowIfNull(configure);

        return ShowTypedAsync<TViewModel, bool>(configure, placement);
    }

    /// <summary>
    /// 型指定 show の本体。解決 → 生成 → configure → 提示の順序をここで固定する。
    /// </summary>
    /// <remarks>
    /// 解決は呼び出し時点のエントリのスナップショットで行い、以後の再登録には影響されない。
    /// 生成と configure は中身の生成と同じ UI スレッドで行い、configure の完了までは提示へ進まない
    /// (core/ADR-0019)。この時点では報告口の紐付けがまだ無いため、失敗しても後始末は要らない。
    /// </remarks>
    /// <typeparam name="TViewModel">表示する ViewModel の型。</typeparam>
    /// <typeparam name="TResult">ViewModel が宣言する結果値の型。</typeparam>
    /// <param name="configure">生成した ViewModel の状態を整える処理。省略時は <see langword="null"/>。</param>
    /// <param name="placement">show の引数で渡された置き場所。</param>
    /// <returns>completed(結果値) または cancelled。</returns>
    private async Task<DialogResult<TResult>> ShowTypedAsync<TViewModel, TResult>(
        Func<TViewModel, Task>? configure,
        DialogPlacement? placement)
        where TViewModel : class, IDialogViewModel<TResult>
    {
        DialogRegistryEntry? entry = Registry.Entry(typeof(TViewModel));
        DialogViewModelFactory viewModelFactory =
            DialogResolution.ResolveViewModelFactory(Registry, entry, typeof(TViewModel));
        DialogViewFactory viewFactory =
            DialogResolution.ResolveViewFactory(Registry, entry, typeof(TViewModel));

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

        DialogOutcome outcome = await DialogPresenter
            .PresentAsync(viewModel, viewFactory, _gateway, placement)
            .ConfigureAwait(false);

        return ToResult<TResult>(outcome);
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

    /// <summary>型を消した結果を、ViewModel の宣言結果型で受け取れる形へ戻す。</summary>
    private static DialogResult<TResult> ToResult<TResult>(DialogOutcome outcome) => outcome switch
    {
        // 結果値は ViewModel の宣言結果型に固定された報告口からしか入らないため、ここでの型は常に一致する
        DialogOutcome.Completed completed => new DialogResult<TResult>.Completed((TResult)completed.Value!),
        _ => new DialogResult<TResult>.Cancelled(),
    };
}
