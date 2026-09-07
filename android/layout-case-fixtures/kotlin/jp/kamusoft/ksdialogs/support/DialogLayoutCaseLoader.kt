package jp.kamusoft.ksdialogs.support

import androidx.test.platform.app.InstrumentationRegistry
import jp.kamusoft.ksdialogs.DialogEdgeInsets
import org.json.JSONObject

/**
 * 共通ケース表を読み込む。
 *
 * 表は iOS / Android の両実装が参照する単一の正であり、どちらのビルドルートにも属さない
 * リポジトリルート直下 (`core/layout-spec/cases.json`) に置かれている (core/ADR-0009)。
 * その置き場を instrumented test の asset として運んでいるため、期待値の写しはどこにも作らない。
 */
internal object DialogLayoutCaseLoader {

    /** 読み込み済みのケース表。 */
    val table: DialogLayoutCaseTable by lazy { loadTable() }

    /** ケース表を運んでいる asset の名前。 */
    const val CASE_TABLE_ASSET: String = "cases.json"

    /** 期待値を Android 向けに差し替えてよい唯一の入口。 */
    private const val APPROVED_DIFF_PLATFORM = "android"

    /**
     * 読み込みに失敗したら黙って 0 件で通さず、その場で落として気づけるようにする。
     */
    private fun loadTable(): DialogLayoutCaseTable {
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        val text = try {
            assets.open(CASE_TABLE_ASSET).bufferedReader().use { it.readText() }
        } catch (failure: Exception) {
            throw AssertionError("共通ケース表を読み込めなかった: $CASE_TABLE_ASSET", failure)
        }
        val root = JSONObject(text)
        val cases = root.getJSONArray("cases")
        return DialogLayoutCaseTable(
            unit = root.getString("unit"),
            tolerance = root.getDouble("tolerance"),
            cases = (0 until cases.length()).map { index -> parseCase(cases.getJSONObject(index)) },
        )
    }

    private fun parseCase(json: JSONObject): DialogLayoutCase {
        val id = json.getString("id")
        return DialogLayoutCase(
            id = id,
            screen = parseSize(json.getJSONObject("screen")),
            insets = parseInsets(json.getJSONObject("insets")),
            contentSize = parseSize(json.getJSONObject("contentSize")),
            attributes = parseAttributes(json.optJSONObject("attributes")),
            expected = parseExpected(id, json),
        )
    }

    private fun parseSize(json: JSONObject): DialogLayoutCase.Size =
        DialogLayoutCase.Size(w = json.getDouble("w"), h = json.getDouble("h"))

    private fun parseInsets(json: JSONObject): DialogLayoutCase.Insets =
        DialogLayoutCase.Insets(
            top = json.optDouble("top", 0.0),
            left = json.optDouble("left", 0.0),
            bottom = json.optDouble("bottom", 0.0),
            right = json.optDouble("right", 0.0),
        )

    /**
     * 期待 rect を読む。
     *
     * OS 差は理由と承認の記録を持つ `approvedDiff` のエントリだけが有効で、
     * 記録の欠けたエントリは期待値の逃げ道になるため読み込みの時点で失敗させる (core/ADR-0009)。
     */
    private fun parseExpected(id: String, json: JSONObject): DialogLayoutCase.Rect {
        val expected = json.getJSONObject("expected")
        var x = expected.getDouble("x")
        var y = expected.getDouble("y")
        var w = expected.getDouble("w")
        var h = expected.getDouble("h")

        val approved = json.optJSONObject("approvedDiff")?.optJSONObject(APPROVED_DIFF_PLATFORM)
        if (approved != null) {
            val reason = approved.optString("reason")
            val approvedBy = approved.optString("approvedBy")
            if (reason.isEmpty() || approvedBy.isEmpty()) {
                throw AssertionError("$id: 理由と承認の記録がない approvedDiff は無効")
            }
            val overrides = approved.getJSONObject("expected")
            x = overrides.optDouble("x", x)
            y = overrides.optDouble("y", y)
            w = overrides.optDouble("w", w)
            h = overrides.optDouble("h", h)
        }
        return DialogLayoutCase.Rect(x, y, w, h)
    }

    private fun parseAttributes(json: JSONObject?): DialogLayoutCaseAttributes {
        if (json == null) {
            return DialogLayoutCaseAttributes()
        }
        return DialogLayoutCaseAttributes(
            layoutArea = json.optStringOrNull("layoutArea"),
            dialogMargin = parseMargin(json.opt("dialogMargin")),
            proportionalWidth = json.optDoubleOrNull("proportionalWidth"),
            proportionalHeight = json.optDoubleOrNull("proportionalHeight"),
            horizontalAlignment = json.optStringOrNull("horizontalAlignment"),
            verticalAlignment = json.optStringOrNull("verticalAlignment"),
            offsetX = json.optDoubleOrNull("offsetX"),
            offsetY = json.optDoubleOrNull("offsetY"),
            overlayColor = parseColor(json.optStringOrNull("overlayColor")),
        )
    }

    /** 余白は全辺同値の数値と、辺ごとの指定の両方を受け付ける。 */
    private fun parseMargin(value: Any?): DialogEdgeInsets? = when (value) {
        null -> null
        is Number -> DialogEdgeInsets(value.toDouble())
        is JSONObject -> DialogEdgeInsets(
            top = value.optDouble("top", 0.0),
            left = value.optDouble("left", 0.0),
            bottom = value.optDouble("bottom", 0.0),
            right = value.optDouble("right", 0.0),
        )

        else -> throw AssertionError("余白の指定を解釈できなかった: $value")
    }

    /** `#AARRGGBB` 形式の色を読む (形態間の interop 境界と同じ ARGB 並び)。 */
    private fun parseColor(text: String?): Int? {
        if (text == null) {
            return null
        }
        val digits = text.removePrefix("#")
        if (digits.length != 8) {
            throw AssertionError("色の指定を解釈できなかった: $text")
        }
        return digits.toLong(radix = 16).toInt()
    }

    private fun JSONObject.optStringOrNull(name: String): String? =
        if (isNull(name)) null else optString(name).takeIf { it.isNotEmpty() }

    private fun JSONObject.optDoubleOrNull(name: String): Double? =
        if (has(name) && !isNull(name)) getDouble(name) else null
}
