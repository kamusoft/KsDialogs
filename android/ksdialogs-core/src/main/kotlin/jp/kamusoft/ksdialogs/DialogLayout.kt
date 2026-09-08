package jp.kamusoft.ksdialogs

/**
 * 供給を合成し終えた実効値。
 *
 * 未指定を表す値 (0 以下の比率・非有限値) はここで null へ畳み込まれるため、
 * 以降の計算は「指定があるかどうか」だけを見ればよい。
 *
 * @param options 静的メタ属性の実効値
 * @param placement 動的メタ属性の実効値
 */
internal class DialogLayout(
    options: DialogOptions = DialogOptions(),
    placement: DialogPlacement = DialogPlacement(),
) {

    /** 幅の比率指定 (dp 換算前の割合)。null なら未指定。 */
    val proportionalWidth: Double? = normalizedProportion(options.proportionalWidth)

    /** 高さの比率指定。null なら未指定。 */
    val proportionalHeight: Double? = normalizedProportion(options.proportionalHeight)

    /** 水平方向の配置。 */
    val horizontalAlignment: DialogAlignment = placement.horizontalAlignment

    /** 垂直方向の配置。 */
    val verticalAlignment: DialogAlignment = placement.verticalAlignment

    /** 水平方向の移動量 (dp)。 */
    val offsetX: Double = normalizedOffset(placement.offsetX)

    /** 垂直方向の移動量 (dp)。 */
    val offsetY: Double = normalizedOffset(placement.offsetY)

    /** 基準 rect から控除する余白 (dp)。 */
    val dialogMargin: DialogEdgeInsets = normalizedMargin(options.dialogMargin)

    /** サイズと位置の計算の基準になる領域。 */
    val layoutArea: DialogLayoutArea = options.layoutArea

    /** 背後を覆う色 (ARGB 32bit)。 */
    val overlayColor: Int = options.overlayColor

    /** 外側タップをキャンセルとして扱うか。 */
    val isCanceledOnTouchOutside: Boolean = options.isCanceledOnTouchOutside

    companion object {
        /** 契約が定める余白の既定値。非有限値の辺を戻す先になる。 */
        private val DEFAULT_MARGIN: DialogEdgeInsets = DialogEdgeInsets(24.0)

        /** 比率指定を有効域 0 < 値 ≤ 1 に収める。0 以下と非有限値は未指定。 */
        private fun normalizedProportion(value: Double): Double? =
            if (value.isFinite() && value > 0.0) minOf(value, 1.0) else null

        /** 移動量を有限値に限る。非有限値は既定値の 0。 */
        private fun normalizedOffset(value: Double): Double =
            if (value.isFinite()) value else 0.0

        /** 余白を辺ごとに丸める。負の辺は下限の 0 へ、非有限値の辺は既定値へ戻す。 */
        private fun normalizedMargin(margin: DialogEdgeInsets): DialogEdgeInsets =
            DialogEdgeInsets(
                top = normalizedMarginEdge(margin.top, DEFAULT_MARGIN.top),
                left = normalizedMarginEdge(margin.left, DEFAULT_MARGIN.left),
                bottom = normalizedMarginEdge(margin.bottom, DEFAULT_MARGIN.bottom),
                right = normalizedMarginEdge(margin.right, DEFAULT_MARGIN.right),
            )

        private fun normalizedMarginEdge(value: Double, defaultValue: Double): Double =
            if (value.isFinite()) maxOf(0.0, value) else defaultValue
    }
}
