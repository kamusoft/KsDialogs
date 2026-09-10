# iOS host integration

## Prerequisite and three setup steps

Prerequisite: a Kotlin Multiplatform shared module with an iOS target, and an Xcode application project that already builds and links its framework.

1. Add `api("jp.kamusoft:ksdialogs-kmp:0.1.0-beta.1")` to the shared module's commonMain dependencies.
2. Run `integrateLinkagePackage` once with `XCODEPROJ_PATH`, then commit the generated `KotlinMultiplatformLinkedPackage/` directory with the project.
3. In Xcode, add `https://github.com/kamusoft/KsDialogs-SPM` to Package Dependencies as an exact requirement on the same version, and link the `KsDialogs` product to the application target.

```bash
XCODEPROJ_PATH="$PWD/iosApp/MyApp.xcodeproj" \
  ./gradlew :shared:integrateLinkagePackage
```

The Maven dependency carries the Swift package reference in its published metadata, and the generated package feeds that reference into Xcode's dependency graph so the shared framework's unresolved Swift symbols link. The direct package entry in step 3 is separate: it is what lets application source call the registration APIs. SwiftPM folds the two into one package identity, so the Swift implementation is not duplicated.

The published metadata pins that Swift package to the exact version of the Maven artifact, and a released version always has a tag of that same version in the Swift package repository, so state that one version in both places and move the two together.

Step 2 is a one-time integration. After that, ordinary Gradle builds refresh the generated package when the dependencies change. Keep the directory in version control — Xcode references it, so it is needed right after a clone.

## Register host content

Shared view models do not conform to the iOS Native view-model contract, so registration goes through the KMP entry: `Dialog.shared.kmp`, `Loading.shared.kmp`, and `Toast.shared.kmp`. They write into the same registries as pure-native registration. Import the generated shared framework by its module name; `Shared` is used below as an example.

```swift
import KsDialogs
import Shared
import UIKit

enum DialogHostRegistration {
    @MainActor
    static func register() {
        Dialog.shared.kmp.register(DeleteViewModel.self) { viewModel, notifier in
            var configuration = UIButton.Configuration.filled()
            configuration.title = "Delete \(viewModel.itemName)"
            let view = UIButton(
                configuration: configuration,
                primaryAction: UIAction { _ in notifier.complete(true) }
            )
            view.ksDialogTransition = DialogTransition.fade()
            return view
        }

        Loading.shared.kmp.register(UploadLoadingViewModel.self) { _ in
            let view = UILabel()
            view.text = "Uploading"
            return view
        }

        Toast.shared.kmp.register(StatusToastViewModel.self) { viewModel in
            let view = UILabel()
            view.text = viewModel.message
            return view
        }
    }
}
```

Call `DialogHostRegistration.register()` once from a main-actor startup path, before shared code shows anything. Re-registering the same class replaces its factory, and the factory runs on every show. UIKit and SwiftUI factories share the same method name and differ only in return type, and both behave identically from the caller's side.

This Swift-facing entry has no display that takes a view-model class. The class-based `show` and `registerViewModel` of shared code are for shared Kotlin code only and do not appear in the generated framework's Objective-C header either ([View models](view-models.md)). To show a shared view model from Swift, build the Kotlin view model in Swift and call the instance-based `show`.

## Declare the result type and read the notifier

A shared view model declares its result type in Kotlin, where Swift cannot see it, so this entry takes the result type as an argument and falls back to `Bool` when it is omitted. Pass the same type the shared `DialogViewModel<R>` declares; a mismatch surfaces as a typed failure when the result is restored.

| Form | What it means |
|---|---|
| `register(DeleteViewModel.self) { viewModel, notifier in … }` | Result type is `Bool` |
| `register(ChoiceViewModel.self, result: String.self) { … }` | Result type stated explicitly |
| `show(viewModel, placement:)` / `show(viewModel, result:placement:)` | Show from Swift; `result:` defaults to `Bool` |
| `notifier(for:)` / `notifier(for:result:)` | Read the reporting handle of a shared view model that is currently showing |

A one-argument factory reads the handle itself:

```swift
import KsDialogs
import Shared
import UIKit

Dialog.shared.kmp.register(ChoiceViewModel.self, result: String.self) { viewModel in
    let notifier = try? Dialog.shared.kmp.notifier(
        for: viewModel,
        result: String.self
    )
    let view = UIButton(type: .system)
    view.setTitle(viewModel.title, for: .normal)
    view.addAction(
        UIAction { _ in notifier?.complete("accepted") },
        for: .touchUpInside
    )
    return view
}
```

`notifier(for:result:)` returns `nil` outside a show and throws when the requested result type disagrees with the registration, so "not showing" is never confused with "wrong type"; `try?` collapses that failure into `nil`, so a type mismatch leaves this button unable to report. The result type used while showing is fixed by the registration in force when that show started.

## Tell the failures of a Swift-side show apart

Showing through a KMP entry is a Swift throwing call, so a misconfiguration reaches `catch` instead of turning into a result. Two types arrive there. Failures about the binding of a shared view model and its result type arrive as `KsDialogsKmpError`, which only the Dialog entry maps to; every other failure, and the failures of the Loading and Toast entries, arrive as the library-wide `DialogError`. Both are thrown by the Swift entry itself, which is a different route from the Kotlin exceptions that reach Swift as `NSError` in the next section.

| Failure | Entry that throws it | Cause and remedy |
|---|---|---|
| `KsDialogsKmpError.notRegistered(viewModelType:)` | `Dialog.shared.kmp.show` | No content is registered for that shared view-model class. Run the startup registration first |
| `KsDialogsKmpError.resultTypeMismatch(expected:actual:)` | `Dialog.shared.kmp.show`, `notifier(for:result:)` | The requested result type disagrees with the registration. Match `result:` to the type of the shared `DialogViewModel<R>` |
| `DialogError.viewFactoryNotRegistered(viewModelType:)` | `Loading.shared.kmp.show`, `Toast.shared.kmp.show` | No content is registered for that shared view-model class. The Loading and Toast entries throw this failure as it is instead of mapping it |
| `DialogError.presentationHostUnavailable` | `Dialog.shared.kmp.show` | No screen can present it yet. Call once a screen is on display |
| `DialogError.viewModelAlreadyShowing(viewModelType:)` | `Dialog.shared.kmp.show` | The same view-model instance is shown again while it is showing. Create a new instance per call; the dialog already on screen is unaffected |

The Dialog entry throws the first two as `KsDialogsKmpError` and leaves the rest as `DialogError`. Nothing else is mapped, so write two `catch` clauses when the two kinds need different handling.

```swift
import KsDialogs
import Shared

@MainActor
func confirmDeleteFromSwift() async -> Bool {
    do {
        switch try await Dialog.shared.kmp.show(DeleteViewModel(itemName: "Report")) {
        case .completed(let value):
            return value
        case .cancelled:
            return false
        }
    } catch let error as KsDialogsKmpError {
        assertionFailure("The registration or the result type does not match: \(error)")
        return false
    } catch {
        assertionFailure("Could not present the dialog: \(error)")
        return false
    }
}
```

## Add `@Throws` at the Swift boundary

Any shared function called from Swift that can let an exception escape needs `@Throws`, whether it is suspend or non-suspend. Without the annotation, Kotlin exceptions are not converted to `NSError`: a suspend function terminates the process with an uncaught exception, and a non-suspend function lets nothing reach Swift at all.

The library declares only the failures each route can report. Its message-only Loading and Toast routes declare none.

| Shared library call | Declared exceptions |
|---|---|
| `Dialog.instance.show(viewModel)` | `DialogException`, `CancellationException` |
| `Loading.instance.show(viewModel)` | `DialogException`, `CancellationException` |
| `Loading.instance.start(viewModel, action)` | `DialogException`, `CancellationException` |
| `Toast.instance.show(viewModel)` | `DialogException` |
| `Loading.instance.show(message)`, `Loading.instance.start(message, action)`, `Toast.instance.show(message)` | none |

The class-based `show` and `start` are not visible from Swift, so they appear neither in this table nor in `@Throws`.

Annotate your own exported wrapper with each exception that wrapper can expose.

An exported wrapper placed in the shared module looks like this. The suspend wrapper around `Dialog.instance.show(viewModel)` is annotated with the two exceptions from the table, while the wrapper around the message-only Toast route is annotated with nothing.

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.Dialog
import jp.kamusoft.ksdialogs.kmp.DialogException
import jp.kamusoft.ksdialogs.kmp.DialogResult
import jp.kamusoft.ksdialogs.kmp.KsDialog
import jp.kamusoft.ksdialogs.kmp.KsToast
import jp.kamusoft.ksdialogs.kmp.Toast
import kotlin.coroutines.cancellation.CancellationException

class DeletePresenter(
    private val dialogs: KsDialog = Dialog.instance,
    private val toast: KsToast = Toast.instance,
) {
    @Throws(DialogException::class, CancellationException::class)
    suspend fun confirmDelete(itemName: String): String =
        when (val result = dialogs.show(DeleteViewModel(itemName))) {
            is DialogResult.Completed -> if (result.value) "Deleted" else "Kept"
            DialogResult.Cancelled -> "Cancelled"
        }

    fun notifyDeleted(itemName: String) {
        toast.show("Deleted $itemName")
    }
}
```

Calling that wrapper from Swift looks like this. The annotated `confirmDelete` becomes a Swift throwing function, so it is called with `try await` and its failure arrives in `catch` as an `NSError`. The unannotated `notifyDeleted` is called without `try`.

```swift
import Shared

@MainActor
func confirmDelete(itemName: String) async -> String? {
    let presenter = DeletePresenter()
    do {
        let outcome = try await presenter.confirmDelete(itemName: itemName)
        presenter.notifyDeleted(itemName: itemName)
        return outcome
    } catch {
        assertionFailure("Could not confirm the delete: \(error)")
        return nil
    }
}
```

When the shared module's `iosMain` overrides a member that a commonMain interface declares with `@Throws`, such as substituting `KsDialog`, `KsLoading`, or `KsToast` with a test double, do not repeat `@Throws` on the overriding member. An override inherits the declaration from the interface, so the Swift throwing contract is unchanged, and repeating it makes the metadata compilation of the native intermediate source sets fail on Kotlin 2.4.x (fixed in Kotlin 2.5.0). This happens with any of these contracts, including an override of the generic `KsDialog.show`. Overrides written in `commonMain` are not affected and may repeat the annotation.

A substitute placed in `iosMain` looks like this. `KsToast.show(viewModel, …)` declares `@Throws(DialogException::class)`, but the overriding member carries no annotation. The contract also has `registry` and the class-based `show`, so a substitute supplies its own table of view-model factories.

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.DialogPlacement
import jp.kamusoft.ksdialogs.kmp.KsToast
import jp.kamusoft.ksdialogs.kmp.ToastViewModel
import jp.kamusoft.ksdialogs.kmp.ToastViewRegistry
import kotlin.reflect.KClass

class RecordingKsToast : KsToast {
    val shown: MutableList<ToastViewModel> = mutableListOf()

    private val factories: MutableMap<KClass<*>, () -> ToastViewModel> = mutableMapOf()

    override val registry: ToastViewRegistry = object : ToastViewRegistry {
        override fun <VM : ToastViewModel> registerViewModel(
            viewModelClass: KClass<VM>,
            factory: () -> VM,
        ) {
            factories[viewModelClass] = factory
        }
    }

    override fun show(message: String, durationMs: Int?, placement: DialogPlacement?) = Unit

    override fun show(viewModel: ToastViewModel, durationMs: Int?, placement: DialogPlacement?) {
        shown += viewModel
    }

    override fun <VM : ToastViewModel> show(
        viewModelClass: KClass<VM>,
        durationMs: Int?,
        placement: DialogPlacement?,
        configure: ((VM) -> Unit)?,
    ) {
        @Suppress("UNCHECKED_CAST")
        val viewModel = factories.getValue(viewModelClass)() as VM
        configure?.invoke(viewModel)
        show(viewModel, durationMs, placement)
    }
}
```

## Continue with feature recipes

- [Layout](layout.md) for the options and placement you attach to the returned content.
- [Transitions](transitions.md) for the enter and exit animation.
- [Dialog](dialogs.md), [Loading](loading.md), and [Toast](toast.md) for the shared-code side.
