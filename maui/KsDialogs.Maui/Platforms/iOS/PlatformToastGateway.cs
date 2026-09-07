using Foundation;
using KsDialogs.Bridge;

namespace KsDialogs;

/// <summary>
/// iOS の ObjC 互換面 (Toast) へ委譲する面。
/// </summary>
/// <remarks>
/// 表示中のリスト・重なり順・各表示の計時は Native ライブラリの coordinator が持ち、
/// この面は MAUI View の platform view 化と値の写しだけを行う (core/ADR-0030)。
/// </remarks>
internal sealed class PlatformToastGateway : IToastGateway
{
    /// <inheritdoc/>
    public void ApplyStyle(ToastStyle style) =>
        MauiToastBridge.Shared.ApplyStyle(ToBridgeStyle(style));

    /// <inheritdoc/>
    public void Show(ToastPresentationRequest request) =>
        MauiToastBridge.Shared.Show(ToBridgeContent(request));

    /// <summary>表示の要求を、互換面が受け取る中身の指定へ写す。</summary>
    /// <remarks>
    /// デフォルト View では中身の供給が無く、メッセージと duration・置き場所だけを渡す。
    /// カスタム View では中身の生成を供給元として渡し、器が提示先を確保した後に呼ばれる。
    /// 供給元は互換面 (ObjC) から呼ばれるため、失敗を例外のまま境界へ返さず
    /// <see langword="null"/> として返す (<see cref="BridgeContentSupply"/>)。
    /// </remarks>
    /// <param name="request">その表示の中身と供給値。</param>
    /// <returns>互換面へ渡す中身の指定。</returns>
    private static MauiToastContent ToBridgeContent(ToastPresentationRequest request)
    {
        MauiDialogPlacement? placement = request.ShowPlacement is null
            ? null
            : PlatformDialogContent.ToBridgePlacement(request.ShowPlacement);
        NSNumber? duration = request.DurationMilliseconds is int milliseconds
            ? NSNumber.FromInt32(milliseconds)
            : null;

        return request.IsBuiltin
            ? new MauiToastContent(request.Message ?? string.Empty, duration, placement)
            : new MauiToastContent(
                () => BridgeContentSupply.CreateOrDiscard(
                    () => PlatformDialogContent.Create(
                        request.CreateContent(),
                        // 供給元が呼ばれるのは器が提示先を確保した後なので、この時点では文脈が取れる
                        PlatformDialogContent.ResolveMauiContext()
                            ?? throw new DialogException.PresentationHostUnavailable())),
                duration,
                placement);
    }

    /// <summary>Toast の一括設定を、互換面が受け取る形へ写す。</summary>
    /// <remarks>見えの実装は Native 実装の責務なので、ここでは表現を変えるだけにする。</remarks>
    /// <param name="style">設定されたスタイル。</param>
    /// <returns>互換面へ渡すスタイル。</returns>
    private static MauiToastStyle ToBridgeStyle(ToastStyle style) => new()
    {
        BackgroundColorArgb = style.BackgroundColorArgb,
        TextColorArgb = style.TextColorArgb,
        FontSize = style.FontSize,
        CornerRadius = style.CornerRadius,
        DefaultDuration = style.DefaultDuration,
        DefaultPlacement = style.DefaultPlacement is null
            ? null
            : PlatformDialogContent.ToBridgePlacement(style.DefaultPlacement),
    };
}
