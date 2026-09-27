namespace KsDialogs.Sample.Maui;

/// <summary>
/// Layout Dialog の ViewModel。
/// </summary>
/// <remarks>
/// 結果型は <see cref="bool"/> で、完了操作は <see langword="true"/> を報告する。
/// この型そのものがレジストリの登録キーになる。
/// </remarks>
/// <param name="message">ダイアログに表示するメッセージ。</param>
/// <param name="layoutArea">
/// サイズと位置の計算に使う基準領域。静的メタ属性は中身の性質なので、
/// View factory が作った View への添付として供給する。
/// </param>
/// <param name="dialogMargin">全辺そろえで添付する余白。基準領域と同じく View への添付で供給する。</param>
public sealed class LayoutDialogViewModel(string message, DialogLayoutArea layoutArea, double dialogMargin)
    : IDialogViewModel<bool>
{
    /// <summary>ダイアログに表示するメッセージ。</summary>
    public string Message { get; } = message;

    /// <summary>サイズと位置の計算に使う基準領域。</summary>
    public DialogLayoutArea LayoutArea { get; } = layoutArea;

    /// <summary>全辺そろえで添付する余白。</summary>
    public double DialogMargin { get; } = dialogMargin;
}
