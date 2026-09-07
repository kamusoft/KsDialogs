package jp.kamusoft.ksdialogs.samples.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.DialogAlignment
import jp.kamusoft.ksdialogs.DialogPlacement
import jp.kamusoft.ksdialogs.DialogResult
import jp.kamusoft.ksdialogs.Loading
import jp.kamusoft.ksdialogs.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** メニュー画面と、レイアウト属性・トランジションの調整パネルを表示する画面。 */
internal class MainActivity : ComponentActivity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var menuView: SampleMenuView
    private var layoutPanelView: SampleLayoutPanelView? = null
    private var transitionPanelView: SampleTransitionPanelView? = null

    /**
     * パネルを開いている間の戻る操作を受け、メニューへ戻す受け口。
     *
     * 開いていない間は無効にしておき、戻る操作を OS 既定 (画面の終了) に委ねる。
     */
    private val panelBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            if (layoutPanelView?.isAttachedToWindow == true) {
                closeLayoutPanel()
            } else if (transitionPanelView?.isAttachedToWindow == true) {
                closeTransitionPanel()
            }
        }
    }

    /** 刻みごとの待ち時間 (ミリ秒)。起動引数の指定があればその値になる。 */
    private var loadingStepIntervalMilliseconds = DEFAULT_LOADING_STEP_INTERVAL_MILLISECONDS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        menuView = SampleMenuView(
            context = this,
            onBasicDialogSelected = ::showBasicDialog,
            onLayoutDialogSelected = ::openLayoutPanel,
            onDeclarativeDialogSelected = ::showDeclarativeDialog,
            onTextInputDialogSelected = ::showTextInputDialog,
            onInlineDialogSelected = ::showInlineDialog,
            onTransitionDialogSelected = ::openTransitionPanel,
            onModelDialogSelected = ::showModelDialog,
            onDefaultLoadingSelected = ::runDefaultLoading,
            onCustomLoadingSelected = ::runCustomLoading,
            onDefaultToastSelected = ::showDefaultToast,
            onCustomToastSelected = ::showCustomToast,
            onToastStackSelected = ::showToastStack,
            onToastPlacementSelected = ::showToastPlacement,
            onToastOverlapSelected = ::runToastOverlap,
        )
        setContentView(menuView)
        onBackPressedDispatcher.addCallback(this, panelBackCallback)

        val options = SampleCaptureOptions.from(intent)
        loadingStepIntervalMilliseconds =
            options.loadingStepIntervalMilliseconds ?: DEFAULT_LOADING_STEP_INTERVAL_MILLISECONDS
        if (savedInstanceState == null) {
            autoPlay(options.demo)
        }
    }

    /**
     * 起動引数で指定されたデモを、メニュー項目のタップと同じ入口で自動再生する。
     *
     * 再生はプロセスの起動につき 1 回だけで、画面の再生成では再生しない。
     * ダイアログの提示先はメニューが画面に載ってから決まるため、再生もその時点まで待つ。
     */
    private fun autoPlay(demo: SampleDemoId?) {
        if (demo == null || autoPlayConsumed) {
            return
        }
        autoPlayConsumed = true
        menuView.post { play(demo) }
    }

    /** メニュー項目のタップハンドラと同じ入口を呼ぶ。 */
    private fun play(demo: SampleDemoId) {
        when (demo) {
            SampleDemoId.BASIC_DIALOG -> showBasicDialog()
            SampleDemoId.DECLARATIVE_DIALOG -> showDeclarativeDialog()
            SampleDemoId.MODEL_DIALOG -> showModelDialog()
            SampleDemoId.TEXT_INPUT_DIALOG -> showTextInputDialog()
            SampleDemoId.INLINE_DIALOG -> showInlineDialog()
            SampleDemoId.TRANSITION_DIALOG -> openTransitionPanel()
            SampleDemoId.LAYOUT_DIALOG -> openLayoutPanel()
            SampleDemoId.DEFAULT_LOADING -> runDefaultLoading()
            SampleDemoId.CUSTOM_LOADING -> runCustomLoading()
            SampleDemoId.DEFAULT_TOAST -> showDefaultToast()
            SampleDemoId.CUSTOM_TOAST -> showCustomToast()
            SampleDemoId.TOAST_STACK -> showToastStack()
            SampleDemoId.TOAST_PLACEMENT -> showToastPlacement()
            SampleDemoId.TOAST_OVERLAP -> runToastOverlap()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    /** Basic Dialog を表示し、結果を直近の結果として取り込む。 */
    private fun showBasicDialog() {
        scope.launch {
            val viewModel = BasicDialogViewModel(SampleText.BASIC_DIALOG_MESSAGE)
            menuView.showResult(displayText(Dialog.instance.show(viewModel)))
        }
    }

    /**
     * Declarative Dialog を表示し、結果を直近の結果として取り込む。
     *
     * 中身が Compose で書かれていても、呼び出し方も結果の返り方も Basic Dialog と変わらない。
     */
    private fun showDeclarativeDialog() {
        scope.launch {
            val viewModel = DeclarativeDialogViewModel(SampleText.DECLARATIVE_DIALOG_MESSAGE)
            menuView.showResult(displayText(Dialog.instance.show(viewModel)))
        }
    }

    /**
     * Model Dialog を表示し、結果を直近の結果として取り込む。
     *
     * ViewModel のインスタンスは渡さず、クラス参照と configure だけを渡す。
     * 生成はレジストリの ViewModel factory が行い、結果は ViewModel 自身が報告する。
     */
    private fun showModelDialog() {
        scope.launch {
            val result = Dialog.instance.show(ModelDialogViewModel::class) { viewModel ->
                viewModel.message = SampleText.MODEL_DIALOG_MESSAGE
            }
            menuView.showResult(displayText(result))
        }
    }

    /** Text Input Dialog を表示し、入力された文字列を直近の結果として取り込む。 */
    private fun showTextInputDialog() {
        scope.launch {
            val viewModel = TextInputDialogViewModel(SampleText.TEXT_INPUT_DIALOG_MESSAGE)
            val result = when (val outcome = Dialog.instance.show(viewModel)) {
                is DialogResult.Completed -> SampleText.completedResult(outcome.value)
                DialogResult.Cancelled -> SampleText.CANCELLED_RESULT
            }
            menuView.showResult(result)
        }
    }

    /**
     * Inline Dialog を表示し、結果を直近の結果として取り込む。
     *
     * 中身はこの場で渡すため、この ViewModel 型はレジストリに登録していない (core/ADR-0013)。
     */
    private fun showInlineDialog() {
        scope.launch {
            val viewModel = InlineDialogViewModel(SampleText.INLINE_DIALOG_MESSAGE)
            val result = Dialog.instance.show(viewModel) { suppliedViewModel, notifier ->
                InlineDialogCardView(
                    context = this,
                    message = suppliedViewModel.message,
                    onCancel = { notifier.cancel() },
                    onComplete = { notifier.complete(true) },
                )
            }
            menuView.showResult(displayText(result))
        }
    }

    /**
     * Default Loading を実行し、完了を直近の結果として取り込む。
     *
     * スコープ形の start は処理の間だけ既定ローディングを出し、処理の完了で自動的に閉じる。
     * 処理は 0 から 1 まで進捗を段階的に報告し、途中で表示中のメッセージを差し替える。
     */
    private fun runDefaultLoading() {
        scope.launch {
            Loading.instance.start(message = SampleText.LOADING_START_MESSAGE) { report ->
                for (step in 0..LOADING_STEP_COUNT) {
                    report(step.toDouble() / LOADING_STEP_COUNT)
                    if (step == LOADING_MESSAGE_UPDATE_STEP) {
                        Loading.instance.setMessage(SampleText.LOADING_UPDATE_MESSAGE)
                    }
                    delay(loadingStepIntervalMilliseconds)
                }
            }
            menuView.showResult(SampleText.LOADING_COMPLETED_RESULT)
        }
    }

    /**
     * Custom Loading を実行し、完了を直近の結果として取り込む。
     *
     * 呼び出しの形は Default Loading と同じスコープ形で、渡すのが登録済みの ViewModel の**型**である
     * 点だけが違う。実体はレジストリの ViewModel factory が作る。
     * 報告した進捗は ViewModel の受け口へ転送され、中身のカスタム View がそれを読んで表示を更新する。
     */
    private fun runCustomLoading() {
        scope.launch {
            Loading.instance.start(CustomLoadingViewModel::class) { report ->
                for (step in 0..LOADING_STEP_COUNT) {
                    report(step.toDouble() / LOADING_STEP_COUNT)
                    delay(loadingStepIntervalMilliseconds)
                }
            }
            menuView.showResult(SampleText.LOADING_COMPLETED_RESULT)
        }
    }

    /**
     * Default Toast を表示する。
     *
     * duration も配置も渡さないので、デフォルト View が契約既定の配置に出て既定 duration で消える。
     * Toast は fire-and-forget なので戻り値も待機もなく、結果表示も変えない (core/ADR-0031)。
     */
    private fun showDefaultToast() {
        Toast.instance.show(SampleText.DEFAULT_TOAST_MESSAGE)
    }

    /**
     * Custom Toast を表示する。
     *
     * 登録経路とインライン経路の 2 枚を続けて出す。同じ配置では重なって見分けられないため、
     * 上方向オフセットを変えて 2 段に置く。
     */
    private fun showCustomToast() {
        // 登録経路は型を渡し、実体はレジストリの ViewModel factory が作る。文言は configure で入れる
        Toast.instance.show(
            viewModelClass = CustomToastViewModel::class,
            durationMs = CUSTOM_TOAST_REGISTERED_DURATION_MS,
            placement = bottomToastPlacement(TOAST_MIDDLE_OFFSET_Y),
            configure = { viewModel -> viewModel.message = SampleText.CUSTOM_TOAST_MESSAGE },
        )
        // 中身はこの場で渡すため、この ViewModel 型は登録していない (core/ADR-0013)
        Toast.instance.show(
            viewModel = InlineToastViewModel(SampleText.INLINE_TOAST_MESSAGE),
            durationMs = CUSTOM_TOAST_INLINE_DURATION_MS,
            placement = bottomToastPlacement(TOAST_LOWER_OFFSET_Y),
        ) { suppliedViewModel ->
            InlineToastCardView(context = this, viewModel = suppliedViewModel)
        }
    }

    /**
     * Toast Stack を表示する。
     *
     * 3 枚を続けて出し、duration の短いものから独立して消える様子を見せる。
     * 3 枚目は長文で、デフォルト View が複数行に折り返して高さを伸ばすことを確かめる。
     */
    private fun showToastStack() {
        Toast.instance.show(
            message = SampleText.TOAST_STACK_FIRST_MESSAGE,
            durationMs = TOAST_STACK_FIRST_DURATION_MS,
            placement = bottomToastPlacement(TOAST_LOWER_OFFSET_Y),
        )
        Toast.instance.show(
            message = SampleText.TOAST_STACK_SECOND_MESSAGE,
            durationMs = TOAST_STACK_SECOND_DURATION_MS,
            placement = bottomToastPlacement(TOAST_MIDDLE_OFFSET_Y),
        )
        Toast.instance.show(
            message = SampleText.TOAST_STACK_THIRD_MESSAGE,
            durationMs = TOAST_STACK_THIRD_DURATION_MS,
            placement = bottomToastPlacement(TOAST_UPPER_OFFSET_Y),
        )
    }

    /** Toast Placement を表示する。show の引数で契約既定と違う配置へ上書きする。 */
    private fun showToastPlacement() {
        Toast.instance.show(
            message = SampleText.TOAST_PLACEMENT_MESSAGE,
            durationMs = TOAST_PLACEMENT_DURATION_MS,
            placement = TOP_TOAST_PLACEMENT,
        )
    }

    /**
     * Toast Overlap を実行し、時系列の完了を直近の結果として取り込む。
     *
     * 操作者に依存しない固定の時系列で自動進行する — Toast を出したまま Dialog を重ね、
     * Dialog を閉じたあと Loading を重ねる。Loading は Toast より前面に出る (core/ADR-0030)。
     * Loading が終わっても Toast は残っており、duration の満了で消えてから結果を出す。
     */
    private fun runToastOverlap() {
        scope.launch {
            Toast.instance.show(
                message = SampleText.TOAST_OVERLAP_MESSAGE,
                durationMs = TOAST_OVERLAP_TOAST_DURATION_MS,
            )

            // 待機を打ち切るとダイアログは cancelled で確定して閉じる。結果は使わない
            val dialogViewModel = BasicDialogViewModel(SampleText.BASIC_DIALOG_MESSAGE)
            withTimeoutOrNull(TOAST_OVERLAP_DIALOG_DURATION_MS) {
                Dialog.instance.show(dialogViewModel)
            }

            Loading.instance.start(message = SampleText.LOADING_START_MESSAGE) {
                delay(TOAST_OVERLAP_LOADING_DURATION_MS)
            }

            // Toast の残り時間 (と満了を跨ぐ余白) を待ってから結果を出す
            delay(
                TOAST_OVERLAP_TOAST_DURATION_MS - TOAST_OVERLAP_DIALOG_DURATION_MS -
                    TOAST_OVERLAP_LOADING_DURATION_MS + TOAST_OVERLAP_RESULT_MARGIN_MS,
            )
            menuView.showResult(SampleText.TOAST_OVERLAP_COMPLETED_RESULT)
        }
    }

    /** 可視領域の下部中央から上方向へ動かした配置。 */
    private fun bottomToastPlacement(offsetY: Double): DialogPlacement = DialogPlacement(
        horizontalAlignment = DialogAlignment.CENTER,
        verticalAlignment = DialogAlignment.END,
        offsetY = offsetY,
    )

    /** 属性調整パネルを開く。開いている間の状態は同じ View に保たれる。 */
    private fun openLayoutPanel() {
        val panel = layoutPanelView ?: SampleLayoutPanelView(
            context = this,
            onBack = ::closeLayoutPanel,
            onShow = ::showLayoutDialog,
        ).also { layoutPanelView = it }
        setContentView(panel)
        panelBackCallback.isEnabled = true
    }

    /** メニュー画面へ戻る。 */
    private fun closeLayoutPanel() {
        setContentView(menuView)
        panelBackCallback.isEnabled = false
    }

    /** パネルで調整した属性でダイアログを表示し、結果をパネルとメニューの両方へ出す。 */
    private fun showLayoutDialog() {
        val panel = layoutPanelView ?: return
        scope.launch {
            val viewModel = LayoutDialogViewModel(
                message = SampleText.LAYOUT_DIALOG_MESSAGE,
                usesVisibleArea = panel.usesVisibleArea,
            )
            // 置き場所は呼び出しごとに変わるので show の引数で渡す
            val result = displayText(Dialog.instance.show(viewModel, panel.placement()))
            panel.showResult(result)
            menuView.showResult(result)
        }
    }

    /** トランジションデモ画面を開く。開いている間の状態は同じ View に保たれる。 */
    private fun openTransitionPanel() {
        val panel = transitionPanelView ?: SampleTransitionPanelView(
            context = this,
            onBack = ::closeTransitionPanel,
            onShow = ::showTransitionDialog,
        ).also { transitionPanelView = it }
        setContentView(panel)
        panelBackCallback.isEnabled = true
    }

    /** メニュー画面へ戻る。 */
    private fun closeTransitionPanel() {
        setContentView(menuView)
        panelBackCallback.isEnabled = false
    }

    /** デモ画面で選んだ演出でダイアログを表示し、結果をデモ画面とメニューの両方へ出す。 */
    private fun showTransitionDialog() {
        val panel = transitionPanelView ?: return
        scope.launch {
            val viewModel = TransitionDialogViewModel(
                message = SampleText.TRANSITION_DIALOG_MESSAGE,
                transition = panel.transition(),
            )
            val result = displayText(Dialog.instance.show(viewModel))
            panel.showResult(result)
            menuView.showResult(result)
        }
    }

    private fun displayText(result: DialogResult<Boolean>): String = when (result) {
        is DialogResult.Completed -> SampleText.completedResult(result.value)
        DialogResult.Cancelled -> SampleText.CANCELLED_RESULT
    }

    private companion object {
        /** 進捗を報告する刻みの数。0 から 1 までをこの数で割った値を順に報告する。 */
        const val LOADING_STEP_COUNT = 4

        /** メッセージを差し替える刻み。 */
        const val LOADING_MESSAGE_UPDATE_STEP = 2

        /** 刻みごとの待ち時間 (ミリ秒) の既定値。 */
        const val DEFAULT_LOADING_STEP_INTERVAL_MILLISECONDS = 400L

        /** Toast を重ねて置くときの上方向オフセット (論理単位) の下段。契約既定と同じ高さ。 */
        const val TOAST_LOWER_OFFSET_Y = -80.0

        /** 重ねて置くときの中段。 */
        const val TOAST_MIDDLE_OFFSET_Y = -160.0

        /** 重ねて置くときの上段。 */
        const val TOAST_UPPER_OFFSET_Y = -240.0

        /** 上部中央へ置くときの下方向オフセット (論理単位)。 */
        const val TOAST_TOP_OFFSET_Y = 80.0

        /** 可視領域の上部中央へ置く配置。契約既定 (下部中央) と対になる位置。 */
        val TOP_TOAST_PLACEMENT = DialogPlacement(
            horizontalAlignment = DialogAlignment.CENTER,
            verticalAlignment = DialogAlignment.START,
            offsetY = TOAST_TOP_OFFSET_Y,
        )

        /** Custom Toast の登録経路の表示時間 (ミリ秒)。 */
        const val CUSTOM_TOAST_REGISTERED_DURATION_MS = 3000

        /** Custom Toast のインライン経路の表示時間 (ミリ秒)。登録経路より先に消える値にする。 */
        const val CUSTOM_TOAST_INLINE_DURATION_MS = 2000

        /** Toast Stack の 1 枚目の表示時間 (ミリ秒)。 */
        const val TOAST_STACK_FIRST_DURATION_MS = 2000

        /** Toast Stack の 2 枚目の表示時間 (ミリ秒)。 */
        const val TOAST_STACK_SECOND_DURATION_MS = 3000

        /** Toast Stack の 3 枚目の表示時間 (ミリ秒)。 */
        const val TOAST_STACK_THIRD_DURATION_MS = 4000

        /** Toast Placement の表示時間 (ミリ秒)。配置を見比べられるよう既定より長くする。 */
        const val TOAST_PLACEMENT_DURATION_MS = 3000

        /** Toast Overlap の Toast の表示時間 (ミリ秒)。Dialog と Loading の時系列を跨ぐ長さにする。 */
        const val TOAST_OVERLAP_TOAST_DURATION_MS = 10000

        /** Toast Overlap の Dialog を出しておく時間 (ミリ秒)。 */
        const val TOAST_OVERLAP_DIALOG_DURATION_MS = 2000L

        /** Toast Overlap の Loading を出しておく時間 (ミリ秒)。 */
        const val TOAST_OVERLAP_LOADING_DURATION_MS = 2000L

        /** Toast の満了を確実に過ぎてから結果を出すための余白 (ミリ秒)。 */
        const val TOAST_OVERLAP_RESULT_MARGIN_MS = 500L

        /** 自動再生を消費したかどうか。プロセスの生存期間で 1 回に限るために持つ。 */
        var autoPlayConsumed = false
    }
}
