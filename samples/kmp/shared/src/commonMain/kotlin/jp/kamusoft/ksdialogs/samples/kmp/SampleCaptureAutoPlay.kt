package jp.kamusoft.ksdialogs.samples.kmp

/**
 * 起動引数で指定されたデモの自動再生を、プロセスの起動につき 1 回だけ取り出す受け口。
 *
 * 一度取り出したあとは null を返すため、画面が作り直されても再生は繰り返されない。
 */
object SampleCaptureAutoPlay {
    private var isConsumed = false

    /**
     * 自動再生するデモを取り出す。
     *
     * @param options この起動で読み取った設定
     * @return 自動再生するデモ。指定が無い場合と 2 回目以降は null
     */
    fun consumeDemo(options: SampleCaptureOptions): SampleDemoId? {
        if (isConsumed) {
            return null
        }
        isConsumed = true
        return options.demo
    }
}
