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
}
