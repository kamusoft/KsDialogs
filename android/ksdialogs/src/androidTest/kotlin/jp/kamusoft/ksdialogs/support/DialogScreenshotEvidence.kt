package jp.kamusoft.ksdialogs.support

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

/**
 * 実機での見えを証跡として残すための撮影。
 *
 * 見えの検査は自動テストの判定には使わない (端末・タイミング依存で脆いため) が、
 * 実行時挙動の確認では「実際にどう見えたか」の記録が要る。
 * 撮影は計測用の引数 [ARGUMENT_NAME] が与えられたときだけ行い、通常の実行では何もしない。
 *
 * 保存先はテスト対象アプリの外部ファイル領域で、`adb pull` で取り出せる。
 */
internal object DialogScreenshotEvidence {

    /** 撮影を有効にする計測用の引数名。 */
    const val ARGUMENT_NAME: String = "ksdialogsEvidence"

    /** 保存先のディレクトリ名。 */
    private const val DIRECTORY_NAME = "evidence"

    /** 撮影が有効か。 */
    val isEnabled: Boolean
        get() = InstrumentationRegistry.getArguments().getString(ARGUMENT_NAME) != null

    /**
     * 今の画面を撮って保存する。無効なら何もしない。
     *
     * @param name 拡張子を除いたファイル名
     * @return 保存先のパス。撮影しなかった場合は null
     */
    fun capture(name: String): String? {
        if (!isEnabled) {
            return null
        }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        val screen = instrumentation.uiAutomation.takeScreenshot() ?: return null
        return try {
            val directory = File(
                requireNotNull(instrumentation.targetContext.getExternalFilesDir(null)) {
                    "外部ファイル領域が使えない"
                },
                DIRECTORY_NAME,
            ).apply { mkdirs() }
            val file = File(directory, "$name.png")
            file.outputStream().use { screen.compress(Bitmap.CompressFormat.PNG, 100, it) }
            file.absolutePath
        } finally {
            screen.recycle()
        }
    }
}
