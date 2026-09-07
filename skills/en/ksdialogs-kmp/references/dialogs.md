# Show a Dialog from shared code

Dialog calls live in shared code (`commonMain`), and each host registers the content View. Shared code holds three things — a view-model class that declares the result type, the `show` call, and the branch on the returned `DialogResult`.

Shared code has no boolean shorthand (the alias of the view model that omits the result type) of the kind the native libraries offer. A view-model declaration always spells the result type out, and `DialogViewModel<Boolean>` is the boolean form. The caller writes no result type — the `DialogResult<R>` that `show` returns follows from the view-model declaration.

## How results and cancellation come back

`DialogResult` is a sealed interface: `Completed` carries the value and `Cancelled` carries none. Exactly one of them is returned per `show`.

`show` can be called from any thread and needs no presentation target. It returns only after the exit animation has finished and the container has been removed, so the Dialog is already off screen when the result arrives — even with no animation attached, the built-in cross-fade delays the return by its own duration.

| Situation | What shared code sees |
|---|---|
| The host content reported completion | `DialogResult.Completed` with the value |
| Cancel from the content, a tap outside the Dialog, or the Android back button | `DialogResult.Cancelled` |
| The OS removed the container before any report | `DialogResult.Cancelled` |
| The calling coroutine was cancelled | `CancellationException`; the Dialog closes and settles internally as cancelled |
| The view-model class is not registered, no screen can present the Dialog, or this exact instance is already showing | `DialogException` |

Outside-tap cancellation is enabled by default; to turn it off, write it in the options the host attaches ([Layout](layout.md)).

## Choose a `show`

There is a single axis: build the view-model instance yourself and pass it, or pass only the class and let the library create it. Calling `show` on `Dialog.instance` or on an injected `KsDialog` reaches the same registry and the same display.

| Signature | What it does | When to choose it | Registration required |
|---|---|---|---|
| `suspend fun <R> show(viewModel: DialogViewModel<R>, placement: DialogPlacement? = null): DialogResult<R>` | Shows the registered content and awaits the settled result | When the caller builds the view model | Content for that view-model class ([Android host](android-host.md) / [iOS host](ios-host.md)) |
| `suspend fun <R, VM : DialogViewModel<R>> show(viewModelClass: KClass<VM>, placement: DialogPlacement? = null, configure: (suspend (VM) -> Unit)? = null): DialogResult<R>` | Creates the instance with the registered view-model factory and shows it once `configure` has finished | When the library builds it for you | The above plus a shared-code view-model factory ([View models](view-models.md)) |

`placement` is the position for this call alone; passing it replaces the whole placement attached to the content ([Layout](layout.md)). The inline factory `show` does not exist in shared code; it lives on the native host surfaces. The class-based `show` is for shared Kotlin code only and is not visible from Swift ([iOS host](ios-host.md)). For the Loading and Toast entry points, see the selection tables in [Loading](loading.md) and [Toast](toast.md).

`Dialog.instance.registry` is the one registry every entry point shares. The only thing shared code can register on it is a view-model factory (`registerViewModel`); it carries no content registration API, because the content type differs per OS ([View models](view-models.md)).

## Declare a view model

A view model is a class that only declares the result type, and it lives in the shared module. That class is itself the key in each host's registry.

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.DialogViewModel

class DeleteViewModel(var itemName: String = "") : DialogViewModel<Boolean>
```

## Register content in each host

Before calling `show`, register content for this class once in each host's startup path. Android uses `Dialog.instance.registry.register(DeleteViewModel::class) { … }` and iOS uses `Dialog.shared.kmp.register(DeleteViewModel.self) { … }`. Complete recipes are in [Android host](android-host.md) and [iOS host](ios-host.md).

## Call from shared code

Put the caller in a shared view-model or use-case class and take the display entry point through the constructor. With `Dialog.instance` as the default value you normally pass nothing, and tests can pass a substitute ([View models](view-models.md)).

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.Dialog
import jp.kamusoft.ksdialogs.kmp.DialogException
import jp.kamusoft.ksdialogs.kmp.DialogResult
import jp.kamusoft.ksdialogs.kmp.KsDialog
import kotlin.coroutines.cancellation.CancellationException

class ItemListViewModel(
    private val repository: ItemRepository,
    private val dialogs: KsDialog = Dialog.instance,
) {
    @Throws(DialogException::class, CancellationException::class)
    suspend fun deleteItem(itemName: String) {
        when (val result = dialogs.show(DeleteViewModel(itemName))) {
            is DialogResult.Completed -> if (result.value) repository.delete(itemName)
            DialogResult.Cancelled -> Unit
        }
    }
}
```

Calling this function from Swift requires `@Throws`. Without the declaration, Kotlin/Native does not convert the exception to `NSError`, and a `suspend` function terminates the process with an uncaught exception ([iOS host](ios-host.md)).

## Return a result type other than a boolean

The type the view model declares becomes the `DialogResult<R>` as it is. The type argument has no constraint, so types other than a boolean can be declared.

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.DialogViewModel

class ChoiceViewModel(val title: String) : DialogViewModel<String>
```

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.Dialog
import jp.kamusoft.ksdialogs.kmp.DialogException
import jp.kamusoft.ksdialogs.kmp.DialogResult
import jp.kamusoft.ksdialogs.kmp.KsDialog
import kotlin.coroutines.cancellation.CancellationException

class PlanPicker(private val dialogs: KsDialog = Dialog.instance) {
    @Throws(DialogException::class, CancellationException::class)
    suspend fun pickPlan(): String? =
        when (val result = dialogs.show(ChoiceViewModel("Choose a plan"))) {
            is DialogResult.Completed -> result.value
            DialogResult.Cancelled -> null
        }
}
```

The host reports with the same type. The `DialogNotifier<R>` an Android factory receives has its type fixed by the declaration, so reporting a different type does not compile. On iOS the result type is invisible from Swift, so pass the same type to `result:` in the registration and in the `notifier` call — a mismatch surfaces as a failure when the result is returned ([iOS host](ios-host.md)).

## Show Dialogs independently and stacked

Use a different view-model instance for each call. Results stay independent when Dialogs overlap; the later Dialog appears in front and receives the user input. Showing the same instance twice at once fails, because the result binding is one per instance.

A user cannot close a covered lower Dialog through the UI. The following platform difference occurs only when application code retains the lower content's notifier and reports that lower result first. Android closes only the lower Dialog; the upper one stays visible and later returns its own completed or cancelled result. iOS removes both from the presentation chain, and the upper `show` returns `DialogResult.Cancelled`. Its dismissal hook does not run, because the OS has already removed the upper container.

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.Dialog
import jp.kamusoft.ksdialogs.kmp.DialogException
import jp.kamusoft.ksdialogs.kmp.DialogResult
import jp.kamusoft.ksdialogs.kmp.KsDialog
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class ChoicePairPicker(private val dialogs: KsDialog = Dialog.instance) {
    @Throws(DialogException::class, CancellationException::class)
    suspend fun pickBoth(): Pair<DialogResult<String>, DialogResult<String>> = coroutineScope {
        val first = async { dialogs.show(ChoiceViewModel("First")) }
        val second = async { dialogs.show(ChoiceViewModel("Second")) }
        first.await() to second.await()
    }
}
```

Only the first completion or cancellation reported by the host content takes effect; later reports do nothing. The library never counts how many Dialogs are stacked, and does not expose that as API.

## Minimal example per method

Of the selection table, the instance-based `show` is already shown as a minimal example in the sections above. The argument forms that do not appear in those examples are placed here.

Passing `placement`. For the duration of this call alone, it replaces the whole placement attached to the content.

```kotlin
dialogs.show(DeleteViewModel("Report"), DialogPlacement(verticalAlignment = DialogAlignment.END))
```

Passing the view-model class. The view-model factory registered in shared code creates the instance. `configure` is the last parameter, so you can pass it as a trailing lambda. It is `suspend`, so it can await inside ([View models](view-models.md)).

```kotlin
dialogs.show(DeleteViewModel::class)
dialogs.show(DeleteViewModel::class) { viewModel -> viewModel.itemName = "Report" }
```

## Handle configuration-mistake failures

A configuration mistake does not return `Cancelled`; it throws `DialogException`, so that a missing registration is not mistaken for a user cancellation. Nothing is created or shown in that case. `DialogException` in shared code has no subclasses, so the cause is only readable from the message — a failure raised in the native library passes its description through, and a failure of the class-based `show` has its description assembled by shared code.

| Situation | Message | What to do |
|---|---|---|
| No content for that view-model class is registered in the host | `No View factory is registered for ViewModel type {TypeName}.` | Register it in each host at startup ([Android host](android-host.md) / [iOS host](ios-host.md)) |
| A class was passed but no view-model factory is registered in shared code | `No ViewModel factory is registered for ViewModel type {TypeName}.` | Register it with `registry.registerViewModel` at startup ([View models](view-models.md)) |
| The view-model factory returned an instance of a class other than its registration key | `The registered ViewModel factory does not produce ViewModel type {TypeName}. It produced {TypeName} instead. A ViewModel factory must return a ViewModel of the same class as its registration key.` | Make the factory return the same class as its registration key |
| No screen can present it | `No screen is available to present the Dialog.` | Call `show` after the first screen appears; it fails immediately instead of queueing |
| The same view-model instance was shown stacked | `This ViewModel instance of type {TypeName} is already being shown.` | Create a new instance for each `show` |
| The reported result value cannot be converted back to the declared result type (iOS) | `The result value type does not match (expected: {TypeName} / actual: {TypeName}).` | Pass the same type the view model declares to the registration and to `result:` on the `notifier` |
| The view model was declared as a value class (Android) | `ViewModel type {TypeName} is a value class and cannot be used as a ViewModel.` | Write the view model as a class |

The messages in the table are the values the current implementation returns, not a stable API (what does not change is the exception type and the condition it is thrown under; the wording can change without notice).

On iOS the messages also include `The registered View factory cannot accept ViewModel type {TypeName}.` when a registered factory cannot accept that view model, and `No Dialog result was delivered.` and `Failed to show the Dialog.` when no result arrives.

An exception thrown by the view-model factory or by `configure` is not wrapped in `DialogException`. The display is not reached, and that exception propagates to the caller as it is.

For example, if the Android startup path registers only `DeleteViewModel` and the `ChoiceViewModel` registration is forgotten, `show` for `ChoiceViewModel` fails because its content cannot be resolved.

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

The caller can `catch` `DialogException`. A configuration mistake cannot be fixed at run time, so log it and rethrow to make it visible during development. `CancellationException` is a different type and does not enter this `catch`, so coroutine cancellation propagates as it is.

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.Dialog
import jp.kamusoft.ksdialogs.kmp.DialogException
import jp.kamusoft.ksdialogs.kmp.DialogResult
import jp.kamusoft.ksdialogs.kmp.KsDialog
import kotlin.coroutines.cancellation.CancellationException

class PlanPicker(private val dialogs: KsDialog = Dialog.instance) {
    @Throws(DialogException::class, CancellationException::class)
    suspend fun pickPlan(): String? =
        try {
            when (val result = dialogs.show(ChoiceViewModel("Choose a plan"))) {
                is DialogResult.Completed -> result.value
                DialogResult.Cancelled -> null
            }
        } catch (failure: DialogException) {
            println("Dialog content is not registered: ${failure.message}")
            throw failure
        }
}
```
