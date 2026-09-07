package jp.kamusoft.ksdialogs.support

/**
 * 共通ケース表の全体。
 *
 * @property unit 数値の単位の説明 (Android では dp)
 * @property tolerance 座標比較の許容誤差
 * @property cases 全ケース
 */
internal class DialogLayoutCaseTable(
    val unit: String,
    val tolerance: Double,
    val cases: List<DialogLayoutCase>,
)
