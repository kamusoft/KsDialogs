namespace KsDialogs;

/// <summary>
/// Toast レジストリの ViewModel 型 1 つ分のエントリ (core/ADR-0035)。
/// </summary>
/// <remarks>
/// View factory と ViewModel factory の 2 スロットを持ち、再登録は<b>スロット単位の後勝ち</b>で
/// 該当スロットだけを置き換える。片方だけを登録し直しても、もう片方は保持される。
/// </remarks>
/// <param name="ViewFactory">中身の View を生成する factory。インスタンス渡し show と型指定 show の両方が使う。</param>
/// <param name="ViewModelFactory">ViewModel を生成する factory。型指定 show だけが使う。</param>
internal sealed record ToastRegistryEntry(
    ToastViewFactory? ViewFactory = null,
    ToastViewModelFactory? ViewModelFactory = null);

/// <summary>
/// Toast レジストリが保持する ViewModel 生成関数の型消去表現 (core/ADR-0035)。
/// </summary>
/// <remarks>
/// 型指定 show はこれで ViewModel を作ってから configure・View 生成へ進む。生成は、中身の生成と同じく
/// 表示が受理されたあとの UI スレッドで行われる。
/// </remarks>
/// <returns>登録キーの型の ViewModel。</returns>
internal delegate object ToastViewModelFactory();
