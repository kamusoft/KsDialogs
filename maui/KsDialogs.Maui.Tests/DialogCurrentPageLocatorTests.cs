using System;
using System.Collections.Generic;
using KsDialogs.Maui.Tests.Support;
using Microsoft.Maui.Controls;
using NUnit.Framework;

namespace KsDialogs.Maui.Tests;

/// <summary>
/// 基準領域「表示中のページ」の既定の探し方 (ページ構成の辿り方) と、取得元の優先順位の検証。
/// </summary>
/// <remarks>
/// 辿り方と優先順位は両 OS で共通の部品が受け持つため、ここではハンドラの代わりに
/// platform view を持つだけの <see cref="StubPlatformViewHandler"/> を付けて、どの要素の platform view が
/// 選ばれるかを見る。選ばれた platform view から矩形を求めて配置する部分は Native 実装の責務で、
/// 実際の配置は両 OS のテストホストで確かめる。
/// </remarks>
[TestFixture]
public class DialogCurrentPageLocatorTests
{
    private IDisposable? _dispatcher;

    /// <summary>ページ構成を組めるよう、その場で呼ぶ dispatcher に差し替える。</summary>
    [SetUp]
    public void InstallDispatcher() => _dispatcher = InlineDispatcherProvider.Install();

    /// <summary>dispatcher を元へ戻す。</summary>
    [TearDown]
    public void RestoreDispatcher() => _dispatcher?.Dispose();

    /// <summary>TabbedPage の選択中の子が NavigationPage なら、その先端の ContentPage まで降りる。</summary>
    [Test]
    [Description("TabbedPage → NavigationPage → ContentPage を先端まで降りる")]
    public void TabbedPageWithNavigationPageResolvesToTheTopContentPage()
    {
        ContentPage top = new() { Title = "top" };
        NavigationPage navigation = new(top);
        TabbedPage tabbed = new() { Children = { new ContentPage(), navigation } };
        tabbed.CurrentPage = navigation;

        Assert.That(DialogCurrentPageLocator.FindDefaultPage([], tabbed), Is.SameAs(top));
    }

    /// <summary>Shell では <see cref="Shell.CurrentPage"/> が表示中のページになる。</summary>
    [Test]
    [Description("Shell は Shell.CurrentPage まで降りる")]
    public void ShellResolvesToItsCurrentPage()
    {
        ContentPage shown = new();
        Shell shell = new() { Items = { new ShellContent { Content = shown } } };

        Assert.That(DialogCurrentPageLocator.FindDefaultPage([], shell), Is.SameAs(shown));
    }

    /// <summary>モーダルで出したページがあれば、根のページではなくその先頭が表示中のページになる。</summary>
    [Test]
    [Description("モーダルの先頭が根のページより優先される")]
    public void ModalPageWinsOverTheRootPage()
    {
        ContentPage underneath = new();
        TabbedPage tabbed = new() { Children = { underneath } };
        ContentPage firstModal = new();
        ContentPage topModal = new();

        Assert.That(
            DialogCurrentPageLocator.FindDefaultPage([firstModal, topModal], tabbed),
            Is.SameAs(topModal));
    }

    /// <summary>モーダルで出したのが NavigationPage なら、その先端まで降りる。</summary>
    [Test]
    [Description("モーダルの NavigationPage は先端まで降りる")]
    public void ModalNavigationPageResolvesToItsTopPage()
    {
        ContentPage top = new();

        Assert.That(
            DialogCurrentPageLocator.FindDefaultPage([new NavigationPage(top)], new ContentPage()),
            Is.SameAs(top));
    }

    /// <summary>FlyoutPage では Detail の NavigationPage の先端が表示中のページになる。</summary>
    [Test]
    [Description("FlyoutPage は Detail のスタック先端まで降りる")]
    public void FlyoutPageResolvesToTheTopOfItsDetail()
    {
        ContentPage top = new();
        FlyoutPage flyout = new()
        {
            Flyout = new ContentPage { Title = "menu" },
            Detail = new NavigationPage(top),
        };

        Assert.That(DialogCurrentPageLocator.FindDefaultPage([], flyout), Is.SameAs(top));
    }

    /// <summary>容れ物でない素のページは、そのまま表示中のページになる。</summary>
    [Test]
    [Description("素の ContentPage はそのまま表示中のページ")]
    public void PlainContentPageResolvesToItself()
    {
        ContentPage page = new();

        Assert.That(DialogCurrentPageLocator.FindDefaultPage([], page), Is.SameAs(page));
    }

    /// <summary>画面の <see cref="Window.Page"/> を起点に辿る。</summary>
    [Test]
    [Description("Window.Page を起点に辿る")]
    public void WindowPageIsTheStartingPoint()
    {
        ContentPage top = new();
        Window window = new(new NavigationPage(top));

        Assert.That(DialogCurrentPageLocator.FindDefaultPage(window), Is.SameAs(top));
    }

    /// <summary>子を持たない容れ物は、表示中のページが無いものとして扱う。</summary>
    [Test]
    [Description("子の無い TabbedPage は表示中のページなし")]
    public void ContainerWithoutChildrenHasNoCurrentPage()
    {
        Assert.That(DialogCurrentPageLocator.FindDefaultPage([], new TabbedPage()), Is.Null);
    }

    /// <summary>登録した関数が返した要素の platform view が、既定の探し方より優先される。</summary>
    [Test]
    [Description("登録した関数の要素が既定の探し方に勝つ")]
    public void RegisteredElementWinsOverTheDefaultPage()
    {
        (Window window, StubPlatformView defaultView) = CreateRenderedWindow();
        StubPlatformView overrideView = new();
        Label element = Rendered(new Label(), overrideView);

        DialogCurrentPageSelection<StubPlatformView> selection =
            DialogCurrentPageLocator.Select<StubPlatformView>(() => element, window, Judge);

        Assert.Multiple(() =>
        {
            Assert.That(selection.PlatformView, Is.SameAs(overrideView));
            Assert.That(selection.PlatformView, Is.Not.SameAs(defaultView));
            Assert.That(selection.Reasons, Is.Empty);
        });
    }

    /// <summary>関数が登録されていなければ、既定の探し方の platform view が選ばれる。</summary>
    [Test]
    [Description("未登録なら既定の探し方のページ")]
    public void DefaultPageIsUsedWithoutRegistration()
    {
        (Window window, StubPlatformView defaultView) = CreateRenderedWindow();

        DialogCurrentPageSelection<StubPlatformView> selection =
            DialogCurrentPageLocator.Select<StubPlatformView>(null, window, Judge);

        Assert.That(selection.PlatformView, Is.SameAs(defaultView));
    }

    /// <summary>登録した関数が要素を返さない・失敗する・描画前の要素を返す・別の画面の要素を返すときは、既定の探し方へ進む。</summary>
    /// <param name="kind">登録した関数の振る舞い。</param>
    /// <param name="expectedReason">見つからなかった理由として残る文の一部。</param>
    [TestCase("null", "returned null")]
    [TestCase("throw", "threw an exception (InvalidOperationException: broken provider)")]
    [TestCase("unrendered", "The element from the registered current page provider has no platform view yet.")]
    [TestCase("elsewhere", "The platform view from the registered current page provider is not in the window presenting the dialog.")]
    [Description("登録した関数から得られなければ既定の探し方へ進む")]
    public void RegisteredProviderFallsBackToTheDefaultPage(string kind, string expectedReason)
    {
        (Window window, StubPlatformView defaultView) = CreateRenderedWindow();
        Func<VisualElement?> provider = kind switch
        {
            "null" => () => null,
            "throw" => () => throw new InvalidOperationException("broken provider"),
            "unrendered" => () => new Label(),
            _ => () => Rendered(new Label(), new StubPlatformView { Rejection = DialogCurrentPageRejection.NotInPresentingWindow }),
        };

        DialogCurrentPageSelection<StubPlatformView> selection =
            DialogCurrentPageLocator.Select<StubPlatformView>(provider, window, Judge);

        Assert.Multiple(() =>
        {
            Assert.That(selection.PlatformView, Is.SameAs(defaultView));
            Assert.That(selection.Reasons, Has.Count.EqualTo(1));
            Assert.That(selection.Reasons[0], Does.Contain(expectedReason));
        });
    }

    /// <summary>
    /// 登録した関数の要素が描画済みでも、Native 実装がページとして受け付けない platform view
    /// (矩形が空・KsDialogs 自身の器に載っている・提示先のウィンドウの外) なら、既定の探し方へ進む。
    /// </summary>
    /// <remarks>
    /// ここで通すと Native 実装がそれを外し、既定の探し方を経ずに可視領域へ落ちる。
    /// </remarks>
    /// <param name="rejectionName">登録した関数の要素の platform view が受け付けられない理由の名前 (内部の型は公開のテストメソッドの引数にできないため名前で渡す)。</param>
    /// <param name="expectedReason">見つからなかった理由として残る文。</param>
    [TestCase(nameof(DialogCurrentPageRejection.EmptyArea), "The platform view from the registered current page provider has an empty area.")]
    [TestCase(nameof(DialogCurrentPageRejection.EmptySafeArea), "The safe area of the platform view from the registered current page provider is empty.")]
    [TestCase(nameof(DialogCurrentPageRejection.InContainerWindow), "The platform view from the registered current page provider is in a window of a KsDialogs container.")]
    [TestCase(nameof(DialogCurrentPageRejection.OutsidePresentingWindow), "The platform view from the registered current page provider lies outside the window presenting the dialog.")]
    [Description("Native 実装が受け付けない要素を返したら既定の探し方へ進む")]
    public void RejectedRegisteredElementFallsBackToTheDefaultPage(string rejectionName, string expectedReason)
    {
        DialogCurrentPageRejection rejection = Enum.Parse<DialogCurrentPageRejection>(rejectionName);
        (Window window, StubPlatformView defaultView) = CreateRenderedWindow();
        Label element = Rendered(new Label(), new StubPlatformView { Rejection = rejection });

        DialogCurrentPageSelection<StubPlatformView> selection =
            DialogCurrentPageLocator.Select<StubPlatformView>(() => element, window, Judge);

        Assert.Multiple(() =>
        {
            Assert.That(selection.PlatformView, Is.SameAs(defaultView));
            Assert.That(selection.Reasons, Is.EqualTo(new[] { expectedReason }));
        });
    }

    /// <summary>既定の探し方のページも受け付けられなければ、ページは決まらない (未解決)。</summary>
    [Test]
    [Description("既定の探し方のページが空なら未解決")]
    public void RejectedDefaultPageIsUnresolved()
    {
        ContentPage top = new();
        Window window = new(new NavigationPage(top));
        Rendered(top, new StubPlatformView { Rejection = DialogCurrentPageRejection.EmptyArea });

        DialogCurrentPageSelection<StubPlatformView> selection =
            DialogCurrentPageLocator.Select<StubPlatformView>(null, window, Judge);

        Assert.Multiple(() =>
        {
            Assert.That(selection.PlatformView, Is.Null);
            Assert.That(
                selection.Reasons,
                Is.EqualTo(new[] { "The platform view from the MAUI page hierarchy has an empty area." }));
        });
    }

    /// <summary>表示中のページがまだ描画されていなければ、ページは決まらない (未解決)。</summary>
    [Test]
    [Description("描画前のページは未解決")]
    public void UnrenderedDefaultPageIsUnresolved()
    {
        Window window = new(new NavigationPage(new ContentPage()));

        DialogCurrentPageSelection<StubPlatformView> selection =
            DialogCurrentPageLocator.Select<StubPlatformView>(null, window, Judge);

        Assert.Multiple(() =>
        {
            Assert.That(selection.PlatformView, Is.Null);
            Assert.That(
                selection.Reasons,
                Is.EqualTo(new[] { "The element from the MAUI page hierarchy has no platform view yet." }));
        });
    }

    /// <summary>提示先の画面が分からなければ、ページは決まらない (未解決)。</summary>
    [Test]
    [Description("提示先の画面が無ければ未解決")]
    public void MissingWindowIsUnresolved()
    {
        DialogCurrentPageSelection<StubPlatformView> selection =
            DialogCurrentPageLocator.Select<StubPlatformView>(() => null, null, Judge);

        Assert.Multiple(() =>
        {
            Assert.That(selection.PlatformView, Is.Null);
            Assert.That(selection.Reasons, Is.EqualTo(new[]
            {
                "The registered current page provider returned null.",
                "The window presenting the dialog could not be matched to a MAUI window.",
            }));
        });
    }

    /// <summary>未解決の診断ログは、Native 実装と同じ書き出しに辿った順の理由を続けた英文になる。</summary>
    [Test]
    [Description("未解決の診断ログは書き出しと理由を並べた英文")]
    public void DiagnosticMessageStartsWithTheUnresolvedLead()
    {
        string message = DialogCurrentPageLocator.DiagnosticMessage(
            DialogCurrentPageLocator.UnresolvedLead,
            ["First reason.", "Second reason."]);

        Assert.That(
            message,
            Is.EqualTo("The current page could not be resolved, so the visible area is used instead. First reason. Second reason."));
    }

    /// <summary>登録口は代入した関数を返し、<see langword="null"/> の代入で既定の探し方に戻る。</summary>
    [Test]
    [Description("登録口は代入と解除ができる")]
    public void ProviderCanBeRegisteredAndCleared()
    {
        Func<VisualElement?> provider = () => null;
        try
        {
            DialogCurrentPage.Provider = provider;
            // 関数を Assert.That へ直接渡すと検査対象のコードとして呼ばれてしまうため、値として比べる
            Assert.That((object?)DialogCurrentPage.Provider, Is.SameAs(provider));

            DialogCurrentPage.Provider = null;
            Assert.That((object?)DialogCurrentPage.Provider, Is.Null);
        }
        finally
        {
            DialogCurrentPage.Provider = null;
        }
    }

    /// <summary>TabbedPage の中の NavigationPage の先端が描画済みの画面を作る。</summary>
    private static (Window Window, StubPlatformView DefaultView) CreateRenderedWindow()
    {
        StubPlatformView defaultView = new();
        ContentPage top = new();
        TabbedPage tabbed = new() { Children = { new NavigationPage(top) } };
        Window window = new(tabbed);
        // ページ遷移の通知はハンドラに文脈を求めるため、構成を組み終えてから描画済みにする
        Rendered(top, defaultView);
        return (window, defaultView);
    }

    /// <summary>要素に platform view を持たせて、描画済みの状態にする。</summary>
    private static T Rendered<T>(T element, StubPlatformView platformView)
        where T : VisualElement
    {
        element.Handler = new StubPlatformViewHandler(platformView);
        return element;
    }

    private static DialogCurrentPageRejection Judge(StubPlatformView view) => view.Rejection;
}
