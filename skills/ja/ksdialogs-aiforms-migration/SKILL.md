---
name: ksdialogs-aiforms-migration
description: AiForms.Maui.Dialogs から KsDialogs.Maui へ .NET MAUI の Dialog・Loading・Toast コードを移行するための Skill。旧公開 API をメンバー単位で対応付け、廃止された再利用 View・ライフサイクルフック・レイアウトプロパティ・IoC 設定の代替を示す。
license: MIT
metadata:
  language: ja
  source: https://github.com/kamusoft/KsDialogs
---

# AiForms.Maui.Dialogs から移行する

KsDialogs.Maui は AiForms.Maui.Dialogs を、Dialog の結果型・factory による content・合流する Loading・非対話の Toast 通知へ置き換える。既定エントリ (`Dialog.Instance`、`Loading.Instance`、`Toast.Instance`) から始め、アプリが dependency injection を使う場合は代わりに `IKsDialog`、`IKsLoading`、`IKsToast` を注入する — どちらの経路も同じレジストリと同じアプリ全体の設定を共有する。この Skill は旧利用者向け API と新しい面の対応を示す。新 API の完全なレシピと詳細は `ksdialogs-maui` Skill を使う。

## 何が変わるか

| 対象 | AiForms.Maui.Dialogs | KsDialogs.Maui |
|---|---|---|
| エントリと契約 | `Dialog.Instance` / `Loading.Instance` / `Toast.Instance` と `IDialog` / `ILoading` / `IToast` | 既定エントリは同じで、契約は `IKsDialog` / `IKsLoading` / `IKsToast` |
| show の動詞 | `ShowAsync`、`ShowResultAsync`、`ShowFromModelAsync`、`ShowResultFromModelAsync` | `ShowAsync` 一族に統一。結果型は ViewModel が宣言する |
| 結果 | 結果値は型消去され、cancel は結果型の既定値を返す | `Completed` と `Cancelled` の枝を持つ `DialogResult<TResult>` |
| content | `DialogView`・`ExtraView`・`LoadingView`・`ToastView` を継承する | 登録済みまたは inline の factory が返す通常の MAUI `View` |
| 再利用 | `IReusableDialog` と `IReusableLoading` の handle | 再利用 handle はなく、content は表示ごとに作り直す |
| 結果報告 | View に bind した `DialogNotifier` | factory から渡る型付き `DialogNotifier<TResult>`、または `viewModel.Notifier` |
| レイアウト属性 | `ExtraView` と `DialogView` の property | `Dialog.*` 添付 property、`DialogPlacement`、`DialogOptions` |
| 基準領域 | 真偽値の `UseCurrentPageLocation` (垂直方向にだけ効く) | 両軸に効く `DialogLayoutArea` (`Window` / `VisibleArea` / `CurrentPage`) |
| animation | `RunPresentationAnimation` / `RunDismissalAnimation` の override | `Dialog.SetTransition` で添付する `DialogTransition` |
| 登録と IoC | `Configurations.SetIocConfig` | `RegisterForDialog` / `RegisterForLoading` / `RegisterForToast` と、Dialog に効く `AddKsDialogs` の fallback |
| 型から show する | `ShowFromModelAsync`、`CreateFromModel` | Dialog / Loading / Toast それぞれの型を渡す `ShowAsync` / `Show` と `configure` コールバック |
| Loading の設定 | `LoadingConfig` | `Loading.Instance.Style` (`LoadingStyle`) と `Loading.Instance.Options` (`DialogOptions`) |
| Toast | obsolete で custom `ToastView` 経路のみ | message・登録・inline・型指定の経路を持つ `IKsToast` |
| 変わった既定値 | 透明な overlay、window 全体の layout area | 黒 40% の overlay、visible area (dialog margin の既定は旧と同じ全辺 0) |

## 能力マップ

| やりたいこと | 読む場所 |
|---|---|
| package・namespace・IoC 設定を置き換える | 下の「導入」と [API 対応表](references/api-mapping.md) |
| Dialog の show・結果・ViewModel・notifier を移行する | [API 対応表](references/api-mapping.md) |
| 再利用 Dialog と Loading handle を置き換える | [API 対応表](references/api-mapping.md) |
| 型から show する旧経路 (`ShowFromModelAsync`・`CreateFromModel`) を置き換える | [API 対応表](references/api-mapping.md) |
| layout・overlay・animation 設定を `ExtraView` から移す | [API 対応表](references/api-mapping.md) |
| `UseCurrentPageLocation` を表示中のページ基準 (`DialogLayoutArea.CurrentPage`) へ移す | [API 対応表](references/api-mapping.md) |
| Loading の処理を始めるスレッドを選ぶ (`LoadingActionThread`) | 下の「最小移行」と [API 対応表](references/api-mapping.md) |
| obsolete の custom View 専用 Toast API を置き換える | [API 対応表](references/api-mapping.md) |
| 登録の配線ミスがどう失敗するかを知る | 下の「移行後の失敗・待ち・打ち切り」 |
| 画面がまだ無いときの show の待ちと、`CancellationToken` による打ち切りを知る | 下の「移行後の失敗・待ち・打ち切り」と [API 対応表](references/api-mapping.md) |
| KsDialogs.Maui の API 自体を調べる | `ksdialogs-maui` Skill |

## 導入

`AiForms.Maui.Dialogs` package を外し、.NET 10 の MAUI project に nuget.org の `KsDialogs.Maui` を追加する。project に書く参照はこれだけでよい。native 側を運ぶ binding package `KsDialogs.Binding.iOS` と `KsDialogs.Binding.Android` は iOS / Android の target framework で推移的に届くので、直接参照しない。`using AiForms.Dialogs;` は `using KsDialogs;` へ置き換える。

```xml
<PackageReference Include="KsDialogs.Maui" Version="{version}" />
```

`{version}` はプレースホルダで、使う version に置き換える (そのままでは復元に失敗する)。現在の version は、常に最新のリリースへ解決される [latest release](https://github.com/kamusoft/KsDialogs/releases/latest) のページで確認できる。

project には `Microsoft.Maui.Controls` 10.0.20 以降も要る。project に MAUI の version を書かなければ、入っている .NET workload set に同梱の version が使われ、それが 10.0.20 以降ならそのまま導入できる。10.0.20 未満の version は、明示した場合も、古い workload set の同梱版がそうである場合も、restore が NU1605 (NuGet の package ダウングレードのエラー) を報告する。そのときは 10.0.20 以降を明示するか、workload set を更新する。.NET SDK は iOS / Android の MAUI workload を入れた .NET 10 を使う。KsDialogs.Maui は iOS 17.0 以降と Android 7.0 (API 24) 以降に対応し、これを下回る `SupportedOSPlatformVersion` は、SDK の既定値が下回る未設定の場合も含めて、ビルドを `KSDLG0001` のエラーで止める。

## 最小移行

処理スコープ付き Loading は、既定エントリと progress の形を維持している。旧 `isCurrentScope` 引数は削除する。表示位置を変える必要がある場合は `DialogPlacement` を渡す。渡した処理は、呼び出し元のスレッドによらず既定 (`LoadingActionThread.Main`) で UI スレッドで始まるので、処理の中から UI スレッドへ移し直さずに画面の要素へ触れる。UI に触れない重い処理は `actionThread: LoadingActionThread.Background` を渡して UI スレッド外で始める。

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

## 移行後の失敗・待ち・打ち切り

構成ミスは cancel された結果ではなく `DialogException` の入れ子の型として届く。そのうち 1 つは移植元に対応物が無い。1 行登録 (`RegisterForDialog`・`RegisterForLoading`・`RegisterForToast`) が結び付けた View をライブラリが組み立てられなかった場合、失敗は `DialogException.ViewCreationFailed` になり、元の失敗は `InnerException` にそのまま残り、`ViewTypeName` と `ViewModelTypeName` で型名を読める。包まれるのはライブラリ自身が View を組み立てる経路だけで、利用者が書いた factory や fallback resolver が投げた例外は包まれずそのまま届く。Dialog と Loading では show / start の task の失敗として届き、`Toast.Show` は task を返さないため、原因を書いた警告を残してその 1 枚だけを破棄する。

iOS / Android では、出す先の画面 (提示先) がまだ無いときに呼んでも失敗しない。アプリの起動直後に最初の画面の表示処理から呼んだ場合、アプリが背面にいる場合、システムの許可ダイアログが出ている場合などは、画面が現れるのを待ってから表示する。これは Dialog・Loading・Toast で共通で、Loading は待っている間も処理を実行する。`DialogException.PresentationHostUnavailable` が届くのは、表示の仕組みを持たない素の .NET (単体テストの `net10.0` など) で Dialog を show した場合だけである。

待ちに上限は無いので、画面が現れる見込みの無い場所から呼ぶ Dialog には、`ShowAsync` の末尾の `CancellationToken` を渡す。打ち切ると、待っている間ならダイアログは一度も表示されず、表示中ならダイアログが閉じ、どちらも `OperationCanceledException` が投げられる。打ち切りは結果の `Cancelled` には変換されない。`Cancelled` が返るのは、キャンセルの報告・外側タップ・画面の破棄でダイアログが閉じた場合である。

## 対応表を選ぶ

- [API 対応表](references/api-mapping.md) には、setup・Dialog・結果報告・Loading・layout と lifecycle・Toast に分けた新旧対応と、書き換えのコードがある。
- 対応先を見つけた後は、完全な新 API のレシピを `ksdialogs-maui` Skill で確認する。
