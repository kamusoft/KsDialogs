# Attach an entry and exit animation to content

The Dialog's entry and exit animation is bundled into a `DialogTransition` and passed by attaching it to the content (the presentable the factory returns). There is no route for passing it as a `show` argument.

How you attach depends on the kind of content.

| Content | How to attach |
|---|---|
| SwiftUI `View` | `.ksDialogTransition(_:)` modifier |
| UIKit `UIView` | `ksDialogTransition` property |

## Choose a preset

The static methods on `DialogTransition` return a value with all three of `presentation`, `dismissal`, and `overlayDuration` filled in.

| Preset | Signature | Animation |
|---|---|---|
| fade | `DialogTransition.fade(duration:easing:)` | Enters and exits by opacity |
| slide | `DialogTransition.slide(from:duration:easing:)` | Slides in from the `from` edge and slides out toward the same edge |
| zoom | `DialogTransition.zoom(duration:easing:)` | Expands from slightly shrunk to full size, and shrinks back to the same scale as it disappears |
| none | `DialogTransition.none()` | No animation on the content side |

| Argument | Type | Default | Meaning |
|---|---|---|---|
| `from` | `DialogTransitionEdge` | None (required for slide only) | The edge to slide in from and out toward |
| `duration` | `TimeInterval` (seconds) | `0.25` | The one-way duration |
| `easing` | `any UITimingCurveProvider` | `.standard` (a curve that accelerates then decelerates) | How progress moves over time |

| Value passed to `from` | Direction |
|---|---|
| `.top` / `.bottom` | Stays as the physical direction |
| `.leading` / `.trailing` | Follows the layout direction (left and right swap in a right-to-left environment) |

## Defaults and details

- **When nothing is attached** — the content and the overlay behind it cross-fade over 0.25 seconds
- **`none()`** — only the content becomes instantaneous. The overlay still fades as usual
- **Concurrency** — the content animation and the overlay fade run in parallel, and presentation, dismissal, and result delivery all wait for both to finish
- **Attaching only one side** — `presentation` and `dismissal` can be passed one at a time. The side not passed becomes the container's default cross-fade, and the side passed fully replaces the default rather than blending with it
- **An invalid `duration`** — 0, a negative value, NaN, or infinity skips the animation and jumps straight to the final state
- **Handling failures** — a failure thrown by a hook is absorbed by the container and does not affect the Dialog result
- **Waiting for completion** — the container sets no timeout and waits for the hook to finish, so a hook that never completes stops the removal of the Dialog and the delivery of the result
- **When they are adopted** — the attached values are used as of the completion of the first native layout pass. Changing the attachment on a Dialog already on screen is not reflected in the current presentation
- **Where to call** — call preset factories on `MainActor`. SwiftUI's `body`, `UIView` initialization, and `register` factories are all on `MainActor`, so this normally needs no attention

## Attach to SwiftUI content and show

The following attaches a preset at the content's root. `ConfirmViewModel` and `ItemScreenModel` are the ones defined in [Dialog](dialogs.md).

```swift
import SwiftUI
import KsDialogs

struct ConfirmSheetView: View {
    let viewModel: ConfirmViewModel
    let notifier: DialogNotifier<Bool>

    var body: some View {
        VStack(spacing: 16) {
            Text(viewModel.message)
            Button("OK") { notifier.complete(true) }
        }
        .padding(20)
        .background(.regularMaterial, in: .rect(cornerRadius: 16))
        .ksDialogTransition(.slide(from: .bottom, duration: 0.3))
    }
}
```

Register this content at application startup.

```swift
@main
struct MyApp: App {
    init() {
        Dialog.shared.registry.register(ConfirmViewModel.self) { viewModel, notifier in
            ConfirmSheetView(viewModel: viewModel, notifier: notifier)
        }
    }

    var body: some Scene {
        WindowGroup {
            ItemScreen()
        }
    }
}
```

The caller writes nothing about the animation. On `show`, the content slides in from the bottom over 0.3 seconds, and on closing it slides out toward the same edge.

```swift
extension ItemScreenModel {
    func confirmDelete() async throws {
        let result = try await dialogs.show(ConfirmViewModel(message: "Delete this item?"))
        status = result == .completed(true) ? "Deleted" : "Kept"
    }
}
```

## Attach to UIKit content

The following attaches the animation during the initialization of the content `UIView` itself. Registration and calling are unchanged from SwiftUI content.

```swift
import UIKit
import KsDialogs

final class ConfirmPanelView: UIView {
    init(message: String, notifier: DialogNotifier<Bool>) {
        super.init(frame: .zero)
        backgroundColor = .secondarySystemBackground
        ksDialogTransition = .zoom(duration: 0.2)

        let button = UIButton(primaryAction: UIAction(title: message) { _ in notifier.complete(true) })
        button.translatesAutoresizingMaskIntoConstraints = false
        addSubview(button)
        NSLayoutConstraint.activate([
            button.topAnchor.constraint(equalTo: topAnchor, constant: 20),
            button.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -20),
            button.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 20),
            button.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -20)
        ])
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) is not supported")
    }
}
```

## Pass a custom hook

All three arguments of `DialogTransition(presentation:dismissal:overlayDuration:)` can be omitted. Both hooks have the type `DialogTransition.Hook` = `@MainActor @Sendable (UIView) async throws -> Void`, receiving the laid-out host `UIView` and returning once the animation has finished. For SwiftUI content the host `UIView` is the view wrapping it, and the overlay behind it is a sibling layer, so it is not a target of the hook.

The following assembles the animation in the registration factory and passes it to the content to attach. It rises into view from below on entry, and disappears by opacity alone on exit.

```swift
@main
struct MyApp: App {
    init() {
        Dialog.shared.registry.register(ConfirmViewModel.self) { viewModel, notifier in
            ConfirmSheetView(
                viewModel: viewModel,
                notifier: notifier,
                transition: DialogTransition(
                    presentation: { hostView in
                        hostView.alpha = 0
                        hostView.transform = CGAffineTransform(translationX: 0, y: 24)
                        await withCheckedContinuation { continuation in
                            UIView.animate(
                                withDuration: 0.2,
                                animations: {
                                    hostView.alpha = 1
                                    hostView.transform = .identity
                                },
                                completion: { _ in continuation.resume() }
                            )
                        }
                    },
                    dismissal: { hostView in
                        await withCheckedContinuation { continuation in
                            UIView.animate(
                                withDuration: 0.2,
                                animations: { hostView.alpha = 0 },
                                completion: { _ in continuation.resume() }
                            )
                        }
                    },
                    overlayDuration: 0.2
                )
            )
        }
    }

    var body: some Scene {
        WindowGroup {
            ItemScreen()
        }
    }
}
```

The content side simply attaches the animation it received.

```swift
struct ConfirmSheetView: View {
    let viewModel: ConfirmViewModel
    let notifier: DialogNotifier<Bool>
    let transition: DialogTransition

    var body: some View {
        VStack(spacing: 16) {
            Text(viewModel.message)
            Button("OK") { notifier.complete(true) }
        }
        .padding(20)
        .background(.regularMaterial, in: .rect(cornerRadius: 16))
        .ksDialogTransition(transition)
    }
}
```

With this, `presentation` is called when `show(ConfirmViewModel(message:))` presents and `dismissal` when it closes, and the overlay fades over 0.2 seconds. The caller is unchanged from the preset case.

## Combine a preset with a custom hook

You can take just one side's hook out of the value a preset returned and pass it alongside your own hook. The following keeps the entry as the zoom preset and makes only the exit custom.

```swift
Dialog.shared.registry.register(ConfirmViewModel.self) { viewModel, notifier in
    let zoomIn = DialogTransition.zoom(duration: 0.2)
    return ConfirmSheetView(
        viewModel: viewModel,
        notifier: notifier,
        transition: DialogTransition(
            presentation: zoomIn.presentation,
            dismissal: { hostView in
                await withCheckedContinuation { continuation in
                    UIView.animate(
                        withDuration: 0.14,
                        animations: { hostView.alpha = 0 },
                        completion: { _ in continuation.resume() }
                    )
                }
            },
            overlayDuration: zoomIn.overlayDuration
        )
    )
}
```

To match only the overlay duration to the preset, pass `zoomIn.overlayDuration` the same way; to make only the closing use the preset, pass `zoomIn.dismissal`.

## Wait for dismissal before continuing

Treat an awaited result as a post-dismissal value. The result is settled by the first report, but `show` returns only after the dismissal hook, the overlay fade, and the removal of the container have all finished. The following proceeds to the next step after the Dialog has left the screen.

```swift
extension ItemScreenModel {
    func confirmThenRefresh() async throws {
        let result = try await dialogs.show(ConfirmViewModel(message: "Refresh now?"))
        if case .completed(true) = result {
            await reloadItems()
        }
    }

    func reloadItems() async {}
}
```
