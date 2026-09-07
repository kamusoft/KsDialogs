using System;
using System.Collections.Generic;
using System.Threading;
using Microsoft.Maui.Controls;

namespace KsDialogs;

/// <summary>
/// ViewModel 型をキーに MAUI View factory と ViewModel factory を引くレジストリ (core/ADR-0004・0021)。
/// </summary>
/// <remarks>
/// キーは ViewModel の型そのもので、リフレクションによる型探索は行わない。
/// 既定 singleton エントリ (<see cref="Dialog.Instance"/>) と DI 注入で使うインスタンスは
/// <see cref="Shared"/> を共有するため、どちらの入口から登録しても同じ紐付けが引ける。
/// 登録・解決は任意のスレッドから行える。
/// このレジストリは MAUI 形態の層に属し、Native ライブラリ側のレジストリとは別物である (maui/ADR-0001)。
/// <para>
/// 1 つの ViewModel 型のエントリは View factory と ViewModel factory の 2 スロットからなり、
/// 再登録はスロット単位の後勝ちで、もう片方のスロットは保持される。
/// </para>
/// </remarks>
public sealed class DialogViewRegistry
{
    /// <summary>全入口が共有する既定のレジストリ。</summary>
    public static DialogViewRegistry Shared { get; } = new();

    private readonly Lock _gate = new();
    private readonly Dictionary<Type, DialogRegistryEntry> _entries = [];
    private DialogFallbackResolvers? _fallbacks;

    /// <summary>
    /// ViewModel 型に対する MAUI View factory を登録する。同じスロットへの再登録は後勝ちで置き換える。
    /// </summary>
    /// <remarks>
    /// factory は show のたびに呼ばれ、View を毎回新規に生成する (core/ADR-0005)。
    /// 受け取る <see cref="DialogNotifier{TResult}"/> は ViewModel が宣言した結果型に固定され、
    /// show 1 回ごとに新しいものが渡る。
    /// 報告口を引数で受け取らず、中身から <c>viewModel.Notifier</c> で引く登録の形もある。
    /// </remarks>
    /// <typeparam name="TViewModel">登録キーになる ViewModel の型。</typeparam>
    /// <typeparam name="TResult">ViewModel が宣言する結果値の型。</typeparam>
    /// <param name="factory">中身の MAUI View を生成する関数。</param>
    public void Register<TViewModel, TResult>(Func<TViewModel, DialogNotifier<TResult>, View> factory)
        where TViewModel : class, IDialogViewModel<TResult>
    {
        ArgumentNullException.ThrowIfNull(factory);

        StoreViewFactory(typeof(TViewModel), DialogViewFactories.Erase(factory));
    }

    /// <summary>
    /// 真偽値の顔で宣言した ViewModel 型に対する MAUI View factory を登録する (core/ADR-0012)。
    /// </summary>
    /// <remarks>
    /// 結果型を型引数に書かない省略形で、挙動は 2 型引数の
    /// <see cref="Register{TViewModel, TResult}(Func{TViewModel, DialogNotifier{TResult}, View})"/> に
    /// 真偽値を渡した場合とまったく同じ。
    /// 受け取る <see cref="DialogNotifier{TResult}"/> は真偽値の報告口になり、
    /// show は <c>DialogResult&lt;bool&gt;</c> を返す。
    /// </remarks>
    /// <typeparam name="TViewModel">登録キーになる ViewModel の型。</typeparam>
    /// <param name="factory">中身の MAUI View を生成する関数。</param>
    public void Register<TViewModel>(Func<TViewModel, DialogNotifier<bool>, View> factory)
        where TViewModel : class, IDialogViewModel
    {
        ArgumentNullException.ThrowIfNull(factory);

        StoreViewFactory(typeof(TViewModel), DialogViewFactories.Erase(factory));
    }

    /// <summary>
    /// ViewModel 型に対する MAUI View factory を、ViewModel だけを受け取る形で登録する (core/ADR-0018)。
    /// </summary>
    /// <remarks>
    /// 結果報告口は中身の中から <c>viewModel.Notifier</c> で取り出す。
    /// 紐付けは提示層が factory を呼ぶ前に済ませているため、factory 本体からも読める。
    /// </remarks>
    /// <typeparam name="TViewModel">登録キーになる ViewModel の型。</typeparam>
    /// <typeparam name="TResult">ViewModel が宣言する結果値の型。</typeparam>
    /// <param name="factory">中身の MAUI View を生成する関数。</param>
    public void Register<TViewModel, TResult>(Func<TViewModel, View> factory)
        where TViewModel : class, IDialogViewModel<TResult>
    {
        ArgumentNullException.ThrowIfNull(factory);

        StoreViewFactory(typeof(TViewModel), DialogViewFactories.Erase(factory));
    }

    /// <summary>
    /// 真偽値の顔で宣言した ViewModel 型に対する MAUI View factory を、ViewModel だけを受け取る形で
    /// 登録する (core/ADR-0012・0018)。
    /// </summary>
    /// <typeparam name="TViewModel">登録キーになる ViewModel の型。</typeparam>
    /// <param name="factory">中身の MAUI View を生成する関数。</param>
    public void Register<TViewModel>(Func<TViewModel, View> factory)
        where TViewModel : class, IDialogViewModel
    {
        ArgumentNullException.ThrowIfNull(factory);

        StoreViewFactory(typeof(TViewModel), DialogViewFactories.Erase(factory));
    }

    /// <summary>
    /// ViewModel 型に対する ViewModel factory を登録する (core/ADR-0021)。
    /// </summary>
    /// <remarks>
    /// 型指定 show (<c>ShowAsync&lt;TViewModel&gt;()</c> の形) はこの factory で ViewModel を作る。
    /// factory は show のたびに呼ばれ、UI スレッドで実行される。
    /// View factory とはスロットが別なので、どちらを登録し直しても他方は保持される。
    /// </remarks>
    /// <typeparam name="TViewModel">登録キーになる ViewModel の型。</typeparam>
    /// <typeparam name="TResult">ViewModel が宣言する結果値の型。</typeparam>
    /// <param name="factory">ViewModel を生成する関数。</param>
    public void RegisterViewModel<TViewModel, TResult>(Func<TViewModel> factory)
        where TViewModel : class, IDialogViewModel<TResult>
    {
        ArgumentNullException.ThrowIfNull(factory);

        StoreViewModelFactory(typeof(TViewModel), () => factory());
    }

    /// <summary>
    /// 真偽値の顔で宣言した ViewModel 型に対する ViewModel factory を登録する
    /// (core/ADR-0012・0021)。
    /// </summary>
    /// <typeparam name="TViewModel">登録キーになる ViewModel の型。</typeparam>
    /// <param name="factory">ViewModel を生成する関数。</param>
    public void RegisterViewModel<TViewModel>(Func<TViewModel> factory)
        where TViewModel : class, IDialogViewModel
    {
        ArgumentNullException.ThrowIfNull(factory);

        StoreViewModelFactory(typeof(TViewModel), () => factory());
    }

    /// <summary>型消去した View factory を該当スロットへ入れる。</summary>
    /// <param name="viewModelType">登録キーになる ViewModel の型。</param>
    /// <param name="factory">型消去した View factory。</param>
    internal void StoreViewFactory(Type viewModelType, DialogViewFactory factory) =>
        UpdateEntry(viewModelType, entry => entry with { ViewFactory = factory });

    /// <summary>型消去した ViewModel factory を該当スロットへ入れる。</summary>
    /// <param name="viewModelType">登録キーになる ViewModel の型。</param>
    /// <param name="factory">型消去した ViewModel factory。</param>
    internal void StoreViewModelFactory(Type viewModelType, DialogViewModelFactory factory) =>
        UpdateEntry(viewModelType, entry => entry with { ViewModelFactory = factory });

    /// <summary>一括解決の関数の組を設定する。<see langword="null"/> で解除する。</summary>
    /// <param name="fallbacks">一括解決の関数の組。</param>
    internal void UseFallbacks(DialogFallbackResolvers? fallbacks)
    {
        lock (_gate)
        {
            _fallbacks = fallbacks;
        }
    }

    /// <summary>
    /// 一括解決の関数の組を、設定されているスロットだけ差し替える形で合成する。
    /// </summary>
    /// <remarks>
    /// 明示登録 (<see cref="StoreViewFactory"/> / <see cref="StoreViewModelFactory"/>) と同じく
    /// スロット単位の後勝ちで、渡された組で未設定 (<see langword="null"/>) のスロットは
    /// 既に設定されている関数を保持する。設定を重ねても既存の一括解決が消えない
    /// (maui/ADR-0005 の「null 上書き・後勝ちの粗を構造ごと消す」)。
    /// </remarks>
    /// <param name="fallbacks">重ねる一括解決の関数の組。</param>
    internal void MergeFallbacks(DialogFallbackResolvers fallbacks)
    {
        ArgumentNullException.ThrowIfNull(fallbacks);

        lock (_gate)
        {
            _fallbacks = _fallbacks is null
                ? fallbacks
                : _fallbacks with
                {
                    View = fallbacks.View ?? _fallbacks.View,
                    ViewModel = fallbacks.ViewModel ?? _fallbacks.ViewModel,
                };
        }
    }

    /// <summary>設定されている一括解決の関数の組。未設定なら <see langword="null"/>。</summary>
    internal DialogFallbackResolvers? Fallbacks
    {
        get
        {
            lock (_gate)
            {
                return _fallbacks;
            }
        }
    }

    /// <summary>
    /// キーに対応するエントリを返す。どちらのスロットも未登録なら <see langword="null"/>。
    /// </summary>
    /// <remarks>返すのは呼び出し時点のスナップショットで、以後の再登録には影響されない。</remarks>
    /// <param name="viewModelType">解決したい ViewModel の型。</param>
    /// <returns>登録済みのエントリ。未登録なら <see langword="null"/>。</returns>
    internal DialogRegistryEntry? Entry(Type viewModelType)
    {
        lock (_gate)
        {
            return _entries.TryGetValue(viewModelType, out DialogRegistryEntry? entry) ? entry : null;
        }
    }

    /// <summary>キーに対応する View factory を返す。未登録なら <see langword="null"/>。</summary>
    /// <param name="viewModelType">解決したい ViewModel の型。</param>
    /// <returns>登録済みの View factory。未登録なら <see langword="null"/>。</returns>
    internal DialogViewFactory? Factory(Type viewModelType) => Entry(viewModelType)?.ViewFactory;

    /// <summary>エントリの片方のスロットだけを置き換える。未登録の型なら空のエントリから作る。</summary>
    private void UpdateEntry(Type viewModelType, Func<DialogRegistryEntry, DialogRegistryEntry> update)
    {
        lock (_gate)
        {
            _entries[viewModelType] =
                update(_entries.TryGetValue(viewModelType, out DialogRegistryEntry? entry) ? entry : new());
        }
    }
}
