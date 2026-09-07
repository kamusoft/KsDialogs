---
type: concept
title: MAUI の Toast 公開面
description: .NET MAUI (C#) で Toast を使うときの公開名と署名 — 契約と既定エントリ・4 経路の Show と durationMs 引数・カスタム View の登録と DI 糖衣・型指定 Show と VM factory 登録・ToastStyle のプロパティ・戻り値を持たないことと器メタ属性の受け口が無いこと
tags: [maui, toast, api, surface]
timestamp: 2026-09-06
---

# MAUI の Toast 公開面

この文書を読むと、.NET MAUI (C#) で Toast を表示・カスタマイズするときに書く型名・プロパティ名・メソッドの署名とコード例が分かる。

**この文書は MAUI の公開面 (名前・署名・コード例・framework 固有の注意) だけを扱い、挙動の契約は core が正である。** 対応する契約は [Toast のルール](../../core/api/toast-semantics.md) で、duration の時間モデル・失敗モデル・多重表示・非対話の保証はそちらを読む。

## 契約と入口

| 用途 | 書く名前 |
|---|---|
| DI で注入する契約 | `IKsToast` (実体は `Toast`) |
| 既定の表示エントリ | `Toast.Instance` |
| レジストリのハンドル | `Toast.Instance.Registry` (型は `ToastViewRegistry`。実体は `ToastViewRegistry.Shared`) |
| 一括設定 | `Toast.Instance.Style` (型は `ToastStyle`) |

`new Toast()` を `IKsToast` として注入しても、既定エントリと同じレジストリ・同じ一括設定を共有する。

## 4 経路の Show

**`Show` は `void` を返す同期メソッド**で、`Async` 接尾辞を持たない — fire-and-forget であり待つ対象が無いためである。

| 経路 | 署名 |
|---|---|
| メッセージ入口 | `Show(message, durationMs, placement)` |
| 登録経路 | `Show(viewModel, durationMs, placement)` |
| インライン経路 | `Show<TViewModel>(viewModel, factory, durationMs, placement)` |
| 型指定経路 (VM の型を渡す) | `Show<TViewModel>(configure, durationMs, placement)` (configure は `Action<TViewModel>?` のみ・省略可) |

**表示時間の引数名は `durationMs`** (`int?`、ミリ秒) である。`placement` は `DialogPlacement?` で、どちらも省略できる。

```csharp
Toast.Instance.Show("保存しました");
Toast.Instance.Show("保存しました", durationMs: 2500);
```

## カスタム View の登録

中身になれるのは MAUI の `View` だけである。カスタム Toast の ViewModel は `IToastViewModel` を実装させ、レジストリの `Register<TViewModel>(Func<TViewModel, View> factory)` で登録する。DI から使う構成では 1 行登録の糖衣 `RegisterForToast<TView, TViewModel>()` がある — View factory と VM factory (サービスから引く) の両スロットを配線する ([MAUI の DI 連携と登録糖衣](di-registration.md) の Dialog 版と同じ生成規則) ので、この 1 行だけでインスタンス渡しと型指定の両方の Show が使える。

```csharp
builder.Services.RegisterForToast<NoticeToastView, NoticeViewModel>();

Toast.Instance.Show(new NoticeViewModel("同期が完了しました"), durationMs: 2000);
```

同じ ViewModel 型を Dialog / Loading のレジストリにも登録できる (レジストリは互いに独立している)。未登録の型で表示すると `DialogException.ViewFactoryNotRegistered` で失敗する。

## 型指定 Show と VM factory

ViewModel のインスタンスではなく型引数だけを渡す形で、`Toast.Instance.Show<NoticeViewModel>(vm => vm.Message = "...")` と書く。VM factory の登録は `RegisterViewModel<TViewModel>(Func<TViewModel> factory)` (View factory とは別の低水準 API) か、両スロットをまとめて配線する `RegisterForToast` で行う。型引数の制約は `where TViewModel : class, IToastViewModel` で、値型の ViewModel はコンパイル時に弾かれる。configure は同期の `Action<TViewModel>` だけで、非同期形は無い (Show が fire-and-forget の同期メソッドのため)。挙動 (生成 → configure → 中身の生成の順序、失敗の分類) は [Toast のルール](../../core/api/toast-semantics.md) と [ViewModel 主導の呼び出しのルール](../../core/api/model-binding-semantics.md) が正である。

```csharp
builder.Services.RegisterForToast<NoticeToastView, NoticeViewModel>();

Toast.Instance.Show<NoticeViewModel>(
    vm => vm.Message = "同期が完了しました",       // configure: 中身の生成前に必ず完了する
    durationMs: 2000);
```

VM factory 未登録は `DialogException.ViewModelFactoryNotRegistered` として呼び出し時点で同期に失敗する。VM factory / configure が投げた例外は呼び出し元へは返らず「受理後の失敗」(警告を残してその 1 枚だけ破棄) になる。`AddKsDialogs` の fallback resolver は Dialog レジストリの機構で、Toast には効かない (明示登録か 1 行登録のみ)。

## `ToastStyle` のプロパティ

`record` なので `with` 式で一部だけ差し替えられる。

| プロパティ | 型 | 既定値 |
|---|---|---|
| `BackgroundColor` | `Color` | `ToastStyle.BuiltinBackgroundColor` (わずかに透過するダークグレー) |
| `TextColor` | `Color` | `Colors.White` |
| `FontSize` | `double` | `14d` |
| `CornerRadius` | `double` | `22d` |
| `DefaultDuration` | `int` | `ToastStyle.BuiltinDefaultDuration` (`1500`) |
| `DefaultPlacement` | `DialogPlacement?` | `null` |

```csharp
Toast.Instance.Style = Toast.Instance.Style with { DefaultDuration = 2000 };
```

## framework 固有の注意

- **閉じる操作が無い** — Loading の `HideAsync` にあたるメソッドも、メッセージ更新もスコープ形も進捗の報告口も、この契約には無い ([MAUI の Loading 公開面](loading-surface.md))
- **器メタ属性の受け口が無い** — 覆いの色や外側タップの扱いを運ぶプロパティを `IKsToast` は持たない。渡せるのは配置 (`DialogPlacement`) だけである
- **カスタム View への配置・演出の添付はダイアログと同じ添付プロパティ** (`ksd:Dialog.*`) を使う ([MAUI のレイアウト公開面](layout-surface.md))
- **レジストリは C# 層にある** — Native 側のレジストリとは層が別で、MAUI から登録した View は MAUI の表示だけに使われる

## 関連

- [Toast のルール](../../core/api/toast-semantics.md) — duration・失敗モデル・多重表示・非対話 (契約の正)
- [ViewModel 主導の呼び出しのルール](../../core/api/model-binding-semantics.md) — 型指定 show の順序保証と VM factory 解決 (3 機能共通の契約)
- [MAUI の Loading 公開面](loading-surface.md) — 操作の多い側との対比
- [MAUI の DI 連携と登録糖衣](di-registration.md) — サービス登録と 1 行登録の規則
- [MAUI のレイアウト公開面](layout-surface.md) — `DialogPlacement` と添付プロパティ
