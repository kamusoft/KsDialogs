using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Maui.Controls;

namespace KsDialogs.Maui.Tests.Support;

/// <summary>
/// 呼び出し元の打ち切りを、互換面のハンドルの打ち切りへ中継する platform 実装の代わり。
/// </summary>
/// <remarks>
/// platform 実装と同じ順序で振る舞う — UI スレッドへ移り、打ち切りの中継
/// (<see cref="DialogCallerCancellation"/>) を通して互換面を呼び、ハンドルを得る。結果を確定した show は
/// ハンドルを閉じ、閉鎖の通知を受けて結果を配送する。
/// 互換面は <see cref="TestBridgeHandle"/> で模し、提示先の有無・中身の生成・閉鎖の通知を再現する。
/// UI スレッドへ移る途中で止めて、ハンドルを得る前の打ち切りも再現できる。
/// </remarks>
/// <param name="hasPresentationHost">互換面を呼んだ時点で提示先があるか。無ければ出現を待つ。</param>
/// <param name="holdsUiThreadHop">UI スレッドへ移る途中で止めるか。止めたら <see cref="ReleaseUiThreadHop"/> で進める。</param>
/// <param name="holdsRemoval">
/// 表示中の閉鎖要求で、撤去 (退出の演出) の完了を止めておくか。止めたら <see cref="TestBridgeHandle.CompleteRemoval"/> で進める。
/// </param>
internal sealed class CancellableTestDialogGateway(
    bool hasPresentationHost,
    bool holdsUiThreadHop = false,
    bool holdsRemoval = false) : IDialogGateway
{
    private readonly TaskCompletionSource _uiThreadHopReached =
        new(TaskCreationOptions.RunContinuationsAsynchronously);

    private readonly TaskCompletionSource _uiThreadHopGate =
        new(TaskCreationOptions.RunContinuationsAsynchronously);

    /// <summary>委譲を受けた回数。</summary>
    public int PresentCallCount { get; private set; }

    /// <summary>互換面を呼んだ回数。</summary>
    public int BridgeCallCount { get; private set; }

    /// <summary>互換面が返したハンドル (呼ばれた順)。</summary>
    public List<TestBridgeHandle> Handles { get; } = [];

    /// <summary>作られた中身の View (作られた順)。</summary>
    public List<View> CreatedViews { get; } = [];

    /// <summary>UI スレッドへ移る途中で止まったら完了する。</summary>
    public Task UiThreadHopReached => _uiThreadHopReached.Task;

    /// <summary>UI スレッドへ移る途中で止めていた委譲を進める。</summary>
    public void ReleaseUiThreadHop() => _uiThreadHopGate.TrySetResult();

    /// <summary>提示先を出現させ、待っている互換面の提示を進める。</summary>
    public void MakeHostAppear()
    {
        foreach (TestBridgeHandle handle in Handles)
        {
            handle.PresentIfWaiting();
        }
    }

    /// <inheritdoc/>
    public async Task<DialogOutcome> PresentAsync(DialogPresentationRequest request)
    {
        PresentCallCount++;
        DialogPresentationCompletion completion = new(request.ResultChannel);
        using DialogCallerCancellation cancellation = new(request.CancellationToken);

        if (holdsUiThreadHop)
        {
            _uiThreadHopReached.TrySetResult();
            await _uiThreadHopGate.Task.ConfigureAwait(false);
        }

        TestBridgeHandle? handle = cancellation.Present(
            () =>
            {
                BridgeCallCount++;
                TestBridgeHandle created = new(request, completion, CreatedViews, holdsRemoval);
                Handles.Add(created);
                if (hasPresentationHost)
                {
                    created.PresentIfWaiting();
                }

                return created;
            },
            created => created.Cancel());
        if (handle is null)
        {
            completion.Settle(DialogOutcome.Cancelled.Instance);
            completion.Deliver();
            return await completion.Delivered.ConfigureAwait(false);
        }

        _ = request.ResultChannel.Result.ContinueWith(
            _ => handle.Dismiss(),
            TaskContinuationOptions.ExecuteSynchronously);
        return await completion.Delivered.ConfigureAwait(false);
    }
}

/// <summary>
/// 互換面のハンドルの代わり。提示先を待つ・表示中・閉じた、の 3 つの状態を持つ。
/// </summary>
/// <remarks>
/// 互換面と同じく、待っている間に閉じる・打ち切ると中身を作らずに止まり、表示中に閉じる・打ち切ると
/// 閉じる。どちらも閉鎖の通知 (配送) はちょうど 1 回になる。
/// </remarks>
/// <param name="request">その show が提示する内容。</param>
/// <param name="completion">その show の結果の確定と配送を扱う面。</param>
/// <param name="createdViews">作った中身の View の記録先。</param>
/// <param name="holdsRemoval">表示中の閉鎖要求で、撤去の完了を止めておくか。</param>
internal sealed class TestBridgeHandle(
    DialogPresentationRequest request,
    DialogPresentationCompletion completion,
    List<View> createdViews,
    bool holdsRemoval = false)
{
    private readonly Lock _lock = new();
    private bool _isPresented;
    private bool _isClosed;
    private bool _isRemoving;

    /// <summary>打ち切りを受けた回数。</summary>
    public int CancelCount { get; private set; }

    /// <summary>閉鎖要求を受けた回数。</summary>
    public int DismissCount { get; private set; }

    /// <summary>中身を作って表示しているか。</summary>
    public bool IsPresented
    {
        get
        {
            lock (_lock)
            {
                return _isPresented && !_isClosed;
            }
        }
    }

    /// <summary>閉じたか。</summary>
    public bool IsClosed
    {
        get
        {
            lock (_lock)
            {
                return _isClosed;
            }
        }
    }

    /// <summary>提示先を待っていれば、中身を作って表示する。</summary>
    public void PresentIfWaiting()
    {
        lock (_lock)
        {
            if (_isPresented || _isClosed)
            {
                return;
            }

            _isPresented = true;
        }

        createdViews.Add(request.CreateContent().ContentView);
    }

    /// <summary>
    /// 互換面の打ち切り。待っている間なら表示せず、表示中なら閉じて、cancelled で通知する。
    /// 撤去の途中なら、その完了を待たずに通知する。
    /// </summary>
    public void Cancel()
    {
        lock (_lock)
        {
            CancelCount++;
            if (_isClosed && !_isRemoving)
            {
                return;
            }

            _isClosed = true;
            _isRemoving = false;
        }

        completion.Settle(DialogOutcome.Cancelled.Instance);
        completion.Deliver();
    }

    /// <summary>互換面の閉鎖要求。待っている間なら中身を作らずに止め、表示中なら閉じる。</summary>
    public void Dismiss()
    {
        lock (_lock)
        {
            DismissCount++;
            if (_isClosed)
            {
                return;
            }

            _isClosed = true;
            if (holdsRemoval && _isPresented)
            {
                // 撤去の完了 (閉鎖の通知) は CompleteRemoval まで持ち越す
                _isRemoving = true;
                return;
            }
        }

        completion.Deliver();
    }

    /// <summary>止めていた撤去を完了させ、閉鎖を通知する。</summary>
    public void CompleteRemoval()
    {
        lock (_lock)
        {
            if (!_isRemoving)
            {
                return;
            }

            _isRemoving = false;
        }

        completion.Deliver();
    }
}
