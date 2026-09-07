package jp.kamusoft.ksdialogs.samples.kmp

import jp.kamusoft.ksdialogs.kmp.LoadingProgressReceiver
import jp.kamusoft.ksdialogs.kmp.LoadingViewModel

/**
 * Custom Loading の ViewModel。
 *
 * 共有コードで定義したこのクラスがそのまま各 OS のレジストリの登録キーになる。
 * 進捗の受け口 ([LoadingProgressReceiver]) を実装しているため、スコープ形の処理が報告した進捗が
 * 表示中のあいだ転送される。届いた値は保持したうえで、購読している中身の View へそのまま流す。
 */
class CustomLoadingViewModel : LoadingViewModel, LoadingProgressReceiver {
    /** 0〜1 に丸めた後の進捗。まだ報告が無い開始直後は 0。 */
    var progress: Double = 0.0
        private set

    /** 進捗の変化を受け取る中身の View。表示していない間は null。 */
    var onProgressChanged: ((Double) -> Unit)? = null

    /** 進捗の報告を受け取る。呼び出しは UI スレッド上で行われる。 */
    override fun onProgress(progress: Double) {
        this.progress = progress
        onProgressChanged?.invoke(progress)
    }
}
