---
type: concept
title: MAUI の Loading 公開面
description: .NET MAUI (C#) で Loading を使うときの公開名と署名 — 契約と既定エントリ・ShowAsync / HideAsync / SetMessage / StartAsync の署名・IProgress の報告口と進捗受け口・カスタム View の登録と DI 糖衣・型指定 show / start と VM factory 登録 (オーバーロード束縛の注意)・LoadingStyle と器メタ属性・中身は MAUI の View だけであること
tags: [maui, loading, api, surface]
timestamp: 2026-09-06
---

# MAUI の Loading 公開面

この文書を読むと、.NET MAUI (C#) で Loading を表示・カスタマイズするときに書く型名・プロパティ名・メソッドの署名とコード例が分かる。

**この文書は MAUI の公開面 (名前・署名・コード例・framework 固有の注意) だけを扱い、挙動の契約は core が正である。** 対応する契約は [Loading のルール](../../core/api/loading-semantics.md) で、合流の数え方・世代・器の性質・保証と禁止はそちらを読む。

## 契約と入口

| 用途 | 書く名前 |
|---|---|
| DI で注入する契約 | `IKsLoading` (実体は `Loading`) |
| 既定の表示エントリ | `Loading.Instance` |
| レジストリのハンドル | `Loading.Instance.Registry` (型は `LoadingViewRegistry`。実体は `LoadingViewRegistry.Shared`) |
| 既定ローディングの styling | `Loading.Instance.Style` (型は `LoadingStyle`) |
| 既定ローディングの器メタ属性 | `Loading.Instance.Options` (型は `DialogOptions`) |

`new Loading()` を `IKsLoading` として注入しても、既定エントリと同じレジストリ・同じ一括設定を共有する。表示の合流状態は Native ライブラリ側が持つので、C# 側の入口が複数あっても表示は 1 つに合流する。

## 操作の署名

| 操作 | 署名 |
|---|---|
| 既定ローディングの表示 | `ShowAsync(message, placement)` |
| 登録済みカスタム View の表示 | `ShowAsync(viewModel, placement)` |
| インライン factory での表示 | `ShowAsync<TViewModel>(viewModel, factory, placement)` |
| 型指定 show (VM の型を渡す) | `ShowAsync<TViewModel>(configure, placement)` (configure は `Action<TViewModel>?` と `Func<TViewModel, Task>` の 2 形。省略可) |
| 閉鎖 | `HideAsync()` |
| メッセージの差し替え | `SetMessage(message)` (唯一の同期メソッド。待たない) |
| スコープ形 | `StartAsync(action, message, placement)` / `StartAsync<T>(...)` / `StartAsync(viewModel, action, placement)` / `StartAsync<TViewModel>(viewModel, factory, action, placement)` |
| 型指定 start | `StartAsync<TViewModel>(action, configure, placement)` / `StartAsync<TViewModel, T>(action, configure, placement)` (action が先頭。configure は同じく 2 形) |

**閉鎖の綴りは `HideAsync`** で、他形態のような短い名前ではない (C# の非同期メソッドの命名慣習に従う)。スコープ形は値を返さない形と `Task<T>` を返す形の対で提供する。

スコープ形の処理に渡る**進捗報告口**は `IProgress<double>` である。

```csharp
var uploaded = await Loading.Instance.StartAsync(async progress =>
{
    return await UploadAsync(progress);   // progress.Report(0.0〜1.0) で進捗を報告する
}, message: "アップロード中…");
```

## カスタム View の登録

中身になれるのは MAUI の `View` だけである (宣言的 UI 系の別経路は無い)。カスタム Loading の ViewModel は `ILoadingViewModel` を実装させ、レジストリの `Register<TViewModel>(Func<TViewModel, View> factory)` で登録する。

```csharp
Loading.Instance.Registry.Register<UploadViewModel>(vm => new UploadLoadingView(vm));
```

DI から使う構成では 1 行登録の糖衣 `RegisterForLoading<TView, TViewModel>()` がある — View factory と VM factory (サービスから引く) の両スロットを配線し、`TView` / `TViewModel` を transient としてサービスに登録する ([MAUI の DI 連携と登録糖衣](di-registration.md) の Dialog 版と同じ生成規則)。この 1 行だけでインスタンス渡しの表示と型指定 show / start の両方が使える。

```csharp
builder.Services.RegisterForLoading<UploadLoadingView, UploadViewModel>();
```

進捗を中身へ届けたい ViewModel は `ILoadingProgressReceiver` を実装し、`OnProgress(double)` で受け取る。**メソッド名は PascalCase の `OnProgress`** である。実装しない ViewModel では転送されない。未登録の型で表示すると `DialogException.ViewFactoryNotRegistered` で失敗する。

## 型指定 show / start と VM factory

| 書き方 | 用途 |
|---|---|
| `ShowAsync<TViewModel>(configure, placement)` | 型を渡す表示。configure は `Action<TViewModel>?` (同期) または `Func<TViewModel, Task>` (非同期) |
| `StartAsync<TViewModel>(action, configure, placement)` | 型を渡すスコープ形 (値なし)。`StartAsync<TViewModel, T>` が値あり |
| `RegisterViewModel<TViewModel>(Func<TViewModel> factory)` | VM factory の登録 (View factory とは別に登録する低水準 API) |

1 つの ViewModel 型に対して View factory と VM factory は別々に登録され、型指定 show / start を使うには両方が要る。`RegisterForLoading` を使えば両スロットの配線と DI 登録がまとめて済む。型引数の制約は `where TViewModel : class, ILoadingViewModel` で、値型の ViewModel はコンパイル時に弾かれる。挙動 (生成 → configure → 進捗受け口の紐付け → 中身の生成の順序、合流との関係、失敗の扱い) は [Loading のルール](../../core/api/loading-semantics.md) と [ViewModel 主導の呼び出しのルール](../../core/api/model-binding-semantics.md) が正である。

```csharp
builder.Services.RegisterForLoading<UploadLoadingView, UploadViewModel>();

var uploaded = await Loading.Instance.StartAsync<UploadViewModel, bool>(
    async progress => await UploadAsync(progress),
    vm => vm.Title = "アップロード中");           // configure: 中身の生成前に必ず完了する
```

### オーバーロードの束縛

既存の `StartAsync<T>` (メッセージ入口の値あり) と型指定の `StartAsync<TViewModel>` は型引数 1 個で並ぶが、実引数の型で一意に束縛される — action (`Func<IProgress<double>, Task>` / `Task<T>`) が先頭で、configure は ViewModel 型を受ける `Action` / `Func` だからである。async lambda の configure は `Func<TViewModel, Task>` 側に束縛される。引数をすべて `null` リテラルで渡す呼び出し (`ShowAsync<VM>(null, null)` など) は既存のインライン factory 版との間で曖昧になるが、意味のある呼び出しではないため互換性の対象にしない (Dialog の型指定 show と同じ)。

## `LoadingStyle` のプロパティ

`record` なので `with` 式で一部だけ差し替えられる。

| プロパティ | 型 | 既定値 |
|---|---|---|
| `IndicatorColor` | `Color` | `Colors.White` |
| `MessageFontSize` | `double` | `14d` |
| `MessageColor` | `Color` | `Colors.White` |
| `DefaultMessage` | `string?` | `null` |
| `ProgressFormat` | `Func<string?, double?, string>` | `LoadingStyle.DefaultProgressFormat` |

器メタ属性は `DialogOptions` をそのまま使う ([MAUI のレイアウト公開面](layout-surface.md) の束ねた値オブジェクト)。

```csharp
Loading.Instance.Style = Loading.Instance.Style with { DefaultMessage = "処理中…" };
```

## framework 固有の注意

- **カスタム View への属性・演出の添付はダイアログと同じ添付プロパティ** (`ksd:Dialog.*`) を使う ([MAUI のレイアウト公開面](layout-surface.md))。既定ローディングには添付する View が無いため `Loading.Instance.Options` がその代わりになる
- **`IsCanceledOnTouchOutside` は Loading では常に無効**で、添付しても設定しても効かない
- **VM factory 未登録の型指定 show / start は `DialogException.ViewModelFactoryNotRegistered`** で失敗する (View factory 未登録とは別の例外)
- **fallback resolver は Loading には効かない** — `AddKsDialogs` の fallback は Dialog レジストリの機構で、Loading は明示登録か 1 行登録のみ
- **レイアウト計算は Native 側**で行う。MAUI の面は属性を無変換で渡すパススルーである
- **カスタム View 版の演出**は中身への `DialogTransition` 添付で差し替える ([MAUI のトランジション公開面](transition-surface.md))。既定ローディングにはこの口が無い

## 関連

- [Loading のルール](../../core/api/loading-semantics.md) — 合流・世代・器の性質・保証と禁止 (契約の正)
- [ViewModel 主導の呼び出しのルール](../../core/api/model-binding-semantics.md) — 型指定 show の順序保証と VM factory 解決 (3 機能共通の契約)
- [MAUI の DI 連携と登録糖衣](di-registration.md) — サービス登録と 1 行登録の規則
- [MAUI のレイアウト公開面](layout-surface.md) — `DialogOptions` / `DialogPlacement` と添付プロパティ
- [MAUI の Toast 公開面](toast-surface.md) — もう 1 つの非ダイアログ表示の公開面
