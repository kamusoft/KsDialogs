package jp.kamusoft.ksdialogs

import android.os.IBinder
import android.view.View
import android.view.WindowManager

/**
 * 表示中のページの矩形を、画面座標 (px) で返す取得元。
 *
 * 利用者に見せる登録口 (Compose の modifier・View を返す関数) はすべてこの形へ揃えてから問い合わせる。
 * 矩形を直接返す形は公開しない (core/ADR-0038)。問い合わせは UI スレッドで行う。
 */
internal fun interface DialogCurrentPageSource {
    /**
     * ダイアログを出す画面 (Activity) が持つウィンドウに属するページを探す。
     *
     * 候補になるのは Activity のメインウィンドウと、同じ Activity に属するモーダル・ダイアログのウィンドウに
     * 載ったもの ([DialogCurrentPageGeometry.isInPresentingActivity])。別の Activity のウィンドウ・
     * KsDialogs 自身の器のウィンドウに属するもの、メインウィンドウと重ならないもの、空の矩形は
     * 見つからなかった扱いにする。
     *
     * @param pageWindowRoot ダイアログを出す画面 (Activity) のメインウィンドウの根の View
     */
    fun lookUpPageRect(pageWindowRoot: View): DialogCurrentPageLookup
}

/** 1 つの取得元に表示中のページを問い合わせた結果。 */
internal sealed interface DialogCurrentPageLookup {
    /** ページの基準矩形 (画面座標、px)。 */
    data class Found(val rect: DialogScreenRect) : DialogCurrentPageLookup

    /**
     * この取得元からはページが得られなかった。
     *
     * @property reason 診断ログにそのまま載る英語の 1 文
     * @property cause 取得元が投げた例外。例外でなければ null
     */
    data class NotFound(val reason: String, val cause: Throwable? = null) : DialogCurrentPageLookup
}

/**
 * 画面座標 (px) の矩形。原点は画面の左上。
 *
 * ページの矩形と器の位置は別々のウィンドウに属するため、両者を突き合わせるときの共通の座標にする。
 */
internal data class DialogScreenRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    /** 幅か高さが 0 以下 (または非有限) か。 */
    val isEmpty: Boolean
        get() = !(right - left > 0f && bottom - top > 0f)

    /** 他方の矩形を丸ごと含むか。 */
    fun contains(other: DialogScreenRect): Boolean =
        left <= other.left && top <= other.top && right >= other.right && bottom >= other.bottom

    /** 他方の矩形と面積を持って重なるか。 */
    fun intersects(other: DialogScreenRect): Boolean =
        left < other.right && other.left < right && top < other.bottom && other.top < bottom

    /** 平行移動した矩形。 */
    fun offset(dx: Float, dy: Float): DialogScreenRect =
        DialogScreenRect(left + dx, top + dy, right + dx, bottom + dy)
}

/** View の位置と大きさを画面座標で読む。 */
internal object DialogCurrentPageGeometry {

    /** View の矩形を画面座標で返す。 */
    fun screenRect(view: View): DialogScreenRect {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val left = location[0].toFloat()
        val top = location[1].toFloat()
        return DialogScreenRect(left, top, left + view.width, top + view.height)
    }

    /**
     * View が、ダイアログを出す画面 (Activity) の持つウィンドウに載っているか。
     *
     * 候補にするのは Activity のメインウィンドウと、同じ Activity に属する別のウィンドウ
     * (モーダル・ダイアログ)。MAUI の Android 版のようにモーダルのページを Activity とは別の
     * ダイアログウィンドウに載せる構成でも、そのページを候補にするため。
     *
     * 同じ Activity に属するかは、ウィンドウの根が持つ配置情報の token で判定する。アプリ側の
     * ウィンドウ (メインウィンドウ・Activity の Context で出したダイアログ) は、OS が付ける
     * Activity の token を共有するため。Context から Activity を辿る方法は採らない: メインウィンドウの根の
     * Context は Activity を辿れないことがあり、ダイアログの中身を別の Context で作ることもできるため。
     * パネル (PopupWindow など) は親ウィンドウの token を持つため候補にならない。
     *
     * KsDialogs 自身の器のウィンドウは、同じ Activity に属していても候補にしない。
     *
     * @param view 判定する View
     * @param pageWindowRoot ダイアログを出す画面のメインウィンドウの根の View
     */
    fun isInPresentingActivity(view: View, pageWindowRoot: View): Boolean {
        if (!view.isAttachedToWindow) {
            return false
        }
        val root = view.rootView
        if (root === pageWindowRoot) {
            return true
        }
        if (root.isKsDialogsContainerWindowRoot) {
            return false
        }
        val activityToken = pageWindowRoot.windowOwnerToken ?: return false
        return root.windowOwnerToken === activityToken
    }

    /** ウィンドウの根の View が持つ、ウィンドウの所有者の token。ウィンドウの根でなければ null。 */
    private val View.windowOwnerToken: IBinder?
        get() = (layoutParams as? WindowManager.LayoutParams)?.token

    /**
     * 取得元が返した View から、ページの基準矩形を求める。
     *
     * @param view 取得元が返した View
     * @param pageWindowRoot ダイアログを出す画面のメインウィンドウの根の View
     * @param origin 見つからなかった理由の文に入れる、取得元の呼び名
     */
    fun pageRect(view: View, pageWindowRoot: View, origin: String): DialogCurrentPageLookup {
        if (!isInPresentingActivity(view, pageWindowRoot)) {
            return DialogCurrentPageLookup.NotFound(
                "The view from $origin is not in a window of the activity presenting the dialog.",
            )
        }
        val rect = screenRect(view)
        if (rect.isEmpty) {
            return DialogCurrentPageLookup.NotFound("The view from $origin has an empty area.")
        }
        if (!rect.intersects(screenRect(pageWindowRoot))) {
            return DialogCurrentPageLookup.NotFound(
                "The view from $origin lies outside the window presenting the dialog.",
            )
        }
        return DialogCurrentPageLookup.Found(rect)
    }
}

/**
 * アプリが [DialogCurrentPage.provider] に登録した関数を取得元にする。
 *
 * 関数は表示の開始時に捕まえたものを使い続けるため、表示中に登録を差し替えても
 * そのダイアログの基準は変わらない。
 */
internal class DialogRegisteredCurrentPageSource(
    private val provider: () -> View?,
) : DialogCurrentPageSource {

    override fun lookUpPageRect(pageWindowRoot: View): DialogCurrentPageLookup {
        val view = try {
            provider()
        } catch (failure: Exception) {
            // 利用者の関数の失敗で表示を止めない。理由を添えて次の取得元へ進む
            return DialogCurrentPageLookup.NotFound(
                "The registered current page provider threw an exception.",
                cause = failure,
            )
        } ?: return DialogCurrentPageLookup.NotFound("The registered current page provider returned null.")
        return DialogCurrentPageGeometry.pageRect(view, pageWindowRoot, ORIGIN)
    }

    private companion object {
        const val ORIGIN = "the registered current page provider"
    }
}

/** 関数が登録されていないことを、見つからなかった理由として返す取得元。 */
internal object DialogUnregisteredCurrentPageSource : DialogCurrentPageSource {
    override fun lookUpPageRect(pageWindowRoot: View): DialogCurrentPageLookup =
        DialogCurrentPageLookup.NotFound("No current page provider is registered.")
}
