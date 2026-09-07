package jp.kamusoft.ksdialogs.support

import android.content.Context
import android.content.ContextWrapper
import jp.kamusoft.ksdialogs.DialogContainer
import jp.kamusoft.ksdialogs.DialogPlacement
import jp.kamusoft.ksdialogs.DialogPresentationRequest
import jp.kamusoft.ksdialogs.DialogPresentationSurface
import jp.kamusoft.ksdialogs.PresentedDialog

/**
 * 提示面の差し替え実装。
 *
 * ダイアログのウィンドウ表示はテスト実行環境 (画面を持たない JVM) では完走しないため、
 * 器そのものは実物を組み立てたうえで、重なりだけをこの面で観察する。
 * 実装との対応は次のとおり:
 *
 * - `present` は Activity を提示先とする表示に対応し、器を重なりの一番上へ積む
 * - handle の `onDelivery` は結果の配送に対応する。器はウィンドウに載らないと出入りの演出を
 *   進められないため、この面は撤去が即座に済んだものとして確定した結果をそのまま配送する。
 *   配送を退出の演出・撤去の後に置く契約は、実ウィンドウを使う instrumented テストが受け持つ
 * - [detachExternally] は器1枚が画面から外れる経路に対応し、その器だけを重なりから外して器へ伝える
 *   (ダイアログ1枚が1つのウィンドウなので、下の1枚が消えても上の1枚は残る)
 */
internal class DialogTestPresentationSurface : DialogPresentationSurface {
    private val lock = Any()
    private val containers = mutableListOf<DialogContainer>()
    private val requestedPlacements = mutableListOf<DialogPlacement?>()

    /** 提示先の画面が持つ Context 相当。 */
    val context: Context = ContextWrapper(null)

    /** 提示先が存在するか。false にすると提示先不在の状況を再現できる。 */
    @Volatile
    var isPresentationHostAvailable: Boolean = true

    override val canPresent: Boolean
        get() = isPresentationHostAvailable

    /** 下から順に並んだ、提示中のダイアログの器。 */
    val presentedContainers: List<DialogContainer>
        get() = synchronized(lock) { containers.toList() }

    /** 一番手前のダイアログの器。 */
    val topmostContainer: DialogContainer?
        get() = presentedContainers.lastOrNull()

    /** 提示を求められたときに渡された置き場所を、提示順に並べたもの。 */
    val presentedPlacements: List<DialogPlacement?>
        get() = synchronized(lock) { requestedPlacements.toList() }

    override fun present(request: DialogPresentationRequest): PresentedDialog {
        val container = DialogContainer(
            context = context,
            contentView = request.createContentView(context),
            resultChannel = request.resultChannel,
            placement = request.placement,
        )
        synchronized(lock) {
            containers.add(container)
            requestedPlacements.add(request.placement)
        }
        return PresentedDialog { handler ->
            request.resultChannel.onSettle { outcome ->
                detach(container)
                handler(outcome)
            }
        }
    }

    /**
     * ライブラリの外の要因 (画面の破棄など) で器が画面から外れる状況を再現する。
     * show からの閉鎖を経ずに器だけが消える経路。
     */
    fun detachExternally(container: DialogContainer) {
        detach(container)
    }

    private fun detach(container: DialogContainer) {
        val wasPresented = synchronized(lock) { containers.remove(container) }
        if (wasPresented) {
            container.reportDetached()
        }
    }
}
