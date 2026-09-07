using System.Threading.Tasks;

namespace KsDialogs.ApiSurfaceCheck.NegativeChecks;

/// <summary>
/// Toast の表示は待てない (表示の終了を待つ手段を契約に設けない fire-and-forget。core/ADR-0031)。
/// </summary>
/// <remarks>
/// このソースはビルドプロパティ KsDialogsNegativeCheckToastShowAwait を true にしたときだけ加わり、
/// <b>コンパイルエラーで失敗すること</b>が期待結果になる。
/// 期待する診断: CS4008 (void は待機できない)
/// </remarks>
internal static class RejectsAwaitOnToastShow
{
    public static async Task TS_MA_03_Await(IKsToast toast) => await toast.Show("保存しました");
}
