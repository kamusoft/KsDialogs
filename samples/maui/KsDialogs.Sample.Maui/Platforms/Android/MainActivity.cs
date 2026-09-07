using Android.App;
using Android.Content.PM;
using Android.OS;

namespace KsDialogs.Sample.Maui;

[Activity(Theme = "@style/Maui.SplashTheme", MainLauncher = true, LaunchMode = LaunchMode.SingleTop, ConfigurationChanges = ConfigChanges.ScreenSize | ConfigChanges.Orientation | ConfigChanges.UiMode | ConfigChanges.ScreenLayout | ConfigChanges.SmallestScreenSize | ConfigChanges.Density)]
public class MainActivity : MauiAppCompatActivity
{
    /// <inheritdoc/>
    protected override void OnCreate(Bundle? savedInstanceState)
    {
        // 画面の組み立ては base の中で始まるため、撮影支援設定の取り込みはその前に済ませる
        SampleCaptureArguments.Capture(Intent);
        base.OnCreate(savedInstanceState);
    }
}
