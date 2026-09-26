package jp.kamusoft.ksdialogs.compose

import android.graphics.RectF
import android.view.View
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalView
import jp.kamusoft.ksdialogs.DialogCurrentPageLedger
import jp.kamusoft.ksdialogs.DialogCurrentPageMarker
import jp.kamusoft.ksdialogs.KsDialogsInternalApi

/**
 * この composable を、基準領域 `DialogLayoutArea.CURRENT_PAGE` が使う「表示中のページ」として名乗らせる。
 *
 * 下部ナビゲーションやタブを持つ画面では、`Scaffold` の content 枠 (バーの内側) に 1 回付ける。
 * 付けた composable が画面に配置されている間だけ候補になり、画面遷移などで外れると候補から外れる。
 * 候補が複数あるときは次の順で 1 つに決まる。
 *
 * - ダイアログを出す画面 (Activity) が持つウィンドウ (メインウィンドウと、同じ Activity で出したモーダル・
 *   ダイアログのウィンドウ) 以外にあるもの、KsDialogs 自身のダイアログ・Loading・Toast の中にあるもの、
 *   ウィンドウの外に置かれたものは使わない
 * - 候補が入れ子になっていれば内側の composable を使う
 * - それ以外は最後に画面へ配置された composable を使う
 *
 * 基準になるのは付けた composable の矩形のうち、システムバーを除いた可視領域と重なる部分。
 * 候補が無いときは `DialogCurrentPage.provider` に登録した関数が返す View を使い、
 * それも無ければ基準は可視領域と同じになる。
 */
public fun Modifier.ksDialogCurrentPage(): Modifier = this then DialogCurrentPageElement

/** [ksDialogCurrentPage] が付ける要素。状態を持たないので 1 つを使い回す。 */
private data object DialogCurrentPageElement : ModifierNodeElement<DialogCurrentPageNode>() {
    override fun create(): DialogCurrentPageNode = DialogCurrentPageNode()

    override fun update(node: DialogCurrentPageNode): Unit = Unit

    override fun InspectorInfo.inspectableProperties() {
        name = "ksDialogCurrentPage"
    }
}

/**
 * 名乗った枠 1 つ。配置 (attach) と離脱 (detach) をそのまま台帳への出入りにする。
 *
 * 枠の矩形は問い合わせのたびに直近の配置結果から読み、ウィンドウの座標で返す。
 * 親に切り取られた部分は含めないため、画面外に置かれた枠は空の矩形になり候補から外れる。
 */
@OptIn(KsDialogsInternalApi::class)
private class DialogCurrentPageNode :
    Modifier.Node(),
    GlobalPositionAwareModifierNode,
    CompositionLocalConsumerModifierNode,
    DialogCurrentPageMarker {

    /** 直近の配置結果。配置される前と離脱した後は null。 */
    private var coordinates: LayoutCoordinates? = null

    /** 枠が載っている View。配置されている間だけ持つ。 */
    private var attachedHostView: View? = null

    override val hostView: View
        get() = checkNotNull(attachedHostView) { "The marker is not attached." }

    override fun onAttach() {
        attachedHostView = currentValueOf(LocalView)
        DialogCurrentPageLedger.attach(this)
    }

    override fun onDetach() {
        DialogCurrentPageLedger.detach(this)
        coordinates = null
        attachedHostView = null
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
    }

    override fun boundsInWindow(): RectF? {
        val current = coordinates?.takeIf { it.isAttached } ?: return null
        val bounds = current.boundsInWindow()
        return RectF(bounds.left, bounds.top, bounds.right, bounds.bottom)
    }
}
