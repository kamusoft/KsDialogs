# Attach layout behavior to content

Size, position, overlay, and outside-tap behavior are specified by attaching them to the content view. Attachment uses the static members of the `Dialog` class. Anything not attached falls back to the library default.

## What can be attached

| Attached property | Type | Supplies | Default |
|---|---|---|---|
| `Dialog.LayoutArea` | `DialogLayoutArea` (`Window` / `VisibleArea`) | Reference area for sizing and positioning | `VisibleArea` |
| `Dialog.DialogMargin` | `Thickness` | Inset deducted from each edge of the reference area | 24 on every edge |
| `Dialog.ProportionalWidth` / `Dialog.ProportionalHeight` | `double` | Fraction of the reference area on that axis | `-1` (unspecified) |
| `Dialog.OverlayColor` | `Color?` | Color painted behind the Dialog | 40% black |
| `Dialog.IsCanceledOnTouchOutside` | `bool` | Whether a tap outside cancels | `true` |
| `Dialog.HorizontalAlignment` / `Dialog.VerticalAlignment` | `DialogAlignment` (`Start` / `Center` / `End` / `Fill`) | Alignment on that axis | `Center` |
| `Dialog.OffsetX` / `Dialog.OffsetY` | `double` | Translation applied after alignment | `0` |

`Start` and `End` in `DialogAlignment` are physical directions (left and right on the horizontal axis, top and bottom on the vertical one) and do not follow the writing direction.

Each name `X` in that table has a `Dialog.GetX` / `Dialog.SetX` pair plus a `BindableProperty` named `Dialog.XProperty`, for example `Dialog.GetLayoutArea`, `Dialog.SetLayoutArea`, and `Dialog.LayoutAreaProperty`.

The reference area is where AiForms.Maui.Dialogs had the boolean `UseCurrentPageLocation` property on the view. Here it is an enumeration covering both axes, written as `ksd:Dialog.LayoutArea` in XAML or `Dialog.SetLayoutArea` from code.

## Sizing and positioning rules

| Item | Rule |
|---|---|
| Valid proportional range | `0` or less means "unspecified"; a value above `1` is clamped to `1` |
| Precedence per axis | On one axis a proportional size wins over `DialogAlignment.Fill`, and `Fill` then acts as centering |
| Clamping | The resulting size is clamped to fit the area left after `DialogMargin` |
| Offset | Not clamped, so content can be pushed off screen deliberately |
| Where it is computed | MAUI passes the attached values through unchanged and the rect is computed natively |

## When the effective values are decided

| Event | Result |
|---|---|
| The first native layout pass completes | The values attached at that moment become the effective ones |
| An attached value changes while shown | No effect |
| The window size or the system insets change | The effective values stay and the content is re-placed |
| The soft keyboard opens or closes | It does not move |

## Attach in XAML

Declare the library namespace and write the attached properties on the content view. Layout attributes are the part of the surface that XAML can express; a transition value holds closures and must be attached from code-behind ([Transitions](transitions.md)).

The following example collects the attachments that show a `ContentView` at 90% width, bottom-aligned, against the whole window.

```xml
<?xml version="1.0" encoding="utf-8" ?>
<ContentView xmlns="http://schemas.microsoft.com/dotnet/2021/maui"
             xmlns:x="http://schemas.microsoft.com/winfx/2009/xaml"
             xmlns:ksd="clr-namespace:KsDialogs;assembly=KsDialogs.Maui"
             x:Class="MyApp.ConfirmContentView"
             ksd:Dialog.LayoutArea="Window"
             ksd:Dialog.DialogMargin="16"
             ksd:Dialog.ProportionalWidth="0.9"
             ksd:Dialog.OverlayColor="#88000000"
             ksd:Dialog.IsCanceledOnTouchOutside="False"
             ksd:Dialog.HorizontalAlignment="Center"
             ksd:Dialog.VerticalAlignment="End"
             ksd:Dialog.OffsetY="-24">
    <Label Text="Delete this item?" />
</ContentView>
```

## Attach from code-behind

Set the same values with the `Dialog.SetX` methods when the content is built in code. Attaching them while the content view is constructed puts them before the first native layout pass, so they always take effect.

The following example performs the same attachments as the XAML above, in the constructor of a content view built in code.

```csharp
using System.Windows.Input;
using KsDialogs;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Graphics;

namespace MyApp;

public sealed class ConfirmCardView : ContentView
{
    public ConfirmCardView(string message, ICommand closeCommand)
    {
        Content = new VerticalStackLayout
        {
            Spacing = 16,
            Children =
            {
                new Label { Text = message },
                new Button { Text = "OK", Command = closeCommand },
            },
        };

        Dialog.SetLayoutArea(this, DialogLayoutArea.Window);
        Dialog.SetDialogMargin(this, new Thickness(16));
        Dialog.SetProportionalWidth(this, 0.9);
        Dialog.SetOverlayColor(this, Color.FromArgb("#88000000"));
        Dialog.SetIsCanceledOnTouchOutside(this, false);
        Dialog.SetHorizontalAlignment(this, DialogAlignment.Center);
        Dialog.SetVerticalAlignment(this, DialogAlignment.End);
        Dialog.SetOffsetY(this, -24);
    }
}
```

To vary the values per show, call `Dialog.SetX` inside the registered view factory.

## Pass the placement as a show argument

The four placement fields are also bundled by `DialogPlacement`. Passing one to the optional trailing `placement` argument of `ShowAsync` replaces the placement attached to the content as a whole.

- The replacement covers all four fields at once. An attached `OffsetY` stops being used the moment a `placement` that does not repeat it is passed
- Without a `placement`, the attached values are used, and without attachments, the defaults
- There is no show argument for the static options such as `OverlayColor` or `IsCanceledOnTouchOutside`

The following example shows the same content at a different position per call. The view-model declaration and its registration are as in [Dialog](dialogs.md), and `_dialogs` is an injected `IKsDialog`.

```csharp
private async void OnDeleteClicked(object? sender, EventArgs e)
{
    var placement = new DialogPlacement
    {
        HorizontalAlignment = DialogAlignment.Center,
        VerticalAlignment = DialogAlignment.Start,
        OffsetY = 16,
    };

    var result = await _dialogs.ShowAsync(new ConfirmDialogViewModel("Delete this item?"), placement);
    StatusLabel.Text = result is DialogResult<bool>.Completed ? "Deleted" : "Kept";
}
```

## Bundle the values into value objects

| Type | Fields it bundles | Supply route |
|---|---|---|
| `DialogPlacement` | `HorizontalAlignment`, `VerticalAlignment`, `OffsetX`, `OffsetY` | Per-field attachment, or the `placement` argument of `ShowAsync` |
| `DialogOptions` | `LayoutArea`, `DialogMargin`, `ProportionalWidth`, `ProportionalHeight`, `OverlayColor`, `IsCanceledOnTouchOutside` | Per-field attachment |

Both are records, so `with` replaces individual fields. Built-in Loading content has no view to attach to, and only there does `Loading.Instance.Options` take a `DialogOptions` directly ([Loading](loading.md)).
