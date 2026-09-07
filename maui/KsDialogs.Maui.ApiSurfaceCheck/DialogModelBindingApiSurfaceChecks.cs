using System.Threading.Tasks;
using Microsoft.Maui.Controls;

namespace KsDialogs.ApiSurfaceCheck;

/// <summary>ViewModel 主導の呼び出し面で利用者が書く形の ViewModel。真偽値の顔で宣言する。</summary>
public sealed class ConsumerModelBindingViewModel : IDialogViewModel
{
    /// <summary>ダイアログに出す文言。表示の直前に configure から設定する。</summary>
    public string Message { get; set; } = string.Empty;

    /// <summary>中身を経由せず ViewModel 自身が結果を報告する形。</summary>
    public void Confirm() => this.Notifier?.Complete(true);
}

/// <summary>結果型を宣言する側の、ViewModel 主導の呼び出し面の検証用 ViewModel。</summary>
public sealed class ConsumerModelBindingTextViewModel : IDialogViewModel<string>
{
    /// <summary>入力欄の初期値。</summary>
    public string Text { get; set; } = string.Empty;
}

/// <summary>
/// ViewModel 主導の呼び出し面 (VM 引数のみの登録・ViewModel factory 登録・<c>vm.Notifier</c>・
/// 型指定 show) の公開 API 形状の正の検証。
/// </summary>
/// <remarks>
/// このファイルがコンパイルできることが検証結果であり、公開すべき型・メンバが利用者から
/// 見えなくなるか、オーバーロードが意図した経路に解決されなくなればビルドが失敗する。
/// 既存のインスタンス渡し show・インライン show と同居させてあるため、
/// 型指定 show の追加で既存経路の解決が壊れれば同じくここで落ちる。
/// </remarks>
public static class DialogModelBindingApiSurfaceChecks
{
    /// <summary>ViewModel 引数だけの factory で登録でき、<c>vm.Notifier</c> が宣言結果型に型付く。</summary>
    /// <param name="registry">紐付けを持つレジストリ。</param>
    public static void MB_MA_01_AcceptsViewModelOnlyViewFactory(DialogViewRegistry registry)
    {
        registry.Register((ConsumerModelBindingViewModel viewModel) =>
        {
            DialogNotifier<bool>? notifier = viewModel.Notifier;
            notifier?.Complete(true);
            return new Label();
        });
    }

    /// <summary>結果型を宣言した ViewModel も、ViewModel 引数だけの factory で登録できる。</summary>
    /// <param name="registry">紐付けを持つレジストリ。</param>
    public static void MB_MA_01_AcceptsViewModelOnlyViewFactoryWithDeclaredResult(DialogViewRegistry registry)
    {
        registry.Register<ConsumerModelBindingTextViewModel, string>(viewModel =>
        {
            DialogNotifier<string>? notifier = viewModel.Notifier;
            notifier?.Complete(viewModel.Text);
            return new Label();
        });
    }

    /// <summary>ViewModel factory を登録できる。View factory とはスロットが別で共存する。</summary>
    /// <param name="registry">紐付けを持つレジストリ。</param>
    public static void MB_MA_01_AcceptsViewModelFactoryRegistration(DialogViewRegistry registry)
    {
        registry.RegisterViewModel(() => new ConsumerModelBindingViewModel());
        registry.RegisterViewModel<ConsumerModelBindingTextViewModel, string>(
            () => new ConsumerModelBindingTextViewModel());
    }

    /// <summary>configure なしの型指定 show は宣言結果型 (真偽値の顔) の結果を返す。</summary>
    /// <param name="dialogs">表示の入口。</param>
    /// <returns>その show の結果。</returns>
    public static async Task<DialogResult<bool>> MB_MA_01_AcceptsTypedShowWithoutConfigure(IKsDialog dialogs)
    {
        DialogResult<bool> result = await dialogs.ShowAsync<ConsumerModelBindingViewModel>();
        return result;
    }

    /// <summary>configure つきの型指定 show を書ける。</summary>
    /// <param name="dialogs">表示の入口。</param>
    /// <returns>その show の結果。</returns>
    public static async Task<DialogResult<bool>> MB_MA_01_AcceptsTypedShowWithConfigure(IKsDialog dialogs)
    {
        DialogResult<bool> result = await dialogs.ShowAsync<ConsumerModelBindingViewModel>(
            viewModel => viewModel.Message = "確認");
        return result;
    }

    /// <summary>configure は非同期でも書ける。</summary>
    /// <param name="dialogs">表示の入口。</param>
    /// <returns>その show の結果。</returns>
    public static async Task<DialogResult<bool>> MB_MA_01_AcceptsTypedShowWithAsynchronousConfigure(
        IKsDialog dialogs)
    {
        DialogResult<bool> result = await dialogs.ShowAsync<ConsumerModelBindingViewModel>(
            async viewModel =>
            {
                await Task.Yield();
                viewModel.Message = "非同期で用意";
            });
        return result;
    }

    /// <summary>カスタム結果型は 2 型引数の形で宣言結果型のまま受け取れる。</summary>
    /// <param name="dialogs">表示の入口。</param>
    /// <returns>その show の結果。</returns>
    public static async Task<DialogResult<string>> MB_MA_01_AcceptsTypedShowWithDeclaredResult(
        IKsDialog dialogs)
    {
        DialogResult<string> withoutConfigure =
            await dialogs.ShowAsync<ConsumerModelBindingTextViewModel, string>();
        DialogResult<string> withConfigure =
            await dialogs.ShowAsync<ConsumerModelBindingTextViewModel, string>(
                viewModel => viewModel.Text = "初期値");
        DialogResult<string> withAsynchronousConfigure =
            await dialogs.ShowAsync<ConsumerModelBindingTextViewModel, string>(
                async viewModel =>
                {
                    await Task.Yield();
                    viewModel.Text = "非同期で用意";
                });
        _ = withoutConfigure;
        _ = withConfigure;
        return withAsynchronousConfigure;
    }

    /// <summary>型指定 show でも置き場所を渡せる。</summary>
    /// <param name="dialogs">表示の入口。</param>
    /// <returns>その show の結果。</returns>
    public static Task<DialogResult<bool>> MB_MA_01_AcceptsTypedShowWithPlacement(IKsDialog dialogs) =>
        dialogs.ShowAsync<ConsumerModelBindingViewModel>(
            viewModel => viewModel.Message = "確認",
            new DialogPlacement { HorizontalAlignment = DialogAlignment.Start });

    /// <summary>既存のインスタンス渡し show とインライン show が、型指定 show と同居しても解決される。</summary>
    /// <param name="dialogs">表示の入口。</param>
    /// <returns>その show の結果。</returns>
    public static async Task<DialogResult<bool>> MB_MA_01_AcceptsExistingShowRoutesAlongsideTypedShow(
        IKsDialog dialogs)
    {
        DialogResult<bool> byInstance = await dialogs.ShowAsync(new ConsumerModelBindingViewModel());
        DialogResult<bool> inline = await dialogs.ShowAsync(
            new ConsumerModelBindingViewModel(),
            (ConsumerModelBindingViewModel viewModel, DialogNotifier<bool> notifier) =>
            {
                notifier.Complete(true);
                return new Label();
            });
        _ = byInstance;
        return inline;
    }
}
