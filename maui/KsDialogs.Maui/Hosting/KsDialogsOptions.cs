using System;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Maui.Controls;

namespace KsDialogs;

/// <summary>
/// 一括解決 (fallback resolver) の設定面 (maui/ADR-0005)。
/// </summary>
/// <remarks>
/// 明示登録のない ViewModel 型をまとめて解決する規約を、DI チェーン上の一箇所で設定する。
/// static な差し込み口は用意しないため、後勝ちや <see langword="null"/> 上書きの事故は起こらない。
/// 解決順序は「明示レジストリ → fallback → 構成ミスとして失敗」で、判定は View / ViewModel の
/// スロットごとに独立して働く。
/// </remarks>
public sealed class KsDialogsOptions
{
    /// <summary>設定された View の一括解決関数。未設定なら <see langword="null"/>。</summary>
    internal Func<Type, IServiceProvider, View?>? ViewFallback { get; private set; }

    /// <summary>設定された ViewModel の一括解決関数。未設定なら <see langword="null"/>。</summary>
    internal Func<Type, IServiceProvider, object?>? ViewModelFallback { get; private set; }

    /// <summary>
    /// 明示登録のない ViewModel 型から中身の View を作る規約を設定する。
    /// </summary>
    /// <remarks>
    /// この関数は明示登録が無いときだけ呼ばれる。<see langword="null"/> を返すと解決不能を表し、
    /// その show は構成ミスとして失敗する (cancelled 等の結果には化けない)。
    /// 返した View には、ライブラリが表示対象の ViewModel を BindingContext として設定する。
    /// </remarks>
    /// <param name="resolver">ViewModel 型と provider から View を作る関数。</param>
    /// <returns>チェーンできるように自分自身。</returns>
    public KsDialogsOptions UseViewFallback(Func<Type, IServiceProvider, View?> resolver)
    {
        ArgumentNullException.ThrowIfNull(resolver);

        ViewFallback = resolver;
        return this;
    }

    /// <summary>
    /// 明示登録のない ViewModel 型を、サービスから引く既定の規約で解決する。
    /// </summary>
    /// <remarks>型指定 show で ViewModel factory が未登録のときに使われる。</remarks>
    /// <returns>チェーンできるように自分自身。</returns>
    public KsDialogsOptions UseViewModelFallback() =>
        UseViewModelFallback(static (viewModelType, provider) => provider.GetService(viewModelType));

    /// <summary>
    /// 明示登録のない ViewModel 型から ViewModel を作る規約を設定する。
    /// </summary>
    /// <remarks>
    /// この関数は明示登録が無いときだけ呼ばれる。<see langword="null"/> を返すと解決不能を表し、
    /// その型指定 show は構成ミスとして失敗する。
    /// </remarks>
    /// <param name="resolver">ViewModel 型と provider から ViewModel を作る関数。</param>
    /// <returns>チェーンできるように自分自身。</returns>
    public KsDialogsOptions UseViewModelFallback(Func<Type, IServiceProvider, object?> resolver)
    {
        ArgumentNullException.ThrowIfNull(resolver);

        ViewModelFallback = resolver;
        return this;
    }
}
