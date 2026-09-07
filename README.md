# KsDialogs

> **Release preparation:** Packages are being prepared for their initial public release.

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
| .NET MAUI | iOS 17 / Android 7.0 (API 24) | .NET 10 (`net10.0`), Microsoft.Maui.Controls 10.0.1 |
| Kotlin Multiplatform | iOS 17 / Android 7.0 (API 24) | Kotlin 2.4.10, AGP 9.3.0, Gradle 9.7.0, Swift 6.3 |

The Android targets use minSdk 24 and compileSdk 36. The .NET MAUI consumer baseline is Microsoft.Maui.Controls 10.0.1. The minimum consumer Kotlin version for Android Native and Kotlin Multiplatform has not yet been finalized and will be established before the initial release. The versions in the table are used to build the library; they are not consumer minimums. SwiftPM linkage on the Kotlin side of the KMP integration is Alpha.

## Installation

The declarations below cover the package coordinates and prerelease version syntax. See [Agent Skills](#agent-skills) for platform setup and IDE-specific details.

### iOS Native

Add the Swift package and use its `KsDialogs` product.

```swift
dependencies: [
    .package(
        url: "https://github.com/kamusoft/KsDialogs-SPM",
        exact: "<version>"
    )
]
```

For a prerelease, replace `<version>` with an exact tag such as `X.Y.Z-alpha.N`, `X.Y.Z-beta.N`, or `X.Y.Z-rc.N`.

### Android Native

For a View-only application, add the core Maven artifact.

```kotlin
dependencies {
    implementation("jp.kamusoft:ksdialogs:<version>")
}
```

For a Compose application, add only `ksdialogs-compose`; it brings in the core artifact transitively.

```kotlin
dependencies {
    implementation("jp.kamusoft:ksdialogs-compose:<version>")
}
```

For a prerelease, replace `<version>` with `X.Y.Z-alpha.N`, `X.Y.Z-beta.N`, or `X.Y.Z-rc.N`.

### .NET MAUI

Add the NuGet package.

```xml
<PackageReference Include="KsDialogs.Maui" Version="0.1.0" />
```

For a prerelease, replace the `Version` value with `X.Y.Z-alpha.N`, `X.Y.Z-beta.N`, or `X.Y.Z-rc.N`.

### Kotlin Multiplatform

Add the Maven artifact to `commonMain`.

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("jp.kamusoft:ksdialogs-kmp:<version>")
        }
    }
}
```

The iOS application also adds `https://github.com/kamusoft/KsDialogs-SPM` and links its `KsDialogs` product. For a prerelease, use the same `X.Y.Z-alpha.N`, `X.Y.Z-beta.N`, or `X.Y.Z-rc.N` version for the Maven artifact and Swift package tag.

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

The [Agent Skills index](skills/README.md) provides platform-specific setup and complete recipes for Dialog, Loading, Toast, layout, transitions, view models, and migration from AiForms.Maui.Dialogs.

## Repository layout

| Directory | Entry point |
|---|---|
| [`ios/`](ios/) | Native iOS library |
| [`android/`](android/) | Native Android libraries |
| [`maui/`](maui/) | .NET MAUI wrapper |
| [`kmp/`](kmp/) | Kotlin Multiplatform wrapper |
| `samples/` | Sample applications for the four forms |
| [`skills/`](skills/) | Agent Skills in English and Japanese |
| [`assets/`](assets/) | Public documentation images |
| [`kasane/`](kasane/) | Project knowledge and change records |

[AGENTS.md](AGENTS.md) · [Concept documentation](kasane/concepts/)

## Contributing

This project does not accept external pull requests. Please use an Issue to report a bug, propose a feature, or ask a question, and choose the matching Issue template so the necessary context is included. See [CONTRIBUTING.md](.github/CONTRIBUTING.md) for the contribution policy and reporting guidance.

## License

KsDialogs is available under the [MIT License](LICENSE).
