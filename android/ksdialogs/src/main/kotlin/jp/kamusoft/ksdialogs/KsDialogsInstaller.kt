package jp.kamusoft.ksdialogs

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

/**
 * アプリ起動時に Activity の追跡を始めるための仕掛け。
 *
 * ContentProvider はアプリのプロセス生成時に自動で生成されるため、
 * 利用者が初期化コードを書かなくても [ResumedActivityTracker] の購読が始まる。
 * データの入出力は行わない。
 */
internal class KsDialogsInstaller : ContentProvider() {
    override fun onCreate(): Boolean {
        (context?.applicationContext as? Application)?.let(ResumedActivityTracker.shared::install)
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0
}
