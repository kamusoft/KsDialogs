import SampleShared
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
        case .standard: SampleText.shared.EASING_STANDARD
        case .linear: SampleText.shared.EASING_LINEAR
        case .accelerate: SampleText.shared.EASING_ACCELERATE
        case .decelerate: SampleText.shared.EASING_DECELERATE
        }
    }

    /// 共有コードが運ぶ選択へ言い換える。
    var preset: SampleEasingPreset {
        switch self {
        case .standard: SampleEasingPreset.standard
        case .linear: SampleEasingPreset.linear
        case .accelerate: SampleEasingPreset.accelerate
        case .decelerate: SampleEasingPreset.decelerate
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

    /// 共有コードが運んできた選択を、画面が扱う選択肢へ戻す。
    ///
    /// - Parameter preset: 共有コードが運ぶ選択
    /// - Returns: 対応する選択肢。見つからなければ初期選択
    static func of(_ preset: SampleEasingPreset) -> SampleEasingChoice {
        allCases.first { $0.preset == preset } ?? .standard
    }
}
