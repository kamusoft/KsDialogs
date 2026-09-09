using Microsoft.Maui.Controls;

namespace MyApp;

/// <summary>消費者検証アプリのアプリケーション。</summary>
/// <remarks>
/// 画面は最小例を呼ぶボタン 1 つだけで、検証はビルドが成立することを見る。
/// </remarks>
public class App : Application
{
    /// <inheritdoc/>
    protected override Window CreateWindow(IActivationState? activationState)
        => new(new ContentPage
        {
            Content = new Button
            {
                Text = "Show toast",
                Command = new Command(Notifications.ShowSaved),
            },
        });
}
