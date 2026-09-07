package jp.kamusoft.ksdialogs.maui

/** カスタム Toast の中身と、それに効くメタ属性を新規に供給するもの。 */
public fun interface MauiToastContentProvider {
    /**
     * 中身と属性を新規に供給する。UI スレッドから呼ばれる。
     *
     * 供給元 (MAUI 側) は managed / native の境界を跨いで呼ばれるため、失敗を例外のまま返さず
     * 中身なし (null) として返す。失敗の詳細は MAUI 側が警告として残している。
     *
     * @return 供給された中身。作れなかったときは null
     */
    public fun createContent(): MauiDialogContent?
}

/**
 * 表示 1 回分の中身の指定。
 *
 * デフォルト View (ライブラリ同梱の内蔵コンテンツ) では中身の供給がなく、メッセージと duration・
 * 置き場所だけを運ぶ。カスタム Toast では MAUI 側が中身とメタ属性を供給する。
 *
 * @property provider カスタム Toast の中身の供給元。デフォルト View では null
 * @property message デフォルト View に表示するメッセージ。カスタム Toast では null
 * @property durationMs 表示するミリ秒。未指定なら null
 * @property placement 表示 API の引数で渡された置き場所。未指定なら null
 */
public class MauiToastContent private constructor(
    internal val provider: MauiToastContentProvider?,
    internal val message: String?,
    internal val durationMs: Int?,
    internal val placement: MauiDialogPlacement?,
) {
    /**
     * デフォルト View の表示を指定する。
     *
     * @param message 表示する文言
     * @param durationMs 表示するミリ秒。null なら一括設定の既定 duration
     * @param placement 置き場所。null なら一括設定のアプリ既定配置、それも無ければ契約の既定値
     */
    public constructor(message: String, durationMs: Int?, placement: MauiDialogPlacement?) :
        this(provider = null, message = message, durationMs = durationMs, placement = placement)

    /**
     * カスタム Toast の表示を指定する。
     *
     * @param provider 中身とメタ属性の供給元
     * @param durationMs 表示するミリ秒。null なら一括設定の既定 duration
     * @param placement 置き場所。null なら中身への添付が使われる
     */
    public constructor(
        provider: MauiToastContentProvider,
        durationMs: Int?,
        placement: MauiDialogPlacement?,
    ) : this(provider = provider, message = null, durationMs = durationMs, placement = placement)
}
