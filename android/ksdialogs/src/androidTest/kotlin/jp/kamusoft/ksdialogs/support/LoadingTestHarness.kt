package jp.kamusoft.ksdialogs.support

import android.content.Context
import android.view.View
import jp.kamusoft.ksdialogs.Loading
import jp.kamusoft.ksdialogs.LoadingContainer
import jp.kamusoft.ksdialogs.LoadingCoordinator
import jp.kamusoft.ksdialogs.LoadingDefaultContentView
import jp.kamusoft.ksdialogs.LoadingPresentationSurface
import jp.kamusoft.ksdialogs.LoadingSettings
import jp.kamusoft.ksdialogs.LoadingViewRegistry
import kotlinx.coroutines.runBlocking

/**
 * レジストリ・提示先・[Loading] を組み立てて、合流状態と器の取り付きを観察できるようにする。
 *
 * 既定のシングルトンとは別の coordinator を使うため、テストどうしが状態を共有しない。
 *
 * @param surface 提示先を供給する面。実際の画面の追跡を通す検証では本番の面を渡す
 */
internal class LoadingTestHarness(val surface: LoadingPresentationSurface) {

    /**
     * 提示先を固定した面で組み立てる。
     *
     * @param hostContext 提示先。null にすると提示先が存在しない状況になる
     */
    constructor(hostContext: Context?) : this(LoadingTestPresentationSurface(hostContext))

    val registry: LoadingViewRegistry = LoadingViewRegistry()
    val settings: LoadingSettings = LoadingSettings()
    val coordinator: LoadingCoordinator = LoadingCoordinator(registry, settings, surface)
    val loading: Loading = Loading(coordinator)

    /** 器が取り付いているか。 */
    val isPresenting: Boolean
        get() = coordinator.isPresenting

    /** 表示中の中身の View。 */
    val contentView: View?
        get() = coordinator.presentedContentView

    /** 表示中の器。 */
    val container: LoadingContainer?
        get() = coordinator.presentedContainer

    /** 表示中の既定ローディングの内蔵コンテンツ。 */
    val builtinContentView: LoadingDefaultContentView?
        get() = coordinator.presentedBuiltinContentView

    /** 表示中の既定ローディングの表示テキスト。 */
    val builtinText: String?
        get() = builtinContentView?.displayedText

    /** 現在の合流数。 */
    val coalescedUseCount: Int
        get() = coordinator.coalescedUseCount

    /** 器が取り付くまで待つ。 */
    suspend fun waitUntilPresenting(): Boolean =
        InstrumentedDialogWaiting.waitUntil { isPresenting }

    /** 表示を残さずに片付ける。 */
    fun tearDown() {
        runBlocking { loading.hide() }
    }
}
