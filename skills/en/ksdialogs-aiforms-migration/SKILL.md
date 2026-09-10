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
| Animation | `RunPresentationAnimation` / `RunDismissalAnimation` overrides | `DialogTransition` attached with `Dialog.SetTransition` |
| Registration and IoC | `Configurations.SetIocConfig` | `RegisterForDialog` / `RegisterForLoading` / `RegisterForToast`, and `AddKsDialogs` fallbacks for dialogs |
| Show from a model type | `ShowFromModelAsync`, `CreateFromModel` | a type-based `ShowAsync` / `Show` for dialogs, loading, and toasts, with a `configure` callback |
| Loading settings | `LoadingConfig` | `Loading.Instance.Style` (`LoadingStyle`) and `Loading.Instance.Options` (`DialogOptions`) |
| Toast | obsolete, custom `ToastView` routes only | `IKsToast` with message, registered, inline, and type-based routes |
| Changed defaults | transparent overlay, dialog margin 0, window-wide layout area | 40% black overlay, dialog margin 24 on every side, visible area |

## Capability map

| Goal | Where to look |
|---|---|
| Replace the package, namespace, and IoC setup | Setup below, then [API mapping](references/api-mapping.md) |
| Migrate dialog show, result, ViewModel, and notifier code | [API mapping](references/api-mapping.md) |
| Replace reusable dialogs and loading handles | [API mapping](references/api-mapping.md) |
| Replace model-type-based show (`ShowFromModelAsync`, `CreateFromModel`) | [API mapping](references/api-mapping.md) |
| Move layout, overlay, and animation settings off `ExtraView` | [API mapping](references/api-mapping.md) |
| Replace the obsolete custom-view-only toast API | [API mapping](references/api-mapping.md) |
| Understand how a mis-wired registration fails now | Failures after migrating below |
| Look up the KsDialogs.Maui API itself | the `ksdialogs-maui` Skill |

## Setup

Remove the `AiForms.Maui.Dialogs` package and add `KsDialogs.Maui` to a .NET 10 MAUI project. The package is not published on NuGet yet, so `<version>` below stands for the version of the package you obtained. Replace `using AiForms.Dialogs;` with `using KsDialogs;`.

```xml
<PackageReference Include="KsDialogs.Maui" Version="<version>" />
```

The project also needs `Microsoft.Maui.Controls` 10.0.20 or later. That is the version bundled with the .NET workload set this library is built and tested against, so a project on the same workload set can leave the version unwritten; pinning an older one (below 10.0.20) makes restore report NU1605, the NuGet package-downgrade error. Use the .NET 10 SDK with the iOS and Android MAUI workloads installed. KsDialogs.Maui supports iOS 17.0 or later and Android 7.0 (API 24) or later; a `SupportedOSPlatformVersion` below that, including one left unset where the SDK default is lower, stops the build with error `KSDLG0001`.

## Minimal migration

The operation-scoped loading route keeps the same default entry and progress shape. Remove the old `isCurrentScope` argument; pass a `DialogPlacement` when the display must move.

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

## Failures after migrating

Setup mistakes surface as `DialogException` subtypes rather than as a cancelled result. One new subtype has no counterpart in the old library: when a one-line registration (`RegisterForDialog`, `RegisterForLoading`, `RegisterForToast`) cannot build the View it wired, the failure is `DialogException.ViewCreationFailed`, which keeps the original failure in `InnerException` and exposes `ViewTypeName` and `ViewModelTypeName`. Only the route where the library itself builds the View is wrapped — an exception thrown by a factory you wrote or by a fallback resolver arrives unwrapped. Dialog and Loading report it as a faulted show or start task; `Toast.Show` returns no task, so it discards that one toast with a warning that names the cause.

## Choose the mapping

- Read [API mapping](references/api-mapping.md) for the old-to-new table and rewrite recipes, grouped by setup, dialogs, result reporting, loading, layout and lifecycle, and toast.
- After locating a replacement, use the `ksdialogs-maui` Skill for a complete new-API recipe.
