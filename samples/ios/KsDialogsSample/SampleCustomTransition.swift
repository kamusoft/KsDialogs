import KsDialogs
import UIKit

/// 自作のフックで組む演出。
///
/// プリセットを使わずに `DialogTransition` を直接組み立てる書き方の実演で、
/// 時間もイージングもこの演出自身が決めるため、画面の調整値は使わない。
enum SampleCustomTransition {
    /// 入場でホスト View が移動する距離。下から持ち上げる分だけ最初に押し下げる。
    private static let travelDistance: CGFloat = 80
    /// 入場・退場の時間。
    private static let duration: TimeInterval = 0.3

    /// 入場は上方向へ移動しながら現れ、退場は透明度だけで消える演出を作る。
    @MainActor
    static func make() -> DialogTransition {
        DialogTransition(
            presentation: { hostView in
                hostView.transform = CGAffineTransform(translationX: 0, y: travelDistance)
                hostView.alpha = 0
                // 演出が終わってから戻る (戻るまで器は「表示中」へ進まない)
                await animate {
                    hostView.transform = .identity
                    hostView.alpha = 1
                }
            },
            dismissal: { hostView in
                await animate {
                    hostView.alpha = 0
                }
            }
        )
    }

    /// アニメーションを1本走らせ、完了するまで待つ。
    @MainActor
    private static func animate(_ changes: @escaping () -> Void) async {
        let animator = UIViewPropertyAnimator(
            duration: duration,
            timingParameters: UICubicTimingParameters(animationCurve: .easeInOut)
        )
        animator.addAnimations(changes)
        await withCheckedContinuation { (continuation: CheckedContinuation<Void, Never>) in
            animator.addCompletion { _ in continuation.resume() }
            animator.startAnimation()
        }
    }
}
