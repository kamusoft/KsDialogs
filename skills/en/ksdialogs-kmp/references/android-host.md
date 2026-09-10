# Android host integration

The shared view-model contracts are type aliases of the Android Native ones, so the Android host uses the native API directly and needs no KMP-specific entry point. Content is registered on `Dialog.instance.registry` (a `DialogViewRegistry`), `Loading.instance.registry` (a `LoadingViewRegistry`), and `Toast.instance.registry` (a `ToastViewRegistry`). The three are independent: a class registered for Dialog is not registered for Loading or Toast.

## Register View content

Put this in the Android application's startup path. The shared view-model classes used below are defined in the feature recipes.

```kotlin
import android.widget.Button
import android.widget.TextView
import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.DialogTransition
import jp.kamusoft.ksdialogs.Loading
import jp.kamusoft.ksdialogs.Toast
import jp.kamusoft.ksdialogs.ksDialogTransition
import jp.kamusoft.ksdialogs.notifier

object DialogHostRegistration {
    fun register() {
        Dialog.instance.registry.register(DeleteViewModel::class) { viewModel, notifier ->
            Button(this).apply {
                text = "Delete ${viewModel.itemName}"
                setOnClickListener { notifier.complete(true) }
                ksDialogTransition = DialogTransition.fade()
            }
        }

        Dialog.instance.registry.register(ChoiceViewModel::class) { viewModel ->
            Button(this).apply {
                text = viewModel.title
                setOnClickListener { viewModel.notifier?.complete("accepted") }
            }
        }

        Loading.instance.registry.register(UploadLoadingViewModel::class) {
            TextView(this).apply { text = "Uploading" }
        }

        Toast.instance.registry.register(StatusToastViewModel::class) { viewModel ->
            TextView(this).apply { text = viewModel.message }
        }
    }
}
```

The factory receiver is the presenting screen's `Context`, and it runs on every show, so the content is new each time. A Dialog factory either takes a second `DialogNotifier<R>` argument or reads `viewModel.notifier`, which is bound before the factory runs and is null outside a show. Loading and Toast factories receive their view model only. A returned View carries its own layout and transition attachments; when the View constructor is `(Context, VM)`, a constructor reference works as the one-argument factory.

## Register Compose content

The KMP artifact brings the Android Views artifact `jp.kamusoft:ksdialogs-core` in transitively, but not the Compose extensions. Add the Compose artifact `jp.kamusoft:ksdialogs` to the Android application — it brings the core artifact along — and use each registry's `registerCompose` overload; the name differs from `register` because a `@Composable` function type and a plain one cannot be overloaded together without making the call site ambiguous.

```kotlin
dependencies {
    implementation("jp.kamusoft:ksdialogs:<version>")
    implementation("androidx.compose.foundation:foundation:1.8.1")
}
```

```kotlin
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.Loading
import jp.kamusoft.ksdialogs.Toast
import jp.kamusoft.ksdialogs.compose.registerCompose
import jp.kamusoft.ksdialogs.notifier

object ComposeHostRegistration {
    fun register() {
        Dialog.instance.registry.registerCompose(ChoiceViewModel::class) { viewModel ->
            BasicText(
                text = viewModel.title,
                modifier = Modifier.clickable {
                    viewModel.notifier?.complete("accepted")
                },
            )
        }

        Loading.instance.registry.registerCompose(UploadLoadingViewModel::class) {
            BasicText("Uploading")
        }

        Toast.instance.registry.registerCompose(StatusToastViewModel::class) { viewModel ->
            BasicText(viewModel.message)
        }
    }
}
```

Showing registered content still goes through the same `show` and `start` calls, whichever technology the factory used. Attributes are declared inside the composable with `KsDialogAttributes`, which also ships in `jp.kamusoft:ksdialogs`; see [Layout](layout.md) and [Transitions](transitions.md).

## Call registration at startup

```kotlin
import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        DialogHostRegistration.register()
    }
}
```

Register once before any commonMain `show` or `start`. Registration is thread-safe, and re-registering the same class replaces that slot's previous factory. If the application uses Compose content, call `ComposeHostRegistration.register()` at the same point.

## Register a view-model factory and show by type

A registry entry has two slots per view-model class: the View factory, which every route uses, and a view-model factory, which lets the library build the view model itself. Once both slots are filled, Android UI code passes the class instead of an instance, and the library creates the view model, completes `configure`, and only then calls the View factory — so the content reads state that `configure` already wrote. The slots you fill here belong to the Android Native registry, which is separate from the table of view-model factories that shared code holds ([View models](view-models.md)). Re-registering replaces only the slot you registered, and the resolution uses a snapshot of the entry taken when the call is made, so re-registering while something is on screen does not change what is showing.

Give the shared view model the state that `configure` writes.

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.LoadingProgressReceiver
import jp.kamusoft.ksdialogs.kmp.LoadingViewModel

class ImportLoadingViewModel : LoadingViewModel, LoadingProgressReceiver {
    var title = ""
    var progress = 0.0
        private set

    override fun onProgress(progress: Double) {
        this.progress = progress
    }
}
```

Fill both slots in the Android startup path, then show by class. `Loading` has the scoped `start(VM::class)` as well; the Dialog form is `Dialog.instance.show(VM::class)` and the Toast form is `Toast.instance.show(VM::class)`. For Loading the progress receiver is bound after `configure` and before the View factory, so a view model created this way receives progress exactly as an instance-passing one does.

```kotlin
import android.widget.TextView
import jp.kamusoft.ksdialogs.Loading

object ImportLoadingRegistration {
    fun register() {
        Loading.instance.registry.register(ImportLoadingViewModel::class) { viewModel ->
            TextView(this).apply { text = viewModel.title }
        }
        Loading.instance.registry.registerViewModel(ImportLoadingViewModel::class, ::ImportLoadingViewModel)
    }
}

suspend fun importLibrary(source: ImportSource): Int =
    Loading.instance.start(ImportLoadingViewModel::class, configure = { viewModel ->
        viewModel.title = "Importing"
    }) { report ->
        source.importAll(onProgress = report)
    }
```

`configure` may suspend for Dialog and Loading, and is synchronous for Toast because its `show` is fire-and-forget. A view model that a dependency-injection container builds is expressed inside the view-model factory; the library never looks at a container. Shared code has the same `show(VM::class)` form, but it reads a different table of view-model factories, so register in shared code when you show from shared code ([View models](view-models.md)). The Swift entry of the iOS host does not have this form.

## Style the built-in Loading and Toast

Colours never cross the shared-code boundary, so the built-in visuals are configured on the Android entries. Both style types are `data class`es, so `copy(...)` changes one field at a time. The container reads them when each display starts: a change applies to the next display, not to one already on screen.

```kotlin
Loading.instance.style = Loading.instance.style.copy(
    indicatorColor = Color.WHITE,
    messageFontSize = 16.0,
    messageColor = Color.WHITE,
    defaultMessage = "Working",
    progressFormat = LoadingStyle.DEFAULT_PROGRESS_FORMAT,
)
Loading.instance.options = DialogOptions(proportionalWidth = 0.5)

Toast.instance.style = Toast.instance.style.copy(
    backgroundColor = ToastStyle.BUILTIN_BACKGROUND_COLOR,
    textColor = Color.WHITE,
    fontSize = 16.0,
    cornerRadius = 20.0,
    defaultDuration = ToastStyle.BUILTIN_DEFAULT_DURATION,
    defaultPlacement = DialogPlacement(verticalAlignment = DialogAlignment.END, offsetY = -120.0),
)
```

`Loading.instance.options` takes the same `DialogOptions` used for dialog attachment and stands in for the attachment the built-in loading content has no place for; its outside-tap field stays inactive. `defaultPlacement` is the app-wide fallback placement for every toast, which is how an app avoids its own bottom bar. `progressFormat` is a `(String?, Double?) -> String` function, not a format string.

## Configuration failures

`show` returns `DialogResult.Completed` or `DialogResult.Cancelled`; a misconfiguration throws instead, and the type says which one.

| Situation | Exception |
|---|---|
| No View factory registered for the class (Dialog, Loading, or Toast) | `DialogException.ViewFactoryNotRegistered` |
| No screen available to present on | `DialogException.PresentationHostUnavailable` |
| No ViewModel factory registered for a type-based show (Dialog, Loading, or Toast) | `DialogException.ViewModelFactoryNotRegistered` |
| The same view-model instance is already showing | `DialogException.ViewModelAlreadyShowing` |
| A value class was used as a view model | `DialogException.ValueClassViewModel` |

On a type-based show, an exception thrown by the view-model factory or by `configure` is a Dialog and Loading failure that propagates to the caller with nothing presented, and a typed `start` does not run its action. Toast reports only the missing view-model factory that way: because its `show` has already returned, an exception from the factory or from `configure` cannot reach the caller, so that one toast is dropped with a warning log.

Cancelling the calling coroutine propagates `CancellationException`; the dialog still finishes its exit animation and removal.

## Android-only entry points

These exist on the Android surface but have no counterpart in shared code, so reach for them only in Android-specific UI:

| Entry point | What it adds |
|---|---|
| `show(viewModel, placement, factory)` and `showCompose(viewModel, placement, content)` | Show content inline without touching the registry; `Loading` also has `startCompose` |
| `SimpleDialogViewModel` | Alias of `DialogViewModel<Boolean>`; shared code writes the type argument out instead |
