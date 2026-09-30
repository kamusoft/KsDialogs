using System;
using System.Collections.Concurrent;
using System.Threading;
using System.Threading.Tasks;

namespace KsDialogs.Maui.Tests.Support;

/// <summary>
/// 「UI スレッドで呼ぶ口」の偽物。専用スレッド 1 本の上で、渡された処理を受け取った順に呼ぶ。
/// </summary>
/// <remarks>
/// 素の .NET には UI スレッドが無いため、platform 実装の UI スレッドへの移送にあたる部分を
/// この専用スレッドで置き換える。口を通った回数 (<see cref="DispatchCount"/>) と
/// 専用スレッドの識別子 (<see cref="ManagedThreadId"/>) から、処理がどの経路でどのスレッドから
/// 始まったかを確かめられる。
/// </remarks>
internal sealed class DedicatedUiThread : IDisposable
{
    private readonly BlockingCollection<Action> _queue = [];
    private readonly Thread _thread;
    private int _dispatchCount;

    /// <summary>専用スレッドを起動する。</summary>
    public DedicatedUiThread()
    {
        _thread = new Thread(Loop) { IsBackground = true, Name = nameof(DedicatedUiThread) };
        _thread.Start();
    }

    /// <summary>専用スレッドの識別子。</summary>
    public int ManagedThreadId => _thread.ManagedThreadId;

    /// <summary>「UI スレッドで呼ぶ口」として処理を受け取った回数。</summary>
    public int DispatchCount => Volatile.Read(ref _dispatchCount);

    /// <summary>「UI スレッドで呼ぶ口」。渡された処理を専用スレッドで呼び、その完了を返す。</summary>
    /// <param name="work">専用スレッドで呼ぶ処理。</param>
    /// <returns>処理の完了。処理が失敗すればその失敗で終わる。</returns>
    public Task InvokeAsync(Func<Task> work)
    {
        Interlocked.Increment(ref _dispatchCount);
        return PostAsync(work);
    }

    /// <summary>
    /// 口を通った回数に数えずに、渡された処理を専用スレッドで呼ぶ。検証の準備で専用スレッドへ移るときに使う。
    /// </summary>
    /// <param name="work">専用スレッドで呼ぶ処理。</param>
    /// <returns>処理の完了。処理が失敗すればその失敗で終わる。</returns>
    public Task PostAsync(Func<Task> work)
    {
        TaskCompletionSource completion = new(TaskCreationOptions.RunContinuationsAsynchronously);
        _queue.Add(() =>
        {
            Task running;
            try
            {
                running = work();
            }
            catch (Exception failure)
            {
                completion.TrySetException(failure);
                return;
            }

            running.ContinueWith(
                finished =>
                {
                    if (finished.Exception is AggregateException aggregate)
                    {
                        completion.TrySetException(aggregate.InnerExceptions);
                    }
                    else if (finished.IsCanceled)
                    {
                        completion.TrySetCanceled();
                    }
                    else
                    {
                        completion.TrySetResult();
                    }
                },
                TaskScheduler.Default);
        });
        return completion.Task;
    }

    /// <inheritdoc/>
    public void Dispose()
    {
        _queue.CompleteAdding();
        _thread.Join();
        _queue.Dispose();
    }

    /// <summary>専用スレッドの本体。受け取った処理を順に呼ぶ。</summary>
    private void Loop()
    {
        foreach (Action item in _queue.GetConsumingEnumerable())
        {
            item();
        }
    }
}
