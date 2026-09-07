namespace KsDialogs.Sample.Maui;

/// <summary>配置を3択から選ぶセグメント。</summary>
/// <remarks>
/// 契約の配置には有効領域いっぱいに広げる選択肢もあるが、このセグメントは寄せ先の3択だけを扱う。
/// </remarks>
public partial class SampleAlignmentSegmentsView : ContentView
{
    private string _axisLabel = string.Empty;

    /// <summary>セグメントを組み立てる。初期の選択は契約の既定値と同じ中央。</summary>
    public SampleAlignmentSegmentsView()
    {
        InitializeComponent();
        RefreshSelection();
    }

    /// <summary>このセグメントが属する行の項目名。読み上げ名を行ごとに一意にするために使う。</summary>
    public string AxisLabel
    {
        get => _axisLabel;
        set
        {
            _axisLabel = value;
            RefreshSelection();
        }
    }

    /// <summary>選択中の配置。</summary>
    public DialogAlignment Selection { get; private set; } = DialogAlignment.Center;

    private void OnStartSelected(object? sender, EventArgs e) => Select(DialogAlignment.Start);

    private void OnCenterSelected(object? sender, EventArgs e) => Select(DialogAlignment.Center);

    private void OnEndSelected(object? sender, EventArgs e) => Select(DialogAlignment.End);

    /// <summary>表示が組み上がったあとに、読み上げ用の名前と選択状態を与え直す。</summary>
    /// <param name="sender">未使用。</param>
    /// <param name="e">未使用。</param>
    private void OnSegmentHandlerChanged(object? sender, EventArgs e) => RefreshSelection();

    private void Select(DialogAlignment alignment)
    {
        Selection = alignment;
        RefreshSelection();
    }

    private void RefreshSelection()
    {
        ApplySegmentState(StartSegment, Selection == DialogAlignment.Start);
        ApplySegmentState(CenterSegment, Selection == DialogAlignment.Center);
        ApplySegmentState(EndSegment, Selection == DialogAlignment.End);
    }

    private void ApplySegmentState(Button segment, bool isSelected)
    {
        segment.BackgroundColor = isSelected ? ThemeColor("SamplePrimary") : Colors.Transparent;
        segment.TextColor = isSelected ? ThemeColor("SampleOnPrimary") : ThemeColor("SampleOnSurfaceMuted");
        segment.FontAttributes = isSelected ? FontAttributes.Bold : FontAttributes.None;

        // 同じ文言の選択肢が2行に並ぶため、読み上げ名は行の文言と組にして一意にする
        SemanticProperties.SetDescription(segment, $"{_axisLabel} {segment.Text}");
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
