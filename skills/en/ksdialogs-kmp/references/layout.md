# Decide the placement and size

Two kinds of attributes decide how the container looks. Shared code can pass the placement only; everything else is attached to the content by the host.

| Attribute | What it decides | Where it is supplied from |
|---|---|---|
| Placement (`DialogPlacement`) | Alignment and offset | The `show` argument in shared code, or an attachment on the host content |
| Static options (`DialogOptions`) | Reference area, margin, proportion, overlay, outside tap | Attachment on the host content only |

Loading and Toast use the same attributes. Loading takes the same options but always blocks outside taps, and Toast takes the placement only.

## Pass a placement from shared code

Passing a `DialogPlacement` to `show` decides the placement for that call alone. The value **replaces the whole placement object** attached to the content; fields are never merged. On a Loading or Toast call that passes only a message, it replaces the app-default placement the host decided.

| Property | Type | Default | What it decides |
|---|---|---|---|
| `horizontalAlignment` | `DialogAlignment` | `CENTER` | Alignment on the horizontal axis |
| `verticalAlignment` | `DialogAlignment` | `CENTER` | Alignment on the vertical axis |
| `offsetX` | `Double` | `0.0` | Horizontal shift applied after the alignment; a positive value moves right |
| `offsetY` | `Double` | `0.0` | Vertical shift applied after the alignment; a positive value moves down |

- `DialogAlignment` is one of `START` / `CENTER` / `END` / `FILL`. `START` and `END` are physical directions (left and right on the horizontal axis, top and bottom on the vertical one) and do not follow the writing direction
- `FILL` stretches the size to the reference area as well as the position. When a proportion is specified on that axis, it is not adopted as the way to decide the size, and the position is treated as `CENTER`
- The offset is added last and is not clamped back into the screen; you can deliberately push the container off screen
- Lengths are logical units — pt on iOS, dp on Android. Non-finite values are rounded to 0

The following example adds a placement to the caller from [Dialog](dialogs.md) so that the delete confirmation Dialog sits at the bottom of the screen. The `DeleteViewModel` declaration and its host registration are in that same document.

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.Dialog
import jp.kamusoft.ksdialogs.kmp.DialogAlignment
import jp.kamusoft.ksdialogs.kmp.DialogException
import jp.kamusoft.ksdialogs.kmp.DialogPlacement
import jp.kamusoft.ksdialogs.kmp.DialogResult
import jp.kamusoft.ksdialogs.kmp.KsDialog
import kotlin.coroutines.cancellation.CancellationException

class ItemListViewModel(
    private val repository: ItemRepository,
    private val dialogs: KsDialog = Dialog.instance,
) {
    @Throws(DialogException::class, CancellationException::class)
    suspend fun deleteItem(itemName: String) {
        val result = dialogs.show(
            viewModel = DeleteViewModel(itemName),
            placement = DialogPlacement(
                horizontalAlignment = DialogAlignment.FILL,
                verticalAlignment = DialogAlignment.END,
                offsetY = -24.0,
            ),
        )
        if (result is DialogResult.Completed && result.value) repository.delete(itemName)
    }
}
```

## Attach static options in the host

Everything from the reference area to the outside tap is a property of the content, so commonMain exposes no `DialogOptions`. The host attaches it to the content it builds.

| Property | Default | What it does |
|---|---|---|
| `layoutArea` | visible area | Picks the reference rect: the whole window, the visible area (the window minus the system insets), or the current page. It applies to both axes |
| `dialogMargin` | 0 on every edge | Inset from the reference rect; caps the size and also pushes start- and end-aligned content away from the edge. Negative and non-finite edges become 0 |
| `proportionalWidth` / `proportionalHeight` | unspecified (`-1`) | Fraction of the reference rect on that axis; `0` or less means unspecified, above `1` is clamped to `1` |
| `overlayColor` | black at 40% | Colour of the layer behind the Dialog |
| `isCanceledOnTouchOutside` | `true` | Whether a tap outside cancels the Dialog; when off, the tap does not reach the screen behind either |

The margin defaults to 0 on every edge, so an aligned container touches the edge of the reference rect. Attach a `dialogMargin` to keep it away from the edge. Only the built-in Toast carries a 24 margin on every edge of its own content ([Toast](toast.md)).

The spellings differ per host.

| Type | Android | iOS |
|---|---|---|
| Reference area | `DialogLayoutArea.WINDOW` / `DialogLayoutArea.VISIBLE_AREA` / `DialogLayoutArea.CURRENT_PAGE` | `DialogLayoutArea.window` / `DialogLayoutArea.visibleArea` / `DialogLayoutArea.currentPage` |
| Alignment | `DialogAlignment.START` / `CENTER` / `END` / `FILL` | `DialogAlignment.start` / `.center` / `.end` / `.fill` |
| Edge insets | `DialogEdgeInsets(top, left, bottom, right)`, `DialogEdgeInsets(all)`, `DialogEdgeInsets.ZERO` | `DialogEdgeInsets(top:left:bottom:right:)`, `DialogEdgeInsets(all:)`, `DialogEdgeInsets.zero` |
| Overlay colour | ARGB 32-bit `Int` | `UIColor` |
| Attachment on classic views | `ksDialogOptions` / `ksDialogPlacement` extension properties | `ksDialogOptions` / `ksDialogPlacement` extension properties |
| Attachment on declarative UI | `KsDialogAttributes(options = …, placement = …)` | `.ksDialogOptions(…)` / `.ksDialogPlacement(…)` modifiers |

How to write the registration itself is in [Android host](android-host.md) and [iOS host](ios-host.md).

### Android host

On classic views, set it on the content's extension properties.

```kotlin
val content = DeleteContentView(this, viewModel, notifier)
content.ksDialogOptions = DialogOptions(
    layoutArea = DialogLayoutArea.WINDOW,
    dialogMargin = DialogEdgeInsets(all = 16.0),
    proportionalWidth = 0.9,
    proportionalHeight = 0.6,
    overlayColor = 0x66000000,
    isCanceledOnTouchOutside = false,
)
content.ksDialogPlacement = DialogPlacement(verticalAlignment = DialogAlignment.END, offsetY = -24.0)
```

In Compose content, declare `KsDialogAttributes` at the top of the composable.

```kotlin
Dialog.instance.registry.registerCompose(DeleteViewModel::class) { viewModel, notifier ->
    KsDialogAttributes(
        options = DialogOptions(layoutArea = DialogLayoutArea.VISIBLE_AREA, proportionalWidth = 0.8),
        placement = DialogPlacement(verticalAlignment = DialogAlignment.END),
    )
    DeleteContent(viewModel.itemName, notifier)
}
```

### iOS host

In UIKit content, set it on the view's extension properties.

```swift
let content = DeleteContentView(itemName: viewModel.itemName, notifier: notifier)
content.ksDialogOptions = DialogOptions(
    layoutArea: .window,
    dialogMargin: DialogEdgeInsets(all: 16),
    proportionalWidth: 0.9,
    proportionalHeight: 0.6,
    overlayColor: UIColor.black.withAlphaComponent(0.6),
    isCanceledOnTouchOutside: false
)
content.ksDialogPlacement = DialogPlacement(verticalAlignment: .end, offsetY: -24)
```

In SwiftUI content, attach it with modifiers.

```swift
Dialog.shared.kmp.register(DeleteViewModel.self) { viewModel, notifier in
    DeleteContent(itemName: viewModel.itemName, notifier: notifier)
        .ksDialogOptions(DialogOptions(layoutArea: .visibleArea, proportionalWidth: 0.8))
        .ksDialogPlacement(DialogPlacement(verticalAlignment: .end))
}
```

## Use the current page as the reference

Choosing the current page as the reference area makes the reference rect the inside of the page on screen, without its navigation bar or tab bar (intersected with the visible area). On a screen with a tab bar, an end-aligned Dialog sits `dialogMargin` above the top of the tab bar, and proportional sizes are fractions of the height without the tab bar.

Shared code has no way to choose the reference area and no way to name the page. The host attaches the choice to the content, and each host tells the library which view is the page.

| Host | How to choose it | How to name the page |
|---|---|---|
| Android | `DialogLayoutArea.CURRENT_PAGE` | `Modifier.markAsDialogCurrentPage()` in Compose, `DialogCurrentPage.provider` for classic views. Without either, no page is found |
| iOS | `DialogLayoutArea.currentPage` | `markAsDialogCurrentPage()` in SwiftUI, `DialogCurrentPage.provider` for UIKit. Even with neither, the library walks UIKit navigation and tab containers by default |

When no page is found, the display still does not fail; it comes out as if the visible area had been chosen (the reason goes to an English warning log).

On Android, the attachment only changes the choice.

```kotlin
Dialog.instance.registry.register(DeleteViewModel::class) { viewModel, notifier ->
    DeleteContentView(this, viewModel, notifier).apply {
        ksDialogOptions = DialogOptions(
            layoutArea = DialogLayoutArea.CURRENT_PAGE,
            dialogMargin = DialogEdgeInsets(all = 16.0),
        )
        ksDialogPlacement = DialogPlacement(verticalAlignment = DialogAlignment.END)
    }
}
```

The iOS attachment is the same.

```swift
Dialog.shared.kmp.register(DeleteViewModel.self) { viewModel, notifier in
    DeleteContent(itemName: viewModel.itemName, notifier: notifier)
        .ksDialogOptions(DialogOptions(layoutArea: .currentPage, dialogMargin: DialogEdgeInsets(all: 16)))
        .ksDialogPlacement(DialogPlacement(verticalAlignment: .end))
}
```

Complete recipes for naming the page are in [Android host](android-host.md) and [iOS host](ios-host.md). Loading and Toast content accept the same value and resolve the page by the same rules as a Dialog, but nothing beyond that is settled (such as how it fits with the Toast app-default placement).

## When an attachment takes effect

- Write the attachment where it is evaluated before the first display
- In Compose, a `KsDialogAttributes` placed inside a lazily evaluated scope such as a `LazyColumn` item does not run during the initial composition and has no effect on that display
- SwiftUI attachments merge from child to parent, so the outer one wins when the same attribute is attached twice
- The effective values are frozen at the first native layout pass. Rewriting an attachment afterwards does not move a Dialog that is already showing
- Rotation and changes to the window size or insets re-run the layout with the frozen values. With the current page chosen, the page rect is fetched again at that point (replacing how the page is named takes effect from the next display)
- A navigation that changes the current page while the Dialog is showing does not move it by itself
- Opening and closing the soft keyboard does not move the Dialog
