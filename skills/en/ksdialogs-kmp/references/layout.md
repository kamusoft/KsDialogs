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

## Receive the safe area in iOS SwiftUI content

On iOS, SwiftUI content for a Dialog, Loading, or Toast is hosted inside the library's container by a `UIHostingController`, and content registered through `Dialog.shared.kmp` goes through the same hosting. The content receives, as the standard SwiftUI safe area, the part of the container that overlaps the system area (the bands outside the window's safe area, such as the status bar and the home indicator), on the axes in the table below only. Whether the content avoids it (the default) or ignores it with `.ignoresSafeArea(.container)` is written in ordinary SwiftUI.

| Reference area | Alignment on that axis | Safe area received on that axis's edges |
|---|---|---|
| `.window` | Edge: `.start` / `.end`, or `.fill` without `proportionalWidth` / `proportionalHeight` | The overlap between the container and the system area |
| `.window` | Center: `.center`, or `.fill` with a proportion | 0 |
| `.visibleArea` (default) / `.currentPage` | Any, even when an offset pushes the container into the system area | 0 |

- Only the system area is passed; the keyboard area is not included
- When the window size or the window's safe area changes while showing, the received value follows the new overlap
- For example, in a portrait 393 × 852 window whose safe area is 59 at the top and 34 at the bottom, content with `.window` and vertical `.start` (horizontal left at `.center`) receives 59 at the top and 0 on the other edges; with `.fill` on both axes it receives 59 at the top and 34 at the bottom

UIKit content (`UIView`) receives the overlap as its safe area through UIKit regardless of the alignment and the reference area. The two kinds of content see different values when centered content under `.window` overlaps the system area, and when an offset pushes `.visibleArea` / `.currentPage` content into it.

### Cover the whole window and keep the text inside the safe area

With `.fill` on both axes under `.window`, write it the way you write a SwiftUI screen: extend only the background to the window edges and keep the body inside the safe area. `WelcomeViewModel` here is a shared `DialogViewModel<Boolean>` with a `title` property.

```swift
Dialog.shared.kmp.register(WelcomeViewModel.self) { viewModel, notifier in
    VStack(spacing: 16) {
        Text(viewModel.title)
        Button("Continue") { notifier.complete(true) }
    }
    .foregroundStyle(.white)
    .frame(maxWidth: .infinity, maxHeight: .infinity)
    .background(Color.black.ignoresSafeArea())
    .ksDialogOptions(DialogOptions(layoutArea: .window))
    .ksDialogPlacement(DialogPlacement(horizontalAlignment: .fill, verticalAlignment: .fill))
}
```

### Read the overlap with `GeometryReader`

The `GeometryProxy.safeAreaInsets` that a `GeometryReader` reads inside the content depends on where the reading view sits. A `Text` sized to its own content and placed inside the safe area reads 0. To read the overlap, read it from a view that fills the container, such as an outermost `GeometryReader`. The position and size you read match where the content is drawn once the layout settles. While the alignment of content that does not fit (described below) is being re-evaluated, one intermediate position that is never drawn on screen may arrive, so use the last value that arrives. The following keeps a bottom gap only when nothing overlaps the bottom edge, such as on a device without a home indicator.

```swift
Dialog.shared.kmp.register(WelcomeViewModel.self) { viewModel, notifier in
    GeometryReader { proxy in
        VStack(spacing: 16) {
            Text(viewModel.title)
            Button("Continue") { notifier.complete(true) }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(.bottom, proxy.safeAreaInsets.bottom > 0 ? 0 : 24)
    }
    .foregroundStyle(.white)
    .background(Color.black.ignoresSafeArea())
    .ksDialogOptions(DialogOptions(layoutArea: .window))
    .ksDialogPlacement(DialogPlacement(horizontalAlignment: .fill, verticalAlignment: .fill))
}
```

### Give edge-attached content enough room

The received safe area does not enlarge the container. The container asks for the content's ideal size, the same as when no safe area is received, so its size and position stay unchanged and edge-attached content avoids the safe area inside the container; the usable size shrinks by the overlap. In the example window, top-attached shrinkable content (content that shrinks to the proposed size) with an ideal height of 100 keeps its container at y 0 with height 100, while the content is placed from y 59 with height 41. Long wrapping text is shrinkable content too, so it likewise loses 59 of usable height and cannot show all of its text. Pick one of these:

| Remedy | Result |
|---|---|
| Keep the default reference area `.visibleArea` | The container sits inside the safe area, the overlap is 0, and the content uses the whole container |
| Apply `.ignoresSafeArea(.container)` to the outermost view of the content | The content fills the container rect; avoiding the system area becomes the content's job |
| Move the container away from the window edge with `dialogMargin` | The overlap, and so the received value, shrinks by the margin |

When the container is placed at a width different from the one it asked for (capped by the reference area minus the margin, or stretched by `.fill`), the height is recomputed for the placed width. When the content's ideal size changes while showing (such as its text), the container follows the new size.

On an axis with neither a proportion nor `.fill` (its size left to the content), when the width of the reference area minus the margin grows while showing, the size is recomputed from the content's ideal size and grows up to the new limit; when it narrows, the size is capped at the new width. It grows on window rotation, on a change of the window size, and when a `.window` attached to SwiftUI content arrives after the first layout so that the reference grows from the visible area to the whole window. SwiftUI and `UIView` content behave the same. For example, content with an ideal width of 600 that was capped at 393 in portrait becomes 600 in landscape and returns to 393 in portrait. When the container is laid out again while that width stays the same (such as a re-layout in a window of the same size), the size is not recomputed.

### Where content that does not fit inside the safe area goes

On an axis attached to one edge under `.window` (`.start` / `.end`, with or without a proportion), content that does not fit inside the safe area within the container is aligned on that axis to the side opposite the edge the container is attached to (the inner edge: the top side when attached to the bottom, the bottom side when attached to the top, the right side when attached to the left, the left side when attached to the right), and it receives no safe area (0) on that axis. Content that does not fit is content that, shrunk as far as it can, still does not fit inside the safe area: a fixed size such as `.frame(width:height:)`, a stack of parts that cannot shrink such as a `VStack` of text and buttons, and content whose minimum size is larger than the inside of the safe area. The container rect stays the same. In the example window it is placed as follows.

| Content | Attached to the top | Attached to the bottom |
|---|---|---|
| Non-shrinking height 100 (with or without `.ignoresSafeArea`) | Fills the container (y 0, height 100) | Fills the container (y 752, height 100) |
| Ideal height 100, minimum height 80 | Shrinks and aligns to the bottom side (y 20, height 80) | Shrinks and aligns to the top side (y 752, height 80) |
| Non-shrinking height 1000 (the container is capped at the window height 852) | Aligns to the bottom side and overflows the window's top edge by 148 (y -148) | Aligns to the top side and overflows the window's bottom edge by 148 (y 0) |

- Left and right are physical directions and stay the same in right-to-left languages
- With `.fill` on both axes, on a centered axis, and with a reference area other than `.window`, the arrangement is not changed and content that does not fit is centered inside the safe area as standard SwiftUI does. UIKit content (`UIView`) is not covered either
- Whether the content fits is re-evaluated while showing. When the container size, the content's ideal or minimum size, or the window's safe area changes, the container and the content settle into positions for the new state

## When an attachment takes effect

- Write the attachment where it is evaluated before the first display
- In Compose, a `KsDialogAttributes` placed inside a lazily evaluated scope such as a `LazyColumn` item does not run during the initial composition and has no effect on that display
- SwiftUI attachments merge from child to parent, so the outer one wins when the same attribute is attached twice
- The effective values are frozen at the first native layout pass. Rewriting an attachment afterwards does not move a Dialog that is already showing
- Rotation and changes to the window size or insets re-run the layout with the frozen values. With the current page chosen, the page rect is fetched again at that point (replacing how the page is named takes effect from the next display)
- A navigation that changes the current page while the Dialog is showing does not move it by itself
- Opening and closing the soft keyboard does not move the Dialog
