namespace KsDialogs;

/// <summary>
/// カスタム Loading の ViewModel が進捗の配送を受け取るための任意の面。
/// </summary>
/// <remarks>
/// 実装した ViewModel で表示している間だけ、スコープ形の処理が報告した進捗が転送される。
/// 実装しない ViewModel では転送されず、誤りにもならない。
/// 呼び出しは UI スレッド上で行われ、値は 0〜1 に丸めた後のものが渡る
/// (丸めは Native 実装が行う — core/ADR-0001)。
/// </remarks>
public interface ILoadingProgressReceiver
{
    /// <summary>進捗の報告を受け取る。</summary>
    /// <param name="progress">0〜1 に丸めた後の進捗値。</param>
    void OnProgress(double progress);
}
