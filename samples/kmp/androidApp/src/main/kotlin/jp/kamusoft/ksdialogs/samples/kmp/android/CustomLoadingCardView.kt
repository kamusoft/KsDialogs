package jp.kamusoft.ksdialogs.samples.kmp.android

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import jp.kamusoft.ksdialogs.samples.kmp.CustomLoadingViewModel
import jp.kamusoft.ksdialogs.samples.kmp.SampleText

/**
 * Custom Loading の中身。
 *
 * 覆い (scrim) と中央配置はライブラリの器が受け持つため、このカード自体だけを描く。
 * 進捗は ViewModel の受け口へ転送された値を購読して反映するだけで、報告口を直接受け取ることはない。
 *
 * @param viewModel 共有コードで定義された、進捗を保持する ViewModel
 */
internal class CustomLoadingCardView(
    context: Context,
    private val viewModel: CustomLoadingViewModel,
) : LinearLayout(context) {

    private val progressTrack: FrameLayout
    private val progressFill: View
    private val percentageLabel: TextView

    init {
        orientation = VERTICAL
        background = context.roundedFill(CARD_CORNER_RADIUS_DP, SampleTheme.SURFACE)
        setPadding(
            context.dp(CARD_PADDING_DP),
            context.dp(CARD_PADDING_DP),
            context.dp(CARD_PADDING_DP),
            context.dp(CARD_PADDING_DP),
        )

        addView(titleLabel(context))

        progressFill = View(context).apply {
            background = context.roundedFill(BAR_CORNER_RADIUS_DP, SampleTheme.PRIMARY)
        }
        progressTrack = FrameLayout(context).apply {
            background = context.roundedFill(BAR_CORNER_RADIUS_DP, SampleTheme.SURFACE_VARIANT)
            addView(progressFill, FrameLayout.LayoutParams(0, LayoutParams.MATCH_PARENT))
        }
        addView(
            progressTrack,
            LayoutParams(LayoutParams.MATCH_PARENT, context.dp(BAR_HEIGHT_DP)).apply {
                topMargin = context.dp(ELEMENT_SPACING_DP)
            },
        )

        percentageLabel = TextView(context).apply {
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, PERCENTAGE_TEXT_SIZE_SP)
            setTextColor(SampleTheme.ON_SURFACE_MUTED)
        }
        addView(
            percentageLabel,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = context.dp(ELEMENT_SPACING_DP)
            },
        )

        render(viewModel.progress)
    }

    /** 表示している間だけ進捗の変化を購読する。 */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewModel.onProgressChanged = ::render
        render(viewModel.progress)
    }

    /** 撤去された View が更新を受け取り続けないよう、購読を外す。 */
    override fun onDetachedFromWindow() {
        viewModel.onProgressChanged = null
        super.onDetachedFromWindow()
    }

    /**
     * カードの横幅を固定する。
     *
     * 器は中身を wrap content で配置するため、幅の決定はこの View 自身が行う。
     * 画面が狭い端末では器が確保した幅に収める。
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = context.dp(CARD_WIDTH_DP)
        val width = if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            desiredWidth
        } else {
            minOf(desiredWidth, MeasureSpec.getSize(widthMeasureSpec))
        }
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), heightMeasureSpec)
    }

    /** 進捗を帯の長さと百分率の文言へ反映する。 */
    private fun render(progress: Double) {
        percentageLabel.text = SampleText.progressPercentage(progress)
        if (progressTrack.width == 0) {
            // まだ配置されていないので、最初の配置が済んだ時点で1度だけ塗る。
            // 待ちは1回で打ち切り、幅が決まらないまま繰り返し投函し続けることはしない
            progressTrack.addOnLayoutChangeListener(
                object : View.OnLayoutChangeListener {
                    override fun onLayoutChange(
                        view: View,
                        left: Int,
                        top: Int,
                        right: Int,
                        bottom: Int,
                        oldLeft: Int,
                        oldTop: Int,
                        oldRight: Int,
                        oldBottom: Int,
                    ) {
                        view.removeOnLayoutChangeListener(this)
                        fillProgressBar(progress)
                    }
                },
            )
            return
        }
        fillProgressBar(progress)
    }

    /** 帯の塗り幅を進捗に合わせる。幅が決まっていなければ塗りは 0 になる。 */
    private fun fillProgressBar(progress: Double) {
        progressFill.layoutParams = FrameLayout.LayoutParams(
            (progressTrack.width * progress).toInt(),
            FrameLayout.LayoutParams.MATCH_PARENT,
        )
        progressFill.requestLayout()
    }

    private fun titleLabel(context: Context): TextView =
        TextView(context).apply {
            text = SampleText.CUSTOM_LOADING_TITLE
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, TITLE_TEXT_SIZE_SP)
            setTextColor(SampleTheme.ON_SURFACE)
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

    private companion object {
        const val CARD_WIDTH_DP = 210
        const val CARD_CORNER_RADIUS_DP = 14
        const val CARD_PADDING_DP = 20
        const val ELEMENT_SPACING_DP = 12
        const val TITLE_TEXT_SIZE_SP = 14f
        const val BAR_HEIGHT_DP = 8
        const val BAR_CORNER_RADIUS_DP = 4
        const val PERCENTAGE_TEXT_SIZE_SP = 12f
    }
}
