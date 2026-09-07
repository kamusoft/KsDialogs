using System;
using System.Threading.Tasks;

namespace KsDialogs;

/// <summary>
/// 提示先を持たない実行環境の Loading の委譲面。
/// </summary>
/// <remarks>
/// iOS / Android の実装を持たない素の .NET 上には、ローディングを載せられる画面が存在しない。
/// Loading では提示環境の不在は構成ミスではないため、Dialog の委譲面と違って失敗させず、
/// 表示を伴わないまま処理を実行して契約どおりの戻り値・失敗を返す (core/ADR-0024)。
/// </remarks>
internal sealed class HostlessLoadingGateway : ILoadingGateway
{
    /// <inheritdoc/>
    public void ApplyStyle(LoadingStyle style)
    {
    }

    /// <inheritdoc/>
    public void ApplyOptions(DialogOptions options)
    {
    }

    /// <inheritdoc/>
    public Task ShowAsync(LoadingPresentationRequest request) => Task.CompletedTask;

    /// <inheritdoc/>
    public Task RunAsync(LoadingPresentationRequest request, Func<IProgress<double>, Task> action)
    {
        ArgumentNullException.ThrowIfNull(action);

        // 表示は成立しないが、渡された処理は実行される。報告先が無いので進捗は捨てる
        return action(new Progress<double>(static _ => { }));
    }

    /// <inheritdoc/>
    public Task HideAsync() => Task.CompletedTask;

    /// <inheritdoc/>
    public void SetMessage(string? message)
    {
    }
}
