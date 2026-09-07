using KsDialogs.Maui.Tests.Support;
using Microsoft.Maui.Graphics;
using NUnit.Framework;

namespace KsDialogs.Maui.Tests;

/// <summary>
/// Toast の表示 API に渡した値が、そのまま Native への委譲面へ届くことの検証。
/// </summary>
/// <remarks>
/// 表示・重なり・配置・消滅の観察可能挙動の全量検証は Native 側が受け持つ (core/ADR-0001・0009)。
/// MAUI 形態に課されるのは輸送の値保存だけなので、ここでは「duration・placement の各値・
/// スタイルの各値が無変換で委譲面へ渡ること」を見る。
/// </remarks>
[TestFixture]
public class ToastPassthroughTests
{
    /// <summary>表示 API の duration / placement 引数が、値を変えずに委譲面へ届く。</summary>
    [Test]
    [Description("[TS-MA-02] duration と placement 引数が値のまま委譲面へ届く")]
    public void TS_MA_02_TheDurationAndPlacementArgumentsReachTheGatewayUnchanged()
    {
        TestToastGateway gateway = new();
        IKsToast toast = NewToast(gateway);
        DialogPlacement placement = new()
        {
            HorizontalAlignment = DialogAlignment.End,
            VerticalAlignment = DialogAlignment.Fill,
            OffsetX = -12.5d,
            OffsetY = 34d,
        };

        toast.Show("保存しました", 2500, placement);
        // 丸めは Native 実装の責務なので、有効域を外れた値もそのまま運ばれる
        toast.Show("削除しました", 0);

        Assert.Multiple(() =>
        {
            Assert.That(gateway.Requests[0].Message, Is.EqualTo("保存しました"));
            Assert.That(gateway.Requests[0].DurationMilliseconds, Is.EqualTo(2500));
            Assert.That(gateway.Requests[0].ShowPlacement, Is.EqualTo(placement));
            Assert.That(gateway.Requests[1].DurationMilliseconds, Is.EqualTo(0));
            Assert.That(gateway.Requests[1].ShowPlacement, Is.Null);
        });
    }

    /// <summary>カスタム View では、添付した placement を表示 API の引数がまるごと置換する。</summary>
    [Test]
    [Description("[TS-MA-02] カスタム View の添付は placement 引数で置換される")]
    public void TS_MA_02_TheShowArgumentReplacesTheAttachedPlacement()
    {
        TestToastGateway gateway = new();
        IKsToast toast = NewToast(gateway);
        PlainToastTestViewModel viewModel = new();
        DialogPlacement attached = new() { HorizontalAlignment = DialogAlignment.Start, OffsetX = 8d };
        DialogPlacement argument = new() { VerticalAlignment = DialogAlignment.End, OffsetY = -4d };

        toast.Show(viewModel, _ => LayoutTestContentViews.WithPlacement(attached));
        toast.Show(viewModel, _ => LayoutTestContentViews.WithPlacement(attached), 1000, argument);

        Assert.Multiple(() =>
        {
            Assert.That(gateway.Contents[0].Placement, Is.EqualTo(attached));
            Assert.That(gateway.Contents[1].Placement, Is.EqualTo(argument));
        });
    }

    /// <summary>スタイルの各値は、設定した形のまま委譲面へ届く。</summary>
    [Test]
    [Description("[TS-MA-02] ToastStyle の各値が値のまま委譲面へ届く")]
    public void TS_MA_02_TheStyleReachesTheGatewayUnchanged()
    {
        TestToastGateway gateway = new();
        IKsToast toast = NewToast(gateway);
        DialogPlacement defaultPlacement = new() { VerticalAlignment = DialogAlignment.Start, OffsetY = 40d };
        ToastStyle style = new()
        {
            BackgroundColor = Colors.Red,
            TextColor = Color.FromRgba(0x11, 0x22, 0x33, 0x44),
            FontSize = 21d,
            CornerRadius = 8d,
            DefaultDuration = 3000,
            DefaultPlacement = defaultPlacement,
        };

        toast.Style = style;

        ToastStyle delegated = gateway.AppliedStyles[0];
        Assert.Multiple(() =>
        {
            Assert.That(delegated.BackgroundColor, Is.EqualTo(Colors.Red));
            Assert.That(delegated.TextColor, Is.EqualTo(Color.FromRgba(0x11, 0x22, 0x33, 0x44)));
            Assert.That(delegated.FontSize, Is.EqualTo(21d));
            Assert.That(delegated.CornerRadius, Is.EqualTo(8d));
            Assert.That(delegated.DefaultDuration, Is.EqualTo(3000));
            Assert.That(delegated.DefaultPlacement, Is.EqualTo(defaultPlacement));
            Assert.That(toast.Style, Is.SameAs(style), "設定した値をそのまま読み返せること");
        });
    }

    /// <summary>スタイルの既定値は、Native の内蔵既定と同じ値で運ばれる。</summary>
    [Test]
    [Description("[TS-MA-02] 何も設定しないスタイルは契約の既定値で運ばれる")]
    public void TS_MA_02_TheUnconfiguredStyleCarriesTheContractDefaults()
    {
        ToastStyle style = new();

        Assert.Multiple(() =>
        {
            Assert.That(style.BackgroundColorArgb, Is.EqualTo(unchecked((int)0xEB323232u)));
            Assert.That(style.TextColorArgb, Is.EqualTo(unchecked((int)0xFFFFFFFFu)));
            Assert.That(style.FontSize, Is.EqualTo(14d));
            Assert.That(style.CornerRadius, Is.EqualTo(22d));
            Assert.That(style.DefaultDuration, Is.EqualTo(1500));
            Assert.That(style.DefaultPlacement, Is.Null);
        });
    }

    /// <summary>色は ARGB 32bit 整数として、成分の値を保ったまま境界の表現になる。</summary>
    [Test]
    [Description("[TS-MA-02] スタイルの色は ARGB 32bit 整数で値を保つ")]
    public void TS_MA_02_TheStyleColorsBecomeArgbIntegers()
    {
        ToastStyle style = new()
        {
            BackgroundColor = Color.FromRgba(0x55, 0x66, 0x77, 0x88),
            TextColor = Color.FromRgba(0x11, 0x22, 0x33, 0x44),
        };

        Assert.Multiple(() =>
        {
            Assert.That(style.BackgroundColorArgb, Is.EqualTo(unchecked((int)0x88556677u)));
            Assert.That(style.TextColorArgb, Is.EqualTo(unchecked((int)0x44112233u)));
        });
    }

    /// <summary>設定を持ち越さないよう、検証ごとに新しい設定の置き場を使う。</summary>
    /// <param name="gateway">値を受け取る委譲面。</param>
    /// <returns>その委譲面だけを見る表示の入口。</returns>
    private static IKsToast NewToast(TestToastGateway gateway) =>
        new Toast(ToastViewRegistry.Shared, new ToastSettings(), gateway);
}
