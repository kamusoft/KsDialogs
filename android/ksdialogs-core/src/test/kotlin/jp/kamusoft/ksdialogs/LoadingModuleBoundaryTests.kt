package jp.kamusoft.ksdialogs

import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * 本体モジュールが宣言的 UI (Compose) に依存しない境界 (android/ADR-0001) が、
 * Loading 一式を加えた後も保たれていることを確かめる。
 *
 * 依存グラフの走査は配布物の classpath を見るビルド側の検査が受け持ち、
 * ここでは本体モジュール自身から Compose 系と Compose 版登録面が見えないことを実行時に見る。
 */
class LoadingModuleBoundaryTests {

    @Test
    fun LD_AN_02_本体モジュールは_Loading_追加後も_Compose_に依存しない() {
        ABSENT_CLASS_NAMES.forEach { className ->
            assertNull(
                runCatching { Class.forName(className) }.getOrNull(),
                "$className が本体モジュールから見えている",
            )
        }
    }

    private companion object {
        /**
         * 本体モジュールから見えてはいけないクラス。
         *
         * Compose の土台そのものと、Compose 版の登録・表示面 (別モジュールにのみ存在する) を見る。
         */
        val ABSENT_CLASS_NAMES = listOf(
            "androidx.compose.runtime.Composer",
            "androidx.compose.ui.platform.AbstractComposeView",
            "jp.kamusoft.ksdialogs.compose.ComposeLoadingRegistrationKt",
            "jp.kamusoft.ksdialogs.compose.ComposeLoadingShowKt",
        )
    }
}
