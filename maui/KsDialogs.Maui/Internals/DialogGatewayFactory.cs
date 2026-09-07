namespace KsDialogs;

/// <summary>
/// 実行中の platform に対応する委譲面を作る。
/// </summary>
internal static class DialogGatewayFactory
{
    /// <summary>この platform の委譲面を返す。</summary>
    /// <returns>platform 実装があればその委譲面、無ければ提示先を持たない委譲面。</returns>
    public static IDialogGateway Create() =>
#if IOS || ANDROID
        new PlatformDialogGateway();
#else
        new HostlessDialogGateway();
#endif
}
