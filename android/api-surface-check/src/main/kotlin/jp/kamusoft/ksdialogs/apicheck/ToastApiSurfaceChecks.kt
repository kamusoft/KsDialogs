package jp.kamusoft.ksdialogs.apicheck

import android.graphics.Color
import android.widget.TextView
import jp.kamusoft.ksdialogs.DialogAlignment
import jp.kamusoft.ksdialogs.DialogPlacement
import jp.kamusoft.ksdialogs.KsToast
import jp.kamusoft.ksdialogs.Toast
import jp.kamusoft.ksdialogs.ToastStyle
import jp.kamusoft.ksdialogs.ToastViewModel
import jp.kamusoft.ksdialogs.ToastViewRegistry
import jp.kamusoft.ksdialogs.compose.registerCompose
import jp.kamusoft.ksdialogs.compose.showCompose

/** カスタム Toast の表示に使う、利用者が書くのと同じ形の ViewModel。 */
public class ConsumerToastViewModel(public val title: String) : ToastViewModel

/** 型を渡す表示で使う、引数なしで作れて configure で状態を整えられる ViewModel。 */
public class ConsumerDefaultToastViewModel : ToastViewModel {
    /** 中身の組み立てが読む表題。 */
    public var title: String = "同期しました"
}

/**
 * Toast の公開 API 形状の正の検証。
 *
 * このファイルがコンパイルできることが検証結果であり、公開すべき型・メンバが
 * 利用者から見えなくなればビルドが失敗する。
 */
public object ToastApiSurfaceChecks {

    /** メッセージの表示は各省略形で書ける。 */
    public fun TS_AN_01_acceptsMessageShowInEveryShorthand(toast: KsToast) {
        toast.show("保存しました")
        toast.show(message = "保存しました")
        toast.show(message = "保存しました", durationMs = 3000)
        toast.show(
            message = "保存しました",
            durationMs = 3000,
            placement = DialogPlacement(verticalAlignment = DialogAlignment.START),
        )
        toast.show(
            message = "保存しました",
            placement = DialogPlacement(horizontalAlignment = DialogAlignment.END),
        )
    }

    /** 従来 View 系の factory で登録できる。 */
    public fun TS_AN_01_acceptsViewRegistration(registry: ToastViewRegistry) {
        registry.register(ConsumerToastViewModel::class) { viewModel ->
            TextView(this).apply { text = viewModel.title }
        }
    }

    /** Compose のコンテンツでも登録できる (配布物は ksdialogs-compose 側)。 */
    public fun TS_AN_01_acceptsComposeRegistration(registry: ToastViewRegistry) {
        registry.registerCompose(ConsumerToastViewModel::class) { viewModel ->
            check(viewModel.title.isNotEmpty())
        }
    }

    /** 登録済みの ViewModel はインスタンスを渡して表示できる。 */
    public fun TS_AN_01_acceptsRegisteredShow(toast: KsToast) {
        toast.show(ConsumerToastViewModel("同期しました"))
        toast.show(ConsumerToastViewModel("同期しました"), durationMs = 2000)
        toast.show(
            ConsumerToastViewModel("同期しました"),
            durationMs = 2000,
            placement = DialogPlacement(verticalAlignment = DialogAlignment.CENTER),
        )
    }

    /** 登録せずにその場の factory で表示できる (従来 View 系)。 */
    public fun TS_AN_01_acceptsInlineViewFactory(toast: KsToast) {
        toast.show(ConsumerToastViewModel("その場")) { viewModel ->
            TextView(this).apply { text = viewModel.title }
        }
        toast.show(
            ConsumerToastViewModel("その場"),
            durationMs = 1000,
            placement = DialogPlacement(verticalAlignment = DialogAlignment.END),
            factory = { viewModel -> TextView(this).apply { text = viewModel.title } },
        )
    }

    /** 登録せずにその場の Compose のコンテンツで表示できる。 */
    public fun TS_AN_01_acceptsInlineComposeContent(toast: KsToast) {
        toast.showCompose(ConsumerToastViewModel("その場")) { viewModel ->
            check(viewModel.title.isNotEmpty())
        }
        toast.showCompose(
            ConsumerToastViewModel("その場"),
            durationMs = 1000,
            placement = DialogPlacement(verticalAlignment = DialogAlignment.END),
            content = { viewModel -> check(viewModel.title.isNotEmpty()) },
        )
    }

    /** スタイルは既定 duration とアプリ既定配置を含めて一括で設定できる。 */
    public fun TS_AN_01_acceptsStyleConfiguration(toast: KsToast) {
        toast.style = ToastStyle(
            backgroundColor = Color.DKGRAY,
            textColor = Color.WHITE,
            fontSize = 16.0,
            cornerRadius = 22.0,
            defaultDuration = 2500,
            defaultPlacement = DialogPlacement(verticalAlignment = DialogAlignment.END),
        )
    }

    /** 既定シングルトンと DI 注入のどちらも同じ契約で扱える。 */
    public fun TS_AN_01_acceptsSharedEntryAndInjectedInstance(): Pair<KsToast, KsToast> =
        Toast.instance to Toast()

    /** ViewModel factory はラムダでもコンストラクタ参照でも登録できる。 */
    public fun TS_YA_01_acceptsViewModelFactoryRegistration(registry: ToastViewRegistry) {
        registry.registerViewModel(ConsumerToastViewModel::class) {
            ConsumerToastViewModel("同期しました")
        }
        registry.registerViewModel(ConsumerDefaultToastViewModel::class, ::ConsumerDefaultToastViewModel)
    }

    /** 型を渡す表示は configure あり・なし・duration つき・置き場所つきのいずれでも書ける。 */
    public fun TS_YA_01_acceptsTypedShow(toast: KsToast) {
        toast.show(ConsumerDefaultToastViewModel::class)
        toast.show(ConsumerDefaultToastViewModel::class) { viewModel ->
            viewModel.title = "configure で設定"
        }
        toast.show(ConsumerDefaultToastViewModel::class, durationMs = 2000)
        toast.show(
            ConsumerDefaultToastViewModel::class,
            durationMs = 2000,
            placement = DialogPlacement(verticalAlignment = DialogAlignment.START),
            configure = { viewModel -> viewModel.title = "configure で設定" },
        )
    }

    /** レジストリは契約から取り出せる。 */
    public fun TS_AN_01_exposesRegistryOnContract(toast: KsToast): ToastViewRegistry = toast.registry
}
