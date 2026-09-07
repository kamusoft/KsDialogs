# Show a Dialog

Declare the result type on a class-based view model, register a factory that builds fresh content for every presentation, then await `DialogResult`. Completion with a value and cancellation are distinguished by `DialogResult.Completed` and `DialogResult.Cancelled`. Use the alias `SimpleDialogViewModel` for a view model whose result type is `Boolean`, and `DialogViewModel<R>` for another result type.

Content can be written in Jetpack Compose or as an Android View. Both register into the same `DialogViewRegistry`, and `show` is called the same way whether the entry came from `registerCompose` or `register`.

The factory is invoked on every show and receives a `DialogNotifier<R>` that completes or cancels that presentation. With a factory that receives only the view model, the view model itself reports through the extension property `notifier` ([View models](view-models.md)). Only the first report settles the result; later reports do nothing.

A result is `Cancelled` when the notifier reports cancellation, when the overlay is tapped outside the content, when the back button is pressed, or when the screen hosting the Dialog is destroyed.

## Choose a `show`

`KsDialog` (`Dialog.instance` and an injected instance are the same thing) exposes three `show` overloads, and the `ksdialogs-compose` artifact adds the extension function `showCompose`. Two axes decide which to use: whether the view model is passed as an instance or as a type only, and whether the content comes from a registered factory or is supplied at the call site. Every entry takes an optional `placement`, which replaces the whole placement attached to the content when supplied ([Layout](layout.md)). The return value is `DialogResult<R>`, where `R` comes from the view-model declaration.

| Signature | What it does | When to choose it | Registration required |
|---|---|---|---|
| `show(viewModel: DialogViewModel<R>, placement: DialogPlacement? = null)` | Passes a view-model instance you built; a registered factory creates the content | When the caller assembles the view model (passing constructor arguments) | View factory (`register` / `registerCompose`) |
| `show(viewModel: VM, placement: DialogPlacement? = null, factory: Context.(VM, DialogNotifier<R>) -> View)` | Passes both the view-model instance and the View factory at the call site. It neither reads nor changes the registry | One-off View content | None |
| `show(viewModelClass: KClass<VM>, placement: DialogPlacement? = null, configure: (suspend (VM) -> Unit)? = null)` | Passes only the view-model type, then runs `configure` on the instance built by the registered view-model factory before presenting | When the library should build the view model and the state is prepared just before presentation | View factory + view-model factory (`registerViewModel`, see [View models](view-models.md)) |
| `showCompose(viewModel: VM, placement: DialogPlacement? = null, content: @Composable (VM, DialogNotifier<R>) -> Unit)` | Passes both the view-model instance and the Compose content at the call site. It neither reads nor changes the registry | One-off Compose content | None (import from `jp.kamusoft.ksdialogs.compose`) |

There is no entry point that closes a Dialog, and no API equivalent to `hide`. A presentation ends only when the result is settled as listed above.

In the recipes below, `show(ConfirmViewModel(...))` under "Register and call" is the first row, "Show View content without registration" is the second, `show(ItemEditViewModel::class)` under "Return a result type other than Boolean" is the third, and "Show Compose content without registration" is the fourth.

## Declare a view model

A view model declares the result type and holds the values needed for presentation. If reporting the result is left to the content's `notifier`, the view model only needs the value declarations.

```kotlin
import jp.kamusoft.ksdialogs.SimpleDialogViewModel

class ConfirmViewModel(val message: String) : SimpleDialogViewModel
```

## Write the content

The library's container takes care of the overlay and the placement, so the content is only the card itself. In Compose it is written as a `@Composable` function.

```kotlin
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import jp.kamusoft.ksdialogs.DialogNotifier

@Composable
fun ConfirmContent(viewModel: ConfirmViewModel, notifier: DialogNotifier<Boolean>) {
    Column {
        Text(viewModel.message)
        Button(onClick = { notifier.complete(true) }) { Text("OK") }
        Button(onClick = notifier::cancel) { Text("Cancel") }
    }
}
```

Written as a View, it returns a `View` built from a `Context`. Giving the constructor the shape `(Context, VM, DialogNotifier<R>)` keeps the registration lambda to a single line.

```kotlin
import android.content.Context
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import jp.kamusoft.ksdialogs.DialogNotifier

class ConfirmCardView(
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
        addView(
            Button(context).apply {
                text = "Cancel"
                setOnClickListener { notifier.cancel() }
            },
        )
    }
}
```

## Register and call

Register once at startup. In `Application.onCreate`, the registration is in place no matter where in the application `show` is called.

```kotlin
import android.app.Application
import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.compose.registerCompose

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Dialog.instance.registry.registerCompose(ConfirmViewModel::class) { viewModel, notifier ->
            ConfirmContent(viewModel, notifier)
        }
    }
}
```

For View content, call `register` instead of `registerCompose` in the same place.

```kotlin
Dialog.instance.registry.register(ConfirmViewModel::class, ::ConfirmCardView)
```

The caller uses `Dialog.instance` or an instance injected as `KsDialog`. The injected instance can be `Dialog.instance` or a new `Dialog()`; both point at the same process-wide registry and the same state. `show` is a suspend function, so call it from `lifecycleScope` in an Activity.

```kotlin
import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.DialogResult
import kotlinx.coroutines.launch

class ItemActivity : ComponentActivity() {
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        statusText = TextView(this)
        setContentView(statusText)
    }

    fun onDeleteClicked() {
        lifecycleScope.launch {
            val result = Dialog.instance.show(ConfirmViewModel("Delete this item?"))
            statusText.text = when {
                result is DialogResult.Completed && result.value -> "Deleted"
                else -> "Kept"
            }
        }
    }
}
```

## Return a result type other than Boolean

`R` in `DialogViewModel<R>` can be any type, including a data class or `String`. The types of `notifier` and `DialogResult` follow that declaration, so code that reports a different type does not compile.

Registering both a View factory and a view-model factory enables the `show` that takes only the view-model type. The created view model is prepared in `configure` before presentation.

```kotlin
import jp.kamusoft.ksdialogs.DialogViewModel
import jp.kamusoft.ksdialogs.notifier

data class ItemEdit(val name: String, val quantity: Int)

class ItemEditViewModel : DialogViewModel<ItemEdit> {
    var name = ""
    var quantity = 0

    fun save() {
        notifier?.complete(ItemEdit(name, quantity))
    }

    fun cancel() {
        notifier?.cancel()
    }
}
```

Register content that receives only `ItemEditViewModel` and leave reporting to the view model's `save` / `cancel`. `ItemEditContent` is a composable you write yourself, in the same shape as `ConfirmContent`.

```kotlin
Dialog.instance.registry.registerCompose(ItemEditViewModel::class) { viewModel ->
    ItemEditContent(viewModel)
}
Dialog.instance.registry.registerViewModel(ItemEditViewModel::class, ::ItemEditViewModel)
```

To call from a Composable screen, put the call in the screen's view model, which receives `KsDialog`, and expose the result as state.

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jp.kamusoft.ksdialogs.DialogResult
import jp.kamusoft.ksdialogs.KsDialog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ItemScreenViewModel(private val dialogs: KsDialog) : ViewModel() {
    private val mutableStatus = MutableStateFlow("")
    val status = mutableStatus.asStateFlow()

    fun onEditClicked() {
        viewModelScope.launch {
            val result = dialogs.show(ItemEditViewModel::class) { viewModel ->
                viewModel.name = "Apple"
            }
            mutableStatus.value = when (result) {
                is DialogResult.Completed -> "${result.value.name} x${result.value.quantity}"
                DialogResult.Cancelled -> "Cancelled"
            }
        }
    }
}
```

## Show Compose content without registration

Pass Compose content directly to `showCompose` for a one-off Dialog. This route ignores any registration for the same view-model type and does not add, replace, or remove a registry entry.

```kotlin
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.DialogResult
import jp.kamusoft.ksdialogs.SimpleDialogViewModel
import jp.kamusoft.ksdialogs.compose.showCompose

class InlineConfirmViewModel(val label: String) : SimpleDialogViewModel

suspend fun showInlineConfirm(): DialogResult<Boolean> =
    Dialog.instance.showCompose(InlineConfirmViewModel("Continue")) { viewModel, notifier ->
        Button(onClick = { notifier.complete(true) }) {
            Text(viewModel.label)
        }
    }
```

## Show View content without registration

Pass a View factory directly to `show` for one-off content. The factory lambda has an Android `Context` receiver, so `this` inside the lambda is that `Context`; later View factory recipes use the same receiver. This route does not add, replace, or remove a registry entry.

```kotlin
import android.widget.Button
import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.DialogResult
import jp.kamusoft.ksdialogs.SimpleDialogViewModel

class NoticeViewModel(val text: String) : SimpleDialogViewModel

suspend fun showNotice(): DialogResult<Boolean> =
    Dialog.instance.show(NoticeViewModel("Ready")) { viewModel, notifier ->
        Button(this).apply {
            text = viewModel.text
            setOnClickListener { notifier.complete(true) }
        }
    }
```

## Stack independent Dialogs

Start separate `show` calls with separate view-model instances. The later Dialog is in front and each call keeps its own result. On Android, reporting a result to a lower Dialog closes only that one while the upper Dialog remains available. This happens only when the app holds the lower reporting handle and reports through it first — user operations (completion, cancellation, an outside tap, the back button) always reach the frontmost one alone.

```kotlin
import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.DialogResult
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

suspend fun showTwoDialogs(): Pair<DialogResult<Boolean>, DialogResult<Boolean>> = coroutineScope {
    val first = async { Dialog.instance.show(ConfirmViewModel("First")) }
    val second = async { Dialog.instance.show(ConfirmViewModel("Second")) }
    first.await() to second.await()
}
```

## Recover from a configuration mistake

A configuration mistake does not return `Cancelled`; it fails `show` with a nested `DialogException` class. That keeps a missing registration from being mistaken for a user cancellation, and in this case no content is created or presented. Cancelling the calling coroutine is different: it propagates `CancellationException`, and the dismissal animation and container removal still run to completion.

For every exception except `PresentationHostUnavailable`, the type name of the view model in question is readable from `viewModelTypeName`.

| Exception | Message | Cause and remedy |
|---|---|---|
| `DialogException.ViewFactoryNotRegistered` | `No View factory is registered for ViewModel type {TypeName}.` | The content for that view-model type cannot be resolved. Call `register` or `registerCompose` on `Dialog.instance.registry` |
| `DialogException.ViewModelFactoryNotRegistered` | `No ViewModel factory is registered for ViewModel type {TypeName}.` | The type-based `show` has no view-model factory. Call `registerViewModel` (see [View models](view-models.md)) |
| `DialogException.ViewModelAlreadyShowing` | `This ViewModel instance of type {TypeName} is already being shown.` | The same view-model instance is already being shown. Create a new instance for each stacked show |
| `DialogException.ValueClassViewModel` | `ViewModel type {TypeName} is a value class and cannot be used as a ViewModel.` | A value class was used as a view model. Make the view model a class |
| `DialogException.PresentationHostUnavailable` | `No screen is available to present the Dialog.` | There is no resumed Activity. Show after the first screen becomes resumed. Calls are not queued; they fail immediately |

The messages in the table are the values the current implementation returns, not a stable API (what does not change is the exception type and the condition it is thrown under; the wording can change without notice).

For example, showing `ConfirmViewModel` while the startup registration is missing cannot resolve the content and raises `DialogException.ViewFactoryNotRegistered`.

```kotlin
import android.app.Application

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
```

The caller can `catch` the nested exception type directly and read the type name of the unresolved view model from `viewModelTypeName`. A configuration mistake cannot be fixed at runtime, so log it and rethrow to make it visible during development.

```kotlin
import android.util.Log
import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.DialogException
import jp.kamusoft.ksdialogs.DialogResult

suspend fun confirmDeletion(): DialogResult<Boolean> =
    try {
        Dialog.instance.show(ConfirmViewModel("Delete this item?"))
    } catch (exception: DialogException.ViewFactoryNotRegistered) {
        Log.e("MyApp", "Dialog view factory is not registered for ${exception.viewModelTypeName}.")
        throw exception
    }
```

To handle all of them together, including `PresentationHostUnavailable`, which has no `viewModelTypeName`, `catch` the base type `DialogException`.
