package jp.kamusoft.ksdialogs

import android.view.View

/**
 * この器 (Dialog / Loading / Toast) のウィンドウに、KsDialogs 自身の器である印を付ける。
 *
 * 器のウィンドウはダイアログを出す画面 (Activity) に属するため、印が無いと表示中のページの候補に
 * 紛れ込む。印はウィンドウの根の View に付け、候補を決めるときに見て外す。
 * 中身を載せた後 (ウィンドウの根が作られた後) に呼ぶ。
 */
internal fun android.app.Dialog.markAsKsDialogsContainerWindow() {
    window?.decorView?.setTag(R.id.ksdialogs_container_window, true)
}

/** この View が KsDialogs 自身の器のウィンドウの根か。 */
internal val View.isKsDialogsContainerWindowRoot: Boolean
    get() = getTag(R.id.ksdialogs_container_window) == true
