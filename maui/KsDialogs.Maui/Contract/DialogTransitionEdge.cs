namespace KsDialogs;

/// <summary>
/// 滑り込み・滑り出しの出入り口になる辺。
/// </summary>
/// <remarks>
/// <see cref="Start"/> と <see cref="End"/> はレイアウト方向に追随し、右から左へ読む環境では
/// <see cref="Start"/> が右、<see cref="End"/> が左になる。
/// <see cref="Top"/> と <see cref="Bottom"/> は画面上の物理方向で、レイアウト方向では変わらない。
/// </remarks>
public enum DialogTransitionEdge
{
    /// <summary>上辺。</summary>
    Top,

    /// <summary>下辺。</summary>
    Bottom,

    /// <summary>行の始まり側の辺。</summary>
    Start,

    /// <summary>行の終わり側の辺。</summary>
    End,
}
