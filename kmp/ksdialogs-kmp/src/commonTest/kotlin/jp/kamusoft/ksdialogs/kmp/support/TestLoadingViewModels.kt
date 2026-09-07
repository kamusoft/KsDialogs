package jp.kamusoft.ksdialogs.kmp.support

import jp.kamusoft.ksdialogs.kmp.LoadingProgressReceiver
import jp.kamusoft.ksdialogs.kmp.LoadingViewModel

/** 進捗の受け口を持たない検証用のカスタム Loading の ViewModel。 */
internal class PlainTestLoadingViewModel : LoadingViewModel

/**
 * 進捗の受け口を実装した検証用のカスタム Loading の ViewModel。
 *
 * 共有コードで定義した ViewModel が受け口を実装すれば進捗が届くことを見るために使う。
 */
internal class ProgressReceivingTestLoadingViewModel : LoadingViewModel, LoadingProgressReceiver {
    /** 受け取った進捗を呼ばれた順に並べたもの。 */
    val receivedProgress: MutableList<Double> = mutableListOf()

    override fun onProgress(progress: Double) {
        receivedProgress += progress
    }
}
