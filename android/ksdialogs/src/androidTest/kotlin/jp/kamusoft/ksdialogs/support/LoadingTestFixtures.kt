package jp.kamusoft.ksdialogs.support

import android.view.View
import jp.kamusoft.ksdialogs.DialogViewModel
import jp.kamusoft.ksdialogs.LoadingProgressReceiver
import jp.kamusoft.ksdialogs.LoadingViewModel
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/** カスタム Loading の表示に使う最小の ViewModel。 */
internal class LoadingTestViewModel : LoadingViewModel

/** View factory を登録しない ViewModel。未登録の経路を確認するために使う。 */
internal class UnregisteredLoadingTestViewModel : LoadingViewModel

/** Dialog と Loading の両方のレジストリへ登録して、互いの独立を確かめるための ViewModel。 */
internal class DualRegistryLoadingTestViewModel :
    LoadingViewModel,
    DialogViewModel<Boolean>

/** 進捗の受け口を備えた ViewModel。転送された値を記録する。 */
internal class ProgressReceivingLoadingTestViewModel : LoadingViewModel, LoadingProgressReceiver {
    private val received = mutableListOf<Double>()

    /** 受け取った進捗値を受理順に並べたもの。 */
    val receivedProgress: List<Double>
        get() = synchronized(received) { received.toList() }

    override fun onProgress(progress: Double) {
        synchronized(received) { received.add(progress) }
    }
}

/**
 * 型を渡す表示で使う、configure で状態を書き換えられる ViewModel。
 *
 * 進捗の受け口も備えるので、生成された ViewModel へ転送が届くかも同じ型で見られる。
 */
internal class ConfigurableLoadingTestViewModel(var title: String = "factory の既定") :
    LoadingViewModel,
    LoadingProgressReceiver {

    private val received = mutableListOf<Double>()

    /** 受け取った進捗値を受理順に並べたもの。 */
    val receivedProgress: List<Double>
        get() = synchronized(received) { received.toList() }

    override fun onProgress(progress: Double) {
        synchronized(received) { received.add(progress) }
    }
}

/** 合流の相手として別の型が要るときに使う、もう1つの型を渡す表示用の ViewModel。 */
internal class SecondaryConfigurableLoadingTestViewModel(var title: String = "factory の既定") :
    LoadingViewModel

/** value class の ViewModel。参照型限定の拒否を確かめるために使う。 */
@JvmInline
internal value class ValueClassLoadingTestViewModel(val label: String) : LoadingViewModel

/**
 * 型を渡す表示の観察。ViewModel の生成と、中身の生成時に見えた状態を記録する。
 *
 * 中身の生成のたびに、そのとき ViewModel が持っていた表題を控えるので、
 * 「configure の完了後に中身が作られる」順序をそのまま読み取れる。
 */
internal class LoadingTypedShowRecorder {
    private val lock = Any()
    private val created = mutableListOf<Any>()
    private val observed = mutableListOf<String>()
    private val views = mutableListOf<View>()

    /** ViewModel factory が作った ViewModel を生成順に並べたもの。 */
    val createdViewModels: List<Any>
        get() = synchronized(lock) { created.toList() }

    /** 中身の生成時に見えた表題を生成順に並べたもの。 */
    val observedTitles: List<String>
        get() = synchronized(lock) { observed.toList() }

    /** 中身が生成された回数。 */
    val viewCreationCount: Int
        get() = synchronized(lock) { views.size }

    /** 最後に生成された中身の View。 */
    val lastView: View?
        get() = synchronized(lock) { views.lastOrNull() }

    fun recordCreation(viewModel: Any) {
        synchronized(lock) { created.add(viewModel) }
    }

    fun recordView(title: String, view: View) {
        synchronized(lock) {
            observed.add(title)
            views.add(view)
        }
    }
}

/** テストが任意の時点で処理を完了させるための関門。 */
internal class LoadingTestGate {
    private val opened = CompletableDeferred<Unit>()

    /** 開くまで待つ。 */
    suspend fun await() {
        opened.await()
    }

    /** 待っている処理を進ませる。 */
    fun open() {
        opened.complete(Unit)
    }
}

/** factory が生成した View を記録する。 */
internal class LoadingTestViewRecorder {
    private val created = AtomicInteger(0)
    private val last = AtomicReference<View>()

    /** factory が呼ばれた回数。 */
    val createdCount: Int
        get() = created.get()

    /** 最後に生成された View。 */
    val lastView: View?
        get() = last.get()

    fun record(view: View) {
        created.incrementAndGet()
        last.set(view)
    }
}
