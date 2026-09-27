using Microsoft.Maui.Controls;

namespace KsDialogs.PlacementHost;

/// <summary>テストホストのアプリケーション。起動すると位置と大きさの全シナリオを 1 回実行する。</summary>
public sealed class PlacementHostApp : Application
{
    private bool _started;

    /// <inheritdoc/>
    protected override Window CreateWindow(IActivationState? activationState)
    {
        Window window = new(new ContentPage { Content = new Label { Text = "Running placement scenarios...", Margin = 16 } });
        window.Activated += async (_, _) =>
        {
            if (_started)
            {
                return;
            }

            _started = true;
            // 起動直後の最初の画面が描画されるのを待ってから始める
            await Task.Delay(1000);
            await new PlacementRunner().RunAsync(window);
        };
        return window;
    }
}
