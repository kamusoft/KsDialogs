# Show a Dialog

Declare the result type on a class-based view model, register a factory that builds fresh content for every presentation, then await `DialogResult`. Omitting the `Result` declaration selects `Bool`; declare another result type with `typealias Result`.

Content can be written as a SwiftUI view or as a UIKit `UIView`. Registration and presentation take the same overload names, and only the factory's return type differs.

The examples below are split into four parts: the view model, the view, the registration, and the caller.

## Choose a `show`

`Dialog.shared` and an instance injected as `KsDialog` point at the same registry and the same presentation state. There are three axes for choosing a `show` overload: whether the view model is passed as an instance or as a type only, whether the content is left to a registered factory or supplied on the spot, and whether the placement is overridden for this presentation alone.

Every overload is `async throws` and returns `DialogResult<ViewModel.Result>`. Passing `placement` replaces the whole `DialogPlacement` attached to the content ([Layout](layout.md)).

| Signature | What it does | When to choose it | Registration needed |
|---|---|---|---|
| `show(_ viewModel: ViewModel)` | Passes a view-model instance you built, and a registered view factory creates the content | When the caller assembles the view model (passing values to `init`) | View factory |
| `show(_ viewModel: ViewModel, placement: DialogPlacement?)` | The above plus a placement override | When only this presentation changes placement | View factory |
| `show(_ viewModel: ViewModel, factory: (ViewModel, DialogNotifier<Result>) -> some View)` | Passes the view model and a SwiftUI content factory together. It neither reads nor changes the registry | One-off SwiftUI content | None |
| `show(_ viewModel: ViewModel, factory: (ViewModel, DialogNotifier<Result>) -> UIView)` | The UIKit form of the same shape. The factory's return type selects this overload | One-off UIKit content | None |
| `show(_ viewModel:placement:factory:)` (two overloads, SwiftUI / UIKit) | The two above plus a placement override | Same as above, and placement also changes | None |
| `show(_ viewModelType: ViewModel.Type)` | Passes only the view-model type, and shows the instance created by a registered view-model factory | When instance creation is also left to the registration side | View factory + view-model factory ([View models](view-models.md)) |
| `show(_ viewModelType: ViewModel.Type, configure: (ViewModel) async throws -> Void)` | Passes the type, then adjusts the created instance in `configure` before presenting | When values are set before presentation. The content is built after `configure` completes | Same as above |
| `show(_ viewModelType:placement:)` / `show(_ viewModelType:placement:configure:)` | Type-based forms plus a placement override | Same as above, and placement also changes | Same as above |

Omitted from the table: both a registered factory and an inline factory can be `throws`. A failure thrown by the factory becomes the failure of `show`, and the Dialog is not presented (Loading behaves the same way; only Toast cannot return it to the caller).

In the examples below, `show(ConfirmViewModel(message:))` under "Register and call" is row 1, `show(ItemEditViewModel.self) { ... }` under "Return a result type other than `Bool`" is row 7, and the direct factory under "Show content without registration" is row 3. The remaining rows appear in "Minimal example for each overload" next. Registering the view-model factory that type-based `show` requires is also covered in [View models](view-models.md).

## Minimal example for each overload

One example for each shape that does not appear in the sections below. `ConfirmViewModel` / `ItemEditViewModel` and the caller's `dialogs` property are all defined in those sections.

Passing an instance and overriding placement for this presentation only.

```swift
let result = try await dialogs.show(
    ConfirmViewModel(message: "Delete this item?"),
    placement: DialogPlacement(verticalAlignment: .start)
)
```

The UIKit form, whose inline factory returns a `UIView`.

```swift
let result = try await dialogs.show(ConfirmViewModel(message: "Delete this item?")) { viewModel, notifier in
    UIButton(primaryAction: UIAction(title: viewModel.message) { _ in notifier.complete(true) })
}
```

An inline factory plus a placement override (SwiftUI content).

```swift
let result = try await dialogs.show(
    ConfirmViewModel(message: "Delete this item?"),
    placement: DialogPlacement(verticalAlignment: .end)
) { viewModel, notifier in
    Button(viewModel.message) { notifier.complete(true) }
}
```

The same shape with a factory returning a `UIView`.

```swift
let result = try await dialogs.show(
    ConfirmViewModel(message: "Delete this item?"),
    placement: DialogPlacement(verticalAlignment: .end)
) { viewModel, notifier in
    UIButton(primaryAction: UIAction(title: viewModel.message) { _ in notifier.complete(true) })
}
```

Passing only the view-model type, omitting both `configure` and placement.

```swift
let result = try await dialogs.show(ItemEditViewModel.self)
```

Type-based with only a placement override added (no `configure`).

```swift
let result = try await dialogs.show(
    ItemEditViewModel.self,
    placement: DialogPlacement(verticalAlignment: .start)
)
```

Type-based with both a placement override and `configure`.

```swift
let result = try await dialogs.show(
    ItemEditViewModel.self,
    placement: DialogPlacement(verticalAlignment: .start)
) { viewModel in
    viewModel.name = "Apple"
}
```

## Declare a view model

The view model holds the result type and the values the content reads. Because `Result` is not declared, this view model's result is `Bool`.

```swift
import KsDialogs

final class ConfirmViewModel: DialogViewModel {
    let message: String

    init(message: String) {
        self.message = message
    }
}
```

## Write content in SwiftUI

The overlay and the placement are handled by the library's container, so the view contains only the card itself. Report the result to the `DialogNotifier` handed in by the factory. Only the first report settles the result; later reports do nothing. The result reaches the caller after the dismissal animation and the removal of the container have finished.

```swift
import SwiftUI
import KsDialogs

struct ConfirmView: View {
    let viewModel: ConfirmViewModel
    let notifier: DialogNotifier<Bool>

    var body: some View {
        VStack(spacing: 16) {
            Text(viewModel.message)
                .multilineTextAlignment(.center)

            HStack(spacing: 10) {
                Button("Cancel") { notifier.cancel() }
                    .frame(maxWidth: .infinity, minHeight: 44)
                Button("OK") { notifier.complete(true) }
                    .frame(maxWidth: .infinity, minHeight: 44)
            }
        }
        .padding(20)
        .frame(width: 272)
        .background(.regularMaterial, in: .rect(cornerRadius: 16))
    }
}
```

To write it in UIKit, have the factory return a `UIView`. Registration and presentation use the same names and a single registry ([Layout](layout.md) has an example of `UIView` content).

## Register and call

Call `register` once at application startup. In a SwiftUI app, `App`'s `init` is the place for it.

```swift
import SwiftUI
import KsDialogs

@main
struct MyApp: App {
    init() {
        Dialog.shared.registry.register(ConfirmViewModel.self) { viewModel, notifier in
            ConfirmView(viewModel: viewModel, notifier: notifier)
        }
    }

    var body: some Scene {
        WindowGroup {
            ItemScreen()
        }
    }
}
```

The caller can use `Dialog.shared` directly or an instance injected as `KsDialog`.

```swift
import Observation
import SwiftUI
import KsDialogs

@MainActor
@Observable
final class ItemScreenModel {
    private let dialogs: any KsDialog

    private(set) var status = ""

    init(dialogs: any KsDialog = Dialog.shared) {
        self.dialogs = dialogs
    }

    func deleteItem() async {
        do {
            switch try await dialogs.show(ConfirmViewModel(message: "Delete this item?")) {
            case .completed(let accepted):
                status = accepted ? "Deleted" : "Kept"
            case .cancelled:
                status = "Kept"
            }
        } catch {
            status = "Failed: \(error.localizedDescription)"
        }
    }
}

struct ItemScreen: View {
    @State private var model = ItemScreenModel()

    var body: some View {
        VStack(spacing: 16) {
            Text(model.status)
            Button("Delete") {
                Task { await model.deleteItem() }
            }
        }
    }
}
```

`show` takes no cancellation argument. A result becomes `.cancelled` from a cancellation report on the notifier, an outside tap (enabled by default), the disposal of the screen the Dialog was presented on, or cancellation of the Task awaiting `show`.

## Return a result type other than `Bool`

`typealias Result` accepts any `Sendable` type. The types of `notifier` and `DialogResult` follow from that declaration, so reporting a different type does not compile.

The next view model holds the values the content edits as `var` and reports the result itself through the `notifier` property. In this shape the view factory takes only the view model, using the single-argument overload ([View models](view-models.md)).

```swift
import Observation
import KsDialogs

struct ItemEdit: Sendable {
    let name: String
    let quantity: Int
}

@MainActor
@Observable
final class ItemEditViewModel: DialogViewModel {
    typealias Result = ItemEdit

    var name = ""
    var quantity = 0

    func save() {
        notifier?.complete(ItemEdit(name: name, quantity: quantity))
    }

    func cancel() {
        notifier?.cancel()
    }
}
```

Registering both a view factory and a view-model factory also enables type-based `show`. The view is a SwiftUI view that takes `@Bindable var viewModel: ItemEditViewModel` and calls `save()` / `cancel()`.

```swift
Dialog.shared.registry.register(ItemEditViewModel.self) { viewModel in
    ItemEditView(viewModel: viewModel)
}
Dialog.shared.registry.register(ItemEditViewModel.self, viewModel: {
    ItemEditViewModel()
})
```

```swift
extension ItemScreenModel {
    func editItem() async throws {
        let result = try await dialogs.show(ItemEditViewModel.self) { viewModel in
            viewModel.name = "Apple"
        }
        if case .completed(let edit) = result {
            status = "\(edit.name) x\(edit.quantity)"
        }
    }
}
```

## Show content without registration

For one-off content, pass the factory directly to `show`. This route does not add, replace, or remove a registry entry, and it does not use a registered factory for the same view-model type either. Either a SwiftUI or a UIKit factory can be passed.

```swift
import SwiftUI
import KsDialogs

final class NoticeViewModel: DialogViewModel {
    let text: String

    init(text: String) {
        self.text = text
    }
}
```

```swift
extension ItemScreenModel {
    func showNotice() async throws {
        let result = try await dialogs.show(NoticeViewModel(text: "Ready")) { viewModel, notifier in
            Button(viewModel.text) { notifier.complete(true) }
                .padding()
        }
        status = result == .completed(true) ? "Read" : "Dismissed"
    }
}
```

## Stack independent Dialogs

Use separate view-model instances for concurrent shows. The later Dialog is in front, and ordinary user input reaches only that front Dialog.

If app code retains the lower notifier and completes it first, iOS removes the entire presentation chain, so both disappear. The lower show returns its reported value and the upper show returns `.cancelled`. The upper dismissal hook does not run, because the OS removed its container.

```swift
extension ItemScreenModel {
    func showTwoDialogs() async throws {
        async let first = dialogs.show(ConfirmViewModel(message: "First"))
        async let second = dialogs.show(ConfirmViewModel(message: "Second"))
        let results = try await (first, second)
        status = "\(results.0) / \(results.1)"
    }
}
```

## Handle misconfiguration failures

Misconfiguration throws `DialogError` instead of returning `.cancelled`. This keeps a missing registration from being mistaken for an end-user cancellation, and in that case no content is created or presented.

Cancelling the Task that awaits `show` is different: the Dialog closes and `show` returns `.cancelled` without throwing.

| `DialogError` case | Message (`localizedDescription`) | Cause and remedy |
|---|---|---|
| `viewFactoryNotRegistered(viewModelType:)` | `No View factory is registered for ViewModel type {TypeName}.` | The content for that view-model type cannot be resolved. Call `register(_:factory:)` at startup |
| `presentationHostUnavailable` | `No screen is available to present the Dialog.` | There is no screen to present on. Show after the first screen appears. It fails immediately rather than queueing |
| `viewModelFactoryNotRegistered(viewModelType:)` | `No ViewModel factory is registered for ViewModel type {TypeName}.` | A type-based `show` has no view-model factory. Call `register(_:viewModel:)` ([View models](view-models.md)) |
| `viewModelAlreadyShowing(viewModelType:)` | `This ViewModel instance of type {TypeName} is already being shown.` | The same instance was shown again while showing. Create a new instance for each stacked presentation |

The messages in the table are the values the current implementation returns, not a stable API (what does not change is the case and the condition it is thrown under; the wording can change without notice).

The remaining `viewFactoryTypeMismatch` / `viewModelFactoryTypeMismatch` / `resultTypeMismatch` are mismatches in hand-offs that cross type erasure (the KMP entry, type-based `show`), and normally do not occur while registering and showing from Swift alone.

For example, showing `ConfirmViewModel` without calling `register` at startup cannot resolve `ConfirmView` and yields `viewFactoryNotRegistered`.

```swift
@main
struct MyApp: App {
    var body: some Scene {
        WindowGroup {
            ItemScreen()
        }
    }
}
```

The caller can `catch` per case and read the unresolved view-model type name from `viewModelType`. Misconfiguration is not something that can be fixed at run time, so make it noticeable during development and present it to users as a failure. Splitting the `catch` in `deleteItem` above per case looks like this.

```swift
func deleteItem() async {
    do {
        switch try await dialogs.show(ConfirmViewModel(message: "Delete this item?")) {
        case .completed(let accepted):
            status = accepted ? "Deleted" : "Kept"
        case .cancelled:
            status = "Kept"
        }
    } catch DialogError.viewFactoryNotRegistered(let viewModelType) {
        assertionFailure("Register content for \(viewModelType) during startup")
        status = "Failed"
    } catch {
        status = "Failed"
    }
}
```

To handle every case together, catch with `catch let error as DialogError`.
