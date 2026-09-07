namespace KsDialogs.ApiSurfaceCheck.NegativeChecks;

/// <summary>
/// Toast に閉じる操作はない (消滅の契機は duration の経過だけで、fire-and-forget の契約に
/// 閉じる手段は存在しない。core/ADR-0031)。
/// </summary>
/// <remarks>
/// このソースはビルドプロパティ KsDialogsNegativeCheckToastHide を true にしたときだけ加わり、
/// <b>コンパイルエラーで失敗すること</b>が期待結果になる。
/// 期待する診断: CS1061 (IKsToast に Hide がない)
/// </remarks>
internal static class RejectsHideOnToast
{
    public static void TS_MA_03_Hide(IKsToast toast) => toast.Hide();
}
