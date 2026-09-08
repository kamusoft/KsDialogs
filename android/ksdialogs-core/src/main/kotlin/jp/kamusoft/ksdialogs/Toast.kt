package jp.kamusoft.ksdialogs

import android.content.Context
import android.view.View
import kotlin.reflect.KClass

/**
 * Toast 表示の既定エントリ。
 *
 * [Toast.instance] で手軽に呼び出せるほか、`Toast()` を [KsToast] として DI 注入しても
 * 同じ状態 (レジストリ・一括設定・表示中のリスト) を共有する (core/ADR-0002)。
 *
 * この型は状態を持たず、すべての呼び出しをプロセス内で唯一の coordinator へ委譲する。
 */
public class Toast internal constructor(
    internal val coordinator: ToastCoordinator,
) : KsToast {

    /** 既定の状態・レジストリ・取り付け先解決を使うインスタンスを作る。 */
    public constructor() : this(ToastCoordinator.shared)

    override val registry: ToastViewRegistry
        get() = coordinator.registry

    override var style: ToastStyle
        get() = coordinator.settings.style
        set(value) {
            coordinator.settings.style = value
        }

    override fun show(message: String, durationMs: Int?, placement: DialogPlacement?) {
        coordinator.accept(ToastContentRequest.Builtin(message), durationMs, placement)
    }

    override fun show(viewModel: ToastViewModel, durationMs: Int?, placement: DialogPlacement?) {
        coordinator.accept(ToastContentRequest.Registered(viewModel), durationMs, placement)
    }

    override fun <VM : ToastViewModel> show(
        viewModel: VM,
        durationMs: Int?,
        placement: DialogPlacement?,
        factory: Context.(VM) -> View,
    ) {
        // 型消去した factory をそのまま状態の正へ渡す。レジストリは経由しない (core/ADR-0013)
        coordinator.accept(
            ToastContentRequest.Inline(viewModel, erasedToastViewFactory(factory)),
            durationMs,
            placement,
        )
    }

    override fun <VM : ToastViewModel> show(
        viewModelClass: KClass<VM>,
        durationMs: Int?,
        placement: DialogPlacement?,
        configure: ((VM) -> Unit)?,
    ) {
        viewModelClass.requireReferenceTypeViewModel()
        // 解決は呼び出し時点のエントリのスナップショットで行い、以後の再登録には影響されない。
        // 未登録は受理そのものの失敗として、この場で同期に返す (core/ADR-0035)
        val entry = coordinator.registry.entry(viewModelClass)
        val viewModelFactory = entry?.viewModelFactory
            ?: throw DialogException.ViewModelFactoryNotRegistered(viewModelClass.viewModelTypeName)
        val viewFactory = entry.viewFactory
            ?: throw DialogException.ViewFactoryNotRegistered(viewModelClass.viewModelTypeName)

        coordinator.accept(
            ToastContentRequest.Typed(
                prepare = {
                    // ViewModel factory はクラス参照と対で登録されるため、生成物の型は常に一致する
                    @Suppress("UNCHECKED_CAST")
                    val created = viewModelFactory.createViewModel() as VM
                    configure?.invoke(created)
                    created
                },
                factory = viewFactory,
            ),
            durationMs,
            placement,
        )
    }

    public companion object {
        /** 既定の singleton エントリ。 */
        public val instance: Toast = Toast()
    }
}
