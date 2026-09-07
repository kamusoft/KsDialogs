namespace KsDialogs;

/// <summary>
/// 提示先を持たない実行環境の Toast の委譲面。
/// </summary>
/// <remarks>
/// iOS / Android の実装を持たない素の .NET 上には、Toast を載せられる画面が存在しない。
/// 提示環境の不在は Toast では失敗ではなく (呼び出しは成功し、提示先が現れないまま duration が
/// 満了した表示は表示されずに破棄される)、fire-and-forget の呼び出し面はそもそも結末を返さないため、
/// この面は受け取った要求を捨てるだけでよい。
/// </remarks>
internal sealed class HostlessToastGateway : IToastGateway
{
    /// <inheritdoc/>
    public void ApplyStyle(ToastStyle style)
    {
    }

    /// <inheritdoc/>
    public void Show(ToastPresentationRequest request)
    {
    }
}
