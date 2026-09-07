using Microsoft.Maui;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Graphics;

namespace KsDialogs;

/// <summary>
/// ダイアログの中身 (View) にメタ属性を添付する面 (core/ADR-0015)。
/// </summary>
/// <remarks>
/// XAML では中身の View に <c>ksd:Dialog.OverlayColor="#80000000"</c> のように書き、
/// コードからは <c>Dialog.SetOverlayColor(view, color)</c> で設定する。
/// 添付した値は、その View をダイアログとして表示する器が実効値として採用する。
/// 何も添付しなければ全項目が契約の既定値になる。
/// <para>
/// 置き場所 (<see cref="DialogPlacement"/>) の4項目だけは show の引数でも渡せ、渡した場合は
/// 添付された4項目をまるごと置換する。背後の覆いや外側タップの扱いといった残りの項目は
/// 中身の性質なので、添付でしか渡せない。
/// </para>
/// </remarks>
public sealed partial class Dialog
{
    // 添付されていない項目の既定値は、Native へ運ぶ値オブジェクトの既定値と同一にする
    private static readonly DialogOptions s_defaultOptions = new();
    private static readonly DialogPlacement s_defaultPlacement = new();

    /// <summary>サイズと位置の計算の基準になる領域。既定は可視領域。</summary>
    public static readonly BindableProperty LayoutAreaProperty = BindableProperty.CreateAttached(
        "LayoutArea",
        typeof(DialogLayoutArea),
        typeof(Dialog),
        s_defaultOptions.LayoutArea);

    /// <summary>基準 rect の各辺から控除する余白。最大サイズと配置の両方に効く。既定は全辺 24。</summary>
    public static readonly BindableProperty DialogMarginProperty = BindableProperty.CreateAttached(
        "DialogMargin",
        typeof(Thickness),
        typeof(Dialog),
        s_defaultOptions.DialogMargin);

    /// <summary>
    /// 基準 rect の幅に対する比率 (有効域 0 &lt; 値 ≤ 1)。既定は未指定 (-1)。
    /// </summary>
    public static readonly BindableProperty ProportionalWidthProperty = BindableProperty.CreateAttached(
        "ProportionalWidth",
        typeof(double),
        typeof(Dialog),
        s_defaultOptions.ProportionalWidth);

    /// <summary>基準 rect の高さに対する比率。扱いは幅と同じ。</summary>
    public static readonly BindableProperty ProportionalHeightProperty = BindableProperty.CreateAttached(
        "ProportionalHeight",
        typeof(double),
        typeof(Dialog),
        s_defaultOptions.ProportionalHeight);

    /// <summary>ダイアログの背後を覆う色。既定は黒の 40% 不透明。</summary>
    public static readonly BindableProperty OverlayColorProperty = BindableProperty.CreateAttached(
        "OverlayColor",
        typeof(Color),
        typeof(Dialog),
        s_defaultOptions.OverlayColor);

    /// <summary>外側タップをキャンセルと同じ経路で閉じる操作として扱うか。既定は true。</summary>
    public static readonly BindableProperty IsCanceledOnTouchOutsideProperty = BindableProperty.CreateAttached(
        "IsCanceledOnTouchOutside",
        typeof(bool),
        typeof(Dialog),
        s_defaultOptions.IsCanceledOnTouchOutside);

    /// <summary>水平方向の配置。既定は中央。</summary>
    public static readonly BindableProperty HorizontalAlignmentProperty = BindableProperty.CreateAttached(
        "HorizontalAlignment",
        typeof(DialogAlignment),
        typeof(Dialog),
        s_defaultPlacement.HorizontalAlignment);

    /// <summary>垂直方向の配置。既定は中央。</summary>
    public static readonly BindableProperty VerticalAlignmentProperty = BindableProperty.CreateAttached(
        "VerticalAlignment",
        typeof(DialogAlignment),
        typeof(Dialog),
        s_defaultPlacement.VerticalAlignment);

    /// <summary>配置を決めた後に加える水平方向の移動量。正の値で右へ動く。既定は 0。</summary>
    public static readonly BindableProperty OffsetXProperty = BindableProperty.CreateAttached(
        "OffsetX",
        typeof(double),
        typeof(Dialog),
        s_defaultPlacement.OffsetX);

    /// <summary>配置を決めた後に加える垂直方向の移動量。正の値で下へ動く。既定は 0。</summary>
    public static readonly BindableProperty OffsetYProperty = BindableProperty.CreateAttached(
        "OffsetY",
        typeof(double),
        typeof(Dialog),
        s_defaultPlacement.OffsetY);

    /// <summary>
    /// 出入りの演出。既定は未添付 (器の既定のクロスフェード)。
    /// </summary>
    /// <remarks>
    /// 値がクロージャを持つため XAML には書けず、code-behind から
    /// <see cref="SetTransition"/> で添付する (core/ADR-0017)。
    /// </remarks>
    public static readonly BindableProperty TransitionProperty = BindableProperty.CreateAttached(
        "Transition",
        typeof(DialogTransition),
        typeof(Dialog),
        null);

    /// <summary>添付された基準領域を読む。</summary>
    /// <param name="contentView">ダイアログの中身になる View。</param>
    /// <returns>添付された値。未添付なら既定値。</returns>
    public static DialogLayoutArea GetLayoutArea(BindableObject contentView) =>
        (DialogLayoutArea)contentView.GetValue(LayoutAreaProperty);

    /// <summary>基準領域を添付する。</summary>
    /// <param name="contentView">ダイアログの中身になる View。</param>
    /// <param name="value">添付する値。</param>
    public static void SetLayoutArea(BindableObject contentView, DialogLayoutArea value) =>
        contentView.SetValue(LayoutAreaProperty, value);

    /// <summary>添付された余白を読む。</summary>
    /// <param name="contentView">ダイアログの中身になる View。</param>
    /// <returns>添付された値。未添付なら既定値。</returns>
    public static Thickness GetDialogMargin(BindableObject contentView) =>
        (Thickness)contentView.GetValue(DialogMarginProperty);

    /// <summary>余白を添付する。</summary>
    /// <param name="contentView">ダイアログの中身になる View。</param>
    /// <param name="value">添付する値。</param>
    public static void SetDialogMargin(BindableObject contentView, Thickness value) =>
        contentView.SetValue(DialogMarginProperty, value);

    /// <summary>添付された幅の比率を読む。</summary>
    /// <param name="contentView">ダイアログの中身になる View。</param>
    /// <returns>添付された値。未添付なら既定値。</returns>
    public static double GetProportionalWidth(BindableObject contentView) =>
        (double)contentView.GetValue(ProportionalWidthProperty);

    /// <summary>幅の比率を添付する。</summary>
    /// <param name="contentView">ダイアログの中身になる View。</param>
    /// <param name="value">添付する値。</param>
    public static void SetProportionalWidth(BindableObject contentView, double value) =>
        contentView.SetValue(ProportionalWidthProperty, value);

    /// <summary>添付された高さの比率を読む。</summary>
    /// <param name="contentView">ダイアログの中身になる View。</param>
    /// <returns>添付された値。未添付なら既定値。</returns>
    public static double GetProportionalHeight(BindableObject contentView) =>
        (double)contentView.GetValue(ProportionalHeightProperty);

    /// <summary>高さの比率を添付する。</summary>
    /// <param name="contentView">ダイアログの中身になる View。</param>
    /// <param name="value">添付する値。</param>
    public static void SetProportionalHeight(BindableObject contentView, double value) =>
        contentView.SetValue(ProportionalHeightProperty, value);

    /// <summary>添付された覆いの色を読む。</summary>
    /// <param name="contentView">ダイアログの中身になる View。</param>
    /// <returns>添付された値。未添付なら既定値。</returns>
    public static Color? GetOverlayColor(BindableObject contentView) =>
        (Color?)contentView.GetValue(OverlayColorProperty);

    /// <summary>覆いの色を添付する。</summary>
    /// <param name="contentView">ダイアログの中身になる View。</param>
    /// <param name="value">添付する値。</param>
    public static void SetOverlayColor(BindableObject contentView, Color? value) =>
        contentView.SetValue(OverlayColorProperty, value);

    /// <summary>添付された外側タップの扱いを読む。</summary>
    /// <param name="contentView">ダイアログの中身になる View。</param>
    /// <returns>添付された値。未添付なら既定値。</returns>
    public static bool GetIsCanceledOnTouchOutside(BindableObject contentView) =>
        (bool)contentView.GetValue(IsCanceledOnTouchOutsideProperty);

    /// <summary>外側タップの扱いを添付する。</summary>
    /// <param name="contentView">ダイアログの中身になる View。</param>
    /// <param name="value">添付する値。</param>
    public static void SetIsCanceledOnTouchOutside(BindableObject contentView, bool value) =>
        contentView.SetValue(IsCanceledOnTouchOutsideProperty, value);

    /// <summary>添付された水平方向の配置を読む。</summary>
    /// <param name="contentView">ダイアログの中身になる View。</param>
    /// <returns>添付された値。未添付なら既定値。</returns>
    public static DialogAlignment GetHorizontalAlignment(BindableObject contentView) =>
        (DialogAlignment)contentView.GetValue(HorizontalAlignmentProperty);

    /// <summary>水平方向の配置を添付する。</summary>
    /// <param name="contentView">ダイアログの中身になる View。</param>
    /// <param name="value">添付する値。</param>
    public static void SetHorizontalAlignment(BindableObject contentView, DialogAlignment value) =>
        contentView.SetValue(HorizontalAlignmentProperty, value);

    /// <summary>添付された垂直方向の配置を読む。</summary>
    /// <param name="contentView">ダイアログの中身になる View。</param>
    /// <returns>添付された値。未添付なら既定値。</returns>
    public static DialogAlignment GetVerticalAlignment(BindableObject contentView) =>
        (DialogAlignment)contentView.GetValue(VerticalAlignmentProperty);

    /// <summary>垂直方向の配置を添付する。</summary>
    /// <param name="contentView">ダイアログの中身になる View。</param>
    /// <param name="value">添付する値。</param>
    public static void SetVerticalAlignment(BindableObject contentView, DialogAlignment value) =>
        contentView.SetValue(VerticalAlignmentProperty, value);

    /// <summary>添付された水平方向の移動量を読む。</summary>
    /// <param name="contentView">ダイアログの中身になる View。</param>
    /// <returns>添付された値。未添付なら既定値。</returns>
    public static double GetOffsetX(BindableObject contentView) =>
        (double)contentView.GetValue(OffsetXProperty);

    /// <summary>水平方向の移動量を添付する。</summary>
    /// <param name="contentView">ダイアログの中身になる View。</param>
    /// <param name="value">添付する値。</param>
    public static void SetOffsetX(BindableObject contentView, double value) =>
        contentView.SetValue(OffsetXProperty, value);

    /// <summary>添付された垂直方向の移動量を読む。</summary>
    /// <param name="contentView">ダイアログの中身になる View。</param>
    /// <returns>添付された値。未添付なら既定値。</returns>
    public static double GetOffsetY(BindableObject contentView) =>
        (double)contentView.GetValue(OffsetYProperty);

    /// <summary>垂直方向の移動量を添付する。</summary>
    /// <param name="contentView">ダイアログの中身になる View。</param>
    /// <param name="value">添付する値。</param>
    public static void SetOffsetY(BindableObject contentView, double value) =>
        contentView.SetValue(OffsetYProperty, value);

    /// <summary>添付された出入りの演出を読む。</summary>
    /// <param name="contentView">ダイアログの中身になる View。</param>
    /// <returns>添付された値。未添付なら <see langword="null"/>。</returns>
    public static DialogTransition? GetTransition(BindableObject contentView) =>
        (DialogTransition?)contentView.GetValue(TransitionProperty);

    /// <summary>出入りの演出を添付する。</summary>
    /// <param name="contentView">ダイアログの中身になる View。</param>
    /// <param name="value">添付する値。<see langword="null"/> なら器の既定に戻す。</param>
    public static void SetTransition(BindableObject contentView, DialogTransition? value) =>
        contentView.SetValue(TransitionProperty, value);

    /// <summary>添付された静的メタ属性を、Native への輸送単位へ束ねる。</summary>
    /// <param name="contentView">ダイアログの中身になる View。</param>
    /// <returns>束ねた静的メタ属性。添付のない項目は既定値になる。</returns>
    internal static DialogOptions AttachedOptions(BindableObject contentView) => new()
    {
        LayoutArea = GetLayoutArea(contentView),
        DialogMargin = GetDialogMargin(contentView),
        ProportionalWidth = GetProportionalWidth(contentView),
        ProportionalHeight = GetProportionalHeight(contentView),
        OverlayColor = GetOverlayColor(contentView),
        IsCanceledOnTouchOutside = GetIsCanceledOnTouchOutside(contentView),
    };

    /// <summary>添付された動的メタ属性を、Native への輸送単位へ束ねる。</summary>
    /// <param name="contentView">ダイアログの中身になる View。</param>
    /// <returns>束ねた置き場所。添付のない項目は既定値になる。</returns>
    internal static DialogPlacement AttachedPlacement(BindableObject contentView) => new()
    {
        HorizontalAlignment = GetHorizontalAlignment(contentView),
        VerticalAlignment = GetVerticalAlignment(contentView),
        OffsetX = GetOffsetX(contentView),
        OffsetY = GetOffsetY(contentView),
    };
}
