using AndroidTabbedPage = Microsoft.Maui.Controls.PlatformConfiguration.AndroidSpecific.TabbedPage;
using ToolbarPlacement = Microsoft.Maui.Controls.PlatformConfiguration.AndroidSpecific.ToolbarPlacement;

namespace KsDialogs.Sample.Maui;

/// <summary>属性調整パネルの画面。下部のタブバーでパネルのタブと説明のタブを切り替える。</summary>
/// <remarks>
/// パネルのタブは <see cref="NavigationPage" /> に載せて OS 標準のナビゲーションバーを持たせ、
/// 説明のタブはタイトルバーを持たない素のページにする。基準領域「表示中のページ」は
/// MAUI 層の既定の探し方がこの構成 (モーダルの先頭 → 選択中のタブ → ナビゲーションの先頭) を辿って決まるため、
/// 画面側での登録は要らない。
/// </remarks>
public sealed class SampleLayoutPanelTabbedPage : TabbedPage
{
    /// <summary>画面を組み立てる。</summary>
    /// <param name="onResult">確定した結果をメニュー画面へ渡す。</param>
    public SampleLayoutPanelTabbedPage(Action<string> onResult)
    {
        SampleLayoutPanelPage panel = new(onResult);

        BarBackgroundColor = ThemeColor("SampleSurface");
        SelectedTabColor = ThemeColor("SamplePrimary");
        UnselectedTabColor = ThemeColor("SampleOnSurfaceMuted");

        // Android の既定はタブバーを上部に置くため、iOS と同じ下部へ移す
        AndroidTabbedPage.SetToolbarPlacement(this, ToolbarPlacement.Bottom);

        Children.Add(new NavigationPage(panel)
        {
            Title = SampleText.PanelTab,
            IconImageSource = "tab_panel.png",
            BarBackgroundColor = ThemeColor("SampleSurface"),
            BarTextColor = ThemeColor("SampleOnSurface"),
        });
        Children.Add(new SampleLayoutInfoPage(panel.ShowLayoutDialog));
    }

    /// <summary>共有の配色から色を引く。</summary>
    /// <param name="key">配色のキー。</param>
    /// <returns>引けた色。引けなければ透明。</returns>
    private static Color ThemeColor(string key) =>
        Application.Current?.Resources.TryGetValue(key, out object? value) == true && value is Color color
            ? color
            : Colors.Transparent;
}
