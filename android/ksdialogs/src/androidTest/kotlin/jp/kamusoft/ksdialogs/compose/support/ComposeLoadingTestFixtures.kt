package jp.kamusoft.ksdialogs.compose.support

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.setValue
import jp.kamusoft.ksdialogs.LoadingProgressReceiver
import jp.kamusoft.ksdialogs.LoadingViewModel

/**
 * 進捗の受け口を備えたカスタム Loading の ViewModel。
 *
 * 転送された値を受理順に控えつつ、宣言的 UI から観測できる状態としても持つ。
 * 受け口は UI スレッドで呼ばれるため、状態の書き換えはそのまま組み立て直しにつながる。
 */
internal abstract class RecordingLoadingTestViewModel : LoadingViewModel, LoadingProgressReceiver {

    private val received = mutableListOf<Double>()

    /** 中身が観測する最新の進捗。 */
    var progress: Double by mutableDoubleStateOf(0.0)
        private set

    /** 受け取った進捗値を受理順に並べたもの。 */
    val receivedProgress: List<Double>
        get() = synchronized(received) { received.toList() }

    override fun onProgress(progress: Double) {
        synchronized(received) { received.add(progress) }
        this.progress = progress
    }
}

/** 宣言的 UI の中身で登録するカスタム Loading の ViewModel。 */
internal class ComposeLoadingTestViewModel : RecordingLoadingTestViewModel()

/** 対照となる従来 View 系の中身で登録するカスタム Loading の ViewModel。 */
internal class ViewLoadingTestViewModel : RecordingLoadingTestViewModel()
