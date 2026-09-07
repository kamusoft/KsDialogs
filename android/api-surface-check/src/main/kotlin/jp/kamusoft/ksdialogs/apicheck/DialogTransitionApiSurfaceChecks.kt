package jp.kamusoft.ksdialogs.apicheck

import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.compose.runtime.Composable
import jp.kamusoft.ksdialogs.DialogTransition
import jp.kamusoft.ksdialogs.DialogTransitionEdge
import jp.kamusoft.ksdialogs.compose.KsDialogAttributes
import jp.kamusoft.ksdialogs.ksDialogTransition
import kotlin.time.Duration.Companion.milliseconds

/**
 * 出入りの演出の添付面とプリセットの公開 API 形状の正の検証 (core/ADR-0017)。
 *
 * このファイルがコンパイルできることが検証結果であり、利用者が意図どおりに書けることを示す。
 */
public object DialogTransitionApiSurfaceChecks {

    /** 従来 View 系では拡張プロパティで演出を添付でき、読み戻せる。 */
    public fun acceptsViewAttachment(view: View): DialogTransition? {
        view.ksDialogTransition = DialogTransition(
            presentation = { hostView -> hostView.alpha = 1f },
            dismissal = { hostView -> hostView.alpha = 0f },
            overlayDuration = 200.milliseconds,
        )
        return view.ksDialogTransition
    }

    /** フックは両方とも省略でき、覆いの時間も省略できる。 */
    public fun acceptsEmptyTransition(): DialogTransition = DialogTransition()

    /** 添付した演出のフックは公開面から読める。 */
    public fun acceptsHookAccess(transition: DialogTransition): Boolean =
        transition.presentation != null && transition.dismissal != null && transition.overlayDuration != null

    /** プリセットは引数を省略しても、時間とイージングを指定しても作れる。 */
    public fun acceptsPresets(): List<DialogTransition> = listOf(
        DialogTransition.fade(),
        DialogTransition.fade(duration = 400.milliseconds),
        DialogTransition.fade(duration = 400.milliseconds, easing = OvershootInterpolator()),
        DialogTransition.slide(from = DialogTransitionEdge.BOTTOM),
        DialogTransition.slide(DialogTransitionEdge.START, 300.milliseconds, OvershootInterpolator()),
        DialogTransition.zoom(),
        DialogTransition.zoom(duration = 150.milliseconds),
        DialogTransition.none(),
    )

    /** 滑り込みの辺は4方向そろっている。 */
    public fun acceptsEdges(): List<DialogTransitionEdge> = listOf(
        DialogTransitionEdge.TOP,
        DialogTransitionEdge.BOTTOM,
        DialogTransitionEdge.START,
        DialogTransitionEdge.END,
    )

    /** 宣言的 UI では属性宣言の第3スロットで演出を添付できる。 */
    @Composable
    public fun AcceptsComposeAttachment() {
        KsDialogAttributes(transition = DialogTransition.slide(from = DialogTransitionEdge.TOP))
    }
}
