using Microsoft.Maui.Controls;

namespace KsDialogs.ApiSurfaceCheck.NegativeChecks;

/// <summary>真偽値の顔を持たない ViewModel は、真偽値既定の登録に渡せない (core/ADR-0012)。</summary>
/// <remarks>
/// このソースはビルドプロパティ KsDialogsNegativeCheckSimpleRegister を true にしたときだけ加わり、
/// <b>コンパイルエラーで失敗すること</b>が期待結果になる。
/// 期待する診断: CS0311 (ConsumerTextDialogViewModel を IDialogViewModel へ変換できない)
/// </remarks>
internal static class RejectsCustomResultViewModelOnSimpleRegister
{
    public static void Register(DialogViewRegistry registry)
    {
        registry.Register<ConsumerTextDialogViewModel>((_, _) => new Label());
    }
}
