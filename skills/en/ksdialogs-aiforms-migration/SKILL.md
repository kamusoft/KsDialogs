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
| Look up the KsDialogs.Maui API itself | the `ksdialogs-maui` Skill |

## Setup

Remove the `AiForms.Maui.Dialogs` package and add `KsDialogs.Maui` version `0.1.0` to a .NET 10 MAUI project (`net10.0-ios` / `net10.0-android`, Microsoft.Maui.Controls 10.0.1). KsDialogs.Maui supports iOS 17 or later and Android 7.0 (API 24) or later. Replace `using AiForms.Dialogs;` with `using KsDialogs;`.

```xml
<PackageReference Include="KsDialogs.Maui" Version="0.1.0" />
```

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

## Choose the mapping

- Read [API mapping](references/api-mapping.md) for the old-to-new table and rewrite recipes, grouped by setup, dialogs, result reporting, loading, layout and lifecycle, and toast.
- After locating a replacement, use the `ksdialogs-maui` Skill for a complete new-API recipe.
