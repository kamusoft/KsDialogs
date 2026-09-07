using System.Threading.Tasks;
using Microsoft.Maui.Controls;

namespace KsDialogs.ApiSurfaceCheck.NegativeChecks;

/// <summary>値型は ViewModel として登録も型指定 show もできない (core/ADR-0018)。</summary>
/// <remarks>
/// 結果報告口はインスタンスの同一性で紐付くため、boxing のたびに同一性が失われる値型は
/// ViewModel にできない。C# では登録・型指定 show の class 制約でこれを表す。
/// このソースはビルドプロパティ KsDialogsNegativeCheckValueTypeViewModel を true にしたときだけ加わり、
/// <b>コンパイルエラーで失敗すること</b>が期待結果になる。
/// 期待する診断: CS0452 が 2 件 (ConsumerValueDialogViewModel は参照型でなければならない。
/// 登録と型指定 show の両方の呼び出し面で同じ制約に当たるため 2 件出る)
/// </remarks>
internal readonly struct ConsumerValueDialogViewModel : IDialogViewModel
{
}

internal static class RejectsValueTypeViewModel
{
    public static void MB_MA_02_RejectsValueTypeViewModelRegistration(DialogViewRegistry registry)
    {
        registry.Register((ConsumerValueDialogViewModel _) => new Label());
    }

    public static async Task MB_MA_02_RejectsValueTypeViewModelTypedShow(IKsDialog dialogs)
    {
        _ = await dialogs.ShowAsync<ConsumerValueDialogViewModel>();
    }
}
