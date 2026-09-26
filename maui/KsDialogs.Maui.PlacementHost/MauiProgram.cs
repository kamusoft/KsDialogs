using Microsoft.Maui.Hosting;

namespace KsDialogs.PlacementHost;

/// <summary>テストホストアプリの組み立て。</summary>
public static class MauiProgram
{
    /// <summary>アプリを構成して起動可能な状態にする。</summary>
    /// <returns>組み立てた MAUI アプリ。</returns>
    public static MauiApp CreateMauiApp()
    {
        // ライブラリの診断 (Trace の警告) もシナリオの結果と同じコンソールで読めるようにする
        System.Diagnostics.Trace.Listeners.Add(new System.Diagnostics.ConsoleTraceListener());
        MauiAppBuilder builder = MauiApp.CreateBuilder();
        builder.UseMauiApp<PlacementHostApp>();
        builder.Services.AddKsDialogs();
        return builder.Build();
    }
}
