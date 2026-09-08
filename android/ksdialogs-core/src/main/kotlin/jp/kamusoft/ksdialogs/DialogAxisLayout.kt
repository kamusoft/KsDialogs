package jp.kamusoft.ksdialogs

/**
 * 1軸 (水平 または 垂直) 分のレイアウト解 (px)。
 *
 * サイズが内容に委ねられている軸では最終的な位置が測定の後にしか決まらないため、
 * 位置は値ではなく [originFor] で求める。
 *
 * @property size 決まったサイズ。null なら内容サイズに委ねる
 * @property maxSize 有効領域の軸長。どの決め方をしてもサイズはこの値で頭打ちになる
 * @property areaOrigin 有効領域の前端 (ウィンドウの左端 / 上端からの距離)
 * @property alignment 配置。fill が採用されなかった軸では [DialogAlignment.CENTER] に畳み込み済み
 * @property offset 配置を決めた後に加える移動量
 */
internal data class DialogAxisLayout(
    val size: Float?,
    val maxSize: Float,
    val areaOrigin: Float,
    val alignment: DialogAlignment,
    val offset: Float,
) {
    /**
     * 実際のサイズから、その軸の前端 (左端 / 上端) の位置を求める。
     *
     * 移動量は配置によらず同じ向きに加え、有効領域の外へ出てもクランプしない (core/ADR-0008)。
     */
    fun originFor(actualSize: Float): Float {
        val anchored = when (alignment) {
            DialogAlignment.START, DialogAlignment.FILL -> areaOrigin
            DialogAlignment.CENTER -> areaOrigin + (maxSize - actualSize) / 2f
            DialogAlignment.END -> areaOrigin + maxSize - actualSize
        }
        return anchored + offset
    }
}
