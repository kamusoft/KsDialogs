package jp.kamusoft.ksdialogs

/**
 * 4辺それぞれの余白 (論理単位 dp)。
 *
 * @property top 上辺の余白
 * @property left 左辺の余白
 * @property bottom 下辺の余白
 * @property right 右辺の余白
 */
public data class DialogEdgeInsets(
    public val top: Double,
    public val left: Double,
    public val bottom: Double,
    public val right: Double,
) {
    /** 4辺すべてに同じ値を与える。 */
    public constructor(all: Double) : this(all, all, all, all)

    public companion object {
        /** 4辺すべてが 0 の余白。 */
        public val ZERO: DialogEdgeInsets = DialogEdgeInsets(0.0)
    }
}
