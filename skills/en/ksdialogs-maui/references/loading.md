# Run work inside a Loading scope

`Loading.Instance` and an `IKsLoading` taken through DI point at the same object. There is one display per process, and a call through either entry coalesces into that same display.

Loading sits in front of Dialog and Toast, does not close on user interaction, and blocks touches to the content behind it.

## How coalescing behaves

Concurrent requests coalesce into the single process-wide display, and the content of the first participant stays until the end of that generation. A participant that is not the last returns when its own action finishes, and only the participant that brings the count to zero waits for container removal. A failed action releases its participation too, and that failure propagates to its caller.

Progress values are accepted from any thread, arrive as an `IProgress<double>`, and are clamped to between 0 and 1.

## Choose a show method

`IKsLoading` (both `Loading.Instance` and what DI hands over are the same object) exposes the following methods. The axes to choose on are: hand the lifetime of the work to Loading (`StartAsync`) or pair show and hide yourself (`ShowAsync` / `HideAsync`), use built-in or custom content, leave the custom content factory to a registration or pass it on the spot, and assemble the custom content's view model yourself or pass only its type and let the library build it.

| Signature | What it does | When to choose it | Registration needed |
|---|---|---|---|
| `Task ShowAsync(string? message = null, DialogPlacement? placement = null)` | Shows the built-in content and adds one participant to the coalescing. It returns once input blocking is in effect and does not wait for the entry transition | When show and hide are paired by your own code | None |
| `Task ShowAsync(ILoadingViewModel viewModel, DialogPlacement? placement = null)` | Shows custom content built by a registered view factory | When a Loading with a replaced look is shown manually | A view factory ([DI registration](di-registration.md)) |
| `Task ShowAsync<TViewModel>(TViewModel viewModel, Func<TViewModel, View> factory, DialogPlacement? placement = null)` | Shows content whose factory is passed on the spot. It does not touch the registry | Content used once | None |
| `Task HideAsync()` | Removes the display of the current generation only, and returns after removal finishes | When pairing with `ShowAsync` | None |
| `void SetMessage(string? message)` | Replaces the message of the built-in content being shown. The only synchronous operation; it does not wait | When only the wording changes while shown | None |
| `Task StartAsync(Func<IProgress<double>, Task> action, string? message = null, DialogPlacement? placement = null)` | Runs the action with the built-in content shown, and removes it when the action finishes | When the lifetime of the work is paired with the display | None |
| `Task<T> StartAsync<T>(Func<IProgress<double>, Task<T>> action, string? message = null, DialogPlacement? placement = null)` | The value-returning form of the above | The same, when the work returns a value | None |
| `Task StartAsync(ILoadingViewModel viewModel, Func<IProgress<double>, Task> action, DialogPlacement? placement = null)` | Runs the action with registered custom content shown | When progress is shown in a look of your own | A view factory ([DI registration](di-registration.md)) |
| `Task<T> StartAsync<T>(ILoadingViewModel viewModel, Func<IProgress<double>, Task<T>> action, DialogPlacement? placement = null)` | The value-returning form of the above | The same, when the work returns a value | A view factory ([DI registration](di-registration.md)) |
| `Task StartAsync<TViewModel>(TViewModel viewModel, Func<TViewModel, View> factory, Func<IProgress<double>, Task> action, DialogPlacement? placement = null)` | Passes the content factory on the spot and runs the action with it shown | When work is wrapped in content used once | None |
| `Task<T> StartAsync<TViewModel, T>(TViewModel viewModel, Func<TViewModel, View> factory, Func<IProgress<double>, Task<T>> action, DialogPlacement? placement = null)` | The value-returning form of the above | The same, when the work returns a value | None |
| `Task ShowAsync<TViewModel>(Action<TViewModel>? configure = null, DialogPlacement? placement = null)` | Runs `configure` on the view model built by the registered view-model factory, then shows the registered custom content | When assembling the view model is left to DI | A view factory and a view-model factory ([DI registration](di-registration.md)) |
| `Task ShowAsync<TViewModel>(Func<TViewModel, Task> configure, DialogPlacement? placement = null)` | The form of the above whose `configure` is asynchronous | The same, when an asynchronous load is needed before presentation | A view factory and a view-model factory ([DI registration](di-registration.md)) |
| `Task StartAsync<TViewModel>(Func<IProgress<double>, Task> action, Action<TViewModel>? configure = null, DialogPlacement? placement = null)` | Runs the action with custom content the library built from the type shown | When the type-based form is used as a scope | A view factory and a view-model factory ([DI registration](di-registration.md)) |
| `Task StartAsync<TViewModel>(Func<IProgress<double>, Task> action, Func<TViewModel, Task> configure, DialogPlacement? placement = null)` | The form of the above whose `configure` is asynchronous | The same, when an asynchronous load is needed before presentation | A view factory and a view-model factory ([DI registration](di-registration.md)) |
| `Task<T> StartAsync<TViewModel, T>(Func<IProgress<double>, Task<T>> action, Action<TViewModel>? configure = null, DialogPlacement? placement = null)` | The value-returning form of the above | The same, when the work returns a value | A view factory and a view-model factory ([DI registration](di-registration.md)) |
| `Task<T> StartAsync<TViewModel, T>(Func<IProgress<double>, Task<T>> action, Func<TViewModel, Task> configure, DialogPlacement? placement = null)` | The value-returning form whose `configure` is asynchronous | The same, when the work returns a value and an asynchronous load is needed first | A view factory and a view-model factory ([DI registration](di-registration.md)) |

Omitting the trailing `placement` uses, for custom content, the attachment on the view, and the library default placement when there is no attachment either. In the examples below, "Wrap work in a display" is the seventh row, "Show, update, and hide manually" the first, fourth, and fifth, "Register custom Loading content" the eighth, and "Show custom content without registration" the third and tenth. Rows twelve through seventeen, which take the view-model type, are covered in "Show a Loading by view-model type". The remaining rows — second, sixth, ninth, and eleventh — appear as minimal examples in the next section.

## Minimal examples for the remaining overloads

The four that do not appear in the samples of the later sections are called as follows. The types used are `SyncLoadingViewModel` (registered) and `NoticeLoadingViewModel` (not registered), both declared in those later sections.

Showing registered custom content manually, paired with `HideAsync`.

```csharp
await _loading.ShowAsync(new SyncLoadingViewModel());
```

Running an action with the built-in content shown, returning no value.

```csharp
await _loading.StartAsync(
    async progress =>
    {
        progress.Report(1);
        await Task.Yield();
    },
    message: "Saving");
```

Running an action with registered custom content shown, and taking its return value.

```csharp
var items = await _loading.StartAsync<IReadOnlyList<string>>(
    new SyncLoadingViewModel(),
    async progress =>
    {
        progress.Report(1);
        await Task.Yield();
        return new[] { "Ready" };
    });
```

Passing the content factory on the spot and taking the return value of the action run with it shown.

```csharp
var count = await _loading.StartAsync(
    new NoticeLoadingViewModel { Message = "Counting" },
    model => new Label { Text = model.Message },
    async progress =>
    {
        progress.Report(1);
        await Task.Yield();
        return 1;
    });
```

## Wrap work in a display

Use `StartAsync` to pair the lifetime of the work with the Loading display. The caller can be a Page or a view model, and can use `Loading.Instance` or what it took as an `IKsLoading`.

```csharp
using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using KsDialogs;
using Microsoft.Maui.Controls;

namespace MyApp;

public partial class SyncPage : ContentPage
{
    private readonly IKsLoading _loading;

    public SyncPage(IKsLoading loading)
    {
        InitializeComponent();
        _loading = loading;
    }

    private async void OnLoadClicked(object? sender, EventArgs e)
    {
        var items = await _loading.StartAsync<IReadOnlyList<string>>(
            async progress =>
            {
                progress.Report(0.5);
                await Task.Yield();
                progress.Report(1);
                return new[] { "Ready" };
            },
            message: "Loading");
        StatusLabel.Text = $"{items.Count} items";
    }
}
```

## Show, update, and hide manually

Use the imperative route when the lifetime of the work is managed elsewhere. Even with several calls coalesced, `HideAsync` removes only the Loading display of the current generation and returns after removal finishes. A later show starts a new generation, and neither the action completion of the hidden old generation nor a delayed progress report ends or updates the new one.

```csharp
private async void OnRunClicked(object? sender, EventArgs e)
{
    await _loading.ShowAsync("Starting");
    await Task.Delay(500);
    _loading.SetMessage("Finishing");
    await _loading.HideAsync();
}
```

## Register custom Loading content

`Loading.Instance.Registry` is a `LoadingViewRegistry` and is `LoadingViewRegistry.Shared`. It is independent of the Dialog and Toast registries, so the same view-model type can be registered in several of them. Register a class-based `ILoadingViewModel` there with `Register`, or wire the pair from DI with `RegisterForLoading<TView, TViewModel>` ([DI registration](di-registration.md)). One entry holds two slots — a view factory and a view-model factory — and `Register` fills the view factory slot. Showing by instance needs that slot alone; showing by view-model type needs both, and `RegisterForLoading` fills both in one line.

When the content needs progress updates, implement `ILoadingProgressReceiver` and `INotifyPropertyChanged` and bind the view to the notifying property. `OnProgress` is called on the UI thread with a non-null value after a progress report. A view model that does not implement the interface simply receives no forwarding.

Layout and transition are attached to the returned view with the same `Dialog` attached properties as for a Dialog ([Layout](layout.md)), except that `Dialog.IsCanceledOnTouchOutside` has no effect on Loading.

### Declare the view model

```csharp
using System.ComponentModel;
using KsDialogs;

namespace MyApp;

public sealed class SyncLoadingViewModel : ILoadingViewModel, ILoadingProgressReceiver, INotifyPropertyChanged
{
    private double _progress;

    public event PropertyChangedEventHandler? PropertyChanged;

    public double Progress
    {
        get => _progress;
        private set
        {
            if (_progress == value)
            {
                return;
            }

            _progress = value;
            PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(nameof(Progress)));
        }
    }

    public void OnProgress(double progress) => Progress = progress;
}
```

### Write the view in XAML

The overlay and the placement are handled by the library's container, so the XAML holds only the card itself. `BindingContext` holds the view model being shown.

```xml
<?xml version="1.0" encoding="utf-8" ?>
<ContentView xmlns="http://schemas.microsoft.com/dotnet/2021/maui"
             xmlns:x="http://schemas.microsoft.com/winfx/2009/xaml"
             xmlns:local="clr-namespace:MyApp"
             x:Class="MyApp.SyncLoadingView"
             x:DataType="local:SyncLoadingViewModel">

    <Border WidthRequest="210" Padding="20" StrokeThickness="0">
        <Border.StrokeShape>
            <RoundRectangle CornerRadius="14" />
        </Border.StrokeShape>

        <VerticalStackLayout Spacing="12">
            <Label Text="Syncing" HorizontalTextAlignment="Center" />
            <ProgressBar Progress="{Binding Progress}" />
        </VerticalStackLayout>
    </Border>

</ContentView>
```

```csharp
using Microsoft.Maui.Controls;

namespace MyApp;

public partial class SyncLoadingView : ContentView
{
    public SyncLoadingView() => InitializeComponent();
}
```

### Register and call

Call `RegisterForLoading` once at startup. With this one-line registration, the view model being shown is set as the view's `BindingContext`.

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
            .AddSingleton<IKsLoading>(_ => Loading.Instance)
            .AddTransient<SyncPage>()
            .RegisterForLoading<SyncLoadingView, SyncLoadingViewModel>();

        return builder.Build();
    }
}
```

```csharp
private async void OnSyncClicked(object? sender, EventArgs e)
{
    await _loading.StartAsync(
        new SyncLoadingViewModel(),
        async progress =>
        {
            progress.Report(0.5);
            await Task.Yield();
            progress.Report(1);
        });
}
```

## Show a Loading by view-model type

Passing only the view-model type instead of an instance runs `configure` on the instance built by the registered view-model factory and then shows it. `RegisterForLoading` wires that view-model factory too, so a one-line registration costs nothing extra. Written at the low level, `Loading.Instance.Registry.RegisterViewModel` fills the view-model factory slot alone; it is independent of the view factory slot, so re-registering one leaves the other in place.

The order is fixed: create the view model, run `configure` to completion, bind the progress receiver, build the content view, and present. Configuration therefore lands before the content factory reads state, and progress reaches the created view model when it implements `ILoadingProgressReceiver`.

Coalescing is decided after `configure` completes. If another call has already settled the display while an asynchronous `configure` was awaited, this call joins the existing display and the view model it created is not used for presentation — the content on screen belongs to the first participant.

An unregistered view-model factory fails with `DialogException.ViewModelFactoryNotRegistered`. A failure thrown by the view-model factory or by `configure` propagates to the caller without reaching presentation or coalescing, and the action of `StartAsync` does not run. Fallback resolution is a Dialog-registry mechanism and does not apply to Loading ([DI registration](di-registration.md)).

Because `action` is the leading argument of `StartAsync`, the existing overload that takes a message and the overload that takes a type are told apart by argument types. The examples below show `SyncLoadingViewModel`, registered in the previous section, by type.

```csharp
private async void OnSyncByTypeClicked(object? sender, EventArgs e)
{
    await _loading.StartAsync<SyncLoadingViewModel>(
        async progress =>
        {
            progress.Report(0.5);
            await Task.Yield();
            progress.Report(1);
        },
        viewModel => viewModel.OnProgress(0));
}
```

Combining an asynchronous `configure` with a value-returning scope takes this shape.

```csharp
private Task<int> CountAsync() =>
    _loading.StartAsync<SyncLoadingViewModel, int>(
        async progress =>
        {
            progress.Report(1);
            await Task.Yield();
            return 1;
        },
        async viewModel =>
        {
            await Task.Yield();
            viewModel.OnProgress(0);
        });
```

Use the type-based form of `ShowAsync` when show and hide are paired by your own code. Omitting `configure` shows the view model exactly as the view-model factory built it.

```csharp
private async void OnHoldClicked(object? sender, EventArgs e)
{
    await _loading.ShowAsync<SyncLoadingViewModel>();
    await Task.Delay(500);
    await _loading.HideAsync();
}
```

## Show custom content without registration

For custom content used once, pass a view factory directly to `ShowAsync` or `StartAsync`. These inline routes do not use a factory already registered for the same view-model type, and they do not add, replace, or remove a registry entry.

```csharp
using KsDialogs;

namespace MyApp;

public sealed class NoticeLoadingViewModel : ILoadingViewModel
{
    public string Message { get; init; } = string.Empty;
}
```

```csharp
private async void OnPrepareClicked(object? sender, EventArgs e)
{
    var viewModel = new NoticeLoadingViewModel { Message = "Preparing" };
    await _loading.ShowAsync(viewModel, model => new Label { Text = model.Message });
    await Task.Delay(500);
    await _loading.HideAsync();
}

private Task SynchronizeAsync() =>
    _loading.StartAsync(
        new NoticeLoadingViewModel { Message = "Synchronizing" },
        model => new Label { Text = model.Message },
        async progress =>
        {
            progress.Report(1);
            await Task.Yield();
        });
```

## Configure the built-in content

Set `Loading.Instance.Style` (a `LoadingStyle` record) and `Loading.Instance.Options` (a `DialogOptions` record) before a display starts. The show methods take no style argument.

| `LoadingStyle` property | Type | Default |
|---|---|---|
| `IndicatorColor` | `Color` | `Colors.White` |
| `MessageFontSize` | `double` | `14` |
| `MessageColor` | `Color` | `Colors.White` |
| `DefaultMessage` | `string?` | `null` |
| `ProgressFormat` | `Func<string?, double?, string>` | `LoadingStyle.DefaultProgressFormat` |

`ProgressFormat` is called on the UI thread, and its progress argument is `null` until progress is first reported. Assign `LoadingStyle.DefaultProgressFormat` to go back to the published built-in formatter, which returns the message alone before a report and appends a rounded percentage after one.

The style and the options are read at display start, so a change takes effect on the next display. Built-in content uses the library's fixed cross-fade and cannot choose a custom transition, and a custom view uses the layout and transition attached to itself rather than `Loading.Instance.Options`.

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
            .AddSingleton<IKsLoading>(_ => Loading.Instance)
            .AddTransient<SyncPage>();

        Loading.Instance.Style = new LoadingStyle
        {
            IndicatorColor = Colors.White,
            MessageColor = Colors.White,
            MessageFontSize = 16,
            DefaultMessage = "Working",
            ProgressFormat = static (message, progress) =>
                progress is double value
                    ? $"{message}\n{value:P0}"
                    : message ?? string.Empty,
        };
        Loading.Instance.Options = new DialogOptions
        {
            LayoutArea = DialogLayoutArea.Window,
            OverlayColor = Colors.Black.WithAlpha(0.6f),
        };

        return builder.Build();
    }
}
```

## Handle misconfiguration failures

A misconfiguration fails with a nested `DialogException` class. A failure that leaves the view model — or the view-model type — passed to `ShowAsync` / `StartAsync` unresolvable on the spot is thrown synchronously at the call, no view is created or shown, and the action of `StartAsync` does not run either (fail-fast). A failure that arises while the content is being built arrives as a `Task` failure, because the caller is still awaiting.

An exception that carries `ViewModelTypeName` also reads back, from that property, the type name of the view model that could not be resolved.

| Exception | Message | Cause and fix |
|---|---|---|
| `DialogException.ViewFactoryNotRegistered` | `No View factory is registered for ViewModel type {TypeName}.` | The custom Loading's view-model type has no view factory. Call `Loading.Instance.Registry.Register` or `RegisterForLoading` for that type |
| `DialogException.ViewModelFactoryNotRegistered` | `No ViewModel factory is registered for ViewModel type {TypeName}.` | Showing by view-model type has no view-model factory. Call `Loading.Instance.Registry.RegisterViewModel` or `RegisterForLoading` for that type (Loading has no fallback) |
| `DialogException.ValueTypeViewModel` | `ViewModel type {TypeName} is a value type and cannot be used as a ViewModel.` | A value-type view model reached the show entry. Make the view model a `class` (`struct` and `record struct` cannot be used) |
| `DialogException.ServiceProviderUnavailable` | `The app's IServiceProvider is not available yet.` | Content wired with `RegisterForLoading` was shown before startup captured the service provider. Show after `MauiApp` has been built ([DI registration](di-registration.md)) |
| `DialogException.PresentationHostUnavailable` | `No screen is available to present the Dialog.` | No screen is available to present on at the moment the custom content is built. Show after the first Page appears |

The messages in the table are the values the current implementation returns, not a stable API (what does not change is the exception type and the condition it is thrown under; the wording can change without notice).

`ServiceProviderUnavailable` also arises when the view-model factory wired by `RegisterForLoading` — the one that pulls the view model from services — is called before startup.

For example, showing `SyncLoadingViewModel` without calling `RegisterForLoading` or `Registry.Register` at startup leaves `SyncLoadingView` unresolvable and produces `DialogException.ViewFactoryNotRegistered`.

```csharp
public static MauiApp CreateMauiApp()
{
    var builder = MauiApp.CreateBuilder();
    builder.UseMauiApp<App>();
    builder.Services
        .AddSingleton<IKsLoading>(_ => Loading.Instance)
        .AddTransient<SyncPage>();

    return builder.Build();
}
```

The caller can `catch` the nested exception type directly and read the type name of the unresolvable view model from `ViewModelTypeName`. A misconfiguration is not something that can be fixed at run time, so log it and rethrow so that it surfaces during development.

```csharp
private async void OnSyncClicked(object? sender, EventArgs e)
{
    try
    {
        await _loading.StartAsync(
            new SyncLoadingViewModel(),
            async progress =>
            {
                progress.Report(1);
                await Task.Yield();
            });
    }
    catch (DialogException.ViewFactoryNotRegistered ex)
    {
        Debug.WriteLine($"Loading view factory is not registered for {ex.ViewModelTypeName}.");
        throw;
    }
}
```

To handle them together, including `ServiceProviderUnavailable` and `PresentationHostUnavailable` which carry no `ViewModelTypeName`, `catch` the base `DialogException`.
