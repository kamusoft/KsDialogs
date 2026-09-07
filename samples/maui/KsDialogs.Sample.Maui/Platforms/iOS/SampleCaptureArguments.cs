using Foundation;

namespace KsDialogs.Sample.Maui;

internal static partial class SampleCaptureArguments
{
    /// <summary>launch arguments の <c>--&lt;キー&gt;</c> の直後のトークンを値として読む。</summary>
    /// <param name="key">読み取るキー。</param>
    /// <returns>渡された値。渡されていない・空文字なら null。</returns>
    /// <remarks>同じキーが複数回現れたときは最初の 1 組を採る。</remarks>
    internal static partial string? Value(string key)
    {
        string[] arguments = NSProcessInfo.ProcessInfo.Arguments;
        int keyIndex = Array.IndexOf(arguments, $"--{key}");
        if (keyIndex < 0 || keyIndex + 1 >= arguments.Length)
        {
            return null;
        }

        string value = arguments[keyIndex + 1];
        return value.Length == 0 ? null : value;
    }
}
