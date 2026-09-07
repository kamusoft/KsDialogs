using System.Collections.Generic;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Graphics;

namespace KsDialogs.ApiSurfaceCheck;

/// <summary>利用者が書くのと同じ形のカスタム Toast の ViewModel。</summary>
/// <param name="message">中身の View に表示する文言。</param>
public sealed class ConsumerToastViewModel(string message) : IToastViewModel
{
    /// <summary>中身の View に表示する文言。</summary>
    public string Message { get; } = message;
}

/// <summary>利用者が書くのと同じ形のカスタム Toast の中身の View。</summary>
public sealed class ConsumerToastView : ContentView;

/// <summary>
/// Toast の公開 API 形状の正の検証。
/// </summary>
/// <remarks>
/// このファイルがコンパイルできることが検証結果であり、公開すべき型・メンバが
/// 利用者から見えなくなればビルドが失敗する。
/// </remarks>
public static class DialogToastApiSurfaceChecks
{
    /// <summary>既定エントリと DI 注入のどちらも契約として扱える。</summary>
    /// <returns>契約として扱った 2 つの入口。</returns>
    public static IReadOnlyList<IKsToast> TS_MA_01_UsesSharedEntryAndInjectedInstance() =>
        [Toast.Instance, new Toast()];

    /// <summary>デフォルト View は duration も配置も省略して表示できる。</summary>
    /// <param name="toast">表示の入口。</param>
    public static void TS_MA_01_ShowsMessageOnly(IKsToast toast) => toast.Show("保存しました");

    /// <summary>デフォルト View は duration だけを指定して表示できる。</summary>
    /// <param name="toast">表示の入口。</param>
    public static void TS_MA_01_ShowsMessageWithDuration(IKsToast toast) => toast.Show("保存しました", 2500);

    /// <summary>デフォルト View は duration と配置を指定して表示できる。</summary>
    /// <param name="toast">表示の入口。</param>
    public static void TS_MA_01_ShowsMessageWithDurationAndPlacement(IKsToast toast) =>
        toast.Show(
            "保存しました",
            2500,
            new DialogPlacement { VerticalAlignment = DialogAlignment.End, OffsetY = -80d });

    /// <summary>一括設定は 1 つの値オブジェクトで行える (表示 API の引数では渡せない)。</summary>
    /// <param name="toast">表示の入口。</param>
    public static void TS_MA_01_ConfiguresStyle(IKsToast toast) =>
        toast.Style = new ToastStyle
        {
            BackgroundColor = Colors.Black.WithAlpha(0.92f),
            TextColor = Colors.White,
            FontSize = 16d,
            CornerRadius = 20d,
            DefaultDuration = 2000,
            DefaultPlacement = new DialogPlacement
            {
                VerticalAlignment = DialogAlignment.End,
                OffsetY = -80d,
            },
        };

    /// <summary>カスタム Toast はレジストリへ登録して ViewModel のインスタンスで表示できる。</summary>
    /// <param name="toast">表示の入口。</param>
    public static void TS_MA_01_RegistersAndShowsCustomToast(IKsToast toast)
    {
        toast.Registry.Register<ConsumerToastViewModel>(
            viewModel => new ConsumerToastView { BindingContext = viewModel });
        toast.Show(new ConsumerToastViewModel("保存しました"));
        toast.Show(new ConsumerToastViewModel("保存しました"), 2500);
    }

    /// <summary>カスタム Toast は登録せずにその場の factory でも表示できる。</summary>
    /// <param name="toast">表示の入口。</param>
    public static void TS_MA_01_ShowsWithInlineFactory(IKsToast toast) =>
        toast.Show(
            new ConsumerToastViewModel("保存しました"),
            viewModel => new ConsumerToastView { BindingContext = viewModel },
            2500,
            new DialogPlacement { VerticalAlignment = DialogAlignment.Start, OffsetY = 80d });

    /// <summary>カスタム Toast は DI チェーン上の 1 行登録でも配線できる。</summary>
    /// <param name="services">アプリのサービス集合。</param>
    /// <returns>チェーンできるサービス集合。</returns>
    public static IServiceCollection TS_MA_01_RegistersWithOneLineSugar(IServiceCollection services) =>
        services
            .AddKsDialogs()
            .RegisterForToast<ConsumerToastView, ConsumerToastViewModel>();

    /// <summary>配置と演出の添付はダイアログと同じ添付プロパティで供給できる。</summary>
    /// <param name="contentView">カスタム Toast の中身になる View。</param>
    public static void TS_MA_01_AttachesAttributesOnToastContentView(View contentView)
    {
        Dialog.SetHorizontalAlignment(contentView, DialogAlignment.Center);
        Dialog.SetOffsetY(contentView, -80d);
        Dialog.SetTransition(contentView, DialogTransition.Fade());
    }
}
