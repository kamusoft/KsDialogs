using Microsoft.Maui.Controls;
using Microsoft.Maui.Controls.PlatformConfiguration.AndroidSpecific;
using TabbedPage = Microsoft.Maui.Controls.TabbedPage;
using VisualElement = Microsoft.Maui.Controls.VisualElement;

namespace KsDialogs.PlacementHost;

/// <summary>1 つのページ構成と、そこで基準になるはずの要素。</summary>
/// <param name="Name">シナリオ名 (ログの識別子)。</param>
/// <param name="SetUpAsync">画面にページ構成を組み、確かめる内容を返す操作。</param>
internal sealed record PlacementScenario(string Name, Func<Window, Task<PlacementCheck>> SetUpAsync)
{
    /// <summary>確かめるページ構成の一覧。</summary>
    public static IReadOnlyList<PlacementScenario> All { get; } =
    [
        new("tabbed-navigation", TabbedNavigationAsync),
        new("shell", ShellAsync),
        new("modal", ModalAsync),
        new("modal-navigation", ModalNavigationAsync),
        new("flyout", FlyoutAsync),
        new("plain-content-page", PlainContentPageAsync),
        new("provider-override", ProviderOverrideAsync),
        new("provider-empty-falls-back", ProviderEmptyFallsBackAsync),
#if ANDROID
        // iOS の器は提示先と同じ window に載り、器を候補から外す規則を持たないため Android だけで確かめる
        new("provider-container-falls-back", ProviderContainerFallsBackAsync),
#endif
    ];

    /// <summary>TabbedPage の選択中の子が NavigationPage で、2 枚目を push した画面。</summary>
    private static async Task<PlacementCheck> TabbedNavigationAsync(Window window)
    {
        ContentPage top = Labeled("Tabbed / Navigation top");
        NavigationPage navigation = new(Labeled("Tabbed / Navigation root")) { Title = "Navigation" };
        TabbedPage tabbed = BottomTabbed(Labeled("Tab A"), navigation);
        window.Page = tabbed;
        tabbed.CurrentPage = navigation;
        await PlacementWaiting.UntilRenderedAsync(navigation);
        await navigation.PushAsync(top, false);
        return new PlacementCheck(() => top) { ExpectsBarBelowPage = true };
    }

    /// <summary>Shell のタブの 1 枚目を表示している画面。</summary>
    private static Task<PlacementCheck> ShellAsync(Window window)
    {
        Shell shell = new()
        {
            Items =
            {
                new TabBar
                {
                    Items =
                    {
                        new ShellContent { Title = "A", Content = Labeled("Shell A") },
                        new ShellContent { Title = "B", Content = Labeled("Shell B") },
                    },
                },
            },
        };
        window.Page = shell;
        return Task.FromResult(new PlacementCheck(() => shell.CurrentPage) { ExpectsBarBelowPage = true });
    }

    /// <summary>TabbedPage の上にモーダルで ContentPage を出した画面。</summary>
    private static async Task<PlacementCheck> ModalAsync(Window window)
    {
        ContentPage underneath = Labeled("Under the modal");
        TabbedPage tabbed = BottomTabbed(underneath, Labeled("Tab B"));
        window.Page = tabbed;
        // 全画面のモーダルの下になったページは画面から外れて測れなくなるため、出す前に測っておく
        PlacementRect? underneathArea = await PlacementWaiting.UntilStableAsync(
            () => PlacementMeasurement.ExpectedArea(underneath),
            TimeSpan.FromMilliseconds(400));
        ContentPage modal = Labeled("Modal");
        await tabbed.Navigation.PushModalAsync(modal, false);
        return new PlacementCheck(() => modal)
        {
            UnderneathArea = underneathArea,
            TearDownAsync = () => tabbed.Navigation.PopModalAsync(false),
        };
    }

    /// <summary>TabbedPage の上にモーダルで NavigationPage を出した画面。先端のページはナビゲーションバーの下から始まる。</summary>
    private static async Task<PlacementCheck> ModalNavigationAsync(Window window)
    {
        ContentPage underneath = Labeled("Under the modal navigation");
        TabbedPage tabbed = BottomTabbed(underneath, Labeled("Tab B"));
        window.Page = tabbed;
        await PlacementWaiting.UntilRenderedAsync(underneath);
        PlacementRect? underneathArea = await PlacementWaiting.UntilStableAsync(
            () => PlacementMeasurement.ExpectedArea(underneath),
            TimeSpan.FromMilliseconds(400));
        ContentPage top = Labeled("Modal navigation top");
        await tabbed.Navigation.PushModalAsync(new NavigationPage(top), false);
        return new PlacementCheck(() => top)
        {
            UnderneathArea = underneathArea,
            ExpectsBarAbovePage = true,
            TearDownAsync = () => tabbed.Navigation.PopModalAsync(false),
        };
    }

    /// <summary>FlyoutPage の Detail が NavigationPage で、2 枚目を push した画面。</summary>
    private static async Task<PlacementCheck> FlyoutAsync(Window window)
    {
        ContentPage top = Labeled("Flyout / Detail top");
        NavigationPage detail = new(Labeled("Flyout / Detail root"));
        window.Page = new FlyoutPage { Flyout = new ContentPage { Title = "Menu" }, Detail = detail };
        await PlacementWaiting.UntilRenderedAsync(detail);
        await detail.PushAsync(top, false);
        return new PlacementCheck(() => top) { ExpectsBarAbovePage = true };
    }

    /// <summary>容れ物を持たない素の ContentPage の画面。</summary>
    private static Task<PlacementCheck> PlainContentPageAsync(Window window)
    {
        ContentPage page = Labeled("Plain ContentPage");
        window.Page = page;
        return Task.FromResult(new PlacementCheck(() => page) { ComparesWithVisibleArea = true });
    }

    /// <summary>TabbedPage の画面で、ページの中の要素を返す関数を登録した画面。</summary>
    private static Task<PlacementCheck> ProviderOverrideAsync(Window window)
    {
        BoxView target = new() { Color = Microsoft.Maui.Graphics.Colors.LightBlue };
        Grid grid = new()
        {
            RowDefinitions = { new RowDefinition(GridLength.Star), new RowDefinition(200), new RowDefinition(GridLength.Star) },
        };
        grid.Add(target, 0, 1);
        ContentPage page = new() { Title = "Override", Content = grid };
        window.Page = BottomTabbed(page, Labeled("Tab B"));
        return Task.FromResult(new PlacementCheck(() => target)
        {
            Provider = () => target,
            Underneath = page,
        });
    }

    /// <summary>
    /// 描画済みだが大きさが 0 の要素を返す関数を登録した画面。
    /// 空の矩形は受け付けられず、MAUI の既定の探し方のページが基準になる。
    /// </summary>
    /// <remarks>
    /// MAUI 側が空の矩形を外さずに Native 実装へ渡すと、Android では Native 実装が外して可視領域へ落ち、
    /// iOS では Native 実装が外して内蔵の view controller 階層の走査へ進む。どちらでも既定のページと違う基準に
    /// なるよう、iOS は走査がページまで降りない FlyoutPage (Detail の NavigationPage の先端がページ) で組む。
    /// TabbedPage では走査も選択中のタブまで降りて既定のページと同じ矩形を選ぶため、MAUI 側の判定の有無を見分けられない。
    /// </remarks>
    private static async Task<PlacementCheck> ProviderEmptyFallsBackAsync(Window window)
    {
        BoxView empty = new() { WidthRequest = 0, HeightRequest = 0, HorizontalOptions = LayoutOptions.Start };
        ContentPage page = new()
        {
            Title = "Empty override",
            Content = new VerticalStackLayout { Children = { new Label { Text = "Empty override", Margin = 16 }, empty } },
        };
#if IOS
        NavigationPage detail = new(page);
        window.Page = new FlyoutPage { Flyout = new ContentPage { Title = "Menu" }, Detail = detail };
        await PlacementWaiting.UntilRenderedAsync(empty);
        return new PlacementCheck(() => page)
        {
            Provider = () => empty,
            ExpectsBarAbovePage = true,
        };
#else
        window.Page = BottomTabbed(page, Labeled("Tab B"));
        await PlacementWaiting.UntilRenderedAsync(empty);
        return new PlacementCheck(() => page)
        {
            Provider = () => empty,
            ExpectsBarBelowPage = true,
        };
#endif
    }

#if ANDROID
    /// <summary>
    /// TabbedPage の画面で、先に出したダイアログの中身を返す関数を登録した画面。
    /// KsDialogs 自身の器に載った要素は受け付けられず、既定の探し方のページ (タブバーの上まで) が基準になる (Android)。
    /// </summary>
    private static async Task<PlacementCheck> ProviderContainerFallsBackAsync(Window window)
    {
        ContentPage page = Labeled("Container override");
        window.Page = BottomTabbed(page, Labeled("Tab B"));
        await PlacementWaiting.UntilRenderedAsync(page);
        PlacementProbeViewModel holder = new(DialogLayoutArea.VisibleArea, DialogAlignment.Start);
        Task<DialogResult<bool>> holding = Dialog.Instance.ShowAsync(holder);
        await PlacementWaiting.UntilStableAsync(
            () => PlacementMeasurement.ContentRect(holder.Content),
            TimeSpan.FromMilliseconds(400));
        return new PlacementCheck(() => page)
        {
            Provider = () => holder.Content,
            ExpectsBarBelowPage = true,
            TearDownAsync = async () =>
            {
                holder.Close();
                await holding;
            },
        };
    }
#endif

    /// <summary>タブを下に並べた TabbedPage。Android の既定 (上) では基準領域の違いが下端に現れないため揃える。</summary>
    private static TabbedPage BottomTabbed(params Page[] children)
    {
        TabbedPage tabbed = new();
        tabbed.On<Microsoft.Maui.Controls.PlatformConfiguration.Android>().SetToolbarPlacement(ToolbarPlacement.Bottom);
        foreach (Page child in children)
        {
            tabbed.Children.Add(child);
        }

        return tabbed;
    }

    private static ContentPage Labeled(string title) => new()
    {
        Title = title,
        Content = new Label { Text = title, Margin = 16 },
    };
}

/// <summary>1 つのシナリオで確かめる内容。</summary>
/// <param name="ExpectedElement">基準になるはずの要素を返す操作 (描画後に評価する)。</param>
internal sealed record PlacementCheck(Func<VisualElement?> ExpectedElement)
{
    /// <summary>登録する上書きの関数。登録しないなら <see langword="null"/>。</summary>
    public Func<VisualElement?>? Provider { get; init; }

    /// <summary>基準にならないはずの下のページ。基準領域がそれと違うことも確かめる。</summary>
    public VisualElement? Underneath { get; init; }

    /// <summary>基準にならないはずの下のページの領域 (画面から外れる前に測ったもの)。</summary>
    public PlacementRect? UnderneathArea { get; init; }

    /// <summary>ページの下にバー (タブバー) があり、ページの下端が可視領域の下端より上にあるはずか。</summary>
    public bool ExpectsBarBelowPage { get; init; }

    /// <summary>ページの上にバー (ナビゲーションバー) があり、ページの上端が可視領域の上端より下にあるはずか。</summary>
    public bool ExpectsBarAbovePage { get; init; }

    /// <summary>基準領域が可視領域と同じ結果になるはずか (可視領域で出したダイアログと位置を比べる)。</summary>
    public bool ComparesWithVisibleArea { get; init; }

    /// <summary>シナリオの後片付け。</summary>
    public Func<Task> TearDownAsync { get; init; } = () => Task.CompletedTask;
}
