using Microsoft.Maui.Controls;
using Microsoft.Maui.Graphics;

namespace KsDialogs.PlacementHost;

/// <summary>中身を出す機能 (3 機能とも同じ中身の受け渡しを通る)。</summary>
internal enum ContentSizeFeature
{
    /// <summary>ダイアログ。</summary>
    Dialog,

    /// <summary>カスタム Loading。</summary>
    Loading,

    /// <summary>カスタム Toast。</summary>
    Toast,
}

/// <summary>
/// 中身のルートが宣言した大きさで表示されるかを確かめる 1 構成。
/// </summary>
/// <remarks>
/// 中の要素はルートの指定より小さい自然サイズにしてある。ルートの指定が効かず中身の大きさで測られると、
/// 表示される大きさが中の要素の大きさまで縮み、期待値と食い違う。
/// </remarks>
/// <param name="Name">シナリオ名 (ログの識別子)。</param>
/// <param name="Feature">中身を出す機能。</param>
/// <param name="CreateContent">中身のルートを作る操作。表示のたびに新しく作る。</param>
/// <param name="Expected">表示されるはずの大きさ (MAUI の単位)。</param>
internal sealed record ContentSizeScenario(
    string Name,
    ContentSizeFeature Feature,
    Func<View> CreateContent,
    Size Expected)
{
    /// <summary>
    /// 外形も確かめるときの期待値。0 より大きい軸は可視領域に対する比率、0 の軸は fill。
    /// 外形を確かめないなら <see langword="null"/>。
    /// </summary>
    public Size? ExpectedOuterRatio { get; init; }

    private static readonly Size s_requested = new(160, 100);
    private static readonly Size s_minimum = new(200, 120);
    private const double ProportionalWidthRatio = 0.8;

    /// <summary>確かめる構成の一覧。</summary>
    public static IReadOnlyList<ContentSizeScenario> All { get; } =
    [
        new("size-dialog-contentview", ContentSizeFeature.Dialog, RequestedContentView, s_requested),
        new("size-dialog-grid", ContentSizeFeature.Dialog, RequestedGrid, s_requested),
        new("size-loading-contentview", ContentSizeFeature.Loading, RequestedContentView, s_requested),
        new("size-loading-grid", ContentSizeFeature.Loading, RequestedGrid, s_requested),
        new("size-toast-contentview", ContentSizeFeature.Toast, RequestedContentView, s_requested),
        new("size-toast-grid", ContentSizeFeature.Toast, RequestedGrid, s_requested),
        // 最小の指定だけを持つルート。中の要素より大きい最小値まで広がるはず
        new("size-dialog-contentview-minimum", ContentSizeFeature.Dialog, MinimumContentView, s_minimum),
        // 器が大きさを決める軸 (幅は比率指定、高さは fill)。外形はその大きさになり、
        // 宣言サイズのルートはその中央に置かれるはず
        new("size-dialog-contentview-proportional-fill", ContentSizeFeature.Dialog, ProportionalFillContentView, s_requested)
        {
            ExpectedOuterRatio = new Size(ProportionalWidthRatio, 0),
        },
    ];

    /// <summary>大きさを指定した ContentView のルート。</summary>
    private static View RequestedContentView() => new ContentView
    {
        WidthRequest = s_requested.Width,
        HeightRequest = s_requested.Height,
        BackgroundColor = Colors.Orange,
        Content = SmallLabel(),
    };

    /// <summary>大きさを指定した Grid のルート。</summary>
    private static View RequestedGrid() => new Grid
    {
        WidthRequest = s_requested.Width,
        HeightRequest = s_requested.Height,
        BackgroundColor = Colors.LightGreen,
        Children = { SmallLabel() },
    };

    /// <summary>大きさを指定し、幅を比率指定・高さを fill にした ContentView のルート。</summary>
    private static View ProportionalFillContentView()
    {
        View root = RequestedContentView();
        Dialog.SetProportionalWidth(root, ProportionalWidthRatio);
        Dialog.SetVerticalAlignment(root, DialogAlignment.Fill);
        return root;
    }

    /// <summary>最小の大きさだけを指定した ContentView のルート。</summary>
    private static View MinimumContentView() => new ContentView
    {
        MinimumWidthRequest = s_minimum.Width,
        MinimumHeightRequest = s_minimum.Height,
        BackgroundColor = Colors.Plum,
        Content = SmallLabel(),
    };

    private static Label SmallLabel() => new()
    {
        Text = "Size",
        HorizontalOptions = LayoutOptions.Center,
        VerticalOptions = LayoutOptions.Center,
    };
}

/// <summary>大きさを測るために出す中身の ViewModel。3 機能のどれでも使えるようにしてある。</summary>
internal sealed class ContentSizeProbeViewModel : IDialogViewModel, ILoadingViewModel, IToastViewModel
{
    /// <summary>ダイアログとして出したときの閉じ口。ダイアログ以外では <see langword="null"/>。</summary>
    public DialogNotifier<bool>? Notifier { get; set; }
}
