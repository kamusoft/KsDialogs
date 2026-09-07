# Show a Loading

A Loading blocks interaction while work runs. It stays in front of Dialogs and Toasts and cannot be closed by the user.

`Loading.shared` and an instance injected as `KsLoading` point at the same presentation state. Concurrent uses merge into a single process-wide presentation, and the content of the first start is kept.

## Choose between `show` / `start` / `hide`

There are two ways to open a presentation. A presentation opened with `show` is closed only by `hide`, whereas `start` ties the presentation's lifetime to the start and completion of the work, so you never close it yourself. Only `start` can report progress.

Passing `placement` replaces the whole `DialogPlacement` attached to the content ([Layout](layout.md)).

| Signature | What it does | When to choose it | Registration needed |
|---|---|---|---|
| `show(message:placement:)` — `async` | Shows the built-in Loading and starts one merge participant | When you only show a message and close it yourself | None |
| `show(message:)` / `show()` | Short forms of the above (placement left to the content's default) | When placement does not change | None |
| `show(_ viewModel:placement:)` — `async throws` | Shows registered custom content | When you present your own appearance | Loading view factory |
| `show(_ viewModel:)` | A short form of the above | When placement does not change | Same as above |
| `show(_ viewModel:placement:factory:)` — two overloads, SwiftUI / `UIView` | Passes a content factory on the spot. It neither reads nor changes the registry | One-off content | None |
| `show(_ viewModel:factory:)` | A short form of the above | When placement does not change | None |
| `show(_ viewModelType:placement:configure:)` — `async throws` | Passes only the view-model type, and shows the instance created by a registered view-model factory after `configure` | When instance creation is also left to the registration side | View factory + view-model factory ([View models](view-models.md)) |
| `show(_ viewModelType:)` / `show(_ viewModelType:configure:)` / `show(_ viewModelType:placement:)` | Short forms of the above | When `configure` or placement is omitted | Same as above |
| `hide()` — `async` | Closes the presentation. It closes immediately regardless of the merge count and does not interfere with running work | When closing a presentation opened with `show` | — |
| `setMessage(_:)` — `async` | Replaces the message being shown | When changing the wording while the built-in Loading is shown (nothing happens while hidden or while custom content is shown) | — |
| `start(message:placement:_:)` — `async throws` | Runs work with the built-in Loading shown and returns its return value | When the presentation's lifetime follows the work | None |
| `start(message:_:)` / `start(_:)` (the form taking only the work) | Short forms of the above | When the message or placement is left to the default | None |
| `start(_ viewModel:placement:_:)` — `async throws` | Does the same with registered custom content | When wrapping work in your own appearance | Loading view factory |
| `start(_ viewModel:_:)` | A short form of the above | When placement does not change | Same as above |
| `start(_ viewModel:placement:factory:_:)` — two overloads, SwiftUI / `UIView` | Does the same with an inline factory | When wrapping work in one-off content | None |
| `start(_ viewModel:factory:_:)` | A short form of the above | When placement does not change | None |
| `start(_ viewModelType:placement:configure:_:)` — `async throws` | Does the same with a type. The work runs after `configure` completes | When creation is also left to the registration side while wrapping work | View factory + view-model factory ([View models](view-models.md)) |
| `start(_ viewModelType:_:)` / `start(_ viewModelType:configure:_:)` / `start(_ viewModelType:placement:_:)` | Short forms of the above | When `configure` or placement is omitted | Same as above |

In the examples below, "Show and close imperatively" corresponds to `show(message:)`, `hide()`, and `setMessage(_:)`; "Show only while work runs" to `start(message:_:)`; "Register and show custom content" to `start(_:_:)`; and "Show custom content by passing a type" to `start(_ viewModelType:configure:_:)`. The remaining rows appear in "Minimal example for each overload" next.

## Minimal example for each overload

One example for each shape that does not appear in the sections below. `ProgressLoadingViewModel` and the caller's `loading` property are both defined in those sections.

Passing both a message and a placement to the built-in Loading.

```swift
await loading.show(message: "Connecting", placement: DialogPlacement(verticalAlignment: .start))
```

Showing the built-in Loading with both message and placement omitted.

```swift
await loading.show()
```

Showing registered custom content with placement omitted.

```swift
try await loading.show(ProgressLoadingViewModel())
```

Registered custom content plus a placement override.

```swift
try await loading.show(ProgressLoadingViewModel(), placement: DialogPlacement(verticalAlignment: .start))
```

Showing SwiftUI content with an inline factory (placement omitted).

```swift
try await loading.show(ProgressLoadingViewModel()) { viewModel in
    ProgressView(value: viewModel.progress)
}
```

The same shape with a factory returning a `UIView`.

```swift
try await loading.show(ProgressLoadingViewModel()) { _ in
    UIActivityIndicatorView(style: .large)
}
```

An inline factory plus a placement override (SwiftUI content).

```swift
try await loading.show(
    ProgressLoadingViewModel(),
    placement: DialogPlacement(verticalAlignment: .start),
    factory: { viewModel in ProgressView(value: viewModel.progress) }
)
```

The same shape with a factory returning a `UIView`.

```swift
try await loading.show(
    ProgressLoadingViewModel(),
    placement: DialogPlacement(verticalAlignment: .start),
    factory: { _ in UIActivityIndicatorView(style: .large) }
)
```

The scoped form of the built-in Loading, passing only the work, with message and placement omitted.

```swift
let data = try await loading.start { report in
    report(1)
    return Data()
}
```

The scoped form of the built-in Loading with both a message and a placement.

```swift
let data = try await loading.start(
    message: "Downloading",
    placement: DialogPlacement(verticalAlignment: .start)
) { report in
    report(1)
    return Data()
}
```

The scoped form of registered custom content plus a placement override.

```swift
try await loading.start(
    ProgressLoadingViewModel(),
    placement: DialogPlacement(verticalAlignment: .start)
) { report in
    report(1)
}
```

The scoped form with an inline factory (SwiftUI content, placement omitted).

```swift
try await loading.start(
    ProgressLoadingViewModel(),
    factory: { viewModel in ProgressView(value: viewModel.progress) }
) { report in
    report(1)
}
```

The same shape with a factory returning a `UIView`.

```swift
try await loading.start(
    ProgressLoadingViewModel(),
    factory: { _ in UIActivityIndicatorView(style: .large) }
) { report in
    report(1)
}
```

The scoped form with an inline factory plus a placement override (SwiftUI content).

```swift
try await loading.start(
    ProgressLoadingViewModel(),
    placement: DialogPlacement(verticalAlignment: .start),
    factory: { viewModel in ProgressView(value: viewModel.progress) }
) { report in
    report(1)
}
```

The same shape with a factory returning a `UIView`.

```swift
try await loading.start(
    ProgressLoadingViewModel(),
    placement: DialogPlacement(verticalAlignment: .start),
    factory: { _ in UIActivityIndicatorView(style: .large) }
) { report in
    report(1)
}
```

Passing the view-model type, with both `configure` and placement omitted.

```swift
try await loading.show(ProgressLoadingViewModel.self)
```

Passing the type and adjusting the state in `configure` before presenting (placement omitted).

```swift
try await loading.show(ProgressLoadingViewModel.self) { viewModel in
    viewModel.message = "Connecting"
}
```

The type-based form plus a placement override (`configure` omitted).

```swift
try await loading.show(ProgressLoadingViewModel.self, placement: DialogPlacement(verticalAlignment: .start))
```

The type-based form with both a placement and a `configure`.

```swift
try await loading.show(
    ProgressLoadingViewModel.self,
    placement: DialogPlacement(verticalAlignment: .start),
    configure: { viewModel in viewModel.message = "Connecting" }
)
```

The type-based scoped form, with both `configure` and placement omitted.

```swift
try await loading.start(ProgressLoadingViewModel.self) { report in
    report(1)
}
```

The type-based scoped form plus a placement override.

```swift
try await loading.start(
    ProgressLoadingViewModel.self,
    placement: DialogPlacement(verticalAlignment: .start)
) { report in
    report(1)
}
```

The type-based scoped form with both a placement and a `configure`.

```swift
try await loading.start(
    ProgressLoadingViewModel.self,
    placement: DialogPlacement(verticalAlignment: .start),
    configure: { viewModel in viewModel.message = "Uploading" }
) { report in
    report(1)
}
```

## Show and close imperatively

`show` and `hide` are used as a pair. `hide` does not cancel running work; it closes the current presentation and returns after the container has been removed.

```swift
import Observation
import KsDialogs

@MainActor
@Observable
final class SyncScreenModel {
    private let loading: any KsLoading

    private(set) var status = ""

    init(loading: any KsLoading = Loading.shared) {
        self.loading = loading
    }

    func synchronize() async {
        await loading.show(message: "Connecting")
        await loading.setMessage("Downloading")
        await downloadItems()
        await loading.hide()
        status = "Synchronized"
    }

    private func downloadItems() async {}
}
```

## Show only while work runs

`start` pairs the presentation's lifetime with the work. The work's return value is returned to the caller as it is, and a failure thrown by the work also propagates after the presentation is closed. Reported progress is clamped to `0...1`.

```swift
import Foundation
import KsDialogs

extension SyncScreenModel {
    func downloadReport() async throws -> Data {
        try await loading.start(message: "Downloading") { report in
            report(0.25)
            let data = try await fetchReport()
            report(1)
            return data
        }
    }

    private func fetchReport() async throws -> Data {
        Data()
    }
}
```

## Configure the appearance of the built-in Loading

`style` and `options` are read when a presentation starts, so changes take effect from the next presentation. Setting them once at application startup is the basic form.

`LoadingStyle` controls the indicator and the message. `progressFormat` is a `LoadingStyle.ProgressFormat` closure and defaults to `LoadingStyle.defaultProgressFormat`. `options` is a `DialogOptions`, in which `isCanceledOnTouchOutside` remains ineffective for Loading.

```swift
import SwiftUI
import UIKit
import KsDialogs

@main
struct MyApp: App {
    init() {
        Loading.shared.style = LoadingStyle(
            indicatorColor: .systemBlue,
            messageFontSize: 16,
            messageColor: .white,
            defaultMessage: "Working",
            progressFormat: { message, progress in
                guard let progress else { return message ?? "" }
                return "\(message ?? "") \(Int(progress * 100))%"
            }
        )
        Loading.shared.options = DialogOptions(
            dialogMargin: DialogEdgeInsets(all: 32),
            overlayColor: UIColor.black.withAlphaComponent(0.5)
        )
    }

    var body: some Scene {
        WindowGroup {
            SyncScreen()
        }
    }
}
```

## Register and show custom content

The registry for custom Loading is `Loading.shared.registry` (`LoadingViewRegistry`), independent of the Dialog one. Make the content's view model a class-based `LoadingViewModel`.

A model that receives progress from `start` also implements `LoadingProgressReceiver`. Progress is simply not forwarded to a model that does not implement it; that is not an error.

```swift
import Observation
import SwiftUI
import KsDialogs

@MainActor
@Observable
final class ProgressLoadingViewModel: LoadingViewModel, LoadingProgressReceiver {
    var message = ""
    var progress = 0.0

    func onProgress(_ progress: Double) {
        self.progress = progress
    }
}

struct ProgressLoadingView: View {
    let viewModel: ProgressLoadingViewModel

    var body: some View {
        ProgressView(value: viewModel.progress) {
            Text(viewModel.message)
        }
        .padding()
        .ksDialogTransition(.fade())
    }
}
```

Add the registration in the same place as the appearance settings (`App`'s `init`).

```swift
Loading.shared.registry.register(ProgressLoadingViewModel.self) { viewModel in
    ProgressLoadingView(viewModel: viewModel)
}
```

The caller takes the same shape as the built-in Loading, only passing a view model.

```swift
extension SyncScreenModel {
    func uploadItems() async throws {
        try await loading.start(ProgressLoadingViewModel()) { report in
            report(0.5)
            try await upload()
        }
    }

    private func upload() async throws {}
}
```

`show` and `start` also accept an inline factory that leaves the registry unchanged.

## Show custom content by passing a type

The `show` / `start` that take only the view-model type present the instance created by a registered view-model factory after running `configure` on it. The caller does not have to assemble the view model, and the shape matches the Dialog type-based route ([View models](view-models.md)).

A registry entry has two slots, a view factory and a view-model factory, and the type-based route needs both. Add the view-model factory registration in the same place as the content registration.

```swift
Loading.shared.registry.register(ProgressLoadingViewModel.self, viewModel: {
    ProgressLoadingViewModel()
})
```

`configure` is `async throws`; the progress receiver is bound and the content is built only after it completes, so values set in `configure` are in place before the content reads them. A failure thrown by the view-model factory or by `configure` does not proceed to presentation, is not counted as a merge participant, and propagates to the caller as it is. In that case the type-based `start` does not run the work either.

The view-model factory and `configure` run on the MainActor, so you can call UIKit APIs directly inside them.

```swift
extension SyncScreenModel {
    func uploadFromRegisteredViewModel() async throws {
        try await loading.start(ProgressLoadingViewModel.self, configure: { viewModel in
            viewModel.message = "Uploading"
        }) { report in
            report(0.5)
            try await upload()
        }
    }
}
```

The merge decision happens after `configure` completes. If another start settled the presentation while an asynchronous `configure` was still running, this call joins that presentation and the view model it created is not used for display (the content of the first start is kept).

What is resolved is the registration as of the moment `show` / `start` was called, so re-registering while a Loading is shown changes neither the content on screen nor the progress delivery target.

## Handle misconfiguration failures

The misconfigurations possible with Loading are content and view-model resolution, and on failure neither the presentation nor the work runs (fail-fast). The built-in Loading routes (`show(message:)` / `start(message:_:)`) have neither resolution, so they do not fail. On the type-based route a missing view-model factory is detected before a missing view factory.

| `DialogError` case | Message (`localizedDescription`) | Cause and remedy |
|---|---|---|
| `viewFactoryNotRegistered(viewModelType:)` | `No View factory is registered for ViewModel type {TypeName}.` | The content for that view-model type cannot be resolved. Call `Loading.shared.registry.register(_:factory:)` at startup (it is separate from the Dialog / Toast registries) |
| `viewModelFactoryNotRegistered(viewModelType:)` | `No ViewModel factory is registered for ViewModel type {TypeName}.` | The type-based `show` / `start` has no view-model factory. Call `Loading.shared.registry.register(_:viewModel:)` at startup |
| `viewFactoryTypeMismatch(viewModelType:)` | `The registered View factory cannot accept ViewModel type {TypeName}.` | A mismatch in a hand-off that crosses type erasure (the KMP entry). It normally does not occur while registering and showing from Swift alone |
| `viewModelFactoryTypeMismatch(viewModelType:)` | `The registered ViewModel factory does not produce ViewModel type {TypeName}.` | Likewise a mismatch across type erasure, possible only on the type-based route |

The messages in the table are the values the current implementation returns, not a stable API (what does not change is the case and the condition it is thrown under; the wording can change without notice).

For example, setting only the appearance at startup without writing `register` cannot resolve the content for `ProgressLoadingViewModel` and yields `viewFactoryNotRegistered`.

```swift
@main
struct MyApp: App {
    init() {
        Loading.shared.style = LoadingStyle(defaultMessage: "Working")
    }

    var body: some Scene {
        WindowGroup {
            SyncScreen()
        }
    }
}
```

The caller can `catch` per case and read the unresolved view-model type name from `viewModelType`. Misconfiguration is not something that can be fixed at run time, so make it noticeable during development.

```swift
extension SyncScreenModel {
    func uploadItemsSafely() async {
        do {
            try await uploadItems()
        } catch DialogError.viewFactoryNotRegistered(let viewModelType) {
            assertionFailure("Register loading content for \(viewModelType) during startup")
        } catch {
            status = "Failed"
        }
    }
}
```
