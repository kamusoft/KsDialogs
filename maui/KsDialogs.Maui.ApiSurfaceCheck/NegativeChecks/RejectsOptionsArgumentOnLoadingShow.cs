using System.Threading.Tasks;

namespace KsDialogs.ApiSurfaceCheck.NegativeChecks;

/// <summary>
/// 既定ローディングの表示 API に options 引数はない (器メタ属性はシングルトンの設定プロパティで
/// 渡す。表示 API から渡せるのは placement だけ。core/ADR-0015・ADR-0023)。
/// </summary>
/// <remarks>
/// このソースはビルドプロパティ KsDialogsNegativeCheckLoadingShowOptions を true にしたときだけ加わり、
/// <b>コンパイルエラーで失敗すること</b>が期待結果になる。
/// 期待する診断: CS1739 (options という名前のパラメーターがない)
/// </remarks>
internal static class RejectsOptionsArgumentOnLoadingShow
{
    public static Task Show(IKsLoading loading) =>
        loading.ShowAsync("読み込み中", options: new DialogOptions());
}
