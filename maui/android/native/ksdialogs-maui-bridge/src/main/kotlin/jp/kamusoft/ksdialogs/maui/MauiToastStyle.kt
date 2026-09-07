package jp.kamusoft.ksdialogs.maui

import jp.kamusoft.ksdialogs.ToastStyle

/**
 * MAUI 側で設定された Toast の一括設定を運ぶ入れ物。
 *
 * この面は見えを作らず、受け取った値をそのまま Native ライブラリの設定へ渡す (core/ADR-0001)。
 * 色は境界を越えられる形として ARGB 32bit 整数で受け取る。
 * 既定値は書き写さず Native ライブラリの既定値から引く。何も設定しないまま渡せば
 * スタイルを設定しない場合と同じ見え方になり、既定値がずれる余地も残らない。
 */
public class MauiToastStyle {
    /** デフォルト View のピルの地色 (ARGB 32bit)。 */
    public var backgroundColorArgb: Int = CONTRACT_DEFAULTS.backgroundColor

    /** デフォルト View のメッセージの文字色 (ARGB 32bit)。 */
    public var textColorArgb: Int = CONTRACT_DEFAULTS.textColor

    /** デフォルト View のメッセージの文字の大きさ (論理単位 sp)。 */
    public var fontSize: Double = CONTRACT_DEFAULTS.fontSize

    /** デフォルト View のピルの角丸半径 (論理単位 dp)。 */
    public var cornerRadius: Double = CONTRACT_DEFAULTS.cornerRadius

    /** duration を省略した表示に使うミリ秒。 */
    public var defaultDuration: Int = CONTRACT_DEFAULTS.defaultDuration

    /** アプリ全体の既定配置。null なら Toast の契約既定値が使われる。 */
    public var defaultPlacement: MauiDialogPlacement? = null

    private companion object {
        /** Native ライブラリが定める既定値。各項目の初期値はここから引く。 */
        val CONTRACT_DEFAULTS = ToastStyle()
    }
}

/** 互換面の一括設定を Native ライブラリの型へ写す。 */
@JvmSynthetic
internal fun MauiToastStyle.toToastStyle(): ToastStyle = ToastStyle(
    backgroundColor = backgroundColorArgb,
    textColor = textColorArgb,
    fontSize = fontSize,
    cornerRadius = cornerRadius,
    defaultDuration = defaultDuration,
    defaultPlacement = defaultPlacement?.toDialogPlacement(),
)
