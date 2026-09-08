package jp.kamusoft.ksdialogs

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.ColorInt
import kotlin.math.roundToInt

/**
 * ライブラリ同梱の既定ローディングの中身 (core/ADR-0023)。
 *
 * カスタム Loading View と同じ共有経路 (レイアウト規則・演出) に載るため、器から見るとただの中身の
 * View である。見た目の設定は表示の開始時に読んだ [LoadingStyle] から与えられ、表示中の設定変更には
 * 追随しない。
 *
 * 見えは AiForms.Maui.Dialogs の `DefaultLoading` と同じ「覆いに素のインジケータとテキストを直置き」で、
 * 中身自身は背景を持たない (背景の覆いは器が描く)。カード風の装飾が要る用途はカスタム Loading View が
 * 受け持つ。
 *
 * 表示テキストは合流中の最新のメッセージと進捗から器が組み立てて渡すため、この View は渡されたテキストを
 * 映すだけで、組み立て方には関与しない。
 *
 * @param style この表示に採用されたスタイル
 */
internal class LoadingDefaultContentView(
    context: Context,
    style: LoadingStyle,
) : LinearLayout(context) {

    private val indicatorView: ProgressBar =
        ProgressBar(context, null, android.R.attr.progressBarStyleLarge)

    private val messageView: TextView = TextView(context)

    /**
     * 今映しているテキスト。畳んでいる (空の) ときは null。
     *
     * 表示に使った値をこの View 自身が控えるので、実描画を伴わない環境でも
     * 「何が表示されているか」を観察できる。
     */
    @Volatile
    var displayedText: String? = null
        private set

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        // 中身は覆いの上に直接置かれるため、自身は透明のままにする
        setBackgroundColor(Color.TRANSPARENT)

        indicatorView.isIndeterminate = true
        indicatorView.indeterminateTintList = ColorStateList.valueOf(style.indicatorColor)

        messageView.gravity = Gravity.CENTER
        messageView.setTextColor(style.messageColor)
        messageView.setTextSize(TypedValue.COMPLEX_UNIT_SP, style.messageFontSize.toFloat())
        // 覆いの上で読み取りやすいよう、内蔵コンテンツのメッセージは太字で描く。
        // 大きさと色はスタイルの指定に従い、太さは内蔵コンテンツの見えとして固定する
        messageView.setTypeface(messageView.typeface, Typeface.BOLD)
        // 表示テキストは複数行になり得る (進捗つきではメッセージと百分率が別の行になる) ため、
        // 行が詰まって1つの塊に見えないだけの間隔を空ける。
        // 行間はフォーマットの結果 (改行の数) に依らずこの View が与え、フォーマット関数は
        // 表示テキストの文言だけを決める
        messageView.setLineSpacing(dimension(MESSAGE_LINE_SPACING_DP), 1f)

        addView(indicatorView, wrapContentLayoutParams())
        // この View 自身が apply(text) を持つためスコープ関数の apply は使わない
        val messageLayoutParams = wrapContentLayoutParams()
        messageLayoutParams.topMargin = dimension(INDICATOR_MESSAGE_SPACING_DP).roundToInt()
        addView(messageView, messageLayoutParams)
    }

    /** 表示テキストを差し替える。空のテキストではメッセージの行そのものを畳む。 */
    fun apply(text: String) {
        val visible = text.isNotEmpty()
        displayedText = text.takeIf { visible }
        messageView.text = text
        messageView.visibility = if (visible) View.VISIBLE else View.GONE
    }

    /** 表示中のメッセージの文字の大きさ (論理単位 sp)。観察用。 */
    val displayedMessageFontSize: Double
        get() = (messageView.textSize / dimension(1f, TypedValue.COMPLEX_UNIT_SP)).toDouble()

    /** 表示中のメッセージの文字色。観察用。 */
    @get:ColorInt
    val displayedMessageColor: Int
        get() = messageView.currentTextColor

    /** 表示中のメッセージが太字か。観察用。 */
    val displayedMessageIsBold: Boolean
        get() = messageView.typeface?.isBold ?: false

    /** 表示中のメッセージの行間 (px)。観察用。 */
    val displayedMessageLineSpacing: Float
        get() = messageView.lineSpacingExtra

    /** 表示中のインジケータの色。観察用。 */
    @get:ColorInt
    val displayedIndicatorColor: Int?
        get() = indicatorView.indeterminateTintList?.defaultColor

    /** 内容ぴったりの大きさで縦に積むための配置指定。 */
    private fun wrapContentLayoutParams(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )

    /** 論理単位の値をこの端末の px へ直す。 */
    private fun dimension(value: Float, unit: Int = TypedValue.COMPLEX_UNIT_DIP): Float =
        TypedValue.applyDimension(unit, value, resources.displayMetrics)

    private companion object {
        /** インジケータの下端とメッセージの上端の間隔 (dp)。 */
        const val INDICATOR_MESSAGE_SPACING_DP = 20f

        /** メッセージの行間 (dp)。 */
        const val MESSAGE_LINE_SPACING_DP = 8f
    }
}
