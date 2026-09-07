package jp.kamusoft.ksdialogs

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlin.time.Duration

/**
 * 1回の show が所有するダイアログの器。
 *
 * 背景の覆い (scrim) と中身の View の配置、外側タップのキャンセル、戻るボタンのキャンセル、
 * 出入りの演出、自分自身の閉鎖を受け持つ。1枚が1つのウィンドウなので、
 * 重ね出ししても互いに独立に閉じられる。
 *
 * 出入りの演出は [DialogTransitionRunner] に実行させる (core/ADR-0017)。ウィンドウのアニメーションには頼らず、
 * 覆いと中身を別のレイヤとして扱い、退出の演出が終わってから撤去・配送する。
 *
 * @param placement show の引数で渡された配置。null でなければ中身に添付された placement をまるごと置換する
 */
internal class DialogContainer(
    context: Context,
    /** この器が中身として表示する View。show 1回ごとに factory が生成したもの。 */
    val contentView: View,
    private val resultChannel: DialogResultChannel,
    placement: DialogPlacement? = null,
) : android.app.Dialog(context, android.R.style.Theme_Translucent_NoTitleBar) {

    /**
     * この器が採用するメタ属性の実効値。
     *
     * 「show 引数 > コンテンツ添付 > 契約既定値」の優先順で合成する (core/ADR-0015)。
     * 合成は初回のレイアウトパスの中でも読み直され、**そのパスが完了した時点の添付値**で固定される。
     * 以降に添付が書き換わっても表示は追随しない。
     */
    private val layoutSnapshot: DialogLayoutSnapshot = DialogLayoutSnapshot(contentView, placement)

    /** 出入りの演出を受け持つ部品。器はどの局面を走らせるかだけを決める。 */
    private val transitionRunner: DialogTransitionRunner = DialogTransitionRunner(context, contentView)

    /** 覆いと中身の配置を受け持つ面。ウィンドウの中身を組み立てるまでは null。 */
    var layoutHost: DialogLayoutHost? = null
        private set

    /**
     * 器が今どの段階にいるか。
     *
     * 進行は UI スレッドで行うが、器が画面を失った報告は別スレッドからも届くため揮発で持つ。
     */
    @Volatile
    var containerState: DialogContainerState = DialogContainerState.CREATED
        private set

    /** 実効値と同じ時点で固定された出入りの演出。固定前は null。 */
    val resolvedTransition: DialogTransition?
        get() = transitionRunner.resolvedTransition

    /** 固定された覆いのフェード時間。 */
    val resolvedOverlayDuration: Duration
        get() = transitionRunner.resolvedOverlayDuration

    /** 器の撤去に合わせて提示層が行う後始末。 */
    var onRemoved: (() -> Unit)? = null

    /** 進行中の演出を進める仕事。脱出口ではこれを取り消して先へ進む。 */
    private var lifecycleJob: Job? = null

    /** 撤去まで進んだあとに配送する結果。配送先がまだ無ければここで待たせる。 */
    private var pendingDelivery: DialogOutcome? = null

    /** 確定した結果を呼び出し元へ届ける口。撤去のあとに1回だけ呼ばれる。 */
    private var deliveryHandler: ((DialogOutcome) -> Unit)? = null

    /**
     * 演出を進める文脈。呼び出し元のコルーチンとは切り離してあるため、
     * 呼び出し元がキャンセルされても退出の処理はそのまま完遂される。
     */
    private val lifecycleScope: CoroutineScope by lazy {
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }

    /**
     * ウィンドウが画面から取り除かれたことの受け皿。
     *
     * 閉鎖 (`dismiss`) を経ずにウィンドウだけが取り除かれる経路でも、結果を確定させる。
     */
    private val windowDetachObserver = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(view: View) {
            // 引き継ぎ元のシステムバー設定を読めるのは、ウィンドウが画面に載った後
            window?.let { DialogWindowSystemBars.inheritSystemBarState(it, hostWindow()) }
        }

        override fun onViewDetachedFromWindow(view: View) {
            reportDetached()
        }
    }

    init {
        // 戻るボタンでキャンセルできるようにする
        setCancelable(true)
        // 外側タップは覆いへのタップとして自前で扱うため、ウィンドウ外タップの既定処理は使わない
        setCanceledOnTouchOutside(false)
        // 自前で扱わないキャンセル経路 (戻るボタン) はここへ集まる
        setOnCancelListener { reportBackPress() }
        // 閉鎖を経た場合の受け皿 (ウィンドウの取り外しより先に届く)
        setOnDismissListener { reportDetached() }
        // 演出を始めるまで中身は見せない。最終位置が演出より先に見えるのを防ぐ
        transitionRunner.concealContent()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.apply {
            // 覆いは自前で描くため、ウィンドウ既定の背景と暗転は外す
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            // システムバーの背景もこのウィンドウの責任にして、システムの暗い覆いが敷かれないようにする
            DialogWindowSystemBars.makeSystemBarBackgroundsTransparent(this)
        }
        setContentView(buildContentHierarchy())
        window?.decorView?.addOnAttachStateChangeListener(windowDetachObserver)
    }

    /** 退出中と撤去後は入力を受け付けない (core/ADR-0017)。 */
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (containerState == DialogContainerState.DISMISSING ||
            containerState == DialogContainerState.REMOVED
        ) {
            return true
        }
        return super.dispatchTouchEvent(event)
    }

    /**
     * 外側タップを受け取ったときの扱い。
     *
     * スナップショット時点の `isCanceledOnTouchOutside` が true (既定) なら
     * キャンセル操作と同じ経路で結果を確定させ、false なら何も起こさない。
     * false のときもタップは覆いが受け止めるため背後の画面へは透過しない。
     * 退出中と撤去後は入力を受け付けない。
     */
    fun reportOutsideTap() {
        if (!acceptsInput()) {
            return
        }
        if (!layoutSnapshot.effective().isCanceledOnTouchOutside) {
            return
        }
        resultChannel.settle(DialogOutcome.Cancelled, DialogDismissalOrigin.OUTSIDE_TAP)
    }

    /** 戻るボタン操作をキャンセルとして報告する。退出中と撤去後は受け付けない。 */
    fun reportBackPress() {
        if (!acceptsInput()) {
            return
        }
        resultChannel.settle(DialogOutcome.Cancelled, DialogDismissalOrigin.BACK_PRESS)
    }

    /**
     * 器が画面から外れたことを報告する。
     *
     * 結果が未確定のまま器だけが消える経路 (画面の破棄など、ライブラリの外の要因) でも
     * show が結果を返さないまま残らないよう、この時点で cancelled として確定させる。
     * 器は既に画面を失っており演出は成立しないため、実行中の演出を捨ててそのまま撤去する。
     */
    fun reportDetached() {
        if (containerState == DialogContainerState.REMOVED) {
            return
        }
        lifecycleJob?.cancel()
        resultChannel.settle(DialogOutcome.Cancelled, DialogDismissalOrigin.HOST_LOST)
        finishRemoval()
    }

    /**
     * 提示先の画面が破棄されるときに、器を閉じて結果を確定させる。
     *
     * 画面の破棄ではウィンドウが取り除かれるだけで閉鎖の通知は届かず、
     * 取り残されたウィンドウは leaked window として警告される。
     * 破棄の通知を受けた時点で自分から閉じることで、ウィンドウを残さずに結果も確定させる。
     */
    fun closeOnHostDestroyed() {
        reportDetached()
    }

    /** 確定した結果を届ける口を結び付ける。撤去済みならその場で届く。 */
    fun onDelivery(handler: (DialogOutcome) -> Unit) {
        deliveryHandler = handler
        deliverPendingOutcomeIfPossible()
    }

    /**
     * 器が画面に載り実効値が固まった時点から、出入りの進行を始める (core/ADR-0017)。
     */
    private fun beginLifecycle() {
        if (containerState != DialogContainerState.CREATED) {
            return
        }
        containerState = DialogContainerState.ATTACHED
        transitionRunner.settleSnapshot(contentView.ksDialogTransition)
        observeResultChannel()
        if (resultChannel.isResultSettled) {
            // 提示が始まる前に閉鎖信号が来ていた。演出は成立しないので撤去だけを行う
            scheduleImmediateRemoval()
            return
        }
        beginPresentation()
    }

    /**
     * 結果チャネルの確定と取り消しをこの器へ結び付ける。
     *
     * 扱いは必ず UI スレッドの次の機会へ回す。報告はレイアウトパスの最中や別スレッドからも届くため、
     * その場で器を畳むと画面の組み立てに割り込んでしまう。
     */
    private fun observeResultChannel() {
        resultChannel.onSettle { lifecycleScope.launch(Dispatchers.Main) { handleSettled() } }
        resultChannel.onCallerCancellation {
            lifecycleScope.launch(Dispatchers.Main) { handleCallerCancellation() }
        }
    }

    /**
     * 提示より前の撤去を、レイアウトパスの外で行う。
     *
     * 描画の直前に器を閉じるとウィンドウの取り外しがレイアウト中に割り込むため、1度譲ってから撤去する。
     */
    private fun scheduleImmediateRemoval() {
        layoutHost?.post {
            if (containerState == DialogContainerState.ATTACHED) {
                finishRemoval()
            }
        }
    }

    /** 覆いのフェードと出現の演出を始める。 */
    private fun beginPresentation() {
        containerState = DialogContainerState.PRESENTING
        // 演出の開始を次のフレームへ持ち越すと最終位置が1フレーム見えるため、その場で走らせる
        lifecycleScope.launch(start = CoroutineStart.UNDISPATCHED) {
            // その場で走り出すので、進行中の仕事として自分を控えるのも本体の中で行う
            lifecycleJob = coroutineContext.job
            transitionRunner.runPresentationPhase()
            if (containerState != DialogContainerState.PRESENTING) {
                return@launch
            }
            containerState = DialogContainerState.SHOWN
            if (resultChannel.isResultSettled) {
                // 提示の最中に来ていた閉鎖信号は、提示を完走させてからここで直列に扱う
                beginDismissal()
            }
        }
    }

    /** 退出の演出と覆いの消滅フェードを並行して走らせ、両方の完了を待ってから撤去する。 */
    private fun beginDismissal() {
        if (containerState != DialogContainerState.PRESENTING &&
            containerState != DialogContainerState.SHOWN
        ) {
            return
        }
        containerState = DialogContainerState.DISMISSING
        lifecycleJob?.cancel()
        lifecycleScope.launch(start = CoroutineStart.UNDISPATCHED) {
            lifecycleJob = coroutineContext.job
            transitionRunner.runDismissalPhase()
            if (containerState != DialogContainerState.DISMISSING) {
                return@launch
            }
            finishRemoval()
        }
    }

    /** 器を撤去し、確定済みの結果を配送する。 */
    private fun finishRemoval() {
        if (containerState == DialogContainerState.REMOVED) {
            return
        }
        containerState = DialogContainerState.REMOVED
        val runningLifecycle = lifecycleJob
        lifecycleJob = null
        runningLifecycle?.cancel()
        onRemoved?.invoke()
        onRemoved = null
        closeWindow()
        // ここまで来て未確定なのは器が画面を失った経路だけなので、その扱いで確定させる
        resultChannel.settle(DialogOutcome.Cancelled, DialogDismissalOrigin.HOST_LOST)
        pendingDelivery = resultChannel.settledOutcome
        deliverPendingOutcomeIfPossible()
    }

    /** ウィンドウを画面から外す。既に外れていれば何も起こさない。 */
    private fun closeWindow() {
        try {
            dismiss()
        } catch (alreadyDetached: IllegalArgumentException) {
            // ウィンドウが画面から失われた後の閉鎖要求。器は既に画面から外れている
        }
    }

    /** 撤去済みの結果を呼び出し元へ届ける。配送先がまだ決まっていなければ、決まった時点で届ける。 */
    private fun deliverPendingOutcomeIfPossible() {
        val outcome = pendingDelivery ?: return
        val delivery = deliveryHandler ?: return
        pendingDelivery = null
        deliveryHandler = null
        delivery(outcome)
    }

    /** 呼び出し元が待つのをやめたときの扱い (core/ADR-0017 の脱出口)。 */
    private fun handleCallerCancellation() {
        when (containerState) {
            // まだ画面に載っていない。載った時点で演出なしの撤去に入る
            DialogContainerState.CREATED -> Unit
            DialogContainerState.ATTACHED -> finishRemoval()
            // 出現の演出の完走を待たず、退出へ進む
            DialogContainerState.PRESENTING, DialogContainerState.SHOWN -> beginDismissal()
            // 退出の演出の完了を待つのをやめ、そのまま撤去する
            DialogContainerState.DISMISSING -> finishRemoval()
            DialogContainerState.REMOVED -> Unit
        }
    }

    /**
     * 結果が確定したときの扱い。
     *
     * 器が画面を失った経路と呼び出し元の取り消しは、確定と同時に届く専用の扱いが受け持つ。
     * 取り消しをここでも扱うと「この取り消しで退出に入った」のか「もともと退出中だった」のかを
     * 見分けられなくなり、脱出口の判定 (core/ADR-0017) が狂う。
     */
    private fun handleSettled() {
        val origin = resultChannel.resultOrigin
        if (origin == DialogDismissalOrigin.HOST_LOST || origin == DialogDismissalOrigin.CALLER_CANCELLATION) {
            return
        }
        when (containerState) {
            // 提示前は載った時点で、提示中は完走したあとで扱う。退出中と撤去後は何も起こさない
            DialogContainerState.CREATED,
            DialogContainerState.PRESENTING,
            DialogContainerState.DISMISSING,
            DialogContainerState.REMOVED,
            -> Unit

            DialogContainerState.ATTACHED -> finishRemoval()
            DialogContainerState.SHOWN -> beginDismissal()
        }
    }

    /** 今の段階で利用者の操作を受け付けるか。 */
    private fun acceptsInput(): Boolean =
        containerState != DialogContainerState.DISMISSING && containerState != DialogContainerState.REMOVED

    /** 覆いの上に中身をレイアウト属性どおりに置いた階層を組み立てる。 */
    private fun buildContentHierarchy(): View =
        DialogLayoutHost(
            context = context,
            snapshot = layoutSnapshot,
            contentView = contentView,
            onLayoutSnapshotFrozen = ::beginLifecycle,
        ).apply {
            setOnClickListener { reportOutsideTap() }
            transitionRunner.bindOverlay(overlayView)
            layoutHost = this
        }

    /** 提示先の画面のウィンドウ。画面に紐づかない Context から作られていれば null。 */
    private fun hostWindow() = context.hostActivity()?.window
}
