using Microsoft.Maui.Graphics;

namespace KsDialogs;

/// <summary>
/// Toast の一括設定 (core/ADR-0032)。
/// </summary>
/// <remarks>
/// 項目は適用範囲で 2 種に分かれる。
/// <list type="bullet">
/// <item>
/// <b>視覚項目</b> (<see cref="BackgroundColor"/> / <see cref="TextColor"/> /
/// <see cref="FontSize"/> / <see cref="CornerRadius"/>) はライブラリ同梱のデフォルト View にだけ効き、
/// カスタム Toast View には効かない
/// </item>
/// <item>
/// <b>既定値項目</b> (<see cref="DefaultDuration"/> / <see cref="DefaultPlacement"/>) は
/// デフォルト・カスタムを問わずすべての Toast に効く (表示 API で該当引数・添付を省略したときの既定)
/// </item>
/// </list>
/// <para>
/// 供給経路は <see cref="IKsToast.Style"/> への一括設定だけで、表示 API の引数では渡せない。
/// 器は各表示の受理時にこの値を読むため、設定の変更は次の表示から効く。
/// 見えの実装は Native 実装が持ち、MAUI 形態は値をそのまま引き渡す (core/ADR-0001)。
/// </para>
/// </remarks>
public sealed record ToastStyle
{
    /// <summary>ライブラリが持つ既定 duration (ミリ秒)。</summary>
    /// <remarks><see cref="DefaultDuration"/> が成立しないときの最後の拠り所。</remarks>
    public const int BuiltinDefaultDuration = 1500;

    /// <summary>デフォルト View のピルの既定の地色。OS 慣習に寄せた半透明のダークグレー。</summary>
    public static Color BuiltinBackgroundColor { get; } = Color.FromRgba(0x32, 0x32, 0x32, 0xEB);

    /// <summary>デフォルト View のピルの地色。</summary>
    public Color BackgroundColor { get; init; } = BuiltinBackgroundColor;

    /// <summary>デフォルト View のメッセージの文字色。既定は白。</summary>
    public Color TextColor { get; init; } = Colors.White;

    /// <summary>デフォルト View のメッセージの文字の大きさ (論理単位)。既定は 14。</summary>
    public double FontSize { get; init; } = 14d;

    /// <summary>デフォルト View のピルの角丸半径 (論理単位)。既定は 22。</summary>
    /// <remarks>複数行になっても変わらない固定値として扱う。</remarks>
    public double CornerRadius { get; init; } = 22d;

    /// <summary>duration を省略した表示に使うミリ秒。既定は 1500。</summary>
    /// <remarks>0 以下を設定した場合は内蔵既定 (<see cref="BuiltinDefaultDuration"/>) へ丸められる。</remarks>
    public int DefaultDuration { get; init; } = BuiltinDefaultDuration;

    /// <summary>
    /// アプリ全体の既定配置。<see langword="null"/> なら Toast の契約既定値
    /// (可視領域の下部中央 + 上方向オフセット)。
    /// </summary>
    public DialogPlacement? DefaultPlacement { get; init; }

    /// <summary>ピルの地色を interop 境界の表現 (ARGB 32bit 整数) にしたもの。</summary>
    internal int BackgroundColorArgb => (BackgroundColor ?? BuiltinBackgroundColor).ToInt();

    /// <summary>メッセージの文字色を interop 境界の表現 (ARGB 32bit 整数) にしたもの。</summary>
    internal int TextColorArgb => (TextColor ?? Colors.White).ToInt();
}
