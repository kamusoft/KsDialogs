using System;
using Microsoft.Maui.Controls;

namespace KsDialogs;

/// <summary>
/// Toast の Native ライブラリ Bridge への委譲面 (maui/ADR-0002)。
/// </summary>
/// <remarks>
/// 表示中のリスト・重なり順・各表示の計時はすべて Native 側の coordinator が唯一の正として保持する。
/// MAUI 形態はレジストリ解決と値の写しだけを行い、状態を持たない。
/// 差し替え可能にしてあるため、platform 実装なしで委譲の契約 (引数・呼び出しの対応) を検証できる。
/// </remarks>
internal interface IToastGateway
{
    /// <summary>Toast の一括設定を Native へ渡す。</summary>
    /// <remarks>Native の器は各表示の受理時にこの値を読む (core/ADR-0032)。</remarks>
    /// <param name="style">設定されたスタイル。</param>
    void ApplyStyle(ToastStyle style);

    /// <summary>表示 1 枚を Native へ渡す。</summary>
    /// <remarks>
    /// 表示は fire-and-forget であり、戻り値も表示終了を待つ手段も持たない (core/ADR-0031)。
    /// 受理後の失敗 (中身の実体化・器の取り付けの失敗) は Native 側でその 1 枚の破棄として扱われる。
    /// </remarks>
    /// <param name="request">その表示の中身と供給値。</param>
    void Show(ToastPresentationRequest request);
}

/// <summary>
/// Toast の表示 1 回分の中身と供給値。
/// </summary>
/// <remarks>
/// デフォルト View (<paramref name="createContentView"/> が <see langword="null"/>) では中身を
/// ライブラリ同梱のコンテンツが受け持ち、メッセージと duration・配置だけが供給値になる。
/// カスタム View では配置の供給が中身への添付と表示 API の placement 引数で行われるため、
/// Dialog と同じ合成・固定の段取り (<see cref="DialogPresentationContent"/>) をそのまま使う。
/// </remarks>
/// <param name="createContentView">
/// 中身の MAUI View を新規生成する関数。デフォルト View では <see langword="null"/>。
/// </param>
/// <param name="message">デフォルト View に表示するメッセージ。カスタム View では <see langword="null"/>。</param>
/// <param name="durationMilliseconds">
/// 表示するミリ秒。指定なしは <see langword="null"/> (スタイルの既定 duration が使われる)。
/// </param>
/// <param name="showPlacement">表示 API の引数で渡された置き場所。指定なしは <see langword="null"/>。</param>
internal sealed class ToastPresentationRequest(
    Func<View>? createContentView,
    string? message,
    int? durationMilliseconds,
    DialogPlacement? showPlacement)
{
    /// <summary>デフォルト View (ライブラリ同梱の内蔵コンテンツ) を表示する要求か。</summary>
    public bool IsBuiltin => createContentView is null;

    /// <summary>デフォルト View に表示するメッセージ。</summary>
    public string? Message { get; } = message;

    /// <summary>表示するミリ秒。指定なしは <see langword="null"/>。</summary>
    public int? DurationMilliseconds { get; } = durationMilliseconds;

    /// <summary>表示 API の引数で渡された置き場所。中身への添付より優先される。</summary>
    public DialogPlacement? ShowPlacement { get; } = showPlacement;

    /// <summary>カスタム View の中身を新規生成する。</summary>
    /// <remarks>デフォルト View の要求では呼べない (中身は Native 側が持つ)。</remarks>
    /// <returns>提示する中身と、その表示に効くメタ属性。</returns>
    public DialogPresentationContent CreateContent() =>
        createContentView is null
            ? throw new InvalidOperationException("The Native library owns the content of the default Toast.")
            : new DialogPresentationContent(createContentView(), ShowPlacement);
}
