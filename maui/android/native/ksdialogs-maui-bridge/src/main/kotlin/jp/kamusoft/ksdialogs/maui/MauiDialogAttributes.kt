package jp.kamusoft.ksdialogs.maui

import jp.kamusoft.ksdialogs.DialogAlignment
import jp.kamusoft.ksdialogs.DialogEdgeInsets
import jp.kamusoft.ksdialogs.DialogLayoutArea
import jp.kamusoft.ksdialogs.DialogOptions
import jp.kamusoft.ksdialogs.DialogPlacement

/** ダイアログを基準領域のどこへ寄せるか (互換面の表現)。 */
public enum class MauiDialogAlignment {
    /** 有効領域の前端 (左 / 上) に寄せる。 */
    START,

    /** 有効領域の中央に置く。 */
    CENTER,

    /** 有効領域の後端 (右 / 下) に寄せる。 */
    END,

    /** 位置だけでなくサイズも有効領域いっぱいに広げる。 */
    FILL,
}

/** サイズと位置の計算を行う基準領域 (互換面の表現)。 */
public enum class MauiDialogLayoutArea {
    /** ダイアログを載せるウィンドウの全体。 */
    WINDOW,

    /** ウィンドウからシステムバーなどが占める余白を控除した可視領域。 */
    VISIBLE_AREA,
}

/**
 * MAUI 側で中身に添付された静的メタ属性を運ぶ入れ物。
 *
 * この面はレイアウト計算を行わず、受け取った値をそのまま中身の View への添付として
 * Native ライブラリへ渡す (core/ADR-0001)。無効値の丸めは Native ライブラリの責務なので、
 * ここでは値を検査しない。
 * 色は境界を越えられる形として ARGB 32bit 整数で受け取る。
 * 既定値は Native ライブラリの既定値と同じで、何も設定しないまま渡せば
 * 属性を添付しない中身と同じ見え方になる。
 */
public class MauiDialogOptions {
    /** サイズと位置の計算の基準になる領域。 */
    public var layoutArea: MauiDialogLayoutArea = MauiDialogLayoutArea.VISIBLE_AREA

    /** 基準 rect の上辺から控除する余白。 */
    public var marginTop: Double = 24.0

    /** 基準 rect の左辺から控除する余白。 */
    public var marginLeft: Double = 24.0

    /** 基準 rect の下辺から控除する余白。 */
    public var marginBottom: Double = 24.0

    /** 基準 rect の右辺から控除する余白。 */
    public var marginRight: Double = 24.0

    /** 基準 rect の幅に対する比率。0 以下は未指定。 */
    public var proportionalWidth: Double = -1.0

    /** 基準 rect の高さに対する比率。0 以下は未指定。 */
    public var proportionalHeight: Double = -1.0

    /** ダイアログの背後を覆う色 (ARGB 32bit)。 */
    public var overlayColorArgb: Int = 0x66000000.toInt()

    /** 外側タップをキャンセルと同じ経路で閉じる操作として扱うか。 */
    public var isCanceledOnTouchOutside: Boolean = true
}

/**
 * MAUI 側で決まった置き場所を運ぶ入れ物。
 *
 * MAUI 側で「show 引数 > 中身への添付 > 契約既定値」の優先順に合成し終えた実効値が入る
 * (core/ADR-0015)。
 */
public class MauiDialogPlacement {
    /** 水平方向の配置。 */
    public var horizontalAlignment: MauiDialogAlignment = MauiDialogAlignment.CENTER

    /** 垂直方向の配置。 */
    public var verticalAlignment: MauiDialogAlignment = MauiDialogAlignment.CENTER

    /** 配置を決めた後に加える水平方向の移動量。正の値で右へ動く。 */
    public var offsetX: Double = 0.0

    /** 配置を決めた後に加える垂直方向の移動量。正の値で下へ動く。 */
    public var offsetY: Double = 0.0
}

/** 互換面の配置を Native ライブラリの配置へ写す。 */
@JvmSynthetic
internal fun MauiDialogAlignment.toDialogAlignment(): DialogAlignment = when (this) {
    MauiDialogAlignment.START -> DialogAlignment.START
    MauiDialogAlignment.CENTER -> DialogAlignment.CENTER
    MauiDialogAlignment.END -> DialogAlignment.END
    MauiDialogAlignment.FILL -> DialogAlignment.FILL
}

/** 互換面の基準領域を Native ライブラリの基準領域へ写す。 */
@JvmSynthetic
internal fun MauiDialogLayoutArea.toDialogLayoutArea(): DialogLayoutArea = when (this) {
    MauiDialogLayoutArea.WINDOW -> DialogLayoutArea.WINDOW
    MauiDialogLayoutArea.VISIBLE_AREA -> DialogLayoutArea.VISIBLE_AREA
}

/** 互換面の静的メタ属性を Native ライブラリの型へ写す。 */
@JvmSynthetic
internal fun MauiDialogOptions.toDialogOptions(): DialogOptions = DialogOptions(
    layoutArea = layoutArea.toDialogLayoutArea(),
    dialogMargin = DialogEdgeInsets(
        top = marginTop,
        left = marginLeft,
        bottom = marginBottom,
        right = marginRight,
    ),
    proportionalWidth = proportionalWidth,
    proportionalHeight = proportionalHeight,
    overlayColor = overlayColorArgb,
    isCanceledOnTouchOutside = isCanceledOnTouchOutside,
)

/** 互換面の置き場所を Native ライブラリの型へ写す。 */
@JvmSynthetic
internal fun MauiDialogPlacement.toDialogPlacement(): DialogPlacement = DialogPlacement(
    horizontalAlignment = horizontalAlignment.toDialogAlignment(),
    verticalAlignment = verticalAlignment.toDialogAlignment(),
    offsetX = offsetX,
    offsetY = offsetY,
)
