---
name: ksdialogs-android
description: Build KsDialogs Dialog, Loading, and Toast on Android from Views or Jetpack Compose, with typed results, layout, and transitions.
license: MIT
metadata:
  language: en
  source: https://github.com/kamusoft/KsDialogs
---

# KsDialogs for Android

KsDialogs is a UI library that lets an application call a Dialog from anywhere. You write the content yourself as a View, either registered ahead of time or passed at the call site, and the caller only asks for presentation and awaits the result. Three kinds can be presented — a Dialog that receives the user's answer, a Loading that blocks interaction while work runs, and a Toast that shows a non-interactive notification. This Skill covers the Android edition, which is normally called from the default entries (`Dialog.instance`, `Loading.instance`, `Toast.instance`). In tests or an application's dependency injection, inject the `KsDialog`, `KsLoading`, or `KsToast` contract backed by `Dialog()`, `Loading()`, or `Toast()`.

Calling through a default entry and calling through an injected contract reach the same process-wide state. There is a single view registry per kind: `Dialog.instance.registry` is `DialogViewRegistry.shared`, `Loading.instance.registry` is `LoadingViewRegistry.shared`, and `Toast.instance.registry` is `ToastViewRegistry.shared`. The styles and container options set on the default entries are shared the same way. Content can be written as an Android View or in Jetpack Compose, but the registry is not split per UI technology: `register` and `registerCompose` both put entries into the same registry. In every registry an entry for one view-model type has two slots, a View factory and a view-model factory, and all three kinds offer a `show` that takes only the view-model type.

## Capability map

| Goal | API | Recipe |
|---|---|---|
| Register and show a Dialog | `DialogViewModel`, `register`, `registerCompose`, `Dialog.instance.show` | [Dialog](references/dialogs.md) |
| Recover from a missing registration or absent host | `DialogException` | [Dialog](references/dialogs.md) |
| Let a view model report a result | `notifier` | [View models](references/view-models.md) |
| Have a view model created from its type alone | `registerViewModel`, type-based `show` / `start` | [View models](references/view-models.md) |
| Control size, placement, overlay, and outside taps | `DialogOptions`, `DialogPlacement`, View properties, `KsDialogAttributes` | [Layout](references/layout.md) |
| Animate presentation and dismissal | `DialogTransition`, `ksDialogTransition`, `KsDialogAttributes` | [Transitions](references/transitions.md) |
| Block interaction while work runs | `Loading.instance`, `LoadingStyle`, `LoadingProgressReceiver` | [Loading](references/loading.md) |
| Show fire-and-forget notifications | `Toast.instance`, `ToastStyle`, `ToastViewRegistry` | [Toast](references/toast.md) |

## Setup

The artifacts are built with Kotlin 2.4.10 and support consumers on the same minor line (2.4.x), with minSdk 24 or later. `jp.kamusoft:ksdialogs-core` (Android Views) and `jp.kamusoft:ksdialogs` (Jetpack Compose) are the planned public coordinates; they are not published to Maven Central yet. Replace `<version>` below with the release version.

For a View-only Android application, declare only the core artifact:

```kotlin
dependencies {
    implementation("jp.kamusoft:ksdialogs-core:0.1.0-beta.1")
}
```

For a Compose application, declare only the Compose artifact; it brings in the core artifact transitively. The recipes use the Compose BOM 2025.05.00, which matches the library's Compose 1.8.1 line, plus Lifecycle 2.8.7 and Coroutines 1.11.0:

```kotlin
dependencies {
    implementation("jp.kamusoft:ksdialogs:0.1.0-beta.1")
    val composeBom = platform("androidx.compose:compose-bom:2025.05.00")
    implementation(composeBom)
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
}
```

For a module containing Compose content, enable Compose and apply the Compose compiler plugin with the same version as Kotlin. With Kotlin 2.4.10, the relevant build configuration is:

```kotlin
plugins {
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10"
}

android {
    buildFeatures {
        compose = true
    }
}
```

The library tracks the resumed `Activity` automatically, so application initialization is not required. Import APIs from `jp.kamusoft.ksdialogs`; Compose extensions are in `jp.kamusoft.ksdialogs.compose`.

## Minimal example

```kotlin
import jp.kamusoft.ksdialogs.Toast

fun notifySaved() {
    Toast.instance.show("Saved")
}
```

## Choose a recipe

| Goal | Recipe to read |
|---|---|
| Registration, typed results, inline content, stacked dialogs, `DialogException` | [Dialog](references/dialogs.md) |
| `notifier`, view-model factories, pre-presentation configuration, the type-based `show` shared by all three kinds | [View models](references/view-models.md) |
| Placement, margins, proportional sizing, overlays, outside-tap cancellation | [Layout](references/layout.md) |
| Presets, custom asynchronous hooks, post-dismissal result delivery | [Transitions](references/transitions.md) |
| Imperative and scoped Loading, progress, styling, custom content, type-based `show` / `start` | [Loading](references/loading.md) |
| Message, registered, inline, and type-based Toast routes | [Toast](references/toast.md) |
