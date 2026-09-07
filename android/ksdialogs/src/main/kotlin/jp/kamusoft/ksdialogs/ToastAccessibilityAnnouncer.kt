package jp.kamusoft.ksdialogs

import android.content.Context
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager

/**
 * 支援技術 (TalkBack) への通知口。
 *
 * デフォルト View の Toast は表示時にメッセージを読み上げへ流すが、フォーカスは移動させない。
 * フォーカスを動かすイベント (`TYPE_VIEW_ACCESSIBILITY_FOCUSED` /
 * `TYPE_WINDOW_STATE_CHANGED`) を使わないことがその保証であり、
 * この面を差し替えると発行したイベントの種類まで観察できる。
 */
internal fun interface ToastAccessibilityAnnouncer {
    /**
     * 通知を1件発行する。
     *
     * @param source 通知の発生元になる View
     * @param eventType 発行するアクセシビリティイベントの種類
     * @param text 読み上げへ流す文言
     */
    fun announce(source: View, eventType: Int, text: CharSequence)
}

/** OS の支援技術へそのまま流す既定の通知口。 */
internal class SystemToastAccessibilityAnnouncer : ToastAccessibilityAnnouncer {
    override fun announce(source: View, eventType: Int, text: CharSequence) {
        // 支援技術が動いていないときに発行すると OS が IllegalStateException を投げる
        // (AccessibilityManager.sendAccessibilityEvent の契約)。無効なら黙って捨てる
        if (!source.context.isAccessibilityEnabled()) {
            return
        }
        @Suppress("DEPRECATION")
        val event = AccessibilityEvent.obtain(eventType)
        event.className = source.javaClass.name
        event.packageName = source.context.packageName
        event.text.add(text)
        source.parent?.requestSendAccessibilityEvent(source, event)
    }

    private fun Context.isAccessibilityEnabled(): Boolean =
        (getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager)?.isEnabled == true
}
