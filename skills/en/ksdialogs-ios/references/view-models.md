# Report a result from a view model

A Dialog's result is reported by the view model being shown, through its `notifier` property. This document covers how to read `notifier` and how to use the `show` that takes only the view-model type. Loading and Toast offer the type-based `show` in the same shape, and the last section collects the per-feature differences.

## Read `notifier`

`notifier` holds a value only while being shown. Calling it with `?.` makes a report while not shown do nothing.

| Item | Content |
|---|---|
| Type | `DialogNotifier<Result>` for the `Result` the view model declared |
| Before and after `show` | `nil` (it returns a value only while shown) |
| When it is attached | Immediately before `show` creates the content |
| When it is removed | Before the result reaches the caller. It is removed on completion, cancellation, or failure alike |
| Relation to the factory's notifier argument | They point at the same delivery target |
| Number of reports | Only the first one settles the result; later reports do nothing |

The following is a view model whose result is a string, where `save()` reports completion and `close()` reports cancellation. The content edits the state through `@Bindable`.

```swift
import Observation
import SwiftUI
import KsDialogs

@MainActor
@Observable
final class EditorViewModel: DialogViewModel {
    typealias Result = String
    var text = ""

    func save() {
        notifier?.complete(text)
    }

    func close() {
        notifier?.cancel()
    }
}

struct EditorView: View {
    @Bindable var viewModel: EditorViewModel

    var body: some View {
        VStack {
            TextField("Name", text: $viewModel.text)
            Button("Save") { viewModel.save() }
            Button("Cancel") { viewModel.close() }
        }
        .padding()
    }
}
```

## Make the view model a class

- Only a `Sendable` class can conform to `DialogViewModel`. Because the notifier is looked up by instance identity, a struct cannot conform
- Only one notifier is bound to a single instance. Showing the same instance again while it is shown makes the second call throw `DialogError.viewModelAlreadyShowing(viewModelType:)`. The first presentation is unaffected
- To stack presentations, create a new instance for each one. Separate instances of the same type stack independently

## Create a view model from a type and configure it

The type-based `show` takes only the view-model type, configures the instance created by a registered view-model factory, then presents it. The caller does not have to assemble the view model.

### Registration needed

A registry entry has two independent slots.

| Slot | Registration API | Which show uses it |
|---|---|---|
| View factory | `registry.register(_:factory:)` | Both instance-based and type-based |
| View-model factory | `registry.register(_:viewModel:)` | Type-based only |

- The type-based `show` needs both. Without a view-model factory it throws `DialogError.viewModelFactoryNotRegistered(viewModelType:)` before creating the content. A missing startup registration is caught by this error
- Re-registering into the same slot replaces it, last one wins, and the other slot remains
- What is resolved is the registration as of the moment `show` was called, so re-registering while a Dialog is shown does not change the one on screen

### Steps from creation to presentation

The order is fixed as follows.

1. The view-model factory creates the instance
2. `configure` completes
3. The notifier is bound
4. The view factory creates the content
5. The Dialog is presented

Values set in `configure` are therefore always in place before the content reads the state. Creation and `configure` run on `MainActor`. A failure thrown by the view-model factory or by `configure` does not become a cancellation; it propagates to the caller as it is, without proceeding to presentation.

The following registers the `EditorViewModel` above into both slots. Register once at application startup.

```swift
@main
struct MyApp: App {
    init() {
        Dialog.shared.registry.register(EditorViewModel.self) { viewModel in
            EditorView(viewModel: viewModel)
        }
        Dialog.shared.registry.register(EditorViewModel.self, viewModel: {
            EditorViewModel()
        })
    }

    var body: some Scene {
        WindowGroup {
            ItemScreen()
        }
    }
}
```

The caller passes the type and sets initial values in `configure` before presenting. `ItemScreenModel` is the one defined in [Dialog](dialogs.md).

```swift
extension ItemScreenModel {
    func renameItem() async throws {
        let result = try await dialogs.show(EditorViewModel.self) { viewModel in
            viewModel.text = "Apple"
        }
        if case .completed(let name) = result {
            status = name
        }
    }
}
```

`configure` is `async throws`, so the initial state can also be loaded asynchronously before presentation. The content is created after that completes.

```swift
extension ItemScreenModel {
    func editLoadedItem() async throws {
        let result = try await dialogs.show(EditorViewModel.self) { viewModel in
            viewModel.text = try await self.loadCurrentName()
        }
        if case .completed(let name) = result {
            status = name
        }
    }

    func loadCurrentName() async throws -> String { "Apple" }
}
```

## Loading and Toast also take a type

The type-based `show` is not a Dialog-only route: Loading and Toast offer it in the same shape, and Loading additionally has the type-based scoped `start`. The three registries are independent of one another, so the same view-model type can be registered separately in each, yet the slot registration APIs are spelled the same everywhere: `register(_:factory:)` and `register(_:viewModel:)`.

What the three features share: the view-model factory creates the instance, resolution uses a snapshot taken at the call, the order is "create → `configure` → build content → present", creation and `configure` run on the MainActor, and a missing view-model factory fails as misconfiguration. The differences are the following three.

| Feature | `configure` | Binding inserted before the content is built | Failure of the view-model factory or `configure` |
|---|---|---|---|
| Dialog | `async throws` | notifier | Does not proceed to presentation; propagates to the caller instead of becoming a cancellation |
| Loading (`show` / `start`) | `async throws` | Progress receiver | Does not proceed to presentation, propagates to the caller, and is not counted as a merge participant. The type-based `start` does not run the work either |
| Toast | Synchronous (`throws` can be written) | None | `show` has already returned, so nothing comes back; only that one display is discarded |

The spelling and examples live in [Loading](loading.md) and [Toast](toast.md).
