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

    /** 中身を組み立てて器を最前面へ提示し、その1枚の結果を受け取るための handle を返す。 */
    fun present(request: DialogPresentationRequest): PresentedDialog
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
