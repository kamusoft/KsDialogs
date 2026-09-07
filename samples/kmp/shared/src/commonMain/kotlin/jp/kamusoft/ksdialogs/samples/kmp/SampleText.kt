package jp.kamusoft.ksdialogs.samples.kmp

import kotlin.math.roundToInt

/** 4ルートの Sample が一字一句同じものを表示する文言 (cross/ADR-0007)。 */
object SampleText {
    /** メニュー画面のタイトル。 */
    const val MENU_TITLE: String = "KsDialogs Sample"

    /** デモ項目の文言。メニュー項目の文言が4ルートの対応を取る単位になる。 */
    const val BASIC_DIALOG_ITEM: String = "Basic Dialog"

    /** Basic Dialog が表示するメッセージ (デモデータの初期値)。 */
    const val BASIC_DIALOG_MESSAGE: String = "こんにちは、KsDialogs!"

    /** デモ項目の文言。属性調整パネルの画面タイトルも兼ねる。 */
    const val LAYOUT_DIALOG_ITEM: String = "Layout Dialog"

    /** Layout Dialog が表示するメッセージ。 */
    const val LAYOUT_DIALOG_MESSAGE: String = "レイアウト確認"

    /** デモ項目の文言。 */
    const val DECLARATIVE_DIALOG_ITEM: String = "Declarative Dialog"

    /** Declarative Dialog が表示するメッセージ。中身の書き方が違うだけなので Basic Dialog と同じ文言にする。 */
    const val DECLARATIVE_DIALOG_MESSAGE: String = "こんにちは、KsDialogs!"

    /** デモ項目の文言。 */
    const val TEXT_INPUT_DIALOG_ITEM: String = "Text Input Dialog"

    /** Text Input Dialog が表示するメッセージ。 */
    const val TEXT_INPUT_DIALOG_MESSAGE: String = "メッセージを入力してください"

    /** Text Input Dialog の入力欄の下書き文言。入力の初期値は空。 */
    const val TEXT_INPUT_PLACEHOLDER: String = "ここに入力"

    /** デモ項目の文言。 */
    const val INLINE_DIALOG_ITEM: String = "Inline Dialog"

    /** Inline Dialog が表示するメッセージ。 */
    const val INLINE_DIALOG_MESSAGE: String = "インライン表示です"

    /** デモ項目の文言。トランジションデモ画面の画面タイトルも兼ねる。 */
    const val TRANSITION_DIALOG_ITEM: String = "Transition Dialog"

    /** Transition Dialog が表示するメッセージ。 */
    const val TRANSITION_DIALOG_MESSAGE: String = "トランジションのデモです"

    /** デモ項目の文言。 */
    const val MODEL_DIALOG_ITEM: String = "Model Dialog"

    /** Model Dialog が表示するメッセージ。差は実装経路 — ViewModel 主導の呼び出し — のみ。 */
    const val MODEL_DIALOG_MESSAGE: String = "ViewModel から表示しています"

    /** デモ項目の文言。 */
    const val DEFAULT_LOADING_ITEM: String = "Default Loading"

    /** ローディングを開始するときのメッセージ。 */
    const val LOADING_START_MESSAGE: String = "Loading..."

    /** 処理の途中で差し替えるメッセージ。 */
    const val LOADING_UPDATE_MESSAGE: String = "Soon..."

    /** デモ項目の文言。 */
    const val CUSTOM_LOADING_ITEM: String = "Custom Loading"

    /** カスタム Loading の中身に出す見出し。 */
    const val CUSTOM_LOADING_TITLE: String = "カスタムローディング"

    /** デモ項目の文言。 */
    const val DEFAULT_TOAST_ITEM: String = "Default Toast"

    /** Default Toast が表示するメッセージ。 */
    const val DEFAULT_TOAST_MESSAGE: String = "Hello Toast!"

    /** デモ項目の文言。 */
    const val CUSTOM_TOAST_ITEM: String = "Custom Toast"

    /** 登録経路のカスタム Toast の中身に出す文言。 */
    const val CUSTOM_TOAST_MESSAGE: String = "カスタムトースト"

    /** インライン経路のカスタム Toast の中身に出す文言。 */
    const val INLINE_TOAST_MESSAGE: String = "インライントースト"

    /** デモ項目の文言。 */
    const val TOAST_STACK_ITEM: String = "Toast Stack"

    /** Toast Stack の 1 枚目のメッセージ。 */
    const val TOAST_STACK_FIRST_MESSAGE: String = "Toast 1"

    /** Toast Stack の 2 枚目のメッセージ。 */
    const val TOAST_STACK_SECOND_MESSAGE: String = "Toast 2"

    /** Toast Stack の 3 枚目のメッセージ。複数行への折り返しを見せるための長文。 */
    const val TOAST_STACK_THIRD_MESSAGE: String =
        "Toast 3: 長いメッセージは複数行に折り返され、コンテンツの高さが確保されることを確認する"

    /** デモ項目の文言。 */
    const val TOAST_PLACEMENT_ITEM: String = "Toast Placement"

    /** Toast Placement が表示するメッセージ。 */
    const val TOAST_PLACEMENT_MESSAGE: String = "Placed Toast"

    /** デモ項目の文言。 */
    const val TOAST_OVERLAP_ITEM: String = "Toast Overlap"

    /** Toast Overlap が表示するメッセージ。 */
    const val TOAST_OVERLAP_MESSAGE: String = "Overlap Toast"

    /** Toast Overlap の時系列が終わったときの結果表示。結果値を持たないので固定の文言にする。 */
    const val TOAST_OVERLAP_COMPLETED_RESULT: String = "結果: 完了"

    /** 演出の選び方をまとめた区画の見出し。 */
    const val PRESET_CAPTION: String = "プリセット"

    /** 時間とイージングをまとめた区画の見出し。 */
    const val ADJUST_CAPTION: String = "調整"

    /** 演出の選択肢 (透明度で出入りする)。 */
    const val TRANSITION_FADE: String = "Fade"

    /** 演出の選択肢 (下辺から出入りする)。 */
    const val TRANSITION_SLIDE_UP: String = "Slide Up"

    /** 演出の選択肢 (上辺から出入りする)。 */
    const val TRANSITION_SLIDE_DOWN: String = "Slide Down"

    /** 演出の選択肢 (前端から出入りする)。 */
    const val TRANSITION_SLIDE_START: String = "Slide Start"

    /** 演出の選択肢 (後端から出入りする)。 */
    const val TRANSITION_SLIDE_END: String = "Slide End"

    /** 演出の選択肢 (縮小から等倍へ広がる)。 */
    const val TRANSITION_ZOOM: String = "Zoom"

    /** 演出の選択肢 (中身の演出なし)。 */
    const val TRANSITION_NONE: String = "None"

    /** 演出の選択肢 (自作のフックを実演する)。 */
    const val TRANSITION_CUSTOM_HOOK: String = "Custom Hook"

    /** 演出の時間を調整する行の項目名。 */
    const val DURATION_LABEL: String = "時間"

    /** 演出のイージングを選ぶ行の項目名。 */
    const val EASING_LABEL: String = "イージング"

    /** イージングの選択肢 (加速して減速する)。 */
    const val EASING_STANDARD: String = "Standard"

    /** イージングの選択肢 (等速)。 */
    const val EASING_LINEAR: String = "Linear"

    /** イージングの選択肢 (加速する)。 */
    const val EASING_ACCELERATE: String = "Accelerate"

    /** イージングの選択肢 (減速する)。 */
    const val EASING_DECELERATE: String = "Decelerate"

    /** トランジションデモ画面の表示操作。 */
    const val DISPLAY_ACTION: String = "表示"

    /** 水平方向の配置を選ぶ行の項目名。 */
    const val HORIZONTAL_LABEL: String = "Horizontal"

    /** 垂直方向の配置を選ぶ行の項目名。 */
    const val VERTICAL_LABEL: String = "Vertical"

    /** 配置の選択肢 (前端)。 */
    const val ALIGNMENT_START: String = "Start"

    /** 配置の選択肢 (中央)。 */
    const val ALIGNMENT_CENTER: String = "Center"

    /** 配置の選択肢 (後端)。 */
    const val ALIGNMENT_END: String = "End"

    /** 水平方向の移動量を入れる行の項目名。 */
    const val OFFSET_X_LABEL: String = "OffsetX"

    /** 垂直方向の移動量を入れる行の項目名。 */
    const val OFFSET_Y_LABEL: String = "OffsetY"

    /** 基準領域に可視領域を使うかを切り替える行の項目名。 */
    const val USE_VISIBLE_AREA_LABEL: String = "Use visible area"

    /** 属性調整パネルの表示操作。 */
    const val SHOW_ACTION: String = "Show"

    /** 完了操作の表示。 */
    const val COMPLETE_ACTION: String = "OK"

    /** キャンセル操作の表示。 */
    const val CANCEL_ACTION: String = "キャンセル"

    /** 結果表示エリアの見出し。 */
    const val RESULT_CAPTION: String = "直近の結果"

    /** キャンセルの結果表示。 */
    const val CANCELLED_RESULT: String = "結果: cancelled"

    /** ローディングの処理が終わったときの結果表示。結果値を持たないので書式ではなく固定の文言にする。 */
    const val LOADING_COMPLETED_RESULT: String = "結果: 完了"

    /** 演出の時間の表示 (ミリ秒)。 */
    fun durationValue(milliseconds: Int): String = "$milliseconds ms"

    /** カスタム Loading の中身に出す進捗の百分率 (小数なし)。 */
    fun progressPercentage(progress: Double): String = "${(progress * 100).roundToInt()}%"

    /** 完了の結果表示 (真偽値の結果)。 */
    fun completedResult(value: Boolean): String = "結果: completed($value)"

    /** 完了の結果表示 (文字列の結果)。値は引用符で囲んで真偽値と見分けられるようにする。 */
    fun completedResult(value: String): String = "結果: completed(\"$value\")"
}
