# Report a result from a view model

The result of a Dialog can be reported by the view model being shown, through the extension property `notifier`. This document covers how to read `notifier` and how to use the `show` that takes only the view-model type.

## Read `notifier`

`notifier` holds a value only while the view model is being shown. Inside the view model itself, write `notifier` and call it with `?.`.

| Item | Detail |
|---|---|
| Type | `DialogNotifier<R>` for the result type the view model declared |
| Before and after the show | `null` (it returns a value only while shown) |
| When it is removed | Removed at any terminal state: completion, cancellation, or failure |
| Which show routes bind it | All of them: instance, inline factory, and type-based |
| Relation to the two-argument factory | The notifier read from the view model and the factory's notifier argument point at the same delivery target |

The following view model has a Boolean result, where `accept` reports completion and `cancel` reports cancellation.

```kotlin
import jp.kamusoft.ksdialogs.SimpleDialogViewModel
import jp.kamusoft.ksdialogs.notifier

class ConfirmViewModel(val message: String) : SimpleDialogViewModel {
    fun accept() {
        notifier?.complete(true)
    }

    fun cancel() {
        notifier?.cancel()
    }
}
```

When reporting is left to the view model, the content can be registered in the form that receives only the view model.

```kotlin
import android.app.Application
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.compose.registerCompose

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Dialog.instance.registry.registerCompose(ConfirmViewModel::class) { viewModel ->
            Button(onClick = viewModel::accept) { Text(viewModel.message) }
        }
    }
}
```

View content can be passed to `register` in the same single-argument form. For a View whose constructor is `(Context, VM)`, a constructor reference works as the factory directly.

## Make the view model a class

Because notifier binding is looked up by instance identity, a view model must be a class.

- A value class cannot be used, because identity is lost on every boxing. It is rejected with `DialogException.ValueClassViewModel` both at registration time and at every presentation, including inline factories
- Separate instances that compare equal (`equals`) do not interfere with each other. Each has its own `notifier`

## Do not stack the same instance

One instance can hold only one binding.

- Showing the same instance concurrently fails with `DialogException.ViewModelAlreadyShowing`
- The failure does not affect the Dialog already on screen
- Separate instances of the same type stack independently (see "Stack independent Dialogs" in [Dialog](dialogs.md))
- The binding is released at every terminal state, so the same instance can be shown again right after a failure

## Create and configure a view model by type

The type-based `show` takes only the view-model type, then runs `configure` on the instance built by the registered view-model factory before presenting.

### Required registrations

A registry entry has two independent slots.

| Slot | API that fills it | Which show uses it |
|---|---|---|
| View factory | `register` / `registerCompose` | Both instance-based and type-based |
| View-model factory | `registerViewModel` | Type-based only |

- The type-based `show` needs both. Without a view-model factory it fails with `DialogException.ViewModelFactoryNotRegistered` before any content is created
- Re-registering replaces only the slot it touches; the other one remains
- Resolution is a snapshot taken when `show` is called, so re-registering while a Dialog is shown does not affect the one on screen
- To take the view model from a DI container, call the container inside the `registerViewModel` factory

### The order from creation to presentation

The order is fixed as follows.

1. The view-model factory creates the view model (Main dispatcher)
2. `configure` completes (it can be written as `suspend`)
3. `notifier` is bound
4. The View factory creates the content
5. The Dialog is presented

The state set by `configure` is therefore always readable from the content's initialization. A failure thrown by the view-model factory or by `configure` does not become `Cancelled`: it propagates to the caller without proceeding to presentation.

The following registers both slots for a view model that declares `String` as its result type, and presents it after the type-based `show` sets `prompt` in `configure`.

```kotlin
import jp.kamusoft.ksdialogs.DialogViewModel
import jp.kamusoft.ksdialogs.notifier

class ProfileViewModel : DialogViewModel<String> {
    var prompt = ""

    fun save(name: String) {
        notifier?.complete(name)
    }
}
```

```kotlin
import android.app.Application
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.compose.registerCompose

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Dialog.instance.registry.registerCompose(ProfileViewModel::class) { viewModel ->
            Button(onClick = { viewModel.save("Ada") }) { Text(viewModel.prompt) }
        }
        Dialog.instance.registry.registerViewModel(ProfileViewModel::class, ::ProfileViewModel)
    }
}
```

The caller does not assemble a view-model instance; it passes only the type and `configure`.

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jp.kamusoft.ksdialogs.DialogResult
import jp.kamusoft.ksdialogs.KsDialog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileScreenViewModel(private val dialogs: KsDialog) : ViewModel() {
    private val mutableStatus = MutableStateFlow("")
    val status = mutableStatus.asStateFlow()

    fun onEditClicked() {
        viewModelScope.launch {
            val result = dialogs.show(ProfileViewModel::class) { viewModel ->
                viewModel.prompt = "Continue?"
            }
            mutableStatus.value = when (result) {
                is DialogResult.Completed -> result.value
                DialogResult.Cancelled -> "Cancelled"
            }
        }
    }
}
```

`configure` can be written as `suspend`, so the initial state can also be loaded before presentation. The content is created only after it completes.

## Loading and Toast use the same shape

The type-based `show` is not a Dialog-only route. Loading has a type-based `show` and a type-based `start`, Toast has a type-based `show`, and all of them share with Dialog the order "the registered view-model factory creates it, `configure` completes, the content is created, it is presented" and the resolution from a snapshot taken at the call. The three registries are independent, and each has its own `registerViewModel`. The recipes are in [Loading](loading.md) and [Toast](toast.md).

The differences come from the nature of each feature.

| Feature | `configure` | Binding inserted before content creation | Failure thrown by the view-model factory or `configure` |
|---|---|---|---|
| Dialog | Can be written as `suspend` | `notifier` | Propagates to the caller without proceeding to presentation |
| Loading (`show` / `start`) | Can be written as `suspend` | The progress receiver. The coalescing decision comes after `configure` completes, and the view model of a call that joins an existing display is not used | Proceeds to neither presentation nor coalescing and propagates to the caller; with `start` the operation is not run either |
| Toast | Synchronous only, because `show` is a synchronous call that returns nothing | None | Does not reach the caller, because `show` has already returned; it is logged and only that one display is discarded |

For calling code the type-based `show` is an addition: instance-based `show`, inline factories, and two-argument factory registration keep working. A type that implements `KsDialog`, `KsLoading`, or `KsToast` itself (a test double or an adapter) does gain the type-based `show` members, so it must implement them when recompiled.
