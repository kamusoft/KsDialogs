package jp.kamusoft.ksdialogs.maui

import jp.kamusoft.ksdialogs.LoadingStyle

/**
 * MAUI 側が設定した進捗テキストの組み立て方。
 *
 * 進捗が未報告のときは [progress] が null になる。Java 互換面では Kotlin の関数型を
 * そのまま表せないため、1 メソッドの面として渡す。
 */
public fun interface MauiLoadingProgressFormat {
    /**
     * メッセージと進捗から表示テキストを組み立てる。UI スレッドから呼ばれる。
     *
     * @param message 表示中のメッセージ。未指定なら null
     * @param progress 0〜1 に丸めた後の進捗値。未報告なら null
     * @return 既定ローディングに表示するテキスト
     */
    public fun format(message: String?, progress: Double?): String
}

/**
 * MAUI 側で設定された既定ローディングの見た目を運ぶ入れ物。
 *
 * この面は見えを作らず、受け取った値をそのまま Native ライブラリの設定へ渡す (core/ADR-0001)。
 * 色は境界を越えられる形として ARGB 32bit 整数で受け取る。
 * 既定値は書き写さず Native ライブラリの既定値から引く。何も設定しないまま渡せば
 * スタイルを設定しない場合と同じ見え方になり、既定値がずれる余地も残らない。
 */
public class MauiLoadingStyle {
    /** 回転インジケータの色 (ARGB 32bit)。 */
    public var indicatorColorArgb: Int = CONTRACT_DEFAULTS.indicatorColor

    /** メッセージの文字の大きさ (論理単位 sp)。 */
    public var messageFontSize: Double = CONTRACT_DEFAULTS.messageFontSize

    /** メッセージの文字色 (ARGB 32bit)。 */
    public var messageColorArgb: Int = CONTRACT_DEFAULTS.messageColor

    /** メッセージを省略して表示したときに使う文言。null ならメッセージなしで表示する。 */
    public var defaultMessage: String? = CONTRACT_DEFAULTS.defaultMessage

    /** 表示テキストの組み立て方。null なら Native ライブラリの既定の組み立て方が使われる。 */
    public var progressFormat: MauiLoadingProgressFormat? = null

    private companion object {
        /** Native ライブラリが定める既定値。各項目の初期値はここから引く。 */
        val CONTRACT_DEFAULTS = LoadingStyle()
    }
}

/** 互換面のスタイルを Native ライブラリの型へ写す。 */
@JvmSynthetic
internal fun MauiLoadingStyle.toLoadingStyle(): LoadingStyle {
    val format = progressFormat
    return LoadingStyle(
        indicatorColor = indicatorColorArgb,
        messageFontSize = messageFontSize,
        messageColor = messageColorArgb,
        defaultMessage = defaultMessage,
        progressFormat = if (format == null) {
            LoadingStyle.DEFAULT_PROGRESS_FORMAT
        } else {
            { message, progress -> format.format(message, progress) }
        },
    )
}
