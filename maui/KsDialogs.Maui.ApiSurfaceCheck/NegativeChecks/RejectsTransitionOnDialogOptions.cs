using Microsoft.Maui.Controls;

namespace KsDialogs.ApiSurfaceCheck.NegativeChecks;

/// <summary>
/// 静的メタ属性の添付面は出入りの演出を持たない (演出は第3の添付スロット。core/ADR-0015・core/ADR-0017)。
/// </summary>
/// <remarks>
/// このソースはビルドプロパティ KsDialogsNegativeCheckOptionsTransition を true にしたときだけ加わり、
/// <b>コンパイルエラーで失敗すること</b>が期待結果になる。
/// 演出を静的メタ属性へ畳み込んだ場合に生えるはずの項目 (覆いや中身の時間を表すスカラーの添付) を突き、
/// そうした項目が添付面に無いことを示す。
/// 期待する診断: CS0117 (Dialog に TransitionDuration の定義がない)
/// </remarks>
internal static class RejectsTransitionOnDialogOptions
{
    public static object? Read(View contentView) => Dialog.GetTransitionDuration(contentView);
}
