using Android.Content;
using Android.OS;

namespace KsDialogs.Sample.Maui;

internal static partial class SampleCaptureArguments
{
    /// <summary>起動 Intent が運んできた extra。取り込む前は null。</summary>
    private static Bundle? s_extras;

    /// <summary>起動 Intent の extra を受け口へ取り込む。</summary>
    /// <param name="intent">Activity の起動 Intent。</param>
    /// <remarks>画面の組み立てより先に呼ぶ。</remarks>
    internal static void Capture(Intent? intent) => s_extras = intent?.Extras;

    /// <summary>起動 Intent の string extra を値として読む。</summary>
    /// <param name="key">読み取るキー。</param>
    /// <returns>渡された値。渡されていない・空文字なら null。</returns>
    /// <remarks>string 以外の型で渡された値は読めないため null になり、同じく既定動作へ倒れる。</remarks>
    internal static partial string? Value(string key)
    {
        string? value = s_extras?.GetString(key);
        return string.IsNullOrEmpty(value) ? null : value;
    }
}
