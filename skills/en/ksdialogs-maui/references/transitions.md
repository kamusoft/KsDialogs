# Attach a transition preset

Entry and exit animation is bundled into a `DialogTransition` and attached to the content view as an attached property. There is no route that passes one as a show argument.

- A `DialogTransition` carries closures, so it **cannot be written in XAML**. Attach it from code-behind (the attributes in [Layout](layout.md) can be written in XAML; the transition alone cannot)
- Attach it to the content view itself, as in `Dialog.SetTransition(this, DialogTransition.Slide(DialogTransitionEdge.Bottom))`
- Read an attached value with `Dialog.GetTransition(view)`, which returns `null` when nothing is attached
- Use `Dialog.TransitionProperty` when the `BindableProperty` itself is needed

## Choose a preset

| Preset | Signature | Animation |
|---|---|---|
| Fade | `DialogTransition.Fade(duration, easing)` | Enters and leaves by opacity |
| Slide | `DialogTransition.Slide(from, duration, easing)` | Slides in from the `from` edge and out to the same edge |
| Zoom | `DialogTransition.Zoom(duration, easing)` | Grows from slightly shrunk to full scale, and shrinks back to that scale on the way out |
| None | `DialogTransition.None()` | No animation on the content side |

A preset returns a `DialogTransition` with `Presentation`, `Dismissal`, and `OverlayDuration` all filled in.

| Argument | Type | Default | Meaning |
|---|---|---|---|
| `from` | `DialogTransitionEdge` | none (required for `Slide` only) | The edge to slide in from and out to |
| `duration` | `TimeSpan` | 250 ms | The one-way duration |
| `easing` | `Easing` | `Easing.CubicInOut` | How progress advances over time |

| Value passed as `from` | Direction |
|---|---|
| `Top` / `Bottom` | Stays physical |
| `Start` / `End` | Follows the layout direction |

## Defaults and detailed rules

- **With nothing attached** — content and overlay cross-fade for 250 ms
- **`None`** — makes only the content immediate; the overlay still fades
- **Concurrency** — content and overlay run concurrently, and presentation, dismissal, and result delivery all wait for both
- **Invalid `duration`** — zero, negative, or a total in milliseconds beyond the `uint` limit does not throw; it jumps to the final state immediately
- **When the attached value is adopted** — after the first native layout. Changing it on a Dialog that is on screen does not affect the current display

## Declare the view model

Animation is a concern of the content side, so nothing is added to the view model.

```csharp
using System.Windows.Input;
using KsDialogs;
using Microsoft.Maui.Controls;

namespace MyApp;

public sealed class NoticeDialogViewModel : IDialogViewModel
{
    public NoticeDialogViewModel(string message)
    {
        Message = message;
        CloseCommand = new Command(() => this.Notifier?.Complete(true));
    }

    public string Message { get; }

    public ICommand CloseCommand { get; }
}
```

## Write the view in XAML

```xml
<?xml version="1.0" encoding="utf-8" ?>
<ContentView xmlns="http://schemas.microsoft.com/dotnet/2021/maui"
             xmlns:x="http://schemas.microsoft.com/winfx/2009/xaml"
             xmlns:local="clr-namespace:MyApp"
             x:Class="MyApp.NoticeDialogView"
             x:DataType="local:NoticeDialogViewModel">

    <Border WidthRequest="272" Padding="20" StrokeThickness="0">
        <Border.StrokeShape>
            <RoundRectangle CornerRadius="16" />
        </Border.StrokeShape>

        <VerticalStackLayout Spacing="16">
            <Label Text="{Binding Message}" HorizontalTextAlignment="Center" />
            <Button Text="OK" Command="{Binding CloseCommand}" />
        </VerticalStackLayout>
    </Border>

</ContentView>
```

## Attach the transition from code-behind

Attaching it while the content view is constructed puts it before the first native layout pass, so it always takes effect.

```csharp
using System;
using KsDialogs;
using Microsoft.Maui.Controls;

namespace MyApp;

public partial class NoticeDialogView : ContentView
{
    public NoticeDialogView()
    {
        InitializeComponent();
        Dialog.SetTransition(
            this,
            DialogTransition.Slide(DialogTransitionEdge.Bottom, TimeSpan.FromMilliseconds(300)));
    }
}
```

To vary the animation per show, call `Dialog.SetTransition` inside the view factory.

## Register and call

Registration and the call are the same as for a Dialog with no transition, and the attached animation simply runs on the presentation and dismissal of `ShowAsync` ([Dialog](dialogs.md)).

```csharp
Dialog.Instance.Registry.Register<NoticeDialogViewModel>(
    viewModel => new NoticeDialogView { BindingContext = viewModel });
```

```csharp
private async void OnSavedClicked(object? sender, EventArgs e)
{
    var result = await _dialogs.ShowAsync(new NoticeDialogViewModel("Saved"));
    StatusLabel.Text = result is DialogResult<bool>.Completed ? "Closed" : "Dismissed";
}
```

## Supply custom asynchronous hooks

The constructor is `DialogTransition(presentation, dismissal, overlayDuration)` and all three arguments are optional.

- An omitted hook leaves that direction on the library default
- Both hooks are `Func<VisualElement, Task>` and receive the MAUI view hosting the laid-out content, started on the UI thread
- What a transition holds is readable from `DialogTransition.Presentation`, `DialogTransition.Dismissal`, and `DialogTransition.OverlayDuration`
- A hook fault or cancellation is absorbed into the log and does not propagate to the Dialog result
- The library applies no timeout and waits for hook completion, so a task that never completes stops container removal and result delivery

Attachment works exactly as for a preset: pass it to `Dialog.SetTransition` from the code-behind of the content view.

```csharp
using System;
using KsDialogs;
using Microsoft.Maui.Controls;

namespace MyApp;

public partial class NoticeDialogView : ContentView
{
    public NoticeDialogView()
    {
        InitializeComponent();
        Dialog.SetTransition(
            this,
            new DialogTransition(
                presentation: async view =>
                {
                    view.Opacity = 0;
                    await view.FadeToAsync(1, 180, Easing.CubicOut);
                },
                dismissal: view => view.FadeToAsync(0, 140, Easing.CubicIn),
                overlayDuration: TimeSpan.FromMilliseconds(180)));
    }
}
```

With this in place, `presentation` runs when `ShowAsync(new NoticeDialogViewModel("Saved"))` presents, `dismissal` runs on close, and the overlay fades over 180 ms. Neither the registration nor the caller changes from the preset case.

## Mix a preset with a custom hook

Take one hook out of a preset and pass it to the constructor alongside your own. The value you assemble is attached exactly as a preset is.

```csharp
using System;
using KsDialogs;
using Microsoft.Maui.Controls;

namespace MyApp;

public partial class NoticeDialogView : ContentView
{
    public NoticeDialogView()
    {
        InitializeComponent();
        var zoomIn = DialogTransition.Zoom(TimeSpan.FromMilliseconds(200));
        Dialog.SetTransition(
            this,
            new DialogTransition(
                presentation: zoomIn.Presentation,
                dismissal: view => view.FadeToAsync(0, 140, Easing.CubicIn),
                overlayDuration: TimeSpan.FromMilliseconds(140)));
    }
}
```

To match only `OverlayDuration` to the preset, pass `zoomIn.OverlayDuration` the same way; to take only the dismissal from the preset, pass `zoomIn.Dismissal`.
