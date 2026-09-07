namespace KsDialogs.Sample.Maui;

/// <summary>
/// Transition Dialog の ViewModel。
/// </summary>
/// <remarks>
/// 結果型は <see cref="bool"/> で、完了操作は <see langword="true"/> を報告する。
/// この型そのものがレジストリの登録キーになる。
/// </remarks>
/// <param name="message">ダイアログに表示するメッセージ。</param>
/// <param name="transition">
/// 中身へ添付する出入りの演出。演出は呼び出しごとに変わるため、
/// 選ばれた組をここに載せて View factory へ運ぶ。
/// </param>
public sealed class TransitionDialogViewModel(string message, DialogTransition transition) : IDialogViewModel<bool>
{
    /// <summary>ダイアログに表示するメッセージ。</summary>
    public string Message { get; } = message;

    /// <summary>中身へ添付する出入りの演出。</summary>
    public DialogTransition Transition { get; } = transition;
}
