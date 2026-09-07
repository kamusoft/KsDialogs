namespace KsDialogs;

/// <summary>
/// ダイアログを基準領域のどこへ寄せるか。
/// </summary>
/// <remarks>
/// <see cref="Start"/> / <see cref="End"/> は物理方向 (水平軸なら左 / 右、垂直軸なら上 / 下) を指し、
/// 書字方向 (RTL) には追随しない。
/// </remarks>
public enum DialogAlignment
{
    /// <summary>有効領域の前端 (左 / 上) に寄せる。</summary>
    Start,

    /// <summary>有効領域の中央に置く。</summary>
    Center,

    /// <summary>有効領域の後端 (右 / 下) に寄せる。</summary>
    End,

    /// <summary>
    /// 位置だけでなくサイズも有効領域いっぱいに広げる。
    /// </summary>
    /// <remarks>
    /// 比率サイズが指定されている軸ではサイズの決め方として採用されず、位置は <see cref="Center"/> として扱う。
    /// </remarks>
    Fill,
}
