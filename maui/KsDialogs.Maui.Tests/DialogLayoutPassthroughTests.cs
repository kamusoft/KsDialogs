using System.Threading.Tasks;
using KsDialogs.Maui.Tests.Support;
using Microsoft.Maui;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Graphics;
using NUnit.Framework;

namespace KsDialogs.Maui.Tests;

/// <summary>
/// 中身に添付したメタ属性と show の引数が、値のまま Native への委譲面へ届くことの検証。
/// </summary>
/// <remarks>
/// レイアウト計算の実体を持つのは Native 実装だけで、期待 rect の全量検証も Native 側が受け持つ
/// (core/ADR-0009)。MAUI 形態に課されるのは輸送の値保存と供給の優先順位だけなので、ここでは
/// 「添付した値がそのまま委譲面へ渡ること」「show の引数が添付をまるごと置換すること」
/// 「何も添付しなければ既定値が渡ること」を見る。
/// </remarks>
[TestFixture]
public class DialogLayoutPassthroughTests
{
    /// <summary>中身に添付したメタ属性は、値を変えずに委譲面へ届く。</summary>
    [Test]
    [Description("添付したメタ属性が値のまま委譲面へ届く")]
    public async Task AttachedAttributesReachTheGatewayUnchanged()
    {
        DialogPresentationContent delegated = await PresentAsync(LayoutTestContentViews.WithEveryAttribute());

        Assert.Multiple(() =>
        {
            Assert.That(delegated.Options.LayoutArea, Is.EqualTo(DialogLayoutArea.Window));
            Assert.That(delegated.Options.DialogMargin, Is.EqualTo(new Thickness(1d, 2d, 3d, 4d)));
            Assert.That(delegated.Options.ProportionalWidth, Is.EqualTo(1.5d));
            Assert.That(delegated.Options.ProportionalHeight, Is.EqualTo(0.25d));
            Assert.That(delegated.Options.IsCanceledOnTouchOutside, Is.False);
            Assert.That(delegated.Placement.HorizontalAlignment, Is.EqualTo(DialogAlignment.End));
            Assert.That(delegated.Placement.VerticalAlignment, Is.EqualTo(DialogAlignment.Fill));
            Assert.That(delegated.Placement.OffsetX, Is.EqualTo(-12.5d));
            Assert.That(delegated.Placement.OffsetY, Is.EqualTo(34d));
        });
    }

    /// <summary>XAML の添付プロパティで宣言したメタ属性も、同じ値で委譲面へ届く。</summary>
    [Test]
    [Description("XAML で宣言した添付プロパティが委譲面へ届く")]
    public async Task AttributesDeclaredInXamlReachTheGateway()
    {
        DialogPresentationContent delegated = await PresentAsync(new LayoutAttributeTestView());

        Assert.Multiple(() =>
        {
            Assert.That(delegated.Options.LayoutArea, Is.EqualTo(DialogLayoutArea.Window));
            Assert.That(delegated.Options.DialogMargin, Is.EqualTo(new Thickness(1d, 2d, 3d, 4d)));
            Assert.That(delegated.Options.ProportionalWidth, Is.EqualTo(1.5d));
            Assert.That(delegated.Options.ProportionalHeight, Is.EqualTo(0.25d));
            Assert.That(delegated.Options.OverlayColor, Is.EqualTo(LayoutTestContentViews.Overlay));
            Assert.That(delegated.Options.IsCanceledOnTouchOutside, Is.False);
            Assert.That(delegated.Placement.HorizontalAlignment, Is.EqualTo(DialogAlignment.End));
            Assert.That(delegated.Placement.VerticalAlignment, Is.EqualTo(DialogAlignment.Fill));
            Assert.That(delegated.Placement.OffsetX, Is.EqualTo(-12.5d));
            Assert.That(delegated.Placement.OffsetY, Is.EqualTo(34d));
        });
    }

    /// <summary>覆いの色は ARGB 32bit 整数として、成分の値を保ったまま委譲面へ届く。</summary>
    [Test]
    [Description("色は ARGB 32bit 整数で値を保って届く")]
    public async Task OverlayColorReachesTheGatewayAsAnArgbInteger()
    {
        DialogPresentationContent delegated = await PresentAsync(LayoutTestContentViews.WithEveryAttribute());

        Assert.Multiple(() =>
        {
            Assert.That(delegated.Options.OverlayColor, Is.EqualTo(LayoutTestContentViews.Overlay));
            Assert.That(delegated.Options.OverlayColorArgb, Is.EqualTo(unchecked((int)0x88556677)));
            Assert.That(
                Color.FromInt(delegated.Options.OverlayColorArgb),
                Is.EqualTo(LayoutTestContentViews.Overlay));
        });
    }

    /// <summary>メタ属性を添付しない中身では、契約の既定値が委譲面へ届く。</summary>
    [Test]
    [Description("添付のない中身には既定値が届く")]
    public async Task ContentWithoutAttributesDelegatesTheDefaults()
    {
        DialogPresentationContent delegated = await PresentAsync(LayoutTestContentViews.WithoutAttributes());

        Assert.Multiple(() =>
        {
            Assert.That(delegated.Options.LayoutArea, Is.EqualTo(DialogLayoutArea.VisibleArea));
            Assert.That(delegated.Options.DialogMargin, Is.EqualTo(new Thickness(24d)));
            Assert.That(delegated.Options.ProportionalWidth, Is.EqualTo(-1d));
            Assert.That(delegated.Options.ProportionalHeight, Is.EqualTo(-1d));
            Assert.That(delegated.Options.OverlayColorArgb, Is.EqualTo(0x66000000));
            Assert.That(delegated.Options.IsCanceledOnTouchOutside, Is.True);
            Assert.That(delegated.Placement, Is.EqualTo(new DialogPlacement()));
        });
    }

    /// <summary>有効域を外れた値も丸めずに委譲面へ届く (丸めるのは Native 実装)。</summary>
    [Test]
    [Description("有効域を外れた値も丸めずに届く")]
    public async Task ValuesOutsideTheValidRangeAreNotRoundedOnTheWay()
    {
        DialogPresentationContent delegated = await PresentAsync(LayoutTestContentViews.WithInvalidValues());

        Assert.Multiple(() =>
        {
            Assert.That(delegated.Options.ProportionalWidth, Is.EqualTo(2d));
            Assert.That(delegated.Options.ProportionalHeight, Is.EqualTo(-5d));
            Assert.That(delegated.Options.DialogMargin.Left, Is.EqualTo(-30d));
            Assert.That(delegated.Options.DialogMargin.Top, Is.NaN);
            Assert.That(delegated.Placement.OffsetX, Is.NaN);
        });
    }

    /// <summary>show の引数で渡した置き場所が、添付された置き場所より優先される。</summary>
    [Test]
    [Description("show の引数の置き場所が添付に勝つ")]
    public async Task ShowPlacementWinsOverTheAttachedPlacement()
    {
        View contentView = LayoutTestContentViews.WithEveryAttribute();
        DialogPlacement showPlacement = new() { HorizontalAlignment = DialogAlignment.Start };

        DialogPresentationContent delegated = await PresentAsync(contentView, showPlacement);

        Assert.Multiple(() =>
        {
            Assert.That(delegated.Placement, Is.EqualTo(showPlacement));
            // 静的メタ属性は show からは供給できないため、添付した値がそのまま残る
            Assert.That(delegated.Options.LayoutArea, Is.EqualTo(DialogLayoutArea.Window));
            Assert.That(delegated.Options.IsCanceledOnTouchOutside, Is.False);
        });
    }

    /// <summary>show の引数の置き場所は、添付をオブジェクト単位で置換する (項目単位で合成しない)。</summary>
    [Test]
    [Description("show の置き場所はオブジェクト単位で置換する")]
    public async Task ShowPlacementReplacesTheAttachedPlacementAsAWhole()
    {
        View contentView = LayoutTestContentViews.WithPlacement(new DialogPlacement
        {
            HorizontalAlignment = DialogAlignment.End,
            VerticalAlignment = DialogAlignment.End,
            OffsetX = 40d,
            OffsetY = 40d,
        });

        DialogPresentationContent delegated = await PresentAsync(
            contentView,
            new DialogPlacement { HorizontalAlignment = DialogAlignment.Start });

        Assert.Multiple(() =>
        {
            Assert.That(delegated.Placement.HorizontalAlignment, Is.EqualTo(DialogAlignment.Start));
            Assert.That(delegated.Placement.VerticalAlignment, Is.EqualTo(DialogAlignment.Center));
            Assert.That(delegated.Placement.OffsetX, Is.EqualTo(0d));
            Assert.That(delegated.Placement.OffsetY, Is.EqualTo(0d));
        });
    }

    /// <summary>View の生成後・初回レイアウトパスの完了前に変えた添付は、実効値として採用される。</summary>
    [Test]
    [Description("初回レイアウト完了前の添付変更は採用される")]
    public async Task AttributeChangeBeforeTheFirstLayoutPassCompletesIsAdopted()
    {
        View contentView = LayoutTestContentViews.WithoutAttributes();
        DialogPresentationContent delegated = await PresentAsync(contentView);

        // 中身を生成した後 (= 委譲面が受け取った後) の書き換え
        Dialog.SetHorizontalAlignment(contentView, DialogAlignment.End);
        Dialog.SetOffsetX(contentView, 24d);
        Dialog.SetIsCanceledOnTouchOutside(contentView, false);

        Assert.Multiple(() =>
        {
            Assert.That(delegated.AreAttributesFrozen, Is.False, "採用時点はまだ来ていない");
            Assert.That(delegated.Placement.HorizontalAlignment, Is.EqualTo(DialogAlignment.End));
            Assert.That(delegated.Placement.OffsetX, Is.EqualTo(24d));
            Assert.That(delegated.Options.IsCanceledOnTouchOutside, Is.False);
        });
    }

    /// <summary>初回レイアウトパスの完了時点で固定した後の添付変更は、実効値を変えない。</summary>
    [Test]
    [Description("初回レイアウト完了後の添付変更は反映されない")]
    public async Task AttributeChangeAfterTheFirstLayoutPassIsIgnored()
    {
        View contentView = LayoutTestContentViews.WithoutAttributes();
        Dialog.SetHorizontalAlignment(contentView, DialogAlignment.Start);
        DialogPresentationContent delegated = await PresentAsync(contentView);

        // 初回レイアウトパスの完了 = 採用時点
        delegated.FreezeAttributes();
        Dialog.SetHorizontalAlignment(contentView, DialogAlignment.End);
        Dialog.SetIsCanceledOnTouchOutside(contentView, false);

        Assert.Multiple(() =>
        {
            Assert.That(delegated.AreAttributesFrozen, Is.True);
            Assert.That(delegated.Placement.HorizontalAlignment, Is.EqualTo(DialogAlignment.Start));
            Assert.That(delegated.Options.IsCanceledOnTouchOutside, Is.True);
        });
    }

    /// <summary>中身を show して、委譲面が受け取った内容を返す。</summary>
    /// <param name="contentView">その show の中身になる View。</param>
    /// <param name="placement">show の引数で渡す置き場所。</param>
    /// <returns>委譲面が受け取った中身とメタ属性の実効値。</returns>
    private static async Task<DialogPresentationContent> PresentAsync(
        View contentView,
        DialogPlacement? placement = null)
    {
        DialogViewRegistry registry = new();
        registry.Register((BooleanTestDialogViewModel _, DialogNotifier<bool> notifier) =>
        {
            notifier.Complete(true);
            return contentView;
        });
        TestDialogGateway gateway = new();
        await new Dialog(registry, gateway).ShowAsync(new BooleanTestDialogViewModel(), placement);
        return gateway.Contents[0];
    }
}
