---
name: ksdialogs-kmp
description: Build Kotlin Multiplatform dialog, loading, and toast flows with KsDialogs from commonMain, including Android and iOS host registration.
license: MIT
metadata:
  language: en
  source: https://github.com/kamusoft/KsDialogs
---

# KsDialogs for Kotlin Multiplatform

KsDialogs is a UI library that lets you show a Dialog from anywhere in the app. You register content you wrote yourself, and the caller only asks for the display and awaits the result. Three kinds can be shown — a Dialog that receives the user's answer, a Loading that blocks interaction while work runs, and a Toast that shows a non-interactive notification. This Skill covers the Kotlin Multiplatform edition: the calls live in shared Kotlin code, and each host supplies the content natively. Shared code calls `Dialog.instance`, `Loading.instance`, and `Toast.instance`, or receives `KsDialog`, `KsLoading`, and `KsToast` through dependency injection.

Three sides are involved:

| Side | What you write there |
|---|---|
| Shared code (`commonMain`) | View-model classes, result types, `show` / `start` calls, per-call `DialogPlacement` |
| Android host | Android View or Compose content for each shared view-model class, plus attached options and transitions |
| iOS host | UIKit or SwiftUI content on the KMP entry of the Swift package, plus attached options and transitions |

Content registration lives in the hosts because the content type differs per OS. Both hosts write into the same native registry that pure-native callers use, so shared code and native code resolve the same factory. What the shared-code registry holds is the registration of how to build the view model (the view-model factory). Whether a call starts from the default entry or from an injected contract, it reaches this same registry.

## Capability map

| Goal | Where you write it | Recipe |
|---|---|---|
| Show a registered Dialog and await its result | `DialogViewModel<R>`, `Dialog.instance.show`, `DialogResult` | [Dialog](references/dialogs.md) |
| Inject the shared contracts instead of the default entries | `KsDialog`, `KsLoading`, `KsToast` | [View models](references/view-models.md) |
| Show by view-model class and let the library create the instance | `registry.registerViewModel`, `show(VM::class)` | [View models](references/view-models.md) |
| Override the placement of one call | `DialogPlacement`, `DialogAlignment` | [Layout](references/layout.md) |
| Attach size, overlay, and outside-tap options to content | host `DialogOptions`, `ksDialogOptions`, `KsDialogAttributes` | [Layout](references/layout.md) |
| Attach an enter and exit animation to content | host `DialogTransition`, `ksDialogTransition` | [Transitions](references/transitions.md) |
| Block interaction while shared work runs | `Loading.instance`, `LoadingViewModel`, `LoadingProgressReceiver` | [Loading](references/loading.md) |
| Show non-interactive notifications | `Toast.instance`, `ToastViewModel` | [Toast](references/toast.md) |
| Register Android View or Compose content | `Dialog.instance.registry`, `registerCompose` | [Android host](references/android-host.md) |
| Register UIKit or SwiftUI content | `Dialog.shared.kmp`, `Loading.shared.kmp`, `Toast.shared.kmp` | [iOS host](references/ios-host.md) |

## Setup

The current artifact is built with Kotlin 2.4.10 and Gradle 9.7.0. The Android target requires API 24 or later; the iOS target requires iOS 17 or later and Swift tools 6.3. A consumer Kotlin Gradle Plugin on the same minor line (2.4.x) is supported, and 2.4.10 is the verified version. The SwiftPM import that carries the iOS linkage is an Alpha feature of Kotlin 2.4, so no wider range is promised.

`jp.kamusoft:ksdialogs-kmp` is the planned public coordinate; it is not published to Maven Central yet. Replace `<version>` below with the release version.

### Shared module

Add one Maven dependency to `commonMain` in the shared module's `build.gradle.kts`. The artifact metadata carries the Swift package linkage for iOS, so do not redeclare a `swiftPMDependencies` entry for KsDialogs in the consumer build.

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            api("jp.kamusoft:ksdialogs-kmp:0.1.0-beta.1")
        }
    }
}
```

Declare it with `api` rather than `implementation`: a shared view model derives from `DialogViewModel`, so KsDialogs types appear in the shared module's own API, and the Android host's registration code has to see them.

### Android host

The KMP artifact brings the Android Views artifact `jp.kamusoft:ksdialogs-core` in transitively, so the Android application registers content with `Dialog.instance.registry`, `Loading.instance.registry`, and `Toast.instance.registry`. Compose content needs the Compose artifact as well. See [Android host](references/android-host.md).

### iOS host

Prerequisite: finish the standard KMP iOS integration that builds and links the shared module's framework from the Xcode project.

1. Add the single `jp.kamusoft:ksdialogs-kmp:0.1.0-beta.1` Maven dependency to the shared module as shown above.
2. Run `integrateLinkagePackage` once with the Xcode project path, then include the generated `KotlinMultiplatformLinkedPackage/` in version control.
3. Add `https://github.com/kamusoft/KsDialogs-SPM` to Xcode Package Dependencies and link its `KsDialogs` product to the application target.

```bash
XCODEPROJ_PATH="$PWD/iosApp/MyApp.xcodeproj" \
  ./gradlew :shared:integrateLinkagePackage
```

Do not redeclare the SwiftPM dependency in Gradle: the published Maven metadata already carries it. The direct Xcode package entry is needed only so Swift host code can call the typed registration APIs. See [iOS host](references/ios-host.md).

Functions in shared code that Swift calls need `@Throws` whether they are `suspend` or non-suspending. The library declares it only on the routes that can fail while resolving a view model — dialog `show`, custom Loading `show` and `start`, and custom Toast `show`; the message routes do not declare it. Without the declaration, Kotlin/Native does not convert the exception to `NSError`: a `suspend` function terminates the process with an uncaught exception, and a non-suspending function lets nothing reach Swift.

## Minimal example

```kotlin
import jp.kamusoft.ksdialogs.kmp.Dialog
import jp.kamusoft.ksdialogs.kmp.DialogException
import jp.kamusoft.ksdialogs.kmp.DialogResult
import jp.kamusoft.ksdialogs.kmp.DialogViewModel
import kotlin.coroutines.cancellation.CancellationException

class ConfirmViewModel(val message: String) : DialogViewModel<Boolean>

@Throws(DialogException::class, CancellationException::class)
suspend fun showConfirmation(message: String): DialogResult<Boolean> =
    Dialog.instance.show(ConfirmViewModel(message))
```

Shared code has no boolean shorthand (the alias of the view model that omits the result type) of the kind the native libraries offer. A view-model declaration always spells the result type out, and `DialogViewModel<Boolean>` is the boolean form. Register the content for `ConfirmViewModel` once in each host before calling `showConfirmation`.

## Choose a recipe

| What you want to do | Recipe to read |
|---|---|
| Typed results, failures, cancellation, stacked displays | [Dialog](references/dialogs.md) |
| Contract injection, view-model factory registration, host-owned result reporting | [View models](references/view-models.md) |
| Per-call placement and host-side option attachment | [Layout](references/layout.md) |
| Carrying an animation choice to the hosts | [Transitions](references/transitions.md) |
| Imperative and scoped Loading with progress | [Loading](references/loading.md) |
| The message route and the registered custom route | [Toast](references/toast.md) |
| Android Dialog / Loading / Toast registration | [Android host](references/android-host.md) |
| iOS registration, result types, Swift exception bridging | [iOS host](references/ios-host.md) |
