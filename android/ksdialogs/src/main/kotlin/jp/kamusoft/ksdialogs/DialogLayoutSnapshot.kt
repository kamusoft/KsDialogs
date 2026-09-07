package jp.kamusoft.ksdialogs

import android.view.View

/**
 * 実効値の合成の元になる供給値の組。
 *
 * 丸める前の値で持つのは、丸めた後では区別できない書き換え (既定値へ戻す等) も
 * 変化として拾うためである。
 */
internal data class DialogLayoutSupply(
    val options: DialogOptions,
    val placement: DialogPlacement,
)

/**
 * 供給値から実効値を合成し、初回レイアウトパスの完了時点で固定する面。
 *
 * 契約が定めるスナップショット時点は「初回のネイティブレイアウトパス完了時点」であり、
 * 器の組み立て時点ではない (core/ADR-0015)。そのため合成はレイアウトのたびに読み直し、
 * パスが完了して値が落ち着いたところで [freeze] により固定する。
 * 固定した後は供給値が書き換わっても実効値は変わらない。
 *
 * 供給値の出どころは器ごとに違う (中身への添付を読む器と、設定プロパティを読む器がある) ため、
 * 読み方は [readSupply] として渡す。中身への添付を読む器は View を受ける方の作り方を使う。
 *
 * @param readSupply その時点の供給値を読む方法。レイアウトのたびに呼ばれる
 */
internal class DialogLayoutSnapshot(
    private val readSupply: () -> DialogLayoutSupply,
) {

    /**
     * 中身への添付と show の引数から供給値を読む器のための作り方。
     *
     * 優先順は「show 引数 > コンテンツ添付 > 契約既定値」(core/ADR-0015)。
     *
     * @param contentView 添付の読み取り元になる中身の View
     * @param showPlacement show の引数で渡された配置。null なら添付が使われる
     */
    constructor(contentView: View, showPlacement: DialogPlacement?) : this(
        {
            DialogLayoutSupply(
                options = contentView.ksDialogOptions ?: DialogOptions(),
                placement = showPlacement ?: contentView.ksDialogPlacement ?: DialogPlacement(),
            )
        },
    )

    /** 固定済みの実効値。固定前は null。 */
    private var frozen: DialogLayout? = null

    /** 直近の合成に使った供給値。読み直したときの変化の有無をこれで判定する。 */
    private var appliedSupply: DialogLayoutSupply? = null

    /** 直近の合成結果。供給値が変わらない限り作り直さない。 */
    private var composed: DialogLayout? = null

    /** 実効値を固定済みか。 */
    val isFrozen: Boolean
        get() = frozen != null

    /**
     * その時点の実効値。
     *
     * 固定前は供給値を読み直して合成し、固定後は固定した値をそのまま返す。
     */
    fun effective(): DialogLayout {
        frozen?.let { return it }
        val supply = readSupply()
        val cached = composed
        if (cached != null && supply == appliedSupply) {
            return cached
        }
        appliedSupply = supply
        return DialogLayout(options = supply.options, placement = supply.placement).also { composed = it }
    }

    /** 直近の合成から供給値が書き換わっていれば true。固定後は常に false。 */
    fun hasPendingChange(): Boolean = frozen == null && readSupply() != appliedSupply

    /** その時点の実効値をスナップショットとして固定する。2回目以降の呼び出しは何もしない。 */
    fun freeze() {
        if (frozen == null) {
            frozen = effective()
        }
    }
}
