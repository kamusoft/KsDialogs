import KsDialogs
import SampleShared

/// トランジションデモが選べる演出。
///
/// プリセット7種に、自作フックの実演を1つ足したものを並べる。
enum SampleTransitionChoice: CaseIterable, Identifiable {
    case fade
    case slideUp
    case slideDown
    case slideStart
    case slideEnd
    case zoom
    case none
    case customHook

    var id: Self { self }

    /// チップに表示する文言。
    var label: String {
        switch self {
        case .fade: SampleText.shared.TRANSITION_FADE
        case .slideUp: SampleText.shared.TRANSITION_SLIDE_UP
        case .slideDown: SampleText.shared.TRANSITION_SLIDE_DOWN
        case .slideStart: SampleText.shared.TRANSITION_SLIDE_START
        case .slideEnd: SampleText.shared.TRANSITION_SLIDE_END
        case .zoom: SampleText.shared.TRANSITION_ZOOM
        case .none: SampleText.shared.TRANSITION_NONE
        case .customHook: SampleText.shared.TRANSITION_CUSTOM_HOOK
        }
    }

    /// 時間とイージングの調整を使う演出か。
    ///
    /// 無演出と自作フックは調整値を受け取らないため、画面では調整部を操作できなくする。
    var usesAdjustments: Bool {
        switch self {
        case .none, .customHook: false
        default: true
        }
    }

    /// 共有コードが運ぶ選択へ言い換える。
    var preset: SampleTransitionPreset {
        switch self {
        case .fade: SampleTransitionPreset.fade
        case .slideUp: SampleTransitionPreset.slideUp
        case .slideDown: SampleTransitionPreset.slideDown
        case .slideStart: SampleTransitionPreset.slideStart
        case .slideEnd: SampleTransitionPreset.slideEnd
        case .zoom: SampleTransitionPreset.zoom
        case .none: SampleTransitionPreset.none
        case .customHook: SampleTransitionPreset.customHook
        }
    }

    /// 選択中の調整値と組み合わせて、中身へ添付する演出を作る。
    ///
    /// - Parameters:
    ///   - durationMilliseconds: 片道の時間 (ミリ秒)
    ///   - easing: 時間に対する進み方
    @MainActor
    func transition(durationMilliseconds: Int, easing: SampleEasingChoice) -> DialogTransition {
        let duration = Double(durationMilliseconds) / 1000
        let curve = easing.timingCurve
        switch self {
        case .fade:
            return .fade(duration: duration, easing: curve)
        case .slideUp:
            return .slide(from: .bottom, duration: duration, easing: curve)
        case .slideDown:
            return .slide(from: .top, duration: duration, easing: curve)
        case .slideStart:
            return .slide(from: .leading, duration: duration, easing: curve)
        case .slideEnd:
            return .slide(from: .trailing, duration: duration, easing: curve)
        case .zoom:
            return .zoom(duration: duration, easing: curve)
        case .none:
            return .none()
        case .customHook:
            return SampleCustomTransition.make()
        }
    }

    /// 共有コードが運んできた選択を、画面が扱う選択肢へ戻す。
    ///
    /// - Parameter preset: 共有コードが運ぶ選択
    /// - Returns: 対応する選択肢。見つからなければ初期選択
    static func of(_ preset: SampleTransitionPreset) -> SampleTransitionChoice {
        allCases.first { $0.preset == preset } ?? .fade
    }
}
