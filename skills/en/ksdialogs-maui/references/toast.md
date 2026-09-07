# Show a message Toast

`Toast.Instance` and an `IKsToast` taken through DI point at the same object. It has one verb, `Show`, which is fire-and-forget and returns `void` synchronously. There is no hide call, no message update, no scoped form, and no progress channel.

Toast carries neither an overlay color nor an outside-tap option, and Loading stays in front of Toast regardless of acceptance order. Toast content is non-interactive, and touches pass through to the page behind it.

## How the duration is decided

`durationMs` is an `int?` in milliseconds and starts when the call is accepted, continuing while the app is in the background. A missing or non-positive value uses a positive `ToastStyle.DefaultDuration`; if that default is also non-positive, `ToastStyle.BuiltinDefaultDuration` supplies the built-in 1500 ms fallback. No upper clamp is applied.

## How overlapping behaves

Each Toast coexists on an independent timer and they may overlap at the same placement. The library does not queue, replace, or automatically offset them. A Toast for which no container appears before expiry is discarded without ever being shown.

## Choose a `Show`

`IKsToast` (both `Toast.Instance` and what DI hands over are the same object) exposes `Show` in the following overloads. The axes to choose on are: put a message on the built-in message Toast or show custom content, leave the custom content factory to a registration or pass it on the spot, and assemble the custom content's view model yourself or pass only its type and let the library build it.

| Signature | What it does | When to choose it | Registration needed |
|---|---|---|---|
| `void Show(string message, int? durationMs = null, DialogPlacement? placement = null)` | Puts a message on the built-in message Toast and shows it. An empty string stays as it is, and a long one wraps onto several lines | When the wording alone is enough | None |
| `void Show(IToastViewModel viewModel, int? durationMs = null, DialogPlacement? placement = null)` | Shows custom content built by a registered view factory | When a look of your own, with an icon for instance, is reused | A view factory ([DI registration](di-registration.md)) |
| `void Show<TViewModel>(TViewModel viewModel, Func<TViewModel, View> factory, int? durationMs = null, DialogPlacement? placement = null)` | Shows content whose factory is passed on the spot. It does not touch the registry | Content used once | None |
| `void Show<TViewModel>(Action<TViewModel>? configure = null, int? durationMs = null, DialogPlacement? placement = null)` | Runs `configure` on the view model built by the registered view-model factory, then shows the registered custom content | When assembling the view model is left to DI | A view factory and a view-model factory ([DI registration](di-registration.md)) |

`durationMs` and `placement` mean the same thing in every overload. In the examples below, "Show a message" is the first row, "Register custom Toast content" the second, "Show custom content without registration" the third, and "Show a Toast by view-model type" the fourth.

## Show a message

The caller can be a Page or a view model, and can use `Toast.Instance` or what it took as an `IKsToast`. `Show` does not wait for a return value, so do not `await` it.

```csharp
using System;
using KsDialogs;
using Microsoft.Maui.Controls;

namespace MyApp;

public partial class ItemPage : ContentPage
{
    private readonly IKsToast _toast;

    public ItemPage(IKsToast toast)
    {
        InitializeComponent();
        _toast = toast;
    }

    private void OnSaveClicked(object? sender, EventArgs e) =>
        _toast.Show("Saved", durationMs: 2500);
}
```

## Configure Toast style and placement

Set `Toast.Instance.Style` before showing. The `ToastStyle` record is read at display start, so a change takes effect on the next Toast.

| `ToastStyle` property | Type | Default |
|---|---|---|
| `BackgroundColor` | `Color` | `ToastStyle.BuiltinBackgroundColor` |
| `TextColor` | `Color` | `Colors.White` |
| `FontSize` | `double` | `14` |
| `CornerRadius` | `double` | `22` |
| `DefaultDuration` | `int` | `ToastStyle.BuiltinDefaultDuration` |
| `DefaultPlacement` | `DialogPlacement?` | `null` |

The visual properties apply to the built-in message Toast only, while `DefaultDuration` and `DefaultPlacement` apply to custom content as well. A placement passed to `Show` takes precedence over the style default. With neither supplied, the library default placement is bottom-center of the visible area with an upward offset of 80 logical units.

```csharp
using KsDialogs;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Maui.Graphics;
using Microsoft.Maui.Hosting;

namespace MyApp;

public static class MauiProgram
{
    public static MauiApp CreateMauiApp()
    {
        var builder = MauiApp.CreateBuilder();
        builder.UseMauiApp<App>();
        builder.Services
            .AddSingleton<IKsToast>(_ => Toast.Instance)
            .AddTransient<ItemPage>();

        Toast.Instance.Style = new ToastStyle
        {
            BackgroundColor = Colors.Black.WithAlpha(0.92f),
            TextColor = Colors.White,
            FontSize = 16,
            CornerRadius = 20,
            DefaultDuration = 2000,
            DefaultPlacement = new DialogPlacement
            {
                VerticalAlignment = DialogAlignment.End,
                OffsetY = -80,
            },
        };

        return builder.Build();
    }
}
```

## Register custom Toast content

`Toast.Instance.Registry` is a `ToastViewRegistry` and is `ToastViewRegistry.Shared`. It is independent of the Dialog and Loading registries, so the same view-model type can be registered in several of them. Register a class-based `IToastViewModel` there with `Register`, or wire the pair from DI with `RegisterForToast<TView, TViewModel>` ([DI registration](di-registration.md)). One entry holds two slots — a view factory and a view-model factory — and `Register` fills the view factory slot. Showing by instance needs that slot alone; showing by view-model type needs both, and `RegisterForToast` fills both in one line.

Toast has neither an overlay nor a built-in ground, so custom content paints its own background. Placement is attached with the same `Dialog` attached properties as for a Dialog, and only the transition, which carries closures, is attached from code-behind ([Layout](layout.md)).

### Declare the view model

A Toast has neither a result nor progress, so the view model only carries data and acts as the registry's type key.

```csharp
using KsDialogs;

namespace MyApp;

public sealed class SavedToastViewModel : IToastViewModel
{
    public string Message { get; set; } = string.Empty;
}
```

### Write the view in XAML

```xml
<?xml version="1.0" encoding="utf-8" ?>
<ContentView xmlns="http://schemas.microsoft.com/dotnet/2021/maui"
             xmlns:x="http://schemas.microsoft.com/winfx/2009/xaml"
             xmlns:ksd="clr-namespace:KsDialogs;assembly=KsDialogs.Maui"
             xmlns:local="clr-namespace:MyApp"
             x:Class="MyApp.SavedToastView"
             x:DataType="local:SavedToastViewModel"
             ksd:Dialog.VerticalAlignment="End"
             ksd:Dialog.OffsetY="-80">

    <Border Padding="18,12" StrokeThickness="0">
        <Border.StrokeShape>
            <RoundRectangle CornerRadius="12" />
        </Border.StrokeShape>

        <Label Text="{Binding Message}" />
    </Border>

</ContentView>
```

```csharp
using KsDialogs;
using Microsoft.Maui.Controls;

namespace MyApp;

public partial class SavedToastView : ContentView
{
    public SavedToastView()
    {
        InitializeComponent();
        Dialog.SetTransition(this, DialogTransition.Fade());
    }
}
```

### Register and call

Call `RegisterForToast` once at startup. With this one-line registration, the view model being shown is set as the view's `BindingContext`.

```csharp
using KsDialogs;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Maui.Hosting;

namespace MyApp;

public static class MauiProgram
{
    public static MauiApp CreateMauiApp()
    {
        var builder = MauiApp.CreateBuilder();
        builder.UseMauiApp<App>();
        builder.Services
            .AddSingleton<IKsToast>(_ => Toast.Instance)
            .AddTransient<ItemPage>()
            .RegisterForToast<SavedToastView, SavedToastViewModel>();

        return builder.Build();
    }
}
```

```csharp
private void OnSaveClicked(object? sender, EventArgs e) =>
    _toast.Show(new SavedToastViewModel { Message = "Saved" });
```

## Show custom content without registration

For content used once, pass a view factory directly to `Show`. This route leaves the Toast registry unchanged.

```csharp
using KsDialogs;

namespace MyApp;

public sealed class NoticeToastViewModel : IToastViewModel
{
    public string Message { get; init; } = string.Empty;
}
```

```csharp
private void OnConnectedClicked(object? sender, EventArgs e) =>
    _toast.Show(
        new NoticeToastViewModel { Message = "Connected" },
        viewModel => new Label { Text = viewModel.Message },
        durationMs: 1800,
        placement: new DialogPlacement
        {
            VerticalAlignment = DialogAlignment.Start,
            OffsetY = 80,
        });
```

## Show a Toast by view-model type

Passing only the view-model type instead of an instance runs `configure` on the instance built by the registered view-model factory and then shows it. `RegisterForToast` wires that view-model factory too, so a one-line registration costs nothing extra. Written at the low level, `Toast.Instance.Registry.RegisterViewModel` fills the view-model factory slot alone.

`configure` is the synchronous `Action<TViewModel>` only; there is no asynchronous form, because `Show` is a fire-and-forget call that returns synchronously. The order is fixed — create the view model, run `configure` to completion, build the content view, present — so the content factory reads the state `configure` put in place.

```csharp
private void OnSyncedClicked(object? sender, EventArgs e) =>
    _toast.Show<SavedToastViewModel>(
        viewModel => viewModel.Message = "Synced",
        durationMs: 2000);
```

## Handle misconfiguration failures

A misconfiguration that leaves the view model — or the view-model type — passed to `Show` unresolvable on the spot is thrown synchronously at the call as a nested `DialogException` class, and that Toast is not shown. The failures in the table below are the only ones a fire-and-forget call does not swallow.

`ViewModelTypeName` reads back the type name of the view model that could not be resolved.

| Exception | Message | Cause and fix |
|---|---|---|
| `DialogException.ViewFactoryNotRegistered` | `No View factory is registered for ViewModel type {TypeName}.` | The custom Toast's view-model type has no view factory. Call `Toast.Instance.Registry.Register` or `RegisterForToast` for that type |
| `DialogException.ViewModelFactoryNotRegistered` | `No ViewModel factory is registered for ViewModel type {TypeName}.` | A `Show` that takes the view-model type has no view-model factory. Call `Toast.Instance.Registry.RegisterViewModel` or `RegisterForToast` for that type (Toast has no fallback) |
| `DialogException.ValueTypeViewModel` | `ViewModel type {TypeName} is a value type and cannot be used as a ViewModel.` | A boxed value-type view model reached the show entry. Make the view model a `class` (`struct` and `record struct` cannot be used) |

The messages in the table are the values the current implementation returns, not a stable API (what does not change is the exception type and the condition it is thrown under; the wording can change without notice).

Failures after acceptance — a factory failure, a failure to attach to the container, or a failure thrown by the view-model factory or `configure` of a `Show` that takes the view-model type — do not become exceptions. A warning is recorded, that single Toast is discarded, and nothing returns to the caller, which has already returned. Other Toasts are unaffected. Type-based presentation in Dialog and Loading propagates these failures to the caller instead, so Toast is the one that differs.

For example, showing `SavedToastViewModel` without calling `RegisterForToast` or `Registry.Register` at startup leaves `SavedToastView` unresolvable and produces `DialogException.ViewFactoryNotRegistered`.

```csharp
public static MauiApp CreateMauiApp()
{
    var builder = MauiApp.CreateBuilder();
    builder.UseMauiApp<App>();
    builder.Services
        .AddSingleton<IKsToast>(_ => Toast.Instance)
        .AddTransient<ItemPage>();

    return builder.Build();
}
```

The caller can `catch` the nested exception type directly. A misconfiguration is not something that can be fixed at run time, so log it and rethrow so that it surfaces during development.

```csharp
private void OnSaveClicked(object? sender, EventArgs e)
{
    try
    {
        _toast.Show(new SavedToastViewModel { Message = "Saved" });
    }
    catch (DialogException.ViewFactoryNotRegistered ex)
    {
        Debug.WriteLine($"Toast view factory is not registered for {ex.ViewModelTypeName}.");
        throw;
    }
}
```

To handle them at once, `catch` the base `DialogException`.
