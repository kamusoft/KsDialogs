# iOS host integration

## Prerequisite and three setup steps

Prerequisite: a Kotlin Multiplatform shared module with an iOS target, and an Xcode application project that already builds and links its framework.

1. Add `api("jp.kamusoft:ksdialogs-kmp:{version}")` to the shared module's commonMain dependencies.
2. Run `integrateLinkagePackage` once with `XCODEPROJ_PATH`, then commit the generated `KotlinMultiplatformLinkedPackage/` directory with the project.
3. In Xcode, add `https://github.com/kamusoft/KsDialogs-SPM` to Package Dependencies as an exact requirement on the same version, and link the `KsDialogs` product to the application target.

`{version}` in step 1 is a placeholder: replace it with the version you want to use, or resolution fails. To find the current version, open the [latest release](https://github.com/kamusoft/KsDialogs/releases/latest) page, which always resolves to the most recent release.

```bash
XCODEPROJ_PATH="$PWD/iosApp/MyApp.xcodeproj" \
  ./gradlew :shared:integrateLinkagePackage
```

The Maven dependency carries the Swift package reference in its published metadata, and the generated package feeds that reference into Xcode's dependency graph so the shared framework's unresolved Swift symbols link. The direct package entry in step 3 is separate: it is what lets application source call the registration APIs. SwiftPM folds the two into one package identity, so the Swift implementation is not duplicated.

The published metadata pins that Swift package to the exact version of the Maven artifact, and a released version always has a tag of that same version in the Swift package repository, so state that one version in both places and move the two together.

Step 2 is a one-time integration. After that, ordinary Gradle builds refresh the generated package when the dependencies change. Keep the directory in version control — Xcode references it, so it is needed right after a clone.

Building the Swift package import with Xcode 27.0 requires the consumer's Kotlin Gradle Plugin to be 2.4.20 or later. On 2.4.10 the import task fails with `clang: error: unknown argument`. After raising the Kotlin version, run `integrateLinkagePackage` again and commit the files it adds to the generated package as well; from Kotlin 2.4.20 on, `include/module.modulemap` is added.

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

Call `DialogHostRegistration.register()` once from a main-actor startup path, before shared code shows anything. Re-registering the same class replaces its factory, and the factory runs on every show. UIKit and SwiftUI factories share the same method name and differ only in return type. Each comes as a two-argument factory that receives the view model and the notifier, and as a one-argument factory that receives the view model only. Showing, results, and failures behave the same whichever you write; only the safe area that the content sees differs ([Layout](layout.md)).

This Swift-facing entry has no display that takes a view-model class. The class-based `show` and `registerViewModel` of shared code are for shared Kotlin code only and do not appear in the generated framework's Objective-C header either ([View models](view-models.md)). To show a shared view model from Swift, build the Kotlin view model in Swift and call the instance-based `show`.

## Declare the result type and read the notifier

A shared view model declares its result type in Kotlin, where Swift cannot see it, so this entry takes the result type as an argument and falls back to `Bool` when it is omitted. Pass the same type the shared `DialogViewModel<R>` declares. When Swift shows the view model, a mismatch surfaces as a typed failure at the moment the reported value is taken out as the `result:` type. A show started from shared code does not check it ([Use a Kotlin integer as the result type](#use-a-kotlin-integer-as-the-result-type)).

| Form | What it means |
|---|---|
| `register(DeleteViewModel.self) { viewModel, notifier in … }` | Result type is `Bool` |
| `register(ChoiceViewModel.self, result: String.self) { … }` | Result type stated explicitly |
| `show(viewModel, placement:)` / `show(viewModel, result:placement:)` | Show from Swift (`async throws`; waits until the Dialog closes and returns `DialogResult` of the result type). `result:` defaults to `Bool` |
| `notifier(for:)` / `notifier(for:result:)` | Read the reporting handle of a shared view model that is currently showing. Content built by a one-argument factory, which receives no notifier, uses it to report a result |

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

`notifier(for:result:)` returns `nil` outside a show and throws when the requested result type disagrees with the registration, so "not showing" is never confused with "wrong type"; `try?` collapses that failure into `nil`, so a type mismatch leaves this button unable to report. The result type used while showing is fixed by the registration in force when that show started, even if the same class is registered again with another result type while it shows.

## Use a Kotlin integer as the result type

When a shared view model declares a Kotlin integer type as its result type, Swift sees a fixed-width integer, so pass that type to `result:`.

| Result type of the shared view model (Kotlin) | Type to pass to `result:` (Swift) |
|---|---|
| `Int` | `Int32` |
| `Long` | `Int64` |
| `UInt` | `UInt32` |
| `ULong` | `UInt64` |

A shared view model whose result is a Kotlin `Int`:

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.DialogViewModel

class QuantityViewModel(val itemName: String) : DialogViewModel<Int>
```

The Swift host registers and shows it with `Int32`:

```swift
import KsDialogs
import Shared
import SwiftUI

@MainActor
func registerQuantityPicker() {
    Dialog.shared.kmp.register(QuantityViewModel.self, result: Int32.self) { viewModel, notifier in
        VStack(spacing: 12) {
            Text("How many \(viewModel.itemName)?")
            HStack {
                ForEach([Int32(1), 2, 3], id: \.self) { quantity in
                    Button("\(quantity)") { notifier.complete(quantity) }
                }
            }
        }
        .padding()
    }
}

@MainActor
func pickQuantity(itemName: String) async throws -> Int32? {
    switch try await Dialog.shared.kmp.show(QuantityViewModel(itemName: itemName), result: Int32.self) {
    case .completed(let quantity):
        return quantity
    case .cancelled:
        return nil
    }
}
```

Swift `Int` and `UInt` correspond to no Kotlin integer type. Passing `Int.self` or `UInt.self` to an entry that takes `result:` — the `register` overloads, `show(_:result:placement:)`, and `notifier(for:result:)` — compiles, but raises an English deprecation warning that names the type to use (`Int32` / `Int64` for `Int`, `UInt32` / `UInt64` for `UInt`). The warning is backed by a failure at run time:

| Entry | What happens at run time with `Int` / `UInt` |
|---|---|
| `register(_:result:…)` | Registers without failing |
| `show(_:result:placement:)`, including `show(_:placement:)` without `result:` | When the requested result type, or the result type registered for that shared view-model class at the moment `show` is called, is `Int` / `UInt`, it throws `KsDialogsKmpError.resultTypeMismatch` without presenting anything (the content factory is not called). When both are, the failure reports the requested type |
| `notifier(for:result:)` | When the requested result type is `Int` / `UInt`, it throws `KsDialogsKmpError.resultTypeMismatch` before checking whether the view model is showing or what was registered, so it throws even outside a show instead of returning `nil` |

The failure's `expected` holds the type to use (`"Int32 or Int64"` or `"UInt32 or UInt64"`) and `actual` holds the offending type (`"Int"` or `"UInt"`), so the error text itself names the fix. Calling through your own generic function that takes the result type as a type parameter hides the warning, but the run-time check still applies.

These checks cover only the Swift-side entries. When shared code shows the view model (`Dialog.instance.show(...)`), the result type registered in Swift is not checked: shared code receives the reported value as the view model's result type as it is, so a mismatch first surfaces as a `ClassCastException` when shared code uses the value as its Kotlin type. A view model registered with Swift `Int` is also presented on this route and fails with the same `ClassCastException`. Match the Swift registration to the shared declaration even when only shared code shows the Dialog.

## Tell the failures of a Swift-side show apart

Showing through a KMP entry is a Swift throwing call, so a misconfiguration reaches `catch` instead of turning into a result. Two types arrive there. Failures about the binding of a shared view model and its result type arrive as `KsDialogsKmpError`, which only the Dialog entry maps to; every other failure, and the failures of the Loading and Toast entries, arrive as the library-wide `DialogError`. Both are thrown by the Swift entry itself, which is a different route from the Kotlin exceptions that reach Swift as `NSError` in the next section.

| Failure | Entry that throws it | Cause and remedy |
|---|---|---|
| `KsDialogsKmpError.notRegistered(viewModelType:)` | `Dialog.shared.kmp.show` | No content is registered for that shared view-model class. Run the startup registration first |
| `KsDialogsKmpError.resultTypeMismatch(expected:actual:)` | `Dialog.shared.kmp.show`, `notifier(for:result:)` | The requested result type disagrees with the registration, or the requested or registered result type is Swift `Int` / `UInt` (then `show` fails before presenting). Match `result:` to the type of the shared `DialogViewModel<R>`, using the fixed-width types for Kotlin integers |
| `DialogError.viewFactoryNotRegistered(viewModelType:)` | `Loading.shared.kmp.show`, `Toast.shared.kmp.show` | No content is registered for that shared view-model class. The Loading and Toast entries throw this failure as it is instead of mapping it |
| `DialogError.viewModelAlreadyShowing(viewModelType:)` | `Dialog.shared.kmp.show` | The same view-model instance is shown again while it is showing. Create a new instance per call; the dialog already on screen is unaffected |

The Dialog entry throws the first two as `KsDialogsKmpError` and leaves the rest as `DialogError`. Nothing else is mapped, so write two `catch` clauses when the two kinds need different handling.

Having no screen to present on is not a failure. The iOS presentation target is the key window of the foreground active scene, and `show` waits until one exists. The wait has no upper bound, so when you call it from a place where no screen may appear, keep the calling Task cancellable. Cancelling it makes `show` return `.cancelled`, whether it is waiting or showing.

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

## Tell the library which view is the current page

A Dialog whose content has `DialogLayoutArea.currentPage` attached is placed against the current page ([Layout](layout.md)). The page is resolved by asking the sources below in order, moving down when a source has no candidate. When none yields a page, the result is the same as `.visibleArea`, and the reason goes to an English warning log.

| Order | Source | Name to write |
|---|---|---|
| 1 | SwiftUI modifier | `View.markAsDialogCurrentPage()` |
| 2 | Function registered for UIKit | `DialogCurrentPage.provider` (`(@MainActor () throws -> UIView?)?`) |
| 3 | Built-in lookup | none (nothing to register) |

Whichever source is used, the reference is the inside of the page view's safe area intersected with the visible area, so the bars are excluded even when the view extends under them.

### Rely on the built-in lookup

With nothing registered, the library walks the view controllers of the window that presents the Dialog, descending through presented screens, the top of a `UINavigationController`, and the selected tab of a `UITabBarController`; the controller it ends on is the page. Stacked Dialog containers are skipped, so a second Dialog still uses the screen behind as its reference. The lookup reaches only as far as UIKit containers: when the root is a SwiftUI screen, it does not descend into `TabView` or `NavigationStack`, and the tab bar is not avoided.

### Mark a SwiftUI screen

Apply it once to the content frame of each screen. The marked view is a candidate only while it is on screen, and it takes precedence over the registered function and the built-in lookup. With several candidates, the inner one wins when they are nested, otherwise the one placed on screen last; views not visible on screen (such as the leaving tab during a `TabView` switch) are dropped.

```swift
import KsDialogs
import SwiftUI

struct RootView: View {
    var body: some View {
        TabView {
            NavigationStack {
                OrdersScreen()
                    .markAsDialogCurrentPage()
                    .navigationTitle("Orders")
            }
            .tabItem { Label("Orders", systemImage: "list.bullet") }

            SettingsScreen()
                .markAsDialogCurrentPage()
                .tabItem { Label("Settings", systemImage: "gearshape") }
        }
    }
}
```

### Register a function for a custom UIKit container

When screens are switched by a custom container the built-in lookup cannot reach, register once a function that returns the current page's view. Assigning `nil` returns to the built-in lookup. The function is called on the main thread when each display starts and, while it shows, whenever the window size or safe area changes; replacing it takes effect from the next display.

```swift
import KsDialogs
import UIKit

final class PagerViewController: UIViewController {
    private var visibleChild: UIViewController?

    override func viewDidLoad() {
        super.viewDidLoad()
        DialogCurrentPage.provider = { [weak self] in
            self?.visibleChild?.view
        }
    }
}
```

When the function returns `nil`, throws, returns a view that is not on the window presenting the Dialog, returns a view whose safe-area inside is empty, or returns a view outside the window, the built-in lookup's result is used.

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

The class-based `show` and `start` are not visible from Swift, so they appear neither in this table nor in `@Throws`. When Swift calls `Loading.instance.start` directly, pass `actionThread` explicitly: Kotlin default arguments disappear when exported to Swift, while a shared-code wrapper can still rely on the default ([Loading](loading.md)).

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

- [Layout](layout.md) for the options and placement you attach to the returned content, including placement against the current page and the safe area that SwiftUI content receives.
- [Transitions](transitions.md) for the enter and exit animation.
- [Dialog](dialogs.md), [Loading](loading.md), and [Toast](toast.md) for the shared-code side.
