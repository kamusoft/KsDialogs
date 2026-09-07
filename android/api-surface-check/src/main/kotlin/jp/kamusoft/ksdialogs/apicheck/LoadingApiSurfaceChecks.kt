package jp.kamusoft.ksdialogs.apicheck

import android.graphics.Color
import android.widget.TextView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.setValue
import jp.kamusoft.ksdialogs.DialogAlignment
import jp.kamusoft.ksdialogs.DialogOptions
import jp.kamusoft.ksdialogs.DialogPlacement
import jp.kamusoft.ksdialogs.KsLoading
import jp.kamusoft.ksdialogs.Loading
import jp.kamusoft.ksdialogs.LoadingProgressReceiver
import jp.kamusoft.ksdialogs.LoadingStyle
import jp.kamusoft.ksdialogs.LoadingViewModel
import jp.kamusoft.ksdialogs.LoadingViewRegistry
import jp.kamusoft.ksdialogs.compose.KsDialogAttributes
import jp.kamusoft.ksdialogs.compose.registerCompose
import jp.kamusoft.ksdialogs.compose.showCompose
import jp.kamusoft.ksdialogs.compose.startCompose
import kotlin.math.roundToInt

/** カスタム Loading の表示に使う、利用者が書くのと同じ形の ViewModel。 */
public class ConsumerLoadingViewModel(public val title: String) : LoadingViewModel

/** 進捗の受け口を実装した、利用者が書くのと同じ形の ViewModel。 */
public class ConsumerProgressLoadingViewModel : LoadingViewModel, LoadingProgressReceiver {
    /** 受け取った最新の進捗。中身の composable はこれを観測して描き直す。 */
    public var progress: Double by mutableDoubleStateOf(0.0)
        private set

    override fun onProgress(progress: Double) {
        this.progress = progress
    }
}

/**
 * Loading の公開 API 形状の正の検証。
 *
 * このファイルがコンパイルできることが検証結果であり、公開すべき型・メンバが
 * 利用者から見えなくなればビルドが失敗する。
 */
public object LoadingApiSurfaceChecks {

    /** 命令形の表示・メッセージ更新・閉鎖が書ける。 */
    public suspend fun LD_AN_01_acceptsShowSetMessageAndHide(loading: KsLoading) {
        loading.show()
        loading.show(message = "読み込み中")
        loading.show(
            message = "読み込み中",
            placement = DialogPlacement(verticalAlignment = DialogAlignment.END),
        )
        loading.setMessage("あと少し")
        loading.hide()
    }

    /** スコープ形は処理の戻り値をそのまま返し、処理は進捗の報告口を受け取る。 */
    public suspend fun LD_AN_01_acceptsScopeWithProgressAndReturnsValue(loading: KsLoading): String {
        val count: Int = loading.start(message = "集計中") { report ->
            report(0.5)
            42
        }
        return loading.start(
            message = "集計中",
            placement = DialogPlacement(horizontalAlignment = DialogAlignment.START),
        ) { report ->
            report(1.0)
            "完了 ($count)"
        }
    }

    /** スタイルはフォーマット関数を含めて一括で設定でき、器メタ属性は既存の型で設定できる。 */
    public fun LD_AN_01_acceptsStyleAndOptionsConfiguration(loading: KsLoading) {
        loading.style = LoadingStyle(
            indicatorColor = Color.WHITE,
            messageFontSize = 16.0,
            messageColor = Color.WHITE,
            defaultMessage = "読み込み中",
            progressFormat = { message, progress ->
                if (progress == null) {
                    message.orEmpty()
                } else {
                    "${message.orEmpty()} ${(progress * 100).roundToInt()}%"
                }
            },
        )
        loading.options = DialogOptions(overlayColor = Color.argb(128, 0, 0, 0))
    }

    /** 従来 View 系の factory で登録できる。 */
    public fun LD_AN_01_acceptsViewRegistration(registry: LoadingViewRegistry) {
        registry.register(ConsumerLoadingViewModel::class) { viewModel ->
            TextView(this).apply { text = viewModel.title }
        }
    }

    /** Compose のコンテンツでも登録できる。 */
    public fun LD_AN_01_acceptsComposeRegistration(registry: LoadingViewRegistry) {
        registry.registerCompose(ConsumerProgressLoadingViewModel::class) { viewModel ->
            KsDialogAttributes(
                placement = DialogPlacement(verticalAlignment = DialogAlignment.END),
            )
            // 進捗の受け口が書き換える状態を観測すれば、転送のたびに中身が描き直される
            check(viewModel.progress >= 0.0)
        }
    }

    /** 登録済みの ViewModel はインスタンスを渡して表示・スコープ形で実行できる。 */
    public suspend fun LD_AN_01_acceptsRegisteredShowAndStart(loading: KsLoading): Int {
        loading.show(ConsumerLoadingViewModel("同期中"))
        loading.show(
            ConsumerLoadingViewModel("同期中"),
            placement = DialogPlacement(verticalAlignment = DialogAlignment.START),
        )
        return loading.start(ConsumerLoadingViewModel("同期中")) { report ->
            report(0.25)
            1
        }
    }

    /** 登録せずにその場の factory で表示・スコープ形の実行ができる (従来 View 系)。 */
    public suspend fun LD_AN_01_acceptsInlineViewFactory(loading: KsLoading): Boolean {
        loading.show(ConsumerLoadingViewModel("その場")) { viewModel ->
            TextView(this).apply { text = viewModel.title }
        }
        return loading.start(
            ConsumerLoadingViewModel("その場"),
            placement = DialogPlacement(verticalAlignment = DialogAlignment.END),
            factory = { viewModel -> TextView(this).apply { text = viewModel.title } },
        ) { report ->
            report(0.75)
            true
        }
    }

    /** 登録せずにその場の Compose のコンテンツで表示・スコープ形の実行ができる。 */
    public suspend fun LD_AN_01_acceptsInlineComposeContent(loading: KsLoading): Double {
        loading.showCompose(ConsumerProgressLoadingViewModel()) { viewModel ->
            check(viewModel.progress >= 0.0)
        }
        return loading.startCompose(
            ConsumerProgressLoadingViewModel(),
            content = { viewModel -> check(viewModel.progress >= 0.0) },
        ) { report ->
            report(0.5)
            0.5
        }
    }

    /** 既定シングルトンと DI 注入のどちらも同じ契約で扱える。 */
    public fun LD_AN_01_acceptsSharedEntryAndInjectedInstance(): Pair<KsLoading, KsLoading> =
        Loading.instance to Loading()

    /** ViewModel factory はラムダでもコンストラクタ参照でも登録できる。 */
    public fun LD_YA_01_acceptsViewModelFactoryRegistration(registry: LoadingViewRegistry) {
        registry.registerViewModel(ConsumerLoadingViewModel::class) {
            ConsumerLoadingViewModel("同期中")
        }
        registry.registerViewModel(ConsumerProgressLoadingViewModel::class, ::ConsumerProgressLoadingViewModel)
    }

    /** 型を渡す表示は configure あり・なし・中断関数・置き場所つきのいずれでも書ける。 */
    public suspend fun LD_YA_01_acceptsTypedShow(loading: KsLoading) {
        loading.show(ConsumerLoadingViewModel::class)
        loading.show(ConsumerLoadingViewModel::class) { viewModel ->
            check(viewModel.title.isNotEmpty())
        }
        loading.show(ConsumerLoadingViewModel::class) { viewModel ->
            // configure は中断関数として書ける
            loading.setMessage(viewModel.title)
        }
        loading.show(
            ConsumerLoadingViewModel::class,
            placement = DialogPlacement(verticalAlignment = DialogAlignment.END),
        )
    }

    /** 型を渡すスコープ形は処理の戻り値をそのまま返す。 */
    public suspend fun LD_YA_01_acceptsTypedStart(loading: KsLoading): String {
        val count: Int = loading.start(ConsumerProgressLoadingViewModel::class) { report ->
            report(0.5)
            42
        }
        return loading.start(
            ConsumerLoadingViewModel::class,
            placement = DialogPlacement(horizontalAlignment = DialogAlignment.START),
            configure = { viewModel -> check(viewModel.title.isNotEmpty()) },
        ) { report ->
            report(1.0)
            "完了 ($count)"
        }
    }
}
