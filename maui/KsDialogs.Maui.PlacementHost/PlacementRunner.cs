using System.Text;
using Microsoft.Maui.Controls;

namespace KsDialogs.PlacementHost;

/// <summary>
/// シナリオを順に実行し、ダイアログの位置と基準になるはずの領域を突き合わせる。
/// </summary>
/// <remarks>
/// 判定は右下 (End / End) に寄せたダイアログの右端・下端が、期待する領域の右端・下端から余白分内側にあるか
/// (ページの上にバーがある構成では、左上に寄せたダイアログの左端・上端も同じように見る)。
/// 結果は 1 シナリオ 1 行で「KSDPLACEMENT」を頭に付けてコンソールへ出し、最後に件数の行を出す。
/// </remarks>
internal sealed class PlacementRunner
{
    /// <summary>ログ行の頭に付ける印。</summary>
    public const string LogTag = "KSDPLACEMENT";

    private static readonly TimeSpan s_stable = TimeSpan.FromMilliseconds(400);

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
