using CoreGraphics;
using Microsoft.Maui.Controls;
using UIKit;

namespace KsDialogs.PlacementHost;

/// <summary>iOS での矩形の測り方。すべて window 座標 (pt) で測る。</summary>
internal static class PlacementMeasurement
{
    /// <summary>MAUI の単位 1 あたりの pt (同じ単位なので 1)。</summary>
    public const double Scale = 1d;

    /// <summary>ダイアログの既定の余白 (pt)。</summary>
    public const double Margin = 24d;

    /// <summary>位置と大きさの比較に許す誤差 (pt)。</summary>
    public const double Tolerance = 1d;

    /// <summary>要素を基準にしたときに期待する領域 (safe area の内側 ∩ 可視領域)。</summary>
    /// <param name="element">基準にする要素。</param>
    /// <returns>期待する領域。まだ描画されていなければ <see langword="null"/>。</returns>
    public static PlacementRect? ExpectedArea(VisualElement element)
    {
        if (element.Handler?.PlatformView is not UIView view || view.Window is not UIWindow window)
        {
            return null;
        }

        CGRect safe = view.ConvertRectToView(view.SafeAreaLayoutGuide.LayoutFrame, window);
        return ToRect(safe).Intersect(VisibleArea(window));
    }

    /// <summary>ダイアログの中身の矩形。</summary>
    /// <param name="content">ダイアログの中身。</param>
    /// <returns>中身の矩形。まだ画面に載っていなければ <see langword="null"/>。</returns>
    public static PlacementRect? ContentRect(VisualElement content)
    {
        if (content.Handler?.PlatformView is not UIView view || view.Window is not UIWindow window)
        {
            return null;
        }

        return ToRect(view.ConvertRectToView(view.Bounds, window));
    }

    /// <summary>ダイアログの外形 (中身のルートの platform view を直接持つ View) の矩形。</summary>
    /// <param name="content">ダイアログの中身のルート。</param>
    /// <returns>外形の矩形。まだ画面に載っていなければ <see langword="null"/>。</returns>
    public static PlacementRect? OuterRect(VisualElement content)
    {
        if (content.Handler?.PlatformView is not UIView { Superview: UIView parent } || parent.Window is not UIWindow window)
        {
            return null;
        }

        return ToRect(parent.ConvertRectToView(parent.Bounds, window));
    }

    /// <summary>要素が載っている window の可視領域。</summary>
    /// <param name="element">window に載っている要素。</param>
    /// <returns>可視領域。まだ描画されていなければ <see langword="null"/>。</returns>
    public static PlacementRect? VisibleArea(VisualElement element) =>
        element.Handler?.PlatformView is UIView { Window: UIWindow window } ? VisibleArea(window) : null;

    private static PlacementRect VisibleArea(UIWindow window)
    {
        UIEdgeInsets insets = window.SafeAreaInsets;
        CGRect bounds = window.Bounds;
        return new PlacementRect(
            bounds.Left + insets.Left,
            bounds.Top + insets.Top,
            bounds.Right - insets.Right,
            bounds.Bottom - insets.Bottom);
    }

    private static PlacementRect ToRect(CGRect rect) => new(rect.Left, rect.Top, rect.Right, rect.Bottom);
}
