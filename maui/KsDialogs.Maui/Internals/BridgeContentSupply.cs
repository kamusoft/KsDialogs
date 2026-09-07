using System;
using System.Diagnostics;
using System.Runtime.ExceptionServices;

namespace KsDialogs;

/// <summary>
/// 受理後に呼ばれる中身の供給を、失敗を境界の外へ漏らさずに行う面 (core/ADR-0033)。
/// </summary>
/// <remarks>
/// カスタム View の中身は、Native の器が提示先を確保した後に Native 側から C# の供給元を
/// 呼び出して作られる。この呼び出しは managed / native の境界を跨いでおり、そこを例外が越えると
/// 未処理の障害 (プロセス終了) になるため、供給元の失敗はこの面で受け止めて値 (<see langword="null"/>)
/// として返す。
/// <para>
/// 中身を作れなかった表示がどうなるかは面ごとに違う。Toast は show が既に戻っているため
/// その 1 枚を破棄するしかなく (<see cref="CreateOrDiscard{TContent}"/>)、Loading と Dialog は
/// 呼び出し元がまだ待っているため失敗として返せる (<see cref="CreateOrFail{TContent}"/>)。
/// 後者では元の失敗を呼び出し 1 回分の預かり口 (<see cref="BridgeContentFailure"/>) に退避し、
/// 互換面から失敗が返ってきた時点で呼び出し元へそのまま投げ直す。
/// どちらを選んでも、この面の仕事は失敗を値に変えるところまでで、
/// その先の扱いは Native の器に委ねる。
/// </para>
/// </remarks>
internal static class BridgeContentSupply
{
    /// <summary>
    /// 中身を作る。作れなければ、その表示 1 枚が破棄されることを警告に残して
    /// <see langword="null"/> を返す。
    /// </summary>
    /// <typeparam name="TContent">供給する中身の型。</typeparam>
    /// <param name="create">中身を新規生成する関数。</param>
    /// <returns>作れた中身。作れなければ <see langword="null"/>。</returns>
    public static TContent? CreateOrDiscard<TContent>(Func<TContent> create)
        where TContent : class =>
        Create(create, "This presentation is discarded. Other presentations are not affected", failure: null);

    /// <summary>
    /// 中身を作る。作れなければ、元の失敗を預かり口へ退避したうえで
    /// <see langword="null"/> を返す。
    /// </summary>
    /// <remarks>
    /// 呼び出し元はまだ待っているため、退避した失敗は互換面からの失敗の通知を受け取った時点で
    /// そのまま投げ直される。預かり口は呼び出し 1 回分なので、他の呼び出しには影響しない。
    /// </remarks>
    /// <typeparam name="TContent">供給する中身の型。</typeparam>
    /// <param name="create">中身を新規生成する関数。</param>
    /// <param name="failure">その呼び出し 1 回分の失敗の預かり口。</param>
    /// <returns>作れた中身。作れなければ <see langword="null"/>。</returns>
    public static TContent? CreateOrFail<TContent>(Func<TContent> create, BridgeContentFailure failure)
        where TContent : class =>
        Create(create, "This call fails", failure);

    /// <summary>中身を作り、作れなければ失敗の扱いを添えて警告を残す。</summary>
    /// <typeparam name="TContent">供給する中身の型。</typeparam>
    /// <param name="create">中身を新規生成する関数。</param>
    /// <param name="effect">作れなかったときに、その表示がたどる扱い。</param>
    /// <param name="failure">元の失敗の預かり口。預かる先が無ければ <see langword="null"/>。</param>
    /// <returns>作れた中身。作れなければ <see langword="null"/>。</returns>
    private static TContent? Create<TContent>(
        Func<TContent> create,
        string effect,
        BridgeContentFailure? failure)
        where TContent : class
    {
        try
        {
            return create();
        }
        catch (Exception thrown)
        {
            // 預かり先の無い面 (Toast) では、原因を追える手掛かりはこの警告にしか残らない
            Trace.TraceWarning("Could not create the presentation content. {0}: {1}", effect, thrown);
            failure?.Capture(thrown);
            return null;
        }
    }
}

/// <summary>
/// 中身を作れなかった失敗を、呼び出し元へ投げ直すまで預かる口。
/// </summary>
/// <remarks>
/// 中身の供給は互換面 (Native) から呼ばれるため、そこで起きた失敗を例外のまま境界へ返せない。
/// 失敗は値 (<see langword="null"/>) として返しつつ、元の例外はこの口が呼び出し 1 回分だけ預かり、
/// 互換面から失敗の通知が返ってきた時点で型・メッセージ・スタックを保ったまま呼び出し元へ渡す。
/// 供給が呼ばれるのは 1 回分の表示につき 1 回だが、最初の失敗だけを預かることで
/// 呼び出し元が受け取る原因は常に 1 つに定まる。
/// </remarks>
internal sealed class BridgeContentFailure
{
    /// <summary>預かっている失敗。まだ失敗していなければ <see langword="null"/>。</summary>
    private ExceptionDispatchInfo? captured;

    /// <summary>失敗を預かる。既に預かっていれば最初の失敗を残す。</summary>
    /// <param name="thrown">供給元が投げた失敗。</param>
    public void Capture(Exception thrown) =>
        captured ??= ExceptionDispatchInfo.Capture(thrown);

    /// <summary>預かっている失敗。まだ失敗していなければ <see langword="null"/>。</summary>
    public Exception? Cause => captured?.SourceException;
}
