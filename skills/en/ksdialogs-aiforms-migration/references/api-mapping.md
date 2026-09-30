# API mapping from AiForms.Maui.Dialogs

These tables cover the consumer-facing API listed by the AiForms.Maui.Dialogs README and the additional members exposed by the same documented public types. Each old member appears once. "No direct counterpart" means the old abstraction was removed; the last column gives the migration approach. Use the `ksdialogs-maui` Skill for complete KsDialogs.Maui recipes.

## Replace package setup and IoC configuration

| Old member | New counterpart or status | Migration approach |
|---|---|---|
| `AiForms.Maui.Dialogs` package | `KsDialogs.Maui` package | Replace the package reference and change `AiForms.Dialogs` imports to `KsDialogs` |
| `Dialog.Instance` / `IDialog` | `Dialog.Instance` / `IKsDialog` | Keep the default entry, or inject the new contract |
| `Loading.Instance` / `ILoading` | `Loading.Instance` / `IKsLoading` | Keep the default entry, or inject the new contract |
| `Toast.Instance` / `IToast` | `Toast.Instance` / `IKsToast` | The new type is not obsolete and also supplies message notifications |
| `Configurations.LoadingConfig` | `Loading.Instance.Style` and `Loading.Instance.Options` | Split visual loading style from container options |
| `Configurations.SetIocConfig(viewTypeGetter, viewResolver)` | `RegisterForDialog`, or `AddKsDialogs` with `options.UseViewFallback(...)` and `options.UseViewModelFallback()` | Prefer explicit registrations; use the fallbacks only when convention-based resolution should obtain Views or ViewModels from MAUI DI |
| `LoadingConfig` | `LoadingStyle` and `DialogOptions` | Replace the old combined settings object with the two focused values |

`RegisterForDialog<TView, TViewModel>()` wires the View factory slot and the ViewModel factory slot in one call and registers both types as transient services; `RegisterForDialog<TView, TViewModel, TResult>()` is the form for a declared result type. `RegisterForLoading<TView, TViewModel>()` and `RegisterForToast<TView, TViewModel>()` do the same for the Loading and Toast registries: each wires both slots, so one line enables showing by ViewModel instance and by ViewModel type. Neither has a result-type form, because Loading and Toast return no result. Because the library itself builds the View for a one-line registration, a construction that fails there (a constructor dependency missing from the services, for instance) is reported as `DialogException.ViewCreationFailed` rather than as a missing factory: the original failure stays in `InnerException`, and `ViewTypeName` and `ViewModelTypeName` name the pair that could not be built. A factory you wrote yourself and a fallback resolver are not wrapped — their exceptions arrive as thrown.

Registrations and fallbacks live on the process-wide registries (`DialogViewRegistry.Shared`, `LoadingViewRegistry.Shared`, `ToastViewRegistry.Shared`), also reachable as `Dialog.Instance.Registry`, `Loading.Instance.Registry`, and `Toast.Instance.Registry`. The `AddKsDialogs` fallbacks belong to the Dialog registry alone — Loading and Toast take explicit or one-line registration only, and an unresolved type fails there. Explicit registration wins over a fallback, a repeated `AddKsDialogs` merges only the slots it sets (so an argument-less call does not erase a configured fallback), and the public API has no way to remove a configured fallback. DI-backed registration and fallback resolution depend on the application service provider captured during MAUI startup: calling such a route earlier fails with `DialogException.ServiceProviderUnavailable` rather than falling back to an unrelated construction path.

```csharp
using KsDialogs;
using Microsoft.Extensions.DependencyInjection;

namespace MyApp;

public static class MauiProgram
{
    public static MauiApp CreateMauiApp()
    {
        MauiAppBuilder builder = MauiApp.CreateBuilder();
        builder.UseMauiApp<App>();

        builder.Services
            .AddKsDialogs(options => options.UseViewModelFallback())
            .RegisterForDialog<ConfirmDialogView, ConfirmViewModel>()
            .RegisterForLoading<UploadLoadingView, UploadViewModel>()
            .RegisterForToast<NoticeToastView, NoticeViewModel>();

        Loading.Instance.Style = Loading.Instance.Style with
        {
            IndicatorColor = Colors.White,
            MessageFontSize = 14d,
            DefaultMessage = "Working",
        };
        Loading.Instance.Options = Loading.Instance.Options with
        {
            OverlayColor = Color.FromRgba(0, 0, 0, 128),
        };

        return builder.Build();
    }
}
```

## Migrate dialog display and reuse

| Old member | New counterpart or status | Migration approach |
|---|---|---|
| `IDialog.ShowAsync<TView>(object viewModel = null)` | `IKsDialog.ShowAsync(viewModel)` after factory registration | Register the ViewModel-to-View factory; the result is `DialogResult<bool>` unless the ViewModel declares another result type |
| `IDialog.ShowAsync(DialogView view, object viewModel = null)` | Inline `IKsDialog.ShowAsync(viewModel, factory)` | Return a fresh ordinary MAUI `View` from the call-scoped factory |
| `IDialog.ShowAsync(object viewModel)` | `IKsDialog.ShowAsync<TResult>(IDialogViewModel<TResult>)` | Implement the new typed ViewModel contract and register its View factory |
| `IDialog.ShowFromModelAsync<TViewModel>()` | `IKsDialog.ShowAsync<TViewModel>()` | Register View and ViewModel factories, then use the type-based overload |
| `IDialog.ShowFromModelAsync<TViewModel, TParameter>(TParameter parameter)` | `IKsDialog.ShowAsync<TViewModel>(configure)` | Apply the parameter in the synchronous or asynchronous configure callback |
| `IDialog.ShowResultAsync<TView, TResult>(object viewModel = null)` | `IKsDialog.ShowAsync<TResult>(viewModel)` after factory registration | Declare `TResult` on the ViewModel and inspect `DialogResult<TResult>` |
| `IDialog.ShowResultAsync<TResult>(object viewModel)` | `IKsDialog.ShowAsync<TResult>(viewModel)` | Result typing moves from the call alone to `IDialogViewModel<TResult>` |
| `IDialog.ShowResultFromModelAsync<TViewModel, TParameter, TResult>(TParameter parameter)` | `IKsDialog.ShowAsync<TViewModel, TResult>(configure)` | Register both factories and apply the parameter before presentation |
| `IDialog.Create<TView>(object viewModel = null)` | No direct counterpart | Remove reusable dialog ownership; register a factory and call `ShowAsync` for each display |
| `IDialog.Create(DialogView view, object viewModel = null)` | No direct counterpart | Use an inline factory for one-off content or a registered factory for repeated calls |
| `IReusableDialog.ShowAsync()` | No direct counterpart | Call the ordinary `ShowAsync` route with a fresh ViewModel |
| `IReusableDialog.ShowResultAsync<TResult>()` | No direct counterpart | Call typed `ShowAsync` and handle `DialogResult<TResult>` |
| `IReusableDialog.Dispose()` | No direct counterpart | Remove manual reusable-dialog disposal; KsDialogs creates fresh content per show |

`DialogViewRegistry` owns the View and ViewModel factory slots. Register a View factory before instance-based show, and register a ViewModel factory as well (`RegisterViewModel`, or `RegisterForDialog`) before type-based show. A missing View factory fails with `DialogException.ViewFactoryNotRegistered`, a missing ViewModel factory or fallback fails with `DialogException.ViewModelFactoryNotRegistered`, and a View that a one-line registration wired but the library could not build fails with `DialogException.ViewCreationFailed`; these failures arrive as faulted tasks and are not converted to a cancelled result. `DialogException.PresentationHostUnavailable` occurs only when you show on plain .NET, which has no presentation machinery (the `net10.0` target of a unit test, for example). On iOS and Android, a show made while no screen is available to present on does not fail; it waits until a screen appears and then displays. When several dialogs are waiting, those called in succession from the UI thread appear one at a time in call order, each later one on top (the order is not guaranteed for shows called from other threads, or for a type-based show whose asynchronous configure awaits).

Rewrite of `ShowResultFromModelAsync<TViewModel, TParameter, TResult>` — the ViewModel declares the result type and reports through its notifier:

```csharp
public sealed class EditViewModel : IDialogViewModel<string>
{
    public string Text { get; set; } = string.Empty;

    public void Submit() => this.Notifier?.Complete(Text);

    public void Dismiss() => this.Notifier?.Cancel();
}
```

Register the pair once, then show by type and apply the old parameter in the configure callback:

```csharp
builder.Services.RegisterForDialog<EditDialogView, EditViewModel, string>();

DialogResult<string> result = await Dialog.Instance.ShowAsync<EditViewModel, string>(
    viewModel => viewModel.Text = "initial");

string text = result is DialogResult<string>.Completed completed
    ? completed.Value
    : string.Empty;
```

Rewrite of `ShowAsync(DialogView view, object viewModel)` — pass the factory to the call instead of a built View. The inline route leaves the registry untouched:

```csharp
DialogResult<bool> result = await Dialog.Instance.ShowAsync(
    new ConfirmViewModel("Delete this item?"),
    (viewModel, notifier) => new ConfirmContentView(viewModel, notifier));
```

Cancel a show called from a place where a screen may never appear — the wait for a presentation host has no upper bound, so pass the `CancellationToken` that every `ShowAsync` overload takes as its last parameter. Cancelling during the wait means the dialog is never shown; cancelling while it is shown closes it after its exit animation finishes. Either way `OperationCanceledException` is thrown instead of a `Cancelled` result. If the token is already cancelled at the call, it throws without resolving the registration or creating the ViewModel:

```csharp
using CancellationTokenSource cts = new(TimeSpan.FromSeconds(30));

bool confirmed;
try
{
    DialogResult<bool> result = await Dialog.Instance.ShowAsync(
        new ConfirmViewModel("Delete this item?"),
        cancellationToken: cts.Token);
    confirmed = result is DialogResult<bool>.Completed { Value: true };
}
catch (OperationCanceledException)
{
    confirmed = false;
}
```

## Migrate ViewModels and result reporting

| Old member | New counterpart or status | Migration approach |
|---|---|---|
| `IDialogViewModel<T>.DialogInitializeAsync(T parameter)` | `IDialogViewModel<TResult>` plus type-based `ShowAsync(configure)` | `T` now declares the result type; move initialization into the configure callback |
| `IDialogViewModelDestroy.Destroy()` | No direct counterpart | Remove the old lifecycle interface; release app-owned resources after awaiting the show |
| `IDialogNotifier.Complete()` | `DialogNotifier<bool>.Complete(true)` | Use the notifier supplied to the factory or the `viewModel.Notifier` extension property |
| `IDialogNotifier.Complete<T>(T result)` | `DialogNotifier<TResult>.Complete(TResult value)` | Declare the same result type on `IDialogViewModel<TResult>` |
| `IDialogNotifier.Cancel()` | `DialogNotifier<TResult>.Cancel()` | Cancellation becomes `DialogResult<TResult>.Cancelled` |
| `DialogNotifier` | `DialogNotifier<TResult>` | The notifier is typed and scoped to one show |
| `DialogView.DialogNotifier` | Factory notifier or `viewModel.Notifier` | Do not bind a notifier property on the View |

A ViewModel that only answers OK or cancel can declare the non-generic `IDialogViewModel` (equivalent to `IDialogViewModel<bool>`) and skip the result type argument. `viewModel.Notifier` returns a notifier while that instance is being shown and `null` before and after; write `this.Notifier` from inside the ViewModel.

Use class-based ViewModels: registration and type-based show reject value types at compile time, and instance-based show rejects them with `DialogException.ValueTypeViewModel`. Do not start a second show with the same live instance; it fails with `DialogException.ViewModelAlreadyShowing` so the first show's notifier identity remains intact.

## Migrate default and custom loading

The old reusable Loading object owned its own display. KsDialogs.Maui has no handle with the same ownership; overlapping calls coalesce into one display that lasts from the first start to the last end. `IKsLoading.HideAsync()` closes the current process-wide Loading generation regardless of joined use, but it does not cancel actions that are already running. A mechanical replacement can therefore hide Loading owned by other work. Prefer the operation-scoped `IKsLoading.StartAsync` route when the old show/hide pair surrounds an operation.

| Old member | New counterpart or status | Migration approach |
|---|---|---|
| `ILoading.StartAsync(action, message, isCurrentScope)` | `IKsLoading.StartAsync(action, message, placement, actionThread)` | Remove `isCurrentScope`; use a placement when position must differ. The operation starts on the UI thread by default (`LoadingActionThread.Main`) |
| `ILoading.Show(message, isCurrentScope)` | `IKsLoading.ShowAsync(message, placement)` | Await the new start of presentation and remove `isCurrentScope` |
| `ILoading.Hide()` | `IKsLoading.HideAsync()` | Await dismissal and removal |
| `ILoading.SetMessage(message)` | `IKsLoading.SetMessage(message)` | The name remains; it affects built-in loading content while shown |
| `ILoading.Create<TView>(object viewModel = null)` | No direct counterpart | Register a custom Loading factory (`RegisterForLoading`, or `Loading.Instance.Registry.Register<TViewModel>`) and pass its ViewModel to `ShowAsync` or `StartAsync` |
| `ILoading.Create(LoadingView view, object viewModel = null)` | No direct counterpart | Use an inline factory or a registered factory that returns a fresh ordinary MAUI `View` |
| `ILoading.Create(object viewModel)` | No direct counterpart | Implement `ILoadingViewModel`, register its factory, and pass the instance to the new entry |
| `ILoading.CreateFromModel<TViewModel>()` | `IKsLoading.ShowAsync<TViewModel>(configure, placement)` or `StartAsync<TViewModel>(action, configure, placement, actionThread)` | Wire the pair with `RegisterForLoading`, then pass the ViewModel type; the library resolves the instance through the registered ViewModel factory |
| Debug-only `ILoading.Dispose()` | No direct counterpart | Remove the test-only disposal call; the new entry has no public disposal contract |
| `IReusableLoading.Show(bool isCurrentScope = false)` | `IKsLoading.ShowAsync(viewModel, placement)` | Show registered custom content; there is no reusable handle |
| `IReusableLoading.StartAsync(action, bool isCurrentScope = false)` | `IKsLoading.StartAsync(viewModel, action, placement, actionThread)` | Scope the display to the operation instead of keeping a reusable handle |
| `IReusableLoading.Hide()` (`Task` in the public interface and implementations) | No directly equivalent ownership handle; `IKsLoading.HideAsync()` affects the shared current generation | Do not replace it mechanically. Prefer `IKsLoading.StartAsync`; if explicitly closing the shared display is intended, await `IKsLoading.HideAsync()`. The old README incorrectly described this member as returning `void` |
| `IReusableLoading.Dispose()` | No direct counterpart | Remove reusable-handle disposal |

Every overload of the operation-scoped `StartAsync` takes `LoadingActionThread actionThread = LoadingActionThread.Main` as its last parameter, which fixes the thread the operation starts on regardless of the caller's thread. Calls that omit it bind to the same overloads as before.

| Value | Thread the operation starts on |
|---|---|
| `LoadingActionThread.Main` (default) | The UI thread; the operation can touch screen elements without moving back to the UI thread |
| `LoadingActionThread.Background` | Off the UI thread (the thread pool); use it for heavy work that does not touch the UI |

Only the thread that runs the operation's first statement is fixed; where execution continues after an `await` follows the C# rules (`ConfigureAwait(false)` does not return to the UI thread). On plain .NET, which has no UI thread, the operation runs in place on the caller's thread whatever the value. Heavy UI-free work written synchronously under the default blocks the UI thread, and progress and message updates do not reach the screen while it is blocked:

```csharp
await Loading.Instance.StartAsync(
    async progress => await CompressAsync(progress),
    message: "Compressing",
    actionThread: LoadingActionThread.Background);
```

A custom Loading ViewModel implements `ILoadingViewModel`, and receives progress only when it also implements `ILoadingProgressReceiver`:

```csharp
public sealed class UploadViewModel : ILoadingViewModel, ILoadingProgressReceiver
{
    public string Title { get; set; } = string.Empty;

    public double Progress { get; private set; }

    public void OnProgress(double progress) => Progress = progress;
}
```

Rewrite of a reusable custom Loading handle — the display is scoped to the operation. Once the ViewModel type resolves, the action runs even when no presentation host is available, and the display appears when a host appears if it is still in progress then (nothing is displayed if the operation ends first). An unregistered type fails before the action starts, whether or not a host is available:

```csharp
await Loading.Instance.StartAsync(
    new UploadViewModel(),
    async progress =>
    {
        progress.Report(0.5);
        await UploadAsync();
        progress.Report(1);
    });
```

Rewrite of `CreateFromModel<TViewModel>` — pass the ViewModel type and let the registered ViewModel factory resolve the instance from DI. `configure` finishes before the content View is built; a missing ViewModel factory fails with `DialogException.ViewModelFactoryNotRegistered`, which is a separate failure from a missing View factory. When a host is available at the start, a View wired by `RegisterForLoading` that the library cannot build fails the show or start with `DialogException.ViewCreationFailed`. A display started without a host builds its content when a host appears, so a build failure at that point does not return to the show or start: it leaves a warning, gives up only the display, and lets the operation continue. A call that joins a display already on screen still builds and configures its ViewModel, but that instance is not the one on screen:

```csharp
builder.Services.RegisterForLoading<UploadLoadingView, UploadViewModel>();

await Loading.Instance.StartAsync<UploadViewModel>(
    async progress => await UploadAsync(progress),
    viewModel => viewModel.Title = "Uploading");
```

## Move LoadingConfig values

| Old member | New counterpart or status | Migration approach |
|---|---|---|
| `LoadingConfig.OffsetX` | `DialogPlacement.OffsetX` | Pass placement per call; there is no global Loading offset |
| `LoadingConfig.OffsetY` | `DialogPlacement.OffsetY` | Pass placement per call; there is no global Loading offset |
| `LoadingConfig.IndicatorColor` | `LoadingStyle.IndicatorColor` | Set it through `Loading.Instance.Style` |
| `LoadingConfig.FontSize` | `LoadingStyle.MessageFontSize` | Set it through `Loading.Instance.Style` |
| `LoadingConfig.FontColor` | `LoadingStyle.MessageColor` | Set it through `Loading.Instance.Style` |
| `LoadingConfig.OverlayColor` | `DialogOptions.OverlayColor` | Set it through `Loading.Instance.Options` |
| `LoadingConfig.Opacity` | No single equivalent | The old value faded the overlay, indicator, and message together. To approximate it, apply the same alpha to `DialogOptions.OverlayColor`, `LoadingStyle.IndicatorColor`, and `LoadingStyle.MessageColor`; changing only the overlay leaves the indicator and message opaque |
| `LoadingConfig.DefaultMessage` | `LoadingStyle.DefaultMessage` | Set it through `Loading.Instance.Style` |
| `LoadingConfig.ProgressMessageFormat` | `LoadingStyle.ProgressFormat` | Replace the format string with a `Func<string?, double?, string>` delegate; `LoadingStyle.DefaultProgressFormat` is the built-in one |
| `LoadingConfig.IsReusable` | No direct counterpart | Remove view reuse; overlapping loading calls join one native display without reusing app content |

`LoadingStyle` and `DialogOptions` are records, so `with` replaces single values. The container reads both at the start of each display: a change applies to the next display, not to the one on screen. `IsCanceledOnTouchOutside` has no effect for Loading — the display closes on `HideAsync` or on the end of the coalesced work, not on user input.

## Move ExtraView layout and lifecycle behavior

Review the changed defaults before preserving the old appearance: the overlay changes from transparent to 40% black, and the layout area changes from the whole window to the visible area. The dialog margin default stays 0 on every side, as before, so keeping the old margin needs no attachment.

| Old member | New counterpart or status | Migration approach |
|---|---|---|
| `ExtraView` | Ordinary MAUI `View` plus `Dialog` attached properties | Stop deriving dialog content from a library base class |
| `ExtraView.ProportionalWidth` | `Dialog.SetProportionalWidth` | Attach the value to the ordinary MAUI content View |
| `ExtraView.ProportionalWidthProperty` | `Dialog.ProportionalWidthProperty` | Replace direct `BindableProperty` references in XAML styles or code with the new attached property |
| `ExtraView.ProportionalHeight` | `Dialog.SetProportionalHeight` | Attach the value to the ordinary MAUI content View |
| `ExtraView.ProportionalHeightProperty` | `Dialog.ProportionalHeightProperty` | Replace direct `BindableProperty` references in XAML styles or code with the new attached property |
| `ExtraView.VerticalLayoutAlignment` | `Dialog.SetVerticalAlignment` with `DialogAlignment` | Attach placement to the content View |
| `ExtraView.VerticalLayoutAlignmentProperty` | `Dialog.VerticalAlignmentProperty` with `DialogAlignment` | Replace direct `BindableProperty` references and use the renamed attached property |
| `ExtraView.HorizontalLayoutAlignment` | `Dialog.SetHorizontalAlignment` with `DialogAlignment` | Attach placement to the content View |
| `ExtraView.HorizontalLayoutAlignmentProperty` | `Dialog.HorizontalAlignmentProperty` with `DialogAlignment` | Replace direct `BindableProperty` references and use the renamed attached property |
| `ExtraView.OffsetX` | `Dialog.SetOffsetX` | Attach placement to the content View |
| `ExtraView.OffsetXProperty` | `Dialog.OffsetXProperty` | Replace direct `BindableProperty` references in XAML styles or code with the new attached property |
| `ExtraView.OffsetY` | `Dialog.SetOffsetY` | Attach placement to the content View |
| `ExtraView.OffsetYProperty` | `Dialog.OffsetYProperty` | Replace direct `BindableProperty` references in XAML styles or code with the new attached property |
| `ExtraView.CornerRadius` | No library counterpart | Draw corners in the content View itself |
| `ExtraView.CornerRadiusProperty` | No direct counterpart | Remove the library `BindableProperty` reference and style corners on the content View itself |
| `ExtraView.BorderColor` | No library counterpart | Draw the border in the content View itself |
| `ExtraView.BorderColorProperty` | No direct counterpart | Remove the library `BindableProperty` reference and style the border color on the content View itself |
| `ExtraView.BorderWidth` | No library counterpart | Draw the border in the content View itself |
| `ExtraView.BorderWidthProperty` | No direct counterpart | Remove the library `BindableProperty` reference and style the border width on the content View itself |
| `ExtraView.AutoRotateForIOS` | No direct counterpart | Remove it; rotation follows the host and the dialog is relaid out |
| `ExtraView.AutoRotateForIOSProperty` | No direct counterpart | Remove the library `BindableProperty` reference; rotation follows the host and the dialog is relaid out |
| `ExtraView.DialogMargin` | `Dialog.SetDialogMargin` with `Thickness` | Attach margin to the content View |
| `ExtraView.DialogMarginProperty` | `Dialog.DialogMarginProperty` | Replace direct `BindableProperty` references in XAML styles or code with the new attached property |
| `ExtraView.RunPresentationAnimation()` | `Dialog.SetTransition` with a presentation hook or preset | Move presentation animation to `DialogTransition` |
| `ExtraView.RunDismissalAnimation()` | `Dialog.SetTransition` with a dismissal hook or preset | Move dismissal animation to `DialogTransition` |
| `ExtraView.Destroy()` | No direct counterpart | Remove the View lifecycle override; content is created fresh for each display |

For the alignment rows, `DialogAlignment` offers `Start`, `Center`, `End`, and `Fill`. `Start` and `End` mean the physical near and far ends of the axis and do not follow right-to-left reading order, `Center` places the content in the middle of the effective area, and `Fill` stretches position and size to that area — on an axis that already carries a proportional size, `Fill` is treated as `Center`.

Content size is measured the MAUI way, including the root's `WidthRequest` / `HeightRequest`, so a `ContentView` root and a `Grid` root get the same size on iOS and Android. On an axis where the container decides the size through a proportional size or `Fill`, a root with an explicit size does not stretch to the frame: it keeps its declared size and is centered in the frame. Remove the explicit size on that axis to fill the frame.

For both animation rows, the new hook is a `Func<VisualElement, Task>` that receives the content View, and `DialogTransition(presentation, dismissal, overlayDuration)` carries the pair plus the overlay fade time (`OverlayDuration`). The presets `DialogTransition.Fade(duration, easing)`, `DialogTransition.Slide(from, duration, easing)` with a `DialogTransitionEdge`, `DialogTransition.Zoom(duration, easing)`, and `DialogTransition.None()` return a filled pair; take `.Presentation` or `.Dismissal` from a preset to combine it with a hand-written hook. `DialogTransitionEdge` offers `Top`, `Bottom`, `Start`, and `End`; `Start` and `End` follow layout direction and swap in a right-to-left environment, while `Top` and `Bottom` stay physical. KsDialogs awaits the returned `Task` to completion and has no implicit timeout; the ways out of a hook that never finishes are an OS-initiated removal of the container and cancellation through the `CancellationToken` passed to `ShowAsync`; after a dismissal report, the dialog result is delivered only after the dismissal hook and overlay removal have completed. A transition value holds closures, so attach it from code-behind (`Dialog.SetTransition` / `Dialog.GetTransition` / `Dialog.TransitionProperty`) rather than from XAML.

Attach the moved settings to the content View in code-behind:

```csharp
View content = new ConfirmContentView(viewModel, notifier);

Dialog.SetLayoutArea(content, DialogLayoutArea.Window);
Dialog.SetDialogMargin(content, new Thickness(16d));
Dialog.SetProportionalWidth(content, 0.9d);
Dialog.SetIsCanceledOnTouchOutside(content, false);
Dialog.SetOverlayColor(content, Color.FromRgba(0, 0, 0, 102));
Dialog.SetTransition(content, DialogTransition.Slide(DialogTransitionEdge.Bottom));
```

The layout attributes are also writable in XAML on the content View itself:

```xml
<ContentView xmlns="http://schemas.microsoft.com/dotnet/2021/maui"
             xmlns:x="http://schemas.microsoft.com/winfx/2009/xaml"
             xmlns:ksd="clr-namespace:KsDialogs;assembly=KsDialogs.Maui"
             x:Class="MyApp.ConfirmContentView"
             ksd:Dialog.OverlayColor="#80000000"
             ksd:Dialog.ProportionalWidth="0.9"
             ksd:Dialog.VerticalAlignment="End">
</ContentView>
```

## Move DialogView behavior

| Old member | New counterpart or status | Migration approach |
|---|---|---|
| `DialogView` | Ordinary MAUI `View` returned by a dialog factory | Stop deriving dialog content from a library view type |
| `DialogView.IsCanceledOnTouchOutside` | `Dialog.SetIsCanceledOnTouchOutside` | Attach it to the ordinary MAUI content View |
| `DialogView.IsCanceledOnTouchOutsideProperty` | `Dialog.IsCanceledOnTouchOutsideProperty` | Replace direct `BindableProperty` references in XAML styles or code with the new attached property |
| `DialogView.OverlayColor` | `Dialog.SetOverlayColor` | Attach it to the ordinary MAUI content View |
| `DialogView.OverlayColorProperty` | `Dialog.OverlayColorProperty` | Replace direct `BindableProperty` references in XAML styles or code with the new attached property |
| `DialogView.UseCurrentPageLocation` | `Dialog.SetLayoutArea` with `DialogLayoutArea.CurrentPage` or `Window` | Map `true` to `DialogLayoutArea.CurrentPage` and `false` to `DialogLayoutArea.Window`; with nothing attached, the new default is `VisibleArea` |
| `DialogView.UseCurrentPageLocationProperty` | `Dialog.LayoutAreaProperty` with `DialogLayoutArea.CurrentPage` or `Window` | Replace the old bool `BindableProperty`; map its values by the same rule as `DialogView.UseCurrentPageLocation` |
| `DialogView.DialogNotifierProperty` | No direct counterpart | Remove the View-bound `BindableProperty` and use the factory notifier or `viewModel.Notifier` |
| `DialogView.SetUp()` | No direct counterpart | Initialize each fresh View in its factory or configure the ViewModel before presentation |
| `DialogView.TearDown()` | No direct counterpart | Remove reusable-view reset logic and use ordinary resource cleanup |

Passing a `DialogPlacement` to `ShowAsync` replaces the whole attached placement object — `HorizontalAlignment`, `VerticalAlignment`, `OffsetX`, and `OffsetY` together — so an attached `Dialog.SetOffsetY` is not merged into a placement argument. The static attributes in the table above have no show argument and are supplied by attachment only.

`DialogLayoutArea.CurrentPage` uses the current page of the window that presents the dialog, inside that page's own navigation bar and tab bar (intersected with the visible area). Unlike the old `true`, it applies to both axes, and a standard page structure needs no registration: the page is found through a modally pushed page and the visible child of `Shell`, `FlyoutPage`, `TabbedPage`, and `NavigationPage`. When no page can be obtained (including a page not drawn yet), the show does not fail and displays with the same result as `VisibleArea`.

When the app switches screens its own way so that this search cannot reach the page, or when part of a page should be the reference, register once a function returning that page or element in `DialogCurrentPage.Provider`. The registration takes precedence over the default search; when the function returns `null`, throws, returns an element not drawn yet, or returns an element not on the window that presents the dialog, the default search is used. The function is called on the UI thread at the start of each display and when the window size changes during display, and a replaced registration applies from the next display. Assign `null` to return to the default search:

```csharp
Dialog.SetLayoutArea(content, DialogLayoutArea.CurrentPage);

DialogCurrentPage.Provider = () => mainPage.ContentArea;
```

## Move LoadingView behavior

| Old member | New counterpart or status | Migration approach |
|---|---|---|
| `LoadingView` | Ordinary MAUI `View` returned by a Loading factory | Put custom Loading state on an `ILoadingViewModel` |
| `LoadingView.Progress` | `ILoadingProgressReceiver.OnProgress(double)` | Put progress state on the custom Loading ViewModel |
| `LoadingView.ProgressProperty` | No direct counterpart | Remove the View-owned `BindableProperty`; receive progress through `ILoadingProgressReceiver.OnProgress(double)` and expose bindable ViewModel state |
| `LoadingView.OverlayColor` | `Dialog.SetOverlayColor` | Attach it to the custom content View; built-in content uses `Loading.Instance.Options` |
| `LoadingView.OverlayColorProperty` | `Dialog.OverlayColorProperty` | Replace direct `BindableProperty` references on custom content; built-in content uses `Loading.Instance.Options` |

## Replace the obsolete Toast surface

The old Toast type is marked obsolete and only supplies custom `ToastView` routes in the inspected public API. KsDialogs.Maui also has a new built-in message route. It is a new alternative, not a counterpart of an old message overload.

| Old member | New counterpart or status | Migration approach |
|---|---|---|
| `ToastView` | Ordinary MAUI `View` returned by a Toast factory | Implement `IToastViewModel` on the state carrier and register the View with `RegisterForToast` or `Toast.Instance.Registry.Register<TViewModel>` |
| `IToast.Show<TView>(object viewModel = null)` | No direct counterpart | Remove generic View construction; register the ViewModel with `RegisterForToast` and show it by instance (`Toast.Instance.Show(viewModel, durationMs, placement)`) or by type (`Toast.Instance.Show<TViewModel>(configure, durationMs, placement)`) |
| Concrete `Toast.Show(ToastView view, object viewModel = null)` route (DEBUG builds only) | No direct counterpart | Remove the old View instance route; use the registered route or the inline `Show<TViewModel>(viewModel, factory, durationMs, placement)` |
| No old message overload; new alternative | `Toast.Instance.Show(message, durationMs, placement)` | The new message route has no duration upper clamp, concurrent calls overlap instead of queueing, and the display is non-interactive so touches pass through to the page |
| `ToastView.Duration` | `durationMs` or `ToastStyle.DefaultDuration` | Set duration on the new show call or app-wide style |
| `ToastView.DurationProperty` | No direct counterpart | Remove the View-owned `BindableProperty`; pass `durationMs` per show or set `ToastStyle.DefaultDuration` app-wide |

`Show` returns `void` and there is no way to await, update, or close a toast — it disappears when its duration elapses. A `durationMs` of zero or less is treated as unset and falls back to `ToastStyle.DefaultDuration` (`ToastStyle.BuiltinDefaultDuration`, 1500 ms). `ToastStyle` also carries `BackgroundColor`, `TextColor`, `FontSize`, and `CornerRadius` for the built-in message view, and `DefaultPlacement` as the app-wide placement for both built-in and custom content. `BackgroundColor` starts from `ToastStyle.BuiltinBackgroundColor`, the default translucent dark grey of the built-in message Toast, which stands to `BackgroundColor` as `ToastStyle.BuiltinDefaultDuration` stands to `DefaultDuration`. A show with an unregistered ViewModel type fails with `DialogException.ViewFactoryNotRegistered`.

The built-in message view carries a margin of 24 on every side on its own content, and there is no way to change it. A custom view's margin defaults to 0 on every side, as for dialogs, and changes through an attached `Dialog.SetDialogMargin`.

A `Show` made while no screen is available does not fail either; it waits for a screen and then displays. Where the duration count starts depends on the state when the toast is accepted. When a host is available, or when the app is in the background, the count starts at acceptance, so a toast whose duration runs out in the background is discarded without being displayed. When the app is in the foreground but no host exists yet (while the first screen is being prepared after launch, or during a system permission dialog, for example), the count starts when the toast is attached to the screen.

Showing by ViewModel type works once a ViewModel factory is wired, by `RegisterForToast` or by `Toast.Instance.Registry.RegisterViewModel<TViewModel>`. `configure` is synchronous only, because `Show` returns immediately. The ViewModel factory and `configure` run when the toast is attached to a host, together with the content View, so neither is called for a display whose duration runs out before a host appears. A missing ViewModel factory throws `DialogException.ViewModelFactoryNotRegistered` at the call, while an exception from the ViewModel factory or from `configure` cannot reach the caller — it discards that one toast with a warning and leaves other and later toasts untouched. A View wired by `RegisterForToast` that the library cannot build takes the same route, and the warning names `DialogException.ViewCreationFailed` as the cause:

```csharp
Toast.Instance.Style = Toast.Instance.Style with { DefaultDuration = 2000 };

Toast.Instance.Show("Saved");
Toast.Instance.Show(new NoticeViewModel("Sync finished"), durationMs: 3000);
Toast.Instance.Show<NoticeViewModel>(viewModel => viewModel.Message = "Sync finished");
```
