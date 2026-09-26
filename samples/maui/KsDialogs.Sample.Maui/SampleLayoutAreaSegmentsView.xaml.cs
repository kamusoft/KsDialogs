namespace KsDialogs.Sample.Maui;

/// <summary>基準領域を3択から選ぶセグメント。</summary>
/// <remarks>
/// 見た目と読み上げの与え方は配置のセグメント (<see cref="SampleAlignmentSegmentsView" />) と同じにする。
/// 選択肢の文言はどれも一意なので、読み上げ名は選択肢の文言だけにする。
/// </remarks>
public partial class SampleLayoutAreaSegmentsView : ContentView
{
    /// <summary>セグメントを組み立てる。初期の選択は契約の既定値と同じ可視領域。</summary>
    public SampleLayoutAreaSegmentsView()
    {
        InitializeComponent();
        RefreshSelection();
    }

    /// <summary>選択中の基準領域。</summary>
    public DialogLayoutArea Selection { get; private set; } = DialogLayoutArea.VisibleArea;

    private void OnWindowSelected(object? sender, EventArgs e) => Select(DialogLayoutArea.Window);

    private void OnVisibleAreaSelected(object? sender, EventArgs e) => Select(DialogLayoutArea.VisibleArea);

    private void OnCurrentPageSelected(object? sender, EventArgs e) => Select(DialogLayoutArea.CurrentPage);

    /// <summary>表示が組み上がったあとに、読み上げ用の名前と選択状態を与え直す。</summary>
    /// <param name="sender">未使用。</param>
    /// <param name="e">未使用。</param>
    private void OnSegmentHandlerChanged(object? sender, EventArgs e) => RefreshSelection();

    private void Select(DialogLayoutArea layoutArea)
    {
        Selection = layoutArea;
        RefreshSelection();
    }

    private void RefreshSelection()
    {
        ApplySegmentState(WindowSegment, Selection == DialogLayoutArea.Window);
        ApplySegmentState(VisibleAreaSegment, Selection == DialogLayoutArea.VisibleArea);
        ApplySegmentState(CurrentPageSegment, Selection == DialogLayoutArea.CurrentPage);
    }

    private static void ApplySegmentState(Button segment, bool isSelected)
    {
        segment.BackgroundColor = isSelected ? ThemeColor("SamplePrimary") : Colors.Transparent;
        segment.TextColor = isSelected ? ThemeColor("SampleOnPrimary") : ThemeColor("SampleOnSurfaceMuted");
        segment.FontAttributes = isSelected ? FontAttributes.Bold : FontAttributes.None;
        SemanticProperties.SetDescription(segment, segment.Text);
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
