package jp.kamusoft.ksdialogs.support

import org.junit.Assert.assertTrue
import kotlin.math.abs

/**
 * 実測した外形がケースの期待 rect と許容誤差内で一致することを確かめる。
 *
 * @param note 失敗時に条件を読み取れるようにする補足
 */
internal fun assertMatchesExpectedRect(
    layoutCase: DialogLayoutCase,
    actual: DialogLayoutCase.Rect,
    note: String = "",
) {
    assertMatchesRect(
        expected = layoutCase.expected,
        actual = actual,
        note = if (note.isEmpty()) layoutCase.id else "${layoutCase.id} [$note]",
    )
}

/**
 * 実測した外形が期待 rect と許容誤差内で一致することを確かめる。
 *
 * 許容誤差はケース表が1つだけ持つ値 (端末ごとの画素密度による丸めを吸収するためのもの) を使い、
 * テスト側で別の基準を作らない。
 *
 * @param note 失敗時に条件を読み取れるようにする補足
 */
internal fun assertMatchesRect(
    expected: DialogLayoutCase.Rect,
    actual: DialogLayoutCase.Rect,
    note: String = "",
) {
    val tolerance = DialogLayoutCaseLoader.table.tolerance
    val detail = buildString {
        if (note.isNotEmpty()) {
            append(note).append(": ")
        }
        append("期待 ").append(expected).append(" 実測 ").append(actual)
    }
    assertTrue("x が一致しない — $detail", abs(actual.x - expected.x) <= tolerance)
    assertTrue("y が一致しない — $detail", abs(actual.y - expected.y) <= tolerance)
    assertTrue("w が一致しない — $detail", abs(actual.w - expected.w) <= tolerance)
    assertTrue("h が一致しない — $detail", abs(actual.h - expected.h) <= tolerance)
}
