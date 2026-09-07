using System;
using System.Threading.Tasks;
using Microsoft.Maui;
using Microsoft.Maui.Controls;
#if ANDROID
using Microsoft.Maui.Platform;
#endif

namespace KsDialogs;

/// <summary>
/// ダイアログの出入りの演出 (core/ADR-0017)。
/// </summary>
/// <remarks>
/// 中身の定義へ添付 (<see cref="Dialog.SetTransition"/>) すると、器は出現時に <see cref="Presentation"/>、
/// 閉鎖時に <see cref="Dismissal"/> を呼び、その完了を待ってから次へ進む。フックはどちらも省略でき、
/// 省略した側には器の既定のクロスフェードが適用される。指定した側では既定は実行されない (置き換え)。
/// <para>
/// フックが受け取るのは中身の MAUI View で、UI スレッドで開始される。背景の覆いは器が別のレイヤとして
/// 常に扱うためフックの対象にならず、そのフェード時間は <see cref="OverlayDuration"/> に従う
/// (省略時は器の既定値)。
/// </para>
/// <para>
/// フックが有限時間で完了することは利用者の責務で、器はタイムアウトを設けない。フックの失敗
/// (例外・キャンセル) は演出の失敗として吸収され、ダイアログの結果には影響しない。
/// MAUI の show はキャンセル操作を受け取らないため、呼び出し元キャンセルによる打ち切りの経路はない。
/// </para>
/// <para>
/// クロージャは XAML に書けないため、添付は code-behind から行う。
/// </para>
/// </remarks>
public sealed class DialogTransition
{
    /// <summary>プリセットが引数を省略したときの時間。</summary>
    internal static readonly TimeSpan DefaultDuration = TimeSpan.FromMilliseconds(250d);

    /// <summary>ズームの開始・終了倍率。</summary>
    private const double ZoomScale = 0.8d;

    /// <summary>出入りの演出の組を作る。</summary>
    /// <param name="presentation">出現時の演出。<see langword="null"/> なら器の既定が使われる。</param>
    /// <param name="dismissal">閉鎖時の演出。<see langword="null"/> なら器の既定が使われる。</param>
    /// <param name="overlayDuration">背景の覆いのフェード時間。<see langword="null"/> なら器の既定値になる。</param>
    public DialogTransition(
        Func<VisualElement, Task>? presentation = null,
        Func<VisualElement, Task>? dismissal = null,
        TimeSpan? overlayDuration = null)
    {
        Presentation = presentation;
        Dismissal = dismissal;
        OverlayDuration = overlayDuration;
    }

    /// <summary>出現時の演出。<see langword="null"/> なら器の既定が使われる。</summary>
    public Func<VisualElement, Task>? Presentation { get; }

    /// <summary>閉鎖時の演出。<see langword="null"/> なら器の既定が使われる。</summary>
    public Func<VisualElement, Task>? Dismissal { get; }

    /// <summary>背景の覆いのフェード時間。<see langword="null"/> なら器の既定値になる。</summary>
    public TimeSpan? OverlayDuration { get; }

    /// <summary>透明度で出入りする演出。</summary>
    /// <param name="duration">片道の時間。既定は 250 ミリ秒。成立しない値では演出なしで即完了する。</param>
    /// <param name="easing">時間に対する進み方。既定は <see cref="Easing.CubicInOut"/>。</param>
    /// <returns>組み立てた演出の組。</returns>
    public static DialogTransition Fade(TimeSpan? duration = null, Easing? easing = null)
    {
        TimeSpan span = duration ?? DefaultDuration;
        Easing curve = easing ?? Easing.CubicInOut;
        if (!TryAnimationLength(span, out uint length))
        {
            return Inert(span);
        }

        return new DialogTransition(
            async view =>
            {
                view.Opacity = 0d;
                await view.FadeToAsync(1d, length, curve);
            },
            async view => await view.FadeToAsync(0d, length, curve),
            span);
    }

    /// <summary>指定した辺から滑り込み、同じ辺へ滑り出す演出。</summary>
    /// <param name="from">出入り口になる辺。</param>
    /// <param name="duration">片道の時間。既定は 250 ミリ秒。成立しない値では演出なしで即完了する。</param>
    /// <param name="easing">時間に対する進み方。既定は <see cref="Easing.CubicInOut"/>。</param>
    /// <returns>組み立てた演出の組。</returns>
    public static DialogTransition Slide(
        DialogTransitionEdge from,
        TimeSpan? duration = null,
        Easing? easing = null)
    {
        TimeSpan span = duration ?? DefaultDuration;
        Easing curve = easing ?? Easing.CubicInOut;
        if (!TryAnimationLength(span, out uint length))
        {
            return Inert(span);
        }

        return new DialogTransition(
            async view =>
            {
                (double x, double y) = SlideOffset(from, view);
                view.TranslationX = x;
                view.TranslationY = y;
                await view.TranslateToAsync(0d, 0d, length, curve);
            },
            async view =>
            {
                (double x, double y) = SlideOffset(from, view);
                await view.TranslateToAsync(x, y, length, curve);
            },
            span);
    }

    /// <summary>少し縮んだ状態から等倍へ広がり、同じ倍率へ縮んで消える演出。</summary>
    /// <param name="duration">片道の時間。既定は 250 ミリ秒。成立しない値では演出なしで即完了する。</param>
    /// <param name="easing">時間に対する進み方。既定は <see cref="Easing.CubicInOut"/>。</param>
    /// <returns>組み立てた演出の組。</returns>
    public static DialogTransition Zoom(TimeSpan? duration = null, Easing? easing = null)
    {
        TimeSpan span = duration ?? DefaultDuration;
        Easing curve = easing ?? Easing.CubicInOut;
        if (!TryAnimationLength(span, out uint length))
        {
            return Inert(span);
        }

        return new DialogTransition(
            async view =>
            {
                view.Scale = ZoomScale;
                view.Opacity = 0d;
                await Task.WhenAll(
                    view.ScaleToAsync(1d, length, curve),
                    view.FadeToAsync(1d, length, curve));
            },
            async view => await Task.WhenAll(
                view.ScaleToAsync(ZoomScale, length, curve),
                view.FadeToAsync(0d, length, curve)),
            span);
    }

    /// <summary>中身の演出を持たない組。覆いだけが器の既定の時間でフェードする。</summary>
    /// <returns>組み立てた演出の組。</returns>
    public static DialogTransition None() => Inert(DefaultDuration);

    /// <summary>覆いのフェード時間を、ブリッジへ渡すミリ秒へ写す。</summary>
    /// <remarks>
    /// 覆いを駆動するのはネイティブ側なので、成立する時間はそのままミリ秒として運ぶ。成立しない時間
    /// (0 以下・ミリ秒表現に収まらない大きさ) はネイティブ側でも成立しない値になるよう 0 へ寄せる —
    /// 未指定 (<see langword="null"/>) として運ぶと器の既定値でフェードすることになり、中身が即完了する
    /// のに覆いだけ動く形になるため。
    /// </remarks>
    /// <param name="overlayDuration">添付された覆いのフェード時間。未指定なら <see langword="null"/>。</param>
    /// <returns>ブリッジへ渡すミリ秒。未指定なら <see langword="null"/> (器の既定値に委ねる)。</returns>
    internal static double? ToBridgeOverlayMilliseconds(TimeSpan? overlayDuration)
    {
        if (overlayDuration is not { } span)
        {
            return null;
        }

        return TryAnimationLength(span, out uint length) ? length : 0d;
    }

    /// <summary>中身を動かさない組。覆いのフェード時間だけを持つ。</summary>
    /// <param name="overlayDuration">背景の覆いのフェード時間。</param>
    /// <returns>組み立てた演出の組。</returns>
    private static DialogTransition Inert(TimeSpan overlayDuration) =>
        new(_ => Task.CompletedTask, _ => Task.CompletedTask, overlayDuration);

    /// <summary>
    /// 片道の時間を、MAUI のアニメーション API が受け取るミリ秒へ変換する。
    /// </summary>
    /// <remarks>
    /// 0 以下と、ミリ秒表現に収まらない大きさ (<see cref="TimeSpan.MaxValue"/> を含む) は演出が成立しない値で、
    /// 丸めも例外もせずに演出なしとして扱う。
    /// </remarks>
    /// <param name="duration">片道の時間。</param>
    /// <param name="length">変換できた場合のミリ秒。</param>
    /// <returns>演出が成立するなら <see langword="true"/>。</returns>
    private static bool TryAnimationLength(TimeSpan duration, out uint length)
    {
        if (duration <= TimeSpan.Zero || duration.TotalMilliseconds > uint.MaxValue)
        {
            length = 0u;
            return false;
        }

        length = (uint)duration.TotalMilliseconds;
        return true;
    }

    /// <summary>指定の辺の外側まで View を送り出す移動量を求める。</summary>
    /// <param name="edge">出入り口になる辺。</param>
    /// <param name="view">演出の対象になる中身の View。</param>
    /// <returns>移動量 (水平, 垂直)。</returns>
    private static (double X, double Y) SlideOffset(DialogTransitionEdge edge, VisualElement view)
    {
        SlideGeometry geometry = MeasureSlideGeometry(view);
        return ResolveEdge(edge, view) switch
        {
            DialogTransitionEdge.Top => (0d, -(geometry.Top + geometry.Height)),
            DialogTransitionEdge.Bottom => (0d, geometry.ContainerHeight - geometry.Top),
            DialogTransitionEdge.Start => (-(geometry.Left + geometry.Width), 0d),
            _ => (geometry.ContainerWidth - geometry.Left, 0d),
        };
    }

    /// <summary>
    /// 送り出す距離を求めるための、View が載っている面の中での位置と大きさ。
    /// </summary>
    /// <remarks>
    /// 面が分からない環境では View 自身の大きさを面の大きさとして扱う。この場合の移動量は
    /// 「自分の外形の分だけ動く」になり、面の縁を越えるとは限らない。
    /// </remarks>
    /// <param name="view">演出の対象になる中身の View。</param>
    /// <returns>レイアウト単位でそろえた位置と大きさ。</returns>
    private static SlideGeometry MeasureSlideGeometry(VisualElement view)
    {
#if IOS
        if (view.Handler?.PlatformView is UIKit.UIView platformView && platformView.Window is UIKit.UIWindow window)
        {
            CoreGraphics.CGRect inWindow = platformView.ConvertRectToView(platformView.Bounds, null);
            return new SlideGeometry(
                inWindow.X,
                inWindow.Y,
                inWindow.Width,
                inWindow.Height,
                window.Bounds.Width,
                window.Bounds.Height);
        }
#elif ANDROID
        if (view.Handler?.PlatformView is Android.Views.View platformView
            && platformView.Context is Android.Content.Context context)
        {
            Android.Views.View root = platformView.RootView ?? platformView;
            int[] viewLocation = new int[2];
            int[] rootLocation = new int[2];
            platformView.GetLocationInWindow(viewLocation);
            root.GetLocationInWindow(rootLocation);
            // View の座標は物理ピクセルで、MAUI の移動量はレイアウト単位なので換算してそろえる
            return new SlideGeometry(
                context.FromPixels(viewLocation[0] - rootLocation[0]),
                context.FromPixels(viewLocation[1] - rootLocation[1]),
                context.FromPixels(platformView.Width),
                context.FromPixels(platformView.Height),
                context.FromPixels(root.Width),
                context.FromPixels(root.Height));
        }
#endif
        return new SlideGeometry(0d, 0d, view.Width, view.Height, view.Width, view.Height);
    }

    /// <summary>レイアウト方向に追随する辺を、画面上の辺へ読み替える。</summary>
    /// <param name="edge">添付された辺。</param>
    /// <param name="view">演出の対象になる中身の View。</param>
    /// <returns>読み替えた辺。</returns>
    private static DialogTransitionEdge ResolveEdge(DialogTransitionEdge edge, VisualElement view)
    {
        EffectiveFlowDirection direction = ((IVisualElementController)view).EffectiveFlowDirection;
        if (!direction.HasFlag(EffectiveFlowDirection.RightToLeft))
        {
            return edge;
        }

        return edge switch
        {
            DialogTransitionEdge.Start => DialogTransitionEdge.End,
            DialogTransitionEdge.End => DialogTransitionEdge.Start,
            _ => edge,
        };
    }

    /// <summary>送り出す距離を求めるための、レイアウト単位でそろえた位置と大きさ。</summary>
    /// <param name="Left">面の中での左端。</param>
    /// <param name="Top">面の中での上端。</param>
    /// <param name="Width">View の幅。</param>
    /// <param name="Height">View の高さ。</param>
    /// <param name="ContainerWidth">View が載っている面の幅。</param>
    /// <param name="ContainerHeight">View が載っている面の高さ。</param>
    private readonly record struct SlideGeometry(
        double Left,
        double Top,
        double Width,
        double Height,
        double ContainerWidth,
        double ContainerHeight);
}
