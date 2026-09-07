/// 4ルートの Sample が一字一句同じものを表示する文言 (cross/ADR-0007)。
enum SampleText {
    /// メニュー画面のタイトル。
    static let menuTitle = "KsDialogs Sample"
    /// デモ項目の文言。メニュー項目の文言が4ルートの対応を取る単位になる。
    static let basicDialogItem = "Basic Dialog"
    /// Basic Dialog が表示するメッセージ (デモデータの初期値)。
    static let basicDialogMessage = "こんにちは、KsDialogs!"
    /// デモ項目の文言。属性調整パネルの画面タイトルも兼ねる。
    static let layoutDialogItem = "Layout Dialog"
    /// Layout Dialog が表示するメッセージ。
    static let layoutDialogMessage = "レイアウト確認"
    /// デモ項目の文言。
    static let declarativeDialogItem = "Declarative Dialog"
    /// Declarative Dialog が表示するメッセージ。中身の書き方が違うだけなので Basic Dialog と同じ文言にする。
    static let declarativeDialogMessage = "こんにちは、KsDialogs!"
    /// デモ項目の文言。
    static let textInputDialogItem = "Text Input Dialog"
    /// Text Input Dialog が表示するメッセージ。
    static let textInputDialogMessage = "メッセージを入力してください"
    /// Text Input Dialog の入力欄の下書き文言。入力の初期値は空。
    static let textInputPlaceholder = "ここに入力"
    /// デモ項目の文言。
    static let inlineDialogItem = "Inline Dialog"
    /// Inline Dialog が表示するメッセージ。
    static let inlineDialogMessage = "インライン表示です"
    /// デモ項目の文言。トランジションデモ画面の画面タイトルも兼ねる。
    static let transitionDialogItem = "Transition Dialog"
    /// Transition Dialog が表示するメッセージ。
    static let transitionDialogMessage = "トランジションのデモです"
    /// デモ項目の文言。
    static let modelDialogItem = "Model Dialog"
    /// Model Dialog が表示するメッセージ。差は実装経路 — ViewModel 主導の呼び出し — のみ。
    static let modelDialogMessage = "ViewModel から表示しています"
    /// デモ項目の文言。
    static let defaultLoadingItem = "Default Loading"
    /// ローディングを開始するときのメッセージ。
    static let loadingStartMessage = "Loading..."
    /// 処理の途中で差し替えるメッセージ。
    static let loadingUpdateMessage = "Soon..."
    /// デモ項目の文言。
    static let customLoadingItem = "Custom Loading"
    /// カスタム Loading の中身に出す見出し。
    static let customLoadingTitle = "カスタムローディング"
    /// デモ項目の文言。
    static let defaultToastItem = "Default Toast"
    /// Default Toast が表示するメッセージ。
    static let defaultToastMessage = "Hello Toast!"
    /// デモ項目の文言。
    static let customToastItem = "Custom Toast"
    /// 登録経路のカスタム Toast の中身に出す文言。
    static let customToastMessage = "カスタムトースト"
    /// インライン経路のカスタム Toast の中身に出す文言。
    static let inlineToastMessage = "インライントースト"
    /// デモ項目の文言。
    static let toastStackItem = "Toast Stack"
    /// Toast Stack の 1 枚目のメッセージ。
    static let toastStackFirstMessage = "Toast 1"
    /// Toast Stack の 2 枚目のメッセージ。
    static let toastStackSecondMessage = "Toast 2"
    /// Toast Stack の 3 枚目のメッセージ。複数行への折り返しを見せるための長文。
    static let toastStackThirdMessage =
        "Toast 3: 長いメッセージは複数行に折り返され、コンテンツの高さが確保されることを確認する"
    /// デモ項目の文言。
    static let toastPlacementItem = "Toast Placement"
    /// Toast Placement が表示するメッセージ。
    static let toastPlacementMessage = "Placed Toast"
    /// デモ項目の文言。
    static let toastOverlapItem = "Toast Overlap"
    /// Toast Overlap が表示するメッセージ。
    static let toastOverlapMessage = "Overlap Toast"
    /// Toast Overlap の時系列が終わったときの結果表示。結果値を持たないので固定の文言にする。
    static let toastOverlapCompletedResult = "結果: 完了"
    /// 演出の選び方をまとめた区画の見出し。
    static let presetCaption = "プリセット"
    /// 時間とイージングをまとめた区画の見出し。
    static let adjustCaption = "調整"
    /// 演出の選択肢 (透明度で出入りする)。
    static let transitionFade = "Fade"
    /// 演出の選択肢 (下辺から出入りする)。
    static let transitionSlideUp = "Slide Up"
    /// 演出の選択肢 (上辺から出入りする)。
    static let transitionSlideDown = "Slide Down"
    /// 演出の選択肢 (前端から出入りする)。
    static let transitionSlideStart = "Slide Start"
    /// 演出の選択肢 (後端から出入りする)。
    static let transitionSlideEnd = "Slide End"
    /// 演出の選択肢 (縮小から等倍へ広がる)。
    static let transitionZoom = "Zoom"
    /// 演出の選択肢 (中身の演出なし)。
    static let transitionNone = "None"
    /// 演出の選択肢 (自作のフックを実演する)。
    static let transitionCustomHook = "Custom Hook"
    /// 演出の時間を調整する行の項目名。
    static let durationLabel = "時間"
    /// 演出のイージングを選ぶ行の項目名。
    static let easingLabel = "イージング"
    /// イージングの選択肢 (加速して減速する)。
    static let easingStandard = "Standard"
    /// イージングの選択肢 (等速)。
    static let easingLinear = "Linear"
    /// イージングの選択肢 (加速する)。
    static let easingAccelerate = "Accelerate"
    /// イージングの選択肢 (減速する)。
    static let easingDecelerate = "Decelerate"
    /// トランジションデモ画面の表示操作。
    static let displayAction = "表示"
    /// 水平方向の配置を選ぶ行の項目名。
    static let horizontalLabel = "Horizontal"
    /// 垂直方向の配置を選ぶ行の項目名。
    static let verticalLabel = "Vertical"
    /// 配置の選択肢 (前端)。
    static let alignmentStart = "Start"
    /// 配置の選択肢 (中央)。
    static let alignmentCenter = "Center"
    /// 配置の選択肢 (後端)。
    static let alignmentEnd = "End"
    /// 水平方向の移動量を入れる行の項目名。
    static let offsetXLabel = "OffsetX"
    /// 垂直方向の移動量を入れる行の項目名。
    static let offsetYLabel = "OffsetY"
    /// 基準領域に可視領域を使うかを切り替える行の項目名。
    static let useVisibleAreaLabel = "Use visible area"
    /// 属性調整パネルの表示操作。
    static let showAction = "Show"
    /// 完了操作の表示。
    static let completeAction = "OK"
    /// キャンセル操作の表示。
    static let cancelAction = "キャンセル"
    /// 結果表示エリアの見出し。
    static let resultCaption = "直近の結果"

    /// 演出の時間の表示 (ミリ秒)。
    static func durationValue(_ milliseconds: Int) -> String {
        "\(milliseconds) ms"
    }

    /// カスタム Loading の中身に出す進捗の百分率 (小数なし)。
    static func progressPercentage(_ progress: Double) -> String {
        "\(Int((progress * 100).rounded()))%"
    }

    /// 完了の結果表示 (真偽値の結果)。
    static func completedResult(_ value: Bool) -> String {
        "結果: completed(\(value))"
    }

    /// 完了の結果表示 (文字列の結果)。値は引用符で囲んで真偽値と見分けられるようにする。
    static func completedResult(_ value: String) -> String {
        "結果: completed(\"\(value)\")"
    }

    /// キャンセルの結果表示。
    static let cancelledResult = "結果: cancelled"

    /// ローディングの処理が終わったときの結果表示。結果値を持たないので書式ではなく固定の文言にする。
    static let loadingCompletedResult = "結果: 完了"
}
