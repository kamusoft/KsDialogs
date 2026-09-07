namespace KsDialogs.Sample.Maui;

/// <summary>Model Dialog の中身。</summary>
/// <remarks>
/// コンストラクタが受け取るのは表示対象の ViewModel だけで、1 行登録
/// (<c>RegisterForDialog</c>) がその値をそのまま渡す。
/// </remarks>
public partial class ModelDialogCardView : ContentView
{
    private readonly ModelDialogViewModel _viewModel;

    /// <summary>カードを組み立てる。</summary>
    /// <param name="viewModel">表示と報告を受け持つ ViewModel。</param>
    public ModelDialogCardView(ModelDialogViewModel viewModel)
    {
        InitializeComponent();
        _viewModel = viewModel;
    }

    private void OnCancelClicked(object? sender, EventArgs e) => _viewModel.Cancel();

    private void OnCompleteClicked(object? sender, EventArgs e) => _viewModel.Complete();
}
