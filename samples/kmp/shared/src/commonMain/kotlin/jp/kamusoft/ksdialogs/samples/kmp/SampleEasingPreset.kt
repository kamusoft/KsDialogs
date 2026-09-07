package jp.kamusoft.ksdialogs.samples.kmp

/**
 * トランジションデモが選んだイージング。
 *
 * 契約はイージングを形態のネイティブ表現でそのまま受け取るため、共有コードは
 * 「どれを選んだか」だけを運び、View 定義側でその OS の時間曲線へ言い換える。
 */
enum class SampleEasingPreset {
    /** 加速して減速する。 */
    STANDARD,

    /** 等速。 */
    LINEAR,

    /** 加速する。 */
    ACCELERATE,

    /** 減速する。 */
    DECELERATE,
}
