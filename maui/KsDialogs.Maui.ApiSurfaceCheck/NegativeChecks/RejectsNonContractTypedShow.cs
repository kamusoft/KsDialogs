using System.Threading.Tasks;

namespace KsDialogs.ApiSurfaceCheck.NegativeChecks;

/// <summary>ViewModel 契約に準拠しない型では型指定 show を呼べない (core/ADR-0021)。</summary>
/// <remarks>
/// このソースはビルドプロパティ KsDialogsNegativeCheckTypedShowContract を true にしたときだけ加わり、
/// <b>コンパイルエラーで失敗すること</b>が期待結果になる。
/// 期待する診断: CS0311 (string を IDialogViewModel へ変換できない)
/// </remarks>
internal static class RejectsNonContractTypedShow
{
    public static async Task MB_MA_02_RejectsNonContractType(IKsDialog dialogs)
    {
        _ = await dialogs.ShowAsync<string>();
    }
}
