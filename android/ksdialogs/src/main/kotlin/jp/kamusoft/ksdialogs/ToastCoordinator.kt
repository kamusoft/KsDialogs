package jp.kamusoft.ksdialogs

import android.content.Context
import android.os.SystemClock
import android.util.Log
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Loading を Toast より前面に保つための依頼口。
 *
 * Android のウィンドウの重なりは追加順でしか決まらないため、Toast のウィンドウを出した後に
 * Loading を演出なしで載せ直してもらうことで「起動順によらず常に Loading が前面」を保つ
 * (core/ADR-0030)。依頼の向きは Toast から Loading への一方向で、Loading 側は Toast を知らない。
 */
internal fun interface ToastLoadingFrontKeeper {
    /** Loading が表示中なら演出なしで最前面へ載せ直す。表示していなければ何も起こらない。 */
    fun bringLoadingToFront()
}

/**
 * 1 OS プロセス内で唯一の Toast の状態の正 (core/ADR-0030)。
 *
 * 表示中の Toast のリスト (起動順)・各表示の器・各表示の消滅の期限を保持し、
 * 既定シングルトンも DI 注入したインスタンスも、すべての入口がここへ委譲する。
 * これにより入口をまたいだ利用でも、重なり順と消滅の管理が1か所にまとまる。
 *
 * 受理は任意のスレッドから行え、UI スレッド上で受理順に直列化される。
 * 構成ミス (未登録の ViewModel 型) だけは受理そのものの失敗として呼び出し元へ返し、
 * それ以降の失敗は表示1枚の破棄に留める (表示は fire-and-forget なので返せない — core/ADR-0031)。
 */
internal class ToastCoordinator(
    /** カスタム Toast の登録。 */
    val registry: ToastViewRegistry = ToastViewRegistry.shared,
    /** 一括設定。 */
    val settings: ToastSettings = ToastSettings(),
    private val presentationSurface: ToastPresentationSurface = ActivityToastPresentationSurface(),
    private val announcer: ToastAccessibilityAnnouncer = SystemToastAccessibilityAnnouncer(),
    private val loadingFrontKeeper: ToastLoadingFrontKeeper =
        ToastLoadingFrontKeeper { LoadingCoordinator.shared.bringToFrontWithoutPresentation() },
) {

    /**
     * 受理と撤去を進める文脈。
     *
     * 受理は UI スレッドへ積まれた順に実行されるため、重なり順は起動順どおりになる。
     * 呼び出し元のコルーチンとは切り離してあるので、呼び出し元が取り消されても表示は続く。
     */
    private val scope: CoroutineScope by lazy {
        CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }

    /** 表示中の Toast。並びがそのまま起動順で、後ろほど手前に重なる。 */
    private val displays = mutableListOf<ToastDisplay>()

    /** 提示先の入れ替わりの購読。表示が1枚も無い間は持たない。 */
    private var hostRegistration: ToastHostRegistration? = null

    // MARK: - 観察 (テストと内部からの読み取り)

    /** 表示中の Toast の枚数 (取り付け先待ちのものを含む)。 */
    val displayCount: Int
        get() = displays.size

    /** 取り付け済みの器。並びは起動順。 */
    val presentedContainers: List<ToastContainer>
        get() = displays.mapNotNull { it.container }

    /** 取り付け済みの器に載っている中身の View。並びは起動順。 */
    val presentedContentViews: List<View>
        get() = presentedContainers.map { it.contentView }

    /** 表示中のデフォルト View。並びは起動順。 */
    val presentedDefaultContentViews: List<ToastDefaultContentView>
        get() = presentedContentViews.filterIsInstance<ToastDefaultContentView>()

    /** 何かが取り付いているか。 */
    val isPresenting: Boolean
        get() = presentedContainers.isNotEmpty()

    // MARK: - 受理

    /**
     * 表示1枚を受理する。戻り値は持たず、表示の終了も待たない (core/ADR-0031)。
     *
     * 登録経路で ViewModel 型が未登録なら、ここで同期に失敗して表示は行われない。
     *
     * @param request 表示する中身の指定
     * @param duration 表示するミリ秒。null なら [ToastStyle] の既定 duration
     * @param placement 配置。null なら添付・style のアプリ既定配置・契約既定値の順に委ねる
     */
    fun accept(request: ToastContentRequest, duration: Int?, placement: DialogPlacement?) {
        val factory = resolveFactory(request)
        val style = settings.style
        val durationMillis = effectiveDuration(duration, style)
        // 計時は受理時点から始まり、実時間で消費する (アプリが背面にある間も進む)
        val deadline = SystemClock.elapsedRealtime() + durationMillis
        val display = ToastDisplay(
            request = request,
            factory = factory,
            style = style,
            showPlacement = placement,
            fallbackPlacement = style.defaultPlacement ?: ToastPlacementDefault.placement,
            deadlineElapsedRealtime = deadline,
        )
        scope.launch { beginDisplay(display) }
    }

    // MARK: - 表示の出し入れ

    /** 受理した1枚を表示に載せ、期限を待ち始める。 */
    private fun beginDisplay(display: ToastDisplay) {
        if (hasReachedDeadline(display)) {
            // 受理から UI スレッドへ届くまでの遅れだけで期限を越えた表示。
            // 表示リストにも載せずに捨てる (満了した表示は表示されない)
            display.isFinishing = true
            return
        }
        displays.add(display)
        observeHostChanges()
        if (attachIfPossible(display)) {
            // ウィンドウの重なりは追加順で決まるため、Toast を出した直後に Loading を前面へ戻す
            loadingFrontKeeper.bringLoadingToFront()
        }
        if (display.isFinishing) {
            // 中身を作れず、載せる前に破棄された表示。期限を待つ意味がない
            return
        }
        display.timerJob = scope.launch {
            delay(display.deadlineElapsedRealtime - SystemClock.elapsedRealtime())
            finish(display)
        }
    }

    /**
     * 提示先があれば中身と器を組み立てて重ねる。取り付けたかどうかを返す。
     *
     * 提示先が無いときは表示を保留し、出現を待つ。計時は受理時点から進んでいるため、
     * 現れないまま期限が来た表示は表示されずに破棄される。期限の確認はここでも行う —
     * 提示先の復帰が期限のタイマーより先に走っても、満了した表示を一瞬見せないようにする。
     */
    private fun attachIfPossible(display: ToastDisplay): Boolean {
        if (display.container != null || display.isFinishing) {
            return false
        }
        if (hasReachedDeadline(display)) {
            discard(display)
            return false
        }
        val host = presentationSurface.hostContext ?: return false
        if (display.contentView == null) {
            // 中身の実体化の失敗は受理の後なので呼び出し元へは返せない。
            // この1枚だけを破棄して資源を解放し、他の表示には影響させない。
            // 諦めるのは中身の作り手が投げる通常の失敗までとし、実行の継続そのものが
            // 成り立たない致命的な失敗 (メモリ枯渇など) は隠さずそのまま伝える。
            // 互換面 (MAUI / KMP) 経由の中身も、向こう側の失敗は境界を跨がずに
            // 中身なしとして返り、互換面がこの受け皿に届く通常の失敗へ変換する (core/ADR-0033)
            try {
                createContent(display, host)
            } catch (contentFailure: Exception) {
                Log.w(LOG_TAG, "Could not create the Toast content. This presentation is discarded.", contentFailure)
                discard(display)
                return false
            }
        }
        val content = display.contentView ?: return false
        val snapshot = display.layoutSnapshot ?: return false
        val playsPresentation = !display.hasPlayedPresentation
        val container = ToastContainer(host, content, snapshot, playsPresentation)
        display.container = container
        display.attachedHost = host
        try {
            container.show()
        } catch (hostUnavailable: android.view.WindowManager.BadTokenException) {
            // 提示先が既に畳まれていた。この表示は次の入れ替わりで載せ直す
            display.container = null
            display.attachedHost = null
            return false
        }
        display.hasPlayedPresentation = true
        return true
    }

    /** 消滅の期限に達したか。計時は受理時点から単調時計で進む。 */
    private fun hasReachedDeadline(display: ToastDisplay): Boolean =
        SystemClock.elapsedRealtime() >= display.deadlineElapsedRealtime

    /** その提示先の Context で中身を作り、実効値の供給元を結び付ける。 */
    private fun createContent(display: ToastDisplay, host: Context) {
        val request = display.request
        val content = when (request) {
            is ToastContentRequest.Builtin ->
                ToastDefaultContentView(host, request.message, display.style, announcer)

            is ToastContentRequest.Custom ->
                checkNotNull(display.factory).createView(host, request.viewModel)

            is ToastContentRequest.Typed -> {
                // ViewModel の生成と configure はここ (UI スレッド上の受理順) で行う。
                // 解決は受理の時点で終わっているため、レジストリは引き直さない。
                // 作った ViewModel は他の経路と同じく撤去まで表示が握る
                val viewModel = request.prepare()
                display.typedViewModel = viewModel
                checkNotNull(display.factory).createView(host, viewModel)
            }
        }
        display.contentView = content
        display.layoutSnapshot = toastLayoutSnapshot(
            contentView = content,
            showPlacement = display.showPlacement,
            fallbackPlacement = display.fallbackPlacement,
        )
    }

    /** 出の演出と撤去を進め、表示リストから外す。期限の到達で呼ばれる。 */
    private suspend fun finish(display: ToastDisplay) {
        if (display.isFinishing) {
            return
        }
        display.isFinishing = true
        display.container?.runDismissal()
        removeDisplay(display)
    }

    /** 表示を成立しなかったものとして捨てる。演出は走らせない。 */
    private fun discard(display: ToastDisplay) {
        display.isFinishing = true
        display.timerJob?.cancel()
        display.container?.detachForReattach()
        removeDisplay(display)
    }

    /** 表示リストから外し、握っていた参照を手放す。 */
    private fun removeDisplay(display: ToastDisplay) {
        display.releaseResources()
        displays.remove(display)
        if (displays.isEmpty()) {
            hostRegistration?.cancel()
            hostRegistration = null
        }
    }

    // MARK: - 提示先の入れ替わり

    /** 提示先の入れ替わりの購読を始める。表示が並んでいる間は1本だけ張る。 */
    private fun observeHostChanges() {
        if (hostRegistration != null) {
            return
        }
        hostRegistration = presentationSurface.observeHostChange(::onHostChanged)
    }

    /**
     * 提示先が入れ替わったときに、表示中のすべての Toast を新しい画面へ載せ直す。
     *
     * Android の回転では Activity が作り直され、器のウィンドウも失われる。表示のリストと中身は
     * この coordinator が持ち、器だけを使い捨てにすることで表示を継続させる。
     * 載せ直しは起動順に行うので、重なり順は入れ替わりの前後で変わらない。
     * 期限は受理時点から数えているため、載せ直しでは残り時間も巻き戻らない。
     *
     * 中身は作り直さずに新しい画面のウィンドウへ載せ替えるため、中身が握っている Context は
     * 前の画面のものが残る。表示が消えるまで前の画面が到達可能なままになる、という
     * トレードオフを取る (Loading の再取り付けと同じ)。
     */
    private fun onHostChanged() {
        val host = presentationSurface.hostContext
        displays.toList().forEach { display ->
            if (display.attachedHost !== host) {
                display.container?.detachForReattach()
                display.container = null
                display.attachedHost = null
            }
        }
        if (host == null) {
            // 提示先が不在の間は中身を抱えたまま待ち、次の入れ替わりで載せ直す
            return
        }
        var didAttachAny = false
        displays.toList().forEach { display ->
            if (attachIfPossible(display)) {
                didAttachAny = true
            }
        }
        if (didAttachAny) {
            // 前面化は載せ直した枚数によらず1回で足りる (最後に載せた Toast より後へ回れば良い)
            loadingFrontKeeper.bringLoadingToFront()
        }
    }

    // MARK: - 中身の解決

    /**
     * 中身の生成に使う factory を解決する。デフォルト View では null を返す。
     *
     * 未登録の ViewModel 型は構成ミスとして失敗し、表示は行われない (fail-fast)。
     * インライン表示はレジストリを読まないので、登録の有無は表示にも登録内容にも影響しない
     * (core/ADR-0013)。型指定経路は呼び出し時点で解決を終えているため、ここでは引き直さない。
     */
    private fun resolveFactory(request: ToastContentRequest): ToastViewFactory? = when (request) {
        is ToastContentRequest.Builtin -> null

        is ToastContentRequest.Inline -> {
            request.viewModel::class.requireReferenceTypeViewModel()
            request.factory
        }

        is ToastContentRequest.Typed -> request.factory

        is ToastContentRequest.Registered -> {
            val viewModelClass = request.viewModel::class
            viewModelClass.requireReferenceTypeViewModel()
            registry.factory(viewModelClass)
                ?: throw DialogException.ViewFactoryNotRegistered(viewModelClass.viewModelTypeName)
        }
    }

    companion object {
        /** 既定の入口が共有する唯一の coordinator。 */
        val shared: ToastCoordinator = ToastCoordinator()

        /** 警告ログのタグ。ライブラリ全体で同じ名前を使う。 */
        const val LOG_TAG = "KsDialogs"

        /**
         * 有効な duration (ミリ秒) を決める。
         *
         * 0 以下の引数は style の既定へ、style の既定自体が 0 以下なら内蔵既定へ丸め、警告を残す。
         * 上限のクランプは設けない (core/ADR-0031)。
         */
        fun effectiveDuration(duration: Int?, style: ToastStyle): Int {
            if (duration != null) {
                if (duration > 0) {
                    return duration
                }
                Log.w(LOG_TAG, "Toast duration must be a positive integer. Showing with the default duration.")
            }
            if (style.defaultDuration > 0) {
                return style.defaultDuration
            }
            Log.w(LOG_TAG, "The default duration of ToastStyle is not a positive integer. Showing with the built-in default.")
            return ToastStyle.BUILTIN_DEFAULT_DURATION
        }
    }
}
