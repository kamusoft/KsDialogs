namespace KsDialogs;

/// <summary>
/// サイズと位置の計算を行う基準領域。
/// </summary>
/// <remarks>
/// 水平・垂直の両軸に効く (core/ADR-0008)。
/// </remarks>
public enum DialogLayoutArea
{
    /// <summary>ダイアログを載せるウィンドウの全体。</summary>
    Window,

    /// <summary>ウィンドウからシステムバーなどが占める余白を控除した可視領域。</summary>
    VisibleArea,

    /// <summary>表示中のページの領域。タブバーやナビゲーションバーを持つ画面では、それらのバーを除いた領域になる。</summary>
    /// <remarks>
    /// 表示中のページは次の順で探し、見つかった最初のものを使う。
    /// <list type="number">
    /// <item><description><see cref="DialogCurrentPage.Provider"/> に登録した関数が返す要素</description></item>
    /// <item><description>ダイアログを出すウィンドウの表示中のページ (モーダルで出したページがあればその先頭、
    /// 無ければ <see cref="Microsoft.Maui.Controls.Window.Page"/> から、<see cref="Microsoft.Maui.Controls.Shell"/>・
    /// <see cref="Microsoft.Maui.Controls.FlyoutPage"/>・<see cref="Microsoft.Maui.Controls.TabbedPage"/>・
    /// <see cref="Microsoft.Maui.Controls.NavigationPage"/> の表示中の子を辿った先のページ)</description></item>
    /// </list>
    /// どちらからもページが得られないとき (ページがまだ画面に描画されていないときを含む) は
    /// <see cref="VisibleArea"/> と同じ結果になる。
    /// </remarks>
    CurrentPage,
}
