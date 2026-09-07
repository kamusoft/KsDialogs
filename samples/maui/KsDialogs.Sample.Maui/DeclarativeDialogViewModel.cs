namespace KsDialogs.Sample.Maui;

/// <summary>
/// Declarative Dialog の ViewModel。
/// </summary>
/// <remarks>
/// 結果型を書かずに済む顔で宣言しているため、結果は真偽値になる (core/ADR-0012)。
/// この型そのものがレジストリの登録キーになる。
/// </remarks>
/// <param name="message">ダイアログに表示するメッセージ。</param>
public sealed class DeclarativeDialogViewModel(string message) : IDialogViewModel
{
    /// <summary>ダイアログに表示するメッセージ。</summary>
    public string Message { get; } = message;
}
