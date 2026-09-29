using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using KsDialogs.Bridge;
using Microsoft.Maui;
using Microsoft.Maui.ApplicationModel;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Graphics;
using Microsoft.Maui.Platform;
using Activity = Android.App.Activity;
using Context = Android.Content.Context;

namespace KsDialogs;

/// <summary>
/// MAUI の中身を互換面が受け取る形へ写す、器によらない共通部品。
/// </summary>
/// <remarks>
/// 提示先の文脈の解決・platform view 化と測定・メタ属性と演出の写しは、ダイアログの器でも
/// ローディングの器でも同じである。二重実装を避けるため、どちらの委譲面もここを通る
/// (core/ADR-0022 の「共有部品を両方の器から使う」の MAUI 側)。
/// </remarks>
internal static class PlatformDialogContent
{
    /// <summary>
    /// 提示先の画面の文脈を先に解決してから中身を作り、互換面が受け取る形へ写す。
    /// </summary>
    /// <remarks>
    /// 文脈が取れなければ、中身を作る処理 (利用者の View factory) を走らせずに失敗する。
    /// Native が提示先を確保した後の中身の供給で、UI スレッドから呼ぶ。
    /// </remarks>
    /// <param name="createContent">中身の MAUI View とメタ属性の実効値を作る処理。</param>
    /// <returns>互換面へ渡す中身と属性の組。</returns>
    /// <exception cref="InvalidOperationException">提示先の画面の文脈が取れない。</exception>
    public static MauiDialogContent CreateInPresentationContext(Func<DialogPresentationContent> createContent)
    {
        IMauiContext mauiContext = RequirePresentationContext();
        return Create(createContent(), mauiContext);
    }

    /// <summary>中身とメタ属性を、互換面が受け取る形へ写す。</summary>
    /// <remarks>値の丸めや位置の計算は Native 実装の責務なので、ここでは表現を変えるだけにする。</remarks>
    /// <param name="content">中身の MAUI View とメタ属性の実効値。</param>
    /// <param name="mauiContext">platform view 化に使う文脈。</param>
    /// <returns>互換面へ渡す中身と属性の組。</returns>
    private static MauiDialogContent Create(DialogPresentationContent content, IMauiContext mauiContext)
    {
        Android.Views.View platformView = content.ContentView.ToPlatform(mauiContext);
        DialogContentView contentView = new(content, platformView);
        MauiDialogContent bridgeContent = new(
            contentView,
            ToBridgeOptions(content.Options),
            ToBridgePlacement(content.Placement));
        // 添付面への写しと演出の結び付けはこの組を通すため、レイアウトの節目で使えるよう渡しておく
        contentView.BindToBridgeContent(bridgeContent);
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
        DialogLayoutArea.CurrentPage => MauiDialogLayoutArea.CurrentPage!,
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
    /// 中身の供給の時点で、Native の提示先に対応する画面の文脈を取り出す。
    /// </summary>
    /// <remarks>Native が提示先を確保した後に、UI スレッドで呼ぶ。</remarks>
    /// <returns>提示先の画面の文脈。</returns>
    /// <exception cref="InvalidOperationException">提示先の画面の文脈が取れない。</exception>
    public static IMauiContext RequirePresentationContext() =>
        DialogPresentationContext.Require(ResolvePresentationTarget());

    /// <summary>
    /// Native 実装が提示先に選ぶ画面と、platform view 化に使うその画面の文脈を一組で解決する。
    /// </summary>
    /// <remarks>
    /// Native 実装は現在表に出ている Activity を提示先に選ぶため、その Activity を platform view として
    /// 持つ画面を選ぶ。見つからない場合だけ、文脈を持つ最初の画面へ落とす。
    /// アプリ全体の文脈はテーマを持たず、これで作った View は組み立ての時点で失敗するため使わない。
    /// 文脈を持つ画面が 1 つも無ければ解決できない。
    /// </remarks>
    /// <returns>解決できた画面と文脈の組。無ければ <see langword="null"/>。</returns>
    public static DialogPresentationTarget? ResolvePresentationTarget()
    {
        IReadOnlyList<Window> windows = Application.Current?.Windows ?? [];
        Activity? presentationHost = ActivityStateManager.Default.GetCurrentActivity();

        Window? hostWindow = windows.FirstOrDefault(window =>
            window.Handler?.MauiContext is not null
            && ReferenceEquals(window.Handler.PlatformView, presentationHost));
        hostWindow ??= windows.FirstOrDefault(window => window.Handler?.MauiContext is not null);

        return hostWindow?.Handler?.MauiContext is IMauiContext context
            ? new DialogPresentationTarget(hostWindow, context)
            : null;
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

    /// <summary>
    /// 中身の MAUI View を器のレイアウトに乗せるための入れ物。
    /// </summary>
    /// <remarks>
    /// MAUI の View は自前の測定・配置で寸法が決まる。platform view をそのまま器へ渡すと器が Android の測り方で
    /// 直接測ることになり、ルートが自分で宣言した大きさ (WidthRequest など) が効かない
    /// (ContentView 系のルートは中の要素の大きさまで縮む)。ルート自身を配置する親もいなくなり、
    /// ルートの Width / Height が決まらない。この入れ物が MAUI の測り方で測った結果を寸法として器へ伝え、
    /// 器が決めた領域へ MAUI の配置を流し込む。
    /// <para>
    /// 器が中身として受け取るのはこの入れ物なので、添付面への写しと初回レイアウトパスの節目の監視もここで行う。
    /// タップは受け止めず、そのまま中身へ流す (Toast の器のタッチの素通しを妨げない)。
    /// </para>
    /// </remarks>
    private sealed class DialogContentView : Android.Views.ViewGroup
    {
        private readonly View _contentView;
        private readonly DialogAttributeSnapshotRelay _attributeRelay;
        private MauiDialogContent? _bridgeContent;

        /// <param name="content">提示する中身と、その提示に効くメタ属性。</param>
        /// <param name="platformView">その View の platform view。</param>
        public DialogContentView(DialogPresentationContent content, Android.Views.View platformView)
            : base(platformView.Context)
        {
            _contentView = content.ContentView;
            _attributeRelay = new DialogAttributeSnapshotRelay(content, ApplyAttributes);
            // 出入りの演出で中身がこの入れ物の外へ動く (滑り出し等) ため、中身を切り取らない
            SetClipChildren(false);
            AddView(platformView);
        }

        /// <summary>添付面への写しを行う互換面の組を結び付ける。</summary>
        /// <param name="bridgeContent">この View を中身として持つ組。</param>
        public void BindToBridgeContent(MauiDialogContent bridgeContent) => _bridgeContent = bridgeContent;

        protected override void OnAttachedToWindow()
        {
            base.OnAttachedToWindow();
            // 器は画面に載ったあとのパスの計算で添付面を読むため、計算に入る前にその時点の値を渡しておく
            _attributeRelay.TransferBeforeLayout();
        }

        protected override void OnMeasure(int widthMeasureSpec, int heightMeasureSpec)
        {
            Context context = Context!;
            // 測定は MAUI の配置と同じ面 (IView) で行う。制約の無い軸 (UNSPECIFIED) は無限大として渡る
            Size measured = ((IView)_contentView).Measure(
                widthMeasureSpec.ToDouble(context),
                heightMeasureSpec.ToDouble(context));
            // 器が大きさを決めた軸 (EXACTLY) はその大きさに、上限つきの軸 (AT_MOST) は上限までに収める
            SetMeasuredDimension(
                ResolveSize((int)context.ToPixels(measured.Width), widthMeasureSpec),
                ResolveSize((int)context.ToPixels(measured.Height), heightMeasureSpec));
        }

        protected override void OnLayout(bool changed, int l, int t, int r, int b)
        {
            Context context = Context!;
            ((IView)_contentView).Arrange(new Rect(0, 0, context.FromPixels(r - l), context.FromPixels(b - t)));
            // 採用時点は器を画面に載せたあとの初回パスの完了時点 (core/ADR-0015)。
            // 中身の配置まで終えたこの時点で固定し、パスの中で届いた変更まで渡し直す
            if (IsAttachedToWindow)
            {
                _attributeRelay.FreezeAndTransfer();
            }
        }

        /// <summary>MAUI 側で合成した実効値を、器が読む添付面へ写す。</summary>
        /// <param name="attributes">その時点のメタ属性の実効値。</param>
        private void ApplyAttributes(DialogAttributes attributes)
        {
            if (_bridgeContent is null)
            {
                return;
            }

            MauiDialogContent.ApplyAttributes(
                this,
                ToBridgeOptions(attributes.Options),
                ToBridgePlacement(attributes.Placement));
            InstallTransition(_bridgeContent, _contentView, attributes.Transition);
        }
    }
}
