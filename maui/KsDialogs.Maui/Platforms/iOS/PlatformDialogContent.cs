using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using CoreGraphics;
using Foundation;
using KsDialogs.Bridge;
using Microsoft.Maui;
using Microsoft.Maui.ApplicationModel;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Graphics;
using Microsoft.Maui.Platform;
using UIKit;

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
    /// <summary>中身とメタ属性を、互換面が受け取る形へ写す。</summary>
    /// <remarks>値の丸めや位置の計算は Native 実装の責務なので、ここでは表現を変えるだけにする。</remarks>
    /// <param name="content">中身の MAUI View とメタ属性の実効値。</param>
    /// <param name="mauiContext">platform view 化に使う文脈。</param>
    /// <returns>互換面へ渡す中身と属性の組。</returns>
    public static MauiDialogContent Create(DialogPresentationContent content, IMauiContext mauiContext)
    {
        DialogContentView contentView = new(content, content.ContentView.ToPlatform(mauiContext));
        MauiDialogContent bridgeContent = new(
            contentView,
            ToBridgeOptions(content.Options),
            ToBridgePlacement(content.Placement));
        // 添付面への写しは互換面の側が持つため、レイアウトの節目で呼べるよう組を渡しておく
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
        DialogAlignment.Start => MauiDialogAlignment.Start,
        DialogAlignment.End => MauiDialogAlignment.End,
        DialogAlignment.Fill => MauiDialogAlignment.Fill,
        _ => MauiDialogAlignment.Center,
    };

    /// <summary>基準領域を互換面の表現へ写す。</summary>
    /// <param name="area">MAUI 側の基準領域。</param>
    /// <returns>互換面の基準領域。</returns>
    private static MauiDialogLayoutArea ToBridgeLayoutArea(DialogLayoutArea area) => area switch
    {
        DialogLayoutArea.Window => MauiDialogLayoutArea.Window,
        _ => MauiDialogLayoutArea.VisibleArea,
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
    /// Native 実装は前面のシーンの key window から提示先を辿るため、その window を platform view として
    /// 持つ画面の文脈を使う。見つからない場合だけ、文脈を持つ最初の画面へ落とす。
    /// アプリ全体の文脈は画面に紐づく情報を持たず、これで作った View は提示に耐えないため使わない。
    /// 画面の文脈が取れない状態は提示先が無い状態にあたる。
    /// </remarks>
    /// <returns>解決できた文脈。無ければ <see langword="null"/>。</returns>
    public static IMauiContext? ResolveMauiContext()
    {
        IReadOnlyList<Window> windows = Application.Current?.Windows ?? [];
        UIWindow? presentationHost = UIApplication.SharedApplication.ConnectedScenes
            .OfType<UIWindowScene>()
            .Where(scene => scene.ActivationState == UISceneActivationState.ForegroundActive)
            .SelectMany(scene => scene.Windows)
            .FirstOrDefault(window => window.IsKeyWindow);

        Window? hostWindow = windows
            .FirstOrDefault(window => ReferenceEquals(window.Handler?.PlatformView, presentationHost));

        return hostWindow?.Handler?.MauiContext
            ?? windows.Select(window => window.Handler?.MauiContext).FirstOrDefault(context => context is not null);
    }

    /// <summary>
    /// 中身の MAUI View を器のレイアウトに乗せるための入れ物。
    /// </summary>
    /// <remarks>
    /// MAUI の View は自前の測定・配置で寸法が決まるため、そのまま器へ渡すと
    /// 寸法を伝える手立てが無く大きさ 0 のままになる。この入れ物が測定結果を寸法として器へ伝え、
    /// 器が決めた領域へ MAUI の配置を流し込む。
    /// </remarks>
    /// <param name="content">提示する中身と、その提示に効くメタ属性。</param>
    /// <param name="platformView">その View の platform view。</param>
    private sealed class DialogContentView : UIView
    {
        private readonly View _contentView;
        private readonly DialogAttributeSnapshotRelay _attributeRelay;
        private MauiDialogContent? _bridgeContent;
        // 演出の実行口は互換面が保持するが、その実体は managed 側にある。
        // この View はダイアログの寿命の間ずっと器に持たれるため、ここに置くと寿命が揃う
        private MauiDialogTransitionRunner? _presentationRunner;
        private MauiDialogTransitionRunner? _dismissalRunner;

        public DialogContentView(DialogPresentationContent content, UIView platformView)
        {
            _contentView = content.ContentView;
            _attributeRelay = new DialogAttributeSnapshotRelay(content, ApplyAttributes);
            // 中身の位置と大きさは MAUI の配置が決めるため、器のレイアウトの管理下に置かない
            platformView.TranslatesAutoresizingMaskIntoConstraints = true;
            AddSubview(platformView);
        }

        /// <summary>添付面への写しを行う互換面の組を結び付ける。</summary>
        /// <param name="bridgeContent">この View を中身として持つ組。</param>
        public void BindToBridgeContent(MauiDialogContent bridgeContent) => _bridgeContent = bridgeContent;

        /// <summary>制約が無いときの寸法。器はこれを中身の大きさとして扱う。</summary>
        public override CGSize IntrinsicContentSize => Measure(double.PositiveInfinity, double.PositiveInfinity);

        /// <summary>与えられた領域に収まる寸法。</summary>
        public override CGSize SizeThatFits(CGSize size) => Measure(size.Width, size.Height);

        public override void LayoutSubviews()
        {
            // 器はこのパスの計算で添付面を読むため、計算に入る前にその時点の値を渡しておく
            _attributeRelay.TransferBeforeLayout();
            base.LayoutSubviews();
            ((IView)_contentView).Arrange(new Rect(0, 0, Bounds.Width, Bounds.Height));
            // 採用時点は器を画面に載せたあとの初回パスの完了時点 (core/ADR-0015)。
            // 載せる前の暫定のパスでは固定せず、載ってからのパスの完了で固定して渡し直す
            if (Window is not null)
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

            _bridgeContent.ApplyAttributes(
                ToBridgeOptions(attributes.Options),
                ToBridgePlacement(attributes.Placement));
            InstallTransition(attributes.Transition);
        }

        /// <summary>添付された演出を、互換面の実行口として器が読む添付面へ結び付ける。</summary>
        /// <param name="transition">その時点の演出。未添付なら <see langword="null"/>。</param>
        private void InstallTransition(DialogTransition? transition)
        {
            _presentationRunner = ToRunner(transition?.Presentation);
            _dismissalRunner = ToRunner(transition?.Dismissal);
            _bridgeContent?.InstallTransition(
                _presentationRunner,
                _dismissalRunner,
                ToBridgeOverlayDuration(transition?.OverlayDuration));
        }

        /// <summary>覆いのフェード時間を、互換面が受け取る秒へ写す。</summary>
        /// <param name="overlayDuration">添付された覆いのフェード時間。未指定なら <see langword="null"/>。</param>
        /// <returns>互換面へ渡す秒。未指定なら <see langword="null"/> (器の既定値が使われる)。</returns>
        private static NSNumber? ToBridgeOverlayDuration(TimeSpan? overlayDuration) =>
            DialogTransition.ToBridgeOverlayMilliseconds(overlayDuration) is double milliseconds
                ? NSNumber.FromDouble(milliseconds / 1000d)
                : null;

        /// <summary>演出フックを、完了コールバック型の実行口へ写す。</summary>
        /// <remarks>フックが受け取るのは中身の MAUI View なので、器が渡すホスト View は使わない。</remarks>
        /// <param name="hook">利用者が添付した演出。未添付なら <see langword="null"/>。</param>
        /// <returns>互換面へ渡す実行口。未添付なら <see langword="null"/>。</returns>
        private MauiDialogTransitionRunner? ToRunner(Func<VisualElement, Task>? hook)
        {
            DialogTransitionRunner? runner = DialogTransitionRunner.Create(hook, _contentView, RunOnUiThread);
            return runner is null ? null : (_, completion) => runner.Run(() => completion());
        }

        private CGSize Measure(double widthConstraint, double heightConstraint)
        {
            // 測定は MAUI の配置と同じ面 (IView) で行う。View の同名の旧 API とは戻り値が違う
            Size measured = ((IView)_contentView).Measure(widthConstraint, heightConstraint);
            return new CGSize((nfloat)measured.Width, (nfloat)measured.Height);
        }
    }
}
