package jp.kamusoft.ksdialogs

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewOutlineProvider
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import androidx.annotation.ColorInt
import kotlin.math.roundToInt

/**
 * Toast のデフォルト View (core/ADR-0028・0032)。
 *
 * カスタム Toast View と同じ共有経路 (レイアウト規則・演出) に載るため、器から見るとただの中身の
 * View である。Toast の器は覆いを持たないので、この View が自分でピルの地色と角丸を描く。
 *
 * 見えは OS 標準 Toast の慣習に寄せた「半透明ダークグレーのピル + 白い中央寄せテキスト」で、
 * [ToastStyle] で設定できるのは地色・文字色・文字の大きさ・角丸半径だけである。
 * 余白・最大幅・寄せ・行間、および背後と分けるための弱い落ち影は内蔵コンテンツ側の固定値として持つ。
 *
 * 角丸半径は1行のときにピルに見える固定値で、複数行になっても変えない
 * (高さの半分にすると複数行で卵形になるため)。
 *
 * @param message 表示する文言
 * @param style この表示に採用されたスタイル
 * @param announcer 支援技術への通知口
 */
internal class ToastDefaultContentView(
    context: Context,
    private val message: String,
    style: ToastStyle,
    private val announcer: ToastAccessibilityAnnouncer,
) : TextView(context) {

    /** 読み上げへ流したか。ウィンドウへの出入りが繰り返されても通知は1回だけにする。 */
    private var didAnnounce = false

    /** 直近に反映した最大幅 (px)。同じ値なら測定のたびに設定し直さない。 */
    private var appliedMaxWidth = 0

    init {
        text = message
        gravity = Gravity.CENTER
        setTextColor(style.textColor)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, style.fontSize.toFloat())
        // 行間は文字の大きさに比例させる。長文の折り返しで行が詰まって見えないようにする
        setLineSpacing(0f, LINE_HEIGHT_RATIO)
        val horizontalPadding = dimension(HORIZONTAL_PADDING_DP).roundToInt()
        val verticalPadding = dimension(VERTICAL_PADDING_DP).roundToInt()
        setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dimension(style.cornerRadius.toFloat())
            setColor(style.backgroundColor)
        }
        // 承認済みモックの弱い落ち影。地色の輪郭 (角丸長方形) をそのまま影の形にする。
        // iOS の layer の影 (不透明度 0.25 / ぼかし 5 / 下へ 2) に相当する高さを与える
        outlineProvider = ViewOutlineProvider.BACKGROUND
        elevation = dimension(ELEVATION_DP)
        // Toast はタップで消えず、面へのタッチは背後へ素通しする (core/ADR-0031)
        isClickable = false
        isFocusable = false
        // 表示のたびに読み上げのフォーカスが飛ばないよう、この中身は支援技術の走査対象から外す。
        // メッセージは通知として流す
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        applyMaxWidthFromHost()
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    @Suppress("DEPRECATION")
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (didAnnounce) {
            return
        }
        didAnnounce = true
        // フォーカスを動かすイベント (accessibility focus / window state changed) は使わない
        announcer.announce(this, AccessibilityEvent.TYPE_ANNOUNCEMENT, message)
    }

    /** 取り付け先の面の幅に対する比率で最大幅を決める。面の幅が分からない間は制限しない。 */
    private fun applyMaxWidthFromHost() {
        val hostWidth = rootView?.width ?: 0
        if (hostWidth <= 0) {
            return
        }
        val limit = (hostWidth * MAX_WIDTH_RATIO).roundToInt()
        if (limit == appliedMaxWidth) {
            return
        }
        appliedMaxWidth = limit
        maxWidth = limit
    }

    /** 表示中のメッセージ。観察用。 */
    val displayedText: String
        get() = text.toString()

    /** 表示中のメッセージの文字の大きさ (論理単位 sp)。観察用。 */
    val displayedFontSize: Double
        get() = (textSize / dimension(1f, TypedValue.COMPLEX_UNIT_SP)).toDouble()

    /** 表示中のメッセージの文字色。観察用。 */
    @get:ColorInt
    val displayedTextColor: Int
        get() = currentTextColor

    /** 表示中のピルの地色。観察用。 */
    @get:ColorInt
    val displayedBackgroundColor: Int
        get() = (background as GradientDrawable).color?.defaultColor ?: 0

    /** 表示中のピルの角丸半径 (px)。観察用。 */
    val displayedCornerRadius: Float
        get() = (background as GradientDrawable).cornerRadius

    /** 論理単位の値をこの端末の px へ直す。 */
    private fun dimension(value: Float, unit: Int = TypedValue.COMPLEX_UNIT_DIP): Float =
        TypedValue.applyDimension(unit, value, resources.displayMetrics)

    private companion object {
        /** ピルの左右の余白 (dp)。 */
        const val HORIZONTAL_PADDING_DP = 22f

        /** ピルの上下の余白 (dp)。 */
        const val VERTICAL_PADDING_DP = 11f

        /** 取り付け先の面の幅に対するピルの最大幅の比率。 */
        const val MAX_WIDTH_RATIO = 0.8f

        /** 文字の大きさに対する行の高さの比率。 */
        const val LINE_HEIGHT_RATIO = 1.5f

        /** 落ち影を出すための高さ (dp)。iOS のぼかし半径と同じ値にしてある。 */
        const val ELEVATION_DP = 5f
    }
}
