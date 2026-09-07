package jp.kamusoft.ksdialogs

/**
 * カスタム Loading の ViewModel が進捗の配送を受け取るための任意の面。
 *
 * 準拠した ViewModel で表示している間だけ、スコープ形の処理が報告した進捗が転送される。
 * 準拠しない ViewModel では転送されず、誤りにもならない。
 * 呼び出しは UI スレッド上で行われ、値は 0〜1 に丸めた後のものが渡る。
 */
public interface LoadingProgressReceiver {
    /**
     * 進捗の報告を受け取る。
     *
     * @param progress 0〜1 に丸めた後の進捗値
     */
    public fun onProgress(progress: Double)
}
