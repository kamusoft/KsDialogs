package jp.kamusoft.ksdialogs

/**
 * 器が辿る状態。閉鎖信号の扱いはこの状態で決まる (core/ADR-0017)。
 */
internal enum class DialogContainerState {
    /** show の呼び出しで器を組み立てた直後。まだ画面に載っていない。 */
    CREATED,

    /** 器を画面に載せてレイアウトを走らせ、実効値を固定した時点。中身はまだ見えない。 */
    ATTACHED,

    /** 覆いのフェードと出現の演出を進めている間。 */
    PRESENTING,

    /** 利用者の操作と結果報告を受け付ける。 */
    SHOWN,

    /** 退出の演出と覆いのフェードを進めている間。入力は受け付けない。 */
    DISMISSING,

    /** 器を撤去し、確定済みの結果を呼び出し元へ配送した後。 */
    REMOVED,
}
