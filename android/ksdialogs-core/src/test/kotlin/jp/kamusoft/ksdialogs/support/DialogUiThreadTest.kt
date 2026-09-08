package jp.kamusoft.ksdialogs.support

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.Dispatchers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.util.concurrent.Executors

/**
 * UI スレッド相当の単一スレッドを Main ディスパッチャに据えるテストの土台。
 *
 * show の提示処理が UI スレッドで実行されることと、任意のスレッドからの呼び出しが
 * 成立することを、実際のスレッド移動を伴って観察できるようにする。
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class DialogUiThreadTest {
    private val uiExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, UI_THREAD_NAME)
    }
    private val uiDispatcher = uiExecutor.asCoroutineDispatcher()

    /** UI スレッド相当のスレッド。 */
    protected lateinit var uiThread: Thread
        private set

    @BeforeEach
    fun installUiDispatcher() {
        Dispatchers.setMain(uiDispatcher)
        uiThread = runBlocking(uiDispatcher) { Thread.currentThread() }
    }

    @AfterEach
    fun removeUiDispatcher() {
        Dispatchers.resetMain()
        uiDispatcher.close()
    }

    private companion object {
        const val UI_THREAD_NAME = "ksdialogs-ui-test"
    }
}
