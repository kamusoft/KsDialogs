package jp.kamusoft.ksdialogs.maui

import android.view.View
import jp.kamusoft.ksdialogs.DialogTransition
import jp.kamusoft.ksdialogs.ksDialogOptions
import jp.kamusoft.ksdialogs.ksDialogPlacement
import jp.kamusoft.ksdialogs.ksDialogTransition
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * 提示1回分の中身と、その中身に効くメタ属性。
 *
 * MAUI 側が読んだメタ属性を受け取り、Native ライブラリの添付面へ写して渡す。
 *
 * @property view ダイアログの中身になる View
 * @property options その中身に添付された静的メタ属性
 * @property placement その提示の置き場所 (MAUI 側で合成済みの実効値)
 */
public class MauiDialogContent(
    public val view: View,
    public val options: MauiDialogOptions,
    public val placement: MauiDialogPlacement,
) {
    /** MAUI 側が供給した出現の演出。未供給なら Native ライブラリの既定が使われる。 */
    private var presentationRunner: MauiDialogTransitionRunner? = null

    /** MAUI 側が供給した退出の演出。未供給なら Native ライブラリの既定が使われる。 */
    private var dismissalRunner: MauiDialogTransitionRunner? = null

    /** MAUI 側が指定した覆いのフェード時間。未指定なら Native ライブラリの既定値が使われる。 */
    private var overlayDuration: Duration? = null

    /**
     * MAUI 側の演出をこの中身へ結び付ける (core/ADR-0017)。
     *
     * 供給された側だけが Native ライブラリの添付面に載り、未供給の側には器の既定が適用される。
     * 実行の実体はこの中身が保持し、この中身は添付を通じて中身の View から辿れるため、
     * 供給元はダイアログの寿命の間ずっと生きていることになる。
     * 背景の覆いは Native ライブラリの器が駆動するため、その時間だけを値として受け取る。
     *
     * @param presentation 出現の演出の実行口。null なら器の既定が使われる
     * @param dismissal 退出の演出の実行口。null なら器の既定が使われる
     * @param overlayDurationMillis 背景の覆いのフェード時間 (ミリ秒)。null なら器の既定値が使われる
     */
    public fun installTransition(
        presentation: MauiDialogTransitionRunner?,
        dismissal: MauiDialogTransitionRunner?,
        overlayDurationMillis: Long?,
    ) {
        presentationRunner = presentation
        dismissalRunner = dismissal
        overlayDuration = overlayDurationMillis?.milliseconds
        view.ksDialogTransition = composeTransition()
    }

    /**
     * 供給された実行口と覆いの時間を、Native ライブラリの演出の組へ写す。
     *
     * どれも供給されていなければ、演出を持たない (器の既定がそのまま使われる)。
     *
     * @return 添付面へ載せる演出の組。供給が何も無ければ null
     */
    internal fun composeTransition(): DialogTransition? {
        if (presentationRunner == null && dismissalRunner == null && overlayDuration == null) {
            return null
        }
        return DialogTransition(
            presentation = if (presentationRunner == null) {
                null
            } else {
                { hostView -> awaitRun(hostView, ::runPresentation) }
            },
            dismissal = if (dismissalRunner == null) {
                null
            } else {
                { hostView -> awaitRun(hostView, ::runDismissal) }
            },
            overlayDuration = overlayDuration,
        )
    }

    /**
     * 出現の演出を実行し、終わったら完了を1回だけ返す。
     * 演出が供給されていなければ、その場で完了を返す。
     *
     * @param hostView 演出の対象になるコンテンツのホスト View
     * @param completion 演出が終わったときに呼ばれる完了通知
     */
    public fun runPresentation(hostView: View, completion: Runnable) {
        run(presentationRunner, hostView, completion)
    }

    /**
     * 退出の演出を実行し、終わったら完了を1回だけ返す。
     * 演出が供給されていなければ、その場で完了を返す。
     *
     * @param hostView 演出の対象になるコンテンツのホスト View
     * @param completion 演出が終わったときに呼ばれる完了通知
     */
    public fun runDismissal(hostView: View, completion: Runnable) {
        run(dismissalRunner, hostView, completion)
    }

    /** 実行口を呼び、完了通知が何度届いても1回だけ通す。 */
    private fun run(
        runner: MauiDialogTransitionRunner?,
        hostView: View,
        completion: Runnable,
    ) {
        val once = MauiDialogSingleCompletion(completion)
        if (runner == null) {
            once.complete()
            return
        }
        runner.run(hostView) { once.complete() }
    }

    /**
     * 完了コールバック型の実行口を待つ。
     *
     * 待ちが打ち切られたら、演出の完了を待つのをやめて戻る (MAUI 側の処理は取り消せない)。
     */
    private suspend fun awaitRun(
        hostView: View,
        start: (View, Runnable) -> Unit,
    ): Unit = suspendCancellableCoroutine { continuation ->
        start(hostView) {
            if (continuation.isActive) {
                continuation.resume(Unit)
            }
        }
    }

    public companion object {
        /**
         * MAUI 側が読み直したメタ属性を、中身の View の添付面へ写す。
         *
         * 契約が定める採用時点は初回のネイティブレイアウトパス完了時点であり、
         * そこまでの添付変更は採用される (core/ADR-0015)。器がその時点の値を読めるよう、
         * MAUI 側はレイアウトパスの中でこの操作を呼んで添付を更新する。
         *
         * @param view 添付先の中身の View
         * @param options MAUI 側で読んだ静的メタ属性
         * @param placement MAUI 側で読んだ置き場所
         */
        @JvmStatic
        public fun applyAttributes(
            view: View,
            options: MauiDialogOptions,
            placement: MauiDialogPlacement,
        ) {
            view.ksDialogOptions = options.toDialogOptions()
            view.ksDialogPlacement = placement.toDialogPlacement()
        }
    }
}

/** ダイアログの中身と、それに効くメタ属性を新規に供給するもの。 */
public fun interface MauiDialogContentProvider {
    /**
     * 中身と属性を新規に供給する。UI スレッドから呼ばれる。
     *
     * 供給元 (MAUI 側) は managed / native の境界を跨いで呼ばれるため、失敗を例外のまま返さず
     * 中身なし (null) として返す。元の失敗は MAUI 側が呼び出し 1 回分だけ預かっている。
     *
     * @return 供給された中身。作れなかったときは null
     */
    public fun createContent(): MauiDialogContent?
}
