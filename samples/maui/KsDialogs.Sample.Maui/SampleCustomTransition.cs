namespace KsDialogs.Sample.Maui;

/// <summary>
/// 自作のフックで組む演出。
/// </summary>
/// <remarks>
/// プリセットを使わずに <see cref="DialogTransition"/> を直接組み立てる書き方の実演で、
/// 時間もイージングもこの演出自身が決めるため、画面の調整値は使わない。
/// </remarks>
public static class SampleCustomTransition
{
    /// <summary>入場でホスト View が移動する距離。下から持ち上げる分だけ最初に押し下げる。</summary>
    private const double TravelDistance = 80d;

    /// <summary>入場・退場の時間 (ミリ秒)。</summary>
    private const uint DurationMilliseconds = 300u;

    /// <summary>入場は上方向へ移動しながら現れ、退場は透明度だけで消える演出を作る。</summary>
    /// <returns>組み立てた演出の組。</returns>
    public static DialogTransition Create() => new(
        async view =>
        {
            view.TranslationY = TravelDistance;
            view.Opacity = 0d;

            // 演出が終わってから戻る (戻るまで器は「表示中」へ進まない)
            await Task.WhenAll(
                view.TranslateToAsync(0d, 0d, DurationMilliseconds, Easing.CubicInOut),
                view.FadeToAsync(1d, DurationMilliseconds, Easing.CubicInOut));
        },
        async view => await view.FadeToAsync(0d, DurationMilliseconds, Easing.CubicInOut));
}
