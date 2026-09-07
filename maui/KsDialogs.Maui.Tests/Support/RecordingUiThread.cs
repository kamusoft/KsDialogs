using System;

namespace KsDialogs.Maui.Tests.Support;

/// <summary>
/// UI スレッドへの委譲を記録する差し替え。
/// </summary>
/// <remarks>
/// platform 実装では MAUI の UI スレッドへ渡す部分にあたる。渡された処理はその場で実行し、
/// 実行中かどうかを <see cref="IsRunning"/> で見えるようにして、
/// 「その処理が UI スレッドの側で始まったか」を検証できるようにする。
/// </remarks>
internal sealed class RecordingUiThread
{
    /// <summary>委譲を受けた回数。</summary>
    public int DispatchCount { get; private set; }

    /// <summary>いま委譲された処理を実行している最中か。</summary>
    public bool IsRunning { get; private set; }

    /// <summary>渡された処理を UI スレッドの側で実行する。</summary>
    /// <param name="action">実行する処理。</param>
    public void Run(Action action)
    {
        DispatchCount++;
        IsRunning = true;
        try
        {
            action();
        }
        finally
        {
            IsRunning = false;
        }
    }
}
