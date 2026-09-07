using System.Threading.Tasks;
using Microsoft.Maui.Controls;

namespace KsDialogs.ApiSurfaceCheck;

/// <summary>真偽値の顔で宣言した、利用者が書くのと同じ形の ViewModel。結果型は書かない。</summary>
/// <param name="Message">ダイアログに出す文言。</param>
public sealed record ConsumerSimpleDialogViewModel(string Message = "こんにちは") : IDialogViewModel;

/// <summary>カスタム結果型を宣言する、利用者が書くのと同じ形の ViewModel。</summary>
/// <param name="Message">ダイアログに出す文言。</param>
public sealed record ConsumerTextDialogViewModel(string Message = "こんにちは") : IDialogViewModel<string>;

/// <summary>
/// 真偽値の省略形とインライン show の公開 API 形状の正の検証。
/// </summary>
/// <remarks>
/// このファイルがコンパイルできることが検証結果であり、型引数を明示しなくても
/// 利用者が意図どおりに書けることを示す。型が意図と違えば代入先の型注釈で失敗する。
/// </remarks>
public static class DialogExpandedApiSurfaceChecks
{
    /// <summary>ViewModel 型と結果型の 2 つを明示する形はそのまま通る。</summary>
    /// <param name="registry">紐付けを持つレジストリ。</param>
    public static void AcceptsBothTypeArgumentsOnRegister(DialogViewRegistry registry)
    {
        registry.Register<ConsumerTextDialogViewModel, string>((_, notifier) =>
        {
            notifier.Complete("結果");
            return new Label();
        });
    }

    /// <summary>真偽値の顔なら ViewModel 型だけを型引数に書けば登録できる。</summary>
    /// <param name="registry">紐付けを持つレジストリ。</param>
    public static void AcceptsViewModelOnlyTypeArgumentOnRegister(DialogViewRegistry registry)
    {
        registry.Register<ConsumerSimpleDialogViewModel>((_, notifier) =>
        {
            DialogNotifier<bool> boundNotifier = notifier;
            boundNotifier.Complete(true);
            return new Label();
        });
    }

    /// <summary>型引数を書かず、明示型付きラムダの型から真偽値の省略形が選ばれる。</summary>
    /// <param name="registry">紐付けを持つレジストリ。</param>
    public static void AcceptsInferredRegisterForSimpleFace(DialogViewRegistry registry)
    {
        registry.Register((ConsumerSimpleDialogViewModel _, DialogNotifier<bool> notifier) =>
        {
            notifier.Complete(true);
            return new Label();
        });
    }

    /// <summary>型引数を書かず、明示型付きラムダの型からカスタム結果型の形が選ばれる。</summary>
    /// <param name="registry">紐付けを持つレジストリ。</param>
    public static void AcceptsInferredRegisterForCustomResult(DialogViewRegistry registry)
    {
        registry.Register((ConsumerTextDialogViewModel _, DialogNotifier<string> notifier) =>
        {
            notifier.Complete("結果");
            return new Label();
        });
    }

    /// <summary>真偽値の顔で宣言した ViewModel の show は、真偽値の結果を返す。</summary>
    /// <param name="dialogs">表示の入口。</param>
    /// <returns>その show の結果。</returns>
    public static Task<DialogResult<bool>> AcceptsSimpleFaceShow(IKsDialog dialogs) =>
        dialogs.ShowAsync(new ConsumerSimpleDialogViewModel());

    /// <summary>登録せずに factory をその場で渡して表示でき、結果は真偽値に型付く。</summary>
    /// <param name="dialogs">表示の入口。</param>
    /// <returns>その show の結果。</returns>
    public static Task<DialogResult<bool>> AcceptsInlineShow(IKsDialog dialogs) =>
        dialogs.ShowAsync(new ConsumerSimpleDialogViewModel(), (_, notifier) =>
        {
            DialogNotifier<bool> boundNotifier = notifier;
            boundNotifier.Complete(true);
            return new Label();
        });

    /// <summary>インライン show でも置き場所を引数で供給できる。</summary>
    /// <param name="dialogs">表示の入口。</param>
    /// <returns>その show の結果。</returns>
    public static Task<DialogResult<bool>> AcceptsInlineShowWithPlacement(IKsDialog dialogs) =>
        dialogs.ShowAsync(
            new ConsumerSimpleDialogViewModel(),
            (_, _) => new Label(),
            new DialogPlacement { HorizontalAlignment = DialogAlignment.Start });

    /// <summary>
    /// 結果型を宣言した ViewModel でも、インライン show の結果はその型で返る。
    /// </summary>
    /// <remarks>
    /// 結果型はラムダの引数型からしか導けないため、真偽値以外ではラムダの引数型を明示するか
    /// 型引数を 2 つとも書く。登録の 2 型引数の形と同じ書き分けになる。
    /// </remarks>
    /// <param name="dialogs">表示の入口。</param>
    /// <returns>その show の結果。</returns>
    public static Task<DialogResult<string>> AcceptsInlineShowWithDeclaredResultType(IKsDialog dialogs) =>
        dialogs.ShowAsync(
            new ConsumerTextDialogViewModel(),
            (ConsumerTextDialogViewModel _, DialogNotifier<string> notifier) =>
            {
                notifier.Complete("結果");
                return new Label();
            });
}
