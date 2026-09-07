namespace KsDialogs.Sample.Maui;

/// <summary>
/// トランジションデモが選べるイージング。
/// </summary>
/// <remarks>
/// 契約はイージングを形態のネイティブ表現でそのまま受け取るため、
/// 選択肢は <see cref="Easing"/> への言い換えになる。
/// </remarks>
public enum SampleEasingChoice
{
    /// <summary>加速して減速する。</summary>
    Standard,

    /// <summary>等速。</summary>
    Linear,

    /// <summary>加速する。</summary>
    Accelerate,

    /// <summary>減速する。</summary>
    Decelerate,
}

/// <summary>イージングの選択肢を画面と契約の表現へ言い換える。</summary>
public static class SampleEasingChoiceExtensions
{
    /// <summary>チップに表示する文言。</summary>
    /// <param name="choice">対象の選択肢。</param>
    /// <returns>画面に出す文言。</returns>
    public static string Label(this SampleEasingChoice choice) => choice switch
    {
        SampleEasingChoice.Standard => SampleText.EasingStandard,
        SampleEasingChoice.Linear => SampleText.EasingLinear,
        SampleEasingChoice.Accelerate => SampleText.EasingAccelerate,
        _ => SampleText.EasingDecelerate,
    };

    /// <summary>プリセットへ渡す時間曲線。</summary>
    /// <param name="choice">対象の選択肢。</param>
    /// <returns>時間に対する進み方。</returns>
    public static Easing Curve(this SampleEasingChoice choice) => choice switch
    {
        SampleEasingChoice.Standard => Easing.CubicInOut,
        SampleEasingChoice.Linear => Easing.Linear,
        SampleEasingChoice.Accelerate => Easing.CubicIn,
        _ => Easing.CubicOut,
    };
}
