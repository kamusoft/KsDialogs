using Microsoft.Maui.Controls;

namespace KsDialogs.Maui.Tests.Support;

/// <summary>基準領域「表示中のページ」を XAML の添付プロパティで宣言した検証用の中身。</summary>
public partial class CurrentPageAttributeTestView : ContentView
{
    /// <summary>XAML の宣言どおりに初期化する。</summary>
    public CurrentPageAttributeTestView()
    {
        InitializeComponent();
    }
}
