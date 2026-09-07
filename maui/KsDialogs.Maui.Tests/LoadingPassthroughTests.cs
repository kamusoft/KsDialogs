using System;
using System.Threading.Tasks;
using KsDialogs.Maui.Tests.Support;
using Microsoft.Maui;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Graphics;
using NUnit.Framework;

namespace KsDialogs.Maui.Tests;

/// <summary>
/// Loading の表示 API に渡した値が、そのまま Native への委譲面へ届くことの検証。
/// </summary>
/// <remarks>
/// 合流・世代・進捗の丸め・見えの実装を持つのは Native 実装だけで、その全量検証も Native 側が
/// 受け持つ (core/ADR-0001・0024)。MAUI 形態に課されるのは輸送の値保存だけなので、ここでは
/// 「placement の各値・スタイルの各値・進捗値が無変換で委譲面へ渡ること」を見る。
/// </remarks>
[TestFixture]
public class LoadingPassthroughTests
{
    /// <summary>表示 API の placement 引数が、値を変えずに委譲面へ届く。</summary>
    [Test]
    [Description("[LD-MA-03] placement 引数が値のまま委譲面へ届く")]
    public async Task LD_MA_03_ThePlacementArgumentReachesTheGatewayUnchanged()
    {
        TestLoadingGateway gateway = new();
        IKsLoading loading = NewLoading(gateway);
        DialogPlacement placement = new()
        {
            HorizontalAlignment = DialogAlignment.End,
            VerticalAlignment = DialogAlignment.Fill,
            OffsetX = -12.5d,
            OffsetY = 34d,
        };

        await loading.ShowAsync("読み込み中", placement);

        LoadingPresentationRequest delegated = gateway.Requests[0];
        Assert.Multiple(() =>
        {
            Assert.That(delegated.IsBuiltin, Is.True);
            Assert.That(delegated.Message, Is.EqualTo("読み込み中"));
            Assert.That(delegated.ShowPlacement, Is.EqualTo(placement));
        });
    }

    /// <summary>カスタム View では、添付した placement を表示 API の引数がまるごと置換する。</summary>
    [Test]
    [Description("[LD-MA-03] カスタム View の添付は placement 引数で置換される")]
    public async Task LD_MA_03_TheShowArgumentReplacesTheAttachedPlacement()
    {
        TestLoadingGateway gateway = new();
        IKsLoading loading = NewLoading(gateway);
        PlainLoadingTestViewModel viewModel = new();
        DialogPlacement attached = new() { HorizontalAlignment = DialogAlignment.Start, OffsetX = 8d };
        DialogPlacement argument = new() { VerticalAlignment = DialogAlignment.End, OffsetY = -4d };

        await loading.ShowAsync(viewModel, _ => LayoutTestContentViews.WithPlacement(attached));
        await loading.ShowAsync(viewModel, _ => LayoutTestContentViews.WithPlacement(attached), argument);

        Assert.Multiple(() =>
        {
            Assert.That(gateway.Contents[0].Placement, Is.EqualTo(attached));
            Assert.That(gateway.Contents[1].Placement, Is.EqualTo(argument));
        });
    }

    /// <summary>スタイルの各値は、設定した形のまま委譲面へ届く。</summary>
    [Test]
    [Description("[LD-MA-03] LoadingStyle の各値が値のまま委譲面へ届く")]
    public void LD_MA_03_TheStyleReachesTheGatewayUnchanged()
    {
        TestLoadingGateway gateway = new();
        IKsLoading loading = NewLoading(gateway);
        LoadingStyle style = new()
        {
            IndicatorColor = Colors.Red,
            MessageFontSize = 21d,
            MessageColor = Color.FromRgba(0x11, 0x22, 0x33, 0x44),
            DefaultMessage = "しばらくお待ちください",
            ProgressFormat = (message, progress) => $"{message}:{progress}",
        };

        loading.Style = style;

        LoadingStyle delegated = gateway.AppliedStyles[0];
        Assert.Multiple(() =>
        {
            Assert.That(delegated.IndicatorColor, Is.EqualTo(Colors.Red));
            Assert.That(delegated.MessageFontSize, Is.EqualTo(21d));
            Assert.That(delegated.MessageColor, Is.EqualTo(Color.FromRgba(0x11, 0x22, 0x33, 0x44)));
            Assert.That(delegated.DefaultMessage, Is.EqualTo("しばらくお待ちください"));
            Assert.That(delegated.ProgressFormat("読込", 0.5d), Is.EqualTo("読込:0.5"));
            Assert.That(loading.Style, Is.SameAs(style), "設定した値をそのまま読み返せること");
        });
    }

    /// <summary>色は ARGB 32bit 整数として、成分の値を保ったまま境界の表現になる。</summary>
    [Test]
    [Description("[LD-MA-03] スタイルの色は ARGB 32bit 整数で値を保つ")]
    public void LD_MA_03_TheStyleColorsBecomeArgbIntegers()
    {
        LoadingStyle style = new()
        {
            IndicatorColor = Color.FromRgba(0x55, 0x66, 0x77, 0x88),
            MessageColor = Color.FromRgba(0x11, 0x22, 0x33, 0x44),
        };

        Assert.Multiple(() =>
        {
            Assert.That(style.IndicatorColorArgb, Is.EqualTo(unchecked((int)0x88556677u)));
            Assert.That(style.MessageColorArgb, Is.EqualTo(unchecked((int)0x44112233u)));
        });
    }

    /// <summary>既定ローディングの器メタ属性は、設定した値のまま委譲面へ届く。</summary>
    [Test]
    [Description("[LD-MA-03] 設定プロパティの options が値のまま委譲面へ届く")]
    public void LD_MA_03_TheOptionsReachTheGatewayUnchanged()
    {
        TestLoadingGateway gateway = new();
        IKsLoading loading = NewLoading(gateway);
        DialogOptions options = new()
        {
            LayoutArea = DialogLayoutArea.Window,
            DialogMargin = new Thickness(1d, 2d, 3d, 4d),
            ProportionalWidth = 1.5d,
            ProportionalHeight = 0.25d,
            OverlayColor = LayoutTestContentViews.Overlay,
            IsCanceledOnTouchOutside = false,
        };

        loading.Options = options;

        DialogOptions delegated = gateway.AppliedOptions[0];
        Assert.Multiple(() =>
        {
            Assert.That(delegated, Is.EqualTo(options));
            Assert.That(delegated.OverlayColorArgb, Is.EqualTo(unchecked((int)0x88556677u)));
            Assert.That(loading.Options, Is.SameAs(options), "設定した値をそのまま読み返せること");
        });
    }

    /// <summary>スコープ形の処理が報告した進捗は、値を変えずに委譲面へ届く。</summary>
    [Test]
    [Description("[LD-MA-03] 報告した進捗値が無変換で委譲面へ届く")]
    public async Task LD_MA_03_TheReportedProgressReachesTheGatewayUnchanged()
    {
        TestLoadingGateway gateway = new();
        IKsLoading loading = NewLoading(gateway);

        await loading.StartAsync(progress =>
        {
            // 丸めは Native 実装の責務なので、有効域を外れた値もそのまま運ばれる
            progress.Report(0.25d);
            progress.Report(1.5d);
            progress.Report(-2d);
            progress.Report(double.NaN);
            return Task.CompletedTask;
        });

        Assert.That(gateway.ReportedProgress, Is.EqualTo(new[] { 0.25d, 1.5d, -2d, double.NaN }));
    }

    /// <summary>進捗の受け口を実装した ViewModel だけが、転送先として委譲面へ渡る。</summary>
    [Test]
    [Description("[LD-MA-03] 進捗の転送先は受け口を実装した VM のときだけ渡る")]
    public async Task LD_MA_03_TheProgressReceiverIsSuppliedOnlyWhenImplemented()
    {
        TestLoadingGateway gateway = new();
        IKsLoading loading = NewLoading(gateway);
        ProgressReceivingLoadingTestViewModel receiving = new();
        PlainLoadingTestViewModel plain = new();

        await loading.StartAsync(
            receiving,
            _ => new Label(),
            progress =>
            {
                progress.Report(0.5d);
                return Task.CompletedTask;
            });
        await loading.StartAsync(plain, _ => new Label(), _ => Task.CompletedTask);

        Assert.Multiple(() =>
        {
            Assert.That(gateway.Requests[0].ProgressReceiver, Is.SameAs(receiving));
            Assert.That(receiving.ReceivedProgress, Is.EqualTo(new[] { 0.5d }));
            Assert.That(gateway.Requests[1].ProgressReceiver, Is.Null);
        });
    }

    /// <summary>設定を持ち越さないよう、検証ごとに新しい設定の置き場を使う。</summary>
    /// <param name="gateway">値を受け取る委譲面。</param>
    /// <returns>その委譲面だけを見る表示の入口。</returns>
    private static IKsLoading NewLoading(TestLoadingGateway gateway) =>
        new Loading(LoadingViewRegistry.Shared, new LoadingSettings(), gateway);
}
