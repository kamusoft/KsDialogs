namespace KsDialogs.Sample.Maui;

/// <summary>Layout Dialog の中身。</summary>
public partial class LayoutDialogCardView : ContentView
{
    private readonly Action _onCancel;
    private readonly Action _onComplete;

    /// <summary>カードを組み立てる。</summary>
    /// <param name="message">表示するメッセージ。</param>
    /// <param name="onCancel">キャンセル操作。</param>
    /// <param name="onComplete">完了操作。</param>
    public LayoutDialogCardView(string message, Action onCancel, Action onComplete)
    {
        InitializeComponent();
        _onCancel = onCancel;
        _onComplete = onComplete;
        MessageLabel.Text = message;
    }

    private void OnCancelClicked(object? sender, EventArgs e) => _onCancel();

    private void OnCompleteClicked(object? sender, EventArgs e) => _onComplete();
}
