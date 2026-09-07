using Microsoft.Maui;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Graphics;

namespace KsDialogs.Maui.Tests.Support;

/// <summary>メタ属性の添付を検証するための中身の View。</summary>
internal static class LayoutTestContentViews
{
    /// <summary>背後を覆う色。</summary>
    public static readonly Color Overlay = Color.FromRgba(0x55, 0x66, 0x77, 0x88);

    /// <summary>メタ属性を一切添付しない中身。</summary>
    /// <returns>添付のない View。</returns>
    public static View WithoutAttributes() => new Label();

    /// <summary>
    /// 全項目を添付した中身。
    /// </summary>
    /// <remarks>
    /// 値はすべて既定値と異なるものを選び、既定値のまま運ばれた場合に検証が失敗するようにしている。
    /// 比率に有効域 (0〜1) を外れた値を含めるのは、丸めが MAUI 側で起きないことを見るため。
    /// </remarks>
    /// <returns>全項目を添付した View。</returns>
    public static View WithEveryAttribute()
    {
        Label contentView = new();
        Dialog.SetLayoutArea(contentView, DialogLayoutArea.Window);
        Dialog.SetDialogMargin(contentView, new Thickness(1d, 2d, 3d, 4d));
        Dialog.SetProportionalWidth(contentView, 1.5d);
        Dialog.SetProportionalHeight(contentView, 0.25d);
        Dialog.SetOverlayColor(contentView, Overlay);
        Dialog.SetIsCanceledOnTouchOutside(contentView, false);
        Dialog.SetHorizontalAlignment(contentView, DialogAlignment.End);
        Dialog.SetVerticalAlignment(contentView, DialogAlignment.Fill);
        Dialog.SetOffsetX(contentView, -12.5d);
        Dialog.SetOffsetY(contentView, 34d);
        return contentView;
    }

    /// <summary>
    /// 有効域を外れた値と NaN を添付した中身。
    /// </summary>
    /// <remarks>これらの値を有効な値へ丸めるのは Native 実装の責務で、MAUI 側は添付のまま運ぶ。</remarks>
    /// <returns>無効値を添付した View。</returns>
    public static View WithInvalidValues()
    {
        Label contentView = new();
        Dialog.SetProportionalWidth(contentView, 2d);
        Dialog.SetProportionalHeight(contentView, -5d);
        Dialog.SetDialogMargin(contentView, new Thickness(-30d, double.NaN, 0d, 0d));
        Dialog.SetOffsetX(contentView, double.NaN);
        return contentView;
    }

    /// <summary>置き場所だけを添付した中身。</summary>
    /// <param name="placement">添付する置き場所。</param>
    /// <returns>置き場所を添付した View。</returns>
    public static View WithPlacement(DialogPlacement placement)
    {
        Label contentView = new();
        Dialog.SetHorizontalAlignment(contentView, placement.HorizontalAlignment);
        Dialog.SetVerticalAlignment(contentView, placement.VerticalAlignment);
        Dialog.SetOffsetX(contentView, placement.OffsetX);
        Dialog.SetOffsetY(contentView, placement.OffsetY);
        return contentView;
    }
}
