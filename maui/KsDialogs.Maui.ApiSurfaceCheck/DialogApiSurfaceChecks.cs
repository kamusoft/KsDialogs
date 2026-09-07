using System.Threading.Tasks;
using Microsoft.Maui;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Graphics;

namespace KsDialogs.ApiSurfaceCheck;

/// <summary>利用者が書くのと同じ形の ViewModel。真偽値の結果を宣言する。</summary>
/// <param name="Message">ダイアログに出す文言。</param>
public sealed record ConsumerDialogViewModel(string Message = "こんにちは") : IDialogViewModel<bool>;

/// <summary>
/// 公開 API 形状の正の検証。
/// </summary>
/// <remarks>
/// このファイルがコンパイルできることが検証結果であり、公開すべき型・メンバが
/// 利用者から見えなくなればビルドが失敗する。
/// </remarks>
public static class DialogApiSurfaceChecks
{
    /// <summary>メタ属性を供給しない既存の呼び出しがそのまま通る。</summary>
    /// <param name="dialogs">表示の入口。</param>
    /// <returns>その show の結果。</returns>
    public static Task<DialogResult<bool>> AcceptsShowWithoutPlacement(IKsDialog dialogs) =>
        dialogs.ShowAsync(new ConsumerDialogViewModel());

    /// <summary>置き場所は show の引数で供給できる。</summary>
    /// <param name="dialogs">表示の入口。</param>
    /// <returns>その show の結果。</returns>
    public static Task<DialogResult<bool>> AcceptsShowWithPlacement(IKsDialog dialogs) =>
        dialogs.ShowAsync(
            new ConsumerDialogViewModel(),
            new DialogPlacement { HorizontalAlignment = DialogAlignment.Start, OffsetX = 12d });

    /// <summary>メタ属性は中身の View への添付で供給できる。</summary>
    /// <param name="contentView">ダイアログの中身になる View。</param>
    public static void AcceptsAttachmentOnContentView(View contentView)
    {
        Dialog.SetLayoutArea(contentView, DialogLayoutArea.Window);
        Dialog.SetDialogMargin(contentView, new Thickness(8d));
        Dialog.SetProportionalWidth(contentView, 0.5d);
        Dialog.SetProportionalHeight(contentView, 0.5d);
        Dialog.SetOverlayColor(contentView, Colors.Transparent);
        Dialog.SetIsCanceledOnTouchOutside(contentView, false);
        Dialog.SetHorizontalAlignment(contentView, DialogAlignment.Start);
        Dialog.SetVerticalAlignment(contentView, DialogAlignment.End);
        Dialog.SetOffsetX(contentView, 12d);
        Dialog.SetOffsetY(contentView, -12d);
    }

    /// <summary>添付した値は同じ名前で読み出せる。</summary>
    /// <param name="contentView">ダイアログの中身になる View。</param>
    /// <returns>読み出した外側タップの扱い。</returns>
    public static bool AcceptsReadingAttachedAttributes(View contentView)
    {
        _ = Dialog.GetLayoutArea(contentView);
        _ = Dialog.GetDialogMargin(contentView);
        _ = Dialog.GetProportionalWidth(contentView);
        _ = Dialog.GetProportionalHeight(contentView);
        _ = Dialog.GetOverlayColor(contentView);
        _ = Dialog.GetHorizontalAlignment(contentView);
        _ = Dialog.GetVerticalAlignment(contentView);
        _ = Dialog.GetOffsetX(contentView);
        _ = Dialog.GetOffsetY(contentView);
        return Dialog.GetIsCanceledOnTouchOutside(contentView);
    }

    /// <summary>ViewModel が宣言した結果型でそのまま受け取れる。</summary>
    /// <param name="dialogs">表示の入口。</param>
    /// <returns>その show の結果。</returns>
    public static async Task<bool> AcceptsDeclaredResultType(IKsDialog dialogs)
    {
        DialogResult<bool> result = await dialogs.ShowAsync(new ConsumerDialogViewModel());
        return result is DialogResult<bool>.Completed;
    }

    /// <summary>結果報告口も宣言結果型に固定される。</summary>
    /// <param name="registry">紐付けを持つレジストリ。</param>
    public static void NotifierIsBoundToDeclaredResultType(DialogViewRegistry registry)
    {
        registry.Register((ConsumerDialogViewModel _, DialogNotifier<bool> notifier) =>
        {
            notifier.Complete(true);
            return new Label();
        });
    }
}
