using System;
using System.Collections.Generic;
using System.Diagnostics;

namespace KsDialogs.Maui.Tests.Support;

/// <summary>警告として流れた文言を溜める聞き手。</summary>
/// <remarks>
/// 受理後の失敗は呼び出し元へ返らないため、原因を追える手掛かりは警告にしか残らない。
/// その警告が実際に残ることを検証するために使う。
/// </remarks>
internal sealed class RecordingTraceListener : TraceListener
{
    /// <summary>受け取った警告の文言。</summary>
    public List<string> Warnings { get; } = [];

    /// <summary>聞き手を登録し、処理を実行してから必ず外す。</summary>
    /// <param name="work">警告を観測したい処理。</param>
    /// <returns>その間に受け取った警告の文言。</returns>
    public static IReadOnlyList<string> Capture(Action work)
    {
        RecordingTraceListener listener = new();
        Trace.Listeners.Add(listener);
        try
        {
            work();
        }
        finally
        {
            Trace.Listeners.Remove(listener);
        }

        return listener.Warnings;
    }

    /// <inheritdoc/>
    public override void TraceEvent(
        TraceEventCache? eventCache,
        string source,
        TraceEventType eventType,
        int id,
        string? format,
        params object?[]? args)
    {
        if (eventType == TraceEventType.Warning)
        {
            Warnings.Add(format is null ? string.Empty : string.Format(format, args ?? []));
        }
    }

    /// <inheritdoc/>
    public override void Write(string? message)
    {
    }

    /// <inheritdoc/>
    public override void WriteLine(string? message)
    {
    }
}
