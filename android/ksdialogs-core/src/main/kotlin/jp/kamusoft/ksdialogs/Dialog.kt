package jp.kamusoft.ksdialogs

import android.content.Context
import android.view.View
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.reflect.KClass

/**
 * ダイアログ表示の既定エントリ。
 *
 * [Dialog.instance] で手軽に呼び出せるほか、`Dialog()` を [KsDialog] として DI 注入しても
 * 同じレジストリ ([DialogViewRegistry.shared]) を共有する (core/ADR-0002・0004)。
 */
public class Dialog internal constructor(
    override val registry: DialogViewRegistry,
    private val presentationSurface: DialogPresentationSurface,
) : KsDialog {

    /** 既定のレジストリと提示先解決を使うインスタンスを作る。 */
    public constructor() : this(DialogViewRegistry.shared, ActivityDialogPresentationSurface())

    override suspend fun <R> show(
        viewModel: DialogViewModel<R>,
        placement: DialogPlacement?,
    ): DialogResult<R> =
        DialogPresenter.present(viewModel, registry, presentationSurface, placement).toResult()

    override suspend fun <R, VM : DialogViewModel<R>> show(
        viewModel: VM,
        placement: DialogPlacement?,
        factory: Context.(VM, DialogNotifier<R>) -> View,
    ): DialogResult<R> =
        DialogPresenter.present(
            viewModel,
            erasedDialogViewFactory(factory),
            presentationSurface,
            placement,
        ).toResult()

    override suspend fun <R, VM : DialogViewModel<R>> show(
        viewModelClass: KClass<VM>,
        placement: DialogPlacement?,
        configure: (suspend (VM) -> Unit)?,
    ): DialogResult<R> {
        viewModelClass.requireReferenceTypeViewModel()
        // 解決は呼び出し時点のエントリのスナップショットで行い、以後の再登録には影響されない
        val entry = registry.entry(viewModelClass)
        val viewModelFactory = entry?.viewModelFactory
            ?: throw DialogException.ViewModelFactoryNotRegistered(viewModelClass.viewModelTypeName)
        val viewFactory = entry.viewFactory
            ?: throw DialogException.ViewFactoryNotRegistered(viewModelClass.viewModelTypeName)

        // 生成と configure は View factory と同じ UI スレッドで行い、
        // configure の完了までは中身の生成へ進まない (core/ADR-0019)
        val viewModel = withContext(Dispatchers.Main.immediate) {
            // ViewModel factory はクラス参照と対で登録されるため、生成物の型は常に一致する
            @Suppress("UNCHECKED_CAST")
            val created = viewModelFactory.createViewModel() as VM
            configure?.invoke(created)
            created
        }
        return DialogPresenter.present(viewModel, viewFactory, presentationSurface, placement).toResult()
    }

    /** 型消去された結果を、ViewModel の宣言結果型で受け取れる形へ戻す。 */
    private fun <R> DialogOutcome.toResult(): DialogResult<R> = when (this) {
        is DialogOutcome.Cancelled -> DialogResult.Cancelled
        is DialogOutcome.Completed -> {
            // 結果値は ViewModel の宣言結果型に固定された DialogNotifier からしか入らないため、
            // ここでの型は常に一致する
            @Suppress("UNCHECKED_CAST")
            DialogResult.Completed(value as R)
        }
    }

    public companion object {
        /** 既定の singleton エントリ。 */
        public val instance: Dialog = Dialog()
    }
}
