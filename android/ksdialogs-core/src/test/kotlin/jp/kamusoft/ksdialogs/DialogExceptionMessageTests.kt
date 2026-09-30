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
        )
    }

    @Test
    fun `PB-HA-03 DialogException のサブクラスに提示先の不在が無い`() {
        val failures: List<DialogException> = listOf(
            DialogException.ViewFactoryNotRegistered("A"),
            DialogException.ViewModelFactoryNotRegistered("B"),
            DialogException.ViewModelAlreadyShowing("C"),
            DialogException.ValueClassViewModel("D"),
        )

        // else を持たない網羅の when。提示先の不在を表すサブクラスがあればコンパイルが通らない
        val labels = failures.map { failure ->
            when (failure) {
                is DialogException.ViewFactoryNotRegistered -> "view-factory"
                is DialogException.ViewModelFactoryNotRegistered -> "view-model-factory"
                is DialogException.ViewModelAlreadyShowing -> "already-showing"
                is DialogException.ValueClassViewModel -> "value-class"
            }
        }

        assertEquals(listOf("view-factory", "view-model-factory", "already-showing", "value-class"), labels)
    }
}
