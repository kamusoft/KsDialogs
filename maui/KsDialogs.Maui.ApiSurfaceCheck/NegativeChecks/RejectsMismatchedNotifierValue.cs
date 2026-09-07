using Microsoft.Maui.Controls;

namespace KsDialogs.ApiSurfaceCheck.NegativeChecks;

/// <summary>宣言結果型と異なる値では完了報告できない (core/ADR-0003)。</summary>
/// <remarks>
/// このソースはビルドプロパティ KsDialogsNegativeCheckNotifierValue を true にしたときだけ加わり、
/// <b>コンパイルエラーで失敗すること</b>が期待結果になる。
/// 期待する診断: CS1503 (string から bool へ変換できない)
/// </remarks>
internal static class RejectsMismatchedNotifierValue
{
    public static void Register(DialogViewRegistry registry)
    {
        registry.Register((ConsumerDialogViewModel _, DialogNotifier<bool> notifier) =>
        {
            notifier.Complete("bool ではない値");
            return new Label();
        });
    }
}
