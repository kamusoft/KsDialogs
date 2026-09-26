using System;
using Microsoft.Maui.Controls;

namespace KsDialogs;

/// <summary>
/// 基準領域 <see cref="DialogLayoutArea.CurrentPage"/> が使う「表示中のページ」をアプリから教える口。
/// </summary>
/// <remarks>
/// 既定では、ライブラリがダイアログを出すウィンドウのページ構成を辿って表示中のページを見つける
/// (モーダルで出したページ・<see cref="Shell.CurrentPage"/>・<see cref="FlyoutPage.Detail"/>・
/// <see cref="TabbedPage"/> の選択中の子・<see cref="NavigationPage.CurrentPage"/>)。
/// 独自の切り替えで画面を組んでいるなど、その辿り方で届かないアプリや、ページの一部の領域を基準にしたいアプリは、
/// 基準にするページまたは要素を返す関数をここに一度登録する。
/// <para>
/// 登録した関数は UI スレッドで、各表示の開始時と、表示中にウィンドウの寸法やシステムバーの幅が変わったときに呼ばれる。
/// 登録の差し替えは次の表示から効き、表示中のダイアログには影響しない。
/// </para>
/// </remarks>
public static class DialogCurrentPage
{
    private static volatile Func<VisualElement?>? s_provider;

    /// <summary>
    /// 表示中のページ (または基準にしたい要素) を返す関数。<see langword="null"/> を代入すると既定の探し方に戻る。
    /// </summary>
    /// <remarks>
    /// 関数が <see langword="null"/> を返したとき・例外を投げたとき・返した要素がまだ画面に描画されていないとき・
    /// ダイアログを出すウィンドウに載っていないときは、既定の探し方で得たページを使う。
    /// 登録は UI スレッドで行うことを推奨する。
    /// </remarks>
    public static Func<VisualElement?>? Provider
    {
        get => s_provider;
        set
        {
            s_provider = value;
#if IOS || ANDROID
            // Native 実装は表示の開始時に登録内容を捕まえるため、差し替えのたびに登録し直すと
            // 表示中のダイアログは古い関数のまま、次の表示から新しい関数が使われる
            PlatformDialogContent.RunOnUiThread(() => PlatformCurrentPage.Install(value));
#endif
        }
    }
}
