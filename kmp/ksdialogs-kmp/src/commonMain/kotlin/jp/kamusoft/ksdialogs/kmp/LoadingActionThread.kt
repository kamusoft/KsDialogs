package jp.kamusoft.ksdialogs.kmp

/**
 * スコープ形 ([KsLoading.start]) に渡した処理を、どのスレッドで始めるかの指定。
 *
 * どちらの値でも、呼び出し元のスレッドやコルーチン文脈に関係なく、処理の最初の文は指定したスレッドで実行される。
 * 処理の中で中断した後にどのスレッドで再開するかは、コルーチンの通常の規則
 * (処理が動いている dispatcher) に従う。
 *
 * 意味は各 OS の Native ライブラリの同名の型と同一である。
 */
public enum class LoadingActionThread {
    /** UI スレッドで始める。処理の中から UI に直接触れる。既定値。 */
    MAIN,

    /** UI スレッド外で始める。UI に触れない重い処理に使う。 */
    BACKGROUND,
}
