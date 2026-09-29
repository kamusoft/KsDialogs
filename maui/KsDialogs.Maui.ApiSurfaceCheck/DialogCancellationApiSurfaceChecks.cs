using System.Threading;
using System.Threading.Tasks;
using Microsoft.Maui.Controls;

namespace KsDialogs.ApiSurfaceCheck;

/// <summary>
/// Dialog の show の全入口が、呼び出し元の打ち切りを受け付けることの正の検証。
/// </summary>
/// <remarks>
/// このファイルがコンパイルできることが検証結果。打ち切りは省略でき (今までの呼び出しがそのまま通る)、
/// 置き場所に続く位置引数でも、名前付き引数 <c>cancellationToken:</c> でも渡せる。
/// </remarks>
public static class DialogCancellationApiSurfaceChecks
{
    /// <summary>インスタンス渡しの show。</summary>
    /// <param name="dialogs">表示の入口。</param>
    /// <param name="cancellationToken">呼び出し元の打ち切り。</param>
    public static async Task PB_MC_01_AcceptsCancellationOnInstanceShow(
        IKsDialog dialogs,
        CancellationToken cancellationToken)
    {
        DialogResult<bool> withoutToken = await dialogs.ShowAsync(new ConsumerDialogViewModel());
        DialogResult<bool> positional = await dialogs.ShowAsync(
            new ConsumerDialogViewModel(), null, cancellationToken);
        DialogResult<bool> named = await dialogs.ShowAsync(
            new ConsumerDialogViewModel(), cancellationToken: cancellationToken);
        _ = (withoutToken, positional, named);
    }

    /// <summary>インライン表示の show (結果型を書く形と、真偽値の顔の形)。</summary>
    /// <param name="dialogs">表示の入口。</param>
    /// <param name="cancellationToken">呼び出し元の打ち切り。</param>
    public static async Task PB_MC_01_AcceptsCancellationOnInlineShow(
        IKsDialog dialogs,
        CancellationToken cancellationToken)
    {
        DialogResult<string> declaredWithoutToken = await dialogs.ShowAsync(
            new ConsumerTextDialogViewModel(),
            (ConsumerTextDialogViewModel _, DialogNotifier<string> _) => new Label());
        DialogResult<string> declaredPositional = await dialogs.ShowAsync(
            new ConsumerTextDialogViewModel(),
            (ConsumerTextDialogViewModel _, DialogNotifier<string> _) => new Label(),
            null,
            cancellationToken);
        DialogResult<string> declaredNamed = await dialogs.ShowAsync(
            new ConsumerTextDialogViewModel(),
            (ConsumerTextDialogViewModel _, DialogNotifier<string> _) => new Label(),
            cancellationToken: cancellationToken);

        DialogResult<bool> simpleWithoutToken = await dialogs.ShowAsync(
            new ConsumerSimpleDialogViewModel(), (_, _) => new Label());
        DialogResult<bool> simplePositional = await dialogs.ShowAsync(
            new ConsumerSimpleDialogViewModel(), (_, _) => new Label(), null, cancellationToken);
        DialogResult<bool> simpleNamed = await dialogs.ShowAsync(
            new ConsumerSimpleDialogViewModel(), (_, _) => new Label(), cancellationToken: cancellationToken);
        _ = (declaredWithoutToken, declaredPositional, declaredNamed);
        _ = (simpleWithoutToken, simplePositional, simpleNamed);
    }

    /// <summary>型指定の show (同期・非同期の configure、結果型を書く形と真偽値の顔の形)。</summary>
    /// <param name="dialogs">表示の入口。</param>
    /// <param name="cancellationToken">呼び出し元の打ち切り。</param>
    public static async Task PB_MC_01_AcceptsCancellationOnTypedShow(
        IKsDialog dialogs,
        CancellationToken cancellationToken)
    {
        DialogResult<string> declaredWithoutToken =
            await dialogs.ShowAsync<ConsumerModelBindingTextViewModel, string>();
        DialogResult<string> declaredPositional =
            await dialogs.ShowAsync<ConsumerModelBindingTextViewModel, string>(
                viewModel => viewModel.Text = "初期値", null, cancellationToken);
        DialogResult<string> declaredNamed =
            await dialogs.ShowAsync<ConsumerModelBindingTextViewModel, string>(
                cancellationToken: cancellationToken);
        DialogResult<string> declaredAsynchronousPositional =
            await dialogs.ShowAsync<ConsumerModelBindingTextViewModel, string>(
                async viewModel =>
                {
                    await Task.Yield();
                    viewModel.Text = "非同期で用意";
                },
                null,
                cancellationToken);
        DialogResult<string> declaredAsynchronousNamed =
            await dialogs.ShowAsync<ConsumerModelBindingTextViewModel, string>(
                async viewModel =>
                {
                    await Task.Yield();
                    viewModel.Text = "非同期で用意";
                },
                cancellationToken: cancellationToken);

        DialogResult<bool> simpleWithoutToken = await dialogs.ShowAsync<ConsumerModelBindingViewModel>();
        DialogResult<bool> simplePositional = await dialogs.ShowAsync<ConsumerModelBindingViewModel>(
            viewModel => viewModel.Message = "確認", null, cancellationToken);
        DialogResult<bool> simpleNamed = await dialogs.ShowAsync<ConsumerModelBindingViewModel>(
            cancellationToken: cancellationToken);
        DialogResult<bool> simpleAsynchronousPositional = await dialogs.ShowAsync<ConsumerModelBindingViewModel>(
            async viewModel =>
            {
                await Task.Yield();
                viewModel.Message = "非同期で用意";
            },
            null,
            cancellationToken);
        DialogResult<bool> simpleAsynchronousNamed = await dialogs.ShowAsync<ConsumerModelBindingViewModel>(
            async viewModel =>
            {
                await Task.Yield();
                viewModel.Message = "非同期で用意";
            },
            cancellationToken: cancellationToken);
        _ = (declaredWithoutToken, declaredPositional, declaredNamed);
        _ = (declaredAsynchronousPositional, declaredAsynchronousNamed);
        _ = (simpleWithoutToken, simplePositional, simpleNamed);
        _ = (simpleAsynchronousPositional, simpleAsynchronousNamed);
    }
}
