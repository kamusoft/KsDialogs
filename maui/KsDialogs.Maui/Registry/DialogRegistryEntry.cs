namespace KsDialogs;

/// <summary>
/// ViewModel 型 1 つ分のレジストリのエントリ (core/ADR-0021)。
/// </summary>
/// <remarks>
/// View factory と ViewModel factory の 2 スロットを持ち、再登録は<b>スロット単位の後勝ち</b>で
/// 該当スロットだけを置き換える。片方だけを登録し直しても、もう片方は保持される。
/// </remarks>
/// <param name="ViewFactory">中身の View を生成する factory。インスタンス渡し show と型指定 show の両方が使う。</param>
/// <param name="ViewModelFactory">ViewModel を生成する factory。型指定 show だけが使う。</param>
internal sealed record DialogRegistryEntry(
    DialogViewFactory? ViewFactory = null,
    DialogViewModelFactory? ViewModelFactory = null);
