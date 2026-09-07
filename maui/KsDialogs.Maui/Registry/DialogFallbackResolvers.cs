using System;
using Microsoft.Maui.Controls;

namespace KsDialogs;

/// <summary>
/// 明示登録のない ViewModel 型を一括で解決する関数の組 (maui/ADR-0005)。
/// </summary>
/// <remarks>
/// 解決順序は「明示レジストリ → fallback → 構成ミスとして失敗」で、判定は View / ViewModel の
/// スロットごとに独立して働く。どちらの関数も明示登録が無いスロットでだけ呼ばれる。
/// </remarks>
/// <param name="View">
/// 未登録の ViewModel 型から中身の View を作る関数。<see langword="null"/> を返すと解決不能を表す。
/// 組全体で未設定なら <see langword="null"/>。
/// </param>
/// <param name="ViewModel">
/// 未登録の ViewModel 型から ViewModel を作る関数。<see langword="null"/> を返すと解決不能を表す。
/// 組全体で未設定なら <see langword="null"/>。
/// </param>
internal sealed record DialogFallbackResolvers(
    Func<Type, IServiceProvider, View?>? View = null,
    Func<Type, IServiceProvider, object?>? ViewModel = null);
