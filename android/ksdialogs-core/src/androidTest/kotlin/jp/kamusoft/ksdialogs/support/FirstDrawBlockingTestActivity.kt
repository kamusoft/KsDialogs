package jp.kamusoft.ksdialogs.support

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 最初の描画を、テストが解除するまで描画の直前で止める画面。
 *
 * 描画の直前 (PreDraw) で「描画しない」を返し続ける。利用者が起動画面を延ばすときと同じ仕組みなので、
 * resumed だが未描画の画面と、延長中の起動画面の下にある画面を同じ形で作れる。
 *
 * 描画を止めている間は、フレームごとに描画がやり直されて UI スレッドが空かない。UI スレッドが空くのを待つ
 * 起動の手段 (`ActivityScenario`・`Instrumentation.waitForIdleSync`) は返らないので、[launch] で開く。
 */
class FirstDrawBlockingTestActivity : Activity() {

    private lateinit var contentView: View

    private val drawBlocker = ViewTreeObserver.OnPreDrawListener { false }

    /** 実際に描画された回数。UI スレッドで数え、任意のスレッドから読む。 */
    @Volatile
    var drawCount: Int = 0
        private set

    /** resumed か。UI スレッドで書き、任意のスレッドから読む。 */
    @Volatile
    var isResumedNow: Boolean = false
        private set

    /** 破棄されたか。UI スレッドで書き、任意のスレッドから読む。 */
    @Volatile
    var isDestroyedNow: Boolean = false
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contentView = FrameLayout(this).apply { setBackgroundColor(Color.WHITE) }
        setContentView(contentView)
        contentView.viewTreeObserver.addOnPreDrawListener(drawBlocker)
        contentView.viewTreeObserver.addOnDrawListener { drawCount += 1 }
        latest = this
    }

    override fun onResume() {
        super.onResume()
        isResumedNow = true
    }

    override fun onPause() {
        isResumedNow = false
        super.onPause()
    }

    override fun onDestroy() {
        // 作り直しではウィンドウが引き継がれ、描画の直前の listener も同じウィンドウに残る。
        // 残すと新しい画面の描画まで止め続けるので、止めるのは各画面が自分の分だけにする
        window.peekDecorView()?.viewTreeObserver?.takeIf { it.isAlive }?.removeOnPreDrawListener(drawBlocker)
        contentView.viewTreeObserver.takeIf { it.isAlive }?.removeOnPreDrawListener(drawBlocker)
        isDestroyedNow = true
        if (latest === this) latest = null
        super.onDestroy()
    }

    /** 描画の停止を解いて、描画を促す。UI スレッドから呼ぶ。 */
    fun releaseDraw() {
        contentView.viewTreeObserver.removeOnPreDrawListener(drawBlocker)
        contentView.invalidate()
    }

    companion object {
        @Volatile
        private var latest: FirstDrawBlockingTestActivity? = null

        /**
         * 画面を開き、resumed になるまで待って返す。時間切れなら例外を投げる。
         *
         * 開いた画面は、描画の停止を解いてから [finishAndWait] で閉じる。
         */
        suspend fun launch(): FirstDrawBlockingTestActivity {
            latest = null
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val intent = Intent(instrumentation.targetContext, FirstDrawBlockingTestActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            instrumentation.targetContext.startActivity(intent)
            check(InstrumentedDialogWaiting.waitUntil { latest?.isResumedNow == true }) {
                "描画を止めた画面が resumed にならない"
            }
            return checkNotNull(latest)
        }
    }

    /**
     * 構成の変更と同じ作り直しを起こし、新しい画面が resumed になるまで待って返す。時間切れなら例外を投げる。
     *
     * 旧画面の pause・stop・破棄と新画面の作成・start・resume は、同じメッセージの中で続く。
     * 新しい画面も最初の描画を止めた状態で開く。
     */
    suspend fun recreateAndWait(): FirstDrawBlockingTestActivity {
        withContext(Dispatchers.Main) { recreate() }
        check(
            InstrumentedDialogWaiting.waitUntil {
                val recreated = latest
                recreated != null && recreated !== this && recreated.isResumedNow
            },
        ) {
            "作り直した画面が resumed にならない"
        }
        return checkNotNull(latest)
    }

    /** 描画の停止を解いてから閉じ、破棄されるまで待つ。 */
    suspend fun finishAndWait() {
        withContext(Dispatchers.Main) {
            releaseDraw()
            finish()
        }
        InstrumentedDialogWaiting.waitUntil { isDestroyedNow }
    }
}
