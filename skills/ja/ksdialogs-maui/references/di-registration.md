# Dialog の View と ViewModel を 1 行で登録する

## Dialog を登録する

`MauiProgram` で `RegisterForDialog` を chain する。1 回の呼び出しで次の 2 つを行う。

- その ViewModel 型のレジストリ 2 slot (View factory と ViewModel factory) を両方配線する
- `TView` と `TViewModel` を transient として service collection に追加する

利用者が先に登録した型はそのまま尊重される。

表示する View は、現在の ViewModel を明示引数として渡す constructor 経由で生成され、その同じ instance が `BindingContext` になる。したがって `TView` の service 登録が意味を持つのは、その constructor の他の依存を解決するところまでである。

その View をライブラリが組み立てられなかった場合 (constructor の依存が service にない、など) は `DialogException.ViewCreationFailed` で show が失敗する。元の失敗は `InnerException` に残り、組み立てようとした View と ViewModel の型名は `ViewTypeName` / `ViewModelTypeName` から読める。包まれるのは 1 行登録でライブラリ自身が組み立てる View だけで、自分で書いた factory や fallback resolver が投げた失敗はそのまま届く。Toast だけは `Show` が既に戻っているため報告の形が違い、警告に `ViewCreationFailed` が原因として残ってその 1 枚が破棄される ([Toast](toast.md))。

ViewModel が `bool` 以外の custom 結果型を宣言するときは `RegisterForDialog<TView, TViewModel, TResult>` を使う。

## Loading・Toast の custom content を登録する

custom Loading または Toast content には、姉妹 extension を使う。

- `RegisterForLoading<TView, TViewModel>`
- `RegisterForToast<TView, TViewModel>`

どちらも `RegisterForDialog` と同じ配線を、それぞれの機能専用レジストリに対して行う。View factory と、service から ViewModel を引く ViewModel factory の 2 slot を配線し、`TView` と `TViewModel` を transient として追加する。この 1 行だけで、instance を渡す表示と ViewModel の型だけを渡す表示 ([Loading](loading.md) / [Toast](toast.md)) の両方が使える。3 つのレジストリは独立しているので、同じ ViewModel 型を Dialog・Loading・Toast へ別々の View で登録できる。

Dialog 版との違いは 2 点である。

- 結果型を型引数に取る 3 型引数版はない (Loading と Toast は結果を返さない)
- fallback 解決が効かない。fallback は Dialog レジストリだけの機構で、Loading と Toast は明示登録か 1 行登録のみを見る (未登録の型は fallback へ回らずそのまま失敗する)

## DI で受け取る入口と `AddKsDialogs`

`Dialog.Instance` を `IKsDialog`、`Loading.Instance` を `IKsLoading`、`Toast.Instance` を `IKsToast` として singleton で登録し、使う側のクラスは constructor でこれらを受け取る。singleton を勧めるのは、これらが `Dialog.Instance` などの既定の入口と同じレジストリ・同じ設定を共有する 1 つの instance だからである。

`AddKsDialogs` がするのは 2 つで、fallback 解決の設定と、起動時に service provider を捕まえる初期化サービスの登録である。`IKsDialog` などの登録はしないので、それは上のように利用者が書く。初期化サービスの登録は `RegisterForDialog` も同じように行い、何度呼んでも二重には登録されない。したがって fallback を使わないアプリは `AddKsDialogs` を別途呼ばなくてよい。

## `MauiProgram` での登録例

```csharp
using KsDialogs;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Hosting;

namespace MyApp;

public sealed class ConfirmViewModel : IDialogViewModel<string>
{
}

public sealed class ConfirmView : ContentView
{
    public ConfirmView(ConfirmViewModel viewModel)
    {
        Content = new Button
        {
            Text = "OK",
            Command = new Command(() => viewModel.Notifier?.Complete("confirmed")),
        };
    }
}

public sealed class DialogConsumer(
    IKsDialog dialogs,
    IKsLoading loading,
    IKsToast toast)
{
    public Task<DialogResult<string>> ConfirmAsync() =>
        dialogs.ShowAsync(new ConfirmViewModel());

    public Task RefreshAsync() =>
        loading.StartAsync(async _ => await Task.Yield());

    public void NotifySaved() => toast.Show("Saved");
}

public static class MauiProgram
{
    public static MauiApp CreateMauiApp()
    {
        var builder = MauiApp.CreateBuilder();
        builder.UseMauiApp<App>();
        builder.Services
            .AddSingleton<IKsDialog>(_ => Dialog.Instance)
            .AddSingleton<IKsLoading>(_ => Loading.Instance)
            .AddSingleton<IKsToast>(_ => Toast.Instance)
            .AddTransient<DialogConsumer>()
            .RegisterForDialog<ConfirmView, ConfirmViewModel, string>();
        return builder.Build();
    }
}
```

## ViewModel の fallback 解決を有効にする

型指定 `ShowAsync` で未登録の ViewModel を MAUI service から解決する場合は `AddKsDialogs` を呼ぶ。resolver は 2 通りある。

- `UseViewModelFallback()` — service provider へ問い合わせる組み込み resolver を使う
- `UseViewModelFallback(Func<Type, IServiceProvider, object?>)` — 自作の resolver を渡す

明示的な ViewModel factory が fallback より優先される。fallback が効くのは Dialog レジストリだけで、Loading と Toast の型指定表示は fallback を見ない。MAUI startup が application service provider を捕捉する前に DI 経路を使うと、`DialogException.ServiceProviderUnavailable` で失敗する。これは `RegisterForLoading` / `RegisterForToast` が配線した ViewModel factory も同じである。

```csharp
using KsDialogs;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Maui.Hosting;

namespace MyApp;

public static class DialogServices
{
    public static IServiceCollection AddDialogServices(this IServiceCollection services) =>
        services
            .AddTransient<ProfileViewModel>()
            .AddKsDialogs(options => options.UseViewModelFallback());
}

public sealed class ProfileViewModel : IDialogViewModel
{
}
```

## 規約で未登録の View を解決する

`UseViewFallback(Func<Type, IServiceProvider, View?>)` は ViewModel の fallback とは別 slot で、ViewModel の型と provider を受け取る。規約で解決できない型には `null` を返す。その場合 show は `DialogException.ViewFactoryNotRegistered` で失敗する。fallback が返した View にも、ライブラリが現在の ViewModel を `BindingContext` として設定する。

View slot と ViewModel slot は独立に判定され、どちらも「明示登録 → fallback → 失敗」の順で解決される。

`AddKsDialogs` の再呼び出しはその回に設定した slot だけを合成する。後から引数なしの `AddKsDialogs()` を呼んでも先の fallback は黙って消えない (同じ slot を 2 回設定した場合は後が勝つ)。AiForms.Maui.Dialogs の `SetIocConfig` のような static な一括設定口はなく、後の呼び出しが null で先の設定を潰すこともない。解決関数はこの options の slot に載せる。

設定はプロセス内で共有される `DialogViewRegistry.Shared` に載り、公開 API から解除する手段はない。1 つのテストプロセスで複数の host を組み立てるときは注意する。

```csharp
using KsDialogs;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Maui.Controls;

namespace MyApp;

public static class DialogFallbacks
{
    public static IServiceCollection AddDialogFallbacks(this IServiceCollection services) =>
        services.AddKsDialogs(options =>
        {
            options.UseViewFallback((viewModelType, provider) =>
            {
                var viewType = viewModelType.Assembly.GetType($"{viewModelType.Namespace}.{viewModelType.Name}View");
                return viewType is null ? null : (View)ActivatorUtilities.CreateInstance(provider, viewType);
            });
            options.UseViewModelFallback();
        });
}
```
