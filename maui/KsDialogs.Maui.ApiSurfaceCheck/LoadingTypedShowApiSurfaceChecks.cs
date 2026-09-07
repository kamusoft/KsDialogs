using System;
using System.Threading.Tasks;
using Microsoft.Maui.Controls;

namespace KsDialogs.ApiSurfaceCheck;

/// <summary>ViewModel の型を渡す表示で、利用者が書くのと同じ形の Loading の ViewModel。</summary>
public sealed class ConsumerTypedLoadingViewModel : ILoadingViewModel
{
    /// <summary>中身の View に表示する文言。表示の直前に configure から設定する。</summary>
    public string Message { get; set; } = string.Empty;

    /// <summary>非同期の configure から呼ぶ、状態を整える処理。</summary>
    /// <returns>整え終わったこと。</returns>
    public Task PrepareAsync()
    {
        Message = "非同期で用意";
        return Task.CompletedTask;
    }

    /// <summary>整え終わりを表す待機対象。式の lambda を書くときの本体に使う。</summary>
    public Task Preparation => PrepareAsync();
}

/// <summary>
/// ViewModel の型を渡す Loading の表示の公開 API 形状と、オーバーロード束縛の正の検証。
/// </summary>
/// <remarks>
/// このファイルがコンパイルできることが検証結果である。既存の入口 (メッセージ入口・
/// インスタンス渡し・その場の factory) を同じ場所に並べてあるため、型を渡す形の追加で
/// 既存の呼び出しの解決が壊れれば、あいまいさ (CS0121) か引数の不一致としてここで落ちる。
/// <para>
/// 各検証は戻り値の静的な型を明示的な変数で受けており、意図しないオーバーロードへ束縛されれば
/// 型が合わずに落ちる。configure に <c>Task</c> を返す式の lambda を渡す形は、
/// <see cref="Action{T}"/> 側へ束縛されると本体が文にならず落ちるため、
/// <see cref="Func{T, TResult}"/> 側への束縛の証明になる。
/// </para>
/// </remarks>
public static class LoadingTypedShowApiSurfaceChecks
{
    /// <summary>ViewModel factory を登録できる。View factory とはスロットが別で共存する。</summary>
    /// <param name="registry">紐付けを持つレジストリ。</param>
    public static void LD_YM_01_AcceptsViewModelFactoryRegistration(LoadingViewRegistry registry)
    {
        registry.Register((ConsumerTypedLoadingViewModel viewModel) =>
            new Label { Text = viewModel.Message });
        registry.RegisterViewModel(() => new ConsumerTypedLoadingViewModel());
    }

    /// <summary>configure を省略した型指定 show は値を返さない完了になる。</summary>
    /// <param name="loading">表示の入口。</param>
    /// <returns>開始の完了。</returns>
    public static Task LD_YM_01_ShowsTypedWithoutConfigure(IKsLoading loading)
    {
        Task started = loading.ShowAsync<ConsumerTypedLoadingViewModel>();
        return started;
    }

    /// <summary>同期の configure を渡す型指定 show を書ける。</summary>
    /// <param name="loading">表示の入口。</param>
    /// <returns>開始の完了。</returns>
    public static Task LD_YM_01_ShowsTypedWithSynchronousConfigure(IKsLoading loading)
    {
        Task started = loading.ShowAsync<ConsumerTypedLoadingViewModel>(
            viewModel => viewModel.Message = "読み込み中");
        return started;
    }

    /// <summary>非同期の configure を渡す型指定 show を書ける。</summary>
    /// <param name="loading">表示の入口。</param>
    /// <returns>開始の完了。</returns>
    public static Task LD_YM_01_ShowsTypedWithAsynchronousConfigure(IKsLoading loading)
    {
        Task started = loading.ShowAsync<ConsumerTypedLoadingViewModel>(
            async viewModel =>
            {
                await Task.Yield();
                viewModel.Message = "非同期で用意";
            });
        return started;
    }

    /// <summary>
    /// <c>Task</c> を返す式の configure は、非同期の configure を受ける形へ束縛される。
    /// </summary>
    /// <remarks>
    /// この形の lambda は同期の configure を受ける形へは束縛できない (本体が文にならない)。
    /// </remarks>
    /// <param name="loading">表示の入口。</param>
    /// <returns>開始の完了。</returns>
    public static Task LD_YM_01_BindsTheTaskReturningConfigureToTheAsynchronousOverload(IKsLoading loading)
    {
        Task started = loading.ShowAsync<ConsumerTypedLoadingViewModel>(
            viewModel => viewModel.Preparation);
        return started;
    }

    /// <summary>同期・非同期それぞれの configure を、明示的な型の関数として渡せる。</summary>
    /// <param name="loading">表示の入口。</param>
    /// <returns>2 つの開始の完了。</returns>
    public static async Task LD_YM_01_AcceptsExplicitlyTypedConfigureDelegates(IKsLoading loading)
    {
        Action<ConsumerTypedLoadingViewModel> synchronous = viewModel => viewModel.Message = "同期";
        Func<ConsumerTypedLoadingViewModel, Task> asynchronous = viewModel => viewModel.PrepareAsync();

        await loading.ShowAsync(synchronous);
        await loading.ShowAsync(asynchronous);
    }

    /// <summary>置き場所だけを指定した型指定 show を書ける。</summary>
    /// <param name="loading">表示の入口。</param>
    /// <returns>開始の完了。</returns>
    public static Task LD_YM_01_ShowsTypedWithPlacementOnly(IKsLoading loading)
    {
        Task started = loading.ShowAsync<ConsumerTypedLoadingViewModel>(
            placement: new DialogPlacement { VerticalAlignment = DialogAlignment.End });
        return started;
    }

    /// <summary>configure と置き場所の両方を指定した型指定 show を書ける。</summary>
    /// <param name="loading">表示の入口。</param>
    /// <returns>開始の完了。</returns>
    public static Task LD_YM_01_ShowsTypedWithConfigureAndPlacement(IKsLoading loading)
    {
        Task started = loading.ShowAsync<ConsumerTypedLoadingViewModel>(
            viewModel => viewModel.Message = "読み込み中",
            new DialogPlacement { OffsetY = -24d });
        return started;
    }

    /// <summary>値を返さない型指定 start は、configure の 3 形すべてで書ける。</summary>
    /// <param name="loading">表示の入口。</param>
    /// <returns>処理の完了。</returns>
    public static async Task LD_YM_01_StartsTypedWithoutValue(IKsLoading loading)
    {
        Task withoutConfigure = loading.StartAsync<ConsumerTypedLoadingViewModel>(
            progress =>
            {
                progress.Report(1d);
                return Task.CompletedTask;
            });
        Task withSynchronousConfigure = loading.StartAsync<ConsumerTypedLoadingViewModel>(
            _ => Task.CompletedTask,
            viewModel => viewModel.Message = "同期");
        Task withAsynchronousConfigure = loading.StartAsync<ConsumerTypedLoadingViewModel>(
            _ => Task.CompletedTask,
            viewModel => viewModel.PrepareAsync());

        await withoutConfigure;
        await withSynchronousConfigure;
        await withAsynchronousConfigure;
    }

    /// <summary>値を返す型指定 start は、configure の 3 形すべてで処理の戻り値の型を保つ。</summary>
    /// <param name="loading">表示の入口。</param>
    /// <returns>3 回分の処理の戻り値。</returns>
    public static async Task<int> LD_YM_01_StartsTypedWithValue(IKsLoading loading)
    {
        Task<int> withoutConfigure = loading.StartAsync<ConsumerTypedLoadingViewModel, int>(
            _ => Task.FromResult(1));
        Task<int> withSynchronousConfigure = loading.StartAsync<ConsumerTypedLoadingViewModel, int>(
            _ => Task.FromResult(2),
            viewModel => viewModel.Message = "同期");
        Task<int> withAsynchronousConfigure = loading.StartAsync<ConsumerTypedLoadingViewModel, int>(
            _ => Task.FromResult(3),
            viewModel => viewModel.PrepareAsync());

        return await withoutConfigure + await withSynchronousConfigure + await withAsynchronousConfigure;
    }

    /// <summary>メッセージ入口の値を返す start は、型引数 1 個のまま処理の戻り値の型を保つ。</summary>
    /// <param name="loading">表示の入口。</param>
    /// <returns>処理の戻り値。</returns>
    public static async Task<string> LD_YM_01_KeepsTheMessageEntryStartWithValue(IKsLoading loading)
    {
        Task<string> running = loading.StartAsync(_ => Task.FromResult("結果"), "読み込み中");
        return await running;
    }

    /// <summary>インスタンス渡しの show と start は、型を渡す形の追加後もそのまま書ける。</summary>
    /// <param name="loading">表示の入口。</param>
    /// <returns>表示と処理の完了。</returns>
    public static async Task LD_YM_01_KeepsTheInstanceEntries(IKsLoading loading)
    {
        ConsumerTypedLoadingViewModel viewModel = new();

        await loading.ShowAsync(viewModel);
        await loading.StartAsync(viewModel, _ => Task.CompletedTask);
        int value = await loading.StartAsync(viewModel, _ => Task.FromResult(42));
        viewModel.Message = value.ToString();
    }

    /// <summary>その場の factory を渡す show と start も、型を渡す形の追加後もそのまま書ける。</summary>
    /// <param name="loading">表示の入口。</param>
    /// <returns>表示と処理の完了。</returns>
    public static async Task LD_YM_01_KeepsTheInlineFactoryEntries(IKsLoading loading)
    {
        ConsumerTypedLoadingViewModel viewModel = new();

        await loading.ShowAsync(viewModel, model => new Label { Text = model.Message });
        await loading.StartAsync(viewModel, model => new Label { Text = model.Message }, _ => Task.CompletedTask);
        int value = await loading.StartAsync(
            viewModel,
            model => new Label { Text = model.Message },
            _ => Task.FromResult(42));
        viewModel.Message = value.ToString();
    }
}
