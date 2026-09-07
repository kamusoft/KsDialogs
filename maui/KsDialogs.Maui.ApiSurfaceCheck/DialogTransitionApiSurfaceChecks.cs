using System;
using System.Threading.Tasks;
using Microsoft.Maui;
using Microsoft.Maui.Controls;

namespace KsDialogs.ApiSurfaceCheck;

/// <summary>
/// 出入りの演出の公開 API 形状の正の検証。
/// </summary>
/// <remarks>
/// このファイルがコンパイルできることが検証結果であり、添付面・フックの型・プリセットの引数の形が
/// 契約どおりでなくなればビルドが失敗する (core/ADR-0017)。
/// </remarks>
public static class DialogTransitionApiSurfaceChecks
{
    /// <summary>演出は中身の View への添付で供給できる。</summary>
    /// <param name="contentView">ダイアログの中身になる View。</param>
    public static void AcceptsTransitionAttachmentOnContentView(View contentView)
    {
        Dialog.SetTransition(contentView, DialogTransition.Fade());
        // 添付を外して器の既定へ戻せる
        Dialog.SetTransition(contentView, null);
    }

    /// <summary>添付した演出は同じ名前で読み出せ、未添付なら null になる。</summary>
    /// <param name="contentView">ダイアログの中身になる View。</param>
    /// <returns>読み出した演出。</returns>
    public static DialogTransition? AcceptsReadingAttachedTransition(View contentView) =>
        Dialog.GetTransition(contentView);

    /// <summary>添付プロパティ本体も公開されている (スタイルやバインディングの対象にできる)。</summary>
    /// <returns>演出の添付プロパティ。</returns>
    public static BindableProperty AcceptsTransitionBindableProperty() => Dialog.TransitionProperty;

    /// <summary>両フックとも省略でき、片側だけの供給もできる。</summary>
    /// <returns>組み立てた演出の組。</returns>
    public static DialogTransition AcceptsOptionalHooks()
    {
        _ = new DialogTransition();
        _ = new DialogTransition(presentation: view => view.FadeToAsync(1d));
        _ = new DialogTransition(dismissal: view => view.FadeToAsync(0d));
        return new DialogTransition(
            presentation: view => view.FadeToAsync(1d),
            dismissal: view => view.FadeToAsync(0d),
            overlayDuration: TimeSpan.FromMilliseconds(300d));
    }

    /// <summary>フックは中身の MAUI View を受け取り、完了を <see cref="Task"/> で返す形をしている。</summary>
    /// <param name="transition">読み出す演出。</param>
    /// <returns>出現時のフック。</returns>
    public static Func<VisualElement, Task>? AcceptsHookShape(DialogTransition transition)
    {
        Func<VisualElement, Task>? dismissal = transition.Dismissal;
        TimeSpan? overlayDuration = transition.OverlayDuration;
        _ = dismissal;
        _ = overlayDuration;
        return transition.Presentation;
    }

    /// <summary>プリセットは時間とイージングを省略でき、指定もできる。</summary>
    /// <returns>組み立てた演出の組。</returns>
    public static DialogTransition AcceptsPresets()
    {
        TimeSpan duration = TimeSpan.FromMilliseconds(400d);
        _ = DialogTransition.Fade();
        _ = DialogTransition.Fade(duration, Easing.SinInOut);
        _ = DialogTransition.Slide(DialogTransitionEdge.Top);
        _ = DialogTransition.Slide(DialogTransitionEdge.Bottom, duration, Easing.SinInOut);
        _ = DialogTransition.Slide(DialogTransitionEdge.Start, duration);
        _ = DialogTransition.Zoom();
        _ = DialogTransition.Zoom(duration, Easing.SinInOut);
        _ = DialogTransition.Slide(DialogTransitionEdge.End, easing: Easing.Linear);
        return DialogTransition.None();
    }
}
