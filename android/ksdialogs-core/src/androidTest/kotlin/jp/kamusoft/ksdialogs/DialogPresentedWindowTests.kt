package jp.kamusoft.ksdialogs

import android.app.Activity
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowInsets
import android.widget.TextView
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.kamusoft.ksdialogs.support.DialogLayoutTestActivity
import jp.kamusoft.ksdialogs.support.attach
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 提示したダイアログのウィンドウが、契約どおりの基準領域を持つことを確かめる。
 *
 * 共通ケース表の検証は画面条件を入力として与えるため、実際のウィンドウに割り当てられた範囲までは見ていない。
 * 基準領域 `window` が画面全体を指し、システム領域が insets として届くことは、
 * 実際に提示したウィンドウでしか確かめられない。
 */
@RunWith(AndroidJUnit4::class)
class DialogPresentedWindowTests {

    @get:Rule
    val activityRule: ActivityScenarioRule<DialogLayoutTestActivity> =
        ActivityScenarioRule(DialogLayoutTestActivity::class.java)

    @Test
    fun 基準領域は画面全体になる() {
        val display = displayBounds()
        val container = presentContainer()
        val host = awaitLaidOutLayoutHost(container)

        val location = IntArray(2)
        val size = AtomicReference<Rect>()
        activityRule.scenario.onActivity {
            host.getLocationOnScreen(location)
            size.set(Rect(0, 0, host.width, host.height))
        }
        dismiss(container)

        assertEquals("ウィンドウの左上が画面の左上と一致しない", 0, location[0])
        assertEquals("ウィンドウの左上が画面の左上と一致しない", 0, location[1])
        assertEquals("ウィンドウの幅が画面全体でない", display.width(), size.get().width())
        assertEquals("ウィンドウの高さが画面全体でない", display.height(), size.get().height())
    }

    @Test
    fun システム領域は_insets_として届く() {
        val expected = AtomicReference(0)
        activityRule.scenario.onActivity { activity ->
            expected.set(systemBarsTop(activity))
        }
        val container = presentContainer()
        awaitLaidOutLayoutHost(container)

        val actual = AtomicReference(-1)
        activityRule.scenario.onActivity {
            val windowInsets = requireNotNull(container.window).decorView.rootWindowInsets
            actual.set(topInset(requireNotNull(windowInsets)))
        }
        dismiss(container)

        assertTrue("提示先の画面でシステム領域が観測できていない", expected.get() > 0)
        assertEquals("ダイアログ側に届くシステム領域が提示先と食い違う", expected.get(), actual.get())
    }

    @Test
    fun 可視領域基準の上寄せはシステム領域と余白の分だけ下がる() {
        val statusBarTop = AtomicReference(0)
        val density = AtomicReference(1f)
        activityRule.scenario.onActivity { activity ->
            statusBarTop.set(systemBarsTop(activity))
            density.set(activity.resources.displayMetrics.density)
        }

        // 基準領域と余白は既定値 (可視領域・全辺 24) のまま、上寄せだけを添付する
        val container = presentContainer(
            DialogPlacement(verticalAlignment = DialogAlignment.START),
        )
        awaitLaidOutLayoutHost(container)
        val contentTop = AtomicReference(-1)
        activityRule.scenario.onActivity {
            val location = IntArray(2)
            container.contentView.getLocationOnScreen(location)
            contentTop.set(location[1])
        }
        dismiss(container)

        // 可視領域 (システム領域の内側) の上端から、余白の分だけ下がった位置になる
        val expected = statusBarTop.get() +
            (DEFAULT_MARGIN_DP * density.get()).roundToInt()
        assertTrue("提示先の画面でシステム領域が観測できていない", statusBarTop.get() > 0)
        assertTrue(
            "システム領域と余白が位置に反映されていない (期待 $expected 実測 ${contentTop.get()})",
            abs(contentTop.get() - expected) <= POSITION_TOLERANCE_PIXELS,
        )
    }

    /** ダイアログを1枚出す。 */
    private fun presentContainer(placement: DialogPlacement? = null): DialogContainer {
        val container = AtomicReference<DialogContainer>()
        activityRule.scenario.onActivity { activity ->
            DialogContainer(
                context = activity,
                contentView = TextView(activity).apply { text = "ウィンドウの確認" }
                    .attach(options = null, placement = placement),
                resultChannel = DialogResultChannel(),
            ).also {
                container.set(it)
                it.show()
            }
        }
        return container.get()
    }

    /** 覆いの面がレイアウトを終えるまで待つ。 */
    private fun awaitLaidOutLayoutHost(container: DialogContainer): DialogLayoutHost {
        val host = requireNotNull(container.layoutHost) { "覆いの面が組み立てられていない" }
        val laidOut = CountDownLatch(1)
        activityRule.scenario.onActivity {
            if (host.isLaidOut) {
                laidOut.countDown()
                return@onActivity
            }
            host.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> laidOut.countDown() }
        }
        check(laidOut.await(LAYOUT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) { "レイアウトが完了しなかった" }
        return host
    }

    private fun dismiss(container: DialogContainer) {
        activityRule.scenario.onActivity { container.dismiss() }
    }

    /** システムバーを含めた画面全体の大きさ。 */
    private fun displayBounds(): Rect {
        val bounds = AtomicReference<Rect>()
        activityRule.scenario.onActivity { activity ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                bounds.set(Rect(activity.windowManager.currentWindowMetrics.bounds))
            } else {
                val metrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                activity.windowManager.defaultDisplay.getRealMetrics(metrics)
                bounds.set(Rect(0, 0, metrics.widthPixels, metrics.heightPixels))
            }
        }
        return bounds.get()
    }

    private fun systemBarsTop(activity: Activity): Int {
        val windowInsets = activity.window.decorView.rootWindowInsets ?: return 0
        return topInset(windowInsets)
    }

    private fun topInset(windowInsets: WindowInsets): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowInsets.getInsets(WindowInsets.Type.systemBars()).top
        } else {
            @Suppress("DEPRECATION")
            windowInsets.systemWindowInsetTop
        }

    private companion object {
        const val LAYOUT_TIMEOUT_SECONDS = 10L

        /** 位置の比較で許す差 (px)。丸めの分だけを見込む。 */
        const val POSITION_TOLERANCE_PIXELS = 2

        /** 契約が定める余白の既定値 (dp)。 */
        const val DEFAULT_MARGIN_DP = 24.0
    }
}
