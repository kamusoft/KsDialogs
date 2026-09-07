# Carry a transition choice to the hosts

The enter and exit transition is attached to the content, and the content is built in the host. That is why shared code carries no animation type.

- There is no route that passes a transition as a `show` argument
- All shared code can do is put the choice of which transition to use on the view model and carry it
- Each host maps that choice to a native `DialogTransition` and attaches it to the content

## Put the choice on the shared view model

The following declares the choices as an enum and shows a view model that carries one. The transition itself never appears in shared code.

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.Dialog
import jp.kamusoft.ksdialogs.kmp.DialogException
import jp.kamusoft.ksdialogs.kmp.DialogResult
import jp.kamusoft.ksdialogs.kmp.DialogViewModel
import jp.kamusoft.ksdialogs.kmp.KsDialog
import kotlin.coroutines.cancellation.CancellationException

enum class TransitionChoice {
    FADE,
    SLIDE_UP,
    ZOOM,
    NONE,
}

class NoticeViewModel(
    val message: String,
    val transition: TransitionChoice,
) : DialogViewModel<Boolean>

class ItemEditor(private val dialogs: KsDialog = Dialog.instance) {
    @Throws(DialogException::class, CancellationException::class)
    suspend fun notifySaved(): Boolean =
        dialogs.show(NoticeViewModel("Saved", TransitionChoice.SLIDE_UP)) is DialogResult.Completed
}
```

## Choose a preset

A preset fills `presentation`, `dismissal`, and `overlayDuration` at once, so the enter and the exit stay symmetric.

| Preset | Transition | Android | iOS |
|---|---|---|---|
| Fade | Enters and exits by opacity | `DialogTransition.fade(duration, easing)` | `DialogTransition.fade(duration:easing:)` |
| Slide | Slides in from the given edge and out to the same edge | `DialogTransition.slide(from, duration, easing)` | `DialogTransition.slide(from:duration:easing:)` |
| Zoom | Grows from 0.8x to 1x, then shrinks back to that scale as it disappears | `DialogTransition.zoom(duration, easing)` | `DialogTransition.zoom(duration:easing:)` |
| None | No animation for the content; the overlay still fades | `DialogTransition.none()` | `DialogTransition.none()` |

The arguments mean the same thing in every preset; only `from` is specific to slide.

| Argument | What it decides | Android | iOS |
|---|---|---|---|
| `from` | The edge to slide in from and out to (required for slide only) | `DialogTransitionEdge` | `DialogTransitionEdge` |
| `duration` | The one-way time | `kotlin.time.Duration`, default 250 ms | `TimeInterval` in seconds, default `0.25` |
| `easing` | How the progress advances over time | `Interpolator`, default `AccelerateDecelerateInterpolator` | `any UITimingCurveProvider`, default `.standard` |

There are four edges, and only the two that correspond to left and right follow the layout direction.

| Edge | Android | iOS | Direction |
|---|---|---|---|
| Top | `DialogTransitionEdge.TOP` | `.top` | Stays physical and never changes |
| Bottom | `DialogTransitionEdge.BOTTOM` | `.bottom` | Stays physical and never changes |
| Start of the line | `DialogTransitionEdge.START` | `.leading` | Follows the layout direction and swaps in a right-to-left environment |
| End of the line | `DialogTransitionEdge.END` | `.trailing` | Follows the layout direction and swaps in a right-to-left environment |

## Attach it to the content in the host

A transition is the third attachment slot after the static options and the placement, and it follows the same rules — attach it before the first display, and the value attached at the first native layout pass is the one that runs ([Layout](layout.md)).

| Attachment target | Android | iOS |
|---|---|---|
| Classic views | `ksDialogTransition` extension property | `ksDialogTransition` extension property |
| Declarative UI | `KsDialogAttributes(transition = …)` | `.ksDialogTransition(…)` modifier |

The following is the Android registration: it maps the choice the view model carried to an Android transition and attaches it to the content it returns.

```kotlin
Dialog.instance.registry.register(NoticeViewModel::class) { viewModel, notifier ->
    NoticeContentView(this, viewModel, notifier).apply {
        ksDialogTransition = when (viewModel.transition) {
            TransitionChoice.FADE -> DialogTransition.fade()
            TransitionChoice.SLIDE_UP -> DialogTransition.slide(from = DialogTransitionEdge.BOTTOM)
            TransitionChoice.ZOOM -> DialogTransition.zoom()
            TransitionChoice.NONE -> DialogTransition.none()
        }
    }
}
```

The same registration written for iOS looks like this. A shared enum is not exhaustive from Swift, so a `default` is required.

```swift
Dialog.shared.kmp.register(NoticeViewModel.self) { viewModel, notifier in
    let content = NoticeContentView(message: viewModel.message, notifier: notifier)
    content.ksDialogTransition = switch viewModel.transition {
    case .fade: DialogTransition.fade()
    case .slideUp: DialogTransition.slide(from: .bottom)
    case .zoom: DialogTransition.zoom()
    case .none: DialogTransition.none()
    default: DialogTransition.fade()
    }
    return content
}
```

With this registration in place, `show(NoticeViewModel("Saved", TransitionChoice.SLIDE_UP))` in shared code proceeds in this order.

1. The host factory builds the content, and the container lays it out at its final position and size
2. The attached presentation hook starts on the UI thread, and the overlay fade runs in parallel
3. Both finish, the Dialog is showing, and the user's interaction makes the content report a result
4. The dismissal hook and the overlay fade run in parallel
5. The container is removed and `show` returns the result

## Defaults and fine print

- **With nothing attached** — it enters and exits with the container's default cross-fade
- **The overlay fade** — `overlayDuration` decides its time. The overlay always fades, runs in parallel with the content transition, and is never handed to the hooks. A preset sets its own duration here too, so the times match without being specified
- **A `duration` that cannot be animated** — zero, negative, or non-finite, and on Android also a tiny positive value that rounds to zero milliseconds, does not fail: the animation is skipped and the final state is reached immediately
- **When the result arrives** — after both the dismissal hook and the overlay fade have finished and the container has been removed
- **The routes that run the exit transition** — every closing route the library performs, including an outside tap, the Android back button, and a cancelled caller. Only when the OS removes the container does it not run, because there is nothing left to animate
- **Loading and Toast** — their custom content accepts the same attachment. Their built-in content is fixed to the container's default cross-fade

## Write your own hooks

Instead of a preset, you can attach a `DialogTransition` that carries hooks you wrote yourself.

| Piece | Android | iOS |
|---|---|---|
| The transition pair | `DialogTransition(presentation, dismissal, overlayDuration)` | `DialogTransition(presentation:dismissal:overlayDuration:)` |
| The hook type | `suspend (View) -> Unit` | `DialogTransition.Hook` |

- Writing only one side is allowed. The side you leave out falls back to the container's default cross-fade
- An attached hook is not blended with the default; it replaces that side entirely. To mix, take a hook out of the value a preset returned and pass it to a `DialogTransition` you build yourself
- A hook starts on the UI thread, and by that point the host view is already laid out at its final position and size. The container waits for the hook to finish
- A failure thrown by a hook is absorbed by the container. A presentation still moves on to the shown state, a dismissal still continues removing the container, and the result of `show` is unaffected. The library never restores a half-applied visual state
- The library never times a hook out, so a hook that does not return keeps the Dialog on screen. It only logs a warning past a threshold in debug builds
