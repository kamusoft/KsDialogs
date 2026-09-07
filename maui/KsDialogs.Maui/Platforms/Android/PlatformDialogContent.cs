using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using KsDialogs.Bridge;
using Microsoft.Maui;
using Microsoft.Maui.ApplicationModel;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Platform;
using Activity = Android.App.Activity;

namespace KsDialogs;

/// <summary>
/// MAUI の中身を互換面が受け取る形へ写す、器によらない共通部品。
/// </summary>
/// <remarks>
/// 提示先の文脈の解決・platform view 化・メタ属性と演出の写しは、ダイアログの器でも
/// ローディングの器でも同じである。二重実装を避けるため、どちらの委譲面もここを通る
/// (core/ADR-0022 の「共有部品を両方の器から使う」の MAUI 側)。
/// </remarks>
internal static class PlatformDialogContent
{
    /// <summary>中身とメタ属性を、互換面が受け取る形へ写す。</summary>
    /// <remarks>値の丸めや位置の計算は Native 実装の責務なので、ここでは表現を変えるだけにする。</remarks>
    /// <param name="content">中身の MAUI View とメタ属性の実効値。</param>
    /// <param name="mauiContext">platform view 化に使う文脈。</param>
    /// <returns>互換面へ渡す中身と属性の組。</returns>
    public static MauiDialogContent Create(DialogPresentationContent content, IMauiContext mauiContext)
    {
        Android.Views.View platformView = content.ContentView.ToPlatform(mauiContext);
        MauiDialogContent bridgeContent = new(
            platformView,
            ToBridgeOptions(content.Options),
            ToBridgePlacement(content.Placement));
        ObserveFirstLayoutPass(platformView, content, bridgeContent);
        return bridgeContent;
    }

    /// <summary>静的メタ属性を、互換面が受け取る形へ写す。</summary>
    /// <param name="options">静的メタ属性の実効値。</param>
    /// <returns>互換面へ渡す静的メタ属性。</returns>
    public static MauiDialogOptions ToBridgeOptions(DialogOptions options) => new()
    {
        LayoutArea = ToBridgeLayoutArea(options.LayoutArea),
        MarginTop = options.DialogMargin.Top,
        MarginLeft = options.DialogMargin.Left,
        MarginBottom = options.DialogMargin.Bottom,
        MarginRight = options.DialogMargin.Right,
        ProportionalWidth = options.ProportionalWidth,
        ProportionalHeight = options.ProportionalHeight,
        OverlayColorArgb = options.OverlayColorArgb,
        IsCanceledOnTouchOutside = options.IsCanceledOnTouchOutside,
    };

    /// <summary>置き場所を、互換面が受け取る形へ写す。</summary>
    /// <param name="placement">置き場所の実効値。</param>
    /// <returns>互換面へ渡す置き場所。</returns>
    public static MauiDialogPlacement ToBridgePlacement(DialogPlacement placement) => new()
    {
        HorizontalAlignment = ToBridgeAlignment(placement.HorizontalAlignment),
        VerticalAlignment = ToBridgeAlignment(placement.VerticalAlignment),
        OffsetX = placement.OffsetX,
        OffsetY = placement.OffsetY,
    };

    /// <summary>配置を互換面の表現へ写す。</summary>
    /// <param name="alignment">MAUI 側の配置。</param>
    /// <returns>互換面の配置。</returns>
    private static MauiDialogAlignment ToBridgeAlignment(DialogAlignment alignment) => alignment switch
    {
        DialogAlignment.Start => MauiDialogAlignment.Start!,
        DialogAlignment.End => MauiDialogAlignment.End!,
        DialogAlignment.Fill => MauiDialogAlignment.Fill!,
        _ => MauiDialogAlignment.Center!,
    };

    /// <summary>基準領域を互換面の表現へ写す。</summary>
    /// <param name="area">MAUI 側の基準領域。</param>
    /// <returns>互換面の基準領域。</returns>
    private static MauiDialogLayoutArea ToBridgeLayoutArea(DialogLayoutArea area) => area switch
    {
        DialogLayoutArea.Window => MauiDialogLayoutArea.Window!,
        _ => MauiDialogLayoutArea.VisibleArea!,
    };

    /// <summary>渡された処理を UI スレッドで実行する。</summary>
    /// <remarks>
    /// 器は UI スレッドから実行口を呼ぶため、その場で実行できるときは待ちを挟まない。
    /// 演出の開始が次の周回まで遅れると、開始前の 1 フレームが最終位置で見えてしまう。
    /// </remarks>
    /// <param name="action">UI スレッドで実行する処理。</param>
    public static void RunOnUiThread(Action action)
    {
        if (MainThread.IsMainThread)
        {
            action();
            return;
        }

        MainThread.BeginInvokeOnMainThread(action);
    }

    /// <summary>
    /// platform view 化に使う文脈を、Native 実装が提示先に選ぶ画面から解決する。
    /// </summary>
    /// <remarks>
    /// Native 実装は現在表に出ている Activity を提示先に選ぶため、その Activity を platform view として
    /// 持つ画面の文脈を使う。見つからない場合だけ、文脈を持つ最初の画面へ落とす。
    /// アプリ全体の文脈はテーマを持たず、これで作った View は組み立ての時点で失敗するため使わない。
    /// 画面の文脈が取れない状態は提示先が無い状態にあたる。
    /// </remarks>
    /// <returns>解決できた文脈。無ければ <see langword="null"/>。</returns>
    public static IMauiContext? ResolveMauiContext()
    {
        IReadOnlyList<Window> windows = Application.Current?.Windows ?? [];
        Activity? presentationHost = ActivityStateManager.Default.GetCurrentActivity();

        Window? hostWindow = windows
            .FirstOrDefault(window => ReferenceEquals(window.Handler?.PlatformView, presentationHost));

        return hostWindow?.Handler?.MauiContext
            ?? windows.Select(window => window.Handler?.MauiContext).FirstOrDefault(context => context is not null);
    }

    /// <summary>初回のネイティブレイアウトパスの節目で、MAUI 側の添付を器が読む添付面へ写す。</summary>
    /// <remarks>
    /// 添付は「初回のネイティブレイアウトパス完了時点」の値が採用される (core/ADR-0015)。
    /// 画面に載る直前に暫定の値を渡し、配置まで終わったところで固定した値を渡し直すことで、
    /// そのパスの中で届いた変更まで器へ伝わる。以降の変更は反映しない。
    /// 出入りの演出も同じ節目で結び付ける (core/ADR-0017)。
    /// </remarks>
    /// <param name="platformView">器が中身として受け取る platform view。</param>
    /// <param name="content">提示する中身と、その提示に効くメタ属性。</param>
    /// <param name="bridgeContent">互換面へ渡す中身と属性の組。</param>
    private static void ObserveFirstLayoutPass(
        Android.Views.View platformView,
        DialogPresentationContent content,
        MauiDialogContent bridgeContent)
    {
        DialogAttributeSnapshotRelay relay = new(
            content,
            attributes =>
            {
                MauiDialogContent.ApplyAttributes(
                    platformView,
                    ToBridgeOptions(attributes.Options),
                    ToBridgePlacement(attributes.Placement));
                InstallTransition(bridgeContent, content.ContentView, attributes.Transition);
            });

        platformView.ViewAttachedToWindow += (_, _) => relay.TransferBeforeLayout();
        // 配置は View の layout の中で終わるため、この通知の時点でそのパスは完了している
        platformView.LayoutChange += (_, _) => relay.FreezeAndTransfer();
    }

    /// <summary>添付された演出を、互換面の実行口として器が読む添付面へ結び付ける。</summary>
    /// <param name="bridgeContent">互換面へ渡す中身と属性の組。</param>
    /// <param name="contentView">フックへ渡す中身の MAUI View。</param>
    /// <param name="transition">その時点の演出。未添付なら <see langword="null"/>。</param>
    private static void InstallTransition(
        MauiDialogContent bridgeContent,
        View contentView,
        DialogTransition? transition)
    {
        // 実行口は互換面が中身と同じ寿命で保持するため、こちらで持ち続ける必要はない
        bridgeContent.InstallTransition(
            ToRunner(transition?.Presentation, contentView),
            ToRunner(transition?.Dismissal, contentView),
            ToBridgeOverlayDuration(transition?.OverlayDuration));
    }

    /// <summary>覆いのフェード時間を、互換面が受け取るミリ秒へ写す。</summary>
    /// <param name="overlayDuration">添付された覆いのフェード時間。未指定なら <see langword="null"/>。</param>
    /// <returns>互換面へ渡すミリ秒。未指定なら <see langword="null"/> (器の既定値が使われる)。</returns>
    private static Java.Lang.Long? ToBridgeOverlayDuration(TimeSpan? overlayDuration) =>
        DialogTransition.ToBridgeOverlayMilliseconds(overlayDuration) is double milliseconds
            ? Java.Lang.Long.ValueOf((long)milliseconds)
            : null;

    /// <summary>演出フックを、完了コールバック型の実行口へ写す。</summary>
    /// <param name="hook">利用者が添付した演出。未添付なら <see langword="null"/>。</param>
    /// <param name="contentView">フックへ渡す中身の MAUI View。</param>
    /// <returns>互換面へ渡す実行口。未添付なら <see langword="null"/>。</returns>
    private static TransitionRunner? ToRunner(Func<VisualElement, Task>? hook, View contentView)
    {
        DialogTransitionRunner? runner = DialogTransitionRunner.Create(hook, contentView, RunOnUiThread);
        return runner is null ? null : new TransitionRunner(runner);
    }

    /// <summary>MAUI 側の演出を、互換面が呼ぶ実行口として差し出す。</summary>
    /// <remarks>フックが受け取るのは中身の MAUI View なので、器が渡すホスト View は使わない。</remarks>
    /// <param name="runner">演出を動かす面。</param>
    private sealed class TransitionRunner(DialogTransitionRunner runner)
        : Java.Lang.Object, IMauiDialogTransitionRunner
    {
        public void Run(Android.Views.View hostView, Java.Lang.IRunnable completion)
        {
            // 完了通知は演出が終わるまで持ち越すため、その間 Java 側の実体を掴んだままにする
            runner.Run(completion.Run);
        }
    }
}
