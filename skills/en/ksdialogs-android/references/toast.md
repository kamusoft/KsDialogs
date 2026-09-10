# Show a Toast

A Toast is a fire-and-forget notification that returns no result, and is called from `KsToast` (`Toast.instance` and an injected instance are the same thing). There are two routes: presenting the built-in message Toast as it is, or presenting your own content bound to a view-model type.

Each accepted display uses its supplied duration and placement, has its own timer, and may overlap other Toasts. A Toast does not intercept touches and has neither an overlay color nor an outside-tap handler. When one is visible at the same time as a Loading, the Loading stays in front.

## Choose a `show`

`KsToast` exposes several `show` overloads, and the Compose artifact `jp.kamusoft:ksdialogs` adds the extension function `showCompose`. Three axes decide which to use: whether the content is the built-in message Toast or your own, whether your own content comes from a registered factory or is supplied at the call site, and whether the view model is passed as an instance or as a type. Every entry takes an optional `durationMs` and `placement`.

| Signature | What it does | When to choose it | Registration required |
|---|---|---|---|
| `show(message: String, durationMs: Int? = null, placement: DialogPlacement? = null)` | Shows the built-in message Toast carrying the given text | When only a message is conveyed | None |
| `show(viewModel: ToastViewModel, durationMs: Int? = null, placement: DialogPlacement? = null)` | Shows the content created by a registered factory | When you write the appearance yourself and reuse it | View factory (`register` / `registerCompose`) |
| `show(viewModelClass: KClass<VM>, durationMs: Int? = null, placement: DialogPlacement? = null, configure: ((VM) -> Unit)? = null)` | Takes only the view-model type, then runs `configure` on the instance built by the registered view-model factory before presenting | When the library builds the view model and each call supplies the values to display | View factory + view-model factory (`registerViewModel`) |
| `show(viewModel: VM, durationMs: Int? = null, placement: DialogPlacement? = null, factory: Context.(VM) -> View)` | Passes both the view-model instance and the View factory at the call site. It neither reads nor changes the registry | One-off View content | None |
| `showCompose(viewModel: VM, durationMs: Int? = null, placement: DialogPlacement? = null, content: @Composable (VM) -> Unit)` | Passes both the view-model instance and the Compose content at the call site. It neither reads nor changes the registry | One-off Compose content | None (import from `jp.kamusoft.ksdialogs.compose`) |

A Toast has neither a result nor `hide`. The display disappears automatically once the duration elapses. `configure` is a synchronous function only and cannot be written as `suspend`, because `show` itself is a synchronous call that returns nothing.

In the recipes below, "Show the built-in message Toast" is the first row, "Register and call" the second, "Create and show a view model by type" the third, and "Show custom content without registration" the fourth and fifth.

## Show the built-in message Toast

`durationMs` is in milliseconds; omitting it or passing a value of 0 or less uses `ToastStyle.defaultDuration`, and when that is also 0 or less it falls back to `ToastStyle.BUILTIN_DEFAULT_DURATION` (1500). Passing a value of 0 or less leaves a warning in the log, as does a `defaultDuration` of 0 or less; omitting `durationMs` leaves none. Omitting the placement uses the app default, or the library default (bottom centre of the visible area, offset 80 logical units upward) when there is none.

```kotlin
import jp.kamusoft.ksdialogs.DialogAlignment
import jp.kamusoft.ksdialogs.DialogPlacement
import jp.kamusoft.ksdialogs.Toast

fun notifySaved() {
    Toast.instance.show(
        message = "Saved",
        durationMs = 2000,
        placement = DialogPlacement(
            verticalAlignment = DialogAlignment.END,
            offsetY = -100.0,
        ),
    )
}
```

## Configure Toast defaults

Set `ToastStyle` before the next display is accepted. The visual fields affect the built-in message Toast, while `defaultDuration` and `defaultPlacement` also supply defaults to your own content. `ToastStyle` is a data class, so `copy` changes one field, and the built-in values are available as `ToastStyle.BUILTIN_BACKGROUND_COLOR` and `ToastStyle.BUILTIN_DEFAULT_DURATION`. Placement is the only container attribute a Toast takes.

```kotlin
import android.graphics.Color
import jp.kamusoft.ksdialogs.DialogAlignment
import jp.kamusoft.ksdialogs.DialogPlacement
import jp.kamusoft.ksdialogs.Toast
import jp.kamusoft.ksdialogs.ToastStyle

fun configureToast() {
    Toast.instance.style = ToastStyle(
        backgroundColor = Color.DKGRAY,
        textColor = Color.WHITE,
        fontSize = 16.0,
        cornerRadius = 22.0,
        defaultDuration = ToastStyle.BUILTIN_DEFAULT_DURATION,
        defaultPlacement = DialogPlacement(
            verticalAlignment = DialogAlignment.END,
            offsetY = -120.0,
        ),
    )
}
```

## Declare a view model

Your own content uses a class-based `ToastViewModel` as the type key. A Toast has neither a result nor progress, so the view model only carries the values to display and acts as the registry's type key.

```kotlin
import jp.kamusoft.ksdialogs.ToastViewModel

class StatusToastViewModel(val message: String) : ToastViewModel
```

## Write the content

The content is only the appearance of the notification. To replace the animation, attach a `DialogTransition` ([Transitions](transitions.md)).

```kotlin
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import jp.kamusoft.ksdialogs.DialogTransition
import jp.kamusoft.ksdialogs.compose.KsDialogAttributes

@Composable
fun StatusToastContent(viewModel: StatusToastViewModel) {
    KsDialogAttributes(transition = DialogTransition.fade())
    Text(viewModel.message)
}
```

## Register and call

The Toast registry (`Toast.instance.registry`, of type `ToastViewRegistry`) is independent of the Dialog and Loading ones, so register there. Do it once at startup.

```kotlin
import android.app.Application
import jp.kamusoft.ksdialogs.Toast
import jp.kamusoft.ksdialogs.compose.registerCompose

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Toast.instance.registry.registerCompose(StatusToastViewModel::class) { viewModel ->
            StatusToastContent(viewModel)
        }
    }
}
```

The caller uses `Toast.instance` or an instance injected as `KsToast`. Both point at the same process-wide registry and the same state. `show` is not a suspend function, so it can be called as it is from an Activity or from a screen's view model.

```kotlin
import androidx.lifecycle.ViewModel
import jp.kamusoft.ksdialogs.KsToast

class SyncScreenViewModel(private val toast: KsToast) : ViewModel() {
    fun onSynchronized() {
        toast.show(StatusToastViewModel("Synchronized"), durationMs = 1800)
    }
}
```

## Create and show a view model by type

For content where only the values carried change from call to call, the library can build the view model too. Give the type-key view model a constructor without parameters and set the values in `configure`.

```kotlin
import jp.kamusoft.ksdialogs.ToastViewModel

class NoticeToastViewModel : ToastViewModel {
    var message = ""
}
```

The View factory and the view-model factory occupy separate slots of the same registry. Re-registering replaces only the slot it touches; the other one remains ([View models](view-models.md)).

```kotlin
import android.app.Application
import androidx.compose.material3.Text
import jp.kamusoft.ksdialogs.Toast
import jp.kamusoft.ksdialogs.compose.registerCompose

class NoticeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Toast.instance.registry.registerCompose(NoticeToastViewModel::class) { viewModel ->
            Text(viewModel.message)
        }
        Toast.instance.registry.registerViewModel(NoticeToastViewModel::class, ::NoticeToastViewModel)
    }
}
```

The caller does not assemble an instance; it passes the type and `configure`. The order is fixed as "create the view model, complete `configure`, create the content, present", so the values set by `configure` are readable from the content's initialization. Resolution is a snapshot taken when `show` is called, so re-registering while a Toast is on screen does not change it.

```kotlin
import jp.kamusoft.ksdialogs.Toast

fun notifySynchronized() {
    Toast.instance.show(NoticeToastViewModel::class, durationMs = 2000) { viewModel ->
        viewModel.message = "Synchronized"
    }
}
```

On Android, creating the view model and running `configure` happen after the host (a resumed `Activity`) is secured, just like creating the content. A display whose duration expires before a host appears is discarded without either of them being called.

## Show custom content without registration

For one-off content, pass a View factory directly to `show`, or call `showCompose` for Compose content. Neither reads nor changes the registry. The factory lambda has an Android `Context` receiver, so `this` inside the lambda is that `Context`.

```kotlin
import android.widget.TextView
import jp.kamusoft.ksdialogs.DialogAlignment
import jp.kamusoft.ksdialogs.DialogPlacement
import jp.kamusoft.ksdialogs.Toast
import jp.kamusoft.ksdialogs.ToastViewModel

class InlineToastViewModel(val message: String) : ToastViewModel

fun notifyInline() {
    Toast.instance.show(
        viewModel = InlineToastViewModel("Uploaded"),
        durationMs = 1200,
        placement = DialogPlacement(verticalAlignment = DialogAlignment.START),
    ) { viewModel ->
        TextView(this).apply { text = viewModel.message }
    }
}
```

```kotlin
import androidx.compose.material3.Text
import jp.kamusoft.ksdialogs.DialogAlignment
import jp.kamusoft.ksdialogs.DialogPlacement
import jp.kamusoft.ksdialogs.Toast
import jp.kamusoft.ksdialogs.ToastViewModel
import jp.kamusoft.ksdialogs.compose.showCompose

class InlineComposeToastViewModel(val message: String) : ToastViewModel

fun notifyInlineCompose() {
    Toast.instance.showCompose(
        viewModel = InlineComposeToastViewModel("Uploaded"),
        durationMs = 1200,
        placement = DialogPlacement(verticalAlignment = DialogAlignment.START),
    ) { viewModel ->
        Text(viewModel.message)
    }
}
```

## Recover from a configuration mistake

A configuration mistake presents nothing and throws `DialogException` synchronously at the call site. The `show` that presents the built-in message Toast needs neither registration nor content, so these failures occur only on the routes that use your own content. A content-factory failure after acceptance is different: it is logged and discards only that display. An exception thrown by the view-model factory or by `configure` on the type-based route is handled the same way, and does not reach the caller because `show` has already returned.

| Exception | Message | Cause and remedy |
|---|---|---|
| `DialogException.ViewFactoryNotRegistered` | `No View factory is registered for ViewModel type {TypeName}.` | The registry-based and type-based `show` has no matching factory. Call `register` or `registerCompose` on `Toast.instance.registry`, or switch to a route that passes the factory at the call site |
| `DialogException.ViewModelFactoryNotRegistered` | `No ViewModel factory is registered for ViewModel type {TypeName}.` | The `show` that takes a type has no view-model factory. Call `registerViewModel` |
| `DialogException.ValueClassViewModel` | `ViewModel type {TypeName} is a value class and cannot be used as a ViewModel.` | A value class was used as a `ToastViewModel`. Make the view model a class. This check runs at `register` / `registerCompose` / `registerViewModel` time as well as at display time |

The messages in the table are the values the current implementation returns, not a stable API (what does not change is the exception type and the condition it is thrown under; the wording can change without notice).

Both exceptions expose the view-model type name through `viewModelTypeName`. For example, showing `StatusToastViewModel` while the startup registration is missing raises `DialogException.ViewFactoryNotRegistered`.

```kotlin
import android.app.Application

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
```

The caller can `catch` the nested exception type directly. A configuration mistake cannot be fixed at runtime, so log it and rethrow to make it visible during development.

```kotlin
import android.util.Log
import jp.kamusoft.ksdialogs.DialogException
import jp.kamusoft.ksdialogs.Toast

fun notifySynchronization() {
    try {
        Toast.instance.show(StatusToastViewModel("Synchronized"), durationMs = 1800)
    } catch (exception: DialogException.ViewFactoryNotRegistered) {
        Log.e("MyApp", "Toast view factory is not registered for ${exception.viewModelTypeName}.")
        throw exception
    }
}
```
