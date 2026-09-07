namespace KsDialogs.Sample.Maui;

/// <summary>選択肢をチップで並べて 1 つ選ばせる操作部。</summary>
public sealed class SampleChipsView : ContentView
{
    /// <summary>チップの角丸。</summary>
    private const int CornerRadius = 10;

    /// <summary>チップの枠線の太さ。</summary>
    private const double StrokeThickness = 1d;

    /// <summary>チップどうしの隙間。</summary>
    private const double Gap = 8d;

    /// <summary>文言の左右の余白。</summary>
    private const double HorizontalPadding = 6d;

    private readonly List<Button> _chips = [];
    private readonly Action<int> _onSelected;
    private int _selectedIndex;

    /// <summary>チップを並べる。</summary>
    /// <param name="labels">チップの文言。読み上げ名にもそのまま使う。</param>
    /// <param name="columns">1 行に並べるチップの数。</param>
    /// <param name="minHeight">チップの高さの下限。</param>
    /// <param name="fontSize">文言の大きさ。</param>
    /// <param name="onSelected">選択が変わったときに呼ばれる。引数は <paramref name="labels"/> の位置。</param>
    public SampleChipsView(
        IReadOnlyList<string> labels,
        int columns,
        double minHeight,
        double fontSize,
        Action<int> onSelected)
    {
        _onSelected = onSelected;

        int rows = (labels.Count + columns - 1) / columns;
        Grid grid = new()
        {
            ColumnSpacing = Gap,
            RowSpacing = Gap,
        };

        for (int column = 0; column < columns; column++)
        {
            grid.ColumnDefinitions.Add(new ColumnDefinition(GridLength.Star));
        }

        for (int row = 0; row < rows; row++)
        {
            grid.RowDefinitions.Add(new RowDefinition(GridLength.Auto));
        }

        for (int index = 0; index < labels.Count; index++)
        {
            Button chip = CreateChip(labels[index], index, minHeight, fontSize);
            _chips.Add(chip);
            grid.Add(chip, index % columns, index / columns);
        }

        Content = grid;
        RefreshSelection();
    }

    /// <summary>選択中の位置。</summary>
    public int SelectedIndex
    {
        get => _selectedIndex;
        set
        {
            _selectedIndex = value;
            RefreshSelection();
        }
    }

    private Button CreateChip(string label, int index, double minHeight, double fontSize)
    {
        Button chip = new()
        {
            Text = label,
            FontSize = fontSize,
            HeightRequest = minHeight,
            MinimumHeightRequest = 0d,
            Padding = new Thickness(HorizontalPadding, 0d),
            CornerRadius = CornerRadius,
            BorderWidth = StrokeThickness,
            LineBreakMode = LineBreakMode.NoWrap,
        };

        // 文言をそのまま読み上げ名にする
        SemanticProperties.SetDescription(chip, label);
        chip.Clicked += (_, _) =>
        {
            SelectedIndex = index;
            _onSelected(index);
        };
        chip.HandlerChanged += (_, _) => RefreshSelection();
        return chip;
    }

    private void RefreshSelection()
    {
        for (int index = 0; index < _chips.Count; index++)
        {
            Button chip = _chips[index];
            bool isSelected = index == _selectedIndex;

            chip.BackgroundColor = isSelected ? ThemeColor("SamplePrimary") : ThemeColor("SampleSurface");
            chip.TextColor = isSelected ? ThemeColor("SampleOnPrimary") : ThemeColor("SampleOnSurface");
            chip.BorderColor = isSelected ? ThemeColor("SamplePrimary") : ThemeColor("SampleDivider");
            chip.FontAttributes = isSelected ? FontAttributes.Bold : FontAttributes.None;
            ApplySelectedState(chip, isSelected);
        }
    }

    /// <summary>選択状態を読み上げ情報に乗せる。</summary>
    /// <param name="chip">対象の選択肢。</param>
    /// <param name="isSelected">選ばれているか。</param>
    /// <remarks>
    /// MAUI には選択状態を読み上げへ伝える共通の API がないため、platform 側の選択状態を直接与える。
    /// 属性は読み上げにだけ効き、見た目は変わらない。
    /// </remarks>
    private static void ApplySelectedState(Button chip, bool isSelected)
    {
#if IOS
        if (chip.Handler?.PlatformView is UIKit.UIButton platformButton)
        {
            platformButton.AccessibilityTraits = isSelected
                ? platformButton.AccessibilityTraits | UIKit.UIAccessibilityTrait.Selected
                : platformButton.AccessibilityTraits & ~UIKit.UIAccessibilityTrait.Selected;
        }
#elif ANDROID
        if (chip.Handler?.PlatformView is Android.Views.View platformView)
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
