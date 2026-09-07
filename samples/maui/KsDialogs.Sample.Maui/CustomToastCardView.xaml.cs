namespace KsDialogs.Sample.Maui;

/// <summary>登録経路のカスタム Toast の中身。</summary>
/// <remarks>
/// コンストラクタが受け取るのは表示対象の ViewModel だけで、Toast 版の 1 行登録
/// (<c>RegisterForToast</c>) がその値をそのまま渡す。
/// </remarks>
public partial class CustomToastCardView : ContentView
{
    /// <summary>中身を組み立てる。</summary>
    /// <param name="viewModel">メッセージを運ぶ ViewModel。</param>
    public CustomToastCardView(CustomToastViewModel viewModel)
    {
        InitializeComponent();
        ArgumentNullException.ThrowIfNull(viewModel);
        MessageLabel.Text = viewModel.Message;
    }
}
