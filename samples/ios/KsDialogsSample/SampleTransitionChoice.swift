import KsDialogs

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
        case .fade: SampleText.transitionFade
        case .slideUp: SampleText.transitionSlideUp
        case .slideDown: SampleText.transitionSlideDown
        case .slideStart: SampleText.transitionSlideStart
        case .slideEnd: SampleText.transitionSlideEnd
        case .zoom: SampleText.transitionZoom
        case .none: SampleText.transitionNone
        case .customHook: SampleText.transitionCustomHook
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
}
