package jp.kamusoft.ksdialogs.samples.android

import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button

/**
 * 押せる [android.widget.TextView] をボタンとして読み上げさせる委譲。
 *
 * 文字や記号を並べただけの操作部は既定では「ボタン」と読み上げられないため、役割だけを補う。
 * 名前は各操作部の contentDescription が持ち、選択状態は View の選択状態がそのまま読み上げ情報に乗る。
 */
internal object SampleButtonRoleDelegate : View.AccessibilityDelegate() {
    override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(host, info)
        info.className = Button::class.java.name
    }
}
