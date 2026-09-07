package jp.kamusoft.ksdialogs.samples.android

import android.app.Application

/** Sample アプリのエントリポイント。ダイアログの登録を起動時に済ませる。 */
internal class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SampleDialogRegistration.register()
        SampleLoadingRegistration.register()
        SampleToastRegistration.register()
    }
}
