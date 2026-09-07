package jp.kamusoft.ksdialogs

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
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

/**
 * 1回の表示が所有する Loading の器 (core/ADR-0022・core/ADR-0026)。
 *
 * Dialog の器 ([DialogContainer]) と同じく1枚が1つの全画面透過ウィンドウだが、
 * Dialog 機構の提示スタック・多段表示の意味論には参加しない。ダイアログのウィンドウより
 * 後から載せることで最前面になり、覆いが全面を占めるので背後 (ダイアログを含む) への入力を遮る。
 *
 * レイアウト適用 ([DialogLayoutHost]) と出入りの演出 ([DialogTransitionRunner]) は Dialog の器と
 * 同じ部品を通すため、レイアウト規則と演出の意味は Dialog と同一になる。
 *
 * Loading はユーザー操作では閉じないため、外側タップと戻るボタンの扱いは持たない。
 * したがって `isCanceledOnTouchOutside` は添付されても常に無効である。
 *
 * @param contentView 中身として表示する View。画面の再生成をまたぐ再取り付けでは同じ View が渡る
 * @param layoutSnapshot 実効値の供給元。再取り付けでは固定済みのものが渡り、実効値は変わらない
 * @param playsPresentation 入りの演出を走らせるか。再取り付けでは false (途中から演出をやり直さない)
 * @param visibleAreaInsets 可視領域を狭めるシステム領域の幅 (px) を求める方法。
 *   既定はウィンドウが報告する値
 */
internal class LoadingContainer(
    context: Context,
    val contentView: View,
    private val layoutSnapshot: DialogLayoutSnapshot,
    private val playsPresentation: Boolean,
    private val visibleAreaInsets: (View) -> DialogPixelInsets = ::windowVisibleAreaInsets,
) : android.app.Dialog(context, android.R.style.Theme_Translucent_NoTitleBar) {

    /** 出入りの演出を受け持つ部品。器はどの局面を走らせるかだけを決める。 */
    private val transitionRunner: DialogTransitionRunner = DialogTransitionRunner(context, contentView)

    /**
     * 覆いと中身の配置を受け持つ面。
     *
     * ウィンドウに載せるより先に器が組み立てるので、ウィンドウを使わずにこの面だけを
     * 画面へ載せてレイアウトを実測することもできる。
     */
    val layoutHost: DialogLayoutHost by lazy(LazyThreadSafetyMode.NONE) { buildContentHierarchy() }

    /** 器が今どの段階にいるか。進行は UI スレッドで行うが、観察は別スレッドからも行われる。 */
    @Volatile
    var containerState: DialogContainerState = DialogContainerState.CREATED
        private set

    /** 実効値と同じ時点で固定された出入りの演出。固定前は null。 */
    val resolvedTransition: DialogTransition?
        get() = transitionRunner.resolvedTransition

    /** 背景の覆い。中身の兄弟として敷かれ、中身とは独立にフェードする。 */
    val overlayView: View
        get() = layoutHost.overlayView

    /** 進行中の演出を進める仕事。撤去へ移るときに取り消す。 */
    private var lifecycleJob: Job? = null

    /**
     * 演出を進める文脈。呼び出し元のコルーチンとは切り離してあるため、
     * 呼び出し元が取り消されても撤去はそのまま完遂される。
     */
    private val lifecycleScope: CoroutineScope by lazy {
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }

    init {
        // Loading はユーザー操作では閉じない。戻るボタンも外側タップも閉鎖の契機にしない
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        if (playsPresentation) {
            // 演出を始めるまで中身は見せない。最終位置が演出より先に見えるのを防ぐ
            transitionRunner.concealContent()
        }
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
        setContentView(layoutHost)
        if (!playsPresentation && layoutSnapshot.isFrozen) {
            // 固定済みの実効値で載せ直す経路では、レイアウトパスの節目 (固定の瞬間) が
            // もう訪れないため、進行はこの時点から始める
            beginLifecycle()
        }
    }

    /**
     * 出の演出を走らせてから器を撤去する。
     *
     * 撤去が完了してから戻るので、戻った時点で操作ブロックは解除されている
     * (core/ADR-0024 の「hide と合流最後の start は撤去完了後に戻る」)。
     */
    suspend fun runDismissal() {
        if (containerState == DialogContainerState.REMOVED) {
            return
        }
        containerState = DialogContainerState.DISMISSING
        lifecycleJob?.cancel()
        lifecycleJob = null
        // 実効値が固まる前に閉じる経路では演出そのものが成立しないため、撤去だけを行う
        if (layoutSnapshot.isFrozen) {
            transitionRunner.runDismissalPhase()
        }
        finishRemoval()
    }

    /**
     * 中身を残したまま器だけを畳む。画面の再生成をまたぐ再取り付けで使う。
     *
     * 中身は次の器へそのまま載せ替えるため、演出は走らせず親からも外す。
     * 入りの演出を終える前に畳む場合だけは、中身を演出後の見えへ戻してから渡す。
     * 次の器は演出をやり直さない (覆いも [overlayView] の復元で見えている状態から続く) ため、
     * ここで戻さないと中身が途中の見えのまま固まる。
     */
    fun detachForReattach() {
        if (isBeforePresentationFinished) {
            transitionRunner.revealContentAsShown()
        }
        finishRemoval()
    }

    /** 入りの演出を終える前か。載せ替えで中身の見えを戻すかの判定に使う。 */
    private val isBeforePresentationFinished: Boolean
        get() = playsPresentation && when (containerState) {
            DialogContainerState.CREATED,
            DialogContainerState.ATTACHED,
            DialogContainerState.PRESENTING,
            -> true

            DialogContainerState.SHOWN,
            DialogContainerState.DISMISSING,
            DialogContainerState.REMOVED,
            -> false
        }

    /** 器を撤去し、中身を親から外す。 */
    private fun finishRemoval() {
        if (containerState == DialogContainerState.REMOVED) {
            return
        }
        containerState = DialogContainerState.REMOVED
        lifecycleJob?.cancel()
        lifecycleJob = null
        (contentView.parent as? ViewGroup)?.removeView(contentView)
        closeWindow()
    }

    /** ウィンドウを画面から外す。既に外れていれば何も起こさない。 */
    private fun closeWindow() {
        try {
            dismiss()
        } catch (alreadyDetached: IllegalArgumentException) {
            // ウィンドウが画面から失われた後の閉鎖要求。器は既に画面から外れている
        }
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
        if (!playsPresentation) {
            // 再取り付けでは既に見えている状態から続くので、入りの演出はやり直さない
            containerState = DialogContainerState.SHOWN
            return
        }
        beginPresentation()
    }

    /** 覆いのフェードと出現の演出を始める。表示の呼び出し元はこの完了を待たない。 */
    private fun beginPresentation() {
        containerState = DialogContainerState.PRESENTING
        // 演出の開始を次のフレームへ持ち越すと最終位置が1フレーム見えるため、その場で走らせる
        lifecycleScope.launch(start = CoroutineStart.UNDISPATCHED) {
            lifecycleJob = coroutineContext.job
            transitionRunner.runPresentationPhase()
            if (containerState != DialogContainerState.PRESENTING) {
                return@launch
            }
            containerState = DialogContainerState.SHOWN
        }
    }

    /** 覆いの上に中身をレイアウト属性どおりに置いた階層を組み立てる。 */
    private fun buildContentHierarchy(): DialogLayoutHost =
        DialogLayoutHost(
            context = context,
            snapshot = layoutSnapshot,
            contentView = contentView,
            visibleAreaInsets = visibleAreaInsets,
            onLayoutSnapshotFrozen = ::beginLifecycle,
        ).apply {
            // 覆いの上のタップをこの面で受け止め、背後の画面・ダイアログへ届かせない
            isClickable = true
            transitionRunner.bindOverlay(overlayView)
            if (!playsPresentation) {
                // 再取り付けでは覆いは既に出ている状態から続く
                overlayView.alpha = 1f
            }
        }
}
