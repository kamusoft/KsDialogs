using KsDialogs;
using Microsoft.Maui.Hosting;

namespace MyApp;

/// <summary>消費者検証アプリの組み立て。</summary>
public static class MauiProgram
{
    /// <summary>アプリを構成して起動可能な状態にする。</summary>
    /// <returns>組み立てた MAUI アプリ。</returns>
    public static MauiApp CreateMauiApp()
    {
        MauiAppBuilder builder = MauiApp.CreateBuilder();

        builder.UseMauiApp<App>();

        // 配布物が提供する組み込みの 1 行。コンテナ連携の公開面もコンパイル対象に入れる
        builder.Services.AddKsDialogs();

        return builder.Build();
    }
}
