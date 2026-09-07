namespace KsDialogs.Sample.Maui;

/// <summary>
/// 登録経路のカスタム Toast の ViewModel。
/// </summary>
/// <remarks>
/// Toast は結果も進捗も持たないため、この型はデータの運搬とレジストリの型キーだけを担う。
/// 中身の View との紐付けは DI チェーンの 1 行登録 (<c>RegisterForToast</c>) が行う。
/// </remarks>
public sealed class CustomToastViewModel : IToastViewModel
{
    /// <summary>Toast に表示するメッセージ。表示の直前に設定する。</summary>
    public string Message { get; set; } = string.Empty;
}
