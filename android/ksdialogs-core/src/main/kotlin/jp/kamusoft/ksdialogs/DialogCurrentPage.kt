package jp.kamusoft.ksdialogs

import android.view.View

/**
 * 基準領域 [DialogLayoutArea.CURRENT_PAGE] が使う「表示中のページ」をアプリから教える口。
 *
 * Android には表示中のページを表す OS 共通の仕組みが無いため、ライブラリは自分ではページを探さない。
 * 従来の View で画面を組んでいるアプリは、表示中のページの View を返す関数をここに一度登録する。
 * Compose で組んだ画面では、各画面の中身の枠に `Modifier.markAsDialogCurrentPage()` を付ける方法を使う
 * (付けた composable が配置されている間は、そちらが登録した関数より優先される)。
 *
 * 登録した関数は UI スレッドで、各表示の開始時と、表示中にウィンドウの寸法やシステムバーの幅が
 * 変わったときに呼ばれる。登録の差し替えは次の表示から効き、表示中のダイアログには影響しない。
 */
public object DialogCurrentPage {
    /**
     * 表示中のページの View を返す関数。`null` を代入すると登録を解除する。
     *
     * 基準になるのは返した View の矩形のうち、システムバーを除いた可視領域と重なる部分。
     * 返す View は、ダイアログを出す画面 (Activity) のウィンドウか、同じ Activity で出したモーダル・
     * ダイアログのウィンドウに載っていればよい (KsDialogs 自身のダイアログ・Loading・Toast の中は除く)。
     * 関数が `null` を返したとき・例外を投げたとき・返した View がそれらのウィンドウに載っていないとき・
     * 矩形が空のとき・矩形がダイアログを出す画面のメインウィンドウと重ならないときは、
     * ページが得られなかった扱いになり、
     * 基準は [DialogLayoutArea.VISIBLE_AREA] と同じになる (その理由は警告ログに出る)。
     */
    @Volatile
    @JvmStatic
    public var provider: (() -> View?)? = null
}
