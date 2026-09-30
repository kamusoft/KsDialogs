package jp.kamusoft.ksdialogs

import android.app.Activity
import android.content.Context

/**
 * 提示先でなくなった Activity に載っている器を、載せたまま残してよいかを供給する。
 *
 * 提示先 (core/ADR-0044) は新しく器を載せる先を決める規則で、既に付いている器を外すことまでは求めない。
 * 背面へ下がる・上に別の画面が開くなどで提示先でなくなっても、画面が破棄されていなければ器を付けたまま
 * にする。外すと、戻った画面の描画を待ってから載せ直すことになり、戻った直後の画面に器の無いコマが挟まる。
 */
internal interface AttachedHostRetention {
    /** [activity] に載っている器を残してよいか。破棄された画面では false。UI スレッドから呼ぶ。 */
    fun retainsAttachment(activity: Activity): Boolean
}

/**
 * 提示先の入れ替わりの通知を受けたとき、[attachedHost] に載っている器を外すかを決める。
 *
 * 外すのは、描画済みの別の提示先が現れたときと、提示先が無く、載っている画面を残せない (破棄された) ときに限る。
 * Loading と Toast が同じ規則で器を残すための共通の判定。
 *
 * @param attachedHost 器が載っている提示先。載っていなければ null
 * @param newHost 通知の時点の提示先。無ければ null
 * @param retainsAttachment 提示先が無い間も [attachedHost] に器を残してよいか
 */
internal fun shouldDetachAttachment(
    attachedHost: Context?,
    newHost: Context?,
    retainsAttachment: (Context) -> Boolean,
): Boolean = when {
    attachedHost == null || attachedHost === newHost -> false
    newHost != null -> true
    else -> !retainsAttachment(attachedHost)
}
