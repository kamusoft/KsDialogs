package jp.kamusoft.ksdialogs.samples.android

import android.content.Context
import android.graphics.Typeface
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.widget.EditText
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * 移動量を入れる数値欄。
 *
 * Compose の入力欄のキーボード指定には符号付き数値の選択肢がなく、負値を打てないため、
 * 符号付き数値の入力種別を持てる [EditText] を載せる。
 *
 * @param label この欄が属する行の項目名。画面には出さず、読み上げ名として与える。
 *   入力中の値は欄の文字列としてそのまま読み上げ情報に乗る
 * @param text 欄を作るときの初期文字列
 * @param onTextChange 入力が変わったときの受け口
 */
@Composable
internal fun SampleOffsetField(label: String, text: String, onTextChange: (String) -> Unit) {
    AndroidView(
        modifier = Modifier.size(FIELD_WIDTH_DP.dp, FIELD_HEIGHT_DP.dp),
        factory = { context -> offsetEditText(context, label, text, onTextChange) },
    )
}

private fun offsetEditText(
    context: Context,
    label: String,
    text: String,
    onTextChange: (String) -> Unit,
): EditText = EditText(context).apply {
    setText(text)
    contentDescription = label
    inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
    // 1 行入力の既定の横スクロールのままだと、Compose が欄の寸法を確定させる前に組んだ文字の配置が残り、
    // 右寄せの初期値が欄の外へ出て見えなくなる (API 35 で実測)。桁数の少ない数値欄なので横スクロールを使わない
    setHorizontallyScrolling(false)
    maxLines = 1
    gravity = Gravity.END or Gravity.CENTER_VERTICAL
    setTextSize(TypedValue.COMPLEX_UNIT_SP, FIELD_TEXT_SIZE_SP)
    setTextColor(SampleTheme.ON_SURFACE)
    typeface = Typeface.MONOSPACE
    background = context.roundedStroke(FIELD_CORNER_RADIUS_DP, FIELD_STROKE_WIDTH_DP, SampleTheme.DIVIDER)
    setPadding(context.dp(FIELD_HORIZONTAL_PADDING_DP), 0, context.dp(FIELD_HORIZONTAL_PADDING_DP), 0)
    addTextChangedListener(
        object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                onTextChange(s?.toString().orEmpty())
            }
        },
    )
}

private const val FIELD_WIDTH_DP = 64
private const val FIELD_HEIGHT_DP = 36
private const val FIELD_CORNER_RADIUS_DP = 8
private const val FIELD_STROKE_WIDTH_DP = 1
private const val FIELD_HORIZONTAL_PADDING_DP = 10
private const val FIELD_TEXT_SIZE_SP = 14f
