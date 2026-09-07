namespace KsDialogs;

/// <summary>
/// レジストリが保持する ViewModel 生成関数の型消去表現 (core/ADR-0021)。
/// </summary>
/// <remarks>
/// 型指定 show はこれで ViewModel を作ってから configure・View 生成へ進む。生成は UI スレッドで行われる。
/// </remarks>
/// <returns>登録キーの型の ViewModel。</returns>
internal delegate object DialogViewModelFactory();
