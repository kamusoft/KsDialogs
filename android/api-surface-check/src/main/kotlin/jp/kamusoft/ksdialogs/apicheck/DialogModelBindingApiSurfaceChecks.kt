package jp.kamusoft.ksdialogs.apicheck

import android.content.Context
import android.view.View
import androidx.compose.runtime.Composable
import jp.kamusoft.ksdialogs.DialogNotifier
import jp.kamusoft.ksdialogs.DialogPlacement
import jp.kamusoft.ksdialogs.DialogResult
import jp.kamusoft.ksdialogs.DialogViewRegistry
import jp.kamusoft.ksdialogs.KsDialog
import jp.kamusoft.ksdialogs.SimpleDialogViewModel
import jp.kamusoft.ksdialogs.compose.registerCompose
import jp.kamusoft.ksdialogs.notifier
import kotlinx.coroutines.delay

/**
 * ViewModel 主導の呼び出し面で利用者が書く形の ViewModel。
 * 状態は表示の直前に configure から設定される。
 */
public class ConsumerModelBindingViewModel : SimpleDialogViewModel {
    public var message: String = ""

    /** 中身を経由せず ViewModel 自身が結果を報告する形。 */
    public fun confirm() {
        notifier?.complete(true)
    }
}

/** コンストラクタ参照だけで登録できる形の中身の View。 */
public class ConsumerModelBindingView(
    context: Context,
    public val viewModel: ConsumerModelBindingViewModel,
) : View(context)

/**
 * ViewModel 主導の呼び出し面 (VM 引数のみの登録・ViewModel factory 登録・`vm.notifier`・型指定 show) の
 * 正の検証。
 *
 * このファイルがコンパイルできることが検証結果であり、型引数を明示しなくても
 * 利用者が意図どおりに書けることを示す。型が意図と違えば代入先の型注釈で失敗する。
 */
public object DialogModelBindingApiSurfaceChecks {

    /** 従来 View 系の中身を ViewModel 引数だけの factory で登録でき、`vm.notifier` が宣言結果型に型付く。 */
    public fun MB_AN_01_acceptsViewModelOnlyViewFactory(registry: DialogViewRegistry) {
        registry.register(ConsumerModelBindingViewModel::class) { viewModel ->
            val boundNotifier: DialogNotifier<Boolean>? = viewModel.notifier
            boundNotifier?.complete(true)
            View(this)
        }
    }

    /** 中身の View のコンストラクタが `(Context, VM)` なら、コンストラクタ参照がそのまま factory になる。 */
    public fun MB_AN_01_acceptsConstructorReferenceRegistration(registry: DialogViewRegistry) {
        registry.register(ConsumerModelBindingViewModel::class, ::ConsumerModelBindingView)
    }

    /** 宣言的 UI の中身も ViewModel 引数だけの factory で登録できる。 */
    public fun MB_AN_01_acceptsViewModelOnlyComposeContent(registry: DialogViewRegistry) {
        registry.registerCompose(ConsumerModelBindingViewModel::class) { viewModel ->
            ConsumerModelBindingContent(viewModel)
        }
    }

    /** ViewModel factory を登録できる。View factory の登録とはスロットが別で共存する。 */
    public fun MB_AN_01_acceptsViewModelFactoryRegistration(registry: DialogViewRegistry) {
        registry.registerViewModel(ConsumerModelBindingViewModel::class) {
            ConsumerModelBindingViewModel()
        }
    }

    /** configure なしの型指定 show は宣言結果型の結果を返す。 */
    public suspend fun MB_AN_01_acceptsTypedShowWithoutConfigure(dialogs: KsDialog): DialogResult<Boolean> =
        dialogs.show(ConsumerModelBindingViewModel::class)

    /** configure つきの型指定 show を書ける。 */
    public suspend fun MB_AN_01_acceptsTypedShowWithConfigure(dialogs: KsDialog): DialogResult<Boolean> =
        dialogs.show(ConsumerModelBindingViewModel::class) { viewModel ->
            viewModel.message = "確認"
        }

    /** configure は中断関数としても書ける。 */
    public suspend fun MB_AN_01_acceptsTypedShowWithSuspendingConfigure(
        dialogs: KsDialog,
    ): DialogResult<Boolean> =
        dialogs.show(ConsumerModelBindingViewModel::class) { viewModel ->
            delay(1)
            viewModel.message = "非同期で用意"
        }

    /** 型指定 show でも配置を渡せる。 */
    public suspend fun MB_AN_01_acceptsTypedShowWithPlacement(dialogs: KsDialog): DialogResult<Boolean> =
        dialogs.show(ConsumerModelBindingViewModel::class, placement = DialogPlacement())
}

/** 宣言的 UI の中身。ViewModel から引いた報告口で結果を報告する。 */
@Composable
private fun ConsumerModelBindingContent(viewModel: ConsumerModelBindingViewModel) {
    val boundNotifier: DialogNotifier<Boolean>? = viewModel.notifier
    boundNotifier?.complete(true)
}
