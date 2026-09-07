namespace KsDialogs.ApiSurfaceCheck.NegativeChecks;

/// <summary>ダイアログ表示の契約に <c>IKsDialogs</c> という型名はない (契約は <c>IKs</c> + 機能名の単数形)。</summary>
/// <remarks>
/// このソースはビルドプロパティ KsDialogsNegativeCheckLegacyContractName を true にしたときだけ加わり、
/// <b>コンパイルエラーで失敗すること</b>が期待結果になる。
/// 期待する診断: CS0246 (型または名前空間の名前 'IKsDialogs' が見つからなかった)
/// </remarks>
internal static class RejectsLegacyContractName
{
    public static object Accept(IKsDialogs dialogs) => dialogs;
}
