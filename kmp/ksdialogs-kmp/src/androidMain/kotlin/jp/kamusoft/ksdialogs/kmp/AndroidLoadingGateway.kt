package jp.kamusoft.ksdialogs.kmp

/**
 * Android Native ライブラリの公開 API へそのまま委譲する委譲面。
 *
 * 合流カウント・表示世代・最新のメッセージと進捗はすべて Native ライブラリ側が持ち、
 * この面は共有コードからの呼び出しをそこへ渡すだけである (core/ADR-0024)。
 * ViewModel は包み直さずに渡すため、レジストリの解決キーは共有コードで定義したクラスそのものになる。
 */
internal class AndroidLoadingGateway(
    private val native: jp.kamusoft.ksdialogs.KsLoading,
) : LoadingGateway {

    override suspend fun show(message: String?, placement: DialogPlacement?) {
        native.show(message, placement?.toNative())
    }

    override suspend fun show(viewModel: LoadingViewModel, placement: DialogPlacement?) {
        delegating { native.show(viewModel, placement?.toNative()) }
    }

    override suspend fun hide() {
        native.hide()
    }

    override suspend fun setMessage(message: String?) {
        native.setMessage(message)
    }

    override suspend fun <T> start(
        message: String?,
        placement: DialogPlacement?,
        action: suspend ((Double) -> Unit) -> T,
    ): T = native.start(message, placement?.toNative(), action)

    override suspend fun <T> start(
        viewModel: LoadingViewModel,
        placement: DialogPlacement?,
        action: suspend ((Double) -> Unit) -> T,
    ): T = delegating { native.start(viewModel, placement?.toNative(), action) }

    /**
     * Native ライブラリの構成エラーを共有コードの契約 (throw 系チャネル) に載せ替える。
     *
     * 未登録の ViewModel 型など、カスタム Loading の解決で失敗しうる呼び出しだけがこれを通る。
     */
    private inline fun <T> delegating(block: () -> T): T =
        try {
            block()
        } catch (failure: jp.kamusoft.ksdialogs.DialogException) {
            throw DialogException(failure.message ?: failure.toString(), failure)
        }
}
