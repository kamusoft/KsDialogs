namespace KsDialogs.Sample.Maui;

/// <summary>
/// インライン経路のカスタム Toast の ViewModel。
/// </summary>
/// <remarks>中身はこの場で渡すため、この型はレジストリに登録しない (core/ADR-0013)。</remarks>
/// <param name="message">Toast に表示するメッセージ。</param>
public sealed class InlineToastViewModel(string message) : IToastViewModel
{
    /// <summary>Toast に表示するメッセージ。</summary>
    public string Message { get; } = message;
}
