package jp.kamusoft.ksdialogs

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.kamusoft.ksdialogs.support.DialogLayoutCase
import jp.kamusoft.ksdialogs.support.DialogLayoutTestActivity
import jp.kamusoft.ksdialogs.support.DialogWindowGeometryStage
import jp.kamusoft.ksdialogs.support.assertMatchesRect
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 表示中にウィンドウの寸法・可視領域の余白が変わったときの追随を、実際のレイアウトパスで確かめる。
 *
 * 実効値は器を画面に載せたあとの初回レイアウトパスで固定されており (core/ADR-0015)、
 * 寸法が変わっても固定済みの属性のまま新しい寸法・余白で配置し直される。
 * ソフトキーボードによる余白の変化ではダイアログを動かさないため、ここでは扱わない。
 *
 * 期待値は属性 (基準領域 visibleArea / 全辺 24 の余白 / 比率 幅 0.5・高さ 0.25 / 中央配置) と
 * 新しい寸法・余白から、軸別レイアウト規則の手順で導いた値。許容差は共通ケース表と同じ。
 */
@RunWith(AndroidJUnit4::class)
class DialogWindowChangeTests {

    @get:Rule
    val activityRule: ActivityScenarioRule<DialogLayoutTestActivity> =
        ActivityScenarioRule(DialogLayoutTestActivity::class.java)

    private var stage: DialogWindowGeometryStage? = null

    @After
    fun tearDown() {
        stage?.dispose()
        stage = null
    }

    @Test
    fun PB_WN_01_回転後も配置規則が新しい寸法で成立する() {
        val stage = makeStage()

        // 縦向き: R = 390 x (844 - 59 - 34) = 390 x 751、A は各辺 24 控除、比率は R 基準
        assertMatchesRect(
            expected = DialogLayoutCase.Rect(x = 97.5, y = 340.625, w = 195.0, h = 187.75),
            actual = stage.contentRect(),
            note = "回転前 (縦向き)",
        )

        // 固定後に添付を書き換えても、回転後の再配置は固定済みの値で行われる
        stage.rewriteAttachedOptions(
            DialogOptions(
                layoutArea = DialogLayoutArea.WINDOW,
                dialogMargin = DialogEdgeInsets.ZERO,
                proportionalWidth = 0.9,
                proportionalHeight = 0.9,
            ),
        )
        stage.changeGeometry(LANDSCAPE_SCREEN, LANDSCAPE_INSETS)

        // 横向き: R = (844 - 118) x (390 - 21) = 726 x 369
        assertMatchesRect(
            expected = DialogLayoutCase.Rect(x = 240.5, y = 138.375, w = 363.0, h = 92.25),
            actual = stage.contentRect(),
            note = "回転後 (横向き)",
        )
        assertTrue("実効値の固定は解けない", stage.isLayoutSnapshotFrozen)
    }

    @Test
    fun PB_WN_02_ウィンドウ寸法のみの変化に追随する() {
        val stage = makeStage()

        // 余白は据え置きで幅だけを狭める (マルチウィンドウのリサイズに相当)
        stage.changeGeometry(DialogLayoutCase.Size(w = 320.0, h = 844.0), PORTRAIT_INSETS)

        // 幅は新しい R (320) から導かれ、垂直方向は変わらない
        assertMatchesRect(
            expected = DialogLayoutCase.Rect(x = 80.0, y = 340.625, w = 160.0, h = 187.75),
            actual = stage.contentRect(),
            note = "幅のみ変更",
        )
    }

    @Test
    fun PB_WN_03_可視領域インセットのみの変化に追随する() {
        val stage = makeStage()

        // 寸法は据え置きでシステム領域の余白だけを無くす
        stage.changeGeometry(
            PORTRAIT_SCREEN,
            DialogLayoutCase.Insets(top = 0.0, left = 0.0, bottom = 0.0, right = 0.0),
        )

        // 基準領域は visibleArea なので、垂直方向が新しい余白から導き直される
        assertMatchesRect(
            expected = DialogLayoutCase.Rect(x = 97.5, y = 316.5, w = 195.0, h = 211.0),
            actual = stage.contentRect(),
            note = "余白のみ変更",
        )
    }

    // 組み立て

    /**
     * 比率サイズ・中央配置の属性を添付したダイアログを縦向きのウィンドウへ載せる。
     *
     * 比率と余白の両方が効く属性にすることで、寸法と余白のどちらの変化も外形に現れる。
     */
    private fun makeStage(): DialogWindowGeometryStage {
        val presented = DialogWindowGeometryStage.present(
            scenario = activityRule.scenario,
            screen = PORTRAIT_SCREEN,
            insets = PORTRAIT_INSETS,
            contentSize = CONTENT_SIZE,
            options = DialogOptions(
                layoutArea = DialogLayoutArea.VISIBLE_AREA,
                dialogMargin = DialogEdgeInsets(24.0),
                proportionalWidth = 0.5,
                proportionalHeight = 0.25,
            ),
        )
        stage = presented
        return presented
    }

    private companion object {
        /** 縦向きのウィンドウ (システム領域は上下)。 */
        val PORTRAIT_SCREEN = DialogLayoutCase.Size(w = 390.0, h = 844.0)
        val PORTRAIT_INSETS = DialogLayoutCase.Insets(top = 59.0, left = 0.0, bottom = 34.0, right = 0.0)

        /** 横向きのウィンドウ (システム領域は左右と下)。 */
        val LANDSCAPE_SCREEN = DialogLayoutCase.Size(w = 844.0, h = 390.0)
        val LANDSCAPE_INSETS = DialogLayoutCase.Insets(top = 0.0, left = 59.0, bottom = 21.0, right = 59.0)

        /** 中身が要求する内容サイズ。比率サイズに負けるだけの小ささにする。 */
        val CONTENT_SIZE = DialogLayoutCase.Size(w = 100.0, h = 100.0)
    }
}
