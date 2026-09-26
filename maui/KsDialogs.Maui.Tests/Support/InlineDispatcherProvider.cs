using System;
using Microsoft.Maui.Dispatching;

namespace KsDialogs.Maui.Tests.Support;

/// <summary>
/// 渡された処理をその場で呼ぶ dispatcher を返す provider。
/// </summary>
/// <remarks>
/// 素の .NET には UI スレッドの dispatcher が無く、生成時に dispatcher を求める MAUI の型
/// (<c>TabbedPage</c> など) を組み立てられない。ページ構成を組むだけの検証で、その代わりに使う。
/// </remarks>
internal sealed class InlineDispatcherProvider : IDispatcherProvider
{
    private static readonly InlineDispatcher s_dispatcher = new();

    /// <summary>provider を差し替え、処理の後に元へ戻す操作を返す。</summary>
    /// <returns>元の provider へ戻す操作。</returns>
    public static IDisposable Install()
    {
        IDispatcherProvider previous = DispatcherProvider.Current;
        DispatcherProvider.SetCurrent(new InlineDispatcherProvider());
        return new Restorer(previous);
    }

    public IDispatcher? GetForCurrentThread() => s_dispatcher;

    private sealed class Restorer(IDispatcherProvider previous) : IDisposable
    {
        public void Dispose() => DispatcherProvider.SetCurrent(previous);
    }

    private sealed class InlineDispatcher : IDispatcher
    {
        public bool IsDispatchRequired => false;

        public bool Dispatch(Action action)
        {
            action();
            return true;
        }

        public bool DispatchDelayed(TimeSpan delay, Action action)
        {
            action();
            return true;
        }

        public IDispatcherTimer CreateTimer() =>
            throw new NotSupportedException("Timers are not used by page hierarchy tests.");
    }
}
