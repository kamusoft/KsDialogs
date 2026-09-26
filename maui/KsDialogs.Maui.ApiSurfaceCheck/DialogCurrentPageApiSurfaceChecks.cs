using System;
using Microsoft.Maui.Controls;

namespace KsDialogs.ApiSurfaceCheck;

/// <summary>
/// 基準領域「表示中のページ」の公開 API 形状の正の検証。
/// </summary>
/// <remarks>
/// このファイルがコンパイルできることが検証結果であり、公開すべき値・登録口が
/// 利用者から見えなくなればビルドが失敗する。
/// </remarks>
public static class DialogCurrentPageApiSurfaceChecks
{
    /// <summary>基準領域に表示中のページを選べる (添付・静的メタ属性の両方)。</summary>
    /// <param name="contentView">ダイアログの中身になる View。</param>
    /// <returns>表示中のページを基準にする静的メタ属性。</returns>
    public static DialogOptions AcceptsCurrentPageLayoutArea(View contentView)
    {
        Dialog.SetLayoutArea(contentView, DialogLayoutArea.CurrentPage);
        return new DialogOptions { LayoutArea = DialogLayoutArea.CurrentPage };
    }

    /// <summary>表示中のページはページを返す関数で上書きでき、null の代入で解除できる。</summary>
    /// <param name="page">基準にするページ。</param>
    public static void AcceptsPageProvider(Page page)
    {
        DialogCurrentPage.Provider = () => page;
        DialogCurrentPage.Provider = null;
    }

    /// <summary>ページの中の要素を返す関数でも上書きできる。</summary>
    /// <param name="element">基準にする要素。</param>
    /// <returns>登録されている関数。</returns>
    public static Func<VisualElement?>? AcceptsVisualElementProvider(VisualElement element)
    {
        DialogCurrentPage.Provider = () => element;
        return DialogCurrentPage.Provider;
    }
}
