package jp.kamusoft.ksdialogs.samples.kmp

import jp.kamusoft.ksdialogs.kmp.Dialog
import jp.kamusoft.ksdialogs.kmp.DialogAlignment
import jp.kamusoft.ksdialogs.kmp.DialogException
import jp.kamusoft.ksdialogs.kmp.DialogPlacement
import jp.kamusoft.ksdialogs.kmp.DialogResult
import jp.kamusoft.ksdialogs.kmp.KsDialog
import jp.kamusoft.ksdialogs.kmp.KsLoading
import jp.kamusoft.ksdialogs.kmp.KsToast
import jp.kamusoft.ksdialogs.kmp.Loading
import jp.kamusoft.ksdialogs.kmp.Toast
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/**
 * デモ項目の起動と結果の言い換えを受け持つ共有 Presenter。
 *
 * 各 OS のアプリはこの Presenter を呼ぶだけで、show の呼び出しと ViewModel factory の登録は
 * 共有コードに閉じる。View factory の登録だけが OS ごとの Native API で行われる。
 *
 * @param dialogs 表示に使うダイアログのエントリ。テストでは差し替えられる
 * @param loading 表示に使うローディングのエントリ。テストでは差し替えられる
 * @param toast 表示に使う Toast のエントリ。テストでは差し替えられる
 * @param options 撮影のために起動時へ渡された設定
 */
class SamplePresenter(
    private val dialogs: KsDialog,
    private val loading: KsLoading,
    private val toast: KsToast,
    options: SampleCaptureOptions = SampleCaptureOptions(),
) {

    /** 既定のエントリを使う Presenter を作る。 */
    constructor() : this(Dialog.instance, Loading.instance, Toast.instance)

    /**
     * 既定のエントリを使い、撮影支援設定を反映した Presenter を作る。
     *
     * @param options 撮影のために起動時へ渡された設定
     */
    constructor(options: SampleCaptureOptions) :
        this(Dialog.instance, Loading.instance, Toast.instance, options)

    /** 刻みごとの待ち時間 (ミリ秒)。起動引数の指定があればその値になる。 */
    private val loadingStepIntervalMilliseconds =
        options.loadingStepIntervalMilliseconds ?: DEFAULT_LOADING_STEP_INTERVAL_MILLISECONDS

    init {
        // 型を渡す表示が実体を作るための ViewModel factory。中身の View の型は OS ごとに違うため、
        // View factory の登録だけが各 OS 側に残る (kmp/ADR-0006)。同じ型への再登録は後勝ちなので、
        // Presenter を複数作っても結果は変わらない
        dialogs.registry.registerViewModel(ModelDialogViewModel::class, ::ModelDialogViewModel)
        loading.registry.registerViewModel(CustomLoadingViewModel::class, ::CustomLoadingViewModel)
        toast.registry.registerViewModel(CustomToastViewModel::class, ::CustomToastViewModel)
    }

    /**
     * 指定されたデモを、メニュー項目のタップハンドラと同じ入口で再生する。
     *
     * 中身をその場で渡す表示 (Inline) と、画面状態として開くパネル (Layout / Transition) は
     * この Presenter の外にあるため、それらは OS の UI 層が受け持つ。
     *
     * @param demo 再生するデモ
     * @return 結果表示エリアに出す文言。OS の UI 層が受け持つデモなら null
     */
    @Throws(DialogException::class, CancellationException::class)
    suspend fun autoPlay(demo: SampleDemoId): String? = when (demo) {
        SampleDemoId.BASIC_DIALOG -> showBasicDialog()
        SampleDemoId.DECLARATIVE_DIALOG -> showDeclarativeDialog()
        SampleDemoId.MODEL_DIALOG -> showModelDialog()
        SampleDemoId.TEXT_INPUT_DIALOG -> showTextInputDialog()
        SampleDemoId.DEFAULT_LOADING -> runDefaultLoading()
        SampleDemoId.CUSTOM_LOADING -> runCustomLoading()
        SampleDemoId.DEFAULT_TOAST -> {
            showDefaultToast()
            null
        }
        SampleDemoId.TOAST_STACK -> {
            showToastStack()
            null
        }
        SampleDemoId.TOAST_PLACEMENT -> {
            showToastPlacement()
            null
        }
        SampleDemoId.TOAST_OVERLAP -> runToastOverlap()
        // インライン経路を含む Custom Toast は OS の UI 層が受け持つ
        SampleDemoId.CUSTOM_TOAST,
        SampleDemoId.INLINE_DIALOG,
        SampleDemoId.TRANSITION_DIALOG,
        SampleDemoId.LAYOUT_DIALOG,
        -> null
    }

    /**
     * Basic Dialog を表示し、結果を表示用の文言にして返す。
     *
     * @return 結果表示エリアに出す文言
     */
    @Throws(DialogException::class, CancellationException::class)
    suspend fun showBasicDialog(): String {
        val viewModel = BasicDialogViewModel(SampleText.BASIC_DIALOG_MESSAGE)
        return when (val result = dialogs.show(viewModel)) {
            is DialogResult.Completed -> SampleText.completedResult(result.value)
            DialogResult.Cancelled -> SampleText.CANCELLED_RESULT
        }
    }

    /**
     * Declarative Dialog を表示し、結果を表示用の文言にして返す。
     *
     * 中身が宣言的 UI で書かれていても、show の呼び方も結果の返り方も Basic Dialog と変わらない。
     *
     * @return 結果表示エリアに出す文言
     */
    @Throws(DialogException::class, CancellationException::class)
    suspend fun showDeclarativeDialog(): String {
        val viewModel = DeclarativeDialogViewModel(SampleText.DECLARATIVE_DIALOG_MESSAGE)
        return when (val result = dialogs.show(viewModel)) {
            is DialogResult.Completed -> SampleText.completedResult(result.value)
            DialogResult.Cancelled -> SampleText.CANCELLED_RESULT
        }
    }

    /**
     * Model Dialog を表示し、結果を表示用の文言にして返す。
     *
     * ViewModel のインスタンスは渡さず、クラス参照と configure だけを渡す。実体はレジストリの
     * ViewModel factory が作る。中身側の結果の報告経路も他のデモと違い、報告口を factory の引数で
     * 受け取らず、表示中の ViewModel から引く。
     *
     * @return 結果表示エリアに出す文言
     */
    @Throws(DialogException::class, CancellationException::class)
    suspend fun showModelDialog(): String {
        val result = dialogs.show(ModelDialogViewModel::class) { viewModel ->
            viewModel.message = SampleText.MODEL_DIALOG_MESSAGE
        }
        return when (result) {
            is DialogResult.Completed -> SampleText.completedResult(result.value)
            DialogResult.Cancelled -> SampleText.CANCELLED_RESULT
        }
    }

    /**
     * Text Input Dialog を表示し、入力された文字列を表示用の文言にして返す。
     *
     * @return 結果表示エリアに出す文言
     */
    @Throws(DialogException::class, CancellationException::class)
    suspend fun showTextInputDialog(): String {
        val viewModel = TextInputDialogViewModel(SampleText.TEXT_INPUT_DIALOG_MESSAGE)
        return when (val result = dialogs.show(viewModel)) {
            is DialogResult.Completed -> SampleText.completedResult(result.value)
            DialogResult.Cancelled -> SampleText.CANCELLED_RESULT
        }
    }

    /**
     * 選んだ演出で Transition Dialog を表示し、結果を表示用の文言にして返す。
     *
     * 演出は show の引数では渡せないため、選択を ViewModel に載せて View 定義側へ運ぶ。
     *
     * @param preset 選ばれた演出
     * @param durationMilliseconds 片道の時間 (ミリ秒)
     * @param easing 時間に対する進み方
     * @return 結果表示エリアに出す文言
     */
    @Throws(DialogException::class, CancellationException::class)
    suspend fun showTransitionDialog(
        preset: SampleTransitionPreset,
        durationMilliseconds: Int,
        easing: SampleEasingPreset,
    ): String {
        val viewModel = TransitionDialogViewModel(
            message = SampleText.TRANSITION_DIALOG_MESSAGE,
            preset = preset,
            durationMilliseconds = durationMilliseconds,
            easing = easing,
        )
        return when (val result = dialogs.show(viewModel)) {
            is DialogResult.Completed -> SampleText.completedResult(result.value)
            DialogResult.Cancelled -> SampleText.CANCELLED_RESULT
        }
    }

    /**
     * 調整した属性で Layout Dialog を表示し、結果を表示用の文言にして返す。
     *
     * @param placement この呼び出しでの置き場所
     * @param usesVisibleArea サイズと位置の計算に可視領域を使うか
     * @return 結果表示エリアに出す文言
     */
    @Throws(DialogException::class, CancellationException::class)
    suspend fun showLayoutDialog(placement: DialogPlacement, usesVisibleArea: Boolean): String {
        val viewModel = LayoutDialogViewModel(SampleText.LAYOUT_DIALOG_MESSAGE, usesVisibleArea)
        return when (val result = dialogs.show(viewModel, placement)) {
            is DialogResult.Completed -> SampleText.completedResult(result.value)
            DialogResult.Cancelled -> SampleText.CANCELLED_RESULT
        }
    }

    /**
     * Default Loading を実行し、完了を表示用の文言にして返す。
     *
     * スコープ形の start は処理の間だけ既定ローディングを出し、処理の完了で自動的に閉じる。
     * 処理は 0 から 1 まで進捗を段階的に報告し、途中で表示中のメッセージを差し替える。
     *
     * @return 結果表示エリアに出す文言
     */
    @Throws(DialogException::class, CancellationException::class)
    suspend fun runDefaultLoading(): String {
        loading.start(message = SampleText.LOADING_START_MESSAGE) { report ->
            for (step in 0..LOADING_STEP_COUNT) {
                report(step.toDouble() / LOADING_STEP_COUNT)
                if (step == LOADING_MESSAGE_UPDATE_STEP) {
                    loading.setMessage(SampleText.LOADING_UPDATE_MESSAGE)
                }
                delay(loadingStepIntervalMilliseconds)
            }
        }
        return SampleText.LOADING_COMPLETED_RESULT
    }

    /**
     * Custom Loading を実行し、完了を表示用の文言にして返す。
     *
     * 呼び出しの形は Default Loading と同じスコープ形で、渡すのが登録済みの ViewModel の**型**である
     * 点だけが違う。実体はレジストリの ViewModel factory が作る。
     * 中身の View の登録は各 OS 側で行い、報告した進捗は ViewModel の受け口へ転送される。
     *
     * @return 結果表示エリアに出す文言
     */
    @Throws(DialogException::class, CancellationException::class)
    suspend fun runCustomLoading(): String {
        loading.start(CustomLoadingViewModel::class) { report ->
            for (step in 0..LOADING_STEP_COUNT) {
                report(step.toDouble() / LOADING_STEP_COUNT)
                delay(loadingStepIntervalMilliseconds)
            }
        }
        return SampleText.LOADING_COMPLETED_RESULT
    }

    /**
     * Default Toast を表示する。
     *
     * duration も配置も渡さないので、デフォルト View が契約既定の配置に出て既定 duration で消える。
     * Toast は fire-and-forget なので戻り値も待機もなく、結果表示も変えない (core/ADR-0031)。
     */
    fun showDefaultToast() {
        toast.show(SampleText.DEFAULT_TOAST_MESSAGE)
    }

    /**
     * Custom Toast の登録経路を表示する。
     *
     * 中身をその場で渡すインライン経路は共有コードの契約に無いため、そちらは OS の UI 層が受け持つ。
     * インライン経路と重ならないよう、こちらは上方向オフセットを足した位置に置く。
     *
     * ViewModel のインスタンスは渡さず、クラス参照と configure だけを渡す。実体はレジストリの
     * ViewModel factory が作る。
     *
     * ViewModel factory が未登録の型は構成ミスとして [DialogException] になる。
     * Swift から呼ぶ場合、この例外は NSError として届く。
     */
    @Throws(DialogException::class)
    fun showCustomToast() {
        toast.show(
            viewModelClass = CustomToastViewModel::class,
            durationMs = CUSTOM_TOAST_REGISTERED_DURATION_MS,
            placement = bottomToastPlacement(TOAST_MIDDLE_OFFSET_Y),
            configure = { viewModel -> viewModel.message = SampleText.CUSTOM_TOAST_MESSAGE },
        )
    }

    /**
     * Toast Stack を表示する。
     *
     * 3 枚を続けて出し、duration の短いものから独立して消える様子を見せる。
     * 3 枚目は長文で、デフォルト View が複数行に折り返して高さを伸ばすことを確かめる。
     */
    fun showToastStack() {
        toast.show(
            message = SampleText.TOAST_STACK_FIRST_MESSAGE,
            durationMs = TOAST_STACK_FIRST_DURATION_MS,
            placement = bottomToastPlacement(TOAST_LOWER_OFFSET_Y),
        )
        toast.show(
            message = SampleText.TOAST_STACK_SECOND_MESSAGE,
            durationMs = TOAST_STACK_SECOND_DURATION_MS,
            placement = bottomToastPlacement(TOAST_MIDDLE_OFFSET_Y),
        )
        toast.show(
            message = SampleText.TOAST_STACK_THIRD_MESSAGE,
            durationMs = TOAST_STACK_THIRD_DURATION_MS,
            placement = bottomToastPlacement(TOAST_UPPER_OFFSET_Y),
        )
    }

    /** Toast Placement を表示する。show の引数で契約既定と違う配置へ上書きする。 */
    fun showToastPlacement() {
        toast.show(
            message = SampleText.TOAST_PLACEMENT_MESSAGE,
            durationMs = TOAST_PLACEMENT_DURATION_MS,
            placement = TOP_TOAST_PLACEMENT,
        )
    }

    /**
     * Toast Overlap を実行し、時系列の完了を表示用の文言にして返す。
     *
     * 操作者に依存しない固定の時系列で自動進行する — Toast を出したまま Dialog を重ね、
     * Dialog を閉じたあと Loading を重ねる。Loading は Toast より前面に出る (core/ADR-0030)。
     * Loading が終わっても Toast は残っており、duration の満了で消えてから結果を返す。
     *
     * @return 結果表示エリアに出す文言
     */
    @Throws(DialogException::class, CancellationException::class)
    suspend fun runToastOverlap(): String {
        toast.show(
            message = SampleText.TOAST_OVERLAP_MESSAGE,
            durationMs = TOAST_OVERLAP_TOAST_DURATION_MS,
        )

        // 待機を打ち切るとダイアログは cancelled で確定して閉じる。結果は使わない
        val dialogViewModel = BasicDialogViewModel(SampleText.BASIC_DIALOG_MESSAGE)
        withTimeoutOrNull(TOAST_OVERLAP_DIALOG_DURATION_MS) { dialogs.show(dialogViewModel) }

        loading.start(message = SampleText.LOADING_START_MESSAGE) {
            delay(TOAST_OVERLAP_LOADING_DURATION_MS)
        }

        // Toast の残り時間 (と満了を跨ぐ余白) を待ってから結果を返す
        delay(
            TOAST_OVERLAP_TOAST_DURATION_MS - TOAST_OVERLAP_DIALOG_DURATION_MS -
                TOAST_OVERLAP_LOADING_DURATION_MS + TOAST_OVERLAP_RESULT_MARGIN_MS,
        )
        return SampleText.TOAST_OVERLAP_COMPLETED_RESULT
    }

    /** 可視領域の下部中央から上方向へ動かした配置。 */
    private fun bottomToastPlacement(offsetY: Double): DialogPlacement = DialogPlacement(
        horizontalAlignment = DialogAlignment.CENTER,
        verticalAlignment = DialogAlignment.END,
        offsetY = offsetY,
    )

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
    }
}
