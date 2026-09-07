package jp.kamusoft.ksdialogs.samples.android

import android.content.Context
import android.graphics.drawable.GradientDrawable

/** dp を実ピクセルへ換算する。 */
internal fun Context.dp(value: Int): Int =
    (value * resources.displayMetrics.density).toInt()

/** 角丸の塗りつぶし背景を作る。 */
internal fun Context.roundedFill(cornerRadiusDp: Int, color: Int): GradientDrawable =
    GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(cornerRadiusDp).toFloat()
        setColor(color)
    }

/** 角丸の塗りつぶしに枠線を重ねた背景を作る。 */
internal fun Context.roundedFillStroke(
    cornerRadiusDp: Int,
    fillColor: Int,
    strokeWidthDp: Int,
    strokeColor: Int,
): GradientDrawable =
    GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(cornerRadiusDp).toFloat()
        setColor(fillColor)
        setStroke(dp(strokeWidthDp), strokeColor)
    }

/** 角丸の枠線だけの背景を作る。 */
internal fun Context.roundedStroke(cornerRadiusDp: Int, strokeWidthDp: Int, color: Int): GradientDrawable =
    GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(cornerRadiusDp).toFloat()
        setStroke(dp(strokeWidthDp), color)
    }
