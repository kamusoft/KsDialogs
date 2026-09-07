namespace KsDialogs.Sample.Maui;

/// <summary>
/// Model Dialog の ViewModel。
/// </summary>
/// <remarks>
/// 結果型は <see cref="bool"/> で、完了操作は <see langword="true"/> を報告する。
/// 表示するメッセージは型指定 show の configure から設定される。
/// 結果は中身から渡される報告口ではなく、この ViewModel 自身が <c>Notifier</c> から報告する
/// (core/ADR-0018)。
/// </remarks>
public sealed class ModelDialogViewModel : IDialogViewModel
{
    /// <summary>ダイアログに表示するメッセージ。表示の直前に configure から設定する。</summary>
    public string Message { get; set; } = string.Empty;

    /// <summary>完了を報告する。</summary>
    public void Complete() => this.Notifier?.Complete(true);

    /// <summary>キャンセルを報告する。</summary>
    public void Cancel() => this.Notifier?.Cancel();
}
