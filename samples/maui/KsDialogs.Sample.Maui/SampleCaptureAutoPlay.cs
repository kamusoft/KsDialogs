namespace KsDialogs.Sample.Maui;

/// <summary>起動引数で指定されたデモの自動再生を、プロセスの起動につき 1 回だけ取り出す受け口。</summary>
/// <remarks>一度取り出したあとは null を返すため、画面が作り直されても再生は繰り返されない。</remarks>
internal static class SampleCaptureAutoPlay
{
    /// <summary>自動再生を取り出したかどうか。</summary>
    private static bool s_consumed;

    /// <summary>自動再生するデモを取り出す。</summary>
    /// <returns>自動再生するデモ。指定が無い場合と 2 回目以降は null。</returns>
    public static SampleDemoId? ConsumeDemo()
    {
        if (s_consumed)
        {
            return null;
        }

        s_consumed = true;
        return SampleCaptureOptions.Current.Demo;
    }
}
