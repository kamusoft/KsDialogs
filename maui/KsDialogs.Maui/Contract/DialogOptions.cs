using Microsoft.Maui;
using Microsoft.Maui.Graphics;

namespace KsDialogs;

/// <summary>
/// ダイアログの器に渡す静的メタ属性。
/// </summary>
/// <remarks>
/// その中身の性質として決まる値の組であり、表示のたびに変えるものではない。
/// そのため供給経路は中身 (View) への添付だけで、show の引数では渡せない (core/ADR-0014・0015)。
/// 添付は項目ごとのスカラー (<see cref="Dialog.OverlayColorProperty"/> など) で行い、
/// 束ねたこの型が Native への輸送単位になる。
/// <para>
/// ダイアログの供給面は <c>Dialog.*</c> の添付プロパティであってこの型ではないが、既定ローディングは
/// 利用者が属性を添付する View を持たないため、<see cref="IKsLoading.Options"/> がこの型を直接受け取る
/// 供給経路になる (core/ADR-0022)。そのため公開型として扱う (供給経路のない公開型ではない)。
/// </para>
/// <para>
/// 全項目に既定値があり、何も添付しなければ契約の既定値で動く。
/// 無効値 (非有限値・負の余白・範囲外の比率) の丸めは Native 実装の責務なので、ここでは値を検査しない。
/// </para>
/// </remarks>
public sealed record DialogOptions
{
    /// <summary>サイズと位置の計算の基準になる領域。既定は可視領域。</summary>
    public DialogLayoutArea LayoutArea { get; init; } = DialogLayoutArea.VisibleArea;

    /// <summary>基準 rect の各辺から控除する余白。最大サイズと配置の両方に効く。既定は全辺 24。</summary>
    public Thickness DialogMargin { get; init; } = new(24d);

    /// <summary>
    /// 基準 rect の幅に対する比率 (有効域 0 &lt; 値 ≤ 1)。既定は未指定 (-1)。
    /// </summary>
    public double ProportionalWidth { get; init; } = -1d;

    /// <summary>基準 rect の高さに対する比率。扱いは <see cref="ProportionalWidth"/> と同じ。</summary>
    public double ProportionalHeight { get; init; } = -1d;

    /// <summary>ダイアログの背後を覆う色。既定は黒の 40% 不透明。色を持たない指定は透明として扱う。</summary>
    public Color? OverlayColor { get; init; } = Color.FromUint(0x66000000u);

    /// <summary>
    /// ダイアログ外形 rect の外側へのタップを、キャンセルと同じ経路で閉じる操作として扱うか。既定は true。
    /// </summary>
    /// <remarks>
    /// false のとき外側タップは何も起こさず、タップは背後の画面へ透過しない。
    /// Loading ではこの項目は常に無効で、設定しても効かない (core/ADR-0022)。
    /// </remarks>
    public bool IsCanceledOnTouchOutside { get; init; } = true;

    /// <summary>背後を覆う色を interop 境界の表現 (ARGB 32bit 整数) にしたもの。</summary>
    /// <remarks>色を持たない指定は覆いを描かない指定にあたるため、透明として運ぶ。</remarks>
    internal int OverlayColorArgb => (OverlayColor ?? Colors.Transparent).ToInt();
}
