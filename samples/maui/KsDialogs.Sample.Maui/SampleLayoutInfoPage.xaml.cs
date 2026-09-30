namespace KsDialogs.Sample.Maui;

/// <summary>タイトルバーを持たないタブ。説明文と、パネルの設定をそのまま使う表示操作を置く。</summary>
public partial class SampleLayoutInfoPage : ContentPage
{
    private readonly Action _onShow;

    /// <summary>説明のタブを組み立てる。</summary>
    /// <param name="onShow">パネルの設定でダイアログを出す操作。</param>
    public SampleLayoutInfoPage(Action onShow)
    {
        InitializeComponent();
        _onShow = onShow;
    }

    private void OnShowSelected(object? sender, EventArgs e) => _onShow();
}
