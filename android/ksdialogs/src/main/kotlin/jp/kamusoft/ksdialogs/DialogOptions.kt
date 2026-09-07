package jp.kamusoft.ksdialogs

import androidx.annotation.ColorInt

/**
 * ダイアログの器に渡す静的メタ属性。
 *
 * その View の性質として決まる値の組であり、表示のたびに変えるものではない。
 * そのため供給経路はダイアログの中身 (View) への添付だけで、show の引数では渡せない
 * (core/ADR-0014・core/ADR-0015)。
 *
 * 全フィールドに既定値があるため、何も設定しなければ契約の既定値で動く。
 * 数値はすべて論理単位 (dp)。無効値 (非有限値・負の余白・範囲外の比率) は器が採用する前に
 * 正規化されるので、指定した値がそのまま計算に使われるとは限らない。
 *
 * @property layoutArea サイズと位置の計算の基準になる領域。既定は可視領域
 * @property dialogMargin 基準 rect の各辺から控除する余白。最大サイズと配置の両方に効く。既定は全辺 24。
 *   負の辺はその辺だけ 0 に、非有限値の辺は既定値に丸める
 * @property proportionalWidth 基準 rect の幅に対する比率。0 < 値 ≤ 1 で比率指定が成立する。
 *   1 を超える値は 1 に丸め、0 以下と非有限値はすべて未指定として扱う。既定は未指定 (-1)
 * @property proportionalHeight 基準 rect の高さに対する比率。扱いは [proportionalWidth] と同じ
 * @property overlayColor ダイアログの背後を覆う色 (ARGB 32bit)。既定は黒の 40% 不透明
 * @property isCanceledOnTouchOutside ダイアログ外形 rect の外側へのタップで、キャンセルと同じ経路で閉じるか。
 *   既定は true。false のとき外側タップは何も起こさず、タップは背後の画面へ透過しない
 */
public data class DialogOptions(
    public val layoutArea: DialogLayoutArea = DialogLayoutArea.VISIBLE_AREA,
    public val dialogMargin: DialogEdgeInsets = DialogEdgeInsets(24.0),
    public val proportionalWidth: Double = -1.0,
    public val proportionalHeight: Double = -1.0,
    @param:ColorInt @get:ColorInt public val overlayColor: Int = 0x66000000,
    public val isCanceledOnTouchOutside: Boolean = true,
)
