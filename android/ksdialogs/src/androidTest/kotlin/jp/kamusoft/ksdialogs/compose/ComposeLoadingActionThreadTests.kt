package jp.kamusoft.ksdialogs.compose

import android.os.Looper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.kamusoft.ksdialogs.Loading
import jp.kamusoft.ksdialogs.LoadingActionThread
import jp.kamusoft.ksdialogs.compose.support.ComposeDialogTestActivity
import jp.kamusoft.ksdialogs.compose.support.ComposeLoadingTestViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose の中身を渡すスコープ形 ([startCompose]) でも、処理を始めるスレッドの指定が効くことを確かめる。
 *
 * どちらの場合も UI スレッド外 (Default dispatcher) から開始し、処理の最初の文で実行スレッドを読み取って
 * その値を処理の戻り値として持ち帰る。提示は公開 API だけを使い、実際に Loading のウィンドウを出す経路で行う。
 */
@RunWith(AndroidJUnit4::class)
class ComposeLoadingActionThreadTests {

    @get:Rule
    val activityRule: ActivityScenarioRule<ComposeDialogTestActivity> =
        ActivityScenarioRule(ComposeDialogTestActivity::class.java)

    @Test
    fun LD_HA_02_Compose_の_startCompose_でも指定が効く() = runBlocking<Unit> {
        val (calledOnMainThread, defaultStartedOnMainThread) = withContext(Dispatchers.Default) {
            isMainThread() to Loading.instance.startCompose(
                ComposeLoadingTestViewModel(),
                content = { _ -> Box(Modifier.size(CONTENT_SIZE_DP.dp)) },
            ) { _ -> isMainThread() }
        }
        val backgroundStartedOnMainThread = withContext(Dispatchers.Default) {
            Loading.instance.startCompose(
                ComposeLoadingTestViewModel(),
                content = { _ -> Box(Modifier.size(CONTENT_SIZE_DP.dp)) },
                actionThread = LoadingActionThread.BACKGROUND,
            ) { _ -> isMainThread() }
        }

        assertFalse("呼び出しは UI スレッド外から行っている", calledOnMainThread)
        assertTrue("指定なしでは処理は UI スレッドで始まる", defaultStartedOnMainThread)
        assertFalse("BACKGROUND では処理は UI スレッド外で始まる", backgroundStartedOnMainThread)
    }

    private fun isMainThread(): Boolean = Looper.myLooper() === Looper.getMainLooper()

    private companion object {
        /** 中身の一辺 (dp)。 */
        const val CONTENT_SIZE_DP = 160
    }
}
