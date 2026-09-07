# Show a Loading

A Loading blocks interaction while work runs, and is called from `KsLoading` (`Loading.instance` and an injected instance are the same thing). There are two routes: presenting the built-in default Loading as it is, or presenting your own content bound to a view-model type.

Concurrent uses coalesce into one process-wide display, and the content of the first start is kept. Outside-tap cancellation stays disabled for Loading.

## Choose `show` or `start`

`KsLoading` has `show`, which opens a display you close yourself, and `start`, which ties the display's lifetime to an operation; each exposes several overloads. The `ksdialogs-compose` artifact adds the extension functions `showCompose` and `startCompose`. Three axes decide which to use: whether the content is the built-in default or your own, whether your own content comes from a registered factory or is supplied at the call site, and whether the view model is passed as an instance or as a type. Every entry takes an optional `placement` ([Layout](layout.md)).

| Signature | What it does | When to choose it | Registration required |
|---|---|---|---|
| `show(message: String? = null, placement: DialogPlacement? = null)` | Shows the built-in default Loading and starts one coalescing use | When only a message is shown and you close it yourself | None |
| `show(viewModel: LoadingViewModel, placement: DialogPlacement? = null)` | Shows the content created by a registered factory | When you write the appearance yourself and reuse it | View factory (`register` / `registerCompose`) |
| `show(viewModelClass: KClass<VM>, placement: DialogPlacement? = null, configure: (suspend (VM) -> Unit)? = null)` | Takes only the view-model type, then runs `configure` on the instance built by the registered view-model factory before presenting | When the library builds the view model and its state is prepared just before presentation | View factory + view-model factory (`registerViewModel`) |
| `show(viewModel: VM, placement: DialogPlacement? = null, factory: Context.(VM) -> View)` | Passes both the view-model instance and the View factory at the call site. It neither reads nor changes the registry | One-off View content | None |
| `showCompose(viewModel: VM, placement: DialogPlacement? = null, content: @Composable (VM) -> Unit)` | Passes both the view-model instance and the Compose content at the call site. It neither reads nor changes the registry | One-off Compose content | None (import from `jp.kamusoft.ksdialogs.compose`) |
| `start(message: String? = null, placement: DialogPlacement? = null, action: suspend ((Double) -> Unit) -> T)` | Runs `action` while the default Loading is shown and returns its value | When the display starts and ends with the operation | None |
| `start(viewModel: LoadingViewModel, placement: DialogPlacement? = null, action: suspend ((Double) -> Unit) -> T)` | Runs `action` while registered content is shown | The same, when you write the appearance yourself | View factory |
| `start(viewModelClass: KClass<VM>, placement: DialogPlacement? = null, configure: (suspend (VM) -> Unit)? = null, action: suspend ((Double) -> Unit) -> T)` | Runs `action` while the content of a view model built from the type is shown | The same, when the library builds the view model too | View factory + view-model factory |
| `start(viewModel: VM, placement: DialogPlacement? = null, factory: Context.(VM) -> View, action: suspend ((Double) -> Unit) -> T)` | Passes the view model, the View factory, and the operation at the call site | When one-off View content wraps an operation | None |
| `startCompose(viewModel: VM, placement: DialogPlacement? = null, content: @Composable (VM) -> Unit, action: suspend ((Double) -> Unit) -> T)` | Passes the view model, the Compose content, and the operation at the call site | When one-off Compose content wraps an operation | None (compose artifact) |
| `hide()` | Closes the display. It closes immediately regardless of the coalescing count and does not interfere with running work | When closing a display opened with `show` | — |
| `setMessage(message: String?)` | Replaces the message on the current display | When reporting how far the default Loading has got | — |

`hide` has no overload, and a display opened with `start` closes automatically when `action` completes or fails, so `hide` need not be called.

In the recipes below, "Show and update the default Loading" covers the first, eleventh, and twelfth rows, "Run work in a Loading scope" the sixth, "Register and call" the seventh and second, "Create and show a view model by type" the third and eighth, and "Show custom content without registration" the ninth and tenth as well as the fourth and fifth.

## Show and update the default Loading

Use `show`, `setMessage`, and `hide` for imperative control. `hide` closes that display without cancelling running work.

```kotlin
import jp.kamusoft.ksdialogs.Loading

suspend fun synchronize() {
    Loading.instance.show("Connecting")
    try {
        Loading.instance.setMessage("Downloading")
        performSynchronization()
    } finally {
        Loading.instance.hide()
    }
}

suspend fun performSynchronization() {}
```

## Run work in a Loading scope

Use `start` to pair the display's lifetime with a suspending operation. Reported progress is clamped to `0.0..1.0`. The operation still runs when its display coalesces with another use, and its value or failure is returned to the caller.

```kotlin
import jp.kamusoft.ksdialogs.Loading

suspend fun download(): ByteArray =
    Loading.instance.start(message = "Downloading") { report ->
        report(0.25)
        val data = fetchData()
        report(1.0)
        data
    }

suspend fun fetchData(): ByteArray = byteArrayOf()
```

## Configure the default Loading

Set `style` and `options` before the next display starts. The container reads both when a display begins, so a change made while one is on screen applies to the next one. Style controls the indicator and message; options control the container's layout and overlay. `LoadingStyle` is a data class, so `copy` changes one field, and `LoadingStyle.DEFAULT_PROGRESS_FORMAT` is the built-in default for `progressFormat`.

```kotlin
import android.graphics.Color
import jp.kamusoft.ksdialogs.DialogEdgeInsets
import jp.kamusoft.ksdialogs.DialogOptions
import jp.kamusoft.ksdialogs.Loading
import jp.kamusoft.ksdialogs.LoadingStyle

fun configureLoading() {
    Loading.instance.style = LoadingStyle(
        indicatorColor = Color.CYAN,
        messageFontSize = 16.0,
        messageColor = Color.WHITE,
        defaultMessage = "Working",
        progressFormat = { message, progress ->
            if (progress == null) message.orEmpty() else "${message.orEmpty()} ${(progress * 100).toInt()}%"
        },
    )
    Loading.instance.options = DialogOptions(
        dialogMargin = DialogEdgeInsets(32.0),
        overlayColor = 0x80000000.toInt(),
    )
}
```

## Declare a view model

Your own content uses a class-based `LoadingViewModel` as the type key. A view model that wants the progress reported by `start` to reach the UI implements `LoadingProgressReceiver` and exposes the received value as state. A view model that does not implement it simply receives no progress; the display itself is unchanged.

```kotlin
import jp.kamusoft.ksdialogs.LoadingProgressReceiver
import jp.kamusoft.ksdialogs.LoadingViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProgressLoadingViewModel : LoadingViewModel, LoadingProgressReceiver {
    private val mutableProgress = MutableStateFlow(0.0)
    val progress = mutableProgress.asStateFlow()

    override fun onProgress(progress: Double) {
        mutableProgress.value = progress
    }
}
```

## Write the content

The content reads the progress state with lifecycle awareness. In Compose, use `collectAsStateWithLifecycle`.

```kotlin
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ProgressLoadingContent(viewModel: ProgressLoadingViewModel) {
    val progress = viewModel.progress.collectAsStateWithLifecycle()
    CircularProgressIndicator(progress = { progress.value.toFloat() })
}
```

Only your own content can replace the animation, specified by attaching a `DialogTransition` to the content ([Transitions](transitions.md)).

## Register and call

The Loading registry (`Loading.instance.registry`, of type `LoadingViewRegistry`) is independent of the Dialog one, so register there. Do it once at startup.

```kotlin
import android.app.Application
import jp.kamusoft.ksdialogs.Loading
import jp.kamusoft.ksdialogs.compose.registerCompose

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Loading.instance.registry.registerCompose(ProgressLoadingViewModel::class) { viewModel ->
            ProgressLoadingContent(viewModel)
        }
    }
}
```

The caller uses `Loading.instance` or an instance injected as `KsLoading`. Both point at the same process-wide registry and the same state. To call from a screen's view model, put it on `viewModelScope`.

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jp.kamusoft.ksdialogs.KsLoading
import kotlinx.coroutines.launch

class SyncScreenViewModel(private val loading: KsLoading) : ViewModel() {
    fun onSyncClicked() {
        viewModelScope.launch {
            loading.start(ProgressLoadingViewModel()) { report ->
                report(0.5)
                performSynchronization()
                report(1.0)
            }
        }
    }
}
```

To open it imperatively without wrapping an operation, pass only the view-model instance to the `show` that uses the same registration. Close it with `hide`.

```kotlin
loading.show(ProgressLoadingViewModel())
```

## Create and show a view model by type

To let the library build the view model as well, register a view-model factory in the same registry as the View factory and pass a class reference to `show` / `start`. A constructor without parameters works as the factory through its reference. Re-registering replaces only the slot it touches, so the View factory registration remains ([View models](view-models.md)).

```kotlin
Loading.instance.registry.registerViewModel(ProgressLoadingViewModel::class, ::ProgressLoadingViewModel)
```

The caller does not assemble an instance; it passes the type and `configure`. The order is fixed as "create the view model, complete `configure`, bind the progress receiver, create the content, present", so the state set by `configure` is readable from the content's initialization. `configure` can be written as `suspend`, so the initial state can also be loaded before presentation.

```kotlin
import jp.kamusoft.ksdialogs.Loading

suspend fun uploadWithTypedLoading(): Int =
    Loading.instance.start(
        ProgressLoadingViewModel::class,
        configure = { viewModel -> viewModel.onProgress(0.0) },
    ) { report ->
        report(1.0)
        1
    }
```

To open it imperatively without wrapping an operation, pass the same type to `show`. The trailing lambda becomes `configure`.

```kotlin
Loading.instance.show(ProgressLoadingViewModel::class) { viewModel -> viewModel.onProgress(0.0) }
```

Once `configure` completes, the call enters exactly the same coalescing decision as the instance route. If another use has already established the display while `configure` was running, this call joins it: the created view model is not used for the display and no content is created. A failure from the view-model factory or from `configure` proceeds to neither presentation nor coalescing; it propagates to the caller, and with `start` the operation is not run either.

## Show custom content without registration

For one-off content, pass a View factory directly to `show` / `start`, or call `showCompose` / `startCompose` for Compose content. Neither reads nor changes the registry. The factory lambda has an Android `Context` receiver, so `this` inside the lambda is that `Context`.

```kotlin
import android.widget.ProgressBar
import jp.kamusoft.ksdialogs.Loading
import jp.kamusoft.ksdialogs.LoadingViewModel

class InlineLoadingViewModel : LoadingViewModel

suspend fun importFile(): Int =
    Loading.instance.start(
        viewModel = InlineLoadingViewModel(),
        factory = { ProgressBar(this) },
    ) { report ->
        report(1.0)
        1
    }
```

```kotlin
import androidx.compose.material3.CircularProgressIndicator
import jp.kamusoft.ksdialogs.Loading
import jp.kamusoft.ksdialogs.LoadingViewModel
import jp.kamusoft.ksdialogs.compose.startCompose

class InlineComposeLoadingViewModel : LoadingViewModel

suspend fun exportFile(): Int =
    Loading.instance.startCompose(
        viewModel = InlineComposeLoadingViewModel(),
        content = { CircularProgressIndicator() },
    ) { report ->
        report(1.0)
        1
    }
```

To open it imperatively without wrapping an operation, pass the same view model and factory to `show`. The trailing lambda becomes `factory`.

```kotlin
Loading.instance.show(InlineLoadingViewModel()) { ProgressBar(this) }
```

Compose content works the same way, with the trailing lambda of `showCompose` becoming `content` (import `jp.kamusoft.ksdialogs.compose.showCompose`).

```kotlin
Loading.instance.showCompose(InlineComposeLoadingViewModel()) { CircularProgressIndicator() }
```

## Recover from a configuration mistake

A configuration mistake throws `DialogException` without presenting anything. With `start`, the operation is not run either (fail-fast). The `show` / `start` that present the built-in default Loading need neither registration nor content, so these failures occur only on the custom-content routes.

| Exception | Message | Cause and remedy |
|---|---|---|
| `DialogException.ViewFactoryNotRegistered` | `No View factory is registered for ViewModel type {TypeName}.` | The registry-based and type-based `show` / `start` has no matching factory. Call `register` or `registerCompose` on `Loading.instance.registry`, or switch to a route that passes the factory at the call site |
| `DialogException.ViewModelFactoryNotRegistered` | `No ViewModel factory is registered for ViewModel type {TypeName}.` | The `show` / `start` that takes a type has no view-model factory. Call `registerViewModel` |
| `DialogException.ValueClassViewModel` | `ViewModel type {TypeName} is a value class and cannot be used as a ViewModel.` | A value class was used as a `LoadingViewModel`. Make the view model a class. This check runs at `register` / `registerCompose` / `registerViewModel` time as well as at display time |

The messages in the table are the values the current implementation returns, not a stable API (what does not change is the exception type and the condition it is thrown under; the wording can change without notice).

Both exceptions expose the view-model type name through `viewModelTypeName`. For example, calling `start` with `ProgressLoadingViewModel` while the startup registration is missing raises `DialogException.ViewFactoryNotRegistered`.

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
import jp.kamusoft.ksdialogs.Loading

suspend fun runCustomLoading() {
    try {
        Loading.instance.start(ProgressLoadingViewModel()) { report ->
            report(0.5)
        }
    } catch (exception: DialogException.ViewFactoryNotRegistered) {
        Log.e("MyApp", "Loading view factory is not registered for ${exception.viewModelTypeName}.")
        throw exception
    }
}
```
