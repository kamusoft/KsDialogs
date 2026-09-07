# Show a Toast from shared code

A Toast is a non-interactive notification that disappears on its own once its duration elapses. The calls live in shared code (`commonMain`), and you either put text on the built-in non-interactive message Toast or show content registered in each host. `show` is synchronous, returns nothing, and can be called from any thread. The hosts process accepted calls in order on the UI thread.

## Choose a `show`

There are two axes: whether you put text on the built-in appearance or show registered content, and, when you show content, whether you pass a view-model instance or only the class. There is no entry point that closes it — the only thing that makes a Toast disappear is its duration elapsing. Calling on `Toast.instance` or on an injected `KsToast` reaches the same registry and the same stacking management ([View models](view-models.md)).

| Signature | What it does | When to choose it | Registration required |
|---|---|---|---|
| `fun show(message: String, durationMs: Int? = null, placement: DialogPlacement? = null)` | Puts the text on the built-in non-interactive message Toast and shows it | When you only convey text | None |
| `fun show(viewModel: ToastViewModel, durationMs: Int? = null, placement: DialogPlacement? = null)` | Shows registered content | When you build the appearance yourself | Content for that view-model class |
| `fun <VM : ToastViewModel> show(viewModelClass: KClass<VM>, durationMs: Int? = null, placement: DialogPlacement? = null, configure: ((VM) -> Unit)? = null)` | Creates the instance with the registered view-model factory and shows it after `configure` | The same, when the library builds it for you | The above plus a shared-code view-model factory |

Omitting `durationMs` uses the default duration configured in the host, and omitting `placement` uses the host's app-default placement, or the contract default when there is none ([Layout](layout.md)). The class-based form has the view-model factory registered in shared code create the instance; it is for shared Kotlin code only and is not visible from Swift ([View models](view-models.md)). Because `show` is synchronous, `configure` is a synchronous function too and cannot be a suspending one. For the Dialog and Loading entry points, see the selection tables in [Dialog](dialogs.md) and [Loading](loading.md).

## Show a text-only Toast

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.DialogAlignment
import jp.kamusoft.ksdialogs.kmp.DialogPlacement
import jp.kamusoft.ksdialogs.kmp.KsToast
import jp.kamusoft.ksdialogs.kmp.Toast

class ItemEditor(
    private val repository: ItemRepository,
    private val toast: KsToast = Toast.instance,
) {
    suspend fun save(item: Item) {
        repository.save(item)
        toast.show(
            message = "Saved",
            durationMs = 2000,
            placement = DialogPlacement(
                verticalAlignment = DialogAlignment.END,
                offsetY = -100.0,
            ),
        )
    }
}
```

## How the duration works

`durationMs` is an `Int?` in milliseconds. It uses no platform-specific time type, so shared code can pass it. It is counted from the moment the call is accepted, on a monotonic clock that keeps running while the app is in the background, and it has no upper clamp. A value of zero or less is not an exception: it falls back to the host default with a warning log.

The message content is not restricted. An empty string shows with empty content, and a long text wraps onto several lines.

## Show registered custom content

Declare a class in `commonMain` that implements `ToastViewModel`. Register content for this class once in each host's startup path, before calling `show`. Android uses `Toast.instance.registry.register(StatusToastViewModel::class) { … }` and iOS uses `Toast.shared.kmp.register(StatusToastViewModel.self) { … }`. Complete recipes are in [Android host](android-host.md) and [iOS host](ios-host.md).

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.ToastViewModel

class StatusToastViewModel(var message: String = "") : ToastViewModel
```

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.DialogException
import jp.kamusoft.ksdialogs.kmp.KsToast
import jp.kamusoft.ksdialogs.kmp.Toast

class SyncStatusReporter(private val toast: KsToast = Toast.instance) {
    @Throws(DialogException::class)
    fun notifyStatus(message: String) {
        toast.show(
            viewModel = StatusToastViewModel(message),
            durationMs = 1800,
        )
    }
}
```

## Minimal example per method

The text form and the instance form in the selection table already have an example in the sections above. The argument forms that do not appear in those examples are placed here.

Omitting `durationMs` and `placement` to leave the host's default duration and default placement in charge.

```kotlin
toast.show("Saved")
```

Passing `placement`. The custom content form takes the same argument, and it replaces the placement attached to the content.

```kotlin
toast.show(StatusToastViewModel("Synced"), placement = DialogPlacement(verticalAlignment = DialogAlignment.END))
```

Passing the view-model class. The view-model factory registered in shared code creates the instance, and a `configure` you pass runs before the display. Unlike Dialog and Loading, this `configure` is not `suspend`, so it cannot await ([View models](view-models.md)).

```kotlin
toast.show(StatusToastViewModel::class)
toast.show(StatusToastViewModel::class) { viewModel -> viewModel.message = "Exported" }
```

## What a Toast never does

- No `hide`, no result, no progress, no scoped form. The only thing that makes it disappear is its duration elapsing.
- No interaction. A touch on the Toast passes through to the page behind it, and controls placed inside custom content do not respond either. Build a notification that needs a button as a Dialog ([Dialog](dialogs.md)).
- No queueing and no replacement. Concurrent Toasts all appear, stacked by acceptance order with the later one in front, and each expires on its own timer. Toasts at the same effective placement overlap without being spread out.
- No page binding. A Toast keeps showing across page transitions, and a rotation or resize re-lays it out without rewinding its remaining duration.

Loading always draws in front of a Toast. The order between a Dialog and a Toast is not guaranteed.

## Handle configuration-mistake failures

The message route has no configuration-mistake exception. Only the view-model route, which uses registered content, can fail.

| Situation | Message | What to do |
|---|---|---|
| No content for that view-model class is registered in the host | `No View factory is registered for ViewModel type {TypeName}.` | Register it in each host at startup ([Android host](android-host.md) / [iOS host](ios-host.md)) |
| A class was passed but no view-model factory is registered in shared code | `No ViewModel factory is registered for ViewModel type {TypeName}.` | Register it with `registry.registerViewModel` at startup ([View models](view-models.md)) |
| The view-model factory returned an instance of a class other than its registration key | `The registered ViewModel factory does not produce ViewModel type {TypeName}. It produced {TypeName} instead. A ViewModel factory must return a ViewModel of the same class as its registration key.` | Make the factory return the same class as its registration key |

The messages in the table are the values the current implementation returns, not a stable API (what does not change is the exception type and the condition it is thrown under; the wording can change without notice).

In the class-based form, the view-model factory and `configure` run on the calling thread. An exception they throw is therefore not a failure after acceptance: it reaches the caller synchronously out of `show`.

An unregistered class throws `DialogException` synchronously before the call is accepted, and nothing is shown. On iOS the messages also include `The registered View factory cannot accept ViewModel type {TypeName}.` when a registered factory cannot accept that view model.

A failure after acceptance — the host factory throwing, or the container failing to attach — cannot be returned to the caller, because `show` has already returned. That one Toast is dropped with a warning log and its resources are released; other displays and later calls are unaffected. When the window to attach to does not exist yet, it waits for one to appear, and it is discarded unshown if its duration expires first.

For example, if the Android startup path registers only the Dialog content and registers nothing with `Toast.instance.registry`, `show` with a `StatusToastViewModel` fails because its content cannot be resolved.

```kotlin
import android.widget.Button
import jp.kamusoft.ksdialogs.Dialog

object DialogHostRegistration {
    fun register() {
        Dialog.instance.registry.register(DeleteViewModel::class) { viewModel, notifier ->
            Button(this).apply {
                text = "Delete ${viewModel.itemName}"
                setOnClickListener { notifier.complete(true) }
            }
        }
    }
}
```

The caller can `catch` `DialogException`. A configuration mistake cannot be fixed at run time, so log it and rethrow.

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.DialogException
import jp.kamusoft.ksdialogs.kmp.KsToast
import jp.kamusoft.ksdialogs.kmp.Toast

class SyncStatusReporter(private val toast: KsToast = Toast.instance) {
    @Throws(DialogException::class)
    fun notifyStatus(message: String) {
        try {
            toast.show(
                viewModel = StatusToastViewModel(message),
                durationMs = 1800,
            )
        } catch (failure: DialogException) {
            println("Toast content is not registered: ${failure.message}")
            throw failure
        }
    }
}
```

## What stays in the hosts

Shared code controls the message, the duration, the placement, and how the view model is built. The visual style, the app-default placement, the registration of custom content, and the transition attachment belong to the native hosts. Because it never touches a content type, what `Toast.instance.registry` carries in shared code is the view-model factory registration alone, and there is no inline factory route; the Android surface has that route for Android-specific UI. See [Android host](android-host.md) and [iOS host](ios-host.md).
