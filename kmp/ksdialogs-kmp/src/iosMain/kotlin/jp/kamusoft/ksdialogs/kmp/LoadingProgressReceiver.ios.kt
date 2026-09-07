package jp.kamusoft.ksdialogs.kmp

/**
 * iOS では進捗の受け口も共有コード側の面のままになる。
 *
 * Native ライブラリの互換面は ViewModel を型消去して受け取り、この面を知らないため、
 * 受け口を実装しているかの判定と転送は委譲面 (共有コード側) が行う (kmp/ADR-0002)。
 */
public actual interface LoadingProgressReceiver {
    public actual fun onProgress(progress: Double)
}
