# Show a Dialog

Declare the result type on a class-based view model, register a factory that builds a fresh MAUI view per show, then await `DialogResult`. Branch on `DialogResult<TResult>.Completed` and `DialogResult<TResult>.Cancelled` to distinguish a completion carrying a value from a cancellation. Implementing the non-generic `IDialogViewModel` makes the result type `bool`; implement `IDialogViewModel<TResult>` for a result type of your own.

`DialogViewRegistry.Register` accepts a two-argument factory `Func<TViewModel, DialogNotifier<TResult>, View>` and a one-argument factory `Func<TViewModel, View>`. The factory is called on every show and receives a `DialogNotifier<TResult>` that completes or cancels the result of that show. With the one-argument factory, the view model itself reports through the extension property `Notifier` ([View models](view-models.md)). Only the first notifier report latches the result; later reports do nothing. The result is delivered only after dismissal and container removal finish.

`ShowAsync` takes no `CancellationToken`, so there is no caller-side cancellation path. `Cancelled` comes from a notifier cancel report, an outside tap, the Android back button, or teardown of the screen that hosted the Dialog.

The content is an ordinary MAUI View, so a `ContentView` written in XAML can be its body as it is.

## Choose a `ShowAsync`

`IKsDialog` (both `Dialog.Instance` and what DI injects are the same object) exposes `ShowAsync` in seven overloads. There are two axes to choose on: pass the view model as an instance or as a type only, and leave the content factory to a registration or pass it on the spot. Every overload takes an optional trailing `placement` argument that replaces the placement attached to the content as a whole ([Layout](layout.md)). The return value is `Task<DialogResult<TResult>>`, where `TResult` is `bool` for a view model declared with the non-generic `IDialogViewModel`.

| Signature | What it does | When to choose it | Registration needed |
|---|---|---|---|
| `ShowAsync<TResult>(IDialogViewModel<TResult> viewModel)` | Takes a view-model instance you built, and a registered view factory builds the content | When the caller assembles the view model (passing constructor arguments) | A view factory ([DI registration](di-registration.md)) |
| `ShowAsync<TViewModel, TResult>(TViewModel viewModel, Func<TViewModel, DialogNotifier<TResult>, View> factory)` | Takes both the view-model instance and the content factory on the spot. It does not touch the registry | Content used once. A view model with an explicit result type | None |
| `ShowAsync<TViewModel>(TViewModel viewModel, Func<TViewModel, DialogNotifier<bool>, View> factory)` | The `IDialogViewModel` shorthand for the above (the result is `bool`) | The same, when the result type is not written as a type argument | None |
| `ShowAsync<TViewModel, TResult>(Action<TViewModel>? configure = null)` | Takes only the view-model type, runs `configure` on the instance built by a registered view-model factory, and shows it | When the view model should be resolved through DI. When one-line registration is in place | A view factory plus a view-model factory ([View models](view-models.md)) |
| `ShowAsync<TViewModel, TResult>(Func<TViewModel, Task> configure)` | The asynchronous `configure` form of the above. It builds the content after `configure` completes | When state is prepared asynchronously before presentation | The same |
| `ShowAsync<TViewModel>(Action<TViewModel>? configure = null)` | The `IDialogViewModel` shorthand for type-based show (the result is `bool`) | The same, when the result type is not written as a type argument | The same |
| `ShowAsync<TViewModel>(Func<TViewModel, Task> configure)` | The asynchronous `configure` form of the above | The same, when state is prepared asynchronously | The same |

In the examples below, the `ShowAsync(new ConfirmDialogViewModel(...))` of "Register and call" is the first row, the `ShowAsync<ItemEditDialogViewModel, ItemEdit>(configure)` of "Return a result type other than bool" is the fourth, and the factory passed directly in "Show content without registration" is the third. The remaining rows — second, fifth, sixth, and seventh — appear as minimal examples in the next section.

## Minimal examples for the remaining overloads

The four that do not appear in the samples of the later sections are called as follows. The types used are `ConfirmDialogViewModel` (result `bool`) and `ItemEditDialogViewModel` (result `ItemEdit`), both declared in those later sections. Some types in the later sections, such as `NoticeDialogViewModel` / `NoticeDialogView`, are ones you write yourself in the same shape.

Passing the view-model instance and a two-argument factory on the spot, with the result type explicit as a type argument.

```csharp
var result = await _dialogs.ShowAsync<ItemEditDialogViewModel, ItemEdit>(
    new ItemEditDialogViewModel(),
    (viewModel, _) => new ItemEditDialogView { BindingContext = viewModel });
```

Passing only the view-model type, preparing state in an asynchronous `configure` before presentation.

```csharp
var result = await _dialogs.ShowAsync<ItemEditDialogViewModel, ItemEdit>(
    async viewModel =>
    {
        viewModel.Name = await LoadDefaultNameAsync();
    });
```

Passing only the type of a boolean view model. `configure` can be omitted, and it is shown in the state the view-model factory built.

```csharp
var result = await _dialogs.ShowAsync<ConfirmDialogViewModel>();
```

Passing only the type of a boolean view model, applying an asynchronous `configure` before presentation.

```csharp
var result = await _dialogs.ShowAsync<ConfirmDialogViewModel>(
    async viewModel =>
    {
        await PrepareAsync(viewModel);
    });
```

The following is a minimal setup split into three parts: the view model, the view, and the caller.

## Declare the view model

The view model declares the result type and exposes completion and cancellation as commands. It reports through `this.Notifier`, which is `null` before and after the show, so call it through `?.`.

```csharp
using System.Windows.Input;
using KsDialogs;
using Microsoft.Maui.Controls;

namespace MyApp;

public sealed class ConfirmDialogViewModel : IDialogViewModel
{
    public ConfirmDialogViewModel(string message)
    {
        Message = message;
        CompleteCommand = new Command(() => this.Notifier?.Complete(true));
        CancelCommand = new Command(() => this.Notifier?.Cancel());
    }

    public string Message { get; }

    public ICommand CompleteCommand { get; }

    public ICommand CancelCommand { get; }
}
```

## Write the view in XAML

The overlay and the placement are handled by the library's container, so the XAML holds only the card itself. `BindingContext` holds the view model being shown.

```xml
<?xml version="1.0" encoding="utf-8" ?>
<ContentView xmlns="http://schemas.microsoft.com/dotnet/2021/maui"
             xmlns:x="http://schemas.microsoft.com/winfx/2009/xaml"
             xmlns:local="clr-namespace:MyApp"
             x:Class="MyApp.ConfirmDialogView"
             x:DataType="local:ConfirmDialogViewModel">

    <Border WidthRequest="272" Padding="20" StrokeThickness="0">
        <Border.StrokeShape>
            <RoundRectangle CornerRadius="16" />
        </Border.StrokeShape>

        <VerticalStackLayout Spacing="16">
            <Label Text="{Binding Message}" HorizontalTextAlignment="Center" />

            <Grid ColumnDefinitions="*,10,*">
                <Button Text="Cancel" Command="{Binding CancelCommand}" />
                <Button Grid.Column="2" Text="OK" Command="{Binding CompleteCommand}" />
            </Grid>
        </VerticalStackLayout>
    </Border>

</ContentView>
```

```csharp
using Microsoft.Maui.Controls;

namespace MyApp;

public partial class ConfirmDialogView : ContentView
{
    public ConfirmDialogView() => InitializeComponent();
}
```

## Register and call

Call `Register` once at startup. The library does not set `BindingContext` on a view returned by an explicitly registered factory — the factory puts the view model being shown into it (a one-line registration through `RegisterForDialog` does set it; see [DI registration](di-registration.md)).

The caller can use `Dialog.Instance` or what it took as an injected `IKsDialog`. Both point at the same registry and the same state.

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
            .AddSingleton<IKsDialog>(_ => Dialog.Instance)
            .AddTransient<ItemPage>();

        Dialog.Instance.Registry.Register<ConfirmDialogViewModel>(
            viewModel => new ConfirmDialogView { BindingContext = viewModel });

        return builder.Build();
    }
}
```

```csharp
using System;
using KsDialogs;
using Microsoft.Maui.Controls;

namespace MyApp;

public partial class ItemPage : ContentPage
{
    private readonly IKsDialog _dialogs;

    public ItemPage(IKsDialog dialogs)
    {
        InitializeComponent();
        _dialogs = dialogs;
    }

    private async void OnDeleteClicked(object? sender, EventArgs e)
    {
        var result = await _dialogs.ShowAsync(new ConfirmDialogViewModel("Delete this item?"));
        StatusLabel.Text = result switch
        {
            DialogResult<bool>.Completed { Value: true } => "Deleted",
            _ => "Kept",
        };
    }
}
```

## Return a result type other than bool

Implementing `IDialogViewModel<TResult>` puts any type in `TResult`, a record or a `string` alike. The types of `Notifier` and `DialogResult<TResult>` are derived from that declaration, so reporting with another type does not compile.

`RegisterForDialog<TView, TViewModel, TResult>` wires both the view factory and the view-model factory in one line, which also makes the type-based `ShowAsync` that takes only the view-model type available. The view is a `ContentView` shaped like `ConfirmDialogView` above, with `x:DataType` set to `ItemEditDialogViewModel` and `SaveCommand` / `CancelCommand` bound.

```csharp
using System.Windows.Input;
using KsDialogs;
using Microsoft.Maui.Controls;

namespace MyApp;

public sealed record ItemEdit(string Name, int Quantity);

public sealed class ItemEditDialogViewModel : IDialogViewModel<ItemEdit>
{
    public ItemEditDialogViewModel()
    {
        SaveCommand = new Command(() => this.Notifier?.Complete(new ItemEdit(Name, Quantity)));
        CancelCommand = new Command(() => this.Notifier?.Cancel());
    }

    public string Name { get; set; } = string.Empty;

    public int Quantity { get; set; }

    public ICommand SaveCommand { get; }

    public ICommand CancelCommand { get; }
}
```

```csharp
builder.Services.RegisterForDialog<ItemEditDialogView, ItemEditDialogViewModel, ItemEdit>();
```

```csharp
private async void OnEditClicked(object? sender, EventArgs e)
{
    var result = await _dialogs.ShowAsync<ItemEditDialogViewModel, ItemEdit>(
        viewModel => viewModel.Name = "Apple");
    if (result is DialogResult<ItemEdit>.Completed { Value: var edit })
    {
        StatusLabel.Text = $"{edit.Name} x{edit.Quantity}";
    }
}
```

## Show content without registration

For content used once, pass the factory directly to `ShowAsync`. This route does not add, replace, or remove a registry entry, and it does not use a factory already registered for the same view-model type. The view can be the same one the registered route uses, so a `ContentView` written in XAML can be passed as it is.

```csharp
private async void OnAboutClicked(object? sender, EventArgs e)
{
    var result = await _dialogs.ShowAsync(
        new NoticeDialogViewModel("Ready"),
        (viewModel, _) => new NoticeDialogView { BindingContext = viewModel });
    StatusLabel.Text = result is DialogResult<bool>.Completed ? "Read" : "Dismissed";
}
```

## Stack independent Dialogs

Use separate view-model instances for concurrent shows. The later Dialog is in front and each call keeps its own result. If the lower Dialog reports first, on iOS both disappear and the upper result is cancelled, while on Android only the lower one closes and the upper remains usable until it reports its own result. The lower one can report first only when the app holds its reporting handle; user operations (completion, cancellation, an outside tap, the back button) always reach the frontmost one alone.

```csharp
private async void OnShowTwoClicked(object? sender, EventArgs e)
{
    var first = _dialogs.ShowAsync(new ConfirmDialogViewModel("First"));
    var second = _dialogs.ShowAsync(new ConfirmDialogViewModel("Second"));
    var results = await Task.WhenAll(first, second);
    StatusLabel.Text = $"{results[0]} / {results[1]}";
}
```

## Handle misconfiguration failures

A misconfiguration does not return `Cancelled`; it faults the `Task` with a nested `DialogException` class, so that a missing registration cannot be mistaken for an end user's cancellation, and in that case no view is created or shown. It is thrown straight to the awaiting caller, so handle it in a form that surfaces during development rather than swallowing it in a `try` / `catch`.

An exception that carries `ViewModelTypeName` also reads back, from that property, the type name of the view model that could not be resolved.

| Exception | Message | Cause and fix |
|---|---|---|
| `DialogException.ViewFactoryNotRegistered` | `No View factory is registered for ViewModel type {TypeName}.` | Neither an explicit registration nor a fallback resolves the content view. Call `Register` / `RegisterForDialog` for that view-model type, or set up convention resolution with `UseViewFallback` |
| `DialogException.ViewModelFactoryNotRegistered` | `No ViewModel factory is registered for ViewModel type {TypeName}.` | Type-based show has no view-model factory. Call `RegisterViewModel` / `RegisterForDialog`, or set up `UseViewModelFallback` ([View models](view-models.md)) |
| `DialogException.PresentationHostUnavailable` | `No screen is available to present the Dialog.` | No screen is available to present on. Show after the first Page appears; it fails immediately rather than queueing |
| `DialogException.ServiceProviderUnavailable` | `The app's IServiceProvider is not available yet.` | A DI-backed route (one-line registration, fallback resolution) ran before startup captured the service provider. Show after `MauiApp` has been built ([DI registration](di-registration.md)) |
| `DialogException.ViewModelAlreadyShowing` | `This ViewModel instance of type {TypeName} is already being shown.` | The same view-model instance is already being shown. Build a new instance for each stacked show |
| `DialogException.ValueTypeViewModel` | `ViewModel type {TypeName} is a value type and cannot be used as a ViewModel.` | A value-type view model reached the presentation entry. Make the view model a `class` (`struct` and `record struct` cannot be used) |

The messages in the table are the values the current implementation returns, not a stable API (what does not change is the exception type and the condition it is thrown under; the wording can change without notice).

For example, showing `ConfirmDialogViewModel` without calling any of `Register`, `RegisterForDialog`, or `UseViewFallback` at startup leaves `ConfirmDialogView` unresolvable and produces `DialogException.ViewFactoryNotRegistered`.

```csharp
public static MauiApp CreateMauiApp()
{
    var builder = MauiApp.CreateBuilder();
    builder.UseMauiApp<App>();
    builder.Services
        .AddSingleton<IKsDialog>(_ => Dialog.Instance)
        .AddTransient<ItemPage>();

    return builder.Build();
}
```

The caller can `catch` the nested exception type directly and read the type name of the unresolvable view model from `ViewModelTypeName`. A misconfiguration is not something that can be fixed at run time, so log it and rethrow so that it surfaces during development.

```csharp
private async void OnDeleteClicked(object? sender, EventArgs e)
{
    try
    {
        var result = await _dialogs.ShowAsync(new ConfirmDialogViewModel("Delete this item?"));
        StatusLabel.Text = result switch
        {
            DialogResult<bool>.Completed { Value: true } => "Deleted",
            _ => "Kept",
        };
    }
    catch (DialogException.ViewFactoryNotRegistered ex)
    {
        Debug.WriteLine($"Dialog view factory is not registered for {ex.ViewModelTypeName}.");
        throw;
    }
}
```

To handle them together, including `PresentationHostUnavailable` and `ServiceProviderUnavailable` which carry no `ViewModelTypeName`, `catch` the base `DialogException`.
