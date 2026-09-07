/// 起動引数から自動再生を指定できるデモの安定 ID。
///
/// 値はメニュー項目と 1 対 1 に対応し、4ルートの Sample が同じ文字列を各自持つ。
enum SampleDemoId: String {
    case basicDialog = "basic-dialog"
    case declarativeDialog = "declarative-dialog"
    case modelDialog = "model-dialog"
    case textInputDialog = "text-input-dialog"
    case inlineDialog = "inline-dialog"
    case transitionDialog = "transition-dialog"
    case layoutDialog = "layout-dialog"
    case defaultLoading = "default-loading"
    case customLoading = "custom-loading"
    case defaultToast = "default-toast"
    case customToast = "custom-toast"
    case toastStack = "toast-stack"
    case toastPlacement = "toast-placement"
    case toastOverlap = "toast-overlap"
}
