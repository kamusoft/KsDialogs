package jp.kamusoft.ksdialogs.kmp

/**
 * Android Native ライブラリの公開 API へそのまま委譲する Toast の委譲面。
 *
 * View factory の紐付け・一括設定・表示中のリストと各表示の期限はすべて Native ライブラリ側が持ち、
 * この面は共有コードからの呼び出しをそこへ渡すだけである (kmp/ADR-0002)。
 * ViewModel は包み直さずに渡すため、レジストリの解決キーは共有コードで定義したクラスそのものになる。
 */
internal class AndroidToastGateway(
    private val native: jp.kamusoft.ksdialogs.KsToast,
) : ToastGateway {

    override fun show(message: String, durationMs: Int?, placement: DialogPlacement?) {
        native.show(message, durationMs, placement?.toNative())
    }

    override fun show(viewModel: ToastViewModel, durationMs: Int?, placement: DialogPlacement?) {
        // 未登録の ViewModel 型は Native 側で同期に失敗する。共有コードの契約へ載せ替えて伝える
        try {
            native.show(viewModel, durationMs, placement?.toNative())
        } catch (failure: jp.kamusoft.ksdialogs.DialogException) {
            throw DialogException(failure.message ?: failure.toString(), failure)
        }
    }
}
