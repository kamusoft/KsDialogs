package jp.kamusoft.ksdialogs.samples.android

/**
 * 起動引数から自動再生を指定できるデモの安定 ID。
 *
 * 値はメニュー項目と 1 対 1 に対応し、4ルートの Sample が同じ文字列を各自持つ。
 */
internal enum class SampleDemoId(val value: String) {
    BASIC_DIALOG("basic-dialog"),
    DECLARATIVE_DIALOG("declarative-dialog"),
    MODEL_DIALOG("model-dialog"),
    TEXT_INPUT_DIALOG("text-input-dialog"),
    INLINE_DIALOG("inline-dialog"),
    TRANSITION_DIALOG("transition-dialog"),
    LAYOUT_DIALOG("layout-dialog"),
    DEFAULT_LOADING("default-loading"),
    CUSTOM_LOADING("custom-loading"),
    DEFAULT_TOAST("default-toast"),
    CUSTOM_TOAST("custom-toast"),
    TOAST_STACK("toast-stack"),
    TOAST_PLACEMENT("toast-placement"),
    TOAST_OVERLAP("toast-overlap"),
    ;

    internal companion object {
        /** 値から安定デモ ID を引く。定義外の値は null を返す。 */
        fun from(value: String?): SampleDemoId? = entries.firstOrNull { it.value == value }
    }
}
