using System.Threading;

namespace KsDialogs;

/// <summary>
/// Toast の一括設定を全入口で共有する置き場。
/// </summary>
/// <remarks>
/// 表示に効く値の正は Native 側の coordinator が持ち、ここが持つのは「設定した値をそのまま読み返す」
/// ための写しだけである。設定は入口をまたいで 1 つなので、既定 singleton エントリと DI 注入した
/// インスタンスは同じ値を見る。
/// <para>
/// 設定のたびに委譲面へ渡し、Native の器は各表示の受理時にそれを読む。
/// したがって表示中の設定変更は既に出ている Toast に影響しない (core/ADR-0032)。
/// </para>
/// <para>
/// 保持値の差し替えと委譲面への受け渡しは別の区間で行うため、複数のスレッドが同時に設定すると
/// Native へ届く順が保持値の順と一致しないことがある。設定は起動時にまとめて行うものとして扱う。
/// </para>
/// </remarks>
internal sealed class ToastSettings
{
    /// <summary>全入口が共有する設定。</summary>
    public static ToastSettings Shared { get; } = new();

    private readonly Lock _gate = new();
    private ToastStyle _style = new();

    /// <summary>Toast の一括設定。</summary>
    public ToastStyle Style
    {
        get
        {
            lock (_gate)
            {
                return _style;
            }
        }
    }

    /// <summary>一括設定を設定し、委譲面へ渡す。</summary>
    /// <param name="style">設定するスタイル。</param>
    /// <param name="gateway">値を渡す委譲面。</param>
    public void SetStyle(ToastStyle style, IToastGateway gateway)
    {
        lock (_gate)
        {
            _style = style;
        }

        gateway.ApplyStyle(style);
    }
}
