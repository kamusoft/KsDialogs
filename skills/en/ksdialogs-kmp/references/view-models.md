# Write shared view models and receive results

Shared code holds only the view-model class and the `show` call; the content in the host is what reports the result. This document covers how to write a view model, how to receive the display entry point, and the flow until the result comes back.

## Write view models as classes

`DialogViewModel<R>`, `LoadingViewModel`, and `ToastViewModel` are markers with no members, and the class itself is the registration key in the registry.

- Declare them as classes. A value type loses instance identity on copy, so it cannot carry a result binding and is rejected before anything is presented
- The result type is declared only by the type argument of the Dialog's `DialogViewModel<R>`. Loading and Toast return no result
- Keep placement, sizing, overlay, and transition out of the view model; they belong to the host content definition ([Layout](layout.md) / [Transitions](transitions.md))
- There are no lifecycle members. Initialize in the constructor, and write the clean-up after the `show` call returns

## Receive the display entry point through the constructor

The caller takes `KsDialog`, `KsLoading`, and `KsToast`, with each default entry as the default value. You normally pass nothing, and tests can pass fakes to verify the shared logic alone. There is no need to replace or duplicate the production native registries. A substitute that implements the contract itself also implements `registry` and the class-based `show` ([iOS host](ios-host.md)).

Whichever entry point you use, the registries, the Loading display, and the Toast display list are shared one each per process.

The following adds Loading and Toast to the caller from [Dialog](dialogs.md).

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.Dialog
import jp.kamusoft.ksdialogs.kmp.DialogException
import jp.kamusoft.ksdialogs.kmp.DialogResult
import jp.kamusoft.ksdialogs.kmp.KsDialog
import jp.kamusoft.ksdialogs.kmp.KsLoading
import jp.kamusoft.ksdialogs.kmp.KsToast
import jp.kamusoft.ksdialogs.kmp.Loading
import jp.kamusoft.ksdialogs.kmp.Toast
import kotlin.coroutines.cancellation.CancellationException

class ItemListViewModel(
    private val repository: ItemRepository,
    private val dialogs: KsDialog = Dialog.instance,
    private val loading: KsLoading = Loading.instance,
    private val toast: KsToast = Toast.instance,
) {
    @Throws(DialogException::class, CancellationException::class)
    suspend fun deleteItem(itemName: String) {
        val result = dialogs.show(DeleteViewModel(itemName))
        if (result !is DialogResult.Completed || !result.value) return
        loading.start(message = "Deleting") { repository.delete(itemName) }
        toast.show("Deleted")
    }
}
```

## The content reports the result

The shared `DialogViewModel<R>` only declares the result type; it does not hold the reporting handle itself. The library binds the handle to that instance for each display and removes it once the result reaches the caller.

| Item | Detail |
|---|---|
| Type | `DialogNotifier<R>` for the result type the view model declared |
| Readable period | Only while it is showing. It is empty before `show`, and after the result or the exception reaches the caller |
| Unit of binding | One per view-model instance. Showing the same instance concurrently fails as a configuration mistake |
| How to report | `complete(value)` returns a value, and `cancel()` reports cancellation |
| Reports after the first | Only the first report takes effect; later ones do nothing |

How the handle is obtained differs per host.

| Host | How the content reaches the handle |
|---|---|
| Android | The two-argument factory receives `DialogNotifier<R>`, or the one-argument factory reads `viewModel.notifier` |
| iOS | The content calls `Dialog.shared.kmp.notifier(for:)`. For a non-boolean result it uses `notifier(for:result:)` |

## Registration required

Before calling `show` or `start`, register content for that view-model class once in each host's startup path. The Dialog, Loading, and Toast registration slots are independent: a class registered for Dialog is never registered for Loading or Toast.

The content registration API belongs to the host, not to the shared code (the only thing shared code can register is how to build the view model — see the next section). Android uses the Android Native registries (`Dialog` / `Loading` / `Toast` in `jp.kamusoft.ksdialogs`); iOS uses the `kmp` entry on each facade.

| What you show | Android registration (Android Native) | iOS registration |
|---|---|---|
| Dialog | `Dialog.instance.registry.register(VM::class) { … }` / `registerCompose(VM::class) { … }` | `Dialog.shared.kmp.register(VM.self) { … }` |
| Custom Loading | `Loading.instance.registry.register(VM::class) { … }` / `registerCompose(VM::class) { … }` | `Loading.shared.kmp.register(VM.self) { … }` |
| Custom Toast | `Toast.instance.registry.register(VM::class) { … }` / `registerCompose(VM::class) { … }` | `Toast.shared.kmp.register(VM.self) { … }` |

Loading and Toast calls that pass only a message use the built-in content, so they need no registration. Complete recipes are in [Android host](android-host.md) and [iOS host](ios-host.md).

## Register a view-model factory and show by class

Each registry entry has two slots per view-model class: the View factory that creates the content, and the view-model factory that creates the view model itself. Each host registers the View factory, and shared code registers the view-model factory with `registry.registerViewModel`. Once both slots are filled, shared code can pass the class instead of an instance. Dialog `show`, Toast `show`, and Loading `show` and `start` all have this form.

The library creates the view model and finishes `configure` before it creates the content, so the content always reads the state `configure` wrote. The view-model factory and `configure` run in the caller's own context and are not moved onto the UI thread — work that needs the UI thread belongs in the host's View factory.

Re-registering replaces only the slot in question, and resolution uses a snapshot taken at call time, so re-registering while something is showing does not change what is on screen. The factory returns an instance of the same class as its registration key; returning a subclass makes the content unresolvable and fails as a type-mismatch configuration mistake.

The shared-code table of view-model factories is separate from the host registries. What you register with the Android Native `registerViewModel` is not visible to a class-based `show` from shared code, so register in shared code when you show from shared code ([Android host](android-host.md)). This form is for shared Kotlin code only and does not exist on the Swift entry of the iOS host ([iOS host](ios-host.md)).

Give the shared view model the state that `configure` writes.

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.DialogViewModel

class ExportViewModel : DialogViewModel<Boolean> {
    var itemName = ""
}
```

The following gathers the Dialog, Loading, and Toast registrations and calls. `UploadLoadingViewModel` is declared in [Loading](loading.md) and `StatusToastViewModel` in [Toast](toast.md). In the Loading scoped form the trailing lambda is the `action`, so `configure` is passed as a named argument.

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.Dialog
import jp.kamusoft.ksdialogs.kmp.DialogException
import jp.kamusoft.ksdialogs.kmp.DialogResult
import jp.kamusoft.ksdialogs.kmp.KsDialog
import jp.kamusoft.ksdialogs.kmp.KsLoading
import jp.kamusoft.ksdialogs.kmp.KsToast
import jp.kamusoft.ksdialogs.kmp.Loading
import jp.kamusoft.ksdialogs.kmp.Toast
import kotlin.coroutines.cancellation.CancellationException

class ExportPresenter(
    private val dialogs: KsDialog = Dialog.instance,
    private val loading: KsLoading = Loading.instance,
    private val toast: KsToast = Toast.instance,
) {
    init {
        dialogs.registry.registerViewModel(ExportViewModel::class, ::ExportViewModel)
        loading.registry.registerViewModel(UploadLoadingViewModel::class, ::UploadLoadingViewModel)
        toast.registry.registerViewModel(StatusToastViewModel::class) { StatusToastViewModel("Exported") }
    }

    @Throws(DialogException::class, CancellationException::class)
    suspend fun exportItem(itemName: String) {
        val result = dialogs.show(ExportViewModel::class) { viewModel ->
            viewModel.itemName = itemName
        }
        if (result !is DialogResult.Completed || !result.value) return
        loading.start(UploadLoadingViewModel::class) { report -> report(1.0) }
        toast.show(StatusToastViewModel::class)
    }
}
```

An unregistered view-model factory, and a factory that returns a class other than its registration key, both become a `DialogException` without reaching the display ([Dialog](dialogs.md) / [Loading](loading.md) / [Toast](toast.md)). An exception thrown by the factory or by `configure` is not wrapped; it propagates to the caller as it is.

## The order from creation to result

1. The caller creates a view-model instance and passes it to `show`
2. The library binds the reporting handle to that instance
3. The host View factory is called and the content is created (the handle is already readable at this point)
4. The content is displayed and the enter transition runs
5. The content reports the result with `complete` or `cancel`. An outside tap or the Android back button also settles it as cancelled
6. The exit transition finishes, the container is removed, and `show` returns the `DialogResult`. The handle binding is released here

The handle binding is always released, not only on a normal result delivery but also on a route where the display failed. That is why the same instance can be shown again right after a failure.
