using System;
using System.Globalization;
using Microsoft.Maui.Graphics;

namespace KsDialogs;

/// <summary>
/// 既定ローディング (ライブラリ同梱の内蔵コンテンツ) の見た目の設定 (core/ADR-0023)。
/// </summary>
/// <remarks>
/// 設定できるのは内蔵コンテンツ固有の項目だけで、器のレイアウト属性 (配置・余白・覆いの色) は
/// <see cref="DialogPlacement"/> / <see cref="DialogOptions"/> が受け持つ。
/// 重複した属性はここに置かない。
/// <para>
/// 供給経路は <see cref="IKsLoading.Style"/> への一括設定だけで、表示 API の引数では渡せない。
/// 器は各表示の開始時にこの値を読むため、設定の変更は次の表示から効く。
/// 見えの実装は Native 実装が持ち、MAUI 形態は値をそのまま引き渡す (core/ADR-0001)。
/// </para>
/// </remarks>
public sealed record LoadingStyle
{
    /// <summary>回転インジケータの色。既定は白。</summary>
    public Color IndicatorColor { get; init; } = Colors.White;

    /// <summary>メッセージの文字の大きさ (論理単位)。既定は 14。</summary>
    public double MessageFontSize { get; init; } = 14d;

    /// <summary>メッセージの文字色。既定は白。</summary>
    public Color MessageColor { get; init; } = Colors.White;

    /// <summary>
    /// メッセージを省略して表示したときに使う文言。
    /// <see langword="null"/> ならメッセージなしで表示する。
    /// </summary>
    public string? DefaultMessage { get; init; }

    /// <summary>
    /// メッセージと進捗から表示テキストを組み立てる関数 (core/ADR-0023)。
    /// </summary>
    /// <remarks>
    /// 進捗が未報告のときは第 2 引数が <see langword="null"/> になる。
    /// 呼び出しは UI スレッド上で行われる。
    /// </remarks>
    public Func<string?, double?, string> ProgressFormat { get; init; } = DefaultProgressFormat;

    /// <summary>ライブラリ既定の組み立て方。</summary>
    /// <remarks>
    /// 進捗が未報告ならメッセージだけを返し、報告済みなら百分率 (小数点以下は四捨五入) を
    /// 改行で続ける。メッセージが無ければ百分率だけを返す。
    /// </remarks>
    public static Func<string?, double?, string> DefaultProgressFormat { get; } =
        static (message, progress) =>
        {
            string? trimmedMessage = string.IsNullOrEmpty(message) ? null : message;
            if (progress is not double value)
            {
                return trimmedMessage ?? string.Empty;
            }

            int rounded = (int)Math.Round(value * 100d, MidpointRounding.AwayFromZero);
            string percentage = string.Create(CultureInfo.CurrentCulture, $"{rounded}%");
            return trimmedMessage is null ? percentage : $"{trimmedMessage}\n{percentage}";
        };

    /// <summary>インジケータの色を interop 境界の表現 (ARGB 32bit 整数) にしたもの。</summary>
    internal int IndicatorColorArgb => (IndicatorColor ?? Colors.White).ToInt();

    /// <summary>メッセージの文字色を interop 境界の表現 (ARGB 32bit 整数) にしたもの。</summary>
    internal int MessageColorArgb => (MessageColor ?? Colors.White).ToInt();
}
