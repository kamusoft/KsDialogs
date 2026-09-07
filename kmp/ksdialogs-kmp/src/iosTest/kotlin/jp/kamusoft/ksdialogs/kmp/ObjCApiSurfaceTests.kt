package jp.kamusoft.ksdialogs.kmp

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Swift / Objective-C から見える面を、生成された framework の ObjC ヘッダで確かめる。
 *
 * ヘッダの場所はビルド定義が環境変数で渡す (テストの実行体からはビルドの出力位置が分からないため)。
 * ヘッダが読めなければ検査は失敗する — 「読めないから素通り」にはしない。
 */
@OptIn(ExperimentalForeignApi::class)
class ObjCApiSurfaceTests {

    @Test
    fun `PB-KT-14 型を渡す表示は Swift や ObjC から見えない`() {
        val header = readGeneratedHeader()

        // 実例を渡す表示とレジストリは従来どおり見える (ヘッダを取り違えていないことの確認も兼ねる)
        assertTrue(
            header.contains("""swift_name("show(viewModel:placement:completionHandler:)")"""),
            "実例を渡す show が Swift から見えなくなりました。",
        )
        assertTrue(
            header.contains("""swift_name("registry")"""),
            "レジストリのプロパティが Swift から見えなくなりました。",
        )

        // ViewModel のクラス参照を引数に取る宣言は、表示も登録口もまとめて ObjC の面から外れている。
        // ObjC 名の綴りを個別に列挙すると、名前が変わったときに検査が黙って素通りする
        val classArgumentDeclarations = header.lines()
            .map { it.trim() }
            .filter { it.startsWith("- (") }
            .filter { it.contains("ViewModelClass:") }

        assertEquals(
            emptyList(),
            classArgumentDeclarations,
            "ViewModel のクラス参照を取る呼び出しが ObjC の面に現れました。",
        )
    }

    /** ビルド定義が渡した場所から、生成された ObjC ヘッダを読む。 */
    private fun readGeneratedHeader(): String {
        val path = NSProcessInfo.processInfo.environment[HEADER_PATH_KEY] as String?
        assertNotNull(path, "ヘッダの場所が渡されませんでした ($HEADER_PATH_KEY)。")
        val header = NSString.stringWithContentsOfFile(path, encoding = NSUTF8StringEncoding, error = null)
        return assertNotNull(header, "生成された ObjC ヘッダを読めませんでした: $path")
    }

    private companion object {
        const val HEADER_PATH_KEY: String = "KSDIALOGS_KMP_OBJC_HEADER"
    }
}
