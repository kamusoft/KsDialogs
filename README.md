# KsDialogs

## Overview and highlights

KsDialogs is a dialog UI library that can present UI from anywhere in an application without wiring it into the current view hierarchy. Native iOS and Android implementations provide the foundation, with thin wrappers for .NET MAUI and Kotlin Multiplatform.

- Present typed dialogs and receive their results.
- Block interaction during work with Loading, including progress updates.
- Show non-interactive Toast notifications that let touches pass through.
- Use native Views or declarative UI such as SwiftUI and Jetpack Compose.

The public API may introduce breaking changes while the version remains 0.x.

## Screenshots

| iOS | Android |
|---|---|
| **Dialog**<br>![A basic dialog in the iOS Sample](https://raw.githubusercontent.com/kamusoft/KsDialogs/develop/assets/ios-dialog.png) | **Dialog**<br>![A basic dialog in the Android Sample](https://raw.githubusercontent.com/kamusoft/KsDialogs/develop/assets/android-dialog.png) |
| **Loading**<br>![Loading at 50 percent in the iOS Sample](https://raw.githubusercontent.com/kamusoft/KsDialogs/develop/assets/ios-loading.png) | **Loading**<br>![Loading at 50 percent in the Android Sample](https://raw.githubusercontent.com/kamusoft/KsDialogs/develop/assets/android-loading.png) |
| **Toast**<br>![Three toast notifications in the iOS Sample](https://raw.githubusercontent.com/kamusoft/KsDialogs/develop/assets/ios-toast.png) | **Toast**<br>![Three toast notifications in the Android Sample](https://raw.githubusercontent.com/kamusoft/KsDialogs/develop/assets/android-toast.png) |

.NET MAUI and Kotlin Multiplatform wrap the native implementations and produce the same screens.

## Supported platforms

| Form | Minimum OS | Toolchain used to build the library |
|---|---|---|
| iOS Native | iOS 17 | Swift 6.3 |
| Android Native | Android 7.0 (API 24) | Kotlin 2.4.10, AGP 9.3.0, Gradle 9.7.0 |
| .NET MAUI | iOS 17 / Android 7.0 (API 24) | .NET 10 (`net10.0`), Microsoft.Maui.Controls 10.0.20 |
| Kotlin Multiplatform | iOS 17 / Android 7.0 (API 24) | Kotlin 2.4.10, AGP 9.3.0, Gradle 9.7.0, Swift 6.3 |

The Android targets use minSdk 24 and compileSdk 36. Android Native and Kotlin Multiplatform support a Kotlin Gradle Plugin from the same minor series, Kotlin 2.4.x; 2.4.10 is the version the consumer builds are verified against. The versions in the table are used to build the library; they are not consumer minimums. SwiftPM linkage on the Kotlin side of the KMP integration is Alpha.

## Installation

The declarations below cover the package coordinates and the version syntax. The version in each example is the current release and is updated with every release. A prerelease is written as `X.Y.Z-alpha.N`, `X.Y.Z-beta.N`, or `X.Y.Z-rc.N`. See [Agent Skills](#agent-skills) for platform setup and IDE-specific details.

### iOS Native

Add the Swift package and use its `KsDialogs` product.

```swift
dependencies: [
    .package(
        url: "https://github.com/kamusoft/KsDialogs-SPM",
        exact: "0.1.0-beta.1"
    )
]
```

SwiftPM resolves a prerelease only when the tag is pinned with `exact`, so the declaration keeps that form for release and prerelease versions alike.

### Android Native

For a View-only application, add the core Maven artifact.

```kotlin
dependencies {
    implementation("jp.kamusoft:ksdialogs-core:0.1.0-beta.1")
}
```

For a Compose application, add only `ksdialogs`; it brings in the core artifact transitively.

```kotlin
dependencies {
    implementation("jp.kamusoft:ksdialogs:0.1.0-beta.1")
}
```

A prerelease is written as the same version string in the coordinate.

### .NET MAUI

Add the NuGet package.

```xml
<PackageReference Include="KsDialogs.Maui" Version="0.1.0-beta.1" />
```

A prerelease is written as the same version string in the `Version` attribute.

The native binding packages arrive transitively for the iOS and Android target frameworks; an application does not reference them directly.

Microsoft.Maui.Controls 10.0.20 or later is required. That is the version bundled with the .NET workload set this repository pins, so an application on the same workload set does not have to state a MAUI version of its own; pinning a version below 10.0.20 makes the build fail with the NuGet downgrade error NU1605.

The package targets `net10.0`, `net10.0-ios`, and `net10.0-android`, so the .NET 10 SDK with the iOS and Android MAUI workloads is required. The minimum OS versions are iOS 17 and Android 7.0 (API 24); an application whose `SupportedOSPlatformVersion` is lower than that — or left unset — stops at build time with the guard diagnostic `KSDLG0001`.

### Kotlin Multiplatform

Add the Maven artifact to `commonMain`.

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            api("jp.kamusoft:ksdialogs-kmp:0.1.0-beta.1")
        }
    }
}
```

`api` keeps the KsDialogs types visible to the Android application, which needs them because a shared view model derives from `DialogViewModel`.

The Android application receives the Android Native artifact `jp.kamusoft:ksdialogs-core` transitively. The Compose artifact `jp.kamusoft:ksdialogs` is not included, so an Android application that writes dialog content in Compose adds it as well.

The iOS application also adds `https://github.com/kamusoft/KsDialogs-SPM` and links its `KsDialogs` product, pinned with `exact` to the same version as the Maven artifact.

## Minimal examples

### iOS Native

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

### Android Native

```kotlin
import jp.kamusoft.ksdialogs.Toast

fun notifySaved() {
    Toast.instance.show("Saved")
}
```

### .NET MAUI

```csharp
using KsDialogs;

namespace MyApp;

public static class Notifications
{
    public static void ShowSaved() => Toast.Instance.Show("Saved");
}
```

### Kotlin Multiplatform

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

## Agent Skills

The [Agent Skills index](https://github.com/kamusoft/KsDialogs/blob/main/skills/README.md) provides platform-specific setup and complete recipes for Dialog, Loading, Toast, layout, transitions, view models, and migration from AiForms.Maui.Dialogs.

## Repository layout

| Directory | Entry point |
|---|---|
| [`ios/`](https://github.com/kamusoft/KsDialogs/tree/main/ios) | Native iOS library |
| [`android/`](https://github.com/kamusoft/KsDialogs/tree/main/android) | Native Android libraries |
| [`maui/`](https://github.com/kamusoft/KsDialogs/tree/main/maui) | .NET MAUI wrapper |
| [`kmp/`](https://github.com/kamusoft/KsDialogs/tree/main/kmp) | Kotlin Multiplatform wrapper |
| [`samples/`](https://github.com/kamusoft/KsDialogs/tree/main/samples) | Sample applications for the four forms |
| [`skills/`](https://github.com/kamusoft/KsDialogs/tree/main/skills) | Agent Skills in English and Japanese |
| [`assets/`](https://github.com/kamusoft/KsDialogs/tree/main/assets) | Public documentation images |
| [`kasane/`](https://github.com/kamusoft/KsDialogs/tree/main/kasane) | Project knowledge and change records |

[AGENTS.md](https://github.com/kamusoft/KsDialogs/blob/main/AGENTS.md) · [Concept documentation](https://github.com/kamusoft/KsDialogs/tree/main/kasane/concepts)

## Contributing

This project does not accept external pull requests. Please use an Issue to report a bug, propose a feature, or ask a question, and choose the matching Issue template so the necessary context is included. See [CONTRIBUTING.md](https://github.com/kamusoft/KsDialogs/blob/main/.github/CONTRIBUTING.md) for the contribution policy and reporting guidance.

## License

KsDialogs is available under the [MIT License](https://github.com/kamusoft/KsDialogs/blob/main/LICENSE).
