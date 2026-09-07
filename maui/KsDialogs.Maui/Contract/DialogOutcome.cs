namespace KsDialogs;

/// <summary>
/// 結果の型消去表現。
/// </summary>
/// <remarks>
/// 公開 API の <see cref="DialogResult{TResult}"/> と違い結果値の型を持たない、
/// レジストリ・提示層・委譲面が共通に使う輸送形。宣言結果型への復元は <see cref="Dialog"/> が行う。
/// </remarks>
internal abstract record DialogOutcome
{
    // 派生を Completed / Cancelled の 2 つに閉じるため、基底のコンストラクタは入れ子の型からしか呼べない
    private DialogOutcome()
    {
    }

    /// <summary>結果値つきの完了。</summary>
    /// <param name="Value">報告された結果値。</param>
    public sealed record Completed(object? Value) : DialogOutcome;

    /// <summary>結果値を持たないキャンセル。</summary>
    public sealed record Cancelled : DialogOutcome
    {
        /// <summary>値を持たないため使い回せる唯一の実体。</summary>
        public static Cancelled Instance { get; } = new();
    }
}
