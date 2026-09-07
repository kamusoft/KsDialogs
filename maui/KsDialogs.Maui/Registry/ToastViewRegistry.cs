using System;
using System.Collections.Generic;
using System.Threading;
using Microsoft.Maui.Controls;

namespace KsDialogs;

/// <summary>
/// カスタム Toast の ViewModel 型をキーに MAUI View factory と ViewModel factory を引くレジストリ。
/// </summary>
/// <remarks>
/// Dialog のレジストリ (<see cref="DialogViewRegistry"/>) とも Loading のレジストリ
/// (<see cref="LoadingViewRegistry"/>) とも独立しており、同じ ViewModel 型をそれぞれへ別々の View で
/// 登録できる。片方の再登録は他方に影響しない。
/// 既定 singleton エントリ (<see cref="Toast.Instance"/>) と DI 注入で使うインスタンスは
/// <see cref="Shared"/> を共有するため、どちらの入口から登録しても同じ紐付けが引ける。
/// 登録・解決は任意のスレッドから行える。
/// <para>
/// このレジストリは MAUI 形態の層に属し、Native ライブラリ側のレジストリとは別物である
/// (maui/ADR-0001)。
/// </para>
/// <para>
/// 1 つの ViewModel 型のエントリは View factory と ViewModel factory の 2 スロットからなり、
/// 再登録はスロット単位の後勝ちで、もう片方のスロットは保持される。
/// </para>
/// </remarks>
public sealed class ToastViewRegistry
{
    /// <summary>全入口が共有する既定のレジストリ。</summary>
    public static ToastViewRegistry Shared { get; } = new();

    private readonly Lock _gate = new();
    private readonly Dictionary<Type, ToastRegistryEntry> _entries = [];

    /// <summary>
    /// ViewModel 型に対する MAUI View factory を登録する。同じスロットへの再登録は後勝ちで置き換える。
    /// </summary>
    /// <remarks>factory は表示のたびに呼ばれ、View を毎回新規に生成する (core/ADR-0005)。</remarks>
    /// <typeparam name="TViewModel">登録キーになる ViewModel の型。</typeparam>
    /// <param name="factory">中身の MAUI View を生成する関数。</param>
    public void Register<TViewModel>(Func<TViewModel, View> factory)
        where TViewModel : class, IToastViewModel
    {
        ArgumentNullException.ThrowIfNull(factory);

        StoreViewFactory(typeof(TViewModel), ToastViewFactories.Erase(factory));
    }

    /// <summary>
    /// ViewModel 型に対する ViewModel factory を登録する。
    /// </summary>
    /// <remarks>
    /// 型指定 show (<c>Show&lt;TViewModel&gt;()</c> の形) はこの factory で ViewModel を作る。
    /// factory は表示のたびに呼ばれ、View factory と同じく表示が受理されたあとの UI スレッドで
    /// 実行される。View factory とはスロットが別なので、どちらを登録し直しても他方は保持される。
    /// </remarks>
    /// <typeparam name="TViewModel">登録キーになる ViewModel の型。</typeparam>
    /// <param name="factory">ViewModel を生成する関数。</param>
    public void RegisterViewModel<TViewModel>(Func<TViewModel> factory)
        where TViewModel : class, IToastViewModel
    {
        ArgumentNullException.ThrowIfNull(factory);

        StoreViewModelFactory(typeof(TViewModel), () => factory());
    }

    /// <summary>型消去した View factory を該当スロットへ入れる。</summary>
    /// <param name="viewModelType">登録キーになる ViewModel の型。</param>
    /// <param name="factory">型消去した View factory。</param>
    internal void StoreViewFactory(Type viewModelType, ToastViewFactory factory) =>
        UpdateEntry(viewModelType, entry => entry with { ViewFactory = factory });

    /// <summary>型消去した ViewModel factory を該当スロットへ入れる。</summary>
    /// <param name="viewModelType">登録キーになる ViewModel の型。</param>
    /// <param name="factory">型消去した ViewModel factory。</param>
    internal void StoreViewModelFactory(Type viewModelType, ToastViewModelFactory factory) =>
        UpdateEntry(viewModelType, entry => entry with { ViewModelFactory = factory });

    /// <summary>
    /// キーに対応するエントリを返す。どちらのスロットも未登録なら <see langword="null"/>。
    /// </summary>
    /// <remarks>返すのは呼び出し時点のスナップショットで、以後の再登録には影響されない。</remarks>
    /// <param name="viewModelType">解決したい ViewModel の型。</param>
    /// <returns>登録済みのエントリ。未登録なら <see langword="null"/>。</returns>
    internal ToastRegistryEntry? Entry(Type viewModelType)
    {
        lock (_gate)
        {
            return _entries.TryGetValue(viewModelType, out ToastRegistryEntry? entry) ? entry : null;
        }
    }

    /// <summary>キーに対応する View factory を返す。未登録なら <see langword="null"/>。</summary>
    /// <param name="viewModelType">解決したい ViewModel の型。</param>
    /// <returns>登録済みの View factory。未登録なら <see langword="null"/>。</returns>
    internal ToastViewFactory? Factory(Type viewModelType) => Entry(viewModelType)?.ViewFactory;

    /// <summary>エントリの片方のスロットだけを置き換える。未登録の型なら空のエントリから作る。</summary>
    private void UpdateEntry(Type viewModelType, Func<ToastRegistryEntry, ToastRegistryEntry> update)
    {
        lock (_gate)
        {
            _entries[viewModelType] =
                update(_entries.TryGetValue(viewModelType, out ToastRegistryEntry? entry) ? entry : new());
        }
    }
}
