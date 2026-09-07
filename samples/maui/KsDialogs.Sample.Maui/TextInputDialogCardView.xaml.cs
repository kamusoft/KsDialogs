namespace KsDialogs.Sample.Maui;

/// <summary>Text Input Dialog の中身。</summary>
public partial class TextInputDialogCardView : ContentView
{
    private readonly Action _onCancel;
    private readonly Action<string> _onComplete;

    /// <summary>カードを組み立てる。</summary>
    /// <param name="message">表示するメッセージ。</param>
    /// <param name="onCancel">キャンセル操作。</param>
    /// <param name="onComplete">完了操作。入力中の文字列を結果として渡す。</param>
    public TextInputDialogCardView(string message, Action onCancel, Action<string> onComplete)
    {
        InitializeComponent();
        _onCancel = onCancel;
        _onComplete = onComplete;
        MessageLabel.Text = message;
    }

    /// <summary>入力欄そのものに付く platform 既定の枠を消す。</summary>
    /// <param name="sender">対象の入力欄。</param>
    /// <param name="e">未使用。</param>
    /// <remarks>
    /// 枠は外側の <see cref="Border" /> が描くため、iOS の角丸枠も Android の下線も要らない。
    /// 残すと枠が二重に見えて、共有の見た目から外れる。
    /// </remarks>
    private void OnInputEntryHandlerChanged(object? sender, EventArgs e)
    {
#if IOS
        if (sender is Entry { Handler.PlatformView: UIKit.UITextField textField })
        {
            textField.BorderStyle = UIKit.UITextBorderStyle.None;
        }
#elif ANDROID
        if (sender is Entry { Handler.PlatformView: Android.Widget.EditText editText })
        {
            editText.BackgroundTintList =
                Android.Content.Res.ColorStateList.ValueOf(Android.Graphics.Color.Transparent);
        }
#endif
    }

    private void OnCancelClicked(object? sender, EventArgs e) => _onCancel();

    private void OnCompleteClicked(object? sender, EventArgs e) => _onComplete(InputEntry.Text ?? string.Empty);
}
