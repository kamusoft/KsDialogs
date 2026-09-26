package jp.kamusoft.ksdialogs.support

import android.graphics.Color
import android.view.View
import android.widget.FrameLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import jp.kamusoft.ksdialogs.DialogCurrentPage
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt

/**
 * 共通ケース表の `pageArea` を、Activity 側に置いたページ領域の View として再現する。
 *
 * ケース表の座標の原点は器を載せるウィンドウの左上で、検証では器の面を `hostContainer` の左上に
 * 置くため、ページ領域の View も同じ原点から `pageArea` の位置・大きさに置く。
 * 器がページへ問い合わせるのは載った瞬間の初回パスなので、器を載せる前にページを配置し終えておく。
 */
internal object CurrentPageAreaFixture {

    /**
     * ページ領域の View を置き、レイアウトが終わるまで待つ。
     *
     * @return 置いた View。検証の後片付けは `hostContainer.removeAllViews()` で一緒に外れる
     */
    fun place(
        scenario: ActivityScenario<DialogLayoutTestActivity>,
        pageArea: DialogLayoutCase.Rect,
    ): View {
        val placed = AtomicReference<View>()
        scenario.onActivity { activity ->
            val density = activity.resources.displayMetrics.density
            fun toPixels(value: Double): Int = (value * density).roundToInt()

            val pageView = View(activity).apply { setBackgroundColor(PAGE_COLOR) }
            activity.hostContainer.addView(
                pageView,
                FrameLayout.LayoutParams(toPixels(pageArea.w), toPixels(pageArea.h)).apply {
                    leftMargin = toPixels(pageArea.x)
                    topMargin = toPixels(pageArea.y)
                },
            )
            placed.set(pageView)
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val pageView = requireNotNull(placed.get())
        val laidOut = AtomicReference(false)
        scenario.onActivity { laidOut.set(pageView.isLaidOut) }
        check(laidOut.get() == true) { "ページ領域の View がレイアウトされなかった" }
        return pageView
    }

    /**
     * ページ領域の View を返す関数を登録した状態で [build] を実行し、終わったら登録を元に戻す。
     *
     * 器は生成の時点で登録内容を捕まえるため、器の生成をこの中で行えば以後の登録には左右されない。
     * [pageView] が null なら登録には触れずに [build] だけを実行する。
     */
    fun <T> registeringPage(pageView: View?, build: () -> T): T {
        if (pageView == null) {
            return build()
        }
        val previous = DialogCurrentPage.provider
        DialogCurrentPage.provider = { pageView }
        return try {
            build()
        } finally {
            DialogCurrentPage.provider = previous
        }
    }

    /** ページ領域を見分けやすくする色。外形の測定には影響しない。 */
    private const val PAGE_COLOR: Int = Color.LTGRAY
}
