namespace KsDialogs.Sample.Maui;

/// <summary>インライン経路のカスタム Toast の中身。</summary>
public partial class InlineToastCardView : ContentView
{
    /// <summary>中身を組み立てる。</summary>
    /// <param name="message">表示するメッセージ。</param>
    public InlineToastCardView(string message)
    {
        InitializeComponent();
        MessageLabel.Text = message;
    }
}
