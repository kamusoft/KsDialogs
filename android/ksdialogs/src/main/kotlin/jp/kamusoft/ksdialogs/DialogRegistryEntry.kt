package jp.kamusoft.ksdialogs

/**
 * ViewModel 型1つ分のレジストリのエントリ (core/ADR-0021)。
 *
 * View factory と ViewModel factory の2スロットを持ち、再登録は**スロット単位の後勝ち**で
 * 該当スロットだけを置き換える。片方だけを登録し直しても、もう片方は保持される。
 */
internal data class DialogRegistryEntry(
    /** 中身の View を生成する factory。インスタンス渡し show と型指定 show の両方が使う。 */
    val viewFactory: DialogViewFactory? = null,
    /** ViewModel を生成する factory。型指定 show だけが使う。 */
    val viewModelFactory: DialogViewModelFactory? = null,
)
