package jp.kamusoft.ksdialogs

/**
 * Toast の契約既定配置 (core/ADR-0032)。
 *
 * Dialog / Loading の契約既定値 (中央) からの意図的な乖離で、可視領域の下部中央に
 * 標準的なボトムバー1本ぶんの上方向オフセットを加えた位置に置く。
 * オフセットの値は両 OS 共通の単一値で、レイアウト共通ケース表が正となる。
 */
internal object ToastPlacementDefault {
    /** 契約既定の上方向オフセット (論理単位 dp)。 */
    const val BOTTOM_BAR_CLEARANCE = 80.0

    /** 契約既定の配置。show 引数・添付・[ToastStyle.defaultPlacement] のいずれも無いときに使う。 */
    val placement: DialogPlacement = DialogPlacement(
        horizontalAlignment = DialogAlignment.CENTER,
        verticalAlignment = DialogAlignment.END,
        offsetX = 0.0,
        offsetY = -BOTTOM_BAR_CLEARANCE,
    )
}
