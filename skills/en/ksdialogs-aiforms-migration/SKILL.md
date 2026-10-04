---
name: ksdialogs-aiforms-migration
description: Migrate .NET MAUI dialog, loading, and toast code from AiForms.Maui.Dialogs to KsDialogs.Maui, with a member-by-member API mapping and explicit replacements for removed reusable views, lifecycle hooks, layout properties, and IoC configuration.
license: MIT
metadata:
  language: en
  source: https://github.com/kamusoft/KsDialogs
---

# Migrate from AiForms.Maui.Dialogs

KsDialogs.Maui replaces AiForms.Maui.Dialogs with Dialog result types, factory-based content, coalesced loading, and non-interactive toast notifications. Start from the default entries (`Dialog.Instance`, `Loading.Instance`, `Toast.Instance`); when an application uses dependency injection, inject `IKsDialog`, `IKsLoading`, or `IKsToast` instead — both routes share the same registry and the same app-wide settings. This Skill maps the old consumer-facing API to the new surface; use the `ksdialogs-maui` Skill for complete recipes and details of the new API.

## What changes

| Area | AiForms.Maui.Dialogs | KsDialogs.Maui |
|---|---|---|
| Entries and contracts | `Dialog.Instance` / `Loading.Instance` / `Toast.Instance` with `IDialog` / `ILoading` / `IToast` | same default entries, with `IKsDialog` / `IKsLoading` / `IKsToast` |
| Show verbs | `ShowAsync`, `ShowResultAsync`, `ShowFromModelAsync`, `ShowResultFromModelAsync` | one `ShowAsync` family; the ViewModel declares the result type |
| Result | result value type-erased; cancellation returns the result type's default value | `DialogResult<TResult>` with `Completed` and `Cancelled` branches |
| Content | derive from `DialogView`, `ExtraView`, `LoadingView`, `ToastView` | ordinary MAUI `View` returned by a registered or inline factory |
| Reuse | `IReusableDialog` and `IReusableLoading` handles | no reuse handle; content is built again for each display |
| Result reporting | `DialogNotifier` bound on the View | typed `DialogNotifier<TResult>` from the factory, or `viewModel.Notifier` |
| Layout attributes | properties on `ExtraView` and `DialogView` | `Dialog.*` attached properties, `DialogPlacement`, `DialogOptions` |
| Reference area | the `UseCurrentPageLocation` bool, effective on the vertical axis only | `DialogLayoutArea` (`Window` / `VisibleArea` / `CurrentPage`), effective on both axes |
| Animation | `RunPresentationAnimation` / `RunDismissalAnimation` overrides | `DialogTransition` attached with `Dialog.SetTransition` |
| Registration and IoC | `Configurations.SetIocConfig` | `RegisterForDialog` / `RegisterForLoading` / `RegisterForToast`, and `AddKsDialogs` fallbacks for dialogs |
| Show from a model type | `ShowFromModelAsync`, `CreateFromModel` | a type-based `ShowAsync` / `Show` for dialogs, loading, and toasts, with a `configure` callback |
| Loading settings | `LoadingConfig` | `Loading.Instance.Style` (`LoadingStyle`) and `Loading.Instance.Options` (`DialogOptions`) |
| Toast | obsolete, custom `ToastView` routes only | `IKsToast` with message, registered, inline, and type-based routes |
| Changed defaults | transparent overlay, window-wide layout area | 40% black overlay, visible area (the dialog margin default stays 0 on every side, as before) |

## Capability map

| Goal | Where to look |
|---|---|
| Replace the package, namespace, and IoC setup | Setup below, then [API mapping](references/api-mapping.md) |
| Migrate dialog show, result, ViewModel, and notifier code | [API mapping](references/api-mapping.md) |
| Replace reusable dialogs and loading handles | [API mapping](references/api-mapping.md) |
| Replace model-type-based show (`ShowFromModelAsync`, `CreateFromModel`) | [API mapping](references/api-mapping.md) |
| Move layout, overlay, and animation settings off `ExtraView` | [API mapping](references/api-mapping.md) |
| Move `UseCurrentPageLocation` to the current-page reference area (`DialogLayoutArea.CurrentPage`) | [API mapping](references/api-mapping.md) |
| Choose the thread a Loading operation starts on (`LoadingActionThread`) | Minimal migration below, then [API mapping](references/api-mapping.md) |
| Replace the obsolete custom-view-only toast API | [API mapping](references/api-mapping.md) |
| Understand how a mis-wired registration fails now | Failures, waiting, and cancellation below |
| Understand how a show waits while no screen exists yet, and how to cancel it with a `CancellationToken` | Failures, waiting, and cancellation below, then [API mapping](references/api-mapping.md) |
| Look up the KsDialogs.Maui API itself | the `ksdialogs-maui` Skill |

## Setup

Remove the `AiForms.Maui.Dialogs` package and add `KsDialogs.Maui` from nuget.org to a .NET 10 MAUI project. That single reference is everything a project writes: the binding packages `KsDialogs.Binding.iOS` and `KsDialogs.Binding.Android` carry the native side and arrive transitively on the iOS and Android target frameworks, so do not reference them directly. Replace `using AiForms.Dialogs;` with `using KsDialogs;`.

```xml
<PackageReference Include="KsDialogs.Maui" Version="{version}" />
```

`{version}` is a placeholder: replace it with the version you want to use, or restore fails. To find the current version, open the [latest release](https://github.com/kamusoft/KsDialogs/releases/latest) page, which always resolves to the most recent release.

The project also needs `Microsoft.Maui.Controls` 10.0.20 or later. A project that does not write a MAUI version uses the one bundled with its installed .NET workload set, and needs nothing more when that bundled version is 10.0.20 or later. A version below 10.0.20, whether pinned explicitly or bundled with an older workload set, makes restore report NU1605, the NuGet package-downgrade error; write 10.0.20 or later, or update the workload set. Use the .NET 10 SDK with the iOS and Android MAUI workloads installed. KsDialogs.Maui supports iOS 17.0 or later and Android 7.0 (API 24) or later; a `SupportedOSPlatformVersion` below that, including one left unset where the SDK default is lower, stops the build with error `KSDLG0001`.

## Minimal migration

The operation-scoped loading route keeps the same default entry and progress shape. Remove the old `isCurrentScope` argument; pass a `DialogPlacement` when the display must move. Whatever thread the caller is on, the operation starts on the UI thread by default (`LoadingActionThread.Main`), so it can touch screen elements without moving back to the UI thread. For heavy work that does not touch the UI, pass `actionThread: LoadingActionThread.Background` to start it off the UI thread.

```csharp
using KsDialogs;

namespace MyApp;

public static class StartupWork
{
    public static Task RunAsync() =>
        Loading.Instance.StartAsync(
            async progress =>
            {
                progress.Report(0.5);
                await Task.Yield();
                progress.Report(1);
            },
            message: "Loading");
}
```

## Failures, waiting, and cancellation

Setup mistakes surface as `DialogException` subtypes rather than as a cancelled result. One new subtype has no counterpart in the old library: when a one-line registration (`RegisterForDialog`, `RegisterForLoading`, `RegisterForToast`) cannot build the View it wired, the failure is `DialogException.ViewCreationFailed`, which keeps the original failure in `InnerException` and exposes `ViewTypeName` and `ViewModelTypeName`. Only the route where the library itself builds the View is wrapped — an exception thrown by a factory you wrote or by a fallback resolver arrives unwrapped. Dialog and Loading report it as a faulted show or start task; `Toast.Show` returns no task, so it discards that one toast with a warning that names the cause.

On iOS and Android, a call made while there is no screen to present on (no presentation host) does not fail. When you call from the first screen's appearance code right after launch, while the app is in the background, or while a system permission dialog is up, the display waits until a screen appears. Dialog, Loading, and Toast share this behavior, and Loading keeps running its operation while it waits. `DialogException.PresentationHostUnavailable` arrives only when a dialog is shown on plain .NET, which has no presentation machinery (the `net10.0` target of a unit test, for example).

The wait has no upper bound, so pass the trailing `CancellationToken` of `ShowAsync` to a dialog called from a place where a screen may never appear. Cancelling during the wait means the dialog is never shown; cancelling while it is shown closes it; either way `OperationCanceledException` is thrown. Cancellation is not converted to a `Cancelled` result: `Cancelled` is returned when the dialog closes through a cancel report, an outside tap, or the destruction of its screen.

## Choose the mapping

- Read [API mapping](references/api-mapping.md) for the old-to-new table and rewrite recipes, grouped by setup, dialogs, result reporting, loading, layout and lifecycle, and toast.
- After locating a replacement, use the `ksdialogs-maui` Skill for a complete new-API recipe.
