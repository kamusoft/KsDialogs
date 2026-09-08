package jp.kamusoft.ksdialogs

/**
 * Loading の設定プロパティ (スタイルと既定ローディングの器メタ属性) の置き場。
 *
 * 設定は任意のスレッドから読み書きでき、器は各表示の開始時にここから読む
 * (core/ADR-0023 の「設定変更は次の表示から効く」)。
 */
internal class LoadingSettings {
    private val lock = Any()

    private var storedStyle: LoadingStyle = LoadingStyle()
    private var storedOptions: DialogOptions = DialogOptions()

    /** 既定ローディングの見た目。 */
    var style: LoadingStyle
        get() = synchronized(lock) { storedStyle }
        set(value) {
            synchronized(lock) { storedStyle = value }
        }

    /** 既定ローディングの器メタ属性。 */
    var options: DialogOptions
        get() = synchronized(lock) { storedOptions }
        set(value) {
            synchronized(lock) { storedOptions = value }
        }
}
