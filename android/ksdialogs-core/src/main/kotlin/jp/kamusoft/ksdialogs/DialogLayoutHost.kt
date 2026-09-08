package jp.kamusoft.ksdialogs

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowInsets
import kotlin.math.roundToInt

/**
 * ウィンドウ全体を占め、背後の覆いを描いて中身を規則どおりの位置・大きさに置く面。
 *
 * ウィンドウの大きさとシステム領域の余白は測定のたびに読み直すため、
 * 画面の回転やシステムバーの変化にもそのまま追随する。
 *
 * @param snapshot 実効値の供給元。初回レイアウトパスの完了時点で固定される
 * @param contentView 中身として表示する View
 * @param visibleAreaInsets 可視領域を狭めるシステム領域の幅 (px) を求める方法。
 *   既定はウィンドウが報告する値
 * @param onLayoutSnapshotFrozen 実効値を固定した時点で呼ばれる。器が出入りの進行を始める合図になる
 */
internal class DialogLayoutHost(
    context: Context,
    private val snapshot: DialogLayoutSnapshot,
    contentView: View,
    private val visibleAreaInsets: (View) -> DialogPixelInsets = ::windowVisibleAreaInsets,
    private val onLayoutSnapshotFrozen: () -> Unit = {},
) : ViewGroup(context) {

    /**
     * 背景の覆い。中身の兄弟として敷き、中身とは独立にフェードする (core/ADR-0017)。
     *
     * 親子にすると覆いのフェードが中身の演出を巻き込むため、同じ面の別の子として置く。
     */
    val overlayView: View = View(context)

    /** ダイアログの外形になる面。測定・配置の対象はこの1つだけ。 */
    val contentHolder: DialogContentHolder = DialogContentHolder(context, contentView)

    /** 直近の測定で求めた解。配置はこの解と実測サイズから決まる。 */
    private var solution: DialogLayoutSolution? = null

    /** 実効値を固定済みか。固定は契約上のスナップショット時点 (初回レイアウトパス完了) で行われる。 */
    val isLayoutSnapshotFrozen: Boolean
        get() = snapshot.isFrozen

    /** 直近の測定で覆いに塗った色。同じ色なら塗り直さない。 */
    private var appliedOverlayColor: Int? = null

    /** 収束のためにレイアウトを回し直せる残り回数。 */
    private var remainingConvergencePasses = MAX_CONVERGENCE_PASSES

    /**
     * 初回レイアウトパスの完了を捉えて実効値を固定する受け皿。
     *
     * 描画の直前はレイアウトが終わった後なので、契約が定めるスナップショット時点にあたる
     * (core/ADR-0015)。この時点で添付が測定時と変わっていれば、描画を見送って
     * 組み直した値でもう一度パスを回し、収束したところで固定する。
     */
    private val snapshotSettlingObserver = object : ViewTreeObserver.OnPreDrawListener {
        override fun onPreDraw(): Boolean {
            if (snapshot.isFrozen) {
                viewTreeObserver.removeOnPreDrawListener(this)
                return true
            }
            if (remainingConvergencePasses > 0 && snapshot.hasPendingChange()) {
                remainingConvergencePasses--
                requestLayout()
                return false
            }
            if (snapshot.hasPendingChange()) {
                // 上限まで回しても添付が変わり続けている。採用されるのは最後のパスの値になり、
                // 契約が定める「初回レイアウトパス完了時点の値」とは限らないため、供給元の不具合として知らせる
                Log.w(
                    LOG_TAG,
                    "The attachments did not converge; using the values from layout pass $MAX_CONVERGENCE_PASSES.",
                )
            }
            snapshot.freeze()
            viewTreeObserver.removeOnPreDrawListener(this)
            onLayoutSnapshotFrozen()
            return true
        }
    }

    init {
        // 出入りの演出で中身が面の外へ動く (滑り出し等) ため、子を面の内側で切り取らない
        clipChildren = false
        addView(overlayView)
        addView(contentHolder)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!snapshot.isFrozen) {
            viewTreeObserver.addOnPreDrawListener(snapshotSettlingObserver)
        }
    }

    override fun onDetachedFromWindow() {
        viewTreeObserver.removeOnPreDrawListener(snapshotSettlingObserver)
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val windowWidth = MeasureSpec.getSize(widthMeasureSpec)
        val windowHeight = MeasureSpec.getSize(heightMeasureSpec)
        // 固定前は測定のたびに供給値を読み直す。固定後は常に同じ値が返る
        val layout = snapshot.effective()
        if (appliedOverlayColor != layout.overlayColor) {
            appliedOverlayColor = layout.overlayColor
            overlayView.setBackgroundColor(layout.overlayColor)
        }
        overlayView.measure(
            MeasureSpec.makeMeasureSpec(windowWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(windowHeight, MeasureSpec.EXACTLY),
        )
        val resolved = DialogLayoutResolver.resolve(
            layout = layout,
            windowWidth = windowWidth.toFloat(),
            windowHeight = windowHeight.toFloat(),
            visibleAreaInsets = visibleAreaInsets(this),
            density = resources.displayMetrics.density,
        )
        solution = resolved
        contentHolder.measure(
            contentHolderMeasureSpec(resolved.horizontal),
            contentHolderMeasureSpec(resolved.vertical),
        )
        setMeasuredDimension(windowWidth, windowHeight)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        overlayView.layout(0, 0, right - left, bottom - top)
        val resolved = solution ?: return
        val width = contentHolder.measuredWidth
        val height = contentHolder.measuredHeight
        val x = resolved.horizontal.originFor(width.toFloat()).roundToInt()
        val y = resolved.vertical.originFor(height.toFloat()).roundToInt()
        contentHolder.layout(x, y, x + width, y + height)
    }

    /**
     * 外形に渡す測定条件を決める。
     *
     * サイズが決まっている軸は有効領域の軸長で頭打ちにした値をそのまま与え、
     * 内容サイズに委ねる軸は有効領域の軸長を上限として与える。
     */
    private fun contentHolderMeasureSpec(axis: DialogAxisLayout): Int {
        val maxSize = maxOf(0f, axis.maxSize).roundToInt()
        val size = axis.size
        return if (size == null) {
            MeasureSpec.makeMeasureSpec(maxSize, MeasureSpec.AT_MOST)
        } else {
            MeasureSpec.makeMeasureSpec(minOf(size.roundToInt(), maxSize), MeasureSpec.EXACTLY)
        }
    }

    private companion object {
        /**
         * 実効値を固定する前にレイアウトを回し直せる上限。
         *
         * 読むたびに値が変わり続ける供給元でも、描画に入れないまま回り続けないようにする。
         */
        const val MAX_CONVERGENCE_PASSES = 4

        /** 供給元の不具合を知らせるときの記録先。 */
        const val LOG_TAG = "KsDialogs"
    }
}

/** ウィンドウが報告するシステムバーなどの余白 (px)。ウィンドウに載っていなければ 0。 */
internal fun windowVisibleAreaInsets(view: View): DialogPixelInsets {
    val windowInsets = view.rootWindowInsets ?: return DialogPixelInsets.ZERO
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val systemBars = windowInsets.getInsets(WindowInsets.Type.systemBars())
        DialogPixelInsets(
            top = systemBars.top.toFloat(),
            left = systemBars.left.toFloat(),
            bottom = systemBars.bottom.toFloat(),
            right = systemBars.right.toFloat(),
        )
    } else {
        @Suppress("DEPRECATION")
        DialogPixelInsets(
            top = windowInsets.systemWindowInsetTop.toFloat(),
            left = windowInsets.systemWindowInsetLeft.toFloat(),
            bottom = windowInsets.systemWindowInsetBottom.toFloat(),
            right = windowInsets.systemWindowInsetRight.toFloat(),
        )
    }
}
