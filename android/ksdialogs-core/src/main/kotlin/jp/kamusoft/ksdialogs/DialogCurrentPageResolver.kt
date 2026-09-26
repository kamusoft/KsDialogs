package jp.kamusoft.ksdialogs

import android.view.View

/**
 * 表示中のページを、優先順に並べた取得元から決める。
 *
 * 優先順は「modifier の台帳 > 登録された関数」で、上位の取得元が候補を持たない・null を返す・
 * 例外を投げる・空の矩形を返すときは次へ進む (core/ADR-0038)。Android には既定の探し方は無い。
 * 1 回の表示の器は表示の開始時に作ったこの値を使い続けるので、登録の差し替えは次の表示から効く。
 *
 * @property sources 問い合わせる順に並べた取得元
 */
internal class DialogCurrentPageResolver(private val sources: List<DialogCurrentPageSource>) {

    /**
     * ダイアログを出す画面 (Activity) が持つウィンドウに属する表示中のページを決める。
     *
     * @param pageWindowRoot ダイアログを出す画面 (Activity) のメインウィンドウの根の View。
     *   画面に紐づかない提示で根が得られなければ null
     */
    fun resolve(pageWindowRoot: View?): DialogCurrentPageResolution {
        if (pageWindowRoot == null) {
            return DialogCurrentPageResolution.Unresolved(
                listOf(DialogCurrentPageLookup.NotFound("The dialog is not presented on an activity window.")),
            )
        }
        val misses = mutableListOf<DialogCurrentPageLookup.NotFound>()
        for (source in sources) {
            when (val lookup = source.lookUpPageRect(pageWindowRoot)) {
                is DialogCurrentPageLookup.Found -> return DialogCurrentPageResolution.Resolved(lookup.rect)
                is DialogCurrentPageLookup.NotFound -> misses += lookup
            }
        }
        return DialogCurrentPageResolution.Unresolved(misses)
    }

    companion object {
        /** 表示の開始時点の登録内容で組み立てる。 */
        fun capturingRegistration(
            ledger: DialogCurrentPageSource = DialogCurrentPageMarkerLedger.shared,
        ): DialogCurrentPageResolver {
            val registered = DialogCurrentPage.provider
                ?.let { DialogRegisteredCurrentPageSource(it) }
                ?: DialogUnregisteredCurrentPageSource
            return DialogCurrentPageResolver(listOf(ledger, registered))
        }
    }
}

/** 取得元を優先順に辿った結果。 */
internal sealed interface DialogCurrentPageResolution {
    /** ページの基準矩形 (画面座標、px)。 */
    data class Resolved(val rect: DialogScreenRect) : DialogCurrentPageResolution

    /**
     * どの取得元からもページが得られなかった。
     *
     * @property misses 辿った順の、見つからなかった理由
     */
    data class Unresolved(val misses: List<DialogCurrentPageLookup.NotFound>) : DialogCurrentPageResolution {

        /** 診断ログの本文。先頭の文は iOS 版と同じ文にしてある。 */
        val diagnosticMessage: String
            get() = (listOf(DIAGNOSTIC_LEAD) + misses.map { it.reason }).joinToString(" ")

        /** 取得元が投げた例外のうち最初のもの。ログに添える。 */
        val cause: Throwable?
            get() = misses.firstNotNullOfOrNull { it.cause }
    }

    companion object {
        /** 未解決のときの診断ログの書き出し。 */
        const val DIAGNOSTIC_LEAD: String = "The current page could not be resolved, so the visible area is used instead."
    }
}
