using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.Maui.Controls;

namespace KsDialogs.Maui.Tests.Support;

/// <summary>
/// platform 実装の代わりに使う Loading の委譲面。
/// </summary>
/// <remarks>
/// 合流・世代・進捗の丸めはすべて Native 実装の責務なので、この差し替えは<b>再現しない</b>。
/// MAUI 形態に課されるのは「解決した中身と供給値がそのまま委譲面へ届くこと」だけなので、
/// 渡された値を記録し、スコープ形では処理をその場で走らせる。
/// </remarks>
internal sealed class TestLoadingGateway : ILoadingGateway
{
    /// <summary>設定されたスタイルを設定順に記録したもの。</summary>
    public List<LoadingStyle> AppliedStyles { get; } = [];

    /// <summary>設定された器メタ属性を設定順に記録したもの。</summary>
    public List<DialogOptions> AppliedOptions { get; } = [];

    /// <summary>委譲を受けた表示の要求を呼ばれた順に記録したもの。</summary>
    public List<LoadingPresentationRequest> Requests { get; } = [];

    /// <summary>生成された中身とメタ属性の実効値を呼ばれた順に記録したもの。</summary>
    public List<DialogPresentationContent> Contents { get; } = [];

    /// <summary>生成された中身の View を呼ばれた順に記録したもの。</summary>
    public List<View> CreatedViews => [.. Contents.Select(content => content.ContentView)];

    /// <summary>報告された進捗を報告順に記録したもの。</summary>
    public List<double> ReportedProgress { get; } = [];

    /// <summary>更新されたメッセージを更新順に記録したもの。</summary>
    public List<string?> UpdatedMessages { get; } = [];

    /// <summary>閉じるよう求められた回数。</summary>
    public int HideCount { get; private set; }

    /// <inheritdoc/>
    public void ApplyStyle(LoadingStyle style) => AppliedStyles.Add(style);

    /// <inheritdoc/>
    public void ApplyOptions(DialogOptions options) => AppliedOptions.Add(options);

    /// <inheritdoc/>
    public Task ShowAsync(LoadingPresentationRequest request)
    {
        Accept(request);
        return Task.CompletedTask;
    }

    /// <inheritdoc/>
    public Task RunAsync(LoadingPresentationRequest request, Func<IProgress<double>, Task> action)
    {
        Accept(request);
        return action(new RecordingProgress(this, request.ProgressReceiver));
    }

    /// <inheritdoc/>
    public Task HideAsync()
    {
        HideCount++;
        return Task.CompletedTask;
    }

    /// <inheritdoc/>
    public void SetMessage(string? message) => UpdatedMessages.Add(message);

    /// <summary>要求を記録し、カスタム View なら中身を生成する (器が提示先を確保した後にあたる)。</summary>
    private void Accept(LoadingPresentationRequest request)
    {
        Requests.Add(request);
        if (!request.IsBuiltin)
        {
            Contents.Add(request.CreateContent());
        }
    }

    /// <summary>報告された進捗を記録し、要求に添えられた受け口へそのまま渡す。</summary>
    /// <remarks>
    /// 値の丸めは Native 実装が行うため、ここでは記録も転送も報告された値のままにする。
    /// </remarks>
    /// <param name="gateway">記録先。</param>
    /// <param name="receiver">要求に添えられた転送先。無ければ <see langword="null"/>。</param>
    private sealed class RecordingProgress(TestLoadingGateway gateway, ILoadingProgressReceiver? receiver)
        : IProgress<double>
    {
        public void Report(double value)
        {
            gateway.ReportedProgress.Add(value);
            receiver?.OnProgress(value);
        }
    }
}
