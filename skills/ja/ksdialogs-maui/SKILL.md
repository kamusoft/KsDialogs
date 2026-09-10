---
name: ksdialogs-maui
description: KsDialogs の .NET MAUI API を使い、Dialog・Loading・Toast、型付き結果、dependency injection、layout、transition を実装する。
license: MIT
metadata:
  language: ja
  source: https://github.com/kamusoft/KsDialogs
---

# .NET MAUI 向け KsDialogs

KsDialogs は、アプリのどこからでも Dialog を呼び出せる UI ライブラリ。中身は自分で書いた View を登録しておくか呼び出し時に渡し、呼び出し側は表示を頼んで結果を待つだけでよい。表示できるのは 3 種類 — 利用者の応答を受け取る Dialog、処理中の操作をブロックする Loading、非対話の通知を出す Toast。この Skill が扱うのは .NET MAUI 版で、通常は既定入口 (`Dialog.Instance`、`Loading.Instance`、`Toast.Instance`) から呼ぶ。テストや DI 構成では、`Dialog`、`Loading`、`Toast` を `IKsDialog`、`IKsLoading`、`IKsToast` として注入できる。

既定入口から呼んでも契約を注入して呼んでも、届く先はプロセス内の同じ状態である。View の登録先は 1 種類につき 1 つで、`Dialog.Instance.Registry` は `DialogViewRegistry.Shared`、`Loading.Instance.Registry` は `LoadingViewRegistry.Shared`、`Toast.Instance.Registry` は `ToastViewRegistry.Shared` を指す。アプリ全体の設定も同じ場所にあり、`Loading.Instance.Style`、`Loading.Instance.Options`、`Toast.Instance.Style` に置いた値はどの入口から表示しても効く。content になれるのは MAUI の `View` だけなので、UI 技術ごとに登録先が分かれることはない。

## 能力マップ

| やりたいこと | API | レシピ |
|---|---|---|
| Dialog を登録して表示する | `IDialogViewModel`、`DialogViewRegistry.Register`、`Dialog.Instance.ShowAsync` | [Dialog](references/dialogs.md) |
| ViewModel から結果を報告する・型を渡して表示する | `DialogNotifier<TResult>`、`Notifier`、`RegisterViewModel`、型指定 `ShowAsync` | [ViewModel](references/view-models.md) |
| 大きさ・配置・覆い・外側タップを制御する | `Dialog` の attached property、`DialogOptions`、`DialogPlacement` | [レイアウト](references/layout.md) |
| 出現と退出をアニメーションさせる | `DialogTransition`、`DialogTransitionEdge`、`Dialog.SetTransition` | [トランジション](references/transitions.md) |
| 処理中の操作をブロックする | `Loading.Instance`、`LoadingViewRegistry`、`LoadingStyle`、`ILoadingProgressReceiver` | [Loading](references/loading.md) |
| fire-and-forget の通知を表示する | `Toast.Instance`、`ToastViewRegistry`、`ToastStyle` | [Toast](references/toast.md) |
| MAUI DI で View と ViewModel を配線する | `AddKsDialogs`、`RegisterForDialog`、`RegisterForLoading`、`RegisterForToast` | [DI 登録](references/di-registration.md) |

## セットアップ

.NET 10 の MAUI project に `KsDialogs.Maui` を追加する。package は NuGet にまだ公開していないため、下の `<version>` は入手した package の version を指す。利用するファイルで `KsDialogs` namespace を import する。

```xml
<PackageReference Include="KsDialogs.Maui" Version="0.1.0-beta.1" />
```

| 要件 | 満たすもの |
|---|---|
| MAUI | `Microsoft.Maui.Controls` 10.0.20 以降。これはライブラリ側がビルドとテストに使う .NET workload set 同梱の version なので、同じ workload set の project なら version を書かなくてよい。それより古い version (10.0.20 未満) を明示すると restore が NU1605 (NuGet の package ダウングレードのエラー) を報告する |
| .NET SDK | iOS / Android の MAUI workload を入れた .NET 10 |
| 最低 OS 版 | iOS 17.0 と Android 7.0 (API 24)。これを下回る `SupportedOSPlatformVersion` は、SDK の既定値が下回る未設定の場合も含めて、ビルドを `KSDLG0001` のエラーで止める。エラーには必要な version と現在の値が出る。検査が走るのは iOS・Android の inner build だけである |

## 最小例

```csharp
using KsDialogs;

namespace MyApp;

public static class Notifications
{
    public static void ShowSaved() => Toast.Instance.Show("Saved");
}
```

## レシピを選ぶ

| やりたいこと | 読むレシピ |
|---|---|
| 登録、型付き結果、インライン content、多段表示、構成ミスの失敗 | [Dialog](references/dialogs.md) |
| `Notifier`、ViewModel factory、表示前 configure、Dialog / Loading / Toast に共通する型指定の表示 | [ViewModel](references/view-models.md) |
| XAML と code-behind からの添付、配置、margin、比率サイズ、overlay、外側タップキャンセル | [レイアウト](references/layout.md) |
| preset と非同期 custom hook | [トランジション](references/transitions.md) |
| 命令形・スコープ形の Loading、進捗、style、custom content | [Loading](references/loading.md) |
| message、登録、インラインの Toast 経路 | [Toast](references/toast.md) |
| 1 行登録、fallback 解決、View の組み立て失敗 | [DI 登録](references/di-registration.md) |
