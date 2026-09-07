package jp.kamusoft.ksdialogs

import android.content.Context
import android.view.View
import kotlinx.coroutines.Job

/** 表示に使う中身の指定。 */
internal sealed interface ToastContentRequest {
    /** ライブラリ同梱のデフォルト View。メッセージだけを運ぶ。 */
    class Builtin(val message: String) : ToastContentRequest

    /** ViewModel から中身を作るカスタム Toast View。factory の出どころだけが下位で分かれる。 */
    sealed interface Custom : ToastContentRequest {
        val viewModel: ToastViewModel
    }

    /** レジストリに登録済みの ViewModel から作るカスタム Toast View。 */
    class Registered(override val viewModel: ToastViewModel) : Custom

    /** その場で渡された factory から作るカスタム Toast View。レジストリは読まない (core/ADR-0013)。 */
    class Inline(
        override val viewModel: ToastViewModel,
        val factory: ToastViewFactory,
    ) : Custom

    /**
     * 呼び出し時点でレジストリから解決済みの factory と、UI スレッド上で作る ViewModel から作る
     * カスタム Toast View (型指定 show。core/ADR-0035)。
     *
     * [prepare] は ViewModel の生成と configure をまとめて行い、受理の待ち行列が処理する時点で
     * UI スレッド上で呼ばれる。その失敗は受理後の失敗 (この表示1枚だけの破棄) になる。
     */
    class Typed(
        val prepare: () -> ToastViewModel,
        val factory: ToastViewFactory,
    ) : ToastContentRequest
}

/**
 * 表示中の Toast 1枚分の状態 (core/ADR-0030 の「1 Toast 1器」)。
 *
 * 器・中身・ViewModel・factory・演出フックへの参照は撤去が完了するまでここが握る
 * (fire-and-forget でも途中で解放しない)。消滅の期限は受理時点で決まった単調時計の時刻で持つので、
 * 画面の再生成で器を作り直しても巻き戻らない。
 *
 * @param request 表示する中身の指定
 * @param factory カスタム Toast の中身を作る関数。デフォルト View では null
 * @param style この表示に採用されたスタイル
 * @param showPlacement 表示 API の引数で渡された配置
 * @param fallbackPlacement show 引数も添付も無いときに採る配置
 * @param deadlineElapsedRealtime 消滅の期限 (単調時計のミリ秒)
 */
internal class ToastDisplay(
    val request: ToastContentRequest,
    val factory: ToastViewFactory?,
    val style: ToastStyle,
    val showPlacement: DialogPlacement?,
    val fallbackPlacement: DialogPlacement,
    val deadlineElapsedRealtime: Long,
) {
    /** 解決済みの中身。提示先が現れるまでは null のまま待つ。 */
    var contentView: View? = null

    /** 中身に紐づく実効値。固定後は再取り付けでも同じ値が使われる。 */
    var layoutSnapshot: DialogLayoutSnapshot? = null

    /** 取り付け済みの器。提示先がまだ無い間は null。 */
    var container: ToastContainer? = null

    /** 器を載せている提示先。入れ替わりの判定に使う。 */
    var attachedHost: Context? = null

    /** 入りの演出を走らせたか。再取り付けでは演出をやり直さない。 */
    var hasPlayedPresentation: Boolean = false

    /** 期限を待つ仕事。 */
    var timerJob: Job? = null

    /** 撤去へ進んだか。期限の到達と後始末が重なっても1回しか進まないための印。 */
    var isFinishing: Boolean = false

    /**
     * 型指定の表示でその場に作った ViewModel。他の経路では中身の指定が ViewModel を持つため null。
     *
     * 中身の View を作ったあとに読む契約は無いが、参照の寿命を他の経路と同じ
     * 「撤去の完了まで」にそろえるためにここが握る。
     */
    var typedViewModel: ToastViewModel? = null

    /** 表示中のデフォルト View。カスタム Toast 表示中は null。 */
    val defaultContentView: ToastDefaultContentView?
        get() = contentView as? ToastDefaultContentView

    /** 撤去の完了後に、保持していた参照を手放す。 */
    fun releaseResources() {
        container = null
        attachedHost = null
        contentView = null
        layoutSnapshot = null
        timerJob = null
        typedViewModel = null
    }
}
