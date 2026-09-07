import KsDialogs

/// Transition Dialog の ViewModel。
///
/// 結果型は `Bool` で、完了操作は `true` を報告する。
/// この型そのものがレジストリの登録キーになる。
/// 演出は呼び出しごとに変わるため、選ばれた組をここに載せて View factory へ運ぶ。
final class TransitionDialogViewModel: DialogViewModel {
    typealias Result = Bool

    /// ダイアログに表示するメッセージ。
    let message: String
    /// 中身へ添付する出入りの演出。
    let transition: DialogTransition

    init(message: String, transition: DialogTransition) {
        self.message = message
        self.transition = transition
    }
}
