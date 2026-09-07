package jp.kamusoft.ksdialogs.samples.kmp

/**
 * 撮影のために起動時へ渡せる設定 (cross/ADR-0010)。
 *
 * 値の運び方は OS ごとに違う (iOS は launch arguments の `--キー 値` の隣接トークンペア、
 * Android は同名キーの string extra) ため、キーに対応する文字列を取り出すところまでを各 OS 側が行い、
 * 取り出した文字列の検証と設定への畳み込みはこの型が受け持つ。
 * 値が無い・空文字・検証に通らない場合は、そのキーを取り込まずに既定動作へ倒す。
 *
 * @property demo 自動再生するデモ。指定なし・定義外の ID なら null
 * @property loadingStepIntervalMilliseconds Loading 進捗の刻み間隔 (ミリ秒)。
 *   指定なし・受理範囲外・数値でない値なら null
 */
data class SampleCaptureOptions(
    val demo: SampleDemoId? = null,
    val loadingStepIntervalMilliseconds: Long? = null,
) {
    companion object {
        /** 自動再生するデモを指定するキー。 */
        const val DEMO_KEY: String = "demo"

        /** Loading 進捗の刻み間隔を指定するキー。 */
        const val LOADING_STEP_INTERVAL_KEY: String = "loading-step-interval-ms"

        /** 受理する刻み間隔の範囲 (ミリ秒)。 */
        private val ACCEPTED_INTERVAL_RANGE = 1L..600_000L

        /**
         * 各 OS 側が起動引数から取り出した文字列を検証して設定にする。
         *
         * @param demo `demo` キーの値。渡されていないなら null
         * @param loadingStepIntervalMilliseconds `loading-step-interval-ms` キーの値。
         *   渡されていないなら null
         * @return 検証を通った分だけを取り込んだ設定
         */
        fun from(demo: String?, loadingStepIntervalMilliseconds: String?): SampleCaptureOptions =
            SampleCaptureOptions(
                demo = SampleDemoId.from(demo?.takeIf { it.isNotEmpty() }),
                loadingStepIntervalMilliseconds = loadingStepIntervalMilliseconds
                    ?.takeIf { it.isNotEmpty() }
                    ?.toLongOrNull()
                    ?.takeIf { it in ACCEPTED_INTERVAL_RANGE },
            )
    }
}
