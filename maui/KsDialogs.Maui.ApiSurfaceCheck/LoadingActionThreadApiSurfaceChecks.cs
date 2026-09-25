using System.Threading.Tasks;

namespace KsDialogs.ApiSurfaceCheck;

/// <summary>
/// スコープ形の処理を始めるスレッドの指定 (<see cref="LoadingActionThread"/>) の公開 API 形状と、
/// オーバーロード束縛の正の検証。
/// </summary>
/// <remarks>
/// このファイルがコンパイルできることが検証結果である。10 本の <c>StartAsync</c> を、指定なし・
/// <see cref="LoadingActionThread.Main"/>・<see cref="LoadingActionThread.Background"/> の 3 通りで書き、
/// 各呼び出しの戻り値を静的な型を明示した変数で受ける。指定を足したことで別のオーバーロードへ
/// 束縛されるようになれば、型が合わないかあいまいさ (CS0121) としてここで落ちる。
/// </remarks>
public static class LoadingActionThreadApiSurfaceChecks
{
    /// <summary>既定ローディングの値を返さないスコープ形は、3 通りとも同じ形で書ける。</summary>
    /// <param name="loading">表示の入口。</param>
    /// <returns>3 回分の処理の完了。</returns>
    public static async Task LD_HM_01_StartsTheDefaultLoadingWithoutValue(IKsLoading loading)
    {
        Task unspecified = loading.StartAsync(_ => Task.CompletedTask, "読み込み中");
        Task onMain = loading.StartAsync(_ => Task.CompletedTask, actionThread: LoadingActionThread.Main);
        Task offMain = loading.StartAsync(
            _ => Task.CompletedTask,
            "読み込み中",
            new DialogPlacement { OffsetY = 12d },
            actionThread: LoadingActionThread.Background);

        await unspecified;
        await onMain;
        await offMain;
    }

    /// <summary>既定ローディングの値を返すスコープ形は、3 通りとも処理の戻り値の型を保つ。</summary>
    /// <param name="loading">表示の入口。</param>
    /// <returns>3 回分の処理の戻り値の合計。</returns>
    public static async Task<int> LD_HM_01_StartsTheDefaultLoadingWithValue(IKsLoading loading)
    {
        Task<int> unspecified = loading.StartAsync(_ => Task.FromResult(1));
        Task<int> onMain = loading.StartAsync(_ => Task.FromResult(2), actionThread: LoadingActionThread.Main);
        Task<int> offMain = loading.StartAsync(
            _ => Task.FromResult(3),
            "読み込み中",
            actionThread: LoadingActionThread.Background);

        return await unspecified + await onMain + await offMain;
    }

    /// <summary>インスタンス渡しのスコープ形は、値の有無どちらも 3 通りで書ける。</summary>
    /// <param name="loading">表示の入口。</param>
    /// <param name="viewModel">表示するカスタム Loading の ViewModel。</param>
    /// <returns>値を返す 3 回分の処理の戻り値の合計。</returns>
    public static async Task<int> LD_HM_01_StartsTheRegisteredLoading(
        IKsLoading loading,
        ConsumerProgressLoadingViewModel viewModel)
    {
        Task unspecified = loading.StartAsync(viewModel, _ => Task.CompletedTask);
        Task onMain = loading.StartAsync(viewModel, _ => Task.CompletedTask, actionThread: LoadingActionThread.Main);
        Task offMain = loading.StartAsync(
            viewModel,
            _ => Task.CompletedTask,
            new DialogPlacement { OffsetY = 12d },
            actionThread: LoadingActionThread.Background);
        Task<int> unspecifiedValue = loading.StartAsync(viewModel, _ => Task.FromResult(1));
        Task<int> onMainValue = loading.StartAsync(
            viewModel,
            _ => Task.FromResult(2),
            actionThread: LoadingActionThread.Main);
        Task<int> offMainValue = loading.StartAsync(
            viewModel,
            _ => Task.FromResult(3),
            actionThread: LoadingActionThread.Background);

        await unspecified;
        await onMain;
        await offMain;
        return await unspecifiedValue + await onMainValue + await offMainValue;
    }

    /// <summary>その場の factory を渡すスコープ形は、値の有無どちらも 3 通りで書ける。</summary>
    /// <param name="loading">表示の入口。</param>
    /// <param name="viewModel">表示するカスタム Loading の ViewModel。</param>
    /// <returns>値を返す 3 回分の処理の戻り値の合計。</returns>
    public static async Task<int> LD_HM_01_StartsTheInlineFactoryLoading(
        IKsLoading loading,
        ConsumerProgressLoadingViewModel viewModel)
    {
        Task unspecified = loading.StartAsync(viewModel, _ => new ConsumerLoadingView(), _ => Task.CompletedTask);
        Task onMain = loading.StartAsync(
            viewModel,
            _ => new ConsumerLoadingView(),
            _ => Task.CompletedTask,
            actionThread: LoadingActionThread.Main);
        Task offMain = loading.StartAsync(
            viewModel,
            _ => new ConsumerLoadingView(),
            _ => Task.CompletedTask,
            new DialogPlacement { OffsetY = 12d },
            LoadingActionThread.Background);
        Task<int> unspecifiedValue = loading.StartAsync(
            viewModel,
            _ => new ConsumerLoadingView(),
            _ => Task.FromResult(1));
        Task<int> onMainValue = loading.StartAsync(
            viewModel,
            _ => new ConsumerLoadingView(),
            _ => Task.FromResult(2),
            actionThread: LoadingActionThread.Main);
        Task<int> offMainValue = loading.StartAsync(
            viewModel,
            _ => new ConsumerLoadingView(),
            _ => Task.FromResult(3),
            actionThread: LoadingActionThread.Background);

        await unspecified;
        await onMain;
        await offMain;
        return await unspecifiedValue + await onMainValue + await offMainValue;
    }

    /// <summary>型指定の値を返さないスコープ形は、同期・非同期の configure のどちらでも 3 通りで書ける。</summary>
    /// <param name="loading">表示の入口。</param>
    /// <returns>処理の完了。</returns>
    public static async Task LD_HM_01_StartsTheTypedLoadingWithoutValue(IKsLoading loading)
    {
        Task synchronousUnspecified = loading.StartAsync<ConsumerTypedLoadingViewModel>(
            _ => Task.CompletedTask,
            viewModel => viewModel.Message = "同期");
        Task synchronousOnMain = loading.StartAsync<ConsumerTypedLoadingViewModel>(
            _ => Task.CompletedTask,
            viewModel => viewModel.Message = "同期",
            actionThread: LoadingActionThread.Main);
        Task synchronousOffMain = loading.StartAsync<ConsumerTypedLoadingViewModel>(
            _ => Task.CompletedTask,
            viewModel => viewModel.Message = "同期",
            actionThread: LoadingActionThread.Background);
        Task asynchronousUnspecified = loading.StartAsync<ConsumerTypedLoadingViewModel>(
            _ => Task.CompletedTask,
            viewModel => viewModel.PrepareAsync());
        Task asynchronousOnMain = loading.StartAsync<ConsumerTypedLoadingViewModel>(
            _ => Task.CompletedTask,
            viewModel => viewModel.PrepareAsync(),
            actionThread: LoadingActionThread.Main);
        Task asynchronousOffMain = loading.StartAsync<ConsumerTypedLoadingViewModel>(
            _ => Task.CompletedTask,
            viewModel => viewModel.PrepareAsync(),
            new DialogPlacement { OffsetY = 12d },
            LoadingActionThread.Background);
        // configure を省略して指定だけを渡す形は、同期の configure を受ける形へ束縛される
        Task withoutConfigure = loading.StartAsync<ConsumerTypedLoadingViewModel>(
            _ => Task.CompletedTask,
            actionThread: LoadingActionThread.Background);

        await synchronousUnspecified;
        await synchronousOnMain;
        await synchronousOffMain;
        await asynchronousUnspecified;
        await asynchronousOnMain;
        await asynchronousOffMain;
        await withoutConfigure;
    }

    /// <summary>型指定の値を返すスコープ形は、同期・非同期の configure のどちらでも 3 通りで戻り値の型を保つ。</summary>
    /// <param name="loading">表示の入口。</param>
    /// <returns>処理の戻り値の合計。</returns>
    public static async Task<int> LD_HM_01_StartsTheTypedLoadingWithValue(IKsLoading loading)
    {
        Task<int> synchronousUnspecified = loading.StartAsync<ConsumerTypedLoadingViewModel, int>(
            _ => Task.FromResult(1),
            viewModel => viewModel.Message = "同期");
        Task<int> synchronousOnMain = loading.StartAsync<ConsumerTypedLoadingViewModel, int>(
            _ => Task.FromResult(2),
            viewModel => viewModel.Message = "同期",
            actionThread: LoadingActionThread.Main);
        Task<int> synchronousOffMain = loading.StartAsync<ConsumerTypedLoadingViewModel, int>(
            _ => Task.FromResult(3),
            viewModel => viewModel.Message = "同期",
            actionThread: LoadingActionThread.Background);
        Task<int> asynchronousUnspecified = loading.StartAsync<ConsumerTypedLoadingViewModel, int>(
            _ => Task.FromResult(4),
            viewModel => viewModel.PrepareAsync());
        Task<int> asynchronousOnMain = loading.StartAsync<ConsumerTypedLoadingViewModel, int>(
            _ => Task.FromResult(5),
            viewModel => viewModel.PrepareAsync(),
            actionThread: LoadingActionThread.Main);
        Task<int> asynchronousOffMain = loading.StartAsync<ConsumerTypedLoadingViewModel, int>(
            _ => Task.FromResult(6),
            viewModel => viewModel.PrepareAsync(),
            new DialogPlacement { OffsetY = 12d },
            LoadingActionThread.Background);

        return await synchronousUnspecified + await synchronousOnMain + await synchronousOffMain
            + await asynchronousUnspecified + await asynchronousOnMain + await asynchronousOffMain;
    }
}
