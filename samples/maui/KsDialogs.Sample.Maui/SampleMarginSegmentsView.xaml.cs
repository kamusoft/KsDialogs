namespace KsDialogs.Sample.Maui;

/// <summary>全辺そろえの余白を3択から選ぶセグメント。</summary>
/// <remarks>
/// 見た目と読み上げの与え方は配置のセグメント (<see cref="SampleAlignmentSegmentsView" />) と同じにする。
/// 0 は契約の既定値、24 は既定 Toast のデフォルト View が自分に持つ余白と同じ値。
/// </remarks>
public partial class SampleMarginSegmentsView : ContentView
{
    /// <summary>セグメントを組み立てる。初期の選択は契約の既定値と同じ 0。</summary>
    public SampleMarginSegmentsView()
    {
        InitializeComponent();
        RefreshSelection();
    }

    /// <summary>選択中の余白 (全辺にそろえて添付する値)。</summary>
    public double Selection { get; private set; }

    private void OnZeroSelected(object? sender, EventArgs e) => Select(0);

    private void OnTwentyFourSelected(object? sender, EventArgs e) => Select(24);

    private void OnFortyEightSelected(object? sender, EventArgs e) => Select(48);

    /// <summary>表示が組み上がったあとに、読み上げ用の名前と選択状態を与え直す。</summary>
    /// <param name="sender">未使用。</param>
    /// <param name="e">未使用。</param>
    private void OnSegmentHandlerChanged(object? sender, EventArgs e) => RefreshSelection();

    private void Select(double margin)
    {
        Selection = margin;
        RefreshSelection();
    }

    private void RefreshSelection()
    {
        ApplySegmentState(ZeroSegment, Selection == 0);
        ApplySegmentState(TwentyFourSegment, Selection == 24);
        ApplySegmentState(FortyEightSegment, Selection == 48);
    }

    private static void ApplySegmentState(Button segment, bool isSelected)
    {
        segment.BackgroundColor = isSelected ? ThemeColor("SamplePrimary") : Colors.Transparent;
        segment.TextColor = isSelected ? ThemeColor("SampleOnPrimary") : ThemeColor("SampleOnSurfaceMuted");
        segment.FontAttributes = isSelected ? FontAttributes.Bold : FontAttributes.None;

        // 数字だけでは何の値か読めないため、読み上げ名は行の文言と組にする
        SemanticProperties.SetDescription(segment, $"{SampleText.MarginLabel} {segment.Text}");
        ApplySelectedState(segment, isSelected);
    }

    /// <summary>選択状態を読み上げ情報に乗せる。</summary>
    /// <param name="segment">対象の選択肢。</param>
    /// <param name="isSelected">選ばれているか。</param>
    /// <remarks>
    /// MAUI には選択状態を読み上げへ伝える共通の API がないため、platform 側の選択状態を直接与える。
    /// 属性は読み上げにだけ効き、見た目は変わらない。
    /// </remarks>
    private static void ApplySelectedState(Button segment, bool isSelected)
    {
#if IOS
        if (segment.Handler?.PlatformView is UIKit.UIButton platformButton)
        {
            platformButton.AccessibilityTraits = isSelected
                ? platformButton.AccessibilityTraits | UIKit.UIAccessibilityTrait.Selected
                : platformButton.AccessibilityTraits & ~UIKit.UIAccessibilityTrait.Selected;
        }
#elif ANDROID
        if (segment.Handler?.PlatformView is Android.Views.View platformView)
        {
            platformView.Selected = isSelected;
        }
#endif
    }

    /// <summary>共有の配色から色を引く。</summary>
    /// <param name="key">配色のキー。</param>
    /// <returns>引けた色。引けなければ透明。</returns>
    private static Color ThemeColor(string key) =>
        Application.Current?.Resources.TryGetValue(key, out object? value) == true && value is Color color
            ? color
            : Colors.Transparent;
}
