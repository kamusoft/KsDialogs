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
 * 基準領域が表示中のページのときは、ページの基準矩形 (画面座標) をこの面の4辺からの幅へ直して
 * 計算へ渡す。ページの矩形と器は別々のウィンドウに属し原点が違い得るため、両者を画面座標で
 * 突き合わせる。ページへの問い合わせは表示の開始時と、ウィンドウの寸法・可視領域の余白が
 * 変わったときだけ行う (再配置のきっかけはこの2つだけ)。
 *
 * きっかけと同じ測定の中では、この面の画面上の位置 (ウィンドウの移動の反映) やページ側の
 * レイアウト (別ウィンドウの置き直し) がまだ済んでいないことがある。そのため同じきっかけに対して、
 * 測定のあとの UI スレッドの番でもう一度だけ問い合わせ、結果が違えば置き直す。
 *
 * @param snapshot 実効値の供給元。初回レイアウトパスの完了時点で固定される
 * @param contentView 中身として表示する View
 * @param visibleAreaInsets 可視領域を狭めるシステム領域の幅 (px) を求める方法。
 *   既定はウィンドウが報告する値
 * @param currentPageResolver 基準領域が表示中のページのときに使う取得元の並び。
 *   既定は生成時点 (表示の開始時) の登録内容で固定したもの
 * @param pageWindowRoot ダイアログを出す画面 (Activity) のウィンドウの根の View を求める方法。
 *   既定はこの面の Context が属する Activity のもの
 * @param onLayoutSnapshotFrozen 実効値を固定した時点で呼ばれる。器が出入りの進行を始める合図になる
 */
internal class DialogLayoutHost(
    context: Context,
    private val snapshot: DialogLayoutSnapshot,
    contentView: View,
    private val visibleAreaInsets: (View) -> DialogPixelInsets = ::windowVisibleAreaInsets,
    private val currentPageResolver: DialogCurrentPageResolver = DialogCurrentPageResolver.capturingRegistration(),
    private val pageWindowRoot: (View) -> View? = ::presentingActivityWindowRoot,
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

    /** 表示中のページへ直近に問い合わせたときのウィンドウの状況。ページを使っていなければ null。 */
    private var appliedPageQueryInput: PageQueryInput? = null

    /** 今の配置に使っている、表示中のページの4辺の幅 (px)。ページが得られていなければ null。 */
    private var appliedCurrentPageInsets: DialogPixelInsets? = null

    /** 基準領域が表示中のページなのにページが得られず、可視領域へ落としたときの診断 (出した順)。 */
    val currentPageDiagnostics: List<String>
        get() = recordedCurrentPageDiagnostics

    private val recordedCurrentPageDiagnostics = mutableListOf<String>()

    /** きっかけのあとの、もう一度の問い合わせ。新しいきっかけが来たら置き換える。 */
    private val settlingPageQuery = Runnable { requeryCurrentPage() }

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
            // 最初の測定は位置が確定する前に走ることがあるため、位置の確定したこの時点で
            // ページへもう一度問い合わせ、違っていれば描画を見送って置き直す
            if (remainingConvergencePasses > 0 && requeryCurrentPage()) {
                remainingConvergencePasses--
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
        removeCallbacks(settlingPageQuery)
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
        val visibleInsets = visibleAreaInsets(this)
        val resolved = DialogLayoutResolver.resolve(
            layout = layout,
            windowWidth = windowWidth.toFloat(),
            windowHeight = windowHeight.toFloat(),
            visibleAreaInsets = visibleInsets,
            density = resources.displayMetrics.density,
            currentPageInsets = currentPageInsetsFor(layout, windowWidth, windowHeight, visibleInsets),
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
     * 基準領域が表示中のページのときに計算へ渡す、ページの4辺の幅 (px)。
     *
     * ウィンドウの寸法と可視領域の余白が前回の問い合わせと同じなら、問い合わせずに前回の結果を使う。
     * 変わっていれば問い合わせ直す。ページが得られなければ null を返し、基準は可視領域に落ちる。
     */
    private fun currentPageInsetsFor(
        layout: DialogLayout,
        windowWidth: Int,
        windowHeight: Int,
        visibleInsets: DialogPixelInsets,
    ): DialogPixelInsets? {
        if (layout.layoutArea != DialogLayoutArea.CURRENT_PAGE) {
            appliedPageQueryInput = null
            appliedCurrentPageInsets = null
            removeCallbacks(settlingPageQuery)
            return null
        }
        // ウィンドウに載る前 (提示前の測定) はページとの位置関係が決まらないため問い合わせない
        if (!isAttachedToWindow) {
            return null
        }
        val input = PageQueryInput(windowWidth, windowHeight, visibleInsets)
        if (input == appliedPageQueryInput) {
            return appliedCurrentPageInsets
        }
        appliedPageQueryInput = input
        appliedCurrentPageInsets = queryCurrentPageInsets(input, reportsUnresolved = true)
        removeCallbacks(settlingPageQuery)
        post(settlingPageQuery)
        return appliedCurrentPageInsets
    }

    /**
     * 直近の問い合わせと同じウィンドウの状況のまま、ページへ問い合わせ直す。
     *
     * 結果が違えば採り直してレイアウトを要求し、true を返す。ページが得られなかったときは
     * 直前の結果を保ち、診断も重ねて出さない。
     */
    private fun requeryCurrentPage(): Boolean {
        val input = appliedPageQueryInput ?: return false
        val insets = queryCurrentPageInsets(input, reportsUnresolved = false) ?: return false
        if (insets == appliedCurrentPageInsets) {
            return false
        }
        appliedCurrentPageInsets = insets
        requestLayout()
        return true
    }

    /**
     * 表示中のページの基準矩形を、この面の4辺からの幅 (px) に直す。
     *
     * 基準はページの矩形と可視領域の共通部分なので、辺ごとに可視領域の余白以上にする。
     * ページの矩形は画面座標で届くため、この面の画面上の位置を引いてこの面の座標へ写す。
     *
     * @param reportsUnresolved ページが得られなかった理由を診断ログに出すか
     */
    private fun queryCurrentPageInsets(input: PageQueryInput, reportsUnresolved: Boolean): DialogPixelInsets? =
        when (val resolution = currentPageResolver.resolve(pageWindowRoot(this))) {
            is DialogCurrentPageResolution.Resolved -> {
                val origin = IntArray(2).also(::getLocationOnScreen)
                val page = resolution.rect.offset(-origin[0].toFloat(), -origin[1].toFloat())
                val visible = input.visibleInsets
                DialogPixelInsets(
                    top = maxOf(visible.top, page.top),
                    left = maxOf(visible.left, page.left),
                    bottom = maxOf(visible.bottom, input.windowHeight - page.bottom),
                    right = maxOf(visible.right, input.windowWidth - page.right),
                )
            }

            is DialogCurrentPageResolution.Unresolved -> {
                if (reportsUnresolved) {
                    val message = resolution.diagnosticMessage
                    recordedCurrentPageDiagnostics += message
                    Log.w(LOG_TAG, message, resolution.cause)
                }
                null
            }
        }

    /**
     * 表示中のページへの問い合わせのきっかけになるウィンドウの状況。
     *
     * @property windowWidth ウィンドウの幅 (px)
     * @property windowHeight ウィンドウの高さ (px)
     * @property visibleInsets 可視領域の余白 (px)
     */
    private data class PageQueryInput(
        val windowWidth: Int,
        val windowHeight: Int,
        val visibleInsets: DialogPixelInsets,
    )

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

/** この面を載せた画面 (Activity) のウィンドウの根の View。画面に紐づかない Context なら null。 */
internal fun presentingActivityWindowRoot(view: View): View? =
    view.context.hostActivity()?.window?.peekDecorView()

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
