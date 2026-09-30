# Attach layout behavior to content

Size, position, overlay, and outside-tap behavior are specified by attaching them to the content view. Attachment uses the static members of the `Dialog` class. Anything not attached falls back to the library default.

## What can be attached

| Attached property | Type | Supplies | Default |
|---|---|---|---|
| `Dialog.LayoutArea` | `DialogLayoutArea` (`Window` / `VisibleArea` / `CurrentPage`) | Reference area for sizing and positioning | `VisibleArea` |
| `Dialog.DialogMargin` | `Thickness` | Inset deducted from each edge of the reference area | 0 on every edge |
| `Dialog.ProportionalWidth` / `Dialog.ProportionalHeight` | `double` | Fraction of the reference area on that axis | `-1` (unspecified) |
| `Dialog.OverlayColor` | `Color?` | Color painted behind the Dialog | 40% black |
| `Dialog.IsCanceledOnTouchOutside` | `bool` | Whether a tap outside cancels | `true` |
| `Dialog.HorizontalAlignment` / `Dialog.VerticalAlignment` | `DialogAlignment` (`Start` / `Center` / `End` / `Fill`) | Alignment on that axis | `Center` |
| `Dialog.OffsetX` / `Dialog.OffsetY` | `double` | Translation applied after alignment | `0` |

`Start` and `End` in `DialogAlignment` are physical directions (left and right on the horizontal axis, top and bottom on the vertical one) and do not follow the writing direction.

Each name `X` in that table has a `Dialog.GetX` / `Dialog.SetX` pair plus a `BindableProperty` named `Dialog.XProperty`, for example `Dialog.GetLayoutArea`, `Dialog.SetLayoutArea`, and `Dialog.LayoutAreaProperty`.

The three `DialogLayoutArea` values measure against the following rectangles. Whichever is chosen applies to both the horizontal and the vertical axis.

| Value | Reference rectangle |
|---|---|
| `Window` | The whole window the Dialog is placed on |
| `VisibleArea` | The window minus the space taken by system bars and similar (the insets). The safe area on iOS |
| `CurrentPage` | The intersection of the visible area with the page currently shown, inside its tab bar and navigation bar (see "Measure against the current page" below) |

The reference area is where AiForms.Maui.Dialogs had the boolean `UseCurrentPageLocation` property on the view. Here it is an enumeration covering both axes, written as `ksd:Dialog.LayoutArea` in XAML or `Dialog.SetLayoutArea` from code. When migrating, replace `true` with `DialogLayoutArea.CurrentPage` and `false` with `DialogLayoutArea.Window`. With nothing specified the area is `VisibleArea`, so a screen that relied on `false` moves from the whole window to the visible area unless it is migrated.

## Sizing and positioning rules

| Item | Rule |
|---|---|
| Valid proportional range | `0` or less means "unspecified"; a value above `1` is clamped to `1` |
| Precedence per axis | On one axis a proportional size wins over `DialogAlignment.Fill`, and `Fill` then acts as centering |
| Clamping | The resulting size is clamped to fit the area left after `DialogMargin` |
| Offset | Not clamped, so content can be pushed off screen deliberately |
| Where it is computed | MAUI passes the attached values through unchanged and the rect is computed natively |
| Content size | Decided by MAUI measurement, including `WidthRequest` / `HeightRequest` (and `MinimumWidthRequest` / `MinimumHeightRequest`) on the content's root. A `ContentView` root and a `Grid` root come out the same size on iOS and Android. This holds for Dialog, Loading, and Toast custom views alike |
| Explicit size with proportional or `Fill` | On an axis where the container decides the size through a proportion or `Fill`, a root with an explicit size does not stretch to the frame; it keeps its declared size and is centered in the frame. To fill the frame, drop the explicit size on that axis |

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

## Measure against the current page

Attaching `CurrentPage` to `Dialog.LayoutArea` (`ksd:Dialog.LayoutArea="CurrentPage"` in XAML) measures against the page currently shown in the window the Dialog appears on. On a screen with a tab bar, setting `VerticalAlignment` to `End` puts the Dialog's bottom edge `DialogMargin` above the top of the tab bar, and a proportional size becomes a fraction of the height without the tab bar.

```xml
<?xml version="1.0" encoding="utf-8" ?>
<ContentView xmlns="http://schemas.microsoft.com/dotnet/2021/maui"
             xmlns:x="http://schemas.microsoft.com/winfx/2009/xaml"
             xmlns:ksd="clr-namespace:KsDialogs;assembly=KsDialogs.Maui"
             x:Class="MyApp.PageSheetView"
             ksd:Dialog.LayoutArea="CurrentPage"
             ksd:Dialog.ProportionalWidth="1"
             ksd:Dialog.VerticalAlignment="End">
    <Label Text="Filter" />
</ContentView>
```

The library finds the page from the MAUI page structure, so a standard structure needs no registration. The lookup is as follows and is the same on iOS and Android.

1. Start from the last page pushed on the window's modal stack, or from `Window.Page` when the modal stack is empty
2. Descend through `Shell`, `FlyoutPage`, `TabbedPage`, and `NavigationPage` into the child being shown (`Detail` for `FlyoutPage`) until the page is no longer a container
3. Measure against the page reached. A page that has not been drawn yet counts as not found

When no page is found, the Dialog appears exactly as it would with `VisibleArea`, and the show does not fail.

### Tell the library which page or element to use

When screens are switched by your own mechanism that the lookup above cannot reach, or when part of a page should be the reference, register a function returning that page or element in `DialogCurrentPage.Provider` (`Func<VisualElement?>?`). A registration takes precedence over the built-in lookup, and assigning `null` returns to the built-in lookup.

- The function is called on the UI thread when each presentation starts, and again when the window size or the system bar widths change while the Dialog is shown
- Replacing the registration takes effect from the next presentation; a Dialog already shown keeps the function captured when it started
- When the function returns `null`, throws, returns an element that has not been drawn, one not placed on the window the Dialog appears on, or one that does not overlap that window, the page found by the built-in lookup is used instead

The following screen measures against its body, excluding the header.

```csharp
using KsDialogs;
using Microsoft.Maui.Controls;

namespace MyApp;

public sealed class DashboardPage : ContentPage
{
    private readonly Grid _contentArea = new();

    public DashboardPage()
    {
        var header = new Label { Text = "Dashboard" };
        Grid.SetRow(_contentArea, 1);
        Content = new Grid
        {
            RowDefinitions =
            {
                new RowDefinition(GridLength.Auto),
                new RowDefinition(GridLength.Star),
            },
            Children = { header, _contentArea },
        };
    }

    protected override void OnAppearing()
    {
        base.OnAppearing();
        DialogCurrentPage.Provider = () => _contentArea;
    }

    protected override void OnDisappearing()
    {
        base.OnDisappearing();
        DialogCurrentPage.Provider = null;
    }
}
```

Navigation that changes the current page while a Dialog is shown does not by itself re-place the Dialog (re-placement is triggered only by changes to the window size and the insets). Loading and Toast custom views can also carry `CurrentPage` and resolve the page by the same rules as a Dialog, but nothing further is settled, such as how it combines with the Toast default placement.

## Bundle the values into value objects

| Type | Fields it bundles | Supply route |
|---|---|---|
| `DialogPlacement` | `HorizontalAlignment`, `VerticalAlignment`, `OffsetX`, `OffsetY` | Per-field attachment, or the `placement` argument of `ShowAsync` |
| `DialogOptions` | `LayoutArea`, `DialogMargin`, `ProportionalWidth`, `ProportionalHeight`, `OverlayColor`, `IsCanceledOnTouchOutside` | Per-field attachment |

Both are records, so `with` replaces individual fields. Built-in Loading content has no view to attach to, and only there does `Loading.Instance.Options` take a `DialogOptions` directly ([Loading](loading.md)).
