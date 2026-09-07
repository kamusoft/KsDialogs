# Attach a presentation and dismissal transition

The presentation and dismissal animations are collected into a `DialogTransition` and attached to the content. There is no route that passes one as a `show` argument. This document covers how to pick a preset and how to write everything from the attachment to the presentation.

## Choose a preset

| Preset | Signature | Animation |
|---|---|---|
| fade | `DialogTransition.fade(duration, easing)` | Enters and leaves through opacity |
| slide | `DialogTransition.slide(from, duration, easing)` | Slides in from the `from` edge and slides out toward the same edge |
| zoom | `DialogTransition.zoom(duration, easing)` | Expands from slightly scaled down to full size, then shrinks back to that scale as it disappears |
| none | `DialogTransition.none()` | No animation on the content side |

A preset returns a `DialogTransition` whose `presentation`, `dismissal`, and `overlayDuration` are all filled in.

| Argument | Type | Default | Meaning |
|---|---|---|---|
| `from` | `DialogTransitionEdge` | None (required only for `slide`) | The edge to slide in from and out toward |
| `duration` | `kotlin.time.Duration` | 250 milliseconds | The one-way duration |
| `easing` | `Interpolator` | `AccelerateDecelerateInterpolator` | How progress advances over time |

| Value passed as `from` | Direction |
|---|---|
| `TOP` / `BOTTOM` | Stays the physical direction |
| `START` / `END` | Follows layout direction (left and right swap in a right-to-left environment) |

## Defaults and details

- **Nothing attached** — the content and the overlay cross-fade over 250 milliseconds
- **Only one side attached** — the side you wrote is replaced entirely, and only the side you did not write falls back to the default cross-fade. It is not merged with the default
- **`none()`** — removes the animation on the content side only. The overlay still fades as it does by default
- **Overlay** — the container always brings it in and out on a layer separate from the content. Its fade follows `overlayDuration`, and a preset that takes a `duration` puts that same `duration` there
- **Concurrency** — the content animation and the overlay fade run in parallel, and both presentation and dismissal wait for both to finish
- **An unusable `duration`** — zero, a negative value, and an infinity, as well as a tiny positive value that rounds down to zero milliseconds, are not treated as errors: the animation is skipped and the final state is reached immediately
- **When the attached values are read** — after the first native layout pass. Rewriting them while the Dialog is shown does not affect the current presentation

## Attach to Compose content

Pass it as the `transition` of `KsDialogAttributes`. It is the same declaration as the layout attributes, so it can be written together with `options` and `placement`.

The following content slides in from the bottom edge over 300 milliseconds.

```kotlin
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import jp.kamusoft.ksdialogs.DialogNotifier
import jp.kamusoft.ksdialogs.DialogTransition
import jp.kamusoft.ksdialogs.DialogTransitionEdge
import jp.kamusoft.ksdialogs.compose.KsDialogAttributes
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SlidingConfirmContent(viewModel: ConfirmViewModel, notifier: DialogNotifier<Boolean>) {
    KsDialogAttributes(
        transition = DialogTransition.slide(
            from = DialogTransitionEdge.BOTTOM,
            duration = 300.milliseconds,
        ),
    )
    Column {
        Text(viewModel.message)
        Button(onClick = { notifier.complete(true) }) { Text("OK") }
    }
}
```

## Attach to View content

Set the `View` extension property `ksDialogTransition`. Setting it while building the View is enough.

The following attaches the same slide to View content.

```kotlin
import android.content.Context
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import jp.kamusoft.ksdialogs.DialogNotifier
import jp.kamusoft.ksdialogs.DialogTransition
import jp.kamusoft.ksdialogs.DialogTransitionEdge
import jp.kamusoft.ksdialogs.ksDialogTransition
import kotlin.time.Duration.Companion.milliseconds

class SlidingConfirmCardView(
    context: Context,
    viewModel: ConfirmViewModel,
    notifier: DialogNotifier<Boolean>,
) : LinearLayout(context) {
    init {
        orientation = VERTICAL
        addView(TextView(context).apply { text = viewModel.message })
        addView(
            Button(context).apply {
                text = "OK"
                setOnClickListener { notifier.complete(true) }
            },
        )
        ksDialogTransition = DialogTransition.slide(
            from = DialogTransitionEdge.BOTTOM,
            duration = 300.milliseconds,
        )
    }
}
```

## Register and call

The animation is written on the content side, so registration and the call look no different from a Dialog without one ([Dialog](dialogs.md)).

```kotlin
import android.app.Application
import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.compose.registerCompose

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Dialog.instance.registry.registerCompose(ConfirmViewModel::class) { viewModel, notifier ->
            SlidingConfirmContent(viewModel, notifier)
        }
    }
}
```

Calling `show` slides the content in with the attached animation, and slides it out toward the same edge once a result is reported. The result latches on the first report, but `show` returns only after the dismissal animation, the overlay fade, and the container removal have all finished. Writing the next screen transition after `show` therefore runs it once the Dialog has fully disappeared.

```kotlin
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.DialogResult
import kotlinx.coroutines.launch

class ItemActivity : ComponentActivity() {
    fun onDeleteClicked() {
        lifecycleScope.launch {
            val result = Dialog.instance.show(ConfirmViewModel("Delete this item?"))
            if (result is DialogResult.Completed && result.value) {
                deleteItem()
            }
        }
    }

    private fun deleteItem() {
    }
}
```

Cancelling the calling coroutine propagates `CancellationException`. The dismissal animation and the container removal still run to completion.

## Pass your own hooks

The constructor is `DialogTransition(presentation, dismissal, overlayDuration)`, and all three arguments can be omitted.

- A hook has the type `suspend (View) -> Unit`
- It starts on the Main dispatcher and receives the laid-out host View, so the animation can begin from a known position and size
- It must return once its own animation has finished. The container waits for the hook with no timeout, so a hook that never returns makes the Dialog impossible to close
- If a hook throws, the container absorbs it and only logs it, and `show` still returns its result. The library does not undo whatever appearance the hook changed part way
- In a debuggable application, a warning is logged when a hook takes more than 5 seconds

The following animation rises from the bottom as it appears and disappears through opacity alone. Waiting for the animation to finish is written with `suspendCancellableCoroutine`.

```kotlin
import android.view.ViewPropertyAnimator
import jp.kamusoft.ksdialogs.DialogTransition
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

fun slideUpTransition() = DialogTransition(
    presentation = { hostView ->
        hostView.translationY = 80f
        hostView.alpha = 0f
        hostView.animate().translationY(0f).alpha(1f).awaitEnd(300L)
    },
    dismissal = { hostView ->
        hostView.animate().alpha(0f).awaitEnd(200L)
    },
    overlayDuration = 200.milliseconds,
)

suspend fun ViewPropertyAnimator.awaitEnd(durationMillis: Long) {
    suspendCancellableCoroutine { continuation ->
        setDuration(durationMillis)
            .withEndAction { if (continuation.isActive) continuation.resume(Unit) }
            .start()
        continuation.invokeOnCancellation { cancel() }
    }
}
```

Attaching it works the same as a preset: `KsDialogAttributes(transition = slideUpTransition())` in Compose, or `ksDialogTransition = slideUpTransition()` for a View. Registration and the caller are unchanged.

## Combine a preset with your own hook

Take just one side's hook out of a preset and pass it to the constructor together with your own hook.

The following leaves the presentation to the zoom preset and uses a custom fade for the dismissal alone.

```kotlin
import jp.kamusoft.ksdialogs.DialogTransition
import kotlin.time.Duration.Companion.milliseconds

fun zoomInFadeOutTransition() = DialogTransition(
    presentation = DialogTransition.zoom(200.milliseconds).presentation,
    dismissal = { hostView -> hostView.animate().alpha(0f).awaitEnd(140L) },
    overlayDuration = 200.milliseconds,
)
```

To align only `overlayDuration` with a preset, pass `DialogTransition.zoom(200.milliseconds).overlayDuration`; to take only the dismissal from a preset, pass its `dismissal` the same way.
