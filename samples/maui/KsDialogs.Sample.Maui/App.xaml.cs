namespace KsDialogs.Sample.Maui;

/// <summary>Sample アプリのエントリポイント。</summary>
public partial class App : Application
{
    /// <summary>アプリを初期化する。</summary>
    public App()
    {
        InitializeComponent();
    }

    /// <inheritdoc/>
    protected override Window CreateWindow(IActivationState? activationState)
    {
        // メニュー画面はタイトル帯を自前で描くため、Shell / NavigationPage は挟まない
        return new Window(new SampleMenuPage());
    }
}
