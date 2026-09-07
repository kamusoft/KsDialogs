using Microsoft.Maui.Controls;

namespace KsDialogs.ApiSurfaceCheck.NegativeChecks;

/// <summary>宣言結果型と異なる報告口を要求する factory は登録できない (core/ADR-0003)。</summary>
/// <remarks>
/// このソースはビルドプロパティ KsDialogsNegativeCheckNotifierType を true にしたときだけ加わり、
/// <b>コンパイルエラーで失敗すること</b>が期待結果になる。
/// 期待する診断: CS0311 (ConsumerDialogViewModel を IDialogViewModel&lt;string&gt; へ変換できない)
/// </remarks>
internal static class RejectsMismatchedNotifierType
{
    public static void Register(DialogViewRegistry registry)
    {
        registry.Register((ConsumerDialogViewModel _, DialogNotifier<string> _) => new Label());
    }
}
