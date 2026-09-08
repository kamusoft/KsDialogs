package jp.kamusoft.ksdialogs

/**
 * Toast の設定プロパティ ([ToastStyle]) の置き場。
 *
 * 設定は任意のスレッドから読み書きでき、器は各表示の受理時にここから読む
 * (core/ADR-0032 の「設定変更は次の表示から効く」)。
 */
internal class ToastSettings {
    private val lock = Any()

    private var storedStyle: ToastStyle = ToastStyle()

    /** Toast の一括設定。 */
    var style: ToastStyle
        get() = synchronized(lock) { storedStyle }
        set(value) {
            synchronized(lock) { storedStyle = value }
        }
}
