package jp.kamusoft.ksdialogs

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * show 1回分の提示処理。
 *
 * 型消去された結果でやり取りし、宣言結果型への復元は呼び出し側が行う。
 */
internal object DialogPresenter {
    /**
     * ViewModel の型で factory を解決し、生成した View をダイアログとして提示して結果を待つ。
     *
     * 呼び出しは任意のスレッドから行え、提示と閉鎖は UI スレッドで実行する。
     * 未登録は提示先の有無にかかわらず待たずに [DialogException] を投げ、View の生成・表示も行わない。
     * 提示先が無ければ失敗せず、提示先の出現を待ってから View を作る (core/ADR-0039)。
     * [placement] は show の引数で渡された配置で、null なら中身の View への添付が使われる。
     */
    suspend fun present(
        viewModel: DialogViewModel<*>,
        registry: DialogViewRegistry,
        presentationSurface: DialogPresentationSurface,
        placement: DialogPlacement? = null,
    ): DialogOutcome = withContext(Dispatchers.Main.immediate) {
        presentWithFactory(viewModel, presentationSurface, placement) {
            val viewModelClass = viewModel::class
            registry.factory(viewModelClass)
                ?: throw DialogException.ViewFactoryNotRegistered(viewModelClass.viewModelTypeName)
        }
    }

    /**
     * 渡された factory をその場で使ってダイアログとして提示し、結果を待つ。
     *
     * レジストリは参照も更新もしないため、この提示は既存の登録に干渉しない (core/ADR-0013)。
     * 提示先が無ければ失敗せず、提示先の出現を待ってから View を作る (core/ADR-0039)。
     */
    suspend fun present(
        viewModel: DialogViewModel<*>,
        factory: DialogViewFactory,
        presentationSurface: DialogPresentationSurface,
        placement: DialogPlacement? = null,
    ): DialogOutcome = withContext(Dispatchers.Main.immediate) {
        presentWithFactory(viewModel, presentationSurface, placement) { factory }
    }

    /**
     * 器を出して結果が確定するまで待つ、全 show 経路の共通入口。UI スレッドで呼ぶ。
     *
     * 参照型限定の強制 (core/ADR-0018) はここで行うため、factory の解決方式 (レジストリ / インライン /
     * 型指定) によらず value class の ViewModel は factory 解決・紐付け・提示先の待ちのどれよりも先に弾かれる。
     *
     * 提示先が無ければ、結果報告口を紐付けたうえで提示先の出現を待つ。待ちは、提示先が現れて列の先頭に
     * 来たとき・呼び出し元が打ち切ったとき・待っている間に結果が確定したときのどれかで明ける。
     * 打ち切りと確定では View を作らずに終え、打ち切りはキャンセルとして呼び出し元へ伝播する。
     *
     * @param resolveFactory 中身の生成に使う factory を返す関数。未登録は [DialogException] を投げる
     */
    private suspend fun presentWithFactory(
        viewModel: DialogViewModel<*>,
        presentationSurface: DialogPresentationSurface,
        placement: DialogPlacement?,
        resolveFactory: () -> DialogViewFactory,
    ): DialogOutcome {
        viewModel::class.requireReferenceTypeViewModel()
        val factory = resolveFactory()

        val resultChannel = DialogResultChannel()

        // 結果報告口の紐付けは中身の生成より前に済ませ、factory 本体からも `viewModel.notifier` が
        // 読めるようにする (core/ADR-0018)。
        // 同じインスタンスが既に表示中 (提示先を待っている間を含む) なら、結果に化けさせずに構成ミスとして失敗する
        if (!DialogNotifierBindings.bind(viewModel, resultChannel)) {
            throw DialogException.ViewModelAlreadyShowing(viewModel::class.viewModelTypeName)
        }
        try {
            // 待ちは紐付けの後に置く。待っている間も同じインスタンスの再 show は「表示中」として失敗し、
            // 表示の前に VM が報告した結果もこの show の結果として受け取れる
            val turn = presentationSurface.hostWaitQueue.waitForTurn(presentationSurface, resultChannel)
            if (turn !is DialogHostWaitTurn.Ready) {
                // 表示しないまま結果が確定した
                currentCoroutineContext().ensureActive()
                return resultChannel.settledOutcome ?: DialogOutcome.Cancelled
            }
            val slot = turn.slot
            try {
                // 明けた直後に呼び出し元が打ち切っていれば、中身を作らずに終える
                currentCoroutineContext().ensureActive()
                val presented = presentationSurface.present(
                    DialogPresentationRequest(
                        createContentView = { context -> factory.createView(context, viewModel, resultChannel) },
                        resultChannel = resultChannel,
                        placement = placement,
                    ),
                )
                // 器のウィンドウを追加し終えたので、列で待っている次の 1 枚を明ける
                slot?.finish()
                return suspendCancellableCoroutine { continuation ->
                    // 配送は退出の演出・覆いの消滅・器の撤去がすべて済んだあとに器から届く (core/ADR-0017)
                    presented.onDelivery { outcome -> continuation.resume(outcome) }
                    continuation.invokeOnCancellation {
                        // 呼び出し元が待つのをやめたらダイアログを残さない。
                        // 未確定ならキャンセルとして確定し、退出中なら演出を待たずに撤去へ進む
                        resultChannel.cancelFromCaller()
                    }
                }
            } finally {
                // 提示に進まずに終わる経路 (中身の生成失敗・打ち切り) でも、列の次の 1 枚を止めない
                slot?.finish()
            }
        } finally {
            // 紐付け後に show が終わる全経路 (正常配送・待ちの間の確定・中身の生成失敗・提示の失敗・
            // 呼び出し元キャンセル・器消失) で外す。呼び出し元へ結果や例外が渡るのはこの除去のあと
            DialogNotifierBindings.unbind(viewModel, resultChannel)
        }
    }
}
