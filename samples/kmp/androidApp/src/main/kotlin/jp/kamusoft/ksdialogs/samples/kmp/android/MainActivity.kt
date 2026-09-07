package jp.kamusoft.ksdialogs.samples.kmp.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.DialogAlignment
import jp.kamusoft.ksdialogs.DialogPlacement
import jp.kamusoft.ksdialogs.DialogResult
import jp.kamusoft.ksdialogs.Toast
import jp.kamusoft.ksdialogs.samples.kmp.SampleCaptureAutoPlay
import jp.kamusoft.ksdialogs.samples.kmp.SampleCaptureOptions
import jp.kamusoft.ksdialogs.samples.kmp.SampleDemoId
import jp.kamusoft.ksdialogs.samples.kmp.SamplePresenter
import jp.kamusoft.ksdialogs.samples.kmp.SampleText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** メニュー画面と、レイアウト属性・トランジションの調整パネルを表示する画面。デモ項目の起動は共有 Presenter に委ねる。 */
internal class MainActivity : ComponentActivity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var presenter: SamplePresenter
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 起動引数から取り出すのは各 OS 側の仕事で、検証と設定への畳み込みは共有の設定型が受け持つ
        val options = SampleCaptureOptions.from(
            demo = intent?.getStringExtra(SampleCaptureOptions.DEMO_KEY),
            loadingStepIntervalMilliseconds =
                intent?.getStringExtra(SampleCaptureOptions.LOADING_STEP_INTERVAL_KEY),
        )
        presenter = SamplePresenter(options)
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

        if (savedInstanceState == null) {
            autoPlay(SampleCaptureAutoPlay.consumeDemo(options))
        }
    }

    /**
     * 起動引数で指定されたデモを、メニュー項目のタップと同じ入口で自動再生する。
     *
     * ダイアログの提示先はメニューが画面に載ってから決まるため、再生もその時点まで待つ。
     */
    private fun autoPlay(demo: SampleDemoId?) {
        if (demo == null) {
            return
        }
        menuView.post { play(demo) }
    }

    /**
     * メニュー項目のタップハンドラと同じ入口を呼ぶ。
     *
     * 中身をその場で渡す Inline と、画面状態として開くパネルはこの画面が受け持ち、
     * 残りは共有 Presenter が受け持つ。
     */
    private fun play(demo: SampleDemoId) {
        when (demo) {
            SampleDemoId.INLINE_DIALOG -> showInlineDialog()
            SampleDemoId.TRANSITION_DIALOG -> openTransitionPanel()
            SampleDemoId.LAYOUT_DIALOG -> openLayoutPanel()
            // インライン経路を含むため、この画面が受け持つ
            SampleDemoId.CUSTOM_TOAST -> showCustomToast()
            // Presenter が受け持つ分も列挙して網羅させる (デモが増えたときに
            // ここを直し忘れると無言で何も起きないため、コンパイルエラーで気づけるようにする)
            SampleDemoId.BASIC_DIALOG,
            SampleDemoId.DECLARATIVE_DIALOG,
            SampleDemoId.MODEL_DIALOG,
            SampleDemoId.TEXT_INPUT_DIALOG,
            SampleDemoId.DEFAULT_LOADING,
            SampleDemoId.CUSTOM_LOADING,
            SampleDemoId.DEFAULT_TOAST,
            SampleDemoId.TOAST_STACK,
            SampleDemoId.TOAST_PLACEMENT,
            SampleDemoId.TOAST_OVERLAP,
            -> scope.launch {
                presenter.autoPlay(demo)?.let { menuView.showResult(it) }
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    /** Basic Dialog を表示し、結果を直近の結果として取り込む。 */
    private fun showBasicDialog() {
        scope.launch {
            menuView.showResult(presenter.showBasicDialog())
        }
    }

    /** Declarative Dialog を表示し、結果を直近の結果として取り込む。 */
    private fun showDeclarativeDialog() {
        scope.launch {
            menuView.showResult(presenter.showDeclarativeDialog())
        }
    }

    /**
     * Model Dialog を表示し、結果を直近の結果として取り込む。
     *
     * 呼び出しは共有 Presenter に閉じており、この画面は結果の文言を受け取るだけである。
     */
    private fun showModelDialog() {
        scope.launch {
            menuView.showResult(presenter.showModelDialog())
        }
    }

    /** Text Input Dialog を表示し、入力された文字列を直近の結果として取り込む。 */
    private fun showTextInputDialog() {
        scope.launch {
            menuView.showResult(presenter.showTextInputDialog())
        }
    }

    /**
     * Inline Dialog を表示し、結果を直近の結果として取り込む。
     *
     * 中身をその場で渡す表示は Android Native API にしかないため、共有 Presenter は経由しない。
     * この ViewModel 型はレジストリに登録していない (core/ADR-0013)。
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
            menuView.showResult(
                when (result) {
                    is DialogResult.Completed -> SampleText.completedResult(result.value)
                    DialogResult.Cancelled -> SampleText.CANCELLED_RESULT
                },
            )
        }
    }

    /**
     * Default Loading を実行し、完了を直近の結果として取り込む。
     *
     * 呼び出しは共有 Presenter に閉じており、この画面は結果の文言を受け取るだけである。
     */
    private fun runDefaultLoading() {
        scope.launch {
            menuView.showResult(presenter.runDefaultLoading())
        }
    }

    /**
     * Custom Loading を実行し、完了を直近の結果として取り込む。
     *
     * 呼び出しは共有 Presenter に閉じており、この画面が受け持つのは中身の View の登録だけである。
     */
    private fun runCustomLoading() {
        scope.launch {
            menuView.showResult(presenter.runCustomLoading())
        }
    }

    /**
     * Default Toast を表示する。
     *
     * 呼び出しは共有 Presenter に閉じており、この画面が受け持つのは起動操作だけである。
     */
    private fun showDefaultToast() {
        presenter.showDefaultToast()
    }

    /**
     * Custom Toast を表示する。
     *
     * 登録経路は共有 Presenter が表示し、中身をその場で渡すインライン経路は
     * Android Native API にしかないためこの画面が受け持つ。
     * 同じ配置では重なって見分けられないため、2 枚は上方向オフセットを変えて置く。
     */
    private fun showCustomToast() {
        presenter.showCustomToast()
        // この ViewModel 型はレジストリに登録していない (core/ADR-0013)
        Toast.instance.show(
            viewModel = InlineToastViewModel(SampleText.INLINE_TOAST_MESSAGE),
            durationMs = CUSTOM_TOAST_INLINE_DURATION_MS,
            placement = DialogPlacement(
                horizontalAlignment = DialogAlignment.CENTER,
                verticalAlignment = DialogAlignment.END,
                offsetY = TOAST_LOWER_OFFSET_Y,
            ),
        ) { suppliedViewModel ->
            InlineToastCardView(context = this, viewModel = suppliedViewModel)
        }
    }

    /**
     * Toast Stack を表示する。
     *
     * 呼び出しは共有 Presenter に閉じており、この画面が受け持つのは起動操作だけである。
     */
    private fun showToastStack() {
        presenter.showToastStack()
    }

    /**
     * Toast Placement を表示する。
     *
     * 呼び出しは共有 Presenter に閉じており、この画面が受け持つのは起動操作だけである。
     */
    private fun showToastPlacement() {
        presenter.showToastPlacement()
    }

    /**
     * Toast Overlap を実行し、時系列の完了を直近の結果として取り込む。
     *
     * 呼び出しは共有 Presenter に閉じており、この画面は結果の文言を受け取るだけである。
     */
    private fun runToastOverlap() {
        scope.launch {
            menuView.showResult(presenter.runToastOverlap())
        }
    }

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
            val result = presenter.showLayoutDialog(panel.placement(), panel.usesVisibleArea)
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
            val result = presenter.showTransitionDialog(
                preset = panel.preset(),
                durationMilliseconds = panel.durationMilliseconds(),
                easing = panel.easing(),
            )
            panel.showResult(result)
            menuView.showResult(result)
        }
    }

    private companion object {
        /** Toast を重ねて置くときの上方向オフセット (論理単位) の下段。契約既定と同じ高さ。 */
        const val TOAST_LOWER_OFFSET_Y = -80.0

        /** Custom Toast のインライン経路の表示時間 (ミリ秒)。登録経路より先に消える値にする。 */
        const val CUSTOM_TOAST_INLINE_DURATION_MS = 2000
    }
}
