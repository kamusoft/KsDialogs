package jp.kamusoft.ksdialogs.maui

/** カスタム Loading の中身と、それに効くメタ属性を新規に供給するもの。 */
public fun interface MauiLoadingContentProvider {
    /** 中身と属性を新規に供給する。UI スレッドから呼ばれる。 */
    public fun createContent(): MauiDialogContent
}

/** MAUI 側の ViewModel へ進捗を届ける口。 */
public fun interface MauiLoadingProgressReceiver {
    /**
     * 進捗の報告を受け取る。UI スレッドから呼ばれる。
     *
     * @param progress 0〜1 に丸めた後の進捗値
     */
    public fun onProgress(progress: Double)
}

/**
 * 表示 1 回分の中身の指定。
 *
 * 既定ローディング (ライブラリ同梱の内蔵コンテンツ) では中身の供給がなく、メッセージと置き場所だけを
 * 運ぶ。カスタム Loading では MAUI 側が中身とメタ属性を供給し、進捗の転送先も一緒に渡す。
 *
 * @property provider カスタム Loading の中身の供給元。既定ローディングでは null
 * @property message 既定ローディングに表示するメッセージ。未指定なら null
 * @property placement 表示 API の引数で渡された置き場所。未指定なら null
 * @property progressReceiver 進捗の転送先。転送しないなら null
 */
public class MauiLoadingContent private constructor(
    internal val provider: MauiLoadingContentProvider?,
    internal val message: String?,
    internal val placement: MauiDialogPlacement?,
    internal val progressReceiver: MauiLoadingProgressReceiver?,
) {
    /**
     * 既定ローディングの表示を指定する。
     *
     * @param message 表示するメッセージ。null ならスタイルの既定メッセージ
     * @param placement 置き場所。null なら契約の既定値
     */
    public constructor(message: String?, placement: MauiDialogPlacement?) :
        this(provider = null, message = message, placement = placement, progressReceiver = null)

    /**
     * カスタム Loading の表示を指定する。
     *
     * @param provider 中身とメタ属性の供給元
     * @param placement 置き場所。null なら中身への添付が使われる
     * @param progressReceiver 進捗の転送先。転送しないなら null
     */
    public constructor(
        provider: MauiLoadingContentProvider,
        placement: MauiDialogPlacement?,
        progressReceiver: MauiLoadingProgressReceiver?,
    ) : this(
        provider = provider,
        message = null,
        placement = placement,
        progressReceiver = progressReceiver,
    )
}
