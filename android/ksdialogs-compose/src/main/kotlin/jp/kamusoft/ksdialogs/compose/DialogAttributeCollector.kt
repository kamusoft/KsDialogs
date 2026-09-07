package jp.kamusoft.ksdialogs.compose

import androidx.compose.runtime.staticCompositionLocalOf
import jp.kamusoft.ksdialogs.DialogOptions
import jp.kamusoft.ksdialogs.DialogPlacement
import jp.kamusoft.ksdialogs.DialogTransition

/**
 * 中身の composable が宣言したメタ属性の受け皿。
 *
 * ホストが組み立てのたびに1つ用意し、[KsDialogAttributes] の宣言がここへ集まる。
 * 集まった値はそのままホストの添付として反映され、器は初回のネイティブレイアウトパス完了時点で
 * その値をスナップショットとして固定する (core/ADR-0015)。
 *
 * @param onSupplied 値が届いたときに呼ばれる。ホストが自分の添付へ反映するために使う
 */
internal class DialogAttributeCollector(private val onSupplied: () -> Unit) {

    /** 届いた静的メタ属性。宣言がなければ null (供給なし)。 */
    var options: DialogOptions? = null
        private set

    /** 届いた動的メタ属性。宣言がなければ null (供給なし)。 */
    var placement: DialogPlacement? = null
        private set

    /** 届いた出入りの演出。宣言がなければ null (供給なし)。 */
    var transition: DialogTransition? = null
        private set

    /** 宣言された値を取り込む。null の面は「宣言していない」を意味し、既に届いた値を消さない。 */
    fun supply(options: DialogOptions?, placement: DialogPlacement?, transition: DialogTransition?) {
        if (options == null && placement == null && transition == null) {
            return
        }
        options?.let { this.options = it }
        placement?.let { this.placement = it }
        transition?.let { this.transition = it }
        onSupplied()
    }
}

/**
 * その composable がどのダイアログの中身として組み立てられているかを運ぶ経路。
 *
 * ダイアログの中身以外で [KsDialogAttributes] が呼ばれた場合は null になり、宣言は何も起こさない。
 */
internal val LocalDialogAttributeCollector = staticCompositionLocalOf<DialogAttributeCollector?> { null }
