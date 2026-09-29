using System;
using System.Threading;
using System.Threading.Tasks;
#if IOS || ANDROID
using Microsoft.Maui.ApplicationModel;
#endif

namespace KsDialogs;

/// <summary>
/// show 1 回分の提示処理。型消去された結果でやり取りし、宣言結果型への復元は呼び出し側が行う。
/// </summary>
internal static class DialogPresenter
{
    /// <summary>
    /// ViewModel の型で factory を解決し、生成した View をダイアログとして提示して結果を待つ。
    /// </summary>
    /// <remarks>
    /// 未登録 (fallback でも解決できない場合を含む) は結果を返さずに
    /// <see cref="DialogException.ViewFactoryNotRegistered"/> を投げ、View の生成・表示も行わない。
    /// 提示先の出現の待ちは委譲面の先 (Native) が受け持つ。値型の ViewModel は解決より前に構成ミスとして失敗する
    /// (<see cref="DialogException.ValueTypeViewModel"/>)。
    /// メタ属性は中身への添付と show の引数から合成されるだけで、位置・大きさの計算は行わない。
    /// </remarks>
    /// <param name="viewModel">show に渡された ViewModel。</param>
    /// <param name="registry">解決に使うレジストリ。</param>
    /// <param name="gateway">提示を委譲する面。</param>
    /// <param name="placement">show の引数で渡された置き場所。指定なしは <see langword="null"/>。</param>
    /// <param name="cancellationToken">show に渡された、呼び出し元の打ち切り。</param>
    /// <returns>型を消した結果。</returns>
    public static Task<DialogOutcome> PresentAsync(
        object viewModel,
        DialogViewRegistry registry,
        IDialogGateway gateway,
        DialogPlacement? placement,
        CancellationToken cancellationToken) =>
        PresentCoreAsync(viewModel, gateway, placement, cancellationToken, () =>
        {
            Type viewModelType = viewModel.GetType();
            return DialogResolution.ResolveViewFactory(
                registry,
                registry.Entry(viewModelType),
                viewModelType);
        });

    /// <summary>
    /// 渡された factory をその場で使ってダイアログとして提示し、結果を待つ。
    /// </summary>
    /// <remarks>
    /// レジストリは参照も更新もしないため、この提示は既存の登録に干渉しない (core/ADR-0013)。
    /// 結果報告口の紐付けは中身の生成より前に済ませ、factory 本体からも <c>viewModel.Notifier</c> が
    /// 読めるようにする (core/ADR-0018)。同じインスタンスが既に表示中なら、結果に化けさせずに
    /// 構成ミスとして失敗する。値型の ViewModel も同様に、紐付けより前に構成ミスとして失敗する
    /// (<see cref="DialogException.ValueTypeViewModel"/>)。提示先の出現の待ちは委譲面の先 (Native) が受け持つ。
    /// </remarks>
    /// <param name="viewModel">show に渡された ViewModel。</param>
    /// <param name="factory">その場で使う型消去済みの factory。</param>
    /// <param name="gateway">提示を委譲する面。</param>
    /// <param name="placement">show の引数で渡された置き場所。指定なしは <see langword="null"/>。</param>
    /// <param name="cancellationToken">show に渡された、呼び出し元の打ち切り。</param>
    /// <returns>型を消した結果。</returns>
    public static Task<DialogOutcome> PresentAsync(
        object viewModel,
        DialogViewFactory factory,
        IDialogGateway gateway,
        DialogPlacement? placement,
        CancellationToken cancellationToken) =>
        PresentCoreAsync(viewModel, gateway, placement, cancellationToken, () => factory);

    /// <summary>
    /// 中身を作って提示し、結果が確定するまで待つ、全 show 経路の共通入口。
    /// </summary>
    /// <remarks>
    /// 参照型限定の強制 (core/ADR-0018) はここで行うため、factory の解決方式 (レジストリ / インライン /
    /// 型指定) によらず値型の ViewModel は factory の解決・提示先への委譲・紐付けのどれよりも先に弾かれる。
    /// <para>
    /// 呼び出し元の打ち切り (maui/ADR-0006) は、ここに来た時点で打ち切り済みなら何もせずに
    /// <see cref="OperationCanceledException"/> を投げる。委譲した後の打ち切りは委譲面が表示を止めて
    /// cancelled で確定させるので、それをここで <see cref="OperationCanceledException"/> へ読み替える。
    /// 結果が先に確定していても、配送 (退出の演出と器の撤去の完了) の前に打ち切られたなら打ち切りとして返す
    /// (Native の呼び出し元の打ち切りと同じく、打ち切りが確定済みの結果より優先する)。
    /// </para>
    /// </remarks>
    /// <param name="viewModel">show に渡された ViewModel。</param>
    /// <param name="gateway">提示を委譲する面。</param>
    /// <param name="placement">show の引数で渡された置き場所。指定なしは <see langword="null"/>。</param>
    /// <param name="cancellationToken">show に渡された、呼び出し元の打ち切り。</param>
    /// <param name="resolveFactory">中身の生成に使う factory を返す処理。未登録は <see cref="DialogException"/> を投げる。</param>
    /// <returns>型を消した結果。</returns>
    private static async Task<DialogOutcome> PresentCoreAsync(
        object viewModel,
        IDialogGateway gateway,
        DialogPlacement? placement,
        CancellationToken cancellationToken,
        Func<DialogViewFactory> resolveFactory)
    {
        cancellationToken.ThrowIfCancellationRequested();
        Type viewModelType = viewModel.GetType();
        if (viewModelType.IsValueType)
        {
            // 登録と型指定 show は class 制約で弾けるが、interface で受けるインスタンス渡し show は
            // コンパイル時に拒否できないため、ここで構成ミスとして失敗させる
            throw new DialogException.ValueTypeViewModel(DialogResolution.TypeName(viewModelType));
        }

        DialogViewFactory factory = resolveFactory();
        DialogResultChannel resultChannel = new();
        if (!DialogNotifierBindings.Bind(viewModel, resultChannel))
        {
            throw new DialogException.ViewModelAlreadyShowing(
                DialogResolution.TypeName(viewModelType));
        }

        try
        {
            DialogPresentationRequest request = new(
                () => factory(viewModel, resultChannel),
                resultChannel,
                placement,
                cancellationToken);
            DialogOutcome outcome = await gateway.PresentAsync(request).ConfigureAwait(false);
            if (cancellationToken.IsCancellationRequested)
            {
                // 打ち切りで止まった show は、結果の cancelled ではなく打ち切りとして呼び出し元へ返す
                throw new OperationCanceledException(cancellationToken);
            }

            return outcome;
        }
        finally
        {
            // 紐付け後に show が終わる全経路 (正常配送・中身の生成失敗・提示の失敗・打ち切り・器消失) で外す。
            // 呼び出し元へ結果や例外が渡るのはこの除去のあと
            DialogNotifierBindings.Unbind(viewModel, resultChannel);
        }
    }

    /// <summary>
    /// ViewModel の生成と configure を、中身の View 生成と同じ UI スレッドで行う。
    /// </summary>
    /// <remarks>
    /// platform 実装を持たない実行環境 (ユニットテストの素の .NET) には UI スレッドが無いため、
    /// その場で実行する。
    /// </remarks>
    /// <typeparam name="T">処理が返す値の型。</typeparam>
    /// <param name="work">UI スレッドで行う処理。</param>
    /// <returns>処理の結果。</returns>
    public static Task<T> OnUiThreadAsync<T>(Func<Task<T>> work) =>
#if IOS || ANDROID
        MainThread.InvokeOnMainThreadAsync(work);
#else
        work();
#endif
}
