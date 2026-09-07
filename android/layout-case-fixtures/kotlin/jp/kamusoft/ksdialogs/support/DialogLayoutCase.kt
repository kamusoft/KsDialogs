package jp.kamusoft.ksdialogs.support

/**
 * 共通ケース表の1ケース。
 *
 * 「画面サイズ・システム領域の余白・内容サイズ・レイアウト属性 → 期待 rect」の組。
 * 数値はすべて論理単位 (dp)。
 */
class DialogLayoutCase(
    val id: String,
    val screen: Size,
    val insets: Insets,
    val contentSize: Size,
    val attributes: DialogLayoutCaseAttributes,
    val expected: Rect,
) {
    /** 幅と高さ。 */
    class Size(val w: Double, val h: Double)

    /** 4辺の余白。 */
    class Insets(val top: Double, val left: Double, val bottom: Double, val right: Double)

    /** 期待する矩形 (原点はウィンドウの左上)。 */
    class Rect(val x: Double, val y: Double, val w: Double, val h: Double) {
        override fun toString(): String = "(x $x, y $y, w $w, h $h)"
    }

    /** 失敗したケースが一目で分かるよう、表示名は ID にする。 */
    override fun toString(): String = id
}
