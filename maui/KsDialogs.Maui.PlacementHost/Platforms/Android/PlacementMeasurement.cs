using AndroidX.Core.View;
using Microsoft.Maui.Controls;
using AView = Android.Views.View;

namespace KsDialogs.PlacementHost;

/// <summary>Android での矩形の測り方。すべて画面座標 (px) で測る。</summary>
/// <remarks>ダイアログは Activity とは別のウィンドウに載るため、画面座標を共通の原点にする。</remarks>
internal static class PlacementMeasurement
{
    /// <summary>ダイアログの既定の余白 (px)。契約の 24 を画面の密度で px にする。</summary>
    public static double Margin => 24d * (Android.App.Application.Context.Resources?.DisplayMetrics?.Density ?? 1f);

    /// <summary>位置の比較に許す誤差 (px)。dp から px への丸めの分を見込む。</summary>
    public const double Tolerance = 2d;

    /// <summary>要素を基準にしたときに期待する領域 (要素の矩形 ∩ 可視領域)。</summary>
    /// <param name="element">基準にする要素。</param>
    /// <returns>期待する領域。まだ描画されていなければ <see langword="null"/>。</returns>
    public static PlacementRect? ExpectedArea(VisualElement element)
    {
        if (element.Handler?.PlatformView is not AView view || !view.IsAttachedToWindow || view.Width == 0)
        {
            return null;
        }

        return ScreenRect(view).Intersect(VisibleArea(view.RootView!));
    }

    /// <summary>ダイアログの中身の矩形。</summary>
    /// <param name="content">ダイアログの中身。</param>
    /// <returns>中身の矩形。まだ画面に載っていなければ <see langword="null"/>。</returns>
    public static PlacementRect? ContentRect(VisualElement content) =>
        content.Handler?.PlatformView is AView { IsAttachedToWindow: true } view && view.Width > 0
            ? ScreenRect(view)
            : null;

    /// <summary>要素が載っている画面の可視領域。</summary>
    /// <param name="element">画面に載っている要素。</param>
    /// <returns>可視領域。まだ描画されていなければ <see langword="null"/>。</returns>
    public static PlacementRect? VisibleArea(VisualElement element) =>
        element.Handler?.PlatformView is AView { IsAttachedToWindow: true } view
            ? VisibleArea(view.RootView!)
            : null;

    private static PlacementRect VisibleArea(AView root)
    {
        PlacementRect rect = ScreenRect(root);
        AndroidX.Core.Graphics.Insets? insets = ViewCompat.GetRootWindowInsets(root)
            ?.GetInsets(WindowInsetsCompat.Type.SystemBars());
        return insets is null
            ? rect
            : new PlacementRect(
                rect.Left + insets.Left,
                rect.Top + insets.Top,
                rect.Right - insets.Right,
                rect.Bottom - insets.Bottom);
    }

    private static PlacementRect ScreenRect(AView view)
    {
        int[] location = new int[2];
        view.GetLocationOnScreen(location);
        return new PlacementRect(location[0], location[1], location[0] + view.Width, location[1] + view.Height);
    }
}
