package jp.kamusoft.ksdialogs.kmp.apicheck

import jp.kamusoft.ksdialogs.kmp.DialogAlignment
import jp.kamusoft.ksdialogs.kmp.DialogPlacement
import jp.kamusoft.ksdialogs.kmp.KsLoading
import jp.kamusoft.ksdialogs.kmp.LoadingProgressReceiver
import jp.kamusoft.ksdialogs.kmp.LoadingViewModel

/** 利用者が書くのと同じ形のカスタム Loading の ViewModel。進捗の受け口を実装している。 */
public class ConsumerLoadingViewModel : LoadingViewModel, LoadingProgressReceiver {
    /** 最後に受け取った進捗。 */
    public var progress: Double = 0.0
        private set

    override fun onProgress(progress: Double) {
        this.progress = progress
    }

    /** 表示前に初期値を入れるための受け口。 */
    public fun receive(progress: Double) {
        onProgress(progress)
    }
}

/**
 * 共有コードの Loading 公開 API 形状の正の検証。
 *
 * このファイルがコンパイルできることが検証結果であり、公開すべき型・メンバが
 * 利用者から見えなくなればビルドが失敗する。
 * 公開してはならないもの (スタイルの型と設定プロパティ) の不在は負の検査が受け持つ。
 */
public object LoadingApiSurfaceChecks {

    /** メッセージも配置も省略した表示が通る。 */
    public suspend fun LD_KM_02_acceptsShowWithoutArguments(loading: KsLoading) {
        loading.show()
    }

    /** メッセージと placement は表示の引数で供給できる。 */
    public suspend fun LD_KM_02_acceptsShowWithMessageAndPlacement(loading: KsLoading) {
        loading.show(
            message = "読み込み中",
            placement = DialogPlacement(verticalAlignment = DialogAlignment.END, offsetY = 12.0),
        )
    }

    /** 表示中のメッセージ更新と、合流数によらない閉じるが通る。 */
    public suspend fun LD_KM_02_acceptsMessageUpdateAndHide(loading: KsLoading) {
        loading.setMessage("残り少し")
        loading.hide()
    }

    /** スコープ形は処理の戻り値をそのまま返し、進捗は関数型の報告口へ渡す。 */
    public suspend fun LD_KM_02_acceptsScopedStart(loading: KsLoading): Int =
        loading.start(message = "読み込み中") { report ->
            report(0.5)
            42
        }

    /** カスタム Loading は共有 VM を渡して表示でき、スコープ形にも渡せる。 */
    public suspend fun LD_KM_02_acceptsCustomLoading(loading: KsLoading): String {
        val viewModel = ConsumerLoadingViewModel()
        loading.show(viewModel, placement = DialogPlacement(offsetX = 4.0))
        return loading.start(viewModel) { report ->
            report(1.0)
            "完了"
        }
    }

    /** ViewModel factory はラムダでもコンストラクタ参照でも登録できる。 */
    public fun PB_KT_10_registersViewModelFactory(loading: KsLoading) {
        loading.registry.registerViewModel(ConsumerLoadingViewModel::class) { ConsumerLoadingViewModel() }
        loading.registry.registerViewModel(ConsumerLoadingViewModel::class, ::ConsumerLoadingViewModel)
    }

    /** 型を渡す表示は configure も placement も省略できる。 */
    public suspend fun PB_KT_10_acceptsTypedShow(loading: KsLoading) {
        loading.show(ConsumerLoadingViewModel::class)
    }

    /** 型を渡す表示には placement と suspend の configure を渡せる。 */
    public suspend fun PB_KT_10_acceptsTypedShowWithPlacementAndConfigure(loading: KsLoading) {
        loading.show(
            ConsumerLoadingViewModel::class,
            placement = DialogPlacement(verticalAlignment = DialogAlignment.END, offsetY = 12.0),
        ) { viewModel ->
            viewModel.receive(loadProgress())
        }
    }

    /** 型を渡すスコープ形は処理の戻り値をそのまま返す。 */
    public suspend fun PB_KT_10_acceptsTypedStart(loading: KsLoading): String =
        loading.start(
            ConsumerLoadingViewModel::class,
            placement = DialogPlacement(offsetX = 4.0),
            configure = { viewModel -> viewModel.receive(loadProgress()) },
        ) { report ->
            report(1.0)
            "完了"
        }

    /** configure から suspend 関数を呼べることを示すための取得処理。 */
    private suspend fun loadProgress(): Double = kotlin.run { 0.5 }
}
