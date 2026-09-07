namespace KsDialogs.Sample.Maui;

/// <summary>Sample のメニュー画面。</summary>
public partial class SampleMenuPage : ContentPage
{
    /// <summary>進捗を報告する刻みの数。0 から 1 までをこの数で割った値を順に報告する。</summary>
    private const int LoadingStepCount = 4;

    /// <summary>メッセージを差し替える刻み。</summary>
    private const int LoadingMessageUpdateStep = 2;

    /// <summary>刻みごとの待ち時間 (ミリ秒) の既定値。</summary>
    private const int DefaultLoadingStepIntervalMilliseconds = 400;

    /// <summary>Toast を重ねて置くときの上方向オフセット (論理単位) の下段。契約既定と同じ高さ。</summary>
    private const double ToastLowerOffsetY = -80;

    /// <summary>重ねて置くときの中段。</summary>
    private const double ToastMiddleOffsetY = -160;

    /// <summary>重ねて置くときの上段。</summary>
    private const double ToastUpperOffsetY = -240;

    /// <summary>上部中央へ置くときの下方向オフセット (論理単位)。</summary>
    private const double ToastTopOffsetY = 80;

    /// <summary>Custom Toast の登録経路の表示時間 (ミリ秒)。</summary>
    private const int CustomToastRegisteredDurationMs = 3000;

    /// <summary>Custom Toast のインライン経路の表示時間 (ミリ秒)。登録経路より先に消える値にする。</summary>
    private const int CustomToastInlineDurationMs = 2000;

    /// <summary>Toast Stack の 1 枚目の表示時間 (ミリ秒)。</summary>
    private const int ToastStackFirstDurationMs = 2000;

    /// <summary>Toast Stack の 2 枚目の表示時間 (ミリ秒)。</summary>
    private const int ToastStackSecondDurationMs = 3000;

    /// <summary>Toast Stack の 3 枚目の表示時間 (ミリ秒)。</summary>
    private const int ToastStackThirdDurationMs = 4000;

    /// <summary>Toast Placement の表示時間 (ミリ秒)。配置を見比べられるよう既定より長くする。</summary>
    private const int ToastPlacementDurationMs = 3000;

    /// <summary>Toast Overlap の Toast の表示時間 (ミリ秒)。Dialog と Loading の時系列を跨ぐ長さにする。</summary>
    private const int ToastOverlapToastDurationMs = 10000;

    /// <summary>Toast Overlap の Dialog を出しておく時間 (ミリ秒)。</summary>
    private const int ToastOverlapDialogDurationMs = 2000;

    /// <summary>Toast Overlap の Loading を出しておく時間 (ミリ秒)。</summary>
    private const int ToastOverlapLoadingDurationMs = 2000;

    /// <summary>Toast の満了を確実に過ぎてから結果を出すための余白 (ミリ秒)。</summary>
    private const int ToastOverlapResultMarginMs = 500;

    /// <summary>可視領域の上部中央へ置く配置。契約既定 (下部中央) と対になる位置。</summary>
    private static readonly DialogPlacement TopToastPlacement = new()
    {
        HorizontalAlignment = DialogAlignment.Center,
        VerticalAlignment = DialogAlignment.Start,
        OffsetY = ToastTopOffsetY,
    };

    /// <summary>刻みごとの待ち時間 (ミリ秒)。起動引数の指定があればその値になる。</summary>
    private readonly int _loadingStepIntervalMilliseconds =
        SampleCaptureOptions.Current.LoadingStepIntervalMilliseconds
        ?? DefaultLoadingStepIntervalMilliseconds;

    /// <summary>メニュー画面を組み立てる。</summary>
    public SampleMenuPage()
    {
        InitializeComponent();
    }

    /// <inheritdoc/>
    protected override void OnAppearing()
    {
        base.OnAppearing();
        AutoPlay();
    }

    /// <summary>起動引数で指定されたデモを、メニュー項目のタップと同じ入口で自動再生する。</summary>
    /// <remarks>再生はプロセスの起動につき 1 回だけで、画面が作り直されても繰り返さない。</remarks>
    private void AutoPlay()
    {
        if (SampleCaptureAutoPlay.ConsumeDemo() is not SampleDemoId demo)
        {
            return;
        }

        // ダイアログの提示先はメニューが画面に載ってから決まるため、再生もその時点まで待つ。
        // 再生を待機して例外を観測し、失敗をメニュー項目のタップと同じ倒れ方で表面化させる
        // (握り潰すと、自動再生が失敗した画面と定義外の ID を渡した画面が見分けられなくなる)
        Dispatcher.Dispatch(async () => await PlayAsync(demo));
    }

    /// <summary>メニュー項目のタップハンドラと同じ入口を呼ぶ。</summary>
    /// <param name="demo">自動再生するデモ。</param>
    /// <returns>再生の完了を表す待機可能な操作。</returns>
    private Task PlayAsync(SampleDemoId demo) => demo switch
    {
        SampleDemoId.BasicDialog => ShowBasicDialogAsync(),
        SampleDemoId.DeclarativeDialog => ShowDeclarativeDialogAsync(),
        SampleDemoId.ModelDialog => ShowModelDialogAsync(),
        SampleDemoId.TextInputDialog => ShowTextInputDialogAsync(),
        SampleDemoId.InlineDialog => ShowInlineDialogAsync(),
        SampleDemoId.TransitionDialog => OpenTransitionPanelAsync(),
        SampleDemoId.LayoutDialog => OpenLayoutPanelAsync(),
        SampleDemoId.DefaultLoading => RunDefaultLoadingAsync(),
        SampleDemoId.CustomLoading => RunCustomLoadingAsync(),
        SampleDemoId.DefaultToast => ShowDefaultToastAsync(),
        SampleDemoId.CustomToast => ShowCustomToastAsync(),
        SampleDemoId.ToastStack => ShowToastStackAsync(),
        SampleDemoId.ToastPlacement => ShowToastPlacementAsync(),
        SampleDemoId.ToastOverlap => RunToastOverlapAsync(),
        _ => Task.CompletedTask,
    };

    /// <summary>Basic Dialog を表示し、結果を直近の結果として取り込む。</summary>
    private async void OnBasicDialogSelected(object? sender, TappedEventArgs e) =>
        await ShowBasicDialogAsync();

    /// <summary>Declarative Dialog を表示し、結果を直近の結果として取り込む。</summary>
    private async void OnDeclarativeDialogSelected(object? sender, TappedEventArgs e) =>
        await ShowDeclarativeDialogAsync();

    /// <summary>Model Dialog を表示し、結果を直近の結果として取り込む。</summary>
    private async void OnModelDialogSelected(object? sender, TappedEventArgs e) =>
        await ShowModelDialogAsync();

    /// <summary>Text Input Dialog を表示し、入力された文字列を直近の結果として取り込む。</summary>
    private async void OnTextInputDialogSelected(object? sender, TappedEventArgs e) =>
        await ShowTextInputDialogAsync();

    /// <summary>Inline Dialog を表示し、結果を直近の結果として取り込む。</summary>
    private async void OnInlineDialogSelected(object? sender, TappedEventArgs e) =>
        await ShowInlineDialogAsync();

    /// <summary>Default Loading を実行し、完了を直近の結果として取り込む。</summary>
    private async void OnDefaultLoadingSelected(object? sender, TappedEventArgs e) =>
        await RunDefaultLoadingAsync();

    /// <summary>Custom Loading を実行し、完了を直近の結果として取り込む。</summary>
    private async void OnCustomLoadingSelected(object? sender, TappedEventArgs e) =>
        await RunCustomLoadingAsync();

    /// <summary>Default Toast を表示する。</summary>
    private async void OnDefaultToastSelected(object? sender, TappedEventArgs e) =>
        await ShowDefaultToastAsync();

    /// <summary>Custom Toast を表示する。</summary>
    private async void OnCustomToastSelected(object? sender, TappedEventArgs e) =>
        await ShowCustomToastAsync();

    /// <summary>Toast Stack を表示する。</summary>
    private async void OnToastStackSelected(object? sender, TappedEventArgs e) =>
        await ShowToastStackAsync();

    /// <summary>Toast Placement を表示する。</summary>
    private async void OnToastPlacementSelected(object? sender, TappedEventArgs e) =>
        await ShowToastPlacementAsync();

    /// <summary>Toast Overlap を実行し、時系列の完了を直近の結果として取り込む。</summary>
    private async void OnToastOverlapSelected(object? sender, TappedEventArgs e) =>
        await RunToastOverlapAsync();

    /// <summary>レイアウト属性の調整パネルを開く。</summary>
    private async void OnLayoutDialogSelected(object? sender, TappedEventArgs e) =>
        await OpenLayoutPanelAsync();

    /// <summary>トランジションデモ画面を開く。</summary>
    private async void OnTransitionDialogSelected(object? sender, TappedEventArgs e) =>
        await OpenTransitionPanelAsync();

    /// <summary>Basic Dialog を表示し、結果を直近の結果として取り込む。</summary>
    /// <returns>表示から結果の取り込みまでを表す待機可能な操作。</returns>
    private async Task ShowBasicDialogAsync()
    {
        BasicDialogViewModel viewModel = new(SampleText.BasicDialogMessage);
        DialogResult<bool> result = await Dialog.Instance.ShowAsync(viewModel);

        ShowResult(result switch
        {
            DialogResult<bool>.Completed completed => SampleText.CompletedResult(completed.Value),
            _ => SampleText.CancelledResult,
        });
    }

    /// <summary>Declarative Dialog を表示し、結果を直近の結果として取り込む。</summary>
    /// <returns>表示から結果の取り込みまでを表す待機可能な操作。</returns>
    /// <remarks>中身が MAUI View で書かれていても、呼び出し方も結果の返り方も Basic Dialog と変わらない。</remarks>
    private async Task ShowDeclarativeDialogAsync()
    {
        DeclarativeDialogViewModel viewModel = new(SampleText.DeclarativeDialogMessage);
        DialogResult<bool> result = await Dialog.Instance.ShowAsync(viewModel);

        ShowResult(result switch
        {
            DialogResult<bool>.Completed completed => SampleText.CompletedResult(completed.Value),
            _ => SampleText.CancelledResult,
        });
    }

    /// <summary>Model Dialog を表示し、結果を直近の結果として取り込む。</summary>
    /// <returns>表示から結果の取り込みまでを表す待機可能な操作。</returns>
    /// <remarks>
    /// ViewModel のインスタンスは渡さず、型と configure だけを渡す。
    /// 生成は 1 行登録が配線した ViewModel factory が行い、結果は ViewModel 自身が報告する。
    /// </remarks>
    private async Task ShowModelDialogAsync()
    {
        DialogResult<bool> result = await Dialog.Instance.ShowAsync<ModelDialogViewModel>(
            viewModel => viewModel.Message = SampleText.ModelDialogMessage);

        ShowResult(result switch
        {
            DialogResult<bool>.Completed completed => SampleText.CompletedResult(completed.Value),
            _ => SampleText.CancelledResult,
        });
    }

    /// <summary>Text Input Dialog を表示し、入力された文字列を直近の結果として取り込む。</summary>
    /// <returns>表示から結果の取り込みまでを表す待機可能な操作。</returns>
    private async Task ShowTextInputDialogAsync()
    {
        TextInputDialogViewModel viewModel = new(SampleText.TextInputDialogMessage);
        DialogResult<string> result = await Dialog.Instance.ShowAsync(viewModel);

        ShowResult(result switch
        {
            DialogResult<string>.Completed completed => SampleText.CompletedResult(completed.Value),
            _ => SampleText.CancelledResult,
        });
    }

    /// <summary>Inline Dialog を表示し、結果を直近の結果として取り込む。</summary>
    /// <returns>表示から結果の取り込みまでを表す待機可能な操作。</returns>
    /// <remarks>中身はこの場で渡すため、この ViewModel 型はレジストリに登録していない (core/ADR-0013)。</remarks>
    private async Task ShowInlineDialogAsync()
    {
        InlineDialogViewModel viewModel = new(SampleText.InlineDialogMessage);
        DialogResult<bool> result = await Dialog.Instance.ShowAsync(
            viewModel,
            (suppliedViewModel, notifier) => new InlineDialogCardView(
                suppliedViewModel.Message,
                onCancel: notifier.Cancel,
                onComplete: () => notifier.Complete(true)));

        ShowResult(result switch
        {
            DialogResult<bool>.Completed completed => SampleText.CompletedResult(completed.Value),
            _ => SampleText.CancelledResult,
        });
    }

    /// <summary>Default Loading を実行し、完了を直近の結果として取り込む。</summary>
    /// <returns>実行から結果の取り込みまでを表す待機可能な操作。</returns>
    /// <remarks>
    /// スコープ形の StartAsync は処理の間だけ既定ローディングを出し、処理の完了で自動的に閉じる。
    /// 処理は 0 から 1 まで進捗を段階的に報告し、途中で表示中のメッセージを差し替える。
    /// </remarks>
    private async Task RunDefaultLoadingAsync()
    {
        await Loading.Instance.StartAsync(
            async progress =>
            {
                for (int step = 0; step <= LoadingStepCount; step++)
                {
                    progress.Report((double)step / LoadingStepCount);
                    if (step == LoadingMessageUpdateStep)
                    {
                        Loading.Instance.SetMessage(SampleText.LoadingUpdateMessage);
                    }

                    await Task.Delay(_loadingStepIntervalMilliseconds);
                }
            },
            SampleText.LoadingStartMessage);

        ShowResult(SampleText.LoadingCompletedResult);
    }

    /// <summary>Custom Loading を実行し、完了を直近の結果として取り込む。</summary>
    /// <returns>実行から結果の取り込みまでを表す待機可能な操作。</returns>
    /// <remarks>
    /// 呼び出しの形は Default Loading と同じスコープ形で、渡すのが登録済みの ViewModel の型である
    /// 点だけが違う。実体は 1 行登録が配線した ViewModel factory が作る。
    /// 報告した進捗は ViewModel の受け口へ転送され、中身のカスタム View がそれを読んで表示を更新する。
    /// </remarks>
    private async Task RunCustomLoadingAsync()
    {
        await Loading.Instance.StartAsync<CustomLoadingViewModel>(
            async progress =>
            {
                for (int step = 0; step <= LoadingStepCount; step++)
                {
                    progress.Report((double)step / LoadingStepCount);
                    await Task.Delay(_loadingStepIntervalMilliseconds);
                }
            });

        ShowResult(SampleText.LoadingCompletedResult);
    }

    /// <summary>Default Toast を表示する。</summary>
    /// <returns>表示の呼び出しを表す待機可能な操作。</returns>
    /// <remarks>
    /// duration も配置も渡さないので、デフォルト View が契約既定の配置に出て既定 duration で消える。
    /// Toast は fire-and-forget なので戻り値も待機もなく、結果表示も変えない (core/ADR-0031)。
    /// </remarks>
    private Task ShowDefaultToastAsync()
    {
        Toast.Instance.Show(SampleText.DefaultToastMessage);
        return Task.CompletedTask;
    }

    /// <summary>Custom Toast を表示する。</summary>
    /// <returns>表示の呼び出しを表す待機可能な操作。</returns>
    /// <remarks>
    /// 登録経路とインライン経路の 2 枚を続けて出す。同じ配置では重なって見分けられないため、
    /// 上方向オフセットを変えて 2 段に置く。
    /// </remarks>
    private Task ShowCustomToastAsync()
    {
        // 登録経路は型を渡し、実体は 1 行登録が配線した ViewModel factory が作る。文言は configure で入れる
        Toast.Instance.Show<CustomToastViewModel>(
            viewModel => viewModel.Message = SampleText.CustomToastMessage,
            CustomToastRegisteredDurationMs,
            BottomToastPlacement(ToastMiddleOffsetY));

        // 中身はこの場で渡すため、この ViewModel 型は登録していない (core/ADR-0013)
        InlineToastViewModel inline = new(SampleText.InlineToastMessage);
        Toast.Instance.Show(
            inline,
            suppliedViewModel => new InlineToastCardView(suppliedViewModel.Message),
            CustomToastInlineDurationMs,
            BottomToastPlacement(ToastLowerOffsetY));

        return Task.CompletedTask;
    }

    /// <summary>Toast Stack を表示する。</summary>
    /// <returns>表示の呼び出しを表す待機可能な操作。</returns>
    /// <remarks>
    /// 3 枚を続けて出し、duration の短いものから独立して消える様子を見せる。
    /// 3 枚目は長文で、デフォルト View が複数行に折り返して高さを伸ばすことを確かめる。
    /// </remarks>
    private Task ShowToastStackAsync()
    {
        Toast.Instance.Show(
            SampleText.ToastStackFirstMessage,
            ToastStackFirstDurationMs,
            BottomToastPlacement(ToastLowerOffsetY));
        Toast.Instance.Show(
            SampleText.ToastStackSecondMessage,
            ToastStackSecondDurationMs,
            BottomToastPlacement(ToastMiddleOffsetY));
        Toast.Instance.Show(
            SampleText.ToastStackThirdMessage,
            ToastStackThirdDurationMs,
            BottomToastPlacement(ToastUpperOffsetY));

        return Task.CompletedTask;
    }

    /// <summary>Toast Placement を表示する。Show の引数で契約既定と違う配置へ上書きする。</summary>
    /// <returns>表示の呼び出しを表す待機可能な操作。</returns>
    private Task ShowToastPlacementAsync()
    {
        Toast.Instance.Show(
            SampleText.ToastPlacementMessage,
            ToastPlacementDurationMs,
            TopToastPlacement);

        return Task.CompletedTask;
    }

    /// <summary>Toast Overlap を実行し、時系列の完了を直近の結果として取り込む。</summary>
    /// <returns>時系列の完了までを表す待機可能な操作。</returns>
    /// <remarks>
    /// 操作者に依存しない固定の時系列で自動進行する — Toast を出したまま Dialog を重ね、
    /// Dialog を閉じたあと Loading を重ねる。Loading は Toast より前面に出る (core/ADR-0030)。
    /// Loading が終わっても Toast は残っており、duration の満了で消えてから結果を出す。
    /// </remarks>
    private async Task RunToastOverlapAsync()
    {
        Toast.Instance.Show(SampleText.ToastOverlapMessage, ToastOverlapToastDurationMs);

        await ShowOverlapDialogAsync();

        await Loading.Instance.StartAsync(
            async _ => await Task.Delay(ToastOverlapLoadingDurationMs),
            SampleText.LoadingStartMessage);

        // Toast の残り時間 (と満了を跨ぐ余白) を待ってから結果を出す
        await Task.Delay(
            ToastOverlapToastDurationMs - ToastOverlapDialogDurationMs
            - ToastOverlapLoadingDurationMs + ToastOverlapResultMarginMs);
        ShowResult(SampleText.ToastOverlapCompletedResult);
    }

    /// <summary>Toast Overlap の Dialog を出し、一定時間後に自動で閉じる。</summary>
    /// <returns>表示から閉じるまでを表す待機可能な操作。</returns>
    /// <remarks>
    /// MAUI の Show は待機の打ち切り口を持たないため、表示中の ViewModel から結果報告口を引いて
    /// 閉じる (core/ADR-0018)。結果は使わない。
    /// </remarks>
    private async Task ShowOverlapDialogAsync()
    {
        BasicDialogViewModel viewModel = new(SampleText.BasicDialogMessage);
        Task<DialogResult<bool>> showTask = Dialog.Instance.ShowAsync(viewModel);
        await Task.Delay(ToastOverlapDialogDurationMs);
        viewModel.Notifier?.Cancel();
        await showTask;
    }

    /// <summary>可視領域の下部中央から上方向へ動かした配置。</summary>
    /// <param name="offsetY">上方向へ動かす量 (論理単位。負の値で上へ動く)。</param>
    /// <returns>組み立てた配置。</returns>
    private static DialogPlacement BottomToastPlacement(double offsetY) => new()
    {
        HorizontalAlignment = DialogAlignment.Center,
        VerticalAlignment = DialogAlignment.End,
        OffsetY = offsetY,
    };

    /// <summary>レイアウト属性の調整パネルを開く。</summary>
    /// <returns>画面遷移を表す待機可能な操作。</returns>
    private async Task OpenLayoutPanelAsync()
    {
        SampleLayoutPanelPage panel = new(ShowResult);
        await Navigation.PushModalAsync(panel);
    }

    /// <summary>トランジションデモ画面を開く。</summary>
    /// <returns>画面遷移を表す待機可能な操作。</returns>
    private async Task OpenTransitionPanelAsync()
    {
        SampleTransitionPanelPage panel = new(ShowResult);
        await Navigation.PushModalAsync(panel);
    }

    /// <summary>直近の結果を表示する。</summary>
    /// <param name="result">結果表示エリアに出す文言。</param>
    private void ShowResult(string result)
    {
        ResultValueLabel.Text = result;
        ResultArea.IsVisible = true;
    }
}
