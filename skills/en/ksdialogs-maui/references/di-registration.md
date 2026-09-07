# Register a Dialog view and view model in one line

## Register a Dialog

Chain `RegisterForDialog` from `MauiProgram`. One call does two things.

- Wires both registry slots for that view-model type — the view factory and the view-model factory
- Adds `TView` and `TViewModel` to the service collection as transient

A registration you made yourself is left as it is.

The displayed view is built through its constructor with the current view model passed as an explicit argument, and that same instance becomes its `BindingContext`, so the service registration for `TView` only resolves its other constructor dependencies.

Use `RegisterForDialog<TView, TViewModel, TResult>` when the view model declares a custom result type instead of `bool`.

## Register custom Loading and Toast content

For custom Loading or Toast content, use the sibling extensions.

- `RegisterForLoading<TView, TViewModel>`
- `RegisterForToast<TView, TViewModel>`

Each performs the same wiring as `RegisterForDialog` against its own feature-specific registry: it fills both slots — the view factory and a view-model factory that pulls the view model from services — and adds `TView` and `TViewModel` as transient. That single line covers both the show that takes an instance and the show that takes only the view-model type ([Loading](loading.md) / [Toast](toast.md)). The three registries are independent, so the same view-model type can be registered with a different view for Dialog, Loading, and Toast.

Two things differ from the Dialog version.

- There is no three-type-argument form for a result type, because Loading and Toast return no result
- Fallback resolution does not apply. The fallback is a Dialog-registry mechanism, and Loading and Toast look only at an explicit registration or the one-line registration; an unregistered type fails outright instead of reaching a fallback

## Entries taken through DI, and `AddKsDialogs`

Register `Dialog.Instance` as `IKsDialog`, `Loading.Instance` as `IKsLoading`, and `Toast.Instance` as `IKsToast` as singletons, and let consuming classes take those contracts in their constructors. Singleton is the recommendation because each is the one instance that shares its registry and settings with the default entry such as `Dialog.Instance`.

`AddKsDialogs` does two things: it configures fallback resolution, and it registers the initialization service that captures the service provider at startup. It does not register `IKsDialog` and its siblings, so you write those yourself as above. `RegisterForDialog` registers the same initialization service, and repeated calls never register it twice. An application that uses no fallback therefore does not need a separate `AddKsDialogs` call.

## A registration example in `MauiProgram`

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

## Enable view-model fallback resolution

Call `AddKsDialogs` when type-based `ShowAsync` should resolve an unregistered view model from MAUI services. There are two resolvers.

- `UseViewModelFallback()` — uses the built-in resolver that asks the service provider
- `UseViewModelFallback(Func<Type, IServiceProvider, object?>)` — takes your own resolver

An explicit view-model factory takes precedence over the fallback. The fallback applies to the Dialog registry alone; type-based Loading and Toast presentation never consults it. If a DI-backed route is used before MAUI startup has captured the application service provider, it fails with `DialogException.ServiceProviderUnavailable` — the same goes for a view-model factory wired by `RegisterForLoading` or `RegisterForToast`.

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

## Resolve unregistered views by convention

`UseViewFallback(Func<Type, IServiceProvider, View?>)` is a separate slot from the view-model fallback and receives the view-model type plus the provider. Return `null` when the convention cannot resolve a type; the show then fails with `DialogException.ViewFactoryNotRegistered`. The library sets `BindingContext` to the current view model on a view returned by the fallback as well.

The view slot and the view-model slot are decided independently, and for each the order is explicit registration, then fallback, then failure.

Re-calling `AddKsDialogs` merges only the slots you set this time. An earlier fallback is not silently cleared by a later bare `AddKsDialogs()`; setting the same slot twice keeps the later one.

The settings live on the process-wide `DialogViewRegistry.Shared` and no public API removes them, which matters when one test process builds several hosts.

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
