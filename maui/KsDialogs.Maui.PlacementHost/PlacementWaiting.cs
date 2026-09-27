using Microsoft.Maui.Controls;

namespace KsDialogs.PlacementHost;

/// <summary>画面の描画と配置が落ち着くのを待つ補助。</summary>
internal static class PlacementWaiting
{
    private static readonly TimeSpan s_interval = TimeSpan.FromMilliseconds(50);
    private static readonly TimeSpan s_timeout = TimeSpan.FromSeconds(10);

    /// <summary>要素が platform view を持つまで待つ。</summary>
    /// <param name="element">待つ要素。</param>
    public static async Task UntilRenderedAsync(VisualElement element)
    {
        DateTime deadline = DateTime.UtcNow + s_timeout;
        while (element.Handler?.PlatformView is null && DateTime.UtcNow < deadline)
        {
            await Task.Delay(s_interval);
        }
    }

    /// <summary>要素が画面から外れるまで待つ。</summary>
    /// <param name="element">待つ要素。</param>
    public static async Task UntilDetachedAsync(VisualElement element)
    {
        DateTime deadline = DateTime.UtcNow + s_timeout;
        while (PlacementMeasurement.ContentRect(element) is not null && DateTime.UtcNow < deadline)
        {
            await Task.Delay(s_interval);
        }
    }

    /// <summary>測った値が空でなく、一定時間続けて変わらなくなるまで待つ。</summary>
    /// <param name="measure">値を測る操作。測れなければ <see langword="null"/>。</param>
    /// <param name="stable">変わらないことを求める時間。</param>
    /// <returns>落ち着いた値。時間切れなら最後に測った値。</returns>
    public static async Task<PlacementRect?> UntilStableAsync(Func<PlacementRect?> measure, TimeSpan stable)
    {
        DateTime deadline = DateTime.UtcNow + s_timeout;
        PlacementRect? previous = null;
        DateTime heldSince = DateTime.UtcNow;
        while (DateTime.UtcNow < deadline)
        {
            PlacementRect? current = measure();
            if (current is null || current.Value.IsEmpty || current != previous)
            {
                heldSince = DateTime.UtcNow;
            }
            else if (DateTime.UtcNow - heldSince >= stable)
            {
                return current;
            }

            previous = current;
            await Task.Delay(s_interval);
        }

        return previous;
    }
}
