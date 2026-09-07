namespace KsDialogs.Sample.Maui;

/// <summary>
/// Inline Dialog の ViewModel。
/// </summary>
/// <remarks>
/// 結果型を書かずに済む顔で宣言しているため、結果は真偽値になる (core/ADR-0012)。
/// レジストリには登録せず表示のたびに中身を直接渡すため、この型が登録キーになることはない。
/// </remarks>
/// <param name="message">ダイアログに表示するメッセージ。</param>
public sealed class InlineDialogViewModel(string message) : IDialogViewModel
{
    /// <summary>ダイアログに表示するメッセージ。</summary>
    public string Message { get; } = message;
}
