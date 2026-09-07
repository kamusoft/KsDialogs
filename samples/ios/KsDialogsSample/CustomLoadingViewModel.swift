import KsDialogs
import Observation

/// Custom Loading の ViewModel。
///
/// 進捗の受け口 (`LoadingProgressReceiver`) を実装しているため、スコープ形の処理が報告した進捗が
/// 表示中のあいだ転送される。届いた値はそのまま観測可能な状態として保持し、中身の View が読む。
@MainActor
@Observable
final class CustomLoadingViewModel: LoadingViewModel, LoadingProgressReceiver {
    /// 0〜1 に丸めた後の進捗。まだ報告が無い開始直後は 0。
    private(set) var progress: Double = 0

    /// 進捗の報告を受け取る。呼び出しは UI スレッド上で行われる。
    func onProgress(_ progress: Double) {
        self.progress = progress
    }
}
