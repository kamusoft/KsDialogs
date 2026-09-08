package jp.kamusoft.ksdialogs

import android.app.Activity
import android.graphics.Color
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.kamusoft.ksdialogs.support.DialogLayoutTestActivity
import jp.kamusoft.ksdialogs.support.InstrumentedDialogWaiting
import jp.kamusoft.ksdialogs.support.LoadingTestGate
import jp.kamusoft.ksdialogs.support.LoadingTestHarness
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt

/**
 * 既定ローディングのスタイル (core/ADR-0023) を確かめる。iOS Native の LoadingStyleTests のミラー。
 *
 * スタイルは設定プロパティへの一括設定だけで供給され、器は各表示の開始時に読む。
 * そのため表示中の設定変更は現在の表示に効かず、次の表示から観察できる。
 */
@RunWith(AndroidJUnit4::class)
class LoadingStyleTests {

    @get:Rule
    val activityRule: ActivityScenarioRule<DialogLayoutTestActivity> =
        ActivityScenarioRule(DialogLayoutTestActivity::class.java)

    @Test
    fun LD_ST_01_スタイル変更は次の表示から効く() = runBlocking<Unit> {
        val harness = newHarness()
        harness.loading.style = LoadingStyle(
            indicatorColor = Color.WHITE,
            messageFontSize = 14.0,
            messageColor = Color.WHITE,
            defaultMessage = "読み込み中",
        )

        harness.loading.show()
        val presentedContentView = requireNotNull(harness.builtinContentView)

        harness.loading.style = LoadingStyle(
            indicatorColor = Color.RED,
            messageFontSize = 24.0,
            messageColor = Color.YELLOW,
            defaultMessage = "処理中",
        )

        assertEquals("表示中の見た目は変わらない", "読み込み中", presentedContentView.displayedText)
        assertEquals(14, presentedContentView.displayedMessageFontSize.roundToInt())
        assertEquals(Color.WHITE, presentedContentView.displayedMessageColor)
        assertEquals(Color.WHITE, presentedContentView.displayedIndicatorColor)

        harness.loading.hide()
        harness.loading.show()
        val restyledContentView = requireNotNull(harness.builtinContentView)

        assertEquals("再表示から変更後のスタイルが効く", "処理中", restyledContentView.displayedText)
        assertEquals(24, restyledContentView.displayedMessageFontSize.roundToInt())
        assertEquals(Color.YELLOW, restyledContentView.displayedMessageColor)
        assertEquals(Color.RED, restyledContentView.displayedIndicatorColor)

        harness.loading.hide()
    }

    @Test
    fun LD_ST_02_メッセージ未指定なら既定メッセージが表示される() = runBlocking<Unit> {
        val harness = newHarness()
        harness.loading.style = LoadingStyle(defaultMessage = "読み込み中")

        harness.loading.show()
        assertEquals("読み込み中", harness.builtinText)
        harness.loading.hide()

        harness.loading.show(message = "保存しています")
        assertEquals(
            "指定したメッセージが既定メッセージより優先される",
            "保存しています",
            harness.builtinText,
        )
        harness.loading.hide()
    }

    @Test
    fun LD_ST_03_フォーマット関数の差し替えが進捗表示に反映される() = runBlocking<Unit> {
        val harness = newHarness()
        harness.loading.style = LoadingStyle(
            defaultMessage = "読み込み中",
            progressFormat = { message, progress ->
                if (progress == null) {
                    "[${message.orEmpty()}]"
                } else {
                    "[${message.orEmpty()}|${(progress * 100).roundToInt()}]"
                }
            },
        )
        val gate = LoadingTestGate()
        val report = AtomicReference<(Double) -> Unit>()

        coroutineScope {
            val scope = async {
                harness.loading.start { reporter ->
                    report.set(reporter)
                    gate.await()
                }
            }
            assertTrue(harness.waitUntilPresenting())
            assertTrue(InstrumentedDialogWaiting.waitUntil { report.get() != null })

            assertEquals(
                "進捗未報告でも差し替えた関数が使われる",
                "[読み込み中]",
                harness.builtinText,
            )

            report.get()(0.45)
            assertTrue(
                InstrumentedDialogWaiting.waitUntil { harness.builtinText == "[読み込み中|45]" },
            )

            gate.open()
            scope.await()
        }
    }

    @Test
    fun 内蔵コンテンツのメッセージは太字で_行間が空く() = runBlocking<Unit> {
        val harness = newHarness()

        harness.loading.show(message = "読み込み中")
        val contentView = requireNotNull(harness.builtinContentView)

        assertTrue(contentView.displayedMessageIsBold)
        assertTrue(
            "複数行になったときに行が詰まらない",
            contentView.displayedMessageLineSpacing > 0f,
        )

        harness.loading.hide()
    }

    @Test
    fun 既定のフォーマットはメッセージと百分率を改行で連ねる() {
        val format = LoadingStyle.DEFAULT_PROGRESS_FORMAT

        assertEquals("読み込み中", format("読み込み中", null))
        assertEquals("読み込み中\n45%", format("読み込み中", 0.45))
        assertEquals("45%", format(null, 0.45))
        assertEquals("", format(null, null))
    }

    private fun newHarness(): LoadingTestHarness {
        val harness = AtomicReference<LoadingTestHarness>()
        activityRule.scenario.onActivity { activity: Activity ->
            harness.set(LoadingTestHarness(activity))
        }
        return requireNotNull(harness.get())
    }
}
