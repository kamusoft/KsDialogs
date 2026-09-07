using System;
using Microsoft.Maui.Controls;

namespace KsDialogs;

/// <summary>
/// Toast 表示の既定エントリ。
/// </summary>
/// <remarks>
/// <see cref="Instance"/> で手軽に呼び出せるほか、<c>new Toast()</c> を <see cref="IKsToast"/> として
/// DI 注入しても同じレジストリ (<see cref="ToastViewRegistry.Shared"/>) と同じ設定を共有する
/// (core/ADR-0002)。
/// <para>
/// この型は表示中のリストも計時も持たず、すべての呼び出しを委譲面越しに Native ライブラリの
/// coordinator へ渡す。そのため既定 singleton と注入したインスタンスを混ぜて使っても、
/// 重なり順と消滅の管理は 1 か所にまとまる。
/// </para>
/// </remarks>
public sealed class Toast : IKsToast
{
    // 既定エントリの生成は platform の委譲面の用意を伴うため、最初に使われるまで遅らせる
    private static readonly Lazy<Toast> s_instance = new(() => new Toast());

    private readonly ToastSettings _settings;
    private readonly IToastGateway _gateway;

    /// <summary>既定のレジストリと platform の委譲面を使うインスタンスを作る。</summary>
    public Toast() : this(ToastViewRegistry.Shared, ToastSettings.Shared, ToastGatewayFactory.Create())
    {
    }

    internal Toast(IToastGateway gateway)
        : this(ToastViewRegistry.Shared, ToastSettings.Shared, gateway)
    {
    }

    internal Toast(ToastViewRegistry registry, ToastSettings settings, IToastGateway gateway)
    {
        Registry = registry;
        _settings = settings;
        _gateway = gateway;
    }

    /// <summary>既定の singleton エントリ。</summary>
    public static Toast Instance => s_instance.Value;

    /// <inheritdoc/>
    public ToastViewRegistry Registry { get; }

    /// <inheritdoc/>
    public ToastStyle Style
    {
        get => _settings.Style;
        set
        {
            ArgumentNullException.ThrowIfNull(value);

            _settings.SetStyle(value, _gateway);
        }
    }

    /// <inheritdoc/>
    public void Show(string message, int? durationMs = null, DialogPlacement? placement = null)
    {
        ArgumentNullException.ThrowIfNull(message);

        _gateway.Show(ToastPresenter.Builtin(message, durationMs, placement));
    }

    /// <inheritdoc/>
    public void Show(IToastViewModel viewModel, int? durationMs = null, DialogPlacement? placement = null)
    {
        ArgumentNullException.ThrowIfNull(viewModel);

        _gateway.Show(ToastPresenter.Resolve(viewModel, Registry, durationMs, placement));
    }

    /// <inheritdoc/>
    public void Show<TViewModel>(
        TViewModel viewModel,
        Func<TViewModel, View> factory,
        int? durationMs = null,
        DialogPlacement? placement = null)
        where TViewModel : class, IToastViewModel
    {
        ArgumentNullException.ThrowIfNull(viewModel);
        ArgumentNullException.ThrowIfNull(factory);

        _gateway.Show(ToastPresenter.ResolveInline(
            viewModel,
            ToastViewFactories.Erase(factory),
            durationMs,
            placement));
    }

    /// <inheritdoc/>
    public void Show<TViewModel>(
        Action<TViewModel>? configure = null,
        int? durationMs = null,
        DialogPlacement? placement = null)
        where TViewModel : class, IToastViewModel =>
        // 解決だけを呼び出し時点で行い、生成と configure は中身の生成と同じ受理後の UI スレッドへ送る
        _gateway.Show(ToastPresenter.ResolveTyped(Registry, configure, durationMs, placement));
}
