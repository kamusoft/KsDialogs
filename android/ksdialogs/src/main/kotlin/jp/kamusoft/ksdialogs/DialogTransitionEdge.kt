package jp.kamusoft.ksdialogs

/**
 * 滑り込み・滑り出しの出入り口になる辺。
 *
 * [START] / [END] はレイアウト方向に追随し、右から左へ読む環境では左右が入れ替わる。
 * [TOP] / [BOTTOM] は物理方向で、レイアウト方向によらない。
 */
public enum class DialogTransitionEdge {
    /** 上辺。 */
    TOP,

    /** 下辺。 */
    BOTTOM,

    /** 行の始まり側の辺 (左から右へ読む環境では左)。 */
    START,

    /** 行の終わり側の辺 (左から右へ読む環境では右)。 */
    END,
}
