using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.Maui.Controls;

namespace KsDialogs.Maui.Tests.Support;

/// <summary>
/// platform 実装の代わりに使う委譲面。
/// </summary>
/// <remarks>
/// 実装と同じ順序 — 中身の View を生成し、結果が確定するまで待つ — で振る舞い、
/// 提示そのものは行わない。ダイアログ側の操作は
/// <paramref name="onPresented"/> から結果チャネルまたは報告口を動かして再現する。
/// </remarks>
/// <param name="onPresented">中身の View を生成した直後に呼ばれる、ダイアログ側の操作の再現。</param>
internal sealed class TestDialogGateway(Action<DialogPresentationRequest>? onPresented = null) : IDialogGateway
{
    /// <summary>委譲を受けた内容を呼ばれた順に記録したもの。</summary>
    public List<DialogPresentationRequest> Requests { get; } = [];

    /// <summary>生成された中身とメタ属性の実効値を呼ばれた順に記録したもの。</summary>
    public List<DialogPresentationContent> Contents { get; } = [];

    /// <summary>生成された中身の View を呼ばれた順に記録したもの。</summary>
    public List<View> CreatedViews => [.. Contents.Select(content => content.ContentView)];

    /// <inheritdoc/>
    public async Task<DialogOutcome> PresentAsync(DialogPresentationRequest request)
    {
        Requests.Add(request);
        Contents.Add(request.CreateContent());
        onPresented?.Invoke(request);
        return await request.ResultChannel.Result.ConfigureAwait(false);
    }
}

/// <summary>
/// 提示に入る前に構成エラーで失敗する委譲面。
/// </summary>
/// <param name="failure">委譲面が投げる構成エラー。</param>
internal sealed class FailingTestDialogGateway(DialogException failure) : IDialogGateway
{
    /// <summary>委譲を受けた回数。</summary>
    public int PresentCount { get; private set; }

    /// <inheritdoc/>
    public Task<DialogOutcome> PresentAsync(DialogPresentationRequest request)
    {
        PresentCount++;
        return Task.FromException<DialogOutcome>(failure);
    }
}
