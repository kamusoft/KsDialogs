package jp.kamusoft.ksdialogs

/**
 * スコープ形 ([KsLoading.start]) に渡した処理を、どのスレッドで始めるかの指定。
 *
 * どちらの値でも、呼び出し元のスレッドに関係なく、処理の最初の文は指定したスレッドで実行される。
 * 処理の中で中断した後にどのスレッドで再開するかは、コルーチンの通常の規則
 * (処理が動いている dispatcher) に従う。
 */
public enum class LoadingActionThread {
    /** UI スレッド (Main dispatcher) で始める。処理の中から View に直接触れる。既定値。 */
    MAIN,

    /** UI スレッド外 (Default dispatcher) で始める。UI に触れない重い処理に使う。 */
    BACKGROUND,
}
