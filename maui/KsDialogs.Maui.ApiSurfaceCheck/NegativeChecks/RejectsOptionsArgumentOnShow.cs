using System.Threading.Tasks;

namespace KsDialogs.ApiSurfaceCheck.NegativeChecks;

/// <summary>show に options 引数はない (静的メタ属性は呼び出しごとに変えられない。core/ADR-0015)。</summary>
/// <remarks>
/// このソースはビルドプロパティ KsDialogsNegativeCheckShowOptions を true にしたときだけ加わり、
/// <b>コンパイルエラーで失敗すること</b>が期待結果になる。
/// 引数の型には公開されている <see cref="DialogPlacement"/> を使い、
/// 「options という名前の引数が無い」ことだけを突く。
/// 期待する診断: CS1739 (options という名前のパラメーターがない)
/// </remarks>
internal static class RejectsOptionsArgumentOnShow
{
    public static Task<DialogResult<bool>> Show(IKsDialog dialogs) =>
        dialogs.ShowAsync(new ConsumerDialogViewModel(), options: new DialogPlacement());
}
