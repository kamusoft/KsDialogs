namespace KsDialogs;

/// <summary>
/// ダイアログの器に渡す動的メタ属性 (置き場所)。
/// </summary>
/// <remarks>
/// 同じ中身を呼び出しごとに違う位置へ出せるよう、ダイアログの中身 (View) への添付に加えて
/// show の引数でも渡せる (core/ADR-0015)。show の引数で渡した値は、添付された置き場所を
/// <b>オブジェクトまるごと置換</b>する (項目単位では合成しない)。
/// <para>
/// 全項目に既定値があるため、変えたい項目だけを指定すればよい。
/// 数値は論理単位で、非有限値は 0 として扱われる。丸めもレイアウト計算も Native 実装の責務で、
/// MAUI 側は指定された値をそのまま引き渡す (core/ADR-0001)。
/// </para>
/// </remarks>
public sealed record DialogPlacement
{
    /// <summary>水平方向の配置。既定は中央。</summary>
    public DialogAlignment HorizontalAlignment { get; init; } = DialogAlignment.Center;

    /// <summary>垂直方向の配置。既定は中央。</summary>
    public DialogAlignment VerticalAlignment { get; init; } = DialogAlignment.Center;

    /// <summary>配置を決めた後に加える水平方向の移動量。正の値で右へ動く。既定は 0。</summary>
    public double OffsetX { get; init; }

    /// <summary>配置を決めた後に加える垂直方向の移動量。正の値で下へ動く。既定は 0。</summary>
    public double OffsetY { get; init; }
}
