package jp.kamusoft.ksdialogs.support

import android.graphics.Rect
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.test.core.app.ActivityScenario
import jp.kamusoft.ksdialogs.DialogLayoutSnapshot
import jp.kamusoft.ksdialogs.DialogOptions
import jp.kamusoft.ksdialogs.DialogPixelInsets
import jp.kamusoft.ksdialogs.DialogPlacement
import jp.kamusoft.ksdialogs.LoadingContainer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt

/**
 * Loading の器が組み立てた面を実際の画面に載せ、レイアウト完了後の外形を測る。
 *
 * rect の計算だけを確かめても測定条件や制約への反映漏れは見つからないため、実際のレイアウトパスを
 * 通した実測で確かめる (core/ADR-0009)。器が変われば反映漏れの形も変わるので、Dialog の器とは別に
 * Loading の器でも同じ表を全量回す。
 *
 * ウィンドウの大きさとシステム領域の余白はケースが与えた値を使う (環境差を期待値の分岐ではなく
 * 入力の差で表すため)。器のウィンドウは端末の画面より大きくできないので、器が組み立てた面だけを
 * ケースの寸法で画面へ載せる — 面の組み立て (実効値の供給元の結び付け・覆いの敷き方・測定条件) は
 * 本番と同じ [LoadingContainer] が行う。
 */
internal object LoadingLayoutMeasurement {

    /** レイアウト完了を待つ上限。 */
    private const val LAYOUT_TIMEOUT_SECONDS = 10L

    /**
     * ケースの画面条件で Loading の器を組み立て、その外形を論理単位 (dp) で返す。
     *
     * @param layoutCase 画面サイズ・システム領域の余白・内容サイズを与えるケース
     * @param options 中身の View に添付する静的メタ属性。null なら添付しない
     * @param attachedPlacement 中身の View に添付する動的メタ属性。null なら添付しない
     * @param showPlacement 表示 API の引数に相当する配置。null でなければ添付を置換する
     */
    fun measureLoadingRect(
        scenario: ActivityScenario<DialogLayoutTestActivity>,
        layoutCase: DialogLayoutCase,
        options: DialogOptions? = null,
        attachedPlacement: DialogPlacement? = null,
        showPlacement: DialogPlacement? = null,
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
                contentWidth = toPixels(layoutCase.contentSize.w).roundToInt(),
                contentHeight = toPixels(layoutCase.contentSize.h).roundToInt(),
            ).attach(options, attachedPlacement)

            val container = LoadingContainer(
                context = activity,
                contentView = contentView,
                layoutSnapshot = DialogLayoutSnapshot(contentView, showPlacement),
                playsPresentation = true,
                visibleAreaInsets = {
                    DialogPixelInsets(
                        top = toPixels(layoutCase.insets.top),
                        left = toPixels(layoutCase.insets.left),
                        bottom = toPixels(layoutCase.insets.bottom),
                        right = toPixels(layoutCase.insets.right),
                    )
                },
            )
            val host = container.layoutHost

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
                    toPixels(layoutCase.screen.w).roundToInt(),
                    toPixels(layoutCase.screen.h).roundToInt(),
                ),
            )
        }

        check(laidOut.await(LAYOUT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            "${layoutCase.id}: レイアウトが完了しなかった"
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
