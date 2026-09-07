package jp.kamusoft.ksdialogs

/**
 * 4辺それぞれの余白 (物理ピクセル)。
 *
 * レイアウトの計算は View の座標系に合わせて px で行うため、
 * 論理単位 (dp) の [DialogEdgeInsets] とは型で区別する。
 */
internal data class DialogPixelInsets(
    val top: Float,
    val left: Float,
    val bottom: Float,
    val right: Float,
) {
    companion object {
        /** 4辺すべてが 0 の余白。 */
        val ZERO: DialogPixelInsets = DialogPixelInsets(0f, 0f, 0f, 0f)
    }
}
