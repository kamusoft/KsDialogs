package jp.kamusoft.ksdialogs

import android.graphics.Color
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.TextView
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import jp.kamusoft.ksdialogs.support.DialogLayoutTestActivity
import jp.kamusoft.ksdialogs.support.DialogScreenshotEvidence
import jp.kamusoft.ksdialogs.support.attach
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/**
 * ダイアログのウィンドウが提示先の画面のシステムバー表示状態を引き継ぐことを、実ウィンドウで確かめる。
 *
 * 「バーが再出現しないか」は提示先のウィンドウに届く insets で見る (バーが本当に出れば、
 * 提示先の可視領域が狭まって insets に現れる)。ダイアログのウィンドウ自身が持つ状態も
 * あわせて読み、引き継ぎがダイアログ側にも入っていることを確かめる。
 *
 * Android 11 未満は systemUiVisibility の丸ごとのコピーが経路になるため、観察対象もフラグになる。
 */
@RunWith(AndroidJUnit4::class)
class DialogSystemBarsTests {

    @get:Rule
    val activityRule: ActivityScenarioRule<DialogLayoutTestActivity> =
        ActivityScenarioRule(DialogLayoutTestActivity::class.java)

    /** 提示先の画面。UI スレッドの外からウィンドウを取り出せるよう、先に掴んでおく。 */
    private lateinit var hostActivity: DialogLayoutTestActivity

    @Before
    fun captureHostActivity() {
        activityRule.scenario.onActivity { hostActivity = it }
    }

    @Test
    fun PB_SB_01_全システムバー非表示の画面でダイアログを出してもバーが再出現しない() {
        assumeInsetsControllerPath()
        val hiddenTypes = WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()
        hideHostSystemBars(hiddenTypes)
        val hostBehavior = readHostBehavior()

        val container = presentContainer()
        try {
            assertTrue(
                "提示先のステータスバーが再出現した",
                awaitHostBarHidden(WindowInsets.Type.statusBars()),
            )
            assertTrue(
                "提示先のナビゲーションバーが再出現した",
                awaitHostBarHidden(WindowInsets.Type.navigationBars()),
            )
            assertTrue(
                "ダイアログのウィンドウ側でステータスバーが表示されている",
                awaitDialogBarVisibility(container, WindowInsets.Type.statusBars(), visible = false),
            )
            assertTrue(
                "ダイアログのウィンドウ側でナビゲーションバーが表示されている",
                awaitDialogBarVisibility(container, WindowInsets.Type.navigationBars(), visible = false),
            )
            assertEquals(
                "隠れたバーの再表示の作法が引き継がれていない",
                hostBehavior,
                readDialogBehavior(container),
            )
            DialogScreenshotEvidence.capture("PB-SB-01-all-bars-hidden")
        } finally {
            dismiss(container)
        }
    }

    @Test
    fun PB_SB_02_ステータスバーのみ非表示の画面を引き継ぐ() {
        assumeInsetsControllerPath()
        hideHostSystemBars(WindowInsets.Type.statusBars())

        val container = presentContainer()
        try {
            assertTrue(
                "提示先のステータスバーが再出現した",
                awaitHostBarHidden(WindowInsets.Type.statusBars()),
            )
            assertTrue(
                "ダイアログのウィンドウ側でステータスバーが表示されている",
                awaitDialogBarVisibility(container, WindowInsets.Type.statusBars(), visible = false),
            )
            assertTrue(
                "隠していないナビゲーションバーまで隠れた",
                awaitDialogBarVisibility(container, WindowInsets.Type.navigationBars(), visible = true),
            )
            DialogScreenshotEvidence.capture("PB-SB-02-status-bar-hidden")
        } finally {
            dismiss(container)
        }
    }

    @Test
    fun PB_SB_03_ナビゲーションバーのみ非表示の画面を引き継ぐ() {
        assumeInsetsControllerPath()
        hideHostSystemBars(WindowInsets.Type.navigationBars())

        val container = presentContainer()
        try {
            assertTrue(
                "提示先のナビゲーションバーが再出現した",
                awaitHostBarHidden(WindowInsets.Type.navigationBars()),
            )
            assertTrue(
                "ダイアログのウィンドウ側でナビゲーションバーが表示されている",
                awaitDialogBarVisibility(container, WindowInsets.Type.navigationBars(), visible = false),
            )
            assertTrue(
                "隠していないステータスバーまで隠れた",
                awaitDialogBarVisibility(container, WindowInsets.Type.statusBars(), visible = true),
            )
            DialogScreenshotEvidence.capture("PB-SB-03-navigation-bar-hidden")
        } finally {
            dismiss(container)
        }
    }

    @Test
    fun PB_SB_04_旧経路でも非表示状態が維持される() {
        assumeTrue(
            "systemUiVisibility の経路は Android 11 未満にしかない",
            Build.VERSION.SDK_INT < Build.VERSION_CODES.R,
        )
        hideHostSystemBarsWithLegacyFlags()

        val container = presentContainer()
        try {
            assertTrue(
                "提示先のステータスバーが再出現した",
                awaitCondition { legacySystemWindowInsetTop(hostWindow()) == 0 },
            )
            val dialogWindow = requireNotNull(container.window)
            @Suppress("DEPRECATION")
            val requested = readOnMainThread { dialogWindow.decorView.systemUiVisibility }
            @Suppress("DEPRECATION")
            val effective = readOnMainThread { dialogWindow.decorView.windowSystemUiVisibility }
            assertEquals(
                "非表示のフラグがダイアログのウィンドウへ引き継がれていない",
                LEGACY_HIDE_FLAGS,
                requested and LEGACY_HIDE_FLAGS,
            )
            assertEquals(
                "システムが実際に適用した状態が非表示になっていない",
                LEGACY_HIDE_FLAGS,
                effective and LEGACY_HIDE_FLAGS,
            )
            DialogScreenshotEvidence.capture("PB-SB-04-legacy-bars-hidden")
        } finally {
            dismiss(container)
        }
    }

    @Test
    fun PB_SB_05_通常表示の画面では従来どおり() {
        val statusInsetBefore = readOnMainThread { legacySystemWindowInsetTop(hostWindow()) }
        assumeTrue("提示先でシステムバーが観測できていない", statusInsetBefore > 0)

        val container = presentContainer()
        try {
            // 提示先に届く可視領域が変わらない = システムバーの表示状態が変わっていない
            assertTrue(
                "ダイアログの表示でシステムバーの表示状態が変わった",
                awaitCondition { legacySystemWindowInsetTop(hostWindow()) == statusInsetBefore },
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                assertTrue(
                    "ダイアログのウィンドウ側でステータスバーが隠れている",
                    awaitDialogBarVisibility(container, WindowInsets.Type.statusBars(), visible = true),
                )
                assertTrue(
                    "ダイアログのウィンドウ側でナビゲーションバーが隠れている",
                    awaitDialogBarVisibility(
                        container,
                        WindowInsets.Type.navigationBars(),
                        visible = true,
                    ),
                )
            }
        } finally {
            dismiss(container)
        }
    }

    @Test
    fun PB_SB_06_表示後の提示先の可視状態の変更には追随しない() {
        assumeInsetsControllerPath()

        val container = presentContainer()
        try {
            assertTrue(
                "表示時点でステータスバーが表示されていない",
                awaitDialogBarVisibility(container, WindowInsets.Type.statusBars(), visible = true),
            )

            // 表示中に提示先が全システムバーを隠す
            hideHostSystemBars(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            waitForSettling()

            assertTrue(
                "表示後の提示先の変更に追随してしまった",
                readOnMainThread { isDialogBarVisible(container, WindowInsets.Type.statusBars()) },
            )
            assertTrue(
                "表示後の提示先の変更に追随してしまった",
                readOnMainThread { isDialogBarVisible(container, WindowInsets.Type.navigationBars()) },
            )
        } finally {
            dismiss(container)
        }
    }

    @Test
    fun PB_SB_07_表示後の提示先の_behavior_の変更には追随しない() {
        assumeInsetsControllerPath()

        val container = presentContainer()
        try {
            val adopted = readDialogBehavior(container)
            assertEquals("提示先の既定の作法が引き継がれていない", readHostBehavior(), adopted)

            // 表示中に提示先が作法を変える
            setHostBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE)
            waitForSettling()

            assertEquals(
                "表示後の提示先の変更に追随してしまった",
                adopted,
                readDialogBehavior(container),
            )
        } finally {
            dismiss(container)
        }
    }

    // 組み立て

    /** ダイアログを1枚出し、ウィンドウが画面に載って表示状態になるまで待つ。 */
    private fun presentContainer(): DialogContainer {
        val container = AtomicReference<DialogContainer>()
        activityRule.scenario.onActivity { activity ->
            DialogContainer(
                context = activity,
                contentView = evidenceContentView(activity),
                resultChannel = DialogResultChannel(),
            ).also {
                container.set(it)
                it.show()
            }
        }
        val presented = requireNotNull(container.get())
        check(
            awaitCondition {
                val decorView = presented.window?.decorView
                decorView?.isAttachedToWindow == true && decorView.rootWindowInsets != null
            },
        ) { "ダイアログのウィンドウが画面に載らなかった" }
        // 出入りの演出が終わるまでは覆いも中身も透明なので、見えを撮る証跡のために表示完了まで待つ
        check(awaitCondition { presented.containerState == DialogContainerState.SHOWN }) {
            "ダイアログが表示状態にならなかった"
        }
        return presented
    }

    /**
     * 証跡の画面で見分けが付く中身。
     *
     * 覆いの上に置かれたことが写真で分かるよう、背景色と大きめの文字を持たせる。
     */
    private fun evidenceContentView(activity: DialogLayoutTestActivity): TextView =
        TextView(activity).apply {
            text = "システムバーの確認"
            setBackgroundColor(Color.WHITE)
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            setPadding(48, 96, 48, 96)
        }.attach(options = null, placement = null)

    private fun dismiss(container: DialogContainer) {
        activityRule.scenario.onActivity { container.dismiss() }
    }

    /** Android 11 以上の経路でだけ意味を持つ検証であることを宣言する。 */
    private fun assumeInsetsControllerPath() {
        assumeTrue(
            "WindowInsetsController の経路は Android 11 以上にしかない",
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
        )
    }

    /** 提示先の画面でシステムバーを隠す (Android 11 以上の経路)。 */
    @Suppress("DEPRECATION")
    private fun hideHostSystemBars(types: Int) {
        activityRule.scenario.onActivity { activity ->
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                return@onActivity
            }
            activity.window.setDecorFitsSystemWindows(false)
            activity.window.insetsController?.apply {
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(types)
            }
        }
        waitForSettling()
    }

    /** 提示先の画面でシステムバーを隠す (Android 11 未満の経路)。 */
    private fun hideHostSystemBarsWithLegacyFlags() {
        activityRule.scenario.onActivity { activity ->
            @Suppress("DEPRECATION")
            activity.window.decorView.systemUiVisibility =
                activity.window.decorView.systemUiVisibility or LEGACY_HIDE_FLAGS
        }
        waitForSettling()
    }

    /** 提示先の画面で隠れたバーの再表示の作法を変える。 */
    private fun setHostBehavior(behavior: Int) {
        activityRule.scenario.onActivity { activity ->
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                return@onActivity
            }
            activity.window.insetsController?.systemBarsBehavior = behavior
        }
        waitForSettling()
    }

    private fun readHostBehavior(): Int = readOnMainThread {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            hostWindow().insetsController?.systemBarsBehavior ?: 0
        } else {
            0
        }
    }

    private fun readDialogBehavior(container: DialogContainer): Int = readOnMainThread {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            container.window?.insetsController?.systemBarsBehavior ?: 0
        } else {
            0
        }
    }

    /** 提示先のウィンドウにそのバーの余地が届かなくなる (= バーが出ていない) まで待つ。 */
    private fun awaitHostBarHidden(type: Int): Boolean = awaitCondition {
        val windowInsets = hostWindow().decorView.rootWindowInsets ?: return@awaitCondition false
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !windowInsets.isVisible(type)
    }

    /** ダイアログのウィンドウ側の可視状態が期待どおりになるまで待つ。 */
    private fun awaitDialogBarVisibility(
        container: DialogContainer,
        type: Int,
        visible: Boolean,
    ): Boolean = awaitCondition { isDialogBarVisible(container, type) == visible }

    /** ダイアログのウィンドウにそのバーが見えているか (Android 11 以上でのみ意味を持つ)。UI スレッドで読む。 */
    private fun isDialogBarVisible(container: DialogContainer, type: Int): Boolean {
        val windowInsets = container.window?.decorView?.rootWindowInsets ?: return false
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && windowInsets.isVisible(type)
    }

    /** そのウィンドウに届いている上辺のシステム領域の幅 (px)。 */
    private fun legacySystemWindowInsetTop(window: Window): Int {
        val windowInsets = window.decorView.rootWindowInsets ?: return -1
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowInsets.getInsets(WindowInsets.Type.statusBars()).top
        } else {
            @Suppress("DEPRECATION")
            windowInsets.systemWindowInsetTop
        }
    }

    /** 提示先の画面のウィンドウ。View の読み出しは UI スレッドの中で行う。 */
    private fun hostWindow(): Window = hostActivity.window

    /** UI スレッドで値を読む。 */
    private fun <T> readOnMainThread(read: () -> T): T {
        val value = AtomicReference<T>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync { value.set(read()) }
        @Suppress("UNCHECKED_CAST")
        return value.get() as T
    }

    /** 条件が満たされるまで待つ。 */
    private fun awaitCondition(condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + CONDITION_TIMEOUT_MILLIS
        while (System.currentTimeMillis() < deadline) {
            if (readOnMainThread(condition)) {
                return true
            }
            Thread.sleep(POLLING_INTERVAL_MILLIS)
        }
        return readOnMainThread(condition)
    }

    /** システムバーの出入りのアニメーションが落ち着くのを待つ。 */
    private fun waitForSettling() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(SETTLING_WAIT_MILLIS)
    }

    private companion object {
        /** 条件が満たされるのを待つ上限 (ミリ秒)。 */
        const val CONDITION_TIMEOUT_MILLIS = 10_000L

        /** 条件の確認の間隔 (ミリ秒)。 */
        const val POLLING_INTERVAL_MILLIS = 50L

        /** システムバーの出入りが落ち着くまでの待ち (ミリ秒)。 */
        const val SETTLING_WAIT_MILLIS = 1_000L

        /** Android 11 未満でシステムバーを隠すためのフラグ。 */
        @Suppress("DEPRECATION")
        const val LEGACY_HIDE_FLAGS: Int =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }
}
