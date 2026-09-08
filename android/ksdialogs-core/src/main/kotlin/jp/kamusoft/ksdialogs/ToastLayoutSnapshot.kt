package jp.kamusoft.ksdialogs

import android.graphics.Color
import android.view.View

/**
 * Toast の器が使う実効値の供給元を作る。
 *
 * 配置の優先順は「show の placement 引数 > 中身への添付 > ToastStyle のアプリ既定配置 >
 * Toast の契約既定値」で、後ろ2段は [fallbackPlacement] に畳み込まれている (core/ADR-0015・0032)。
 *
 * Toast は覆いも外側タップも持たないため、器メタ属性のうちその2つに関わる項目は添付を読まずに
 * 固定する。残るレイアウト系の項目 (基準領域・余白・比率指定) は Dialog と同じ意味で効く
 * (core/ADR-0031)。
 *
 * @param contentView 添付の読み取り元になる中身の View
 * @param showPlacement show の引数で渡された配置。null なら添付が使われる
 * @param fallbackPlacement show 引数も添付も無いときに採る配置
 */
internal fun toastLayoutSnapshot(
    contentView: View,
    showPlacement: DialogPlacement?,
    fallbackPlacement: DialogPlacement,
): DialogLayoutSnapshot = DialogLayoutSnapshot(
    {
        val attached = contentView.ksDialogOptions ?: DialogOptions()
        DialogLayoutSupply(
            options = attached.copy(
                overlayColor = Color.TRANSPARENT,
                isCanceledOnTouchOutside = false,
            ),
            placement = showPlacement ?: contentView.ksDialogPlacement ?: fallbackPlacement,
        )
    },
)
