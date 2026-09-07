namespace KsDialogs.ApiSurfaceCheck.NegativeChecks;

/// <summary>宣言結果型と異なる型では ViewModel 経由の報告口を受け取れない (core/ADR-0003・0018)。</summary>
/// <remarks>
/// このソースはビルドプロパティ KsDialogsNegativeCheckNotifierResultType を true にしたときだけ加わり、
/// <b>コンパイルエラーで失敗すること</b>が期待結果になる。
/// 期待する診断: CS0029 (DialogNotifier&lt;bool&gt; を DialogNotifier&lt;string&gt; へ変換できない)
/// </remarks>
internal static class RejectsMismatchedNotifierResultType
{
    public static void MB_MA_02_RejectsMismatchedNotifierResultType(ConsumerModelBindingViewModel viewModel)
    {
        DialogNotifier<string>? notifier = viewModel.Notifier;
        _ = notifier;
    }
}
