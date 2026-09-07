using System.Threading;

namespace KsDialogs;

/// <summary>
/// Loading の設定プロパティ (スタイルと既定ローディングの器メタ属性) を全入口で共有する置き場。
/// </summary>
/// <remarks>
/// 表示に効く値の正は Native 側の coordinator が持ち、ここが持つのは「設定した値をそのまま読み返す」
/// ための写しだけである (合流状態はここに置かない — core/ADR-0024)。
/// 設定は入口をまたいで 1 つなので、既定 singleton エントリと DI 注入したインスタンスは同じ値を見る。
/// <para>
/// 設定のたびに委譲面へ渡し、Native の器は各表示の開始時にそれを読む。
/// したがって表示中の設定変更は現在の表示に影響しない (core/ADR-0023)。
/// </para>
/// <para>
/// 保持値の差し替えと委譲面への受け渡しは別の区間で行うため、複数のスレッドが同時に設定すると
/// Native へ届く順が保持値の順と一致しないことがある。設定は起動時にまとめて行うものとして扱う。
/// </para>
/// </remarks>
internal sealed class LoadingSettings
{
    /// <summary>全入口が共有する設定。</summary>
    public static LoadingSettings Shared { get; } = new();

    private readonly Lock _gate = new();
    private LoadingStyle _style = new();
    private DialogOptions _options = new();

    /// <summary>既定ローディングの見た目。</summary>
    public LoadingStyle Style
    {
        get
        {
            lock (_gate)
            {
                return _style;
            }
        }
    }

    /// <summary>既定ローディングの器メタ属性。</summary>
    public DialogOptions Options
    {
        get
        {
            lock (_gate)
            {
                return _options;
            }
        }
    }

    /// <summary>見た目を設定し、委譲面へ渡す。</summary>
    /// <param name="style">設定するスタイル。</param>
    /// <param name="gateway">値を渡す委譲面。</param>
    public void SetStyle(LoadingStyle style, ILoadingGateway gateway)
    {
        lock (_gate)
        {
            _style = style;
        }

        gateway.ApplyStyle(style);
    }

    /// <summary>器メタ属性を設定し、委譲面へ渡す。</summary>
    /// <param name="options">設定する静的メタ属性。</param>
    /// <param name="gateway">値を渡す委譲面。</param>
    public void SetOptions(DialogOptions options, ILoadingGateway gateway)
    {
        lock (_gate)
        {
            _options = options;
        }

        gateway.ApplyOptions(options);
    }
}
