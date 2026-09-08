package jp.kamusoft.ksdialogs

/**
 * レジストリが保持する ViewModel 生成関数の型消去表現 (core/ADR-0021)。
 *
 * 型指定 show はこれで ViewModel を作ってから configure・View 生成へ進む。
 * 生成は UI スレッドで行われる。
 */
internal fun interface DialogViewModelFactory {
    fun createViewModel(): Any
}
