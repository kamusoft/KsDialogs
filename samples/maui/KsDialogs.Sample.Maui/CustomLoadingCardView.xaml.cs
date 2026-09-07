using System.ComponentModel;

namespace KsDialogs.Sample.Maui;

/// <summary>Custom Loading の中身。</summary>
/// <remarks>
/// コンストラクタが受け取るのは表示対象の ViewModel だけで、Loading 版の 1 行登録
/// (<c>RegisterForLoading</c>) がその値をそのまま渡す。
/// 帯の長さは幅の比率で決まるため、文言の binding とは別に code-behind で塗り直す。
/// </remarks>
public partial class CustomLoadingCardView : ContentView
{
    private readonly CustomLoadingViewModel _viewModel;

    /// <summary>カードを組み立てる。</summary>
    /// <param name="viewModel">進捗を保持する ViewModel。</param>
    public CustomLoadingCardView(CustomLoadingViewModel viewModel)
    {
        InitializeComponent();
        _viewModel = viewModel;
        _viewModel.PropertyChanged += OnViewModelPropertyChanged;
        // 撤去された View が更新を受け取り続けないよう、購読を外す
        Unloaded += (_, _) => _viewModel.PropertyChanged -= OnViewModelPropertyChanged;
    }

    private void OnViewModelPropertyChanged(object? sender, PropertyChangedEventArgs e)
    {
        if (e.PropertyName == nameof(CustomLoadingViewModel.Progress))
        {
            UpdateProgressFill();
        }
    }

    private void OnProgressTrackSizeChanged(object? sender, EventArgs e) => UpdateProgressFill();

    /// <summary>進捗を帯の長さへ反映する。幅が決まる前は何もしない (決まった時点で呼び直される)。</summary>
    private void UpdateProgressFill()
    {
        double trackWidth = ProgressTrack.Width;
        if (trackWidth <= 0)
        {
            return;
        }

        ProgressFill.WidthRequest = trackWidth * _viewModel.Progress;
    }
}
