namespace KsDialogs;

/// <summary>
/// ViewModel から結果報告口を引く面 (core/ADR-0018)。
/// </summary>
public static class DialogViewModelExtensions
{
    extension<TResult>(IDialogViewModel<TResult> viewModel)
    {
        /// <summary>
        /// 表示中のこの ViewModel に紐付いた結果報告口。
        /// </summary>
        /// <remarks>
        /// show が中身を生成する直前に紐付き、結果が呼び出し元へ渡る前に外れる。
        /// したがって show の前と終わったあとは <see langword="null"/> で、表示中だけ値を返す。
        /// 型は ViewModel が宣言した結果型に固定され、factory の引数で渡される報告口と同じ配送先を指す。
        /// <para>
        /// 紐付けはインスタンスの同一性で引くため、等価な別インスタンスの報告口は返らない。
        /// 参照型の ViewModel だけがこの面に載る — 値型は boxing のたびに同一性が失われるため、
        /// 登録と型指定 show の <c>class</c> 制約で拒否され、残る show 経路でも提示前の実行時検査で弾かれる。
        /// </para>
        /// <example>
        /// <code>
        /// public sealed class ConfirmViewModel : IDialogViewModel
        /// {
        ///     public void OnOkTapped() => this.Notifier?.Complete(true);
        /// }
        /// </code>
        /// </example>
        /// </remarks>
        public DialogNotifier<TResult>? Notifier =>
            DialogNotifierBindings.ResultChannel(viewModel) is { } resultChannel
                ? new DialogNotifier<TResult>(resultChannel)
                : null;
    }
}
