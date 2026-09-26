package jp.kamusoft.ksdialogs.apicheck

import android.view.View
import androidx.compose.ui.Modifier
import jp.kamusoft.ksdialogs.DialogCurrentPage
import jp.kamusoft.ksdialogs.DialogLayoutArea
import jp.kamusoft.ksdialogs.DialogOptions
import jp.kamusoft.ksdialogs.compose.ksDialogCurrentPage
import jp.kamusoft.ksdialogs.ksDialogOptions

/**
 * 基準領域「表示中のページ」の公開 API 形状の正の検証。
 *
 * このファイルがコンパイルできることが検証結果で、基準領域の値・ページの View を返す関数の登録口・
 * Compose の modifier のどれかが利用者から見えなくなればビルドが失敗する。
 */
public object DialogCurrentPageApiSurfaceChecks {

    /** 基準領域に表示中のページを選んで添付できる。 */
    public fun acceptsCurrentPageLayoutArea(contentView: View) {
        contentView.ksDialogOptions = DialogOptions(layoutArea = DialogLayoutArea.CURRENT_PAGE)
    }

    /** ページの View を返す関数を登録でき、null を代入して解除できる。 */
    public fun acceptsCurrentPageProviderRegistration(pageView: View) {
        DialogCurrentPage.provider = { pageView }
        val registered: (() -> View?)? = DialogCurrentPage.provider
        registered?.invoke()
        DialogCurrentPage.provider = null
    }

    /** 関数は null を返してよい (表示中のページが無い状態を表す)。 */
    public fun acceptsProviderReturningNull() {
        DialogCurrentPage.provider = { null }
    }

    /**
     * Compose の画面は中身の枠に modifier を付けて表示中のページを名乗らせる。
     *
     * modifier は利用者の他の modifier と連ねられる。戻り値の型 (`Modifier`) が利用者のコンパイル時の
     * 依存に届いていることも、この連結がコンパイルできることで示される。
     */
    public fun acceptsCurrentPageModifier(frame: Modifier): Modifier =
        frame.ksDialogCurrentPage().then(Modifier.ksDialogCurrentPage())
}
