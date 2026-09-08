package jp.kamusoft.ksdialogs.maui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * 互換面が外へ出す診断文言が英語で固定されていることの検証 (cross/ADR-0015)。
 *
 * 読み手はライブラリを組み込む開発者であり、失敗の説明は英語で書く。
 * 文言そのものは互換契約ではない (契約は例外型と起きる条件) が、ここでは置き換えが
 * 意図どおりであることを完全一致で固定する。
 */
@DisplayName("互換面の診断文言は英語で固定される")
class MauiBridgeDiagnosticMessageTests {

    @Test
    @DisplayName("MAUI 側が中身を作れないと英語文言の失敗になる")
    fun `DM-MA-04 MAUI 側が中身を作れないと英語文言の失敗になる`() {
        val viewModel = MauiToastViewModel(MauiToastContentProvider { null })

        val failure = assertThrows<IllegalStateException> { viewModel.createContentView() }

        assertEquals(
            "The MAUI side could not create the presentation content.",
            failure.message,
        )
    }

    @Test
    @DisplayName("Dialog / Loading でも中身なしは同じ英語文言の失敗になる")
    fun `DM-MA-04 Dialog と Loading でも中身なしは同じ英語文言の失敗になる`() {
        val dialog = MauiDialogViewModel(
            MauiDialogContentProvider { null },
            MauiDialogPresentation(),
        )
        val loading = MauiLoadingViewModel(
            MauiLoadingContentProvider { null },
            progressReceiver = null,
        )

        val dialogFailure = assertThrows<IllegalStateException> { dialog.createContentView() }
        val loadingFailure = assertThrows<IllegalStateException> { loading.createContentView() }

        // 3 種の提示はどれも同じ状況を報告するため、文言も同一にそろえる
        assertEquals(
            "The MAUI side could not create the presentation content.",
            dialogFailure.message,
        )
        assertEquals(
            "The MAUI side could not create the presentation content.",
            loadingFailure.message,
        )
    }
}
