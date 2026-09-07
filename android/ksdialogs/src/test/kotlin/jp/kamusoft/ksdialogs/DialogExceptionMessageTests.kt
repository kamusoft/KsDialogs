package jp.kamusoft.ksdialogs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

/**
 * ライブラリが外へ出す失敗型の文言を固定する検査 (cross/ADR-0015)。
 *
 * 文言そのものは互換契約ではないが、置き換えの取りこぼしと文言の揺れを検出するため完全一致で見る。
 */
@DisplayName("DialogException の message は英語固定")
class DialogExceptionMessageTests {

    @Test
    fun `DM-AN-01 DialogException の全サブクラスが対応表の英語文言を持つ`() {
        val typeName = "com.example.SampleViewModel"

        assertAll(
            {
                assertEquals(
                    "No View factory is registered for ViewModel type com.example.SampleViewModel.",
                    DialogException.ViewFactoryNotRegistered(typeName).message,
                )
            },
            {
                assertEquals(
                    "No ViewModel factory is registered for ViewModel type com.example.SampleViewModel.",
                    DialogException.ViewModelFactoryNotRegistered(typeName).message,
                )
            },
            {
                assertEquals(
                    "This ViewModel instance of type com.example.SampleViewModel is already being shown.",
                    DialogException.ViewModelAlreadyShowing(typeName).message,
                )
            },
            {
                assertEquals(
                    "ViewModel type com.example.SampleViewModel is a value class and cannot be used as a ViewModel.",
                    DialogException.ValueClassViewModel(typeName).message,
                )
            },
            {
                assertEquals(
                    "No screen is available to present the Dialog.",
                    DialogException.PresentationHostUnavailable().message,
                )
            },
        )
    }
}
