import KsDialogs
import UIKit

/// トランジションデモが選べるイージング。
///
/// 契約はイージングを形態のネイティブ表現でそのまま受け取るため、
/// 選択肢は `UITimingCurveProvider` への言い換えになる。
enum SampleEasingChoice: CaseIterable, Identifiable {
    case standard
    case linear
    case accelerate
    case decelerate

    var id: Self { self }

    /// チップに表示する文言。
    var label: String {
        switch self {
        case .standard: SampleText.easingStandard
        case .linear: SampleText.easingLinear
        case .accelerate: SampleText.easingAccelerate
        case .decelerate: SampleText.easingDecelerate
        }
    }

    /// プリセットへ渡す時間曲線。
    ///
    /// `standard` はライブラリがプリセットの既定として公開している曲線をそのまま使う。
    @MainActor
    var timingCurve: any UITimingCurveProvider {
        switch self {
        case .standard: .standard
        case .linear: UICubicTimingParameters(animationCurve: .linear)
        case .accelerate: UICubicTimingParameters(animationCurve: .easeIn)
        case .decelerate: UICubicTimingParameters(animationCurve: .easeOut)
        }
    }
}
