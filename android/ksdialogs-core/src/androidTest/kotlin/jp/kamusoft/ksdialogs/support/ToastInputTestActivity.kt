package jp.kamusoft.ksdialogs.support

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.EditText
import android.widget.FrameLayout
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import java.util.concurrent.atomic.AtomicInteger

/**
 * ソフトキーボードとシステムのジェスチャの届き方を確かめるための画面。
 *
 * Toast のウィンドウがフォーカスもタッチも奪わないことは、入力欄が文字入力の対象で
 * あり続けること・戻るとホームが通常どおり届くことでしか判定できない。
 * そのため入力欄と、戻る・画面の停止を数える受け皿だけを持つ。
 *
 * 戻るを受けても画面は閉じない (閉じてしまうと「届いたかどうか」を後から確かめられなくなる)。
 * 戻るの届き方は 2 経路ある: 予測型バック (predictive back) が有効な環境では
 * [OnBackInvokedDispatcher] に登録したコールバックへ届き [onBackPressed] は呼ばれない。
 * 無効な環境では従来どおり [onBackPressed] へ届く。
 * どちらの経路でも数えられるよう両方の受け皿を持つ (同時に両方へ届くことはない)。
 */
class ToastInputTestActivity : Activity() {

    /** ソフトキーボードを出すための入力欄。 */
    lateinit var inputField: EditText
        private set

    private val backPresses = AtomicInteger(0)
    private val stops = AtomicInteger(0)

    /** 予測型バックの経路で戻るを受ける口。登録できる API レベルでだけ作る。 */
    private var backInvokedCallback: OnBackInvokedCallback? = null

    /** 戻るが届いた回数。 */
    val backPressCount: Int
        get() = backPresses.get()

    /** 画面が前面から外れた回数。 */
    val stopCount: Int
        get() = stops.get()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // 予測型バックが有効だと、システムの戻るはこの口にだけ届き、画面既定の finish は起きない
            val callback = OnBackInvokedCallback { backPresses.incrementAndGet() }
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                callback,
            )
            backInvokedCallback = callback
        }
        inputField = EditText(this).apply { hint = "入力" }
        setContentView(
            FrameLayout(this).apply {
                setBackgroundColor(Color.WHITE)
                addView(
                    inputField,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                    ),
                )
            },
        )
    }

    @Deprecated("フレームワークの受け口の名前に合わせる")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        backPresses.incrementAndGet()
    }

    override fun onStop() {
        stops.incrementAndGet()
        super.onStop()
    }

    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            backInvokedCallback?.let { onBackInvokedDispatcher.unregisterOnBackInvokedCallback(it) }
            backInvokedCallback = null
        }
        super.onDestroy()
    }
}
