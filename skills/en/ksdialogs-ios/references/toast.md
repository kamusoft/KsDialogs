# Show a Toast

A Toast is a fire-and-forget notification. It does not take touches, and each accepted display expires on its own timer.

`Toast.shared` and an instance injected as `KsToast` point at the same registry and the same shared settings.

## Choose a `show`

There are four routes: showing the built-in message Toast, showing registered custom content, passing a content factory on the spot, or passing the view-model type and leaving creation to the registration side as well. The built-in message Toast route has no content resolution, so it does not throw.

`duration` is in milliseconds, and passing `placement` replaces the whole `DialogPlacement` attached to the content ([Layout](layout.md)).

**A Toast has no closing operation.** There is neither an API equivalent to `hide` nor one that replaces the message being shown. The only trigger for disappearance is the duration elapsing (a difference from [Loading](loading.md)).

| Signature | What it does | When to choose it | Registration needed |
|---|---|---|---|
| `show(message:duration:placement:)` | Shows the built-in message Toast | When you only convey wording | None |
| `show(message:duration:)` / `show(message:)` | Short forms of the above | When placement or display time is left to the default | None |
| `show(_ viewModel:duration:placement:)` — `throws` | Shows registered custom content | When you present your own appearance | Toast view factory |
| `show(_ viewModel:duration:)` / `show(_ viewModel:)` | Short forms of the above | When placement or display time is left to the default | Same as above |
| `show(_ viewModel:duration:placement:factory:)` — two overloads, SwiftUI / `UIView` | Passes a content factory on the spot. It neither reads nor changes the registry | One-off content | None |
| `show(_ viewModel:factory:)` | A short form of the above (both display time and placement default) | Same as above, showing it with the defaults | None |
| `show(_ viewModelType:duration:placement:configure:)` — `throws` | Passes only the view-model type, and shows the instance created by a registered view-model factory after `configure` | When instance creation is also left to the registration side | Toast view factory + view-model factory ([View models](view-models.md)) |
| `show(_ viewModelType:)` / `show(_ viewModelType:configure:)` / `show(_ viewModelType:duration:)` / `show(_ viewModelType:duration:configure:)` / `show(_ viewModelType:duration:placement:)` | Short forms of the above | When `configure`, display time, or placement is omitted | Same as above |

In the examples below, "Show a message Toast" corresponds to row 1, "Register and show custom content" to row 4 (`show(_ viewModel:duration:)`), "Show custom content without registration" to the SwiftUI form of row 5, and "Show custom content by passing a type" to `show(_ viewModelType:duration:configure:)`. The remaining rows appear in "Minimal example for each overload" next.

## Minimal example for each overload

One example for each shape that does not appear in the sections below. `StatusToastViewModel` and the caller's `toasts` property are both defined in those sections.

Passing only wording and `duration` to the built-in message Toast (placement defaults).

```swift
toasts.show(message: "Saved", duration: 2000)
```

Passing only wording to the built-in message Toast (`duration` and placement default).

```swift
toasts.show(message: "Saved")
```

Passing both `duration` and a placement to registered custom content.

```swift
try toasts.show(
    StatusToastViewModel(message: "Synchronized"),
    duration: 1800,
    placement: DialogPlacement(verticalAlignment: .start)
)
```

Showing registered custom content with the view model alone (`duration` and placement default).

```swift
try toasts.show(StatusToastViewModel(message: "Synchronized"))
```

An inline factory with `duration` and a placement, in the UIKit form whose factory returns a `UIView`.

```swift
try toasts.show(
    StatusToastViewModel(message: "Uploaded"),
    duration: 1200,
    placement: DialogPlacement(verticalAlignment: .start),
    factory: { (viewModel: StatusToastViewModel) -> UIView in
        let label = UILabel()
        label.text = viewModel.message
        return label
    }
)
```

Passing an inline factory together with the view model and nothing else (SwiftUI content, `duration` and placement default).

```swift
try toasts.show(StatusToastViewModel(message: "Uploaded")) { viewModel in
    Text(viewModel.message)
        .padding()
}
```

The same shape with a factory returning a `UIView`.

```swift
try toasts.show(StatusToastViewModel(message: "Uploaded")) { (viewModel: StatusToastViewModel) -> UIView in
    let label = UILabel()
    label.text = viewModel.message
    return label
}
```

Passing the view-model type, with `configure`, display time, and placement all omitted.

```swift
try toasts.show(StatusToastViewModel.self)
```

Passing the type and adjusting the state in `configure` before presenting (display time and placement default).

```swift
try toasts.show(StatusToastViewModel.self) { viewModel in
    viewModel.message = "Synchronized"
}
```

The type-based form plus a `duration` (`configure` omitted).

```swift
try toasts.show(StatusToastViewModel.self, duration: 1800)
```

The type-based form plus a `duration` and a placement (`configure` omitted).

```swift
try toasts.show(
    StatusToastViewModel.self,
    duration: 1800,
    placement: DialogPlacement(verticalAlignment: .start)
)
```

The type-based form with a `duration`, a placement, and a `configure`.

```swift
try toasts.show(
    StatusToastViewModel.self,
    duration: 1800,
    placement: DialogPlacement(verticalAlignment: .start),
    configure: { viewModel in viewModel.message = "Synchronized" }
)
```

## Show a message Toast

If `duration` is omitted or a nonpositive value is passed, the current `Toast.shared.style.defaultDuration` is used when positive, and otherwise it falls back to `ToastStyle.builtinDefaultDuration`.

If `placement` is omitted, `Toast.shared.style.defaultPlacement` is used, and if that is absent, the library default (bottom centre of the visible area, offset 80 pt upward).

This route does not throw, so it can be called directly from a view.

```swift
import SwiftUI
import KsDialogs

struct SaveButton: View {
    var body: some View {
        Button("Save") {
            Toast.shared.show(
                message: "Saved",
                duration: 2000,
                placement: DialogPlacement(verticalAlignment: .end, offsetY: -100)
            )
        }
    }
}
```

## Configure Toast defaults

`ToastStyle` is read when a display is accepted, so a change applies from the next Toast on. Setting it once at application startup is the basic form.

The visual fields (`backgroundColor`, `textColor`, `fontSize`, `cornerRadius`) affect the built-in view only, and `backgroundColor` defaults to `ToastStyle.builtinBackgroundColor`. `defaultDuration` and `defaultPlacement` also become the defaults for custom content.

```swift
import SwiftUI
import UIKit
import KsDialogs

@main
struct MyApp: App {
    init() {
        Toast.shared.style = ToastStyle(
            backgroundColor: .darkGray,
            textColor: .white,
            fontSize: 16,
            cornerRadius: 22,
            defaultDuration: ToastStyle.builtinDefaultDuration,
            defaultPlacement: DialogPlacement(verticalAlignment: .end, offsetY: -120)
        )
    }

    var body: some Scene {
        WindowGroup {
            NoticeScreen()
        }
    }
}
```

## Register and show custom content

The registry for custom Toasts is `Toast.shared.registry` (`ToastViewRegistry`), independent of the Dialog and Loading ones. Make the content's view model a class-based `ToastViewModel`.

Custom content can carry a `DialogTransition`. The built-in view has no such entry point ([Transitions](transitions.md)).

```swift
import SwiftUI
import KsDialogs

@MainActor
final class StatusToastViewModel: ToastViewModel {
    var message: String

    init(message: String) {
        self.message = message
    }
}

struct StatusToastView: View {
    let viewModel: StatusToastViewModel

    var body: some View {
        Label(viewModel.message, systemImage: "checkmark.circle.fill")
            .padding()
            .background(.regularMaterial)
            .clipShape(.capsule)
            .ksDialogTransition(.slide(from: .bottom))
    }
}
```

Add the registration in the same place as the default settings (`App`'s `init`).

```swift
Toast.shared.registry.register(StatusToastViewModel.self) { viewModel in
    StatusToastView(viewModel: viewModel)
}
```

The caller can use `Toast.shared` directly or an instance injected as `KsToast`.

```swift
import Observation
import KsDialogs

@MainActor
@Observable
final class NoticeScreenModel {
    private let toasts: any KsToast

    init(toasts: any KsToast = Toast.shared) {
        self.toasts = toasts
    }

    func notifySynchronized() throws {
        try toasts.show(StatusToastViewModel(message: "Synchronized"), duration: 1800)
    }
}
```

## Show custom content without registration

For one-off content, pass the factory directly to `show`. This route does not add, replace, or remove a registry entry, and it does not use a registered factory for the same view-model type either. Either a SwiftUI or a UIKit factory can be passed.

```swift
extension NoticeScreenModel {
    func notifyUploaded() throws {
        try toasts.show(
            StatusToastViewModel(message: "Uploaded"),
            duration: 1200,
            placement: DialogPlacement(verticalAlignment: .start),
            factory: { viewModel in
                Text(viewModel.message)
                    .padding()
                    .background(.thinMaterial)
            }
        )
    }
}
```

## Show custom content by passing a type

The `show` that takes only the view-model type presents the instance created by a registered view-model factory after running `configure` on it. The shape matches the Dialog and Loading type-based routes ([View models](view-models.md)).

A registry entry has two slots, a view factory and a view-model factory, and this route needs both. Add the view-model factory registration in the same place as the content registration.

```swift
Toast.shared.registry.register(StatusToastViewModel.self, viewModel: {
    StatusToastViewModel(message: "")
})
```

`configure` is synchronous. It can be `throws` but not `async`, because `show` is a synchronous call with no return value and there is nothing to await its completion on.

The view-model factory and `configure` run on the MainActor, in acceptance order.

```swift
extension NoticeScreenModel {
    func notifyFromRegisteredViewModel() throws {
        try toasts.show(StatusToastViewModel.self, duration: 1800) { viewModel in
            viewModel.message = "Synchronized"
        }
    }
}
```

A failure thrown by `configure` or by the view-model factory does not reach the caller of `show`. Because it runs after `show` has returned synchronously, it leaves a warning log and discards only that one display. The only thing this route throws at the call site is a missing slot.

What is resolved is the registration as of the moment `show` was called, so re-registering while a Toast is shown does not change the one on screen.

## Handle misconfiguration failures

The misconfigurations possible with Toast are content and view-model resolution. If the view-model type is unregistered on the registered route, `show` throws synchronously at the call site and nothing is shown. The type-based route behaves the same way, and a missing view-model factory is detected before a missing view factory.

A factory failure after acceptance cannot be returned to the caller, so it only discards that one display. A `configure` failure on the type-based route, and a mismatch in a hand-off that crosses type erasure (the KMP entry, `viewFactoryTypeMismatch` / `viewModelFactoryTypeMismatch`), also take this route: they only log a warning and cannot be caught from `show`. The type mismatches normally do not occur while registering and showing from Swift alone. The built-in message Toast route (`show(message:)`) has neither resolution, so it does not fail.

| `DialogError` case | Message (`localizedDescription`) | Cause and remedy |
|---|---|---|
| `viewFactoryNotRegistered(viewModelType:)` | `No View factory is registered for ViewModel type {TypeName}.` | The content for that view-model type cannot be resolved. Call `Toast.shared.registry.register(_:factory:)` at startup (it is separate from the Dialog / Loading registries) |
| `viewModelFactoryNotRegistered(viewModelType:)` | `No ViewModel factory is registered for ViewModel type {TypeName}.` | The type-based `show` has no view-model factory. Call `Toast.shared.registry.register(_:viewModel:)` at startup |

The messages in the table are the values the current implementation returns, not a stable API (what does not change is the case and the condition it is thrown under; the wording can change without notice).

For example, setting only the defaults at startup without writing `register` cannot resolve the content for `StatusToastViewModel` and yields `viewFactoryNotRegistered`.

```swift
@main
struct MyApp: App {
    init() {
        Toast.shared.style = ToastStyle(defaultDuration: 2000)
    }

    var body: some Scene {
        WindowGroup {
            NoticeScreen()
        }
    }
}
```

The caller can `catch` this case and read the unresolved view-model type name from `viewModelType`. A Toast is only a notification, so there is nowhere on screen to surface the failure; keep misconfiguration noticeable during development.

```swift
extension NoticeScreenModel {
    func notifySynchronizedSafely() {
        do {
            try notifySynchronized()
        } catch DialogError.viewFactoryNotRegistered(let viewModelType) {
            assertionFailure("Register toast content for \(viewModelType) during startup")
        } catch {
            assertionFailure("Toast failed: \(error.localizedDescription)")
        }
    }
}
```
