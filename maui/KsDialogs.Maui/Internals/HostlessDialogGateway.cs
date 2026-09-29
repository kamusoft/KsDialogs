using System.Threading.Tasks;

namespace KsDialogs;

/// <summary>
/// 提示の仕組みそのものを持たない実行環境の委譲面。
/// </summary>
/// <remarks>
/// iOS / Android の実装を持たない素の .NET 上では、ダイアログを提示できる画面が現れることはない。
/// 待っても表示できないため、提示先の出現は待たずに
/// <see cref="DialogException.PresentationHostUnavailable"/> で即座に失敗する。
/// </remarks>
internal sealed class HostlessDialogGateway : IDialogGateway
{
    /// <inheritdoc/>
    public Task<DialogOutcome> PresentAsync(DialogPresentationRequest request) =>
        Task.FromException<DialogOutcome>(new DialogException.PresentationHostUnavailable());
}
