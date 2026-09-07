using System.Collections.Generic;
using System.Threading.Tasks;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Graphics;

namespace KsDialogs.ApiSurfaceCheck;

/// <summary>進捗の受け口を実装した、利用者が書くのと同じ形の Loading の ViewModel。</summary>
public sealed class ConsumerProgressLoadingViewModel : ILoadingViewModel, ILoadingProgressReceiver
{
    /// <summary>最後に受け取った進捗。</summary>
    public double Progress { get; private set; }

    /// <inheritdoc/>
    public void OnProgress(double progress) => Progress = progress;
}

/// <summary>利用者が書くのと同じ形のカスタム Loading の中身の View。</summary>
public sealed class ConsumerLoadingView : ContentView;

/// <summary>
/// Loading の公開 API 形状の正の検証。
/// </summary>
/// <remarks>
/// このファイルがコンパイルできることが検証結果であり、公開すべき型・メンバが
/// 利用者から見えなくなればビルドが失敗する。
/// </remarks>
public static class DialogLoadingApiSurfaceChecks
{
    /// <summary>既定エントリと DI 注入のどちらも契約として扱える。</summary>
    /// <returns>契約として扱った 2 つの入口。</returns>
    public static IReadOnlyList<IKsLoading> LD_MA_01_UsesSharedEntryAndInjectedInstance() =>
        [Loading.Instance, new Loading()];

    /// <summary>既定ローディングはメッセージも置き場所も省略して表示できる。</summary>
    /// <param name="loading">表示の入口。</param>
    /// <returns>開始の完了。</returns>
    public static Task LD_MA_01_ShowsWithoutArguments(IKsLoading loading) => loading.ShowAsync();

    /// <summary>既定ローディングはメッセージと置き場所を指定して表示できる。</summary>
    /// <param name="loading">表示の入口。</param>
    /// <returns>開始の完了。</returns>
    public static Task LD_MA_01_ShowsWithMessageAndPlacement(IKsLoading loading) =>
        loading.ShowAsync(
            "読み込み中",
            new DialogPlacement { VerticalAlignment = DialogAlignment.End, OffsetY = -24d });

    /// <summary>表示は閉じられ、メッセージは表示中に更新できる。</summary>
    /// <param name="loading">表示の入口。</param>
    /// <returns>撤去の完了。</returns>
    public static Task LD_MA_01_SetsMessageAndHides(IKsLoading loading)
    {
        loading.SetMessage("あと少しです");
        return loading.HideAsync();
    }

    /// <summary>スコープ形は値を返さない処理を受け取れる。</summary>
    /// <param name="loading">表示の入口。</param>
    /// <returns>処理の完了。</returns>
    public static Task LD_MA_01_StartsScopeWithoutValue(IKsLoading loading) =>
        loading.StartAsync(
            async progress =>
            {
                progress.Report(0.5d);
                await Task.Yield();
            },
            "読み込み中");

    /// <summary>スコープ形は処理の戻り値をそのまま返す。</summary>
    /// <param name="loading">表示の入口。</param>
    /// <returns>処理の戻り値。</returns>
    public static Task<IReadOnlyList<string>> LD_MA_01_StartsScopeWithValue(IKsLoading loading) =>
        loading.StartAsync<IReadOnlyList<string>>(
            async progress =>
            {
                progress.Report(1d);
                await Task.Yield();
                return ["結果"];
            },
            placement: new DialogPlacement { OffsetY = 12d });

    /// <summary>見た目は 1 つの値オブジェクトで一括設定できる (表示 API の引数では渡せない)。</summary>
    /// <param name="loading">表示の入口。</param>
    public static void LD_MA_01_ConfiguresStyleAndOptions(IKsLoading loading)
    {
        loading.Style = new LoadingStyle
        {
            IndicatorColor = Colors.White,
            MessageFontSize = 16d,
            MessageColor = Colors.White,
            DefaultMessage = "処理中",
            ProgressFormat = (message, progress) =>
                progress is double value ? $"{message} {value:P0}" : message ?? string.Empty,
        };
        loading.Options = new DialogOptions
        {
            LayoutArea = DialogLayoutArea.Window,
            OverlayColor = Colors.Black.WithAlpha(0.6f),
        };
    }

    /// <summary>カスタム Loading はレジストリへ登録して ViewModel のインスタンスで表示できる。</summary>
    /// <param name="loading">表示の入口。</param>
    /// <returns>開始の完了。</returns>
    public static Task LD_MA_01_RegistersAndShowsCustomLoading(IKsLoading loading)
    {
        loading.Registry.Register<ConsumerProgressLoadingViewModel>(
            viewModel => new ConsumerLoadingView { BindingContext = viewModel });
        return loading.ShowAsync(new ConsumerProgressLoadingViewModel());
    }

    /// <summary>カスタム Loading は登録せずにその場の factory でも表示できる。</summary>
    /// <param name="loading">表示の入口。</param>
    /// <returns>処理の完了。</returns>
    public static Task LD_MA_01_StartsScopeWithInlineFactory(IKsLoading loading) =>
        loading.StartAsync(
            new ConsumerProgressLoadingViewModel(),
            viewModel => new ConsumerLoadingView { BindingContext = viewModel },
            progress =>
            {
                progress.Report(0.25d);
                return Task.CompletedTask;
            });

    /// <summary>カスタム Loading は DI チェーン上の 1 行登録でも配線できる。</summary>
    /// <param name="services">アプリのサービス集合。</param>
    /// <returns>チェーンできるサービス集合。</returns>
    public static IServiceCollection LD_MA_01_RegistersWithOneLineSugar(IServiceCollection services) =>
        services
            .AddKsDialogs()
            .RegisterForLoading<ConsumerLoadingView, ConsumerProgressLoadingViewModel>();

    /// <summary>メタ属性の添付はダイアログと同じ添付プロパティで供給できる。</summary>
    /// <param name="contentView">カスタム Loading の中身になる View。</param>
    public static void LD_MA_01_AttachesAttributesOnLoadingContentView(View contentView)
    {
        Dialog.SetLayoutArea(contentView, DialogLayoutArea.Window);
        Dialog.SetOverlayColor(contentView, Colors.Black);
        Dialog.SetHorizontalAlignment(contentView, DialogAlignment.Start);
    }
}
