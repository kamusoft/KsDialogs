using System.Text;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Graphics;

namespace KsDialogs.PlacementHost;

/// <summary>
/// シナリオを順に実行し、表示された中身の位置と大きさを期待する値と突き合わせる。
/// </summary>
/// <remarks>
/// 位置のシナリオ (<see cref="PlacementScenario"/>) は、右下 (End / End) に寄せたダイアログの右端・下端が、
/// 期待する領域の右端・下端から余白分内側にあるかを見る
/// (ページの上にバーがある構成では、左上に寄せたダイアログの左端・上端も同じように見る)。
/// 大きさのシナリオ (<see cref="ContentSizeScenario"/>) は、Dialog / Loading / Toast で出した中身のルートが
/// 宣言した大きさで表示されているかを見る。
/// 結果は 1 シナリオ 1 行で「KSDPLACEMENT」を頭に付けてコンソールへ出し、最後に件数の行を出す。
/// </remarks>
internal sealed class PlacementRunner
{
    /// <summary>ログ行の頭に付ける印。</summary>
    public const string LogTag = "KSDPLACEMENT";

    private static readonly TimeSpan s_stable = TimeSpan.FromMilliseconds(400);

    /// <summary>大きさを測る Toast の表示時間。現れて落ち着くまで待ってから測れる長さにする。</summary>
    private const int ToastDurationMs = 3000;

    private readonly List<string> _lines = [];
    private int _passed;
    private int _failed;

    /// <summary>全シナリオを実行し、結果の画面を表示する。</summary>
    /// <param name="window">ページ構成を組む画面。</param>
    public async Task RunAsync(Window window)
    {
        PlacementProbeViewModel.Register();
        foreach (PlacementScenario scenario in PlacementScenario.All)
        {
            try
            {
                await RunScenarioAsync(window, scenario);
            }
            catch (Exception error)
            {
                Report(scenario.Name, passed: false, $"exception={error.GetType().Name}: {error.Message}");
            }
            finally
            {
                DialogCurrentPage.Provider = null;
            }
        }

        ContentPage sizePage = new() { Title = "Content size", Content = new Label { Text = "Content size", Margin = 16 } };
        window.Page = sizePage;
        await PlacementWaiting.UntilRenderedAsync(sizePage);
        foreach (ContentSizeScenario scenario in ContentSizeScenario.All)
        {
            try
            {
                await RunSizeScenarioAsync(scenario);
            }
            catch (Exception error)
            {
                Report(scenario.Name, passed: false, $"exception={error.GetType().Name}: {error.Message}");
            }
        }

        string summary = $"{LogTag}|{DeviceInfo.Platform}|SUMMARY|passed={_passed}|failed={_failed}";
        Console.WriteLine(summary);
        _lines.Add(summary);
        window.Page = new ContentPage
        {
            Title = "Result",
            Content = new ScrollView
            {
                Content = new Label { Text = string.Join("\n\n", _lines), Margin = 16, FontSize = 11 },
            },
        };
    }

    private async Task RunScenarioAsync(Window window, PlacementScenario scenario)
    {
        PlacementCheck check = await scenario.SetUpAsync(window);
        DialogCurrentPage.Provider = check.Provider;
        try
        {
            PlacementRect? area = await PlacementWaiting.UntilStableAsync(
                () => check.ExpectedElement() is VisualElement element ? PlacementMeasurement.ExpectedArea(element) : null,
                s_stable);
            if (area is not PlacementRect expectedArea || expectedArea.IsEmpty)
            {
                Report(scenario.Name, passed: false, "the expected element was not rendered");
                return;
            }

            PlacementRect? dialog = await ShowAndMeasureAsync(DialogLayoutArea.CurrentPage, DialogAlignment.End);
            if (dialog is not PlacementRect dialogRect)
            {
                Report(scenario.Name, passed: false, "the dialog was not placed");
                return;
            }

            StringBuilder detail = new($"dialog={dialogRect}|area={expectedArea}");
            bool passed =
                Math.Abs(dialogRect.Right - (expectedArea.Right - PlacementMeasurement.Margin)) <= PlacementMeasurement.Tolerance
                && Math.Abs(dialogRect.Bottom - (expectedArea.Bottom - PlacementMeasurement.Margin)) <= PlacementMeasurement.Tolerance;

            VisualElement expectedElement = check.ExpectedElement()!;
            PlacementRect? visible = PlacementMeasurement.VisibleArea(expectedElement);
            detail.Append($"|visible={visible}");

            if (check.ExpectsBarBelowPage)
            {
                // ページの下にタブバーがあることの確認。無ければこの画面では基準領域の違いを見分けられない
                bool barBelow = visible is PlacementRect v && expectedArea.Bottom < v.Bottom - PlacementMeasurement.Margin;
                detail.Append($"|barBelowPage={barBelow}");
                passed &= barBelow;
            }

            if (check.ExpectsBarAbovePage)
            {
                // 上端は右下寄せの位置に現れないため、左上 (Start / Start) に寄せたダイアログでも確かめる
                bool barAbove = visible is PlacementRect v && expectedArea.Top > v.Top + PlacementMeasurement.Tolerance;
                PlacementRect? startDialog = await ShowAndMeasureAsync(DialogLayoutArea.CurrentPage, DialogAlignment.Start);
                bool startPlaced = startDialog is PlacementRect sd
                    && Math.Abs(sd.Top - (expectedArea.Top + PlacementMeasurement.Margin)) <= PlacementMeasurement.Tolerance
                    && Math.Abs(sd.Left - (expectedArea.Left + PlacementMeasurement.Margin)) <= PlacementMeasurement.Tolerance;
                detail.Append($"|barAbovePage={barAbove}|startDialog={startDialog}|startPlaced={startPlaced}");
                passed &= barAbove && startPlaced;
            }

            if (check.UnderneathArea is not null || check.Underneath is not null)
            {
                PlacementRect? underArea = check.UnderneathArea
                    ?? PlacementMeasurement.ExpectedArea(check.Underneath!);
                bool distinct = underArea is PlacementRect u && !u.Matches(expectedArea, PlacementMeasurement.Tolerance);
                detail.Append($"|underneath={underArea}|distinct={distinct}");
                passed &= distinct;
            }

            if (check.ComparesWithVisibleArea)
            {
                PlacementRect? visibleDialog = await ShowAndMeasureAsync(DialogLayoutArea.VisibleArea, DialogAlignment.End);
                bool same = visibleDialog is PlacementRect vd && vd.Matches(dialogRect, PlacementMeasurement.Tolerance);
                detail.Append($"|visibleAreaDialog={visibleDialog}|sameAsVisibleArea={same}");
                passed &= same;
            }

            Report(scenario.Name, passed, detail.ToString());
        }
        finally
        {
            await check.TearDownAsync();
        }
    }

    /// <summary>中身を出し、表示された大きさが宣言した大きさと一致するかを確かめてから閉じる。</summary>
    private async Task RunSizeScenarioAsync(ContentSizeScenario scenario)
    {
        View content = scenario.CreateContent();
        Func<Task> close = await PresentAsync(scenario.Feature, content);
        try
        {
            PlacementRect? shown = await PlacementWaiting.UntilStableAsync(
                () => PlacementMeasurement.ContentRect(content),
                s_stable);
            if (shown is not PlacementRect rect)
            {
                Report(scenario.Name, passed: false, "the content was not shown");
                return;
            }

            // 比較は OS の単位 (Android は px) で行い、ログには MAUI の単位へ戻した値も出す
            double scale = PlacementMeasurement.Scale;
            double width = rect.Right - rect.Left;
            double height = rect.Bottom - rect.Top;
            bool shownMatches =
                Math.Abs(width - (scenario.Expected.Width * scale)) <= PlacementMeasurement.Tolerance
                && Math.Abs(height - (scenario.Expected.Height * scale)) <= PlacementMeasurement.Tolerance;
            // ルート自身の Width / Height も見る。ルートを配置する親がいないと未設定 (-1) のまま残る
            double boundsTolerance = PlacementMeasurement.Tolerance / scale;
            bool boundsMatch =
                Math.Abs(content.Width - scenario.Expected.Width) <= boundsTolerance
                && Math.Abs(content.Height - scenario.Expected.Height) <= boundsTolerance;
            bool passed = shownMatches && boundsMatch;
            StringBuilder detail = new(
                $"shown={width / scale:0.#}x{height / scale:0.#}|expected={scenario.Expected.Width:0.#}x{scenario.Expected.Height:0.#}"
                + $"|rootBounds={content.Width:0.#}x{content.Height:0.#}");
            if (scenario.ExpectedOuterRatio is Size ratio)
            {
                passed &= CheckOuter(content, rect, ratio, detail);
            }

            Report(scenario.Name, passed, detail.ToString());
        }
        finally
        {
            await close();
        }
    }

    /// <summary>
    /// ダイアログの外形が比率指定と fill のとおりに決まり、宣言サイズのルートがその中央に置かれているかを確かめる。
    /// </summary>
    /// <remarks>
    /// 外形はルートの platform view の親 (器が中身として受け取る View) の矩形で測る。
    /// 基準領域は可視領域で、比率はその軸長に、fill は余白を控除した軸長に対して効く。
    /// </remarks>
    /// <param name="content">中身のルート。</param>
    /// <param name="root">ルートの矩形。</param>
    /// <param name="ratio">外形の期待値。0 より大きい軸は可視領域に対する比率、0 の軸は fill。</param>
    /// <param name="detail">ログへ足す詳細。</param>
    /// <returns>外形と中央寄せがどちらも期待どおりなら <see langword="true"/>。</returns>
    private static bool CheckOuter(View content, PlacementRect root, Size ratio, StringBuilder detail)
    {
        PlacementRect? outer = PlacementMeasurement.OuterRect(content);
        PlacementRect? visible = PlacementMeasurement.VisibleArea(content);
        if (outer is not PlacementRect o || visible is not PlacementRect v)
        {
            detail.Append("|outer=unknown");
            return false;
        }

        double margin = PlacementMeasurement.Margin;
        double expectedWidth = ratio.Width > 0 ? ratio.Width * (v.Right - v.Left) : (v.Right - v.Left) - (2 * margin);
        double expectedHeight = ratio.Height > 0 ? ratio.Height * (v.Bottom - v.Top) : (v.Bottom - v.Top) - (2 * margin);
        bool outerMatches =
            Math.Abs((o.Right - o.Left) - expectedWidth) <= PlacementMeasurement.Tolerance
            && Math.Abs((o.Bottom - o.Top) - expectedHeight) <= PlacementMeasurement.Tolerance;
        // 両側の隙間の差で中央を見る。丸めで 1 単位ずれうるため、差は許容誤差の 2 倍まで許す
        bool centered =
            Math.Abs((root.Left - o.Left) - (o.Right - root.Right)) <= 2 * PlacementMeasurement.Tolerance
            && Math.Abs((root.Top - o.Top) - (o.Bottom - root.Bottom)) <= 2 * PlacementMeasurement.Tolerance;
        detail.Append(
            $"|outer={o}|root={root}|visible={v}|expectedOuter={expectedWidth:0.#}x{expectedHeight:0.#}"
            + $"|outerMatches={outerMatches}|rootCentered={centered}");
        return outerMatches && centered;
    }

    /// <summary>機能ごとの表示の入口で中身を出し、閉じる操作を返す。</summary>
    /// <param name="feature">中身を出す機能。</param>
    /// <param name="content">出す中身のルート。</param>
    /// <returns>表示を閉じ、閉じ終わるまで待つ操作。</returns>
    private static async Task<Func<Task>> PresentAsync(ContentSizeFeature feature, View content)
    {
        ContentSizeProbeViewModel viewModel = new();
        switch (feature)
        {
            case ContentSizeFeature.Dialog:
                Task<DialogResult<bool>> showing = Dialog.Instance.ShowAsync<ContentSizeProbeViewModel>(
                    viewModel,
                    (probe, notifier) =>
                    {
                        probe.Notifier = notifier;
                        return content;
                    });
                return async () =>
                {
                    viewModel.Notifier?.Complete(true);
                    await showing;
                };

            case ContentSizeFeature.Loading:
                await Loading.Instance.ShowAsync(viewModel, _ => content);
                return Loading.Instance.HideAsync;

            default:
                // Toast は閉じる手段を持たないため、時間切れで画面から外れるまで待つ
                Toast.Instance.Show(viewModel, _ => content, ToastDurationMs);
                return () => PlacementWaiting.UntilDetachedAsync(content);
        }
    }

    /// <summary>指定の基準領域と配置でダイアログを出し、位置が落ち着いたところで測ってから閉じる。</summary>
    private static async Task<PlacementRect?> ShowAndMeasureAsync(DialogLayoutArea area, DialogAlignment alignment)
    {
        PlacementProbeViewModel viewModel = new(area, alignment);
        Task<DialogResult<bool>> showing = Dialog.Instance.ShowAsync(viewModel);
        PlacementRect? placed = await PlacementWaiting.UntilStableAsync(
            () => PlacementMeasurement.ContentRect(viewModel.Content),
            s_stable);
        viewModel.Close();
        await showing;
        return placed;
    }

    private void Report(string scenario, bool passed, string detail)
    {
        string line = $"{LogTag}|{DeviceInfo.Platform}|{scenario}|{(passed ? "PASS" : "FAIL")}|{detail}";
        Console.WriteLine(line);
        _lines.Add(line);
        if (passed)
        {
            _passed++;
        }
        else
        {
            _failed++;
        }
    }
}
