namespace KsDialogs.ApiSurfaceCheck.NegativeChecks;

/// <summary>
/// Toast の表示 API に style 引数はない (見た目はシングルトンの設定プロパティで一括設定し、
/// 器は各表示の受理時に読む。core/ADR-0032)。
/// </summary>
/// <remarks>
/// このソースはビルドプロパティ KsDialogsNegativeCheckToastShowStyle を true にしたときだけ加わり、
/// <b>コンパイルエラーで失敗すること</b>が期待結果になる。
/// 期待する診断: CS1739 (style という名前のパラメーターがない)
/// </remarks>
internal static class RejectsStyleArgumentOnToastShow
{
    public static void TS_MA_03_StyleArgument(IKsToast toast) =>
        toast.Show("保存しました", style: new ToastStyle());
}
