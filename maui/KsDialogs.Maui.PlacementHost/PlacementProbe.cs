using Microsoft.Maui.Controls;
using Microsoft.Maui.Graphics;

namespace KsDialogs.PlacementHost;

/// <summary>位置を測るために出すダイアログの ViewModel。中身の View を 1 つ持つ。</summary>
internal sealed class PlacementProbeViewModel : IDialogViewModel<bool>
{
    private DialogNotifier<bool>? _notifier;

    /// <param name="area">中身に添付する基準領域。</param>
    /// <param name="alignment">両軸の配置。</param>
    public PlacementProbeViewModel(DialogLayoutArea area, DialogAlignment alignment)
    {
        Content = new ContentView
        {
            WidthRequest = 120,
            HeightRequest = 80,
            BackgroundColor = Colors.Orange,
            // 基準領域によらず同じ中身にする。中身の大きさが違うと、位置の比較に大きさの差が混ざる
            Content = new Label { Text = "Probe", HorizontalOptions = LayoutOptions.Center },
        };
        // 端に寄せると、基準領域のその側の辺がそのまま位置に現れる
        Dialog.SetLayoutArea(Content, area);
        Dialog.SetHorizontalAlignment(Content, alignment);
        Dialog.SetVerticalAlignment(Content, alignment);
    }

    /// <summary>ダイアログの中身。</summary>
    public ContentView Content { get; }

    /// <summary>ダイアログの登録を行う。アプリの起動につき 1 回呼ぶ。</summary>
    public static void Register() =>
        DialogViewRegistry.Shared.Register((PlacementProbeViewModel viewModel, DialogNotifier<bool> notifier) =>
        {
            viewModel._notifier = notifier;
            return viewModel.Content;
        });

    /// <summary>ダイアログを閉じる。</summary>
    public void Close() => _notifier?.Complete(true);
}
