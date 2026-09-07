package jp.kamusoft.ksdialogs

import android.graphics.Color
import androidx.annotation.ColorInt

/**
 * Toast の一括設定 (core/ADR-0032)。
 *
 * 項目は適用範囲で2種に分かれる。
 *
 * - **視覚項目** ([backgroundColor] / [textColor] / [fontSize] / [cornerRadius]) は
 *   ライブラリ同梱のデフォルト View にだけ効き、カスタム Toast View には効かない
 * - **既定値項目** ([defaultDuration] / [defaultPlacement]) はデフォルト・カスタムを問わず
 *   すべての Toast に効く (表示 API で該当引数・添付を省略したときの既定になる)
 *
 * 供給経路は [KsToast.style] への一括設定だけで、表示 API の引数では渡せない。
 * 器は各表示の受理時にこの値を読むため、設定の変更は次の表示から効く。
 * 色を含むため KMP 共有コードからは設定できず、各 OS 側で設定する (LoadingStyle と同じ非対称)。
 *
 * @property backgroundColor デフォルト View のピルの地色 (ARGB 32bit)
 * @property textColor デフォルト View のメッセージの文字色 (ARGB 32bit)
 * @property fontSize デフォルト View のメッセージの文字の大きさ (論理単位 sp)
 * @property cornerRadius デフォルト View のピルの角丸半径 (論理単位 dp)。
 *   複数行になっても変わらない固定値として扱う
 * @property defaultDuration duration を省略した表示に使うミリ秒。
 *   0 以下を設定した場合は内蔵既定 ([BUILTIN_DEFAULT_DURATION]) へ丸められる
 * @property defaultPlacement アプリ全体の既定配置。null なら Toast の契約既定値
 *   (可視領域の下部中央 + 上方向オフセット)
 */
public data class ToastStyle(
    @param:ColorInt @get:ColorInt public val backgroundColor: Int = BUILTIN_BACKGROUND_COLOR,
    @param:ColorInt @get:ColorInt public val textColor: Int = Color.WHITE,
    public val fontSize: Double = 14.0,
    public val cornerRadius: Double = 22.0,
    public val defaultDuration: Int = BUILTIN_DEFAULT_DURATION,
    public val defaultPlacement: DialogPlacement? = null,
) {
    public companion object {
        /** ライブラリが持つ既定 duration (ミリ秒)。[defaultDuration] が成立しないときの最後の拠り所。 */
        public const val BUILTIN_DEFAULT_DURATION: Int = 1500

        /** デフォルト View のピルの既定の地色。OS 慣習に寄せた半透明のダークグレー。 */
        @ColorInt
        public const val BUILTIN_BACKGROUND_COLOR: Int = 0xEB323232.toInt()
    }
}
