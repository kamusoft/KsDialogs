package jp.kamusoft.ksdialogs

/**
 * KsDialogs のライブラリ内部でモジュールをまたいで使う宣言の印。
 *
 * 利用者向けの API ではなく、予告なく変更・削除される。アプリのコードからは使わないこと。
 */
@RequiresOptIn(
    message = "This is an internal KsDialogs API and is not intended for use by applications.",
    level = RequiresOptIn.Level.ERROR,
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
public annotation class KsDialogsInternalApi
