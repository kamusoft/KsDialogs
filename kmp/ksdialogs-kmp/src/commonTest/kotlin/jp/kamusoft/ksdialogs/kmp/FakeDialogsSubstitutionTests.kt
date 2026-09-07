package jp.kamusoft.ksdialogs.kmp

import jp.kamusoft.ksdialogs.kmp.support.BooleanTestDialogViewModel
import jp.kamusoft.ksdialogs.kmp.support.ConfirmationPresenter
import jp.kamusoft.ksdialogs.kmp.support.FakeKsDialog
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * 契約 interface の差し替えだけで共有コードの分岐が検証できることの確認。
 *
 * Native 実装もダイアログの表示も伴わずに、呼び出し役のロジックを通す。
 */
class FakeDialogsSubstitutionTests {

    @Test
    fun `fake 実装で完了の分岐を検証できる`() = runTest {
        val dialogs = FakeKsDialog(DialogResult.Completed(true))

        val decision = ConfirmationPresenter(dialogs).confirm()

        assertEquals("承諾", decision)
        assertIs<BooleanTestDialogViewModel>(dialogs.shownViewModels.single())
    }

    @Test
    fun `fake 実装でキャンセルの分岐を検証できる`() = runTest {
        val dialogs = FakeKsDialog(DialogResult.Cancelled)

        val decision = ConfirmationPresenter(dialogs).confirm()

        assertEquals("中断", decision)
    }
}
