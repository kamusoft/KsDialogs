using System.Threading.Tasks;

namespace KsDialogs.ApiSurfaceCheck.NegativeChecks;

/// <summary>
/// 既定ローディングの表示 API に style 引数はない (見た目はシングルトンの設定プロパティで
/// 一括設定し、器は各表示の開始時に読む。core/ADR-0023)。
/// </summary>
/// <remarks>
/// このソースはビルドプロパティ KsDialogsNegativeCheckLoadingShowStyle を true にしたときだけ加わり、
/// <b>コンパイルエラーで失敗すること</b>が期待結果になる。
/// 期待する診断: CS1739 (style という名前のパラメーターがない)
/// </remarks>
internal static class RejectsStyleArgumentOnLoadingShow
{
    public static Task Show(IKsLoading loading) =>
        loading.ShowAsync("読み込み中", style: new LoadingStyle());
}
