namespace KsDialogs.Sample.Maui;

/// <summary>
/// Layout Dialog の ViewModel。
/// </summary>
/// <remarks>
/// 結果型は <see cref="bool"/> で、完了操作は <see langword="true"/> を報告する。
/// この型そのものがレジストリの登録キーになる。
/// </remarks>
/// <param name="message">ダイアログに表示するメッセージ。</param>
/// <param name="usesVisibleArea">
/// サイズと位置の計算に可視領域を使うか。静的メタ属性は中身の性質なので、
/// View factory が作った View への添付として供給する。
/// </param>
public sealed class LayoutDialogViewModel(string message, bool usesVisibleArea) : IDialogViewModel<bool>
{
    /// <summary>ダイアログに表示するメッセージ。</summary>
    public string Message { get; } = message;

    /// <summary>サイズと位置の計算に可視領域を使うか。</summary>
    public bool UsesVisibleArea { get; } = usesVisibleArea;
}
