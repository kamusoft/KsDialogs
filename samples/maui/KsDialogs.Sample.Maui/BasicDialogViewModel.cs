namespace KsDialogs.Sample.Maui;

/// <summary>
/// Basic Dialog の ViewModel。
/// </summary>
/// <remarks>
/// 結果型は <see cref="bool"/> で、完了操作は <see langword="true"/> を報告する。
/// この型そのものがレジストリの登録キーになる。
/// </remarks>
/// <param name="message">ダイアログに表示するメッセージ。</param>
public sealed class BasicDialogViewModel(string message) : IDialogViewModel<bool>
{
    /// <summary>ダイアログに表示するメッセージ。</summary>
    public string Message { get; } = message;
}
