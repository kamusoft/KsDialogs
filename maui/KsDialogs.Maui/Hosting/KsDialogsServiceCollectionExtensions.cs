using System;
using System.Linq;
using System.Reflection;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.DependencyInjection.Extensions;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Hosting;

namespace KsDialogs;

/// <summary>
/// DI チェーン上でダイアログの登録と一括解決を設定する面 (maui/ADR-0005・core/ADR-0021)。
/// </summary>
public static class KsDialogsServiceCollectionExtensions
{
    /// <summary>
    /// ライブラリをアプリに組み込み、一括解決 (fallback resolver) を設定する。
    /// </summary>
    /// <remarks>
    /// 設定した規約は共有レジストリ (<see cref="DialogViewRegistry.Shared"/>) に載るため、
    /// 既定 singleton エントリと DI 注入のどちらの入口から show しても同じ順序で解決される
    /// (core/ADR-0002)。
    /// <para>
    /// 複数回呼んでも設定済みの一括解決は消えない。View / ViewModel のスロットごとに、
    /// その呼び出しで設定した側だけが後勝ちで置き換わり、設定しなかった側は保持される
    /// (maui/ADR-0005)。したがって引数なしの呼び出しは既存の設定を <see langword="null"/> で
    /// 上書きしない。設定済みの一括解決を公開 API から解除する手段は提供しない
    /// (置き換えたいスロットに新しい関数を設定し直す)。
    /// </para>
    /// </remarks>
    /// <param name="services">アプリのサービス集合。</param>
    /// <param name="configure">一括解決の設定。省略すると明示登録だけで解決する。</param>
    /// <returns>チェーンできるように受け取ったサービス集合。</returns>
    public static IServiceCollection AddKsDialogs(
        this IServiceCollection services,
        Action<KsDialogsOptions>? configure = null)
    {
        ArgumentNullException.ThrowIfNull(services);

        KsDialogsOptions options = new();
        configure?.Invoke(options);
        DialogViewRegistry.Shared.MergeFallbacks(
            new DialogFallbackResolvers(options.ViewFallback, options.ViewModelFallback));

        AddServiceProviderCapture(services);
        return services;
    }

    /// <summary>
    /// 真偽値の顔で宣言した ViewModel 型と、その中身になる View を 1 行で結び付ける
    /// (core/ADR-0012・0021)。
    /// </summary>
    /// <remarks>
    /// View factory と ViewModel factory の両方を配線するため、この 1 行だけで
    /// インスタンス渡し show と型指定 show の双方が使えるようになる。
    /// <typeparamref name="TView"/> / <typeparamref name="TViewModel"/> は transient として
    /// サービスに自動登録するが、同じ型が既に登録されていれば追加しない。
    /// この「既存の登録を尊重する」効果が表示に効くのは <typeparamref name="TViewModel"/> 側である
    /// (ViewModel factory はサービスから引くため)。
    /// <typeparamref name="TView"/> の表示用インスタンスは、サービス登録を経由せず
    /// <see cref="ActivatorUtilities"/> で表示対象の ViewModel を明示引数として生成するため、
    /// 利用者が <typeparamref name="TView"/> に別の登録 (差し替えた実装や特定のライフタイム) を
    /// していてもその登録は使われない。コンストラクタが <typeparamref name="TViewModel"/> を受ける
    /// View ではその値がそのまま注入され、BindingContext と同じインスタンスになる。
    /// </remarks>
    /// <typeparam name="TView">中身になる MAUI View の型。</typeparam>
    /// <typeparam name="TViewModel">結び付ける ViewModel の型。</typeparam>
    /// <param name="services">アプリのサービス集合。</param>
    /// <returns>チェーンできるように受け取ったサービス集合。</returns>
    public static IServiceCollection RegisterForDialog<TView, TViewModel>(this IServiceCollection services)
        where TView : View
        where TViewModel : class, IDialogViewModel =>
        RegisterPair<TView, TViewModel>(services);

    /// <summary>
    /// 結果型を宣言した ViewModel 型と、その中身になる View を 1 行で結び付ける (core/ADR-0021)。
    /// </summary>
    /// <remarks>
    /// 結果型を型引数に書く点以外は 2 型引数の
    /// <see cref="RegisterForDialog{TView, TViewModel}(IServiceCollection)"/> と同じ。
    /// </remarks>
    /// <typeparam name="TView">中身になる MAUI View の型。</typeparam>
    /// <typeparam name="TViewModel">結び付ける ViewModel の型。</typeparam>
    /// <typeparam name="TResult">ViewModel が宣言する結果値の型。</typeparam>
    /// <param name="services">アプリのサービス集合。</param>
    /// <returns>チェーンできるように受け取ったサービス集合。</returns>
    public static IServiceCollection RegisterForDialog<TView, TViewModel, TResult>(
        this IServiceCollection services)
        where TView : View
        where TViewModel : class, IDialogViewModel<TResult> =>
        RegisterPair<TView, TViewModel>(services);

    /// <summary>
    /// カスタム Loading の ViewModel 型と、その中身になる View を 1 行で結び付ける
    /// (maui/ADR-0005 の Loading 版)。
    /// </summary>
    /// <remarks>
    /// 紐付けは Loading 専用のレジストリ (<see cref="LoadingViewRegistry.Shared"/>) に載るため、
    /// 同じ ViewModel 型を <c>RegisterForDialog</c> にも登録でき、互いに影響しない。
    /// View factory と ViewModel factory の両方を配線するため、この 1 行だけで
    /// インスタンス渡しの表示と ViewModel の型を渡す表示の双方が使えるようになる
    /// (ViewModel factory はサービスから引く)。
    /// <typeparamref name="TView"/> の生成規則 (<see cref="ActivatorUtilities"/> による直接生成・
    /// コンストラクタへの ViewModel の注入・BindingContext の設定) は
    /// <see cref="RegisterForDialog{TView, TViewModel}(IServiceCollection)"/> と同じ。
    /// </remarks>
    /// <typeparam name="TView">中身になる MAUI View の型。</typeparam>
    /// <typeparam name="TViewModel">結び付ける ViewModel の型。</typeparam>
    /// <param name="services">アプリのサービス集合。</param>
    /// <returns>チェーンできるように受け取ったサービス集合。</returns>
    public static IServiceCollection RegisterForLoading<TView, TViewModel>(this IServiceCollection services)
        where TView : View
        where TViewModel : class, ILoadingViewModel
    {
        ArgumentNullException.ThrowIfNull(services);

        AddServiceProviderCapture(services);
        services.TryAddTransient<TView>();
        services.TryAddTransient<TViewModel>();

        LoadingViewRegistry.Shared.StoreViewFactory(
            typeof(TViewModel),
            viewModel => CreateView(typeof(TView), typeof(TViewModel), viewModel));
        LoadingViewRegistry.Shared.StoreViewModelFactory(
            typeof(TViewModel),
            () => DialogServiceProvider.Require().GetRequiredService<TViewModel>());

        return services;
    }

    /// <summary>
    /// カスタム Toast の ViewModel 型と、その中身になる View を 1 行で結び付ける
    /// (maui/ADR-0005 の Toast 版)。
    /// </summary>
    /// <remarks>
    /// 紐付けは Toast 専用のレジストリ (<see cref="ToastViewRegistry.Shared"/>) に載るため、
    /// 同じ ViewModel 型を <c>RegisterForDialog</c> / <c>RegisterForLoading</c> にも登録でき、
    /// 互いに影響しない。
    /// View factory と ViewModel factory の両方を配線するため、この 1 行だけで
    /// インスタンス渡しの表示と ViewModel の型を渡す表示の双方が使えるようになる
    /// (ViewModel factory はサービスから引く)。
    /// <typeparamref name="TView"/> の生成規則 (<see cref="ActivatorUtilities"/> による直接生成・
    /// コンストラクタへの ViewModel の注入・BindingContext の設定) は
    /// <see cref="RegisterForDialog{TView, TViewModel}(IServiceCollection)"/> と同じ。
    /// </remarks>
    /// <typeparam name="TView">中身になる MAUI View の型。</typeparam>
    /// <typeparam name="TViewModel">結び付ける ViewModel の型。</typeparam>
    /// <param name="services">アプリのサービス集合。</param>
    /// <returns>チェーンできるように受け取ったサービス集合。</returns>
    public static IServiceCollection RegisterForToast<TView, TViewModel>(this IServiceCollection services)
        where TView : View
        where TViewModel : class, IToastViewModel
    {
        ArgumentNullException.ThrowIfNull(services);

        AddServiceProviderCapture(services);
        services.TryAddTransient<TView>();
        services.TryAddTransient<TViewModel>();

        ToastViewRegistry.Shared.StoreViewFactory(
            typeof(TViewModel),
            viewModel => CreateView(typeof(TView), typeof(TViewModel), viewModel));
        ToastViewRegistry.Shared.StoreViewModelFactory(
            typeof(TViewModel),
            () => DialogServiceProvider.Require().GetRequiredService<TViewModel>());

        return services;
    }

    /// <summary>View / ViewModel の 2 スロットを自動配線し、サービス登録も済ませる。</summary>
    /// <typeparam name="TView">中身になる MAUI View の型。</typeparam>
    /// <typeparam name="TViewModel">結び付ける ViewModel の型。</typeparam>
    /// <param name="services">アプリのサービス集合。</param>
    /// <returns>チェーンできるように受け取ったサービス集合。</returns>
    private static IServiceCollection RegisterPair<TView, TViewModel>(IServiceCollection services)
        where TView : View
        where TViewModel : class
    {
        ArgumentNullException.ThrowIfNull(services);

        AddServiceProviderCapture(services);
        services.TryAddTransient<TView>();
        services.TryAddTransient<TViewModel>();

        DialogViewRegistry.Shared.StoreViewFactory(
            typeof(TViewModel),
            (viewModel, _) => CreateView(typeof(TView), typeof(TViewModel), viewModel));
        DialogViewRegistry.Shared.StoreViewModelFactory(
            typeof(TViewModel),
            () => DialogServiceProvider.Require().GetRequiredService<TViewModel>());

        return services;
    }

    /// <summary>初期化サービスを冪等に登録する。</summary>
    /// <param name="services">アプリのサービス集合。</param>
    private static void AddServiceProviderCapture(IServiceCollection services) =>
        services.TryAddEnumerable(
            ServiceDescriptor.Singleton<IMauiInitializeService, KsDialogsInitializer>());

    /// <summary>表示対象の ViewModel を明示的に渡して中身の View を作る。</summary>
    /// <remarks>
    /// View は <see cref="ActivatorUtilities"/> で直接生成するため、その型のサービス登録は使わない
    /// (コンストラクタの他の引数だけを provider から解決する)。
    /// コンストラクタが ViewModel を受け取る形なら、show 対象のインスタンスが明示引数として
    /// そのまま注入される (ViewModel が新たに作られることはない)。受け取らない形なら
    /// 他の引数だけを解決して作り、どちらの場合も BindingContext に表示対象の ViewModel を設定する。
    /// </remarks>
    /// <param name="viewType">生成する View の型。</param>
    /// <param name="viewModelType">その View に結び付いた ViewModel の型。</param>
    /// <param name="viewModel">表示対象の ViewModel。</param>
    /// <returns>BindingContext を設定した中身の View。</returns>
    private static View CreateView(Type viewType, Type viewModelType, object viewModel)
    {
        IServiceProvider provider = DialogServiceProvider.Require();
        View view;
        try
        {
            view = (View)(TakesViewModel(viewType, viewModel)
                ? ActivatorUtilities.CreateInstance(provider, viewType, viewModel)
                : ActivatorUtilities.CreateInstance(provider, viewType));
        }
        catch (DialogException)
        {
            // 構成ミスとして既に表されている失敗は、包み直さずそのまま先へ通す
            throw;
        }
        catch (Exception thrown)
        {
            throw new DialogException.ViewCreationFailed(
                viewType.FullName ?? viewType.Name,
                viewModelType.FullName ?? viewModelType.Name,
                thrown);
        }

        view.BindingContext = viewModel;
        return view;
    }

    /// <summary>その View の型に、ViewModel を受け取るコンストラクタがあるか。</summary>
    /// <param name="viewType">調べる View の型。</param>
    /// <param name="viewModel">表示対象の ViewModel。</param>
    /// <returns>受け取るコンストラクタがあれば <see langword="true"/>。</returns>
    private static bool TakesViewModel(Type viewType, object viewModel) =>
        viewType.GetConstructors(BindingFlags.Public | BindingFlags.Instance)
            .Any(constructor => constructor.GetParameters()
                .Any(parameter => parameter.ParameterType.IsInstanceOfType(viewModel)));
}
