namespace KsDialogs;

/// <summary>
/// スコープ形の Loading (<see cref="IKsLoading"/> の <c>StartAsync</c>) で、渡した処理を始めるスレッド。
/// </summary>
/// <remarks>
/// 保証するのは処理の最初の文を実行するスレッドだけである。処理の中で <see langword="await"/> した後に
/// どこへ戻るかは C# の規則に従う (UI スレッドで始まった処理は、既定では UI スレッドの同期コンテキストへ
/// 戻るが、<c>ConfigureAwait(false)</c> を書いた場合は戻らない)。
/// <para>
/// この指定が効くのは UI スレッドを持つ iOS / Android だけである。それ以外の実行環境
/// (UI スレッドも表示先も持たない素の .NET) では、指定に関係なく呼び出し元のスレッドでそのまま始まる。
/// </para>
/// </remarks>
public enum LoadingActionThread
{
    /// <summary>
    /// UI スレッドで始める (既定)。処理の中から、UI スレッドへ移し直さずに画面の要素へ触れられる。
    /// </summary>
    Main,

    /// <summary>
    /// UI スレッド外 (スレッドプール) で始める。UI に触れない重い処理を、UI スレッドを塞がずに走らせる。
    /// </summary>
    Background,
}
