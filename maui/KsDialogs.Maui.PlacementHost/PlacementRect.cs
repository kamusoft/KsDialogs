namespace KsDialogs.PlacementHost;

/// <summary>
/// 画面上の矩形。単位と原点は OS ごとの測り方に従う (iOS は window 座標の pt、Android は画面座標の px)。
/// </summary>
/// <param name="Left">左端。</param>
/// <param name="Top">上端。</param>
/// <param name="Right">右端。</param>
/// <param name="Bottom">下端。</param>
internal readonly record struct PlacementRect(double Left, double Top, double Right, double Bottom)
{
    /// <summary>幅か高さが 0 以下か。</summary>
    public bool IsEmpty => !(Right - Left > 0 && Bottom - Top > 0);

    /// <summary>2 つの矩形の共通部分。</summary>
    /// <param name="other">重ねる矩形。</param>
    /// <returns>共通部分。重ならなければ空の矩形。</returns>
    public PlacementRect Intersect(PlacementRect other) => new(
        Math.Max(Left, other.Left),
        Math.Max(Top, other.Top),
        Math.Min(Right, other.Right),
        Math.Min(Bottom, other.Bottom));

    /// <summary>4 辺がそれぞれ許容誤差の内側で一致するか。</summary>
    /// <param name="other">比べる矩形。</param>
    /// <param name="tolerance">許容誤差。</param>
    /// <returns>一致すれば <see langword="true"/>。</returns>
    public bool Matches(PlacementRect other, double tolerance) =>
        Math.Abs(Left - other.Left) <= tolerance
        && Math.Abs(Top - other.Top) <= tolerance
        && Math.Abs(Right - other.Right) <= tolerance
        && Math.Abs(Bottom - other.Bottom) <= tolerance;

    /// <inheritdoc/>
    public override string ToString() => $"({Left:0.#},{Top:0.#})-({Right:0.#},{Bottom:0.#})";
}
