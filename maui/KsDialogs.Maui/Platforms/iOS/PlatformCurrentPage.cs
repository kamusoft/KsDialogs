using System;
using System.Diagnostics;
using CoreGraphics;
using KsDialogs.Bridge;
using Microsoft.Maui.Controls;
using UIKit;

namespace KsDialogs;

/// <summary>
/// 基準領域「表示中のページ」のページを、MAUI のページ構成から Native 実装へ教える面 (iOS)。
/// </summary>
/// <remarks>
/// Native 実装は登録された関数が返す View の safe area の内側を基準にする。ここでは MAUI 側で決めたページの
/// platform view をその関数として登録し、Native 実装が内蔵する view controller 階層の探し方より優先させる。
/// </remarks>
internal static class PlatformCurrentPage
{
    /// <summary>ページも可視領域を覆う View も見つからなかったときの診断ログの書き出し。</summary>
    /// <remarks>このとき Native 実装は内蔵の探し方へ進むため、可視領域になるとは言えない。</remarks>
    private const string NoVisibleAreaViewLead =
        "The current page could not be resolved, and no view in the window presenting the dialog covers the visible area, so the view controller hierarchy is used instead.";

    /// <summary>可視領域を覆うかの判定で許す端の揺れ (pt)。</summary>
    private const double CoverTolerance = 0.01;

    // 読み書きは UI スレッドだけで行う
    private static bool s_installed;

    /// <summary>まだ登録していなければ、現在の利用者の関数で登録する。</summary>
    /// <remarks>Native 実装の登録口は UI スレッドでしか触れないため、UI スレッドで行う。</remarks>
    public static void EnsureInstalled() => PlatformDialogContent.RunOnUiThread(() =>
    {
        if (!s_installed)
        {
            Install(DialogCurrentPage.Provider);
        }
    });

    /// <summary>利用者の関数を捕まえた関数を Native 実装へ登録する。UI スレッドから呼ぶ。</summary>
    /// <param name="provider">利用者が登録した関数。未登録なら <see langword="null"/>。</param>
    public static void Install(Func<VisualElement?>? provider)
    {
        s_installed = true;
        MauiDialogCurrentPage.SetProvider(() => ResolvePageView(provider));
    }

    /// <summary>表示中のページの platform view を決める。</summary>
    /// <remarks>
    /// MAUI 側でページが決まらないときは、Native 実装の内蔵の探し方 (view controller 階層の走査) へは進ませず、
    /// safe area の内側が可視領域を覆う View を返して基準を可視領域と同じにする (core/ADR-0038)。
    /// Native 実装はページの矩形と可視領域の狭い方を基準にするため、可視領域を覆う View なら結果は可視領域と一致する。
    /// Native 実装はこの場合に診断を出さないため、見つからなかった理由はここで出す。
    /// </remarks>
    /// <param name="provider">利用者が登録した関数。未登録なら <see langword="null"/>。</param>
    /// <returns>ページの platform view。決まらなければ可視領域を覆う View (それも無ければ <see langword="null"/>)。</returns>
    private static UIView? ResolvePageView(Func<VisualElement?>? provider)
    {
        DialogPresentationTarget? target = PlatformDialogContent.ResolvePresentationTarget();
        UIWindow? hostWindow = target?.Window.Handler?.PlatformView as UIWindow;

        DialogCurrentPageSelection<UIView> selection = DialogCurrentPageLocator.Select<UIView>(
            provider,
            target?.Window,
            view => Judge(view, hostWindow));
        if (selection.PlatformView is UIView pageView)
        {
            return pageView;
        }

        UIView? visibleAreaView = FindVisibleAreaView(PlatformDialogContent.FindPresentationHost() ?? hostWindow);
        Trace.TraceWarning(DialogCurrentPageLocator.DiagnosticMessage(
            visibleAreaView is null ? NoVisibleAreaViewLead : DialogCurrentPageLocator.UnresolvedLead,
            selection.Reasons));
        return visibleAreaView;
    }

    /// <summary>提示先の window に載った View のうち、Native 実装が受け付け、safe area の内側が可視領域を覆うものを探す。</summary>
    /// <remarks>
    /// 候補は root から present の連なりを辿った各 view controller の view (root に近い順)。どれも window 全体を占め、
    /// 利用者が safe area を足していなければ safe area の内側は window の safe area の内側 (可視領域) と一致する
    /// (MAUI の TabbedPage・Shell・FlyoutPage・ContentPage の root と、全画面のモーダルの入れ物で一致する)。
    /// 全画面のモーダルの下になった view は window から外れ、root の view controller に safe area が足されていれば
    /// 可視領域を覆わないため、どちらも次の候補へ進む。連なりの先には表示中の KsDialogs の器も含まれ、
    /// Native 実装はその view の safe area を可視領域にしているため、器まで進めばそこで見つかる。
    /// window そのものは返さない。<see cref="UIView.Window"/> が window 自身を指さないため、Native 実装が
    /// 提示先の window に載っていないとして外し、内蔵の探し方へ進んでしまう。
    /// </remarks>
    /// <param name="hostWindow">ダイアログを出す window。解決できなければ <see langword="null"/>。</param>
    /// <returns>可視領域を覆う View。無ければ <see langword="null"/>。</returns>
    private static UIView? FindVisibleAreaView(UIWindow? hostWindow)
    {
        if (hostWindow is null)
        {
            return null;
        }

        CGRect visibleArea = hostWindow.SafeAreaInsets.InsetRect(hostWindow.Bounds);
        for (UIViewController? controller = hostWindow.RootViewController;
            controller is not null;
            controller = controller.PresentedViewController)
        {
            if (!controller.IsViewLoaded || controller.View is not UIView view
                || Judge(view, hostWindow) != DialogCurrentPageRejection.None)
            {
                continue;
            }

            CGRect rect = view.ConvertRectToView(view.SafeAreaLayoutGuide.LayoutFrame, hostWindow);
            if (Covers(rect, visibleArea))
            {
                return view;
            }
        }

        return null;
    }

    /// <summary><paramref name="outer"/> が <paramref name="inner"/> を覆うか。座標の丸めによる端の揺れは許す。</summary>
    private static bool Covers(CGRect outer, CGRect inner) =>
        outer.Left <= inner.Left + CoverTolerance
        && outer.Top <= inner.Top + CoverTolerance
        && outer.Right >= inner.Right - CoverTolerance
        && outer.Bottom >= inner.Bottom - CoverTolerance;

    /// <summary>platform view を Native 実装がページとして受け付けるかを、Native 実装と同じ条件・同じ順で確かめる。</summary>
    /// <remarks>
    /// Native 実装はページの View の safe area の内側を提示先の window の座標に直して基準にし、それが空 (非有限を含む) の
    /// ものと window と重ならないものを外す。
    /// </remarks>
    /// <param name="view">確かめる platform view。</param>
    /// <param name="hostWindow">ダイアログを出す window。解決できなければ <see langword="null"/>。</param>
    /// <returns>受け付けない理由。受け付けるなら <see cref="DialogCurrentPageRejection.None"/>。</returns>
    private static DialogCurrentPageRejection Judge(UIView view, UIWindow? hostWindow)
    {
        if (hostWindow is null || !ReferenceEquals(view.Window, hostWindow))
        {
            return DialogCurrentPageRejection.NotInPresentingWindow;
        }

        CGRect rect = view.ConvertRectToView(view.SafeAreaLayoutGuide.LayoutFrame, hostWindow);
        bool finite = double.IsFinite(rect.X) && double.IsFinite(rect.Y)
            && double.IsFinite(rect.Width) && double.IsFinite(rect.Height);
        if (!finite || rect.Width <= 0 || rect.Height <= 0)
        {
            return DialogCurrentPageRejection.EmptySafeArea;
        }

        return rect.IntersectsWith(hostWindow.Bounds)
            ? DialogCurrentPageRejection.None
            : DialogCurrentPageRejection.OutsidePresentingWindow;
    }
}
