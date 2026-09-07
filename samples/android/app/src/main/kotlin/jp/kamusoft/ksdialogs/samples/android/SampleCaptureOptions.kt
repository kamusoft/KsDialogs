package jp.kamusoft.ksdialogs.samples.android

import android.content.Intent

/**
 * 撮影のために起動時へ渡せる設定 (cross/ADR-0010)。
 *
 * 値は同名キーの string extra で受け取る (`--es demo basic-dialog --es loading-step-interval-ms 2000`)。
 * 値が無い・空文字・検証に通らない場合は、そのキーを取り込まずに既定動作へ倒す。
 */
internal data class SampleCaptureOptions(
    /** 自動再生するデモ。指定なし・定義外の ID なら null。 */
    val demo: SampleDemoId? = null,
    /** Loading 進捗の刻み間隔 (ミリ秒)。指定なし・受理範囲外・数値でない値なら null。 */
    val loadingStepIntervalMilliseconds: Long? = null,
) {
    internal companion object {
        /** 自動再生するデモを指定するキー。 */
        private const val DEMO_KEY = "demo"

        /** Loading 進捗の刻み間隔を指定するキー。 */
        private const val LOADING_STEP_INTERVAL_KEY = "loading-step-interval-ms"

        /** 受理する刻み間隔の範囲 (ミリ秒)。 */
        private val ACCEPTED_INTERVAL_RANGE = 1L..600_000L

        /** 起動 Intent の string extra から設定を読み取る。 */
        fun from(intent: Intent?): SampleCaptureOptions = SampleCaptureOptions(
            demo = SampleDemoId.from(intent.stringValue(DEMO_KEY)),
            loadingStepIntervalMilliseconds = intent.stringValue(LOADING_STEP_INTERVAL_KEY)
                ?.toLongOrNull()
                ?.takeIf { it in ACCEPTED_INTERVAL_RANGE },
        )

        /**
         * string extra を読む。
         *
         * 空文字は「値が渡されていない」と同じに扱う。string 以外の型で渡された値は読めないため
         * null になり、同じく既定動作へ倒れる。
         */
        private fun Intent?.stringValue(key: String): String? =
            this?.getStringExtra(key)?.takeIf { it.isNotEmpty() }
    }
}
