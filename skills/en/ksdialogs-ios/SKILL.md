---
name: ksdialogs-ios
description: Build iOS Dialogs, Loading, and Toasts with the KsDialogs SwiftUI / UIKit API, including typed results, layout, and transitions.
license: MIT
metadata:
  language: en
  source: https://github.com/kamusoft/KsDialogs
---

# KsDialogs for iOS

KsDialogs is a UI library that lets you call up a Dialog from anywhere in an app. The content is a view you wrote yourself, registered up front or passed at call time; the caller only asks for it to be shown and waits for the result. Three kinds can be shown — a Dialog that takes the user's response, a Loading that blocks interaction while work runs, and a Toast that emits a non-interactive notification. This Skill covers the iOS version, which you normally call through the shared entries (`Dialog.shared`, `Loading.shared`, `Toast.shared`). In tests or an application's dependency-injection setup, `Dialog()`, `Loading()`, and `Toast()` can be injected as the `KsDialog`, `KsLoading`, and `KsToast` contracts.

Whether you call through a shared entry or through an injected contract, you reach the same process-wide state. Registered content and the settings given to Loading / Toast are the same for an instance you created yourself and for the shared entry. Content can be written as a SwiftUI view or as a UIKit `UIView`, but registration is not split by UI technology — both go into the same registry.

## Capability map

| Goal | API | Recipe |
|---|---|---|
| Register and show a Dialog | `DialogViewModel`, `DialogViewRegistry`, `Dialog.shared.show`, `DialogResult`, `DialogNotifier` | [Dialog](references/dialogs.md) |
| Report a result from a view model | `notifier`, view-model factory, type-based `show`, `DialogError` | [View models](references/view-models.md) |
| Control size, placement, overlay, and outside taps | `DialogOptions`, `DialogPlacement`, `DialogAlignment`, `DialogLayoutArea`, `DialogEdgeInsets` | [Layout](references/layout.md) |
| Animate presentation and dismissal | `DialogTransition`, `DialogTransitionEdge`, `ksDialogTransition` | [Transitions](references/transitions.md) |
| Block interaction while work runs | `Loading.shared`, `LoadingViewRegistry`, `LoadingStyle`, `LoadingProgressReceiver` | [Loading](references/loading.md) |
| Show fire-and-forget notifications | `Toast.shared`, `ToastStyle`, `ToastViewRegistry` | [Toast](references/toast.md) |

## Setup

Add `https://github.com/kamusoft/KsDialogs-SPM` as a Swift Package dependency, select the `KsDialogs` product, and link it to an iOS 17 or later target using a Swift 6.3 or later toolchain. Import `KsDialogs` where you use the library.

## Minimal example

```swift
import SwiftUI
import KsDialogs

struct ContentView: View {
    var body: some View {
        Button("Show toast") {
            Toast.shared.show(message: "Saved")
        }
    }
}
```

## Choose a recipe

| Goal | Recipe to read |
|---|---|
| Registration, typed results, inline content, stacked dialogs, `DialogError` diagnosis | [Dialog](references/dialogs.md) |
| `notifier`, view-model factories, pre-presentation configuration, how type-based calls differ across Dialog / Loading / Toast | [View models](references/view-models.md) |
| Placement, margins, proportional sizing, overlays, outside-tap cancellation | [Layout](references/layout.md) |
| Presets and custom asynchronous hooks | [Transitions](references/transitions.md) |
| Imperative and scoped Loading, progress, styling, custom content, type-based `show` / `start` | [Loading](references/loading.md) |
| Message, registered, inline, and type-based Toast routes | [Toast](references/toast.md) |
