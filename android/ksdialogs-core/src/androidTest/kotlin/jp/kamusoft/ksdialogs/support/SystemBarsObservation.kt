package jp.kamusoft.ksdialogs.support

import android.app.Activity
import android.os.Build
import android.os.ParcelFileDescriptor
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.atomic.AtomicReference

/**
 * 器のウィンドウと提示先のウィンドウが持つシステムバーの指定を読む。
 *
 * 明暗は「ステータスバーの明暗を決める指定」を読み、実際に描かれた文字色は見ない
 * (暗幕に合わせて OS が文字色を選ぶことは器の約束の外にあるため。core/ADR-0040)。
 *
 * 隠れたバーの再表示の作法は、アプリから読める値 (`WindowInsetsController.getSystemBarsBehavior`) では
 * 「指定していない」と「触れたら出すを指定した」を見分けられない版がある (どちらも 0 を返す)。
 * そのため作法の既定を確かめる検証は、ウィンドウ管理 (WindowManager) が保持している値を
 * `dumpsys window windows` から読む。
 */
internal object SystemBarsObservation {

    /** ウィンドウ管理の出力で、作法を指定していないウィンドウの作法 (BEHAVIOR_DEFAULT) を表す名前。 */
    const val WINDOW_MANAGER_DEFAULT_BEHAVIOR: String = "DEFAULT"

    /** ウィンドウ管理の出力で、作法の値が 0 のとき (項目ごと出力されない) に返す名前。 */
    const val WINDOW_MANAGER_ZERO_BEHAVIOR: String = "(0)"

    /** UI スレッドで値を読む。 */
    fun <T> readOnMain(read: () -> T): T {
        val value = AtomicReference<T>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync { value.set(read()) }
        @Suppress("UNCHECKED_CAST")
        return value.get() as T
    }

    /**
     * そのウィンドウが要求しているアイコンの明暗 (明るい地向けのステータスバー / ナビゲーションバー)。
     *
     * 値は Android 11 以上の表現 (`APPEARANCE_LIGHT_STATUS_BARS` / `APPEARANCE_LIGHT_NAVIGATION_BARS`) にそろえる。
     * Android 11 以上は WindowInsetsController の値を、それ未満は旧来のフラグを同じビットへ写して返す。
     * UI スレッドで呼ぶ。
     */
    fun requestedLightBars(window: Window): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            (window.insetsController?.systemBarsAppearance ?: 0) and LIGHT_BARS_MASK
        } else {
            legacyLightBars(window)
        }

    /**
     * そのウィンドウの View 全体から集めた旧来のフラグが表す明暗を、Android 11 以上の表現で返す。
     * UI スレッドで呼ぶ。
     */
    fun legacyLightBars(window: Window): Int {
        @Suppress("DEPRECATION")
        val flags = window.decorView.windowSystemUiVisibility
        var lightBars = 0
        @Suppress("DEPRECATION")
        if (flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR != 0) {
            lightBars = lightBars or WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
        }
        @Suppress("DEPRECATION")
        if (flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR != 0) {
            lightBars = lightBars or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
        }
        return lightBars
    }

    /** そのウィンドウに届いている insets でバーが見えているか。insets が未着なら null。UI スレッドで呼ぶ。 */
    fun isBarVisible(window: Window, type: Int): Boolean? {
        val insets = window.decorView.rootWindowInsets ?: return null
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && insets.isVisible(type)
    }

    /** そのウィンドウの隠れたバーの再表示の作法としてアプリから読める値 (Android 11 以上)。UI スレッドで呼ぶ。 */
    fun behavior(window: Window): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.systemBarsBehavior ?: 0
        } else {
            0
        }

    /**
     * 提示先の画面でステータスバーとナビゲーションバーを隠し、隠れたバーの再表示の作法を
     * 「スワイプで一時的に出す」に指定する (Android 11 以上)。両バーが隠れたことを確かめてから戻る。
     */
    suspend fun hideHostBars(activity: Activity) {
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { "Android 11 以上でだけ使う" }
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            activity.window.setDecorFitsSystemWindows(false)
            activity.window.insetsController?.apply {
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(SYSTEM_BARS)
            }
        }
        val hidden = InstrumentedStateSettling.awaitSettled(BARS_SETTLE_TIMEOUT_MILLIS, BARS_STABLE_MILLIS) {
            readOnMain {
                isBarVisible(activity.window, WindowInsets.Type.statusBars()) == false &&
                    isBarVisible(activity.window, WindowInsets.Type.navigationBars()) == false
            }
        }
        check(hidden) { "前提: 提示先のステータスバーとナビゲーションバーが隠れなかった" }
    }

    /**
     * 観測の間に、そのウィンドウでバーが一度でも見えたかを記録する。
     *
     * 「隠れたままである」はアサーション時点の 1 回の読みではなく、観測の全期間の履歴で見る
     * (再出現してまた隠れた場合を見落とさないため)。
     */
    class BarVisibilityRecorder(private val window: Window) {

        /** 観測した見えの変化。失敗の説明文に添える。 */
        val history: StateHistory = StateHistory()

        /** 観測中にステータスバーかナビゲーションバーが一度でも見えたか。 */
        @Volatile
        var sawVisibleBar: Boolean = false
            private set

        /** 今の見えを 1 回読んで積む。 */
        fun sample() {
            val (status, navigation) = readOnMain {
                isBarVisible(window, WindowInsets.Type.statusBars()) to
                    isBarVisible(window, WindowInsets.Type.navigationBars())
            }
            history.recordChange("statusBars=$status navigationBars=$navigation")
            if (status == true || navigation == true) {
                sawVisibleBar = true
            }
        }
    }

    /**
     * このテストの package が出しているウィンドウのうち、種類が [windowType] のものについて、
     * ウィンドウ管理が保持している作法の名前を返す。
     *
     * 作法の値が 0 のウィンドウは出力に項目が現れないので、[WINDOW_MANAGER_ZERO_BEHAVIOR] を返す。
     * 該当するウィンドウがちょうど1つでなければ、出力の抜粋を添えて失敗させる
     * (取り違えたウィンドウの値で判定しないため)。
     *
     * @param windowType `APPLICATION` (器のウィンドウ) か `BASE_APPLICATION` (提示先の画面)
     */
    fun windowManagerBehavior(windowType: String): String {
        val blocks = ownWindowBlocks().filter { block -> block.windowType == windowType }
        check(blocks.size == 1) {
            "種類 $windowType のウィンドウがちょうど1つ見つからない (${blocks.size} 件)\n" +
                blocks.joinToString("\n---\n") { it.text }
        }
        return blocks.single().behavior ?: WINDOW_MANAGER_ZERO_BEHAVIOR
    }

    /** このテストの package の起動時の表示 (スプラッシュ) のウィンドウがまだ残っているか。 */
    fun isStartingWindowPresent(): Boolean =
        ownWindowBlocks().any { block -> block.windowType == STARTING_WINDOW_TYPE }

    /** 観測の説明文に添えるための、このテストの package のウィンドウの一覧 (種類と作法)。 */
    fun describeOwnWindows(): String =
        ownWindowBlocks().joinToString(", ") { "${it.windowType}(bhv=${it.behavior})" }

    private fun ownWindowBlocks(): List<WindowBlock> {
        val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        return parseWindowBlocks(dumpWindows()).filter { block -> block.header.contains(packageName) }
    }

    private fun dumpWindows(): String {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("dumpsys window windows")
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText() }
    }

    /** `Window #N Window{...}:` で始まる区切りごとに、種類と作法を取り出す。 */
    private fun parseWindowBlocks(dump: String): List<WindowBlock> {
        val blocks = mutableListOf<WindowBlock>()
        var current: MutableList<String>? = null
        for (line in dump.lineSequence()) {
            if (WINDOW_HEADER.containsMatchIn(line)) {
                current?.let { blocks.add(WindowBlock.of(it)) }
                current = mutableListOf(line)
            } else {
                current?.add(line)
            }
        }
        current?.let { blocks.add(WindowBlock.of(it)) }
        return blocks
    }

    /** ウィンドウ管理の出力のうち、ウィンドウ1つ分。 */
    private class WindowBlock(val header: String, val text: String, val windowType: String?, val behavior: String?) {
        companion object {
            fun of(lines: List<String>): WindowBlock {
                val text = lines.joinToString("\n")
                return WindowBlock(
                    header = lines.first(),
                    text = text,
                    windowType = WINDOW_TYPE.find(text)?.groupValues?.get(1),
                    behavior = BEHAVIOR.find(text)?.groupValues?.get(1),
                )
            }
        }
    }

    private val LIGHT_BARS_MASK: Int = SystemBarsTestActivity.LIGHT_BARS_APPEARANCE

    /** 隠す・出すの対象にするバー。 */
    private val SYSTEM_BARS: Int
        get() = WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()

    /** バーの出入りが落ち着くのを待つ上限 (ミリ秒)。 */
    private const val BARS_SETTLE_TIMEOUT_MILLIS = 10_000L

    /** バーの見えが落ち着いたとみなすまでに成り立ち続ける時間 (ミリ秒)。出入りの演出より長くとる。 */
    private const val BARS_STABLE_MILLIS = 500L

    private const val STARTING_WINDOW_TYPE = "APPLICATION_STARTING"

    private val WINDOW_HEADER = Regex("""^\s*Window #\d+ Window\{""")
    private val WINDOW_TYPE = Regex("""(?:^|\s)ty=([A-Z_]+)""")
    private val BEHAVIOR = Regex("""(?:^|\s)bhv=([A-Z_]+)""")
}
