namespace KsDialogs.ApiSurfaceCheck.NegativeChecks;

/// <summary>
/// Toast の表示は戻り値を持たない (結果も handle も返さない fire-and-forget。core/ADR-0031)。
/// </summary>
/// <remarks>
/// このソースはビルドプロパティ KsDialogsNegativeCheckToastShowReturn を true にしたときだけ加わり、
/// <b>コンパイルエラーで失敗すること</b>が期待結果になる。
/// 期待する診断: CS0029 (void を object に変換できない)
/// </remarks>
internal static class RejectsReturnValueOnToastShow
{
    public static object TS_MA_03_ReturnValue(IKsToast toast)
    {
        object presentation = toast.Show("保存しました");
        return presentation;
    }
}
