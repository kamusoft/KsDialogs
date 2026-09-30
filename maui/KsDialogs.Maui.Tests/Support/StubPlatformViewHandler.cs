using Microsoft.Maui;
using Microsoft.Maui.Graphics;

namespace KsDialogs.Maui.Tests.Support;

/// <summary>platform view の代わりになる印。Native 実装がページとして受け付けない理由だけを持つ。</summary>
internal sealed class StubPlatformView
{
    /// <summary>Native 実装がページとして受け付けない理由。既定は受け付ける。</summary>
    public DialogCurrentPageRejection Rejection { get; init; } = DialogCurrentPageRejection.None;
}

/// <summary>
/// platform view を持つだけのハンドラ。要素を「描画済み」の状態にするために使う。
/// </summary>
/// <remarks>値の反映やレイアウトは何もしない。</remarks>
/// <param name="platformView">この要素の platform view として返す値。</param>
internal sealed class StubPlatformViewHandler(StubPlatformView platformView) : IViewHandler
{
    private IView? _virtualView;

    public object? PlatformView => platformView;

    public IView? VirtualView => _virtualView;

    IElement? IElementHandler.VirtualView => _virtualView;

    public IMauiContext? MauiContext => null;

    public bool HasContainer { get; set; }

    public object? ContainerView => null;

    public void SetMauiContext(IMauiContext mauiContext)
    {
    }

    public void SetVirtualView(IElement view) => _virtualView = (IView)view;

    public void UpdateValue(string property)
    {
    }

    public void Invoke(string command, object? args = null)
    {
    }

    public void DisconnectHandler() => _virtualView = null;

    public Size GetDesiredSize(double widthConstraint, double heightConstraint) => Size.Zero;

    public void PlatformArrange(Rect frame)
    {
    }
}
