using System;
using System.Diagnostics;
using KsDialogs.Bridge;
using Microsoft.Maui.Controls;
using Activity = Android.App.Activity;
using AView = Android.Views.View;
using IBinder = Android.OS.IBinder;
using WindowManagerLayoutParams = Android.Views.WindowManagerLayoutParams;

namespace KsDialogs;

/// <summary>
/// 基準領域「表示中のページ」のページを、MAUI のページ構成から Native 実装へ教える面 (Android)。
/// </summary>
/// <remarks>
/// Native 実装は登録された関数が返す View の矩形のうち可視領域と重なる部分を基準にする。
/// ここでは MAUI 側で決めたページの platform view をその関数として登録する。
/// </remarks>
internal static class PlatformCurrentPage
{
    /// <summary>MAUI 側でページが決まらなかったときの診断ログの書き出し。</summary>
    /// <remarks>
    /// 可視領域へ落とすこと自体は Native 実装が診断を出して行うため、ここでは MAUI 側で見つからなかった理由だけを出す。
    /// </remarks>
    private const string UnresolvedLead = "The current page could not be found in the MAUI page hierarchy.";

    // 読み書きは UI スレッドだけで行う
    private static bool s_installed;

    /// <summary>まだ登録していなければ、現在の利用者の関数で登録する。</summary>
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
        MauiDialogCurrentPage.SetProvider(new PageViewProvider(provider));
    }

    /// <summary>表示中のページの platform view を決める。</summary>
    /// <param name="provider">利用者が登録した関数。未登録なら <see langword="null"/>。</param>
    /// <returns>ページの platform view。決まらなければ <see langword="null"/> (Native 実装が可視領域へ落とす)。</returns>
    private static AView? ResolvePageView(Func<VisualElement?>? provider)
    {
        DialogPresentationTarget? target = PlatformDialogContent.ResolvePresentationTarget();
        AView? decorView = (target?.Window.Handler?.PlatformView as Activity)?.Window?.DecorView;

        DialogCurrentPageSelection<AView> selection = DialogCurrentPageLocator.Select<AView>(
            provider,
            target?.Window,
            view => decorView is null ? DialogCurrentPageRejection.NotInPresentingWindow : Judge(view, decorView));
        if (selection.PlatformView is AView pageView)
        {
            return pageView;
        }

        Trace.TraceWarning(DialogCurrentPageLocator.DiagnosticMessage(UnresolvedLead, selection.Reasons));
        return null;
    }

    /// <summary>platform view を Native 実装がページとして受け付けるかを、Native 実装と同じ条件・同じ順で確かめる。</summary>
    /// <remarks>
    /// 候補にするのは、ダイアログを出す画面 (Activity) のメインウィンドウと、同じ Activity に属するモーダル・ダイアログの
    /// ウィンドウに載ったもの。MAUI の Android 版はモーダルのページを Activity とは別のダイアログウィンドウに
    /// 載せるため、メインウィンドウに限るとモーダルのページが候補から外れる。同じ Activity に属するかは、
    /// ウィンドウの根が持つ配置情報の token (アプリ側のウィンドウは OS が付ける Activity の token を共有する) で判定する。
    /// KsDialogs 自身の器のウィンドウは同じ Activity に属していても候補にしない。器であることは、Native 実装が
    /// 器のウィンドウの根に付ける印 (リソース ID <c>ksdialogs_container_window</c> のタグ) をそのまま読んで判定する。
    /// 候補のウィンドウに載っていても、矩形が空のものと提示先のメインウィンドウと重ならないものは外す。
    /// </remarks>
    /// <param name="view">確かめる platform view。</param>
    /// <param name="decorView">ダイアログを出す画面のメインウィンドウの根。</param>
    /// <returns>受け付けない理由。受け付けるなら <see cref="DialogCurrentPageRejection.None"/>。</returns>
    private static DialogCurrentPageRejection Judge(AView view, AView decorView)
    {
        if (!view.IsAttachedToWindow || view.RootView is not AView root)
        {
            return DialogCurrentPageRejection.NotInPresentingWindow;
        }

        if (!ReferenceEquals(root, decorView))
        {
            if (IsContainerWindowRoot(root))
            {
                return DialogCurrentPageRejection.InContainerWindow;
            }

            IBinder? activityToken = (decorView.LayoutParameters as WindowManagerLayoutParams)?.Token;
            IBinder? token = (root.LayoutParameters as WindowManagerLayoutParams)?.Token;
            if (activityToken is null || token is null || !token.Equals(activityToken))
            {
                return DialogCurrentPageRejection.NotInPresentingWindow;
            }
        }

        ScreenRect rect = ScreenRect.Of(view);
        if (rect.IsEmpty)
        {
            return DialogCurrentPageRejection.EmptyArea;
        }

        return rect.Intersects(ScreenRect.Of(decorView))
            ? DialogCurrentPageRejection.None
            : DialogCurrentPageRejection.OutsidePresentingWindow;
    }

    /// <summary>ウィンドウの根が、KsDialogs 自身の器のウィンドウの印を持つか。</summary>
    private static bool IsContainerWindowRoot(AView root) =>
        root.GetTag(Resource.Id.ksdialogs_container_window) is Java.Lang.Boolean mark && mark.BooleanValue();

    /// <summary>画面座標 (px) の矩形。</summary>
    private readonly record struct ScreenRect(int Left, int Top, int Right, int Bottom)
    {
        /// <summary>幅か高さが 0 以下か。</summary>
        public bool IsEmpty => Right - Left <= 0 || Bottom - Top <= 0;

        /// <summary>View の矩形を画面座標で読む。</summary>
        public static ScreenRect Of(AView view)
        {
            int[] location = new int[2];
            view.GetLocationOnScreen(location);
            return new ScreenRect(location[0], location[1], location[0] + view.Width, location[1] + view.Height);
        }

        /// <summary>他方の矩形と面積を持って重なるか。</summary>
        public bool Intersects(ScreenRect other) =>
            Left < other.Right && other.Left < Right && Top < other.Bottom && other.Top < Bottom;
    }

    /// <summary>MAUI 側で決めたページを、互換面が呼ぶ関数として差し出す。</summary>
    /// <param name="provider">利用者が登録した関数。未登録なら <see langword="null"/>。</param>
    private sealed class PageViewProvider(Func<VisualElement?>? provider)
        : Java.Lang.Object, IMauiDialogCurrentPageProvider
    {
        public AView? CurrentPageView() => ResolvePageView(provider);
    }
}
