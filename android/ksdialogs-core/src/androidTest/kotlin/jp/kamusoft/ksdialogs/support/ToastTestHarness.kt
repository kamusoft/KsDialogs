package jp.kamusoft.ksdialogs.support

import android.content.Context
import android.os.Looper
import android.view.View
import jp.kamusoft.ksdialogs.Loading
import jp.kamusoft.ksdialogs.LoadingCoordinator
import jp.kamusoft.ksdialogs.LoadingSettings
import jp.kamusoft.ksdialogs.LoadingViewRegistry
import jp.kamusoft.ksdialogs.Toast
import jp.kamusoft.ksdialogs.ToastContainer
import jp.kamusoft.ksdialogs.ToastCoordinator
import jp.kamusoft.ksdialogs.ToastDefaultContentView
import jp.kamusoft.ksdialogs.ToastLoadingFrontKeeper
import jp.kamusoft.ksdialogs.ToastSettings
import jp.kamusoft.ksdialogs.ToastViewRegistry

/**
 * レジストリ・提示先・[Toast] を組み立てて、表示中のリストと器の取り付きを観察できるようにする。
 *
 * 既定のシングルトンとは別の coordinator を使うため、テストどうしが状態を共有しない。
 * 同じ提示先に載る [Loading] も併せて用意し、Toast からの前面化の依頼もその Loading へつなぐので、
 * 機能間の前後関係もこの器一式で観察できる。
 *
 * @param surface 提示先と前面の判定を供給する面。提示先不在の状況はこの面に null を持たせて再現する
 */
internal class ToastTestHarness(val surface: ToastTestPresentationSurface) {

    /**
     * 提示先を固定した面で組み立てる。
     *
     * @param hostContext 提示先。null にすると提示先が存在しない状況になる
     */
    constructor(hostContext: Context?) : this(ToastTestPresentationSurface(hostContext))

    val registry: ToastViewRegistry = ToastViewRegistry()
    val settings: ToastSettings = ToastSettings()
    val announcer: ToastTestAnnouncer = ToastTestAnnouncer()

    /** 同じ提示先に載る Loading。機能間の前後関係の観察に使う。 */
    val loadingCoordinator: LoadingCoordinator = LoadingCoordinator(
        LoadingViewRegistry(),
        LoadingSettings(),
        LoadingTestPresentationSurface(surface.hostContext),
    )
    val loading: Loading = Loading(loadingCoordinator)

    val coordinator: ToastCoordinator = ToastCoordinator(
        registry = registry,
        settings = settings,
        presentationSurface = surface,
        announcer = announcer,
        loadingFrontKeeper = ToastLoadingFrontKeeper {
            loadingCoordinator.bringToFrontWithoutPresentation()
        },
    )
    val toast: Toast = Toast(coordinator)

    // 以下の読み取り口は UI スレッドの上で読む。coordinator の表示の列は UI スレッドが書き換えるため、
    // テストのスレッドから走査すると追加・削除と重なって ConcurrentModificationException になり得る

    /** 表示中の Toast の枚数 (提示先待ちのものを含む)。 */
    val displayCount: Int
        get() = readOnMain { coordinator.displayCount }

    /** 取り付け済みの器。並びは起動順 (後ろほど手前)。返すのは読んだ時点の写し。 */
    val containers: List<ToastContainer>
        get() = readOnMain { coordinator.presentedContainers }

    /** 取り付け済みの器に載っている中身の View。並びは起動順。返すのは読んだ時点の写し。 */
    val contentViews: List<View>
        get() = readOnMain { coordinator.presentedContentViews }

    /** 表示中のデフォルト View。並びは起動順。返すのは読んだ時点の写し。 */
    val defaultContentViews: List<ToastDefaultContentView>
        get() = readOnMain { coordinator.presentedDefaultContentViews }

    /** 何かが取り付いているか。 */
    val isPresenting: Boolean
        get() = readOnMain { coordinator.isPresenting }

    /** 指定した枚数の器が取り付くまで待つ。枚数は毎回 UI スレッドで読む。 */
    suspend fun waitUntilPresenting(count: Int = 1): Boolean =
        InstrumentedDialogWaiting.waitUntil { containers.size == count }

    /** 表示が全て消えるまで待つ。 */
    suspend fun waitUntilEmpty(timeoutMillis: Long = 10_000L): Boolean =
        InstrumentedDialogWaiting.waitUntil(timeoutMillis) { displayCount == 0 }

    /** 提示先の入れ替わり (画面の再生成) を起こす。 */
    fun changeHost(newHost: Context?) {
        surface.changeHost(newHost)
    }

    /** 提示先を失ったまま背面へ下がったことを起こす。 */
    fun leaveForeground() {
        surface.leaveForeground()
    }

    companion object {
        /**
         * UI スレッドの上で値を読む。UI スレッドから呼ばれたらその場で読む。
         *
         * coordinator を直接読むテストもこれを通して、UI スレッドが書き換える状態をテストのスレッドから走査しない。
         */
        fun <T> readOnMain(read: () -> T): T =
            if (Looper.getMainLooper().isCurrentThread) read() else LoadingLayoutObservation.readOnMain(read)
    }
}
