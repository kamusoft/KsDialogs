namespace KsDialogs.ApiSurfaceCheck.NegativeChecks;

/// <summary>ViewModel はレイアウト属性を持たない (core/ADR-0014)。</summary>
/// <remarks>
/// このソースはビルドプロパティ KsDialogsNegativeCheckVmAttribute を true にしたときだけ加わり、
/// <b>コンパイルエラーで失敗すること</b>が期待結果になる。
/// 期待する診断: CS1061 ('ConsumerDialogViewModel' に 'ProportionalWidth' の定義が含まれていない)
/// </remarks>
internal static class RejectsLayoutAttributeOnViewModel
{
    public static double Read() => new ConsumerDialogViewModel().ProportionalWidth;
}
