using System.Threading.Tasks;

namespace KsDialogs.ApiSurfaceCheck.NegativeChecks;

/// <summary>show に transition 引数はない (出入りの演出は添付でのみ供給する。core/ADR-0017)。</summary>
/// <remarks>
/// このソースはビルドプロパティ KsDialogsNegativeCheckShowTransition を true にしたときだけ加わり、
/// <b>コンパイルエラーで失敗すること</b>が期待結果になる。
/// 期待する診断: CS1739 (transition という名前のパラメーターがない)
/// </remarks>
internal static class RejectsTransitionArgumentOnShow
{
    public static Task<DialogResult<bool>> Show(IKsDialog dialogs) =>
        dialogs.ShowAsync(new ConsumerDialogViewModel(), transition: DialogTransition.None());
}
