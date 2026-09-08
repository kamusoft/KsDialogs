package jp.kamusoft.ksdialogs

/**
 * Loading のレジストリが保持する ViewModel 生成関数の型消去表現。
 *
 * 型指定の show / start はこれで ViewModel を作ってから configure・View 生成へ進む。
 * 生成は UI スレッドで行われる。
 */
internal fun interface LoadingViewModelFactory {
    fun createViewModel(): Any
}
