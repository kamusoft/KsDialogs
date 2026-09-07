# Show a Loading from shared code

Loading is a display that blocks interaction with the whole screen while work runs, and only one of them exists process-wide. Concurrent uses coalesce into a single display, and every action still runs. The calls live in shared code (`commonMain`), and you either use the built-in appearance as it is or use content registered in each host.

Loading cannot be closed by the user. An outside tap is not a trigger to close it and does not reach the screen behind. When the work needs to be interruptible, use a Dialog with a cancel control instead ([Dialog](dialogs.md)). Loading also stays in front of every Dialog and of every Toast, whatever the order they started in.

## Choose between `start` and `show`

There are three axes: whether the display spans the action or you write the start and the end yourself, whether you use the built-in appearance or registered content, and, when you use content, whether you pass a view-model instance or only the class. Calling on `Loading.instance` or on an injected `KsLoading` reaches the same display and the same coalescing state ([View models](view-models.md)). All of them are `suspend` functions and can be called from any thread.

| Signature | What it does | When to choose it | Registration required |
|---|---|---|---|
| `suspend fun <T> start(message: String? = null, placement: DialogPlacement? = null, action: suspend ((Double) -> Unit) -> T): T` | Runs the action with the built-in Loading showing and returns its value | When the span of the action is the span of the display | None |
| `suspend fun <T> start(viewModel: LoadingViewModel, placement: DialogPlacement? = null, action: suspend ((Double) -> Unit) -> T): T` | Runs the action with registered content showing | The same, when you build the appearance yourself | Content for that view-model class |
| `suspend fun <VM : LoadingViewModel, T> start(viewModelClass: KClass<VM>, placement: DialogPlacement? = null, configure: (suspend (VM) -> Unit)? = null, action: suspend ((Double) -> Unit) -> T): T` | Creates the instance with the registered view-model factory and, once `configure` has finished, does the same as the row above | The same, when the library builds it for you | The above plus a shared-code view-model factory |
| `suspend fun show(message: String? = null, placement: DialogPlacement? = null)` | Shows the built-in Loading and opens one use | When the start and the end are in different places | None |
| `suspend fun show(viewModel: LoadingViewModel, placement: DialogPlacement? = null)` | Shows registered content and opens one use | The same, when you build the appearance yourself | Content for that view-model class |
| `suspend fun <VM : LoadingViewModel> show(viewModelClass: KClass<VM>, placement: DialogPlacement? = null, configure: (suspend (VM) -> Unit)? = null)` | Creates the instance with the registered view-model factory and, once `configure` has finished, does the same as the row above | The same, when the library builds it for you | The above plus a shared-code view-model factory |
| `suspend fun setMessage(message: String?)` | Replaces the text of the built-in Loading that is showing | When you report the situation partway through the work | None |
| `suspend fun hide()` | Closes the display | When you end a display started with `show` | None |

`start` has no matching end — completing the action is the end. The only end that matches `show` is `hide`, which closes immediately regardless of how many uses are open. Omitting `message` uses the default message configured in the host, and omitting `placement` uses the contract default ([Layout](layout.md)). The class-based forms have the view-model factory registered in shared code create the instance; they are for shared Kotlin code only and are not visible from Swift ([View models](view-models.md)). For the Dialog and Toast entry points, see the selection tables in [Dialog](dialogs.md) and [Toast](toast.md).

## Show it only while the work runs

`start` opens one use, runs the action, and closes that use whether the action succeeds, fails, or is cancelled. The call that drops the count to zero returns only after the input-blocking container has been removed.

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.KsLoading
import jp.kamusoft.ksdialogs.kmp.Loading
import kotlin.coroutines.cancellation.CancellationException

class ReportDownloader(
    private val client: ReportClient,
    private val loading: KsLoading = Loading.instance,
) {
    @Throws(CancellationException::class)
    suspend fun download(): ByteArray =
        loading.start(message = "Downloading") { report ->
            report(0.25)
            val data = client.fetch()
            report(1.0)
            data
        }
}
```

The reporting handle is a `(Double) -> Unit` function, callable from any thread. Values outside `0`–`1` are clamped, the latest report wins, and there is no API that receives a stream. When no screen can host the display, the display is skipped but the action still runs and `start` returns its value as usual.

## Write the start and the end yourself

Use `show`, `setMessage`, and `hide` when the lifetime is controlled separately. `show` returns as soon as the interaction block is in effect and does not wait for the enter animation to finish. `hide` does not cancel work that is already running — it ends a generation, so a use started afterwards belongs to a new one and neither closes the old display nor receives progress that arrives late. `setMessage` takes no part in the coalescing and applies only while the built-in Loading is showing (nothing happens while it is hidden or while custom content is showing).

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.KsLoading
import jp.kamusoft.ksdialogs.kmp.Loading
import kotlin.coroutines.cancellation.CancellationException

class LibrarySynchronizer(
    private val client: ReportClient,
    private val loading: KsLoading = Loading.instance,
) {
    @Throws(CancellationException::class)
    suspend fun synchronize() {
        loading.show("Connecting")
        try {
            loading.setMessage("Synchronizing")
            client.synchronize()
        } finally {
            loading.hide()
        }
    }
}
```

## Receive progress in custom content

Declare a class in `commonMain` that implements `LoadingViewModel`, and implement `LoadingProgressReceiver` as well when the content should follow the progress. While that view model is showing, the library forwards the reports to `onProgress` on the UI thread. Nothing is forwarded when the interface is not implemented.

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.LoadingProgressReceiver
import jp.kamusoft.ksdialogs.kmp.LoadingViewModel

class UploadLoadingViewModel : LoadingViewModel, LoadingProgressReceiver {
    var title = ""
    var progress = 0.0
        private set

    override fun onProgress(progress: Double) {
        this.progress = progress
    }
}
```

Register content for this class once in each host's startup path, before calling `start` or `show`. Android uses `Loading.instance.registry.register(UploadLoadingViewModel::class) { … }` and iOS uses `Loading.shared.kmp.register(UploadLoadingViewModel.self) { … }`. Complete recipes are in [Android host](android-host.md) and [iOS host](ios-host.md).

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.DialogException
import jp.kamusoft.ksdialogs.kmp.KsLoading
import jp.kamusoft.ksdialogs.kmp.Loading
import kotlin.coroutines.cancellation.CancellationException

class PhotoUploader(
    private val client: ReportClient,
    private val loading: KsLoading = Loading.instance,
) {
    @Throws(DialogException::class, CancellationException::class)
    suspend fun upload() {
        loading.start(UploadLoadingViewModel()) { report ->
            report(0.5)
            client.send()
            report(1.0)
        }
    }
}
```

## Minimal example per method

Of the selection table, `start(message)`, `start(viewModel)`, `show(message)`, `setMessage`, and `hide` already appear as minimal examples in the sections above. The remaining forms are placed here.

The `show` that displays registered content and opens one use. Unlike `start` it is not bound to the span of an action, so you write `hide` yourself to end it.

```kotlin
loading.show(UploadLoadingViewModel())
```

Passing `placement`. Every `start` and `show` entry point takes the same argument, and it replaces the placement of this display.

```kotlin
loading.show(message = "Downloading", placement = DialogPlacement(verticalAlignment = DialogAlignment.END))
```

```kotlin
loading.start(UploadLoadingViewModel(), DialogPlacement(offsetY = -100.0)) { report -> report(1.0) }
```

The `show` that takes the view-model class. The view-model factory registered in shared code creates the instance. `configure` is the last parameter, so you can pass it as a trailing lambda ([View models](view-models.md)).

```kotlin
loading.show(UploadLoadingViewModel::class)
loading.show(UploadLoadingViewModel::class) { viewModel -> viewModel.title = "Uploading" }
```

The scoped form that takes the class. The trailing lambda is the `action`; pass `configure` as a named argument.

```kotlin
loading.start(UploadLoadingViewModel::class) { report -> report(1.0) }
loading.start(
    UploadLoadingViewModel::class,
    configure = { viewModel -> viewModel.title = "Uploading" },
) { report -> report(1.0) }
```

## Handle configuration-mistake failures

The built-in Loading routes (`show(message)`, `start(message)`, `setMessage`, `hide`) have no configuration-mistake exception. Only the view-model routes, which use registered content, can fail.

| Situation | Message | What to do |
|---|---|---|
| No content for that view-model class is registered in the host | `No View factory is registered for ViewModel type {TypeName}.` | Register it in each host at startup ([Android host](android-host.md) / [iOS host](ios-host.md)) |
| A class was passed but no view-model factory is registered in shared code | `No ViewModel factory is registered for ViewModel type {TypeName}.` | Register it with `registry.registerViewModel` at startup ([View models](view-models.md)) |
| The view-model factory returned an instance of a class other than its registration key | `The registered ViewModel factory does not produce ViewModel type {TypeName}. It produced {TypeName} instead. A ViewModel factory must return a ViewModel of the same class as its registration key.` | Make the factory return the same class as its registration key |

The messages in the table are the values the current implementation returns, not a stable API (what does not change is the exception type and the condition it is thrown under; the wording can change without notice).

An exception thrown by the view-model factory or by `configure` is not wrapped in `DialogException`; it propagates to the caller as it is, and the class-based scoped form does not run the `action` either.

An unregistered class fails with `DialogException` before the action begins, so neither the display nor the action happens. The same holds for a call that was about to join the coalescing, and a Loading that is already showing is unaffected. On iOS the messages also include `The registered View factory cannot accept ViewModel type {TypeName}.` when a registered factory cannot accept that view model, and `Failed to show the Loading.` when the host returns neither a result nor an error.

For example, if the Android startup path registers only the Dialog content and registers nothing with `Loading.instance.registry`, `start` with an `UploadLoadingViewModel` fails because its content cannot be resolved.

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

The caller can `catch` `DialogException`. A configuration mistake cannot be fixed at run time, so log it and rethrow. `CancellationException` is a different type and does not enter this `catch`, so coroutine cancellation propagates as it is.

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.DialogException
import jp.kamusoft.ksdialogs.kmp.KsLoading
import jp.kamusoft.ksdialogs.kmp.Loading
import kotlin.coroutines.cancellation.CancellationException

class PhotoUploader(
    private val client: ReportClient,
    private val loading: KsLoading = Loading.instance,
) {
    @Throws(DialogException::class, CancellationException::class)
    suspend fun upload() {
        try {
            loading.start(UploadLoadingViewModel()) { report ->
                report(0.5)
                client.send()
                report(1.0)
            }
        } catch (failure: DialogException) {
            println("Loading content is not registered: ${failure.message}")
            throw failure
        }
    }
}
```

## What stays in the hosts

Shared code controls the message, the placement, the reported progress, and how the view model is built. Everything visual belongs to the native hosts: the built-in indicator's style, the container options, the registration of custom content, and the transition attachment. Because it never touches a content type, what `Loading.instance.registry` carries in shared code is the view-model factory registration alone, and there is no inline factory route; the Android surface has that route for Android-specific UI. See [Android host](android-host.md) and [iOS host](ios-host.md).
