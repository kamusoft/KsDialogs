package jp.kamusoft.ksdialogs.maui

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * MAUI 側が中身を作れなかった Toast が、器の受け皿に届く通常の失敗になることの検証。
 *
 * 中身の供給は managed / native の境界を跨いで呼ばれるため、MAUI 側は失敗を例外のまま返さず
 * 中身なし (null) として返す。境界をそのまま越えた失敗は器の受け皿 (通常の例外を捕まえる)
 * では受け止められないため、この面が null を通常の失敗へ変換する責務を負う。
 * その先の「その 1 枚だけを破棄する」は Native ライブラリの coordinator が受け持つ。
 */
@DisplayName("MAUI 側が中身を作れなかった Toast は受理後の失敗になる")
class MauiToastContentSupplyTests {

    @Test
    @DisplayName("中身なしで返った供給は器が捕まえられる失敗になる")
    fun `中身なしで返った供給は器が捕まえられる失敗になる`() {
        val viewModel = MauiToastViewModel(MauiToastContentProvider { null })

        // 器の受け皿は Error ではなく Exception を捕まえる。ここで受け止められない失敗は
        // 「その 1 枚だけの破棄」に合流せず、そのまま実行を落とす
        val caught: Exception? = try {
            viewModel.createContentView()
            null
        } catch (failure: Exception) {
            failure
        }

        assertNotNull(caught, "中身なしは器の受け皿が捕まえられる失敗になること")
    }
}
