namespace KsDialogs.Sample.Maui;

/// <summary>
/// Text Input Dialog の ViewModel。
/// </summary>
/// <remarks>
/// 結果型は <see cref="string"/> で、完了操作は入力された文字列を報告する。
/// この型そのものがレジストリの登録キーになる。
/// </remarks>
/// <param name="message">ダイアログに表示するメッセージ。</param>
public sealed class TextInputDialogViewModel(string message) : IDialogViewModel<string>
{
    /// <summary>ダイアログに表示するメッセージ。</summary>
    public string Message { get; } = message;
}
