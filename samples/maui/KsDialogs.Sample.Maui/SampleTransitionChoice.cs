namespace KsDialogs.Sample.Maui;

/// <summary>
/// トランジションデモが選べる演出。
/// </summary>
/// <remarks>プリセット 7 種に、自作フックの実演を 1 つ足したものを並べる。</remarks>
public enum SampleTransitionChoice
{
    /// <summary>透明度で出入りする。</summary>
    Fade,

    /// <summary>下辺から出入りする。</summary>
    SlideUp,

    /// <summary>上辺から出入りする。</summary>
    SlideDown,

    /// <summary>前端から出入りする。</summary>
    SlideStart,

    /// <summary>後端から出入りする。</summary>
    SlideEnd,

    /// <summary>縮小から等倍へ広がる。</summary>
    Zoom,

    /// <summary>中身の演出なし。</summary>
    None,

    /// <summary>自作のフックを実演する。</summary>
    CustomHook,
}

/// <summary>演出の選択肢を画面と契約の表現へ言い換える。</summary>
public static class SampleTransitionChoiceExtensions
{
    /// <summary>チップに表示する文言。</summary>
    /// <param name="choice">対象の選択肢。</param>
    /// <returns>画面に出す文言。</returns>
    public static string Label(this SampleTransitionChoice choice) => choice switch
    {
        SampleTransitionChoice.Fade => SampleText.TransitionFade,
        SampleTransitionChoice.SlideUp => SampleText.TransitionSlideUp,
        SampleTransitionChoice.SlideDown => SampleText.TransitionSlideDown,
        SampleTransitionChoice.SlideStart => SampleText.TransitionSlideStart,
        SampleTransitionChoice.SlideEnd => SampleText.TransitionSlideEnd,
        SampleTransitionChoice.Zoom => SampleText.TransitionZoom,
        SampleTransitionChoice.None => SampleText.TransitionNone,
        _ => SampleText.TransitionCustomHook,
    };

    /// <summary>
    /// 時間とイージングの調整を使う演出か。
    /// </summary>
    /// <param name="choice">対象の選択肢。</param>
    /// <returns>調整値を受け取るなら <see langword="true"/>。</returns>
    /// <remarks>無演出と自作フックは調整値を受け取らないため、画面では調整部を操作できなくする。</remarks>
    public static bool UsesAdjustments(this SampleTransitionChoice choice) =>
        choice is not (SampleTransitionChoice.None or SampleTransitionChoice.CustomHook);

    /// <summary>選択中の調整値と組み合わせて、中身へ添付する演出を作る。</summary>
    /// <param name="choice">対象の選択肢。</param>
    /// <param name="durationMilliseconds">片道の時間 (ミリ秒)。</param>
    /// <param name="easing">時間に対する進み方。</param>
    /// <returns>組み立てた演出の組。</returns>
    public static DialogTransition Transition(
        this SampleTransitionChoice choice,
        int durationMilliseconds,
        SampleEasingChoice easing)
    {
        TimeSpan duration = TimeSpan.FromMilliseconds(durationMilliseconds);
        Easing curve = easing.Curve();

        return choice switch
        {
            SampleTransitionChoice.Fade => DialogTransition.Fade(duration, curve),
            SampleTransitionChoice.SlideUp => DialogTransition.Slide(DialogTransitionEdge.Bottom, duration, curve),
            SampleTransitionChoice.SlideDown => DialogTransition.Slide(DialogTransitionEdge.Top, duration, curve),
            SampleTransitionChoice.SlideStart => DialogTransition.Slide(DialogTransitionEdge.Start, duration, curve),
            SampleTransitionChoice.SlideEnd => DialogTransition.Slide(DialogTransitionEdge.End, duration, curve),
            SampleTransitionChoice.Zoom => DialogTransition.Zoom(duration, curve),
            SampleTransitionChoice.None => DialogTransition.None(),
            _ => SampleCustomTransition.Create(),
        };
    }
}
