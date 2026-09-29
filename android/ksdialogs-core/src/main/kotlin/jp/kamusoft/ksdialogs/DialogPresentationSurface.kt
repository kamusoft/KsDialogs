package jp.kamusoft.ksdialogs

import android.content.Context
import android.view.View

/**
 * ダイアログの器を画面へ出し入れする面。
 *
 * 提示先はライブラリが自動解決するため、show の呼び出し側はこの面に関与しない。
 * すべて UI スレッドから呼ばれる。
 */
internal interface DialogPresentationSurface {
    /** 今ダイアログを提示できるか (アクティブな提示先が存在するか)。 */
    val canPresent: Boolean

    /** 提示先の出現を待つ show が並ぶ列。同じ提示先を共有する面は同じ列を返す。 */
    val hostWaitQueue: DialogHostWaitQueue

    /**
     * 中身を組み立てて器を最前面へ提示し、その1枚の結果を受け取るための handle を返す。
     *
     * 呼ぶのは提示先を確かめた直後で、器を載せたときは戻った時点で器のウィンドウは追加済みになっている。
     * 提示先が得られず器を載せられなければ、中身を作らずに器の消失と同じく cancelled で確定させ、
     * その結果が届く handle を返す。提示先を待っていた次の show は、この戻りのあとに明ける。
     */
    fun present(request: DialogPresentationRequest): PresentedDialog

    /**
     * 提示先の入れ替わり (resume・破棄) を購読する。
     *
     * 通知は「提示先が現れたかもしれない」ことだけを知らせ、UI スレッドで届く。
     * 受け取った側は [canPresent] を読み直し、現れていなければ待ち続ける。
     * 提示先を待っている show が無くなったら購読を解除する。
     */
    fun observeHostChange(onHostChanged: () -> Unit): DialogHostRegistration
}

/** 提示先の入れ替わりの購読1件。 */
internal fun interface DialogHostRegistration {
    /** 購読を解除する。解除後は通知が届かない。 */
    fun cancel()
}

/**
 * 1回の show が提示する内容。
 *
 * @property createContentView 提示先の [Context] から中身の View を新規生成する関数
 * @property resultChannel その show の結果チャネル。器はキャンセル操作をここへ報告する
 * @property placement show の引数で渡された配置。null なら中身への添付が使われる
 */
internal class DialogPresentationRequest(
    val createContentView: (Context) -> View,
    val resultChannel: DialogResultChannel,
    val placement: DialogPlacement? = null,
)

/** 提示済みのダイアログ1枚。 */
internal fun interface PresentedDialog {
    /**
     * 確定した結果が呼び出し元へ届く口を結び付ける。
     *
     * 配送は退出の演出・覆いの消滅・器の撤去がすべて済んだあとに1回だけ行われる (core/ADR-0017)。
     */
    fun onDelivery(handler: (DialogOutcome) -> Unit)
}
