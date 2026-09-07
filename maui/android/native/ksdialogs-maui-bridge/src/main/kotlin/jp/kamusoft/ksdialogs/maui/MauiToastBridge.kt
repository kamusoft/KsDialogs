package jp.kamusoft.ksdialogs.maui

import android.view.View
import jp.kamusoft.ksdialogs.KsToast
import jp.kamusoft.ksdialogs.Toast
import jp.kamusoft.ksdialogs.ToastViewModel

/**
 * MAUI 形態の Toast のための互換面 (maui/ADR-0001)。
 *
 * 表示中のリスト・重なり順・各表示の計時はすべて Native ライブラリの coordinator が持ち、
 * この面は MAUI 側の呼び出しをそこへ渡すだけである (core/ADR-0030)。
 * ViewModel 型から View を解決するレジストリは MAUI 側 (C# 層) が持つため、この面は解決に関与せず、
 * カスタム Toast の中身は表示のたびに MAUI 側から供給される。
 */
public class MauiToastBridge private constructor(private val toast: KsToast) {

    init {
        // 中身は表示のたびに MAUI 側から供給されるため、この面が使う紐付けは1種類だけで足りる
        toast.registry.register(MauiToastViewModel::class) { viewModel ->
            viewModel.createContentView()
        }
    }

    /**
     * Toast の一括設定を反映する。器は各表示の受理時にこの値を読む (core/ADR-0032)。
     *
     * @param style MAUI 側で設定された一括設定
     */
    public fun applyStyle(style: MauiToastStyle) {
        toast.style = style.toToastStyle()
    }

    /**
     * 表示 1 枚を受理する。
     *
     * fire-and-forget なので結末の通知は無く、受理そのものも失敗しない (core/ADR-0031)。
     * この面が使う ViewModel 型は常に登録済みなので、登録経路の解決も失敗しない。
     *
     * @param content 表示する中身の指定
     */
    public fun show(content: MauiToastContent) {
        val placement = content.placement?.toDialogPlacement()
        val provider = content.provider
        if (provider == null) {
            toast.show(content.message.orEmpty(), content.durationMs, placement)
        } else {
            toast.show(MauiToastViewModel(provider), content.durationMs, placement)
        }
    }

    public companion object {
        /** 既定の共有インスタンス。 */
        @JvmStatic
        public val shared: MauiToastBridge = MauiToastBridge(Toast())
    }
}

/**
 * MAUI 側から供給される View をそのまま中身にするカスタム Toast の ViewModel。
 *
 * この面の利用者 (MAUI facade) は ViewModel 型を意識せず、常にこの 1 種類が使われる。
 *
 * ViewModel はメタ属性を持たない。MAUI 側で指定された属性は、中身の View への添付として
 * 器へ渡る (core/ADR-0015)。
 */
internal class MauiToastViewModel(
    private val contentProvider: MauiToastContentProvider,
) : ToastViewModel {

    /**
     * 中身の View を新規生成し、MAUI 側で指定されたメタ属性を添付して返す。
     * 提示先が確保できた後に UI スレッドで呼ばれる。
     *
     * MAUI 側が中身を作れなかったときは失敗を投げる。器はそれを受理後の失敗として扱い、
     * 警告を残してこの 1 枚だけを破棄する (他の表示には影響しない)。
     */
    fun createContentView(): View {
        val content = contentProvider.createContent()
            ?: error("The MAUI side could not create the presentation content.")
        MauiDialogContent.applyAttributes(content.view, content.options, content.placement)
        return content.view
    }
}
