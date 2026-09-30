package jp.kamusoft.ksdialogs.support

import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry

/**
 * OS の「全画面表示の確認」ウィンドウを、検証の間だけ出なくする。
 *
 * バーを隠した画面の上に、フォーカスを取るウィンドウ (Dialog / Loading の器) が載ると、Android 16 では
 * OS が全画面表示の操作方法を知らせる確認ウィンドウを出すことがある。このウィンドウはフォーカスと
 * バーの制御を取り、その間はバーが一瞬出ることもある。器の指定とは関係のない OS 側の現象で、
 * 器が提示先の指定を変えないという約束の外にある (core/ADR-0040)。利用者が一度確認すれば出なくなるものなので、
 * 検証では端末の設定 `immersive_mode_confirmations` を「確認済み」にして止め、終わったら元の値へ戻す。
 *
 * 設定はシェルの権限で読み書きする。元の値へ戻せない値 (空白を含む) だった場合は、書き換える前に失敗させる。
 */
internal object ImmersiveModeConfirmation {

    /** 端末の設定の名前。 */
    private const val SETTING_NAME = "immersive_mode_confirmations"

    /** 確認を全体で済ませたことを表す値。 */
    private const val CONFIRMED = "confirmed"

    /** `settings get` が、設定が存在しないときに返す表記。 */
    private const val UNSET = "null"

    /**
     * 確認ウィンドウを止めた状態で [block] を実行し、終わったら (失敗した場合も) 設定を元の値へ戻す。
     */
    inline fun <T> whileSuppressed(block: () -> T): T {
        val original = suppress()
        try {
            return block()
        } finally {
            restore(original)
        }
    }

    /**
     * 設定を「確認済み」に切り替え、元の値を返す。元が未設定なら null。
     */
    fun suppress(): String? {
        val original = shell("settings get secure $SETTING_NAME").removeSuffix("\n")
        check(original.none { it.isWhitespace() }) {
            "設定 $SETTING_NAME の元の値に空白が含まれ、元へ戻せないので書き換えない: [$original]"
        }
        shell("settings put secure $SETTING_NAME $CONFIRMED")
        return original.takeUnless { it == UNSET }
    }

    /**
     * [suppress] が返した元の値へ戻す。
     *
     * 元が未設定なら設定ごと消す。元の値は空文字のこともあり、`settings put` では空文字を渡せないので、
     * 設定の content provider への挿入 (同じ名前なら置き換え) で書き戻す。
     */
    fun restore(original: String?) {
        if (original == null) {
            shell("settings delete secure $SETTING_NAME")
        } else {
            shell(
                "content insert --uri content://settings/secure" +
                    " --bind name:s:$SETTING_NAME --bind value:s:$original",
            )
        }
    }

    /** シェルの権限でコマンドを実行し、終わるまで待って標準出力を返す。引数は空白で区切られる。 */
    private fun shell(command: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText() }
    }
}
