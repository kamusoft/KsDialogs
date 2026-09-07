package jp.kamusoft.ksdialogs.samples.kmp.android

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import jp.kamusoft.ksdialogs.samples.kmp.SampleEasingPreset
import jp.kamusoft.ksdialogs.samples.kmp.SampleText
import jp.kamusoft.ksdialogs.samples.kmp.SampleTransitionPreset

/**
 * 出入りの演出を選んでからダイアログを表示する画面。
 *
 * 初期値はプリセット `Fade`・時間 250 ms・イージング `Standard`。
 *
 * @param onBack メニュー画面へ戻る操作
 * @param onShow 選んだ演出でダイアログを出す操作
 */
internal class SampleTransitionPanelView(
    context: Context,
    onBack: () -> Unit,
    onShow: () -> Unit,
) : LinearLayout(context) {

    private val presetChips = SampleChipsView(
        context = context,
        labels = SampleTransitionChoice.entries.map { it.label },
        columns = PRESET_COLUMNS,
        minHeightDp = PRESET_CHIP_MIN_HEIGHT_DP,
        textSizeSp = PRESET_CHIP_TEXT_SIZE_SP,
        onSelected = { refreshAdjustAvailability() },
    )
    private val easingChips = SampleChipsView(
        context = context,
        labels = SampleEasingChoice.entries.map { it.label },
        columns = SampleEasingChoice.entries.size,
        minHeightDp = EASING_CHIP_MIN_HEIGHT_DP,
        textSizeSp = EASING_CHIP_TEXT_SIZE_SP,
        onSelected = { },
    )
    private val durationValueLabel: TextView
    private val durationSlider: SeekBar
    private val adjustBlock: LinearLayout
    private val resultValue: TextView
    private val resultArea: LinearLayout

    init {
        durationValueLabel = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, ROW_TEXT_SIZE_SP)
            setTextColor(SampleTheme.ON_SURFACE_MUTED)
            typeface = Typeface.MONOSPACE
            text = SampleText.durationValue(INITIAL_DURATION_MILLIS)
        }
        durationSlider = createDurationSlider(context)
        adjustBlock = createAdjustBlock(context)

        orientation = VERTICAL
        setBackgroundColor(SampleTheme.SURFACE)
        fitsSystemWindows = true

        addView(header(context, onBack))
        addView(divider(context))

        resultValue = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, RESULT_VALUE_TEXT_SIZE_SP)
            setTextColor(SampleTheme.ON_SURFACE)
            typeface = Typeface.MONOSPACE
        }
        resultArea = resultAreaView(context, resultValue)

        val scrollContent = LinearLayout(context).apply {
            orientation = VERTICAL
            addView(sectionCaption(context, SampleText.PRESET_CAPTION))
            addView(
                presetChips,
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                    leftMargin = context.dp(HORIZONTAL_PADDING_DP)
                    rightMargin = context.dp(HORIZONTAL_PADDING_DP)
                },
            )
            addView(sectionCaption(context, SampleText.ADJUST_CAPTION))
            addView(adjustBlock)
            addView(
                resultArea,
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                    topMargin = context.dp(RESULT_TOP_SPACING_DP)
                },
            )
        }
        addView(
            ScrollView(context).apply { addView(scrollContent) },
            LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f),
        )

        addView(divider(context))
        addView(footer(context, onShow))

        refreshAdjustAvailability()
    }

    /** 選んだ演出。 */
    fun preset(): SampleTransitionPreset =
        SampleTransitionChoice.entries[presetChips.selectedIndex].preset

    /** 選んだイージング。 */
    fun easing(): SampleEasingPreset =
        SampleEasingChoice.entries[easingChips.selectedIndex].preset

    /** 選んだ時間 (ミリ秒)。 */
    fun durationMilliseconds(): Int =
        MIN_DURATION_MILLIS + durationSlider.progress * DURATION_STEP_MILLIS

    /** 直近の結果を表示する。 */
    fun showResult(result: String) {
        resultValue.text = result
        resultArea.visibility = View.VISIBLE
    }

    /**
     * 調整部を操作できるかを選択中の演出に合わせる。
     *
     * 無演出と自作フックは調整値を使わないため、値は保ったまま操作できなくする。
     */
    private fun refreshAdjustAvailability() {
        val allowsAdjustments = SampleTransitionChoice.entries[presetChips.selectedIndex].usesAdjustments
        adjustBlock.alpha = if (allowsAdjustments) 1f else DISABLED_ALPHA
        adjustBlock.setDescendantsEnabled(allowsAdjustments)
    }

    private fun View.setDescendantsEnabled(enabled: Boolean) {
        isEnabled = enabled
        if (this is ViewGroup) {
            for (index in 0 until childCount) {
                getChildAt(index).setDescendantsEnabled(enabled)
            }
        }
    }

    private fun header(context: Context, onBack: () -> Unit): LinearLayout =
        LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                context.dp(HORIZONTAL_PADDING_DP),
                context.dp(TITLE_TOP_PADDING_DP),
                context.dp(HORIZONTAL_PADDING_DP),
                context.dp(TITLE_BOTTOM_PADDING_DP),
            )

            val back = TextView(context).apply {
                text = BACK_CHEVRON
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, BACK_TEXT_SIZE_SP)
                setTextColor(SampleTheme.ON_SURFACE)
                // 記号1文字でも押しやすさの下限を満たす領域を確保する
                minimumWidth = context.dp(ACTION_MIN_SIZE_DP)
                minimumHeight = context.dp(ACTION_MIN_SIZE_DP)
                isClickable = true
                isFocusable = true
                // 記号のままでは読み上げが操作の意味を伝えないため、名前と役割を与える
                contentDescription = BACK_DESCRIPTION
                accessibilityDelegate = SampleButtonRoleDelegate
                setOnClickListener { onBack() }
            }
            val title = TextView(context).apply {
                text = SampleText.TRANSITION_DIALOG_ITEM
                setTextSize(TypedValue.COMPLEX_UNIT_SP, TITLE_TEXT_SIZE_SP)
                setTextColor(SampleTheme.ON_SURFACE)
                typeface = Typeface.DEFAULT_BOLD
            }

            addView(back)
            addView(
                title,
                LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                    leftMargin = context.dp(TITLE_LEADING_GAP_DP)
                },
            )
        }

    private fun footer(context: Context, onShow: () -> Unit): LinearLayout =
        LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(
                context.dp(HORIZONTAL_PADDING_DP),
                context.dp(FOOTER_TOP_PADDING_DP),
                context.dp(HORIZONTAL_PADDING_DP),
                context.dp(FOOTER_BOTTOM_PADDING_DP),
            )

            val show = Button(context).apply {
                text = SampleText.DISPLAY_ACTION
                isAllCaps = false
                setTextSize(TypedValue.COMPLEX_UNIT_SP, SHOW_TEXT_SIZE_SP)
                setTextColor(SampleTheme.ON_PRIMARY)
                typeface = Typeface.DEFAULT_BOLD
                background = context.roundedFill(SHOW_CORNER_RADIUS_DP, SampleTheme.PRIMARY)
                // 既定のボタンは押下時に浮き上がる。平面的な見た目に合わせて外す
                stateListAnimator = null
                minWidth = 0
                minimumWidth = 0
                minHeight = context.dp(ACTION_MIN_SIZE_DP)
                minimumHeight = context.dp(ACTION_MIN_SIZE_DP)
                setOnClickListener { onShow() }
            }
            addView(show, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        }

    private fun sectionCaption(context: Context, title: String): TextView =
        TextView(context).apply {
            text = title
            setTextSize(TypedValue.COMPLEX_UNIT_SP, CAPTION_TEXT_SIZE_SP)
            setTextColor(SampleTheme.ON_SURFACE_MUTED)
            setPadding(
                context.dp(HORIZONTAL_PADDING_DP),
                context.dp(CAPTION_TOP_PADDING_DP),
                context.dp(HORIZONTAL_PADDING_DP),
                context.dp(CAPTION_BOTTOM_PADDING_DP),
            )
        }

    private fun createAdjustBlock(context: Context): LinearLayout =
        LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(context.dp(HORIZONTAL_PADDING_DP), 0, context.dp(HORIZONTAL_PADDING_DP), 0)

            val durationRow = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                val label = TextView(context).apply {
                    text = SampleText.DURATION_LABEL
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, ROW_TEXT_SIZE_SP)
                    setTextColor(SampleTheme.ON_SURFACE)
                }
                addView(label, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
                addView(durationValueLabel)
            }
            addView(durationRow, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
            addView(
                durationSlider,
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                    topMargin = context.dp(SLIDER_TOP_SPACING_DP)
                },
            )

            val easingLabel = TextView(context).apply {
                text = SampleText.EASING_LABEL
                setTextSize(TypedValue.COMPLEX_UNIT_SP, ROW_TEXT_SIZE_SP)
                setTextColor(SampleTheme.ON_SURFACE)
            }
            addView(
                easingLabel,
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    topMargin = context.dp(EASING_LABEL_TOP_SPACING_DP)
                },
            )
            addView(
                easingChips,
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                    topMargin = context.dp(EASING_CHIPS_TOP_SPACING_DP)
                },
            )
        }

    /**
     * 時間を選ぶスライダを作る。
     *
     * 位置は 10 ms 刻みの目盛りで持ち、[durationMilliseconds] で実時間へ言い換える。
     * 行の項目名は別の View なので、スライダ自身にも読み上げ名を与える
     * (現在値はスライダの役割としてそのまま読み上げ情報に乗る)。
     */
    private fun createDurationSlider(context: Context): SeekBar = SeekBar(context).apply {
        max = (MAX_DURATION_MILLIS - MIN_DURATION_MILLIS) / DURATION_STEP_MILLIS
        progress = (INITIAL_DURATION_MILLIS - MIN_DURATION_MILLIS) / DURATION_STEP_MILLIS
        contentDescription = SampleText.DURATION_LABEL
        // 既定のトラックと摘みは端末の配色に従うため、共有の配色で塗り直す
        progressTintList = ColorStateList.valueOf(SampleTheme.PRIMARY)
        progressBackgroundTintList = ColorStateList.valueOf(SampleTheme.DIVIDER)
        thumbTintList = null
        thumb = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(SampleTheme.SURFACE)
            setStroke(context.dp(THUMB_STROKE_WIDTH_DP), SampleTheme.DIVIDER)
            setSize(context.dp(THUMB_SIZE_DP), context.dp(THUMB_SIZE_DP))
        }
        splitTrack = false
        setPadding(context.dp(THUMB_SIZE_DP / 2), 0, context.dp(THUMB_SIZE_DP / 2), 0)
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                durationValueLabel.text = SampleText.durationValue(durationMilliseconds())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
        })
    }

    private fun divider(context: Context): View =
        View(context).apply {
            setBackgroundColor(SampleTheme.DIVIDER)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, context.dp(DIVIDER_HEIGHT_DP))
        }

    private fun resultAreaView(context: Context, valueLabel: TextView): LinearLayout =
        LinearLayout(context).apply {
            orientation = VERTICAL
            // 一度もダイアログを閉じていない間は表示するものがない
            visibility = View.GONE
            setBackgroundColor(SampleTheme.SURFACE_VARIANT)
            setPadding(
                context.dp(HORIZONTAL_PADDING_DP),
                context.dp(RESULT_VERTICAL_PADDING_DP),
                context.dp(HORIZONTAL_PADDING_DP),
                context.dp(RESULT_VERTICAL_PADDING_DP),
            )

            val caption = TextView(context).apply {
                text = SampleText.RESULT_CAPTION
                setTextSize(TypedValue.COMPLEX_UNIT_SP, RESULT_CAPTION_TEXT_SIZE_SP)
                setTextColor(SampleTheme.ON_SURFACE_MUTED)
            }
            addView(caption)
            addView(
                valueLabel,
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    topMargin = context.dp(RESULT_CAPTION_SPACING_DP)
                },
            )
        }

    private companion object {
        const val BACK_CHEVRON = "‹"
        const val BACK_DESCRIPTION = "戻る"
        const val PRESET_COLUMNS = 2
        const val MIN_DURATION_MILLIS = 100
        const val MAX_DURATION_MILLIS = 600
        const val DURATION_STEP_MILLIS = 10
        const val INITIAL_DURATION_MILLIS = 250
        const val DISABLED_ALPHA = 0.4f
        const val HORIZONTAL_PADDING_DP = 16
        const val TITLE_TOP_PADDING_DP = 20
        const val TITLE_BOTTOM_PADDING_DP = 12
        const val TITLE_TEXT_SIZE_SP = 17f
        const val TITLE_LEADING_GAP_DP = 8
        const val BACK_TEXT_SIZE_SP = 20f
        const val ACTION_MIN_SIZE_DP = 48
        const val CAPTION_TEXT_SIZE_SP = 12f
        const val CAPTION_TOP_PADDING_DP = 12
        const val CAPTION_BOTTOM_PADDING_DP = 8
        const val PRESET_CHIP_MIN_HEIGHT_DP = 48
        const val PRESET_CHIP_TEXT_SIZE_SP = 14f
        const val EASING_CHIP_MIN_HEIGHT_DP = 36
        const val EASING_CHIP_TEXT_SIZE_SP = 12f
        const val ROW_TEXT_SIZE_SP = 14f
        const val SLIDER_TOP_SPACING_DP = 6
        const val EASING_LABEL_TOP_SPACING_DP = 14
        const val EASING_CHIPS_TOP_SPACING_DP = 6
        const val THUMB_SIZE_DP = 20
        const val THUMB_STROKE_WIDTH_DP = 1
        const val DIVIDER_HEIGHT_DP = 1
        const val SHOW_TEXT_SIZE_SP = 14f
        const val SHOW_CORNER_RADIUS_DP = 10
        const val FOOTER_TOP_PADDING_DP = 12
        const val FOOTER_BOTTOM_PADDING_DP = 16
        const val RESULT_TOP_SPACING_DP = 16
        const val RESULT_VERTICAL_PADDING_DP = 14
        const val RESULT_CAPTION_TEXT_SIZE_SP = 12f
        const val RESULT_CAPTION_SPACING_DP = 4
        const val RESULT_VALUE_TEXT_SIZE_SP = 14f
    }
}
