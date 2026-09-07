import KsDialogs

/// 登録経路のカスタム Toast の ViewModel。
///
/// Toast は結果も進捗も持たないため、この型はデータの運搬とレジストリの型キーだけを担う。
/// この型そのものがレジストリの登録キーになる。
/// 実体はレジストリの ViewModel factory が引数なしで作り、メッセージは表示直前の configure が入れる。
/// 状態の変更は UI スレッドに限るため MainActor に閉じる。
@MainActor
final class CustomToastViewModel: ToastViewModel {
    /// Toast に表示するメッセージ。表示の直前に設定する。
    var message: String = ""
}
