using System;

namespace KsDialogs.ApiSurfaceCheck.NegativeChecks;

/// <summary>none プリセットは引数を取らない (演出しないものに時間もイージングもない。core/ADR-0017)。</summary>
/// <remarks>
/// このソースはビルドプロパティ KsDialogsNegativeCheckNoneArguments を true にしたときだけ加わり、
/// <b>コンパイルエラーで失敗すること</b>が期待結果になる。
/// 期待する診断: CS1501 (引数 1 個を取る None のオーバーロードがない)
/// </remarks>
internal static class RejectsArgumentsOnNonePreset
{
    public static DialogTransition Build() => DialogTransition.None(TimeSpan.FromMilliseconds(200d));
}
