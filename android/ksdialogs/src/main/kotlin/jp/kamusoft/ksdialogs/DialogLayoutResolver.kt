package jp.kamusoft.ksdialogs

/**
 * レイアウト属性とウィンドウの状況から、軸ごとのレイアウト解を導く。
 *
 * 水平と垂直は独立に解かれ、軸をまたぐ依存はない (core/ADR-0007)。
 * 属性は論理単位 (dp) で与えられ、解は View の座標系に合わせた px で返す。
 */
internal object DialogLayoutResolver {

    /**
     * 水平・垂直の解をまとめて求める。
     *
     * @param layout 丸め済みのレイアウト属性
     * @param windowWidth ウィンドウの幅 (px)
     * @param windowHeight ウィンドウの高さ (px)
     * @param visibleAreaInsets ウィンドウから可視領域を狭めるシステム領域の幅 (px)
     * @param density 論理単位 1dp あたりの px 数
     */
    fun resolve(
        layout: DialogLayout,
        windowWidth: Float,
        windowHeight: Float,
        visibleAreaInsets: DialogPixelInsets,
        density: Float,
    ): DialogLayoutSolution {
        val insets = when (layout.layoutArea) {
            DialogLayoutArea.WINDOW -> DialogPixelInsets.ZERO
            DialogLayoutArea.VISIBLE_AREA -> visibleAreaInsets
        }

        val horizontal = resolveAxis(
            regionOrigin = insets.left,
            regionLength = windowWidth - insets.left - insets.right,
            marginLeading = (layout.dialogMargin.left * density).toFloat(),
            marginTrailing = (layout.dialogMargin.right * density).toFloat(),
            proportion = layout.proportionalWidth,
            alignment = layout.horizontalAlignment,
            offset = (layout.offsetX * density).toFloat(),
        )
        val vertical = resolveAxis(
            regionOrigin = insets.top,
            regionLength = windowHeight - insets.top - insets.bottom,
            marginLeading = (layout.dialogMargin.top * density).toFloat(),
            marginTrailing = (layout.dialogMargin.bottom * density).toFloat(),
            proportion = layout.proportionalHeight,
            alignment = layout.verticalAlignment,
            offset = (layout.offsetY * density).toFloat(),
        )
        return DialogLayoutSolution(horizontal, vertical)
    }

    /**
     * 1軸分を解く。
     *
     * @param regionOrigin 基準 rect の前端 (px)
     * @param regionLength 基準 rect の軸長 (px)。比率サイズの基準になる
     * @param marginLeading 前端側の余白 (px)
     * @param marginTrailing 後端側の余白 (px)
     * @param proportion 比率指定 (null で未指定)
     * @param alignment 配置
     * @param offset 配置後の移動量 (px)
     */
    fun resolveAxis(
        regionOrigin: Float,
        regionLength: Float,
        marginLeading: Float,
        marginTrailing: Float,
        proportion: Double?,
        alignment: DialogAlignment,
        offset: Float,
    ): DialogAxisLayout {
        val effectiveAreaOrigin = regionOrigin + marginLeading
        val effectiveAreaLength = maxOf(0f, regionLength - marginLeading - marginTrailing)

        // サイズは 比率 > fill 配置 > 内容 の優先順で決まる。
        // 比率の基準は余白を控除する前の基準 rect、fill の基準は控除後の有効領域。
        // 内容サイズは View 自身の主張に委ねるため、ここでは null を返す (core/ADR-0014)。
        val size: Float?
        val isFilled: Boolean
        when {
            proportion != null -> {
                size = (proportion * regionLength).toFloat()
                isFilled = false
            }

            alignment == DialogAlignment.FILL -> {
                size = effectiveAreaLength
                isFilled = true
            }

            else -> {
                size = null
                isFilled = false
            }
        }

        // サイズの決め方として fill が採用されなかった軸の fill 配置は中央として扱う。
        val effectiveAlignment =
            if (alignment == DialogAlignment.FILL && !isFilled) DialogAlignment.CENTER else alignment

        return DialogAxisLayout(
            size = size?.let { maxOf(0f, it) },
            maxSize = effectiveAreaLength,
            areaOrigin = effectiveAreaOrigin,
            alignment = effectiveAlignment,
            offset = offset,
        )
    }
}

/**
 * 水平・垂直をそろえたレイアウト解。
 *
 * @property horizontal 水平軸の解
 * @property vertical 垂直軸の解
 */
internal data class DialogLayoutSolution(
    val horizontal: DialogAxisLayout,
    val vertical: DialogAxisLayout,
)
