import android.widget.TextView
import jp.kamusoft.ksdialogs.Dialog

/**
 * 共有コードの ViewModel 型と Android の View factory の紐付け。
 *
 * View の型は OS ごとに異なるため、登録は Android Native API に対して行う。
 * 共有コードで定義した ViewModel のクラスがそのまま登録キーになる。
 * 最小例と同じ既定のパッケージに置き、共有モジュールの型をそのまま参照する。
 */
internal object KmpDialogRegistration {
    /** 最小例の確認ダイアログを登録する。 */
    fun register() {
        Dialog.instance.registry.register(ConfirmViewModel::class) { viewModel, notifier ->
            TextView(this).apply {
                text = viewModel.message
                setOnClickListener { notifier.complete(true) }
            }
        }
    }
}
