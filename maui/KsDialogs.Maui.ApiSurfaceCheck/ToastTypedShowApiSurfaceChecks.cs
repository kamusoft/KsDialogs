using System;
using Microsoft.Maui.Controls;

namespace KsDialogs.ApiSurfaceCheck;

/// <summary>ViewModel の型を渡す表示で、利用者が書くのと同じ形の Toast の ViewModel。</summary>
public sealed class ConsumerTypedToastViewModel : IToastViewModel
{
    /// <summary>中身の View に表示する文言。表示の直前に configure から設定する。</summary>
    public string Message { get; set; } = string.Empty;
}

/// <summary>
/// ViewModel の型を渡す Toast の表示の公開 API 形状と、オーバーロード束縛の正の検証。
/// </summary>
/// <remarks>
/// このファイルがコンパイルできることが検証結果である。既存の入口 (メッセージ入口・
/// インスタンス渡し・その場の factory) を同じ場所に並べてあるため、型を渡す形の追加で
/// 既存の呼び出しの解決が壊れれば、あいまいさ (CS0121) か引数の不一致としてここで落ちる。
/// </remarks>
public static class ToastTypedShowApiSurfaceChecks
{
    /// <summary>ViewModel factory を登録できる。View factory とはスロットが別で共存する。</summary>
    /// <param name="registry">紐付けを持つレジストリ。</param>
    public static void TS_YM_01_AcceptsViewModelFactoryRegistration(ToastViewRegistry registry)
    {
        registry.Register((ConsumerTypedToastViewModel viewModel) => new Label { Text = viewModel.Message });
        registry.RegisterViewModel(() => new ConsumerTypedToastViewModel());
    }

    /// <summary>configure を省略した型指定 show を書ける。</summary>
    /// <param name="toast">表示の入口。</param>
    public static void TS_YM_01_ShowsTypedWithoutConfigure(IKsToast toast) =>
        toast.Show<ConsumerTypedToastViewModel>();

    /// <summary>configure を渡す型指定 show を書ける。</summary>
    /// <param name="toast">表示の入口。</param>
    public static void TS_YM_01_ShowsTypedWithConfigure(IKsToast toast) =>
        toast.Show<ConsumerTypedToastViewModel>(viewModel => viewModel.Message = "保存しました");

    /// <summary>configure を明示的な型の関数として渡せる。</summary>
    /// <param name="toast">表示の入口。</param>
    public static void TS_YM_01_AcceptsExplicitlyTypedConfigureDelegate(IKsToast toast)
    {
        Action<ConsumerTypedToastViewModel> configure = viewModel => viewModel.Message = "保存しました";

        toast.Show(configure);
    }

    /// <summary>duration だけを指定した型指定 show を書ける。</summary>
    /// <param name="toast">表示の入口。</param>
    public static void TS_YM_01_ShowsTypedWithDurationOnly(IKsToast toast) =>
        toast.Show<ConsumerTypedToastViewModel>(durationMs: 2500);

    /// <summary>配置だけを指定した型指定 show を書ける。</summary>
    /// <param name="toast">表示の入口。</param>
    public static void TS_YM_01_ShowsTypedWithPlacementOnly(IKsToast toast) =>
        toast.Show<ConsumerTypedToastViewModel>(
            placement: new DialogPlacement { VerticalAlignment = DialogAlignment.End });

    /// <summary>configure・duration・配置をすべて指定した型指定 show を書ける。</summary>
    /// <param name="toast">表示の入口。</param>
    public static void TS_YM_01_ShowsTypedWithConfigureDurationAndPlacement(IKsToast toast) =>
        toast.Show<ConsumerTypedToastViewModel>(
            viewModel => viewModel.Message = "保存しました",
            2500,
            new DialogPlacement { OffsetY = -24d });

    /// <summary>メッセージ入口とインスタンス渡しは、型を渡す形の追加後もそのまま書ける。</summary>
    /// <param name="toast">表示の入口。</param>
    public static void TS_YM_01_KeepsTheMessageAndInstanceEntries(IKsToast toast)
    {
        ConsumerTypedToastViewModel viewModel = new() { Message = "保存しました" };

        toast.Show("保存しました");
        toast.Show("保存しました", 2500);
        toast.Show(viewModel);
        toast.Show(viewModel, 2500);
    }

    /// <summary>その場の factory を渡す show も、型を渡す形の追加後もそのまま書ける。</summary>
    /// <param name="toast">表示の入口。</param>
    public static void TS_YM_01_KeepsTheInlineFactoryEntry(IKsToast toast)
    {
        ConsumerTypedToastViewModel viewModel = new() { Message = "保存しました" };

        toast.Show(viewModel, model => new Label { Text = model.Message });
        toast.Show(viewModel, model => new Label { Text = model.Message }, 2500);
    }
}
