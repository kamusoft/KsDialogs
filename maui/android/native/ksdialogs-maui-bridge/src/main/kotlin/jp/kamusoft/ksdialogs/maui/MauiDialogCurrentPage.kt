package jp.kamusoft.ksdialogs.maui

import android.view.View
import jp.kamusoft.ksdialogs.DialogCurrentPage

/**
 * 表示中のページの View を返す口の形。
 *
 * 表示中のページは MAUI 側がページ構成を辿って決め、その platform view を返す。
 */
public fun interface MauiDialogCurrentPageProvider {
    /**
     * 表示中のページの View を返す。UI スレッドから呼ばれる。
     *
     * @return 表示中のページの View。見つからなければ null
     */
    public fun currentPageView(): View?
}

/**
 * 基準領域「表示中のページ」のページを MAUI 側から教える口 (互換面の表現)。
 *
 * 受け取った関数をそのまま Native ライブラリの登録口 ([DialogCurrentPage.provider]) へ渡すだけで、
 * ページの選び方にも矩形の求め方にも関与しない。
 */
public object MauiDialogCurrentPage {
    /**
     * 表示中のページの View を返す関数を登録する。null を渡すと登録を解除する。
     *
     * Native ライブラリは表示の開始時に登録内容を捕まえるため、差し替えは次の表示から効く。
     *
     * @param provider 表示中のページの View を返す関数
     */
    @JvmStatic
    public fun setProvider(provider: MauiDialogCurrentPageProvider?) {
        DialogCurrentPage.provider = provider?.let { registered -> { registered.currentPageView() } }
    }
}
