package jp.kamusoft.ksdialogs

import android.graphics.Color
import androidx.annotation.ColorInt
import kotlin.math.roundToInt

/**
 * 既定ローディング (ライブラリ同梱の内蔵コンテンツ) の見た目の設定 (core/ADR-0023)。
 *
 * 設定できるのは内蔵コンテンツ固有の項目だけで、器のレイアウト属性 (配置・余白・覆いの色) は
 * [DialogPlacement] / [DialogOptions] が受け持つ。重複した属性はここに置かない。
 *
 * 供給経路は [KsLoading.style] への一括設定だけで、表示 API の引数では渡せない。
 * 器は各表示の開始時にこの値を読むため、設定の変更は次の表示から効く。
 *
 * @property indicatorColor 回転インジケータの色 (ARGB 32bit)
 * @property messageFontSize メッセージの文字の大きさ (論理単位 sp)
 * @property messageColor メッセージの文字色 (ARGB 32bit)
 * @property defaultMessage メッセージを省略して表示したときに使う文言。null ならメッセージなしで表示する
 * @property progressFormat メッセージと進捗から表示テキストを組み立てる関数。
 *   進捗が未報告のときは第2引数が null になる
 */
public data class LoadingStyle(
    @param:ColorInt @get:ColorInt public val indicatorColor: Int = Color.WHITE,
    public val messageFontSize: Double = 14.0,
    @param:ColorInt @get:ColorInt public val messageColor: Int = Color.WHITE,
    public val defaultMessage: String? = null,
    public val progressFormat: (message: String?, progress: Double?) -> String = DEFAULT_PROGRESS_FORMAT,
) {
    public companion object {
        /**
         * ライブラリ既定の組み立て方。
         *
         * 進捗が未報告ならメッセージだけを返し、報告済みなら百分率 (小数点以下は四捨五入) を
         * 改行で続ける。メッセージが無ければ百分率だけを返す。
         */
        public val DEFAULT_PROGRESS_FORMAT: (message: String?, progress: Double?) -> String =
            { message, progress ->
                val trimmedMessage = message?.takeIf { it.isNotEmpty() }
                if (progress == null) {
                    trimmedMessage ?: ""
                } else {
                    val percentage = "${(progress * 100).roundToInt()}%"
                    if (trimmedMessage == null) percentage else "$trimmedMessage\n$percentage"
                }
            }
    }
}
