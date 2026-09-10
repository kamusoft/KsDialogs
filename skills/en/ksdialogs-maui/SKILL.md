---
name: ksdialogs-maui
description: Build .NET MAUI dialog, loading, and toast flows with KsDialogs using typed results, dependency injection, layout, and transitions.
license: MIT
metadata:
  language: en
  source: https://github.com/kamusoft/KsDialogs
---

# KsDialogs for .NET MAUI

KsDialogs is a UI library that calls up a Dialog from anywhere in an application. Its content is a View you write yourself, either registered ahead of time or handed over at the call site, and the caller only asks for the presentation and awaits the result. Three kinds can be shown: a Dialog that collects the user's response, a Loading that blocks interaction while work runs, and a Toast that raises a non-interactive notification. This Skill covers the .NET MAUI edition, which is normally called through the default entries (`Dialog.Instance`, `Loading.Instance`, and `Toast.Instance`); in tests or dependency-injection setups, `Dialog`, `Loading`, and `Toast` can be injected as `IKsDialog`, `IKsLoading`, and `IKsToast`.

A call through a default entry and a call through an injected contract reach the same process-wide state. There is one registry per kind: `Dialog.Instance.Registry` points at `DialogViewRegistry.Shared`, `Loading.Instance.Registry` at `LoadingViewRegistry.Shared`, and `Toast.Instance.Registry` at `ToastViewRegistry.Shared`. The app-wide settings live in the same place — a value placed on `Loading.Instance.Style`, `Loading.Instance.Options`, or `Toast.Instance.Style` applies no matter which entry shows the content. A MAUI `View` is the only content form, so registries never split per UI technology.

## Capability map

| Goal | API | Recipe |
|---|---|---|
| Register and show a Dialog | `IDialogViewModel`, `DialogViewRegistry.Register`, `Dialog.Instance.ShowAsync` | [Dialog](references/dialogs.md) |
| Let a view model report a result, or show by type | `DialogNotifier<TResult>`, `Notifier`, `RegisterViewModel`, type-based `ShowAsync` | [View models](references/view-models.md) |
| Control size, placement, overlay, and outside taps | `Dialog` attached properties, `DialogOptions`, `DialogPlacement` | [Layout](references/layout.md) |
| Animate presentation and dismissal | `DialogTransition`, `DialogTransitionEdge`, `Dialog.SetTransition` | [Transitions](references/transitions.md) |
| Block interaction while work runs | `Loading.Instance`, `LoadingViewRegistry`, `LoadingStyle`, `ILoadingProgressReceiver` | [Loading](references/loading.md) |
| Show fire-and-forget notifications | `Toast.Instance`, `ToastViewRegistry`, `ToastStyle` | [Toast](references/toast.md) |
| Wire views and view models through MAUI DI | `AddKsDialogs`, `RegisterForDialog`, `RegisterForLoading`, `RegisterForToast` | [DI registration](references/di-registration.md) |

## Setup

Add `KsDialogs.Maui` to a .NET 10 MAUI project. The package is not published on NuGet yet, so `<version>` below stands for the version of the package you obtained. Import the `KsDialogs` namespace where you use the library.

```xml
<PackageReference Include="KsDialogs.Maui" Version="0.1.0-beta.1" />
```

| Requirement | What to satisfy |
|---|---|
| MAUI | `Microsoft.Maui.Controls` 10.0.20 or later. That is the version bundled with the .NET workload set this library is built and tested against, so a project on the same workload set can leave the version unwritten. Pinning an older one (below 10.0.20) makes restore report NU1605, the NuGet package-downgrade error |
| .NET SDK | .NET 10 with the iOS and Android MAUI workloads installed |
| Minimum OS | iOS 17.0 and Android 7.0 (API 24). A `SupportedOSPlatformVersion` below that, including one left unset where the SDK default is lower, stops the build with error `KSDLG0001`, which names the required version and the current value. The check runs on the iOS and Android inner builds only |

## Minimal example

```csharp
using KsDialogs;

namespace MyApp;

public static class Notifications
{
    public static void ShowSaved() => Toast.Instance.Show("Saved");
}
```

## Choose a recipe

| Goal | Recipe to read |
|---|---|
| Registration, typed results, inline content, stacked Dialogs, configuration failures | [Dialog](references/dialogs.md) |
| `Notifier`, view-model factories, pre-presentation configuration, type-based presentation shared by Dialog, Loading, and Toast | [View models](references/view-models.md) |
| XAML and code-behind attachment, placement, margins, proportional sizing, overlays, outside-tap cancellation | [Layout](references/layout.md) |
| Presets and custom asynchronous hooks | [Transitions](references/transitions.md) |
| Imperative and scoped Loading, progress, styling, custom content | [Loading](references/loading.md) |
| Message, registered, and inline Toast routes | [Toast](references/toast.md) |
| One-line registration, fallback resolution, and view-construction failures | [DI registration](references/di-registration.md) |
