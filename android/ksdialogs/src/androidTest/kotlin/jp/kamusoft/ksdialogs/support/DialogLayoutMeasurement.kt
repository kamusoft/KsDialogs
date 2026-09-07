package jp.kamusoft.ksdialogs.support

import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.test.core.app.ActivityScenario
import jp.kamusoft.ksdialogs.DialogLayoutHost
import jp.kamusoft.ksdialogs.DialogLayoutSnapshot
import jp.kamusoft.ksdialogs.DialogOptions
import jp.kamusoft.ksdialogs.DialogPixelInsets
import jp.kamusoft.ksdialogs.DialogPlacement
import jp.kamusoft.ksdialogs.ksDialogOptions
import jp.kamusoft.ksdialogs.ksDialogPlacement
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt

/**
 * ダイアログの器を実際の画面に載せ、レイアウト完了後の外形を測る。
 *
 * rect の計算だけを確かめても LayoutParams や測定条件への反映漏れは見つからないため、
 * 実際のレイアウトパスを通した実測で確かめる (core/ADR-0009)。
 * ウィンドウの大きさとシステム領域の余白はケースが与えた値を使う
 * (環境差を期待値の分岐ではなく入力の差で表すため)。
 *
 * メタ属性は中身の View への添付として与え、show の引数に相当する placement も同じ経路で合成する
 * (core/ADR-0015)。
 */
internal object DialogLayoutMeasurement {

    /** レイアウト完了を待つ上限。 */
    private const val LAYOUT_TIMEOUT_SECONDS = 10L

    /**
     * ケースの画面条件でダイアログを組み立て、その外形を論理単位 (dp) で返す。
     *
     * @param layoutCase 画面サイズ・システム領域の余白・内容サイズを与えるケース
     * @param options 中身の View に添付する静的メタ属性。null なら添付しない
     * @param attachedPlacement 中身の View に添付する動的メタ属性。null なら添付しない
     * @param showPlacement show の引数に相当する配置。null でなければ添付を置換する
     */
    fun measureDialogRect(
        scenario: ActivityScenario<DialogLayoutTestActivity>,
        layoutCase: DialogLayoutCase,
        options: DialogOptions? = null,
        attachedPlacement: DialogPlacement? = null,
        showPlacement: DialogPlacement? = null,
    ): DialogLayoutCase.Rect = measureDialogRect(
        scenario = scenario,
        screen = layoutCase.screen,
        insets = layoutCase.insets,
        contentSize = layoutCase.contentSize,
        options = options,
        attachedPlacement = attachedPlacement,
        showPlacement = showPlacement,
        label = layoutCase.id,
    )

    /**
     * 画面条件を直に指定して組み立て、外形を論理単位 (dp) で返す。
     *
     * @param label レイアウトが完了しなかったときに条件を読み取れるようにする名前
     */
    fun measureDialogRect(
        scenario: ActivityScenario<DialogLayoutTestActivity>,
        screen: DialogLayoutCase.Size,
        insets: DialogLayoutCase.Insets,
        contentSize: DialogLayoutCase.Size,
        options: DialogOptions? = null,
        attachedPlacement: DialogPlacement? = null,
        showPlacement: DialogPlacement? = null,
        label: String = "",
    ): DialogLayoutCase.Rect {
        val measured = AtomicReference<Rect>()
        val density = AtomicReference(1f)
        val laidOut = CountDownLatch(1)

        scenario.onActivity { activity ->
            val displayDensity = activity.resources.displayMetrics.density
            density.set(displayDensity)
            fun toPixels(value: Double): Float = (value * displayDensity).toFloat()

            val contentView = FixedContentSizeView(
                context = activity,
                contentWidth = toPixels(contentSize.w).roundToInt(),
                contentHeight = toPixels(contentSize.h).roundToInt(),
            ).attach(options, attachedPlacement)

            val host = DialogLayoutHost(
                context = activity,
                snapshot = DialogLayoutSnapshot(contentView, showPlacement),
                contentView = contentView,
                visibleAreaInsets = {
                    DialogPixelInsets(
                        top = toPixels(insets.top),
                        left = toPixels(insets.left),
                        bottom = toPixels(insets.bottom),
                        right = toPixels(insets.right),
                    )
                },
            )

            val observer = activity.hostContainer.viewTreeObserver
            observer.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (!host.isLaidOut) {
                        return
                    }
                    observer.removeOnGlobalLayoutListener(this)
                    val holder = host.contentHolder
                    measured.set(Rect(holder.left, holder.top, holder.right, holder.bottom))
                    laidOut.countDown()
                }
            })
            activity.hostContainer.addView(
                host,
                FrameLayout.LayoutParams(
                    toPixels(screen.w).roundToInt(),
                    toPixels(screen.h).roundToInt(),
                ),
            )
        }

        check(laidOut.await(LAYOUT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            "$label: レイアウトが完了しなかった"
        }
        scenario.onActivity { activity -> activity.hostContainer.removeAllViews() }

        val displayDensity = density.get()
        val rect = measured.get()
        return DialogLayoutCase.Rect(
            x = rect.left / displayDensity.toDouble(),
            y = rect.top / displayDensity.toDouble(),
            w = rect.width() / displayDensity.toDouble(),
            h = rect.height() / displayDensity.toDouble(),
        )
    }
}

/** メタ属性を添付する。null の面は添付しない (供給なし)。 */
internal fun <V : View> V.attach(options: DialogOptions?, placement: DialogPlacement?): V = apply {
    if (options != null) {
        ksDialogOptions = options
    }
    if (placement != null) {
        ksDialogPlacement = placement
    }
}
