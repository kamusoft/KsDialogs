using System.Threading.Tasks;

namespace KsDialogs.ApiSurfaceCheck.NegativeChecks;

/// <summary>宣言結果型と異なる型では受け取れない (core/ADR-0003)。</summary>
/// <remarks>
/// このソースはビルドプロパティ KsDialogsNegativeCheckResultType を true にしたときだけ加わり、
/// <b>コンパイルエラーで失敗すること</b>が期待結果になる。
/// 期待する診断: CS0029 (DialogResult&lt;bool&gt; を DialogResult&lt;string&gt; へ変換できない)
/// </remarks>
internal static class RejectsMismatchedResultType
{
    public static async Task Receive(IKsDialog dialogs)
    {
        DialogResult<string> result = await dialogs.ShowAsync(new ConsumerDialogViewModel());
        _ = result;
    }
}
