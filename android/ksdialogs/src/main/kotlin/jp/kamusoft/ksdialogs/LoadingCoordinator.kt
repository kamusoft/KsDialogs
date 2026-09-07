package jp.kamusoft.ksdialogs

import android.content.Context
import android.util.Log
import android.view.View
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference

/** 表示に使う中身の指定。 */
internal sealed interface LoadingContentRequest {
    /** ライブラリ同梱の内蔵コンテンツ (既定ローディング)。 */
    object Builtin : LoadingContentRequest

    /** ViewModel から中身を作るカスタム Loading View。factory の出どころだけが下位で分かれる。 */
    sealed interface Custom : LoadingContentRequest {
        val viewModel: LoadingViewModel
    }

    /** レジストリに登録済みの ViewModel から作るカスタム Loading View。 */
    class Registered(override val viewModel: LoadingViewModel) : Custom

    /** その場で渡された factory から作るカスタム Loading View。レジストリは読まない (core/ADR-0013)。 */
    class Inline(
        override val viewModel: LoadingViewModel,
        val factory: LoadingViewFactory,
    ) : Custom

    /**
     * 呼び出し時点でレジストリから解決済みの factory と、その場で生成された ViewModel から作る
     * カスタム Loading View (型指定の show / start。core/ADR-0035)。
     *
     * 解決を呼び出し時点で終えているため、状態の正に届くまでの間に再登録が起きても
     * 最初に取得した組で中身を作る。
     */
    class Resolved(
        override val viewModel: LoadingViewModel,
        val factory: LoadingViewFactory,
    ) : Custom
}

/** 合流1件の身分証。開始した表示世代を持ち、終了・報告がその世代のものかを見分ける。 */
internal data class LoadingUseToken(val generation: Int)

/**
 * 1 OS プロセス内で唯一の Loading の状態の正 (core/ADR-0024・core/ADR-0027)。
 *
 * 合流カウント・表示世代・表示中のコンテンツ・最新のメッセージと進捗を保持し、
 * 既定シングルトンも DI 注入したインスタンスも、すべての入口がここへ委譲する。
 * これにより入口をまたいだ利用でも表示は1つに合流する。
 *
 * 状態を変える操作はすべて UI スレッド上で受理順に直列化され、「最新 (後勝ち)」は受理順で定まる。
 * 呼び出しは任意のスレッドから行える (Dialog 面と同じ)。
 */
internal class LoadingCoordinator(
    /** カスタム Loading の登録。 */
    val registry: LoadingViewRegistry = LoadingViewRegistry.shared,
    /** スタイルと既定ローディングの器メタ属性。 */
    val settings: LoadingSettings = LoadingSettings(),
    private val presentationSurface: LoadingPresentationSurface = ActivityLoadingPresentationSurface(),
) {

    /**
     * 状態の受理と撤去を進める文脈。
     *
     * 呼び出し元のコルーチンとは切り離してあるため、呼び出し元が取り消されても
     * 受理済みの報告と進行中の撤去はそのまま完遂される。
     */
    private val scope: CoroutineScope by lazy {
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }

    /** 表示世代。新しい表示の開始と [hide] で進み、旧世代の終了・報告を締め出す。 */
    private var generation = 0

    /** 現在の世代に合流している利用の数。 */
    @Volatile
    private var activeCount = 0

    /** 表示中の器。取り付け先が無いときは表示が成立しないので null のままになる。 */
    @Volatile
    private var container: LoadingContainer? = null

    /** 表示中の内蔵コンテンツ。カスタム View 表示中は null。 */
    @Volatile
    private var builtinContentView: LoadingDefaultContentView? = null

    /** 表示中のカスタム View の ViewModel。既定ローディング表示中は null。 */
    private var customViewModel: LoadingViewModel? = null

    /** 表示中の中身の View。画面の再生成をまたぐ再取り付けでもこの View を載せ替える。 */
    @Volatile
    private var contentView: View? = null

    /** 中身に紐づく実効値。固定後は再取り付けでも同じ値が使われる。 */
    private var layoutSnapshot: DialogLayoutSnapshot? = null

    /** この表示の開始時に受け取った配置。再取り付けでも同じ値を使う。 */
    private var showPlacement: DialogPlacement? = null

    /** この表示で解決済みの中身の指定と factory。提示先が後から現れた場合の生成に使う。 */
    private var contentRequest: LoadingContentRequest? = null
    private var contentFactory: LoadingViewFactory? = null

    /** 器を載せている提示先。入れ替わりの判定に使う。 */
    private var attachedHost: Context? = null

    /** 提示先の入れ替わりの購読。表示していない間は持たない。 */
    private var hostRegistration: LoadingHostRegistration? = null

    /** この表示の開始時に読んだスタイル。表示中の設定変更には追随しない。 */
    private var displayedStyle = LoadingStyle()

    /** 最新のメッセージ (後勝ち)。 */
    private var latestMessage: String? = null

    /** 最新の進捗 (後勝ち)。未報告なら null。 */
    private var latestProgress: Double? = null

    /** 進行中の撤去。新しい開始はこの完了を待ってから新世代として始まる。 */
    @Volatile
    private var dismissalJob: Job? = null

    // MARK: - 観察 (テストと内部からの読み取り)

    /** 器が取り付いているか。 */
    val isPresenting: Boolean
        get() = container != null && dismissalJob == null

    /** 出の演出と撤去が進行中か。 */
    val isDismissing: Boolean
        get() = dismissalJob != null

    /** 現在の合流数。 */
    val coalescedUseCount: Int
        get() = activeCount

    /** 表示中の中身の View。 */
    val presentedContentView: View?
        get() = contentView

    /** 表示中の内蔵コンテンツ。 */
    val presentedBuiltinContentView: LoadingDefaultContentView?
        get() = builtinContentView

    /** 表示中の器。 */
    val presentedContainer: LoadingContainer?
        get() = container

    // MARK: - 合流の受理

    /**
     * 合流1件を開始する。
     *
     * 出の演出の途中なら、その撤去の完了を待ってから新しい世代として始める。
     * 構成ミス (未登録の ViewModel 型) はここで失敗するため、呼び出し元は処理を実行しない。
     */
    suspend fun beginUse(
        request: LoadingContentRequest,
        message: String?,
        placement: DialogPlacement?,
    ): LoadingUseToken {
        // 受理は UI スレッド上で確定するが、その結果を呼び出し元へ渡す直前に呼び出し元が
        // 取り消されることがある。身分証だけが失われると終了を数える者がいなくなるため、
        // 確定した身分証をここで控え、渡せなかったときはこの場で終了を数え切る
        val acquired = AtomicReference<LoadingUseToken?>(null)
        try {
            return withContext(Dispatchers.Main.immediate) {
                acquireUse(request, message, placement).also(acquired::set)
            }
        } catch (cancellation: CancellationException) {
            acquired.get()?.let { token ->
                withContext(NonCancellable) { endUse(token) }
            }
            throw cancellation
        }
    }

    /** 合流1件を受理して身分証を確定する。UI スレッド上でだけ呼ばれる。 */
    private suspend fun acquireUse(
        request: LoadingContentRequest,
        message: String?,
        placement: DialogPlacement?,
    ): LoadingUseToken {
        waitForPendingDismissal()
        if (activeCount > 0) {
            // 合流。コンテンツは最初の開始のものを維持するが、構成ミスは同じように弾く
            resolveFactory(request)
            activeCount += 1
            if (message != null) {
                latestMessage = message
                refreshBuiltinText()
            }
            return LoadingUseToken(generation)
        }
        // 新しい世代。ここで初めて中身を解決するので、失敗は開始そのものの失敗になる
        val style = settings.style
        val factory = resolveFactory(request)
        generation += 1
        activeCount = 1
        displayedStyle = style
        latestMessage = message
        latestProgress = null
        showPlacement = placement
        contentRequest = request
        contentFactory = factory
        customViewModel = (request as? LoadingContentRequest.Custom)?.viewModel
        try {
            startDisplay()
        } catch (contentFailure: Throwable) {
            // 中身の生成に失敗した開始は成立しない。合流数・購読・控えた中身をすべて戻し、
            // 後続の開始が失敗した世代へ合流しないようにする。
            // 失敗の種類で扱いを変えないのは、ここでは握り潰さず必ず呼び出し元へ投げ返すため。
            // 状態を戻すだけの後始末なので、致命的な失敗でも実行して差し支えない
            rollbackFailedStart()
            throw contentFailure
        }
        return LoadingUseToken(generation)
    }

    /** 成立しなかった開始の痕跡を消す。世代だけは進めたままにして旧い身分証を締め出す。 */
    private fun rollbackFailedStart() {
        activeCount = 0
        container?.detachForReattach()
        container = null
        clearDisplayState()
    }

    /**
     * 合流1件を終了する。旧世代の終了は現在の表示に影響しない。
     * 合流最後の1件なら器の撤去まで待ってから戻る。
     */
    suspend fun endUse(token: LoadingUseToken): Unit = withContext(Dispatchers.Main.immediate) {
        if (token.generation != generation || activeCount == 0) {
            return@withContext
        }
        activeCount -= 1
        if (activeCount > 0) {
            return@withContext
        }
        finishDisplay()
    }

    /** 合流数によらず表示を閉じる。走行中の処理には干渉しない。 */
    suspend fun hide(): Unit = withContext(Dispatchers.Main.immediate) {
        if (activeCount == 0 && container == null && dismissalJob == null) {
            return@withContext
        }
        // 走行中の利用が持つ身分証を旧世代にして、以後の終了・報告を締め出す
        generation += 1
        activeCount = 0
        finishDisplay()
    }

    /** 表示中のメッセージを更新する。合流には関与しない。 */
    suspend fun setMessage(message: String?): Unit = withContext(Dispatchers.Main.immediate) {
        if (activeCount == 0 || builtinContentView == null) {
            return@withContext
        }
        latestMessage = message
        refreshBuiltinText()
    }

    /**
     * 進捗の報告を受理する。任意のスレッドから呼べる。
     *
     * 受理は UI スレッド上で呼ばれた順に直列化され、旧世代の報告はそこで捨てられる。
     */
    fun report(progress: Double, token: LoadingUseToken) {
        scope.launch { acceptReport(progress, token) }
    }

    private fun acceptReport(progress: Double, token: LoadingUseToken) {
        if (token.generation != generation || activeCount == 0) {
            return
        }
        // 非有限値は報告そのものを無視し、直前の表示を保つ
        if (!progress.isFinite()) {
            return
        }
        val clamped = progress.coerceIn(0.0, 1.0)
        latestProgress = clamped
        refreshBuiltinText()
        (customViewModel as? LoadingProgressReceiver)?.onProgress(clamped)
    }

    // MARK: - 表示の出し入れ

    /**
     * 解決済みの中身から器を組み立てて取り付ける。
     *
     * 取り付け先が無いときは中身も器も作らない。表示は成立しないが合流状態は成立し、
     * 呼び出し元の処理は通常どおり実行される (提示環境の不在は構成ミスではない)。
     */
    private fun startDisplay() {
        observeHostChanges()
        val host = presentationSurface.hostContext ?: return
        createContent(host)
        attach(host, playsPresentation = true)
    }

    /** その提示先の Context で中身を作り、実効値の供給元を結び付ける。 */
    private fun createContent(host: Context) {
        val request = contentRequest ?: return
        val content = when (request) {
            LoadingContentRequest.Builtin -> LoadingDefaultContentView(host, displayedStyle).also {
                // 既定ローディングには利用者が属性を添付する View が無いため、
                // 設定プロパティの値をこの内蔵コンテンツへの添付として載せる (core/ADR-0022)
                it.ksDialogOptions = settings.options
                builtinContentView = it
            }

            is LoadingContentRequest.Custom ->
                checkNotNull(contentFactory).createView(host, request.viewModel)
        }
        contentView = content
        layoutSnapshot = DialogLayoutSnapshot(content, showPlacement)
        refreshBuiltinText()
    }

    /** 器を組み立てて最前面へ出す。 */
    private fun attach(host: Context, playsPresentation: Boolean) {
        val content = contentView ?: return
        val snapshot = layoutSnapshot ?: return
        val newContainer = LoadingContainer(host, content, snapshot, playsPresentation)
        container = newContainer
        attachedHost = host
        try {
            newContainer.show()
        } catch (hostUnavailable: android.view.WindowManager.BadTokenException) {
            // 提示先が既に畳まれていた。表示は成立しないが、合流状態と処理はそのまま続く
            container = null
            attachedHost = null
        }
    }

    /**
     * 表示中なら器を作り直して最前面へ載せ直す。表示していなければ何も起こらない。
     *
     * Android のウィンドウの重なりは追加順でしか決まらないため、Loading より後から出た
     * ウィンドウの上へ戻るにはこの載せ直しが要る (core/ADR-0030 の「Loading が常に前面」)。
     * 中身と実効値はそのまま引き継ぎ、演出も走らせないので見た目は連続する。
     * 入りの演出の途中で呼ばれた場合は、その演出を打ち切って中身を演出後の見えへ進める
     * (器の載せ替えが中身を途中の見えのまま固めないようにする — [LoadingContainer.detachForReattach])。
     * UI スレッドから呼ぶ。
     */
    fun bringToFrontWithoutPresentation() {
        if (container == null || dismissalJob != null) {
            return
        }
        val host = attachedHost ?: return
        container?.detachForReattach()
        container = null
        attach(host, playsPresentation = false)
    }

    /** 提示先の入れ替わりの購読を始める。表示1つにつき1回だけ張る。 */
    private fun observeHostChanges() {
        if (hostRegistration != null) {
            return
        }
        hostRegistration = presentationSurface.observeHostChange(::onHostChanged)
    }

    /**
     * 提示先が入れ替わったときに、表示を新しい画面へ載せ直す。
     *
     * Android の回転では Activity が作り直され、器のウィンドウも失われる。合流状態と中身は
     * この coordinator が持ち、器だけを使い捨てにすることで表示を継続させる。
     *
     * 中身は作り直さずに新しい画面のウィンドウへ載せ替えるため、中身が握っている Context は
     * 前の画面のものが残る。表示が閉じるまで前の画面が到達可能なままになり、中身が構成修飾
     * つきリソースを読む場合も入れ替わり前の構成の値のままになる、というトレードオフを取る。
     */
    private fun onHostChanged() {
        if (activeCount == 0 || dismissalJob != null) {
            return
        }
        val host = presentationSurface.hostContext
        if (host === attachedHost) {
            return
        }
        container?.detachForReattach()
        container = null
        attachedHost = null
        if (host == null) {
            // 提示先が不在の間は中身を抱えたまま待ち、次の入れ替わりで載せ直す
            return
        }
        if (contentView == null) {
            // 提示先が無いまま始まった表示。ここで初めて中身を作るので入りの演出から始める。
            // ここは提示先の入れ替わり通知の中であり、既に走り出した処理の途中でもある。
            // 中身の生成の失敗を通知元へ投げ返すと利用者アプリを巻き込むため、この表示は
            // 中身なしとして諦める (合流状態は残るので、走行中の処理はそのまま完了できる)。
            // 諦めるのは中身の作り手が投げる通常の失敗までとし、実行の継続そのものが
            // 成り立たない致命的な失敗 (メモリ枯渇など) は隠さずそのまま伝える
            try {
                createContent(host)
            } catch (contentFailure: Exception) {
                // 生成できない中身を次の入れ替わりで作り直しても同じ失敗を繰り返すため、諦めを控える。
                // 表示が出ないまま処理だけが進む状態の唯一の手掛かりになるので、理由を記録に残す
                Log.w(LOG_TAG, "Could not create the Loading content. Nothing is presented.", contentFailure)
                abandonContent()
                return
            }
            attach(host, playsPresentation = true)
            return
        }
        attach(host, playsPresentation = false)
    }

    /** 出の演出の途中なら、その撤去の完了まで待つ。待つ間に別の撤去が始まっていたらもう一度待つ。 */
    private suspend fun waitForPendingDismissal() {
        while (true) {
            val pending = dismissalJob ?: return
            pending.join()
            // 後始末 (completeDismissal) まで済んでいれば控えは null になっており、次の周回で抜ける。
            // 別の撤去が始まっていれば控えが別の Job に変わっているので、その完了まで待ち直す。
            // 控えが同じ Job のまま残るのは後始末を経ずに join が解けた場合で、
            // 待ち直しても解けないため無限ループを避けてここで抜ける
            if (dismissalJob === pending) {
                return
            }
        }
    }

    /**
     * 出の演出と撤去を進め、完了してから戻る。
     * 撤去が既に進行中なら、その完了に合流する。
     */
    private suspend fun finishDisplay() {
        dismissalJob?.let {
            it.join()
            return
        }
        val presented = container
        if (presented == null) {
            clearDisplayState()
            return
        }
        // 撤去の進行は控えてから走らせる。先に走らせると、中断を挟まない経路で
        // 後始末が控えるより先に済み、控えが残り続けてしまう
        val job = scope.launch(start = CoroutineStart.LAZY) {
            presented.runDismissal()
            completeDismissal()
        }
        dismissalJob = job
        job.join()
    }

    /** 撤去の完了後の後始末。待っている呼び出しはこの後始末のあとに戻る。 */
    private fun completeDismissal() {
        container = null
        clearDisplayState()
        dismissalJob = null
    }

    /**
     * 中身の生成を諦める。合流状態はそのまま残し、表示だけを持たない状態にする。
     *
     * 中身の指定を手放すので、以後の提示先の入れ替わりでも生成をやり直さない。
     */
    private fun abandonContent() {
        builtinContentView = null
        contentView = null
        layoutSnapshot = null
        contentRequest = null
        contentFactory = null
    }

    private fun clearDisplayState() {
        hostRegistration?.cancel()
        hostRegistration = null
        attachedHost = null
        builtinContentView = null
        customViewModel = null
        contentView = null
        layoutSnapshot = null
        showPlacement = null
        contentRequest = null
        contentFactory = null
        latestMessage = null
        latestProgress = null
    }

    /**
     * 最新のメッセージと進捗から表示テキストを組み立て直す。
     * フォーマット関数はこの UI スレッド上で呼ばれる。
     */
    private fun refreshBuiltinText() {
        val builtin = builtinContentView ?: return
        val message = latestMessage ?: displayedStyle.defaultMessage
        builtin.apply(displayedStyle.progressFormat(message, latestProgress))
    }

    // MARK: - 中身の解決

    /**
     * 中身の生成に使う factory を解決する。既定ローディングでは null を返す。
     *
     * 未登録の ViewModel 型は構成ミスとして失敗し、表示も処理も行われない (fail-fast)。
     * インライン表示はレジストリを読まないので、登録の有無は表示にも登録内容にも影響しない
     * (core/ADR-0013)。型指定経路は呼び出し時点で解決を終えているため、ここでは引き直さない。
     */
    private fun resolveFactory(request: LoadingContentRequest): LoadingViewFactory? = when (request) {
        LoadingContentRequest.Builtin -> null

        is LoadingContentRequest.Inline -> {
            request.viewModel::class.requireReferenceTypeViewModel()
            request.factory
        }

        is LoadingContentRequest.Resolved -> request.factory

        is LoadingContentRequest.Registered -> {
            val viewModelClass = request.viewModel::class
            viewModelClass.requireReferenceTypeViewModel()
            registry.factory(viewModelClass)
                ?: throw DialogException.ViewFactoryNotRegistered(viewModelClass.viewModelTypeName)
        }
    }

    companion object {
        /** 既定の入口が共有する唯一の coordinator。 */
        val shared: LoadingCoordinator = LoadingCoordinator()

        /** 警告ログのタグ。ライブラリ全体で同じ名前を使う。 */
        const val LOG_TAG = "KsDialogs"
    }
}
