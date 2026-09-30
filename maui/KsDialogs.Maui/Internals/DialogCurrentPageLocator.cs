using System;
using System.Collections.Generic;
using System.Linq;
using Microsoft.Maui.Controls;

namespace KsDialogs;

/// <summary>
/// 基準領域「表示中のページ」に使う platform view を、MAUI のページ構成から決める。
/// </summary>
/// <remarks>
/// 取得元は「利用者が登録した関数 > ページ構成を辿る既定の探し方」の順で、上位が要素を返さない・
/// 例外を投げる・要素がまだ描画されていない・Native 実装がページとして受け付けない platform view
/// (提示先のウィンドウに載っていない・矩形が空・提示先のウィンドウの外にある・KsDialogs 自身の器に載っている)
/// のときは次へ進む (core/ADR-0038)。受け付けない platform view をここで通すと、Native 実装がそれを外したときに
/// 既定の探し方を経ずに可視領域へ落ちるため、Native 実装と同じ条件をここでも確かめる。
/// どちらの取得元も両 OS で同じ辿り方をし、platform 固有なのは platform view を確かめる部分だけにする。
/// </remarks>
internal static class DialogCurrentPageLocator
{
    /// <summary>未解決のときの診断ログの書き出し。Native 実装の同じ状況の書き出しと同じ文にしてある。</summary>
    public const string UnresolvedLead = "The current page could not be resolved, so the visible area is used instead.";

    private const string RegisteredOrigin = "the registered current page provider";
    private const string DefaultOrigin = "the MAUI page hierarchy";

    /// <summary>容れ物のページを降りる回数の上限。容れ物どうしが循環していても止まるようにする。</summary>
    private const int MaxContainerDepth = 64;

    /// <summary>取得元を優先順に辿り、提示先に載っている platform view を決める。</summary>
    /// <typeparam name="TPlatformView">platform の View の型。</typeparam>
    /// <param name="provider">利用者が登録した関数。未登録なら <see langword="null"/>。</param>
    /// <param name="window">ダイアログを出す画面。解決できなければ <see langword="null"/>。</param>
    /// <param name="judge">platform view を Native 実装がページとして受け付けるかを確かめ、受け付けない理由を返す操作。</param>
    /// <returns>決まった platform view と、辿った取得元で見つからなかった理由。</returns>
    public static DialogCurrentPageSelection<TPlatformView> Select<TPlatformView>(
        Func<VisualElement?>? provider,
        Window? window,
        Func<TPlatformView, DialogCurrentPageRejection> judge)
        where TPlatformView : class
    {
        List<string> reasons = [];

        if (provider is not null
            && ToHostedPlatformView(InvokeProvider(provider, reasons), RegisteredOrigin, judge, reasons)
                is TPlatformView registered)
        {
            return new DialogCurrentPageSelection<TPlatformView>(registered, reasons);
        }

        if (window is null)
        {
            reasons.Add("The window presenting the dialog could not be matched to a MAUI window.");
            return new DialogCurrentPageSelection<TPlatformView>(null, reasons);
        }

        Page? page = FindDefaultPage(window);
        if (page is null)
        {
            reasons.Add("The MAUI page hierarchy has no current page.");
            return new DialogCurrentPageSelection<TPlatformView>(null, reasons);
        }

        TPlatformView? found = ToHostedPlatformView(page, DefaultOrigin, judge, reasons);
        return new DialogCurrentPageSelection<TPlatformView>(found, reasons);
    }

    /// <summary>画面の表示中のページを、ページ構成を辿って決める。</summary>
    /// <param name="window">ダイアログを出す画面。</param>
    /// <returns>表示中のページ。無ければ <see langword="null"/>。</returns>
    public static Page? FindDefaultPage(Window window) =>
        FindDefaultPage(window.Navigation.ModalStack, window.Page);

    /// <summary>
    /// モーダルの積み重ねと画面の根のページから、表示中のページを決める。
    /// </summary>
    /// <remarks>
    /// モーダルで出したページがあればその先頭 (積み重ねの最後)、無ければ根のページを起点にし、
    /// <see cref="Shell"/> → <see cref="Shell.CurrentPage"/>・<see cref="FlyoutPage"/> → <see cref="FlyoutPage.Detail"/>・
    /// <see cref="TabbedPage"/> → <see cref="MultiPage{T}.CurrentPage"/>・<see cref="NavigationPage"/> →
    /// <see cref="NavigationPage.CurrentPage"/> を、容れ物でなくなるまで降りる。
    /// </remarks>
    /// <param name="modalStack">モーダルで出したページの積み重ね (最後が先頭)。</param>
    /// <param name="rootPage">画面の根のページ。</param>
    /// <returns>表示中のページ。無ければ <see langword="null"/>。</returns>
    public static Page? FindDefaultPage(IReadOnlyList<Page> modalStack, Page? rootPage)
    {
        Page? current = modalStack.Count > 0 ? modalStack[^1] : rootPage;
        for (int depth = 0; current is not null && depth < MaxContainerDepth; depth++)
        {
            Page? child = current switch
            {
                Shell shell => shell.CurrentPage,
                FlyoutPage flyout => flyout.Detail,
                TabbedPage tabbed => tabbed.CurrentPage,
                NavigationPage navigation => navigation.CurrentPage,
                _ => current,
            };
            if (ReferenceEquals(child, current))
            {
                return current;
            }

            current = child;
        }

        return null;
    }

    /// <summary>未解決のときの診断ログの本文を組み立てる。</summary>
    /// <param name="lead">書き出しの文。</param>
    /// <param name="reasons">辿った順の、見つからなかった理由。</param>
    /// <returns>診断ログの本文。</returns>
    public static string DiagnosticMessage(string lead, IEnumerable<string> reasons) =>
        string.Join(" ", reasons.Prepend(lead));

    /// <summary>利用者の関数を呼ぶ。失敗しても表示を止めず、理由を残して次の取得元へ進む。</summary>
    private static VisualElement? InvokeProvider(Func<VisualElement?> provider, List<string> reasons)
    {
        VisualElement? element;
        try
        {
            element = provider();
        }
        catch (Exception error)
        {
            reasons.Add($"The registered current page provider threw an exception ({error.GetType().Name}: {error.Message}).");
            return null;
        }

        if (element is null)
        {
            reasons.Add("The registered current page provider returned null.");
        }

        return element;
    }

    /// <summary>要素の platform view を取り出し、Native 実装がページとして受け付けるものならそれを返す。</summary>
    private static TPlatformView? ToHostedPlatformView<TPlatformView>(
        VisualElement? element,
        string origin,
        Func<TPlatformView, DialogCurrentPageRejection> judge,
        List<string> reasons)
        where TPlatformView : class
    {
        if (element is null)
        {
            return null;
        }

        // 描画前の要素は platform view を持たない。矩形が決まらないため、見つからなかった扱いにする
        if (element.Handler?.PlatformView is not TPlatformView platformView)
        {
            reasons.Add($"The element from {origin} has no platform view yet.");
            return null;
        }

        string? reason = judge(platformView) switch
        {
            DialogCurrentPageRejection.None => null,
            DialogCurrentPageRejection.NotInPresentingWindow =>
                $"The platform view from {origin} is not in the window presenting the dialog.",
            DialogCurrentPageRejection.InContainerWindow =>
                $"The platform view from {origin} is in a window of a KsDialogs container.",
            DialogCurrentPageRejection.EmptyArea => $"The platform view from {origin} has an empty area.",
            DialogCurrentPageRejection.EmptySafeArea => $"The safe area of the platform view from {origin} is empty.",
            DialogCurrentPageRejection.OutsidePresentingWindow =>
                $"The platform view from {origin} lies outside the window presenting the dialog.",
            DialogCurrentPageRejection rejection =>
                throw new ArgumentOutOfRangeException(nameof(judge), rejection, "Unknown rejection."),
        };
        if (reason is not null)
        {
            reasons.Add(reason);
            return null;
        }

        return platformView;
    }
}

/// <summary>
/// platform view を Native 実装がページとして受け付けない理由。
/// </summary>
/// <remarks>
/// Native 実装が登録された関数の View を外す条件と同じ並びにしてある。どれかに当たる platform view を Native 実装へ渡すと、
/// Native 実装はそれを外して可視領域へ落ちる (MAUI 側の次の取得元を経ない) ため、MAUI 側で先に外す。
/// </remarks>
internal enum DialogCurrentPageRejection
{
    /// <summary>受け付ける。</summary>
    None,

    /// <summary>提示先のウィンドウ (Android は提示先の Activity の持つウィンドウ) に載っていない。</summary>
    NotInPresentingWindow,

    /// <summary>KsDialogs 自身の器 (Dialog / Loading / Toast) のウィンドウに載っている (Android)。</summary>
    InContainerWindow,

    /// <summary>矩形の幅か高さが 0 (Android)。</summary>
    EmptyArea,

    /// <summary>safe area の内側の幅か高さが 0 (iOS)。</summary>
    EmptySafeArea,

    /// <summary>矩形が提示先のウィンドウと重ならない。</summary>
    OutsidePresentingWindow,
}

/// <summary>表示中のページを決めた結果。</summary>
/// <typeparam name="TPlatformView">platform の View の型。</typeparam>
/// <param name="PlatformView">決まった platform view。見つからなければ <see langword="null"/>。</param>
/// <param name="Reasons">辿った取得元で見つからなかった理由 (辿った順)。</param>
internal sealed record DialogCurrentPageSelection<TPlatformView>(
    TPlatformView? PlatformView,
    IReadOnlyList<string> Reasons)
    where TPlatformView : class;
