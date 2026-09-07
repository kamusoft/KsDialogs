namespace KsDialogs.Sample.Maui;

/// <summary>
/// ViewModel 型と MAUI View factory の紐付け。
/// </summary>
/// <remarks>
/// 利用者アプリと同じ側から、公開 product のレジストリへ登録する。
/// <para>
/// 登録の書き方は 2 通りあり、この Sample は両方を実際の登録で示す。ひとつは型引数を明示する書き方
/// (<c>Register&lt;TViewModel, TResult&gt;</c> と、真偽値の顔なら <c>Register&lt;TViewModel&gt;</c>)。
/// もうひとつはラムダの引数に型を書いて型引数を省く書き方で、どちらも同じ登録になる。
/// </para>
/// </remarks>
public static class SampleDialogRegistration
{
    /// <summary>この Sample が使うダイアログを登録する。</summary>
    public static void Register()
    {
        Dialog.Instance.Registry.Register<BasicDialogViewModel, bool>(
            (viewModel, notifier) => new BasicDialogCardView(
                viewModel.Message,
                onCancel: notifier.Cancel,
                onComplete: () => notifier.Complete(true)));

        Dialog.Instance.Registry.Register<LayoutDialogViewModel, bool>(
            (viewModel, notifier) =>
            {
                LayoutDialogCardView view = new(
                    viewModel.Message,
                    onCancel: notifier.Cancel,
                    onComplete: () => notifier.Complete(true));

                // 基準領域は中身の性質として扱う静的メタ属性なので、View への添付で供給する
                Dialog.SetLayoutArea(
                    view,
                    viewModel.UsesVisibleArea ? DialogLayoutArea.VisibleArea : DialogLayoutArea.Window);

                return view;
            });

        Dialog.Instance.Registry.Register<TransitionDialogViewModel, bool>(
            (viewModel, notifier) =>
            {
                TransitionDialogCardView view = new(
                    viewModel.Message,
                    onCancel: notifier.Cancel,
                    onComplete: () => notifier.Complete(true));

                // 演出は Show の引数では渡せないため、中身への添付で供給する (core/ADR-0017)
                Dialog.SetTransition(view, viewModel.Transition);

                return view;
            });

        // 真偽値の顔で宣言した ViewModel は、結果型の型引数を書かずに登録できる
        Dialog.Instance.Registry.Register<DeclarativeDialogViewModel>(
            (viewModel, notifier) => new DeclarativeDialogCardView(
                viewModel.Message,
                onCancel: notifier.Cancel,
                onComplete: () => notifier.Complete(true)));

        // ラムダの引数に型を書けば、型引数そのものを省ける。上の書き方と同じ登録になる
        Dialog.Instance.Registry.Register(
            (TextInputDialogViewModel viewModel, DialogNotifier<string> notifier) => new TextInputDialogCardView(
                viewModel.Message,
                onCancel: notifier.Cancel,
                onComplete: notifier.Complete));
    }
}
