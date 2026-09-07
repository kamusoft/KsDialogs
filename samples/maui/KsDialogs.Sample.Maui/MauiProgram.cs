using Microsoft.Extensions.Logging;

namespace KsDialogs.Sample.Maui;

/// <summary>Sample アプリの組み立て。</summary>
public static class MauiProgram
{
    /// <summary>アプリを構成して起動可能な状態にする。</summary>
    /// <returns>組み立てた MAUI アプリ。</returns>
    public static MauiApp CreateMauiApp()
    {
        MauiAppBuilder builder = MauiApp.CreateBuilder();
        builder
            .UseMauiApp<App>()
            .ConfigureFonts(fonts =>
            {
                fonts.AddFont("OpenSans-Regular.ttf", "OpenSansRegular");
                fonts.AddFont("OpenSans-Semibold.ttf", "OpenSansSemibold");
            });

#if DEBUG
        builder.Logging.AddDebug();
#endif

        // MAUI ルートはコンテナ連携の見本を担う。ライブラリの組み込みと 1 行登録を
        // DI チェーンの上で続けて書く (Model Dialog はこの 1 行だけで View と ViewModel が揃う)
        builder.Services
            .AddKsDialogs()
            .RegisterForDialog<ModelDialogCardView, ModelDialogViewModel>()
            // Loading 版の 1 行登録。紐付けは Loading 専用のレジストリに載る
            .RegisterForLoading<CustomLoadingCardView, CustomLoadingViewModel>()
            // Toast 版の 1 行登録。紐付けは Toast 専用のレジストリに載る
            .RegisterForToast<CustomToastCardView, CustomToastViewModel>();

        // 呼び出しごとに中身を組み立てる登録は、従来どおりレジストリへ直接書く
        SampleDialogRegistration.Register();

        return builder.Build();
    }
}
