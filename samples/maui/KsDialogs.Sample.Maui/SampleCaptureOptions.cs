using System.Globalization;

namespace KsDialogs.Sample.Maui;

/// <summary>撮影のために起動時へ渡せる設定 (cross/ADR-0010)。</summary>
/// <remarks>
/// 値が無い・空文字・検証に通らない場合は、そのキーを取り込まずに既定動作へ倒す。
/// </remarks>
internal sealed class SampleCaptureOptions
{
    /// <summary>自動再生するデモを指定するキー。</summary>
    private const string DemoKey = "demo";

    /// <summary>Loading 進捗の刻み間隔を指定するキー。</summary>
    private const string LoadingStepIntervalKey = "loading-step-interval-ms";

    /// <summary>受理する刻み間隔の下限 (ミリ秒)。</summary>
    private const int MinimumIntervalMilliseconds = 1;

    /// <summary>受理する刻み間隔の上限 (ミリ秒)。</summary>
    private const int MaximumIntervalMilliseconds = 600_000;

    /// <summary>この起動で渡された設定。</summary>
    public static SampleCaptureOptions Current { get; } = new();

    /// <summary>起動引数の受け口から設定を読み取る。</summary>
    private SampleCaptureOptions()
    {
        Demo = SampleDemoIds.From(SampleCaptureArguments.Value(DemoKey));
        LoadingStepIntervalMilliseconds = ReadLoadingStepInterval();
    }

    /// <summary>自動再生するデモ。指定なし・定義外の ID なら null。</summary>
    public SampleDemoId? Demo { get; }

    /// <summary>Loading 進捗の刻み間隔 (ミリ秒)。指定なし・受理範囲外・数値でない値なら null。</summary>
    public int? LoadingStepIntervalMilliseconds { get; }

    /// <summary>刻み間隔を読み、受理範囲の整数のときだけ返す。</summary>
    /// <returns>受理した刻み間隔 (ミリ秒)。受理しなかったなら null。</returns>
    private static int? ReadLoadingStepInterval()
    {
        string? value = SampleCaptureArguments.Value(LoadingStepIntervalKey);
        // 符号だけを許し、空白や桁区切りは許さない。他ルートの数値解析 (Swift Int / Kotlin toLongOrNull)
        // と受理する文字列を揃えるための指定
        if (!int.TryParse(value, NumberStyles.AllowLeadingSign, CultureInfo.InvariantCulture, out int milliseconds))
        {
            return null;
        }

        return milliseconds is >= MinimumIntervalMilliseconds and <= MaximumIntervalMilliseconds
            ? milliseconds
            : null;
    }
}
