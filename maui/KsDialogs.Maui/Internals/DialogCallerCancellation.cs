using System;
using System.Threading;

namespace KsDialogs;

/// <summary>
/// show 1 回分の呼び出し元の打ち切りを、ブリッジのハンドルの打ち切りへ中継する面 (maui/ADR-0006)。
/// </summary>
/// <remarks>
/// 委譲面は UI スレッドへ移ってからブリッジを呼んでハンドルを得るため、打ち切りはハンドルを得る前にも
/// 後にも届き得る。取りこぼさないよう、打ち切りの受け付けとハンドルの受け渡しをこの面のロックで直列にする。
/// ハンドルを得る前に打ち切られていればブリッジを呼ばず、得た直後に打ち切られていればすぐに打ち切る。
/// 打ち切りの登録は <see cref="Dispose"/> で外す。
/// </remarks>
internal sealed class DialogCallerCancellation : IDisposable
{
    private readonly Lock _lock = new();
    private readonly CancellationTokenRegistration _registration;
    private bool _isCancellationRequested;
    private Action? _cancelHandle;

    /// <summary>呼び出し元の打ち切りの受け付けを始める。</summary>
    /// <param name="cancellationToken">show に渡されたトークン。</param>
    public DialogCallerCancellation(CancellationToken cancellationToken)
    {
        // 登録の時点で打ち切り済みなら、コールバックはこの場で同期に走る
        _registration = cancellationToken.Register(OnCancellationRequested);
    }

    /// <summary>打ち切りを受け付け済みか。</summary>
    public bool IsCancellationRequested
    {
        get
        {
            lock (_lock)
            {
                return _isCancellationRequested;
            }
        }
    }

    /// <summary>
    /// 打ち切られていなければブリッジを呼んでハンドルを得て、以後の打ち切りをそのハンドルへ中継する。
    /// </summary>
    /// <remarks>
    /// 打ち切り済みならブリッジを呼ばずに <see langword="null"/> を返す。ブリッジを呼んでいる間に
    /// 打ち切られた場合は、ハンドルを得た直後にそのハンドルを打ち切ってから返す。
    /// </remarks>
    /// <typeparam name="THandle">ブリッジのハンドルの型。</typeparam>
    /// <param name="present">ブリッジを呼んでハンドルを返す処理。</param>
    /// <param name="cancel">ハンドルを打ち切る処理。</param>
    /// <returns>得たハンドル。打ち切り済みでブリッジを呼ばなかったときは <see langword="null"/>。</returns>
    public THandle? Present<THandle>(Func<THandle> present, Action<THandle> cancel)
        where THandle : class
    {
        if (IsCancellationRequested)
        {
            return null;
        }

        THandle handle = present();
        bool cancelsNow;
        lock (_lock)
        {
            cancelsNow = _isCancellationRequested;
            if (!cancelsNow)
            {
                _cancelHandle = () => cancel(handle);
            }
        }

        if (cancelsNow)
        {
            cancel(handle);
        }

        return handle;
    }

    /// <summary>打ち切りの受け付けをやめる。</summary>
    public void Dispose() => _registration.Dispose();

    private void OnCancellationRequested()
    {
        Action? cancelHandle;
        lock (_lock)
        {
            _isCancellationRequested = true;
            cancelHandle = _cancelHandle;
        }

        cancelHandle?.Invoke();
    }
}
