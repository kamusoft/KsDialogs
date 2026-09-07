namespace KsDialogs.Sample.Maui;

/// <summary>撮影のために起動時へ渡された値を読む受け口。</summary>
/// <remarks>
/// 値の運び方は platform ごとに違う (iOS 系は launch arguments のトークン列、Android 系は起動 Intent の
/// string extra) ため、読み出しの実体は platform 側に置く。
/// </remarks>
internal static partial class SampleCaptureArguments
{
    /// <summary>キーに対応する値を読む。</summary>
    /// <param name="key">読み取るキー。</param>
    /// <returns>渡された値。渡されていない・空文字なら null。</returns>
    internal static partial string? Value(string key);
}
