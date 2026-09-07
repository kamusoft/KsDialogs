using System;
using System.Threading.Tasks;
using Microsoft.Maui.Controls;

namespace KsDialogs;

/// <summary>
/// Loading の Native ライブラリ Bridge への委譲面 (maui/ADR-0002)。
/// </summary>
/// <remarks>
/// 合流カウント・表示世代・最新のメッセージと進捗はすべて Native 側の coordinator が唯一の正として
/// 保持する。MAUI 形態はレジストリ解決と値の写しだけを行い、状態を持たない (core/ADR-0024)。
/// 差し替え可能にしてあるため、platform 実装なしで委譲の契約 (引数・呼び出しの対応) を検証できる。
/// </remarks>
internal interface ILoadingGateway
{
    /// <summary>既定ローディングの見た目の設定を Native へ渡す。</summary>
    /// <remarks>Native の器は各表示の開始時にこの値を読む (core/ADR-0023)。</remarks>
    /// <param name="style">設定されたスタイル。</param>
    void ApplyStyle(LoadingStyle style);

    /// <summary>既定ローディングの器メタ属性を Native へ渡す。</summary>
    /// <remarks>Native の器は各表示の開始時にこの値を読む (core/ADR-0022)。</remarks>
    /// <param name="options">設定された静的メタ属性。</param>
    void ApplyOptions(DialogOptions options);

    /// <summary>合流 1 件を開始して表示する。</summary>
    /// <remarks>
    /// 戻るのは操作ブロックが有効になった時点で、入りの演出の完了は待たない。
    /// 構成ミス (View factory の失敗) は表示に進まず失敗として伝播する。
    /// </remarks>
    /// <param name="request">その表示の中身と供給値。</param>
    /// <returns>開始の完了。</returns>
    Task ShowAsync(LoadingPresentationRequest request);

    /// <summary>
    /// 合流 1 件を握ったまま処理を実行し、終了を 1 回だけ数える (スコープ形)。
    /// </summary>
    /// <remarks>
    /// <paramref name="action"/> は表示状態によらず必ず実行される。失敗しても合流 1 件の終了として
    /// 数えたうえで、その失敗をそのまま呼び出し元へ伝播する。合流最後の 1 件なら器の撤去まで待って
    /// から戻り、そうでなければ処理の完了時点で戻る。
    /// </remarks>
    /// <param name="request">その表示の中身と供給値。</param>
    /// <param name="action">進捗報告口を受け取って走る処理。</param>
    /// <returns>処理と (最後の 1 件なら) 撤去の完了。</returns>
    Task RunAsync(LoadingPresentationRequest request, Func<IProgress<double>, Task> action);

    /// <summary>合流数によらず表示を閉じる。出の演出と器の撤去の完了まで待って戻る。</summary>
    /// <returns>撤去の完了。</returns>
    Task HideAsync();

    /// <summary>表示中のメッセージを更新する。合流には関与しない。</summary>
    /// <param name="message">新しいメッセージ。</param>
    void SetMessage(string? message);
}

/// <summary>
/// Loading の表示 1 回分の中身と供給値。
/// </summary>
/// <remarks>
/// 既定ローディング (<paramref name="createContentView"/> が <see langword="null"/>) では中身を
/// ライブラリ同梱のコンテンツが受け持ち、メッセージと配置だけが供給値になる。
/// カスタム View ではメタ属性の供給は中身への添付と表示 API の placement 引数で行われるため、
/// Dialog と同じ合成・固定の段取り (<see cref="DialogPresentationContent"/>) をそのまま使う。
/// </remarks>
/// <param name="createContentView">
/// 中身の MAUI View を新規生成する関数。既定ローディングでは <see langword="null"/>。
/// </param>
/// <param name="message">既定ローディングに表示するメッセージ。指定なしは <see langword="null"/>。</param>
/// <param name="showPlacement">表示 API の引数で渡された置き場所。指定なしは <see langword="null"/>。</param>
/// <param name="progressReceiver">
/// 進捗の転送先。カスタム View の ViewModel が <see cref="ILoadingProgressReceiver"/> を
/// 実装している場合だけ設定され、それ以外は <see langword="null"/> (転送しない)。
/// </param>
internal sealed class LoadingPresentationRequest(
    Func<View>? createContentView,
    string? message,
    DialogPlacement? showPlacement,
    ILoadingProgressReceiver? progressReceiver)
{
    /// <summary>既定ローディング (ライブラリ同梱の内蔵コンテンツ) を表示する要求か。</summary>
    public bool IsBuiltin => createContentView is null;

    /// <summary>既定ローディングに表示するメッセージ。</summary>
    public string? Message { get; } = message;

    /// <summary>表示 API の引数で渡された置き場所。中身への添付より優先される。</summary>
    public DialogPlacement? ShowPlacement { get; } = showPlacement;

    /// <summary>進捗の転送先。転送しないなら <see langword="null"/>。</summary>
    public ILoadingProgressReceiver? ProgressReceiver { get; } = progressReceiver;

    /// <summary>カスタム View の中身を新規生成する。</summary>
    /// <remarks>既定ローディングの要求では呼べない (中身は Native 側が持つ)。</remarks>
    /// <returns>提示する中身と、その表示に効くメタ属性。</returns>
    public DialogPresentationContent CreateContent() =>
        createContentView is null
            ? throw new InvalidOperationException("The Native library owns the content of the default Loading.")
            : new DialogPresentationContent(createContentView(), ShowPlacement);
}
