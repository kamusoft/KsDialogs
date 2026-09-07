# Report a result from a view model

A Dialog result is reported by the view model being shown, through the extension property `Notifier`. This document covers how to read `Notifier` and how to use the `ShowAsync` that takes only the view-model type.

## Read `Notifier`

`Notifier` holds a value only while the view model is being shown. Write it as `this.Notifier` inside the view model itself and call it through `?.`.

| Item | Detail |
|---|---|
| Type | `DialogNotifier<TResult>` for the result type the view model declared |
| Before and after the show | `null` (it returns a value only while shown) |
| Show routes it binds on | instance-passing, inline factory, and type-based alike |
| Relation to a two-argument factory | the notifier read from the view model and the factory argument target the same delivery |

The following view model has a boolean result, where `Accept` reports completion and `Cancel` reports cancellation.

```csharp
using KsDialogs;
using Microsoft.Maui.Controls;

namespace MyApp;

public sealed class ConfirmViewModel : IDialogViewModel
{
    public string Message { get; set; } = string.Empty;

    public void Accept() => this.Notifier?.Complete(true);

    public void Cancel() => this.Notifier?.Cancel();
}
```

## Make the view model a class

Notifier binding is looked up by instance identity, so a view model has to be a reference type.

- Registration and type-based show carry a `where TViewModel : class` constraint, so a value-type view model does not compile there
- Only instance-passing show, which takes the interface, checks at runtime and rejects it with `DialogException.ValueTypeViewModel`

## Do not stack the same instance

One instance can hold only one notifier binding. Showing the same instance concurrently fails with `DialogException.ViewModelAlreadyShowing`. A different instance of the same type stacks independently.

## Create and configure a view model by type

Type-based `ShowAsync` takes only the view-model type, runs `configure` on the instance built by the registered view-model factory, and then shows it.

### Registrations it needs

A registry entry holds two independent slots.

| Slot | API that fills it | Show that uses it |
|---|---|---|
| View factory | `Register` | both instance-passing and type-based |
| View-model factory | `RegisterViewModel` | type-based only |

Type-based `ShowAsync` needs both. Re-registering replaces only the slot you touched and leaves the other one. Resolution is a snapshot taken when show is called, so registering again while a Dialog is on screen does not affect it. If neither an explicit view-model factory nor a fallback ([DI registration](di-registration.md)) can resolve the type, show fails with `DialogException.ViewModelFactoryNotRegistered` before presentation.

### The order from creation to presentation

The order is fixed.

1. Create the view model
2. Run `configure` to completion
3. Bind the notifier
4. Build the content view

Configuration therefore completes before the content factory reads state. A failure thrown by the view-model factory or by `configure` propagates to the caller instead of turning into a cancelled result.

The following example registers both slots for a view model that declares `string` as its result type, and fills `Prompt` from the `configure` of a type-based `ShowAsync` before showing it.

```csharp
using KsDialogs;
using Microsoft.Maui.Controls;

namespace MyApp;

public sealed class ProfileViewModel : IDialogViewModel<string>
{
    public string Prompt { get; set; } = string.Empty;
}

public static class ProfileDialogs
{
    public static void Register()
    {
        Dialog.Instance.Registry.Register<ProfileViewModel, string>(viewModel =>
            new Button
            {
                Text = viewModel.Prompt,
                Command = new Command(() => viewModel.Notifier?.Complete("accepted")),
            });
        Dialog.Instance.Registry.RegisterViewModel<ProfileViewModel, string>(() => new ProfileViewModel());
    }

    public static Task<DialogResult<string>> ShowAsync() =>
        Dialog.Instance.ShowAsync<ProfileViewModel, string>(viewModel =>
        {
            viewModel.Prompt = "Continue?";
        });
}
```

## Configure asynchronously before presentation

`configure` comes in two forms: a synchronous `Action<TViewModel>` and an asynchronous `Func<TViewModel, Task>`. Use the asynchronous overload when initial state must be loaded before the content factory runs. Register both factories once before calling `ShowAsync`; the content factory then observes the state after configuration finishes.

The following example has a view model with a boolean result, where an asynchronous `configure` fills `Name` before the content view is built.

```csharp
using KsDialogs;
using Microsoft.Maui.Controls;

namespace MyApp;

public sealed class AccountViewModel : IDialogViewModel
{
    public string Name { get; set; } = string.Empty;
}

public static class AccountDialogs
{
    public static void Register()
    {
        Dialog.Instance.Registry.Register<AccountViewModel>(viewModel =>
            new Button
            {
                Text = viewModel.Name,
                Command = new Command(() => viewModel.Notifier?.Complete(true)),
            });
        Dialog.Instance.Registry.RegisterViewModel<AccountViewModel>(() => new AccountViewModel());
    }

    public static Task<DialogResult<bool>> ShowAsync() =>
        Dialog.Instance.ShowAsync<AccountViewModel>(async viewModel =>
        {
            await Task.Yield();
            viewModel.Name = "Ada";
        });
}
```

## The same shape works for Loading and Toast

Showing by type is not Dialog-only: Loading and Toast offer the same shape. What they share is resolution through a view-model factory, resolution taken as a snapshot when show is called, the order of create, run `configure` to completion, build the content view, present, and a missing view-model factory surfacing as `DialogException.ViewModelFactoryNotRegistered`. Loading also has `StartAsync<TViewModel>`, the type-based form of the scope that takes an action.

These points differ per feature.

| Feature | `configure` | Binding inserted before the content view is built | Failure of the view-model factory or `configure` |
|---|---|---|---|
| Dialog | synchronous and asynchronous | notifier | Propagates to the caller without reaching presentation |
| Loading | synchronous and asynchronous | progress receiver. Coalescing is decided after `configure` completes, and the view model of a call that joins an existing display is not used for presentation | Propagates without reaching presentation or coalescing, and the action of `StartAsync` does not run |
| Toast | synchronous only | none | `Show` has already returned, so nothing propagates: a warning is recorded and that single Toast is discarded |

Fallback resolution applies to the Dialog registry alone; Loading and Toast look only at an explicit registration or the one-line registration ([DI registration](di-registration.md)). How to write each is in [Loading](loading.md) and [Toast](toast.md).

From a caller's side this is an addition: instance-passing show, inline factories, and two-argument factory registrations keep working. A type that implements `IKsDialog`, `IKsLoading`, or `IKsToast` itself, however — a test stand-in or an adapter — has to implement the members added to the contract before it recompiles.
