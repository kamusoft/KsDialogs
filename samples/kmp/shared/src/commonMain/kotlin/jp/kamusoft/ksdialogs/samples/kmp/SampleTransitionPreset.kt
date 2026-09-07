package jp.kamusoft.ksdialogs.samples.kmp

/**
 * トランジションデモが選んだ演出。
 *
 * 演出そのものは各 OS のアニメーション API で組み立てるため、共有コードは
 * 「どれを選んだか」だけを運び、View 定義側でその OS の演出へ言い換える。
 */
enum class SampleTransitionPreset {
    /** 透明度で出入りする。 */
    FADE,

    /** 下辺から出入りする。 */
    SLIDE_UP,

    /** 上辺から出入りする。 */
    SLIDE_DOWN,

    /** 前端から出入りする。 */
    SLIDE_START,

    /** 後端から出入りする。 */
    SLIDE_END,

    /** 縮小から等倍へ広がる。 */
    ZOOM,

    /** 中身の演出なし。 */
    NONE,

    /** 自作のフックを実演する。 */
    CUSTOM_HOOK,
}
