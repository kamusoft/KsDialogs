package jp.kamusoft.ksdialogs.kmp

/**
 * Android Native ライブラリのレジストリを指すハンドル。
 *
 * View factory の登録は [native] に対して行い、共有コードからはその同一性の確認に使う。
 * ViewModel factory の表は共有コード側 (このハンドル自身) が持つ (kmp/ADR-0006)。
 */
internal class AndroidDialogViewRegistry(
    val native: jp.kamusoft.ksdialogs.DialogViewRegistry,
) : SharedDialogViewRegistry()

/**
 * Android Native ライブラリの公開 API へそのまま委譲する委譲面。
 *
 * ViewModel は包み直さずに渡すため、レジストリの解決キーは共有コードで定義したクラスそのものになる。
 */
internal class AndroidDialogGateway(
    private val native: jp.kamusoft.ksdialogs.KsDialog,
) : DialogGateway {
    override val registry: SharedDialogViewRegistry = AndroidDialogViewRegistry(native.registry)

    override suspend fun present(
        viewModel: DialogViewModel<*>,
        placement: DialogPlacement?,
    ): DialogOutcome =
        try {
            when (val result = native.show(viewModel, placement?.toNative())) {
                is jp.kamusoft.ksdialogs.DialogResult.Cancelled -> DialogOutcome.Cancelled
                is jp.kamusoft.ksdialogs.DialogResult.Completed -> DialogOutcome.Completed(result.value)
            }
        } catch (failure: jp.kamusoft.ksdialogs.DialogException) {
            // 構成エラーは共有コードの契約 (throw 系チャネル) に載せ替えて投げ直す
            throw DialogException(failure.message ?: failure.toString(), failure)
        }
}

/**
 * 共有コードの置き場所を Android Native ライブラリの同型へ写す。
 *
 * 値の正規化や配置の計算はすべて Native ライブラリの責務なので、ここでは型だけを移し替える (core/ADR-0001)。
 */
internal fun DialogPlacement.toNative(): jp.kamusoft.ksdialogs.DialogPlacement =
    jp.kamusoft.ksdialogs.DialogPlacement(
        horizontalAlignment = horizontalAlignment.toNative(),
        verticalAlignment = verticalAlignment.toNative(),
        offsetX = offsetX,
        offsetY = offsetY,
    )

/** 共有コードの配置を Android Native ライブラリの同型へ写す。 */
private fun DialogAlignment.toNative(): jp.kamusoft.ksdialogs.DialogAlignment =
    when (this) {
        DialogAlignment.START -> jp.kamusoft.ksdialogs.DialogAlignment.START
        DialogAlignment.CENTER -> jp.kamusoft.ksdialogs.DialogAlignment.CENTER
        DialogAlignment.END -> jp.kamusoft.ksdialogs.DialogAlignment.END
        DialogAlignment.FILL -> jp.kamusoft.ksdialogs.DialogAlignment.FILL
    }
