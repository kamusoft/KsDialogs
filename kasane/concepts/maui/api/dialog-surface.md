---
type: concept
title: MAUI の Dialog 公開面
description: .NET MAUI (C#) からダイアログを使うときの公開名と署名 — 既定エントリと登録の入口・真偽値の顔・中身は MAUI の View だけであること・インライン show・結果報告口の取得・構成ミスの例外型・型指定 show と非同期 configure・移植元の API 名との対応
tags: [maui, dialog, api, surface]
timestamp: 2026-09-08
---

# MAUI の Dialog 公開面

この文書を読むと、.NET MAUI (C#) からダイアログを登録・表示するときに書く名前と署名、コード例、C# 固有の注意が分かる。KsDialogs の MAUI 実装は AiForms.Maui.Dialogs (以下**移植元**) からの移植であり、移植元の名前との対応も末尾に置いている。

**この文書は MAUI の公開面 (名前・署名・コード例・framework 固有の注意) だけを扱い、挙動の契約は core が正である。** 対応する契約は次の 4 本で、「何が起きるか」はそちらを読む:

- [登録と表示の呼び出し面のルール](../../core/api/registration-show-semantics.md) — register / show の基本形・結果型の省略形・中身の技術・インライン show
- [結果通知のルール](../../core/api/result-notification-semantics.md) — show が返すもの・キャンセル・構成ミス
- [多段表示のルール](../../core/api/multi-display-semantics.md) — 重ね出しの保証
- [ViewModel 主導の呼び出しのルール](../../core/api/model-binding-semantics.md) — 結果報告口の VM 供給・型指定 show・参照型限定

## 登録と表示の入口

| 用途 | 書く名前 |
|---|---|
| 既定の表示エントリ | `Dialog.Instance` |
| DI で注入する表示契約 | `IKsDialog` (実体は `Dialog`) |
| 登録の入口 | `Dialog.Instance.Registry` (実体は `DialogViewRegistry.Shared`) |
| 表示 | `ShowAsync(...)` (`Task<DialogResult<TResult>>` を返す) |

DI から使う構成と1行登録の糖衣は [MAUI の DI 連携と登録糖衣](di-registration.md) が定める。

## 真偽値の結果の別名

C# には型引数の既定値がないため、型引数のない `IDialogViewModel` (= `IDialogViewModel<bool>`。結果型は `bool` になる) を用意している。この別名で宣言した ViewModel は ViewModel 型だけを型引数に取る `Register` / `ShowAsync` のオーバーロードで登録・表示でき、show は `DialogResult<bool>` を返す。結果型を明示する `IDialogViewModel<TResult>` の形はそのまま併存する。

## 中身は MAUI の View だけ

MAUI には Android の Compose に相当する第二の UI 技術がなく、**MAUI の `View` が唯一の中身の形**である。したがって技術別の呼び分け (別名・オーバーロード) もない。factory は `Func<TViewModel, DialogNotifier<TResult>, View>` の形で、報告口を取らない1引数形 `Func<TViewModel, View>` のオーバーロードもある。

## インライン show

登録せずに factory を `ShowAsync` へ直接渡す。

```csharp
// ConfirmViewModel : IDialogViewModel (非ジェネリック = 結果は bool)
DialogResult<bool> result = await Dialog.Instance.ShowAsync(
    new ConfirmViewModel("削除しますか?"),
    (viewModel, notifier) => new ConfirmContentView(viewModel, notifier));
```

## 結果報告口の取得

C# の extension member として定義した拡張プロパティ `vm.Notifier` で、show 中の ViewModel から `DialogNotifier` を引ける。型は ViewModel が宣言した結果型に固定され、show の前後は null になる。ViewModel 自身から引く場合は `this.Notifier` と書く。

```csharp
public sealed class ConfirmViewModel : IDialogViewModel
{
    public void OnOkTapped() => this.Notifier?.Complete(true);
}
```

## 参照型限定の強制

登録と型指定 show は `where TViewModel : class` 制約でコンパイル時に弾く。class 制約を書けないインスタンス渡し show (interface 受け) だけは、共通の提示入口の実行時検査で拒否する。したがってどの経路からでも値型の ViewModel は提示に至らない。

## 型指定 show と VM factory

| 書き方 | 用途 |
|---|---|
| `ShowAsync<TViewModel>(configure)` | 真偽値の顔で宣言した ViewModel |
| `ShowAsync<TViewModel, TResult>(configure)` | カスタム結果型は型引数で明示する |
| `RegisterViewModel<TViewModel>(...)` | VM factory の登録 (View factory とは別に登録する) |

1 つの ViewModel 型に対して View factory と VM factory は別々に登録され、型指定 show を使うには両方が要る (View factory だけでは型指定 show が VM を作れない)。非同期の configure は `Func<TViewModel, Task>` を取るオーバーロードで書く。`Register` / `RegisterViewModel` は低水準 API で、1行登録の糖衣 `RegisterForDialog` を使えば両スロットの配線と DI 登録がまとめて済む ([MAUI の DI 連携と登録糖衣](di-registration.md))。

## 失敗とキャンセルの形

show は `Task<DialogResult<TResult>>` を返し、結果は `DialogResult` の record (`Completed` / `Cancelled`) として届く。構成ミスは結果ではなく faulted Task として届き、種別は `DialogException` の入れ子クラスで表す。

| 事象 | 例外 |
|---|---|
| View factory 未登録 | `DialogException.ViewFactoryNotRegistered` |
| 提示先の画面が無い | `DialogException.PresentationHostUnavailable` |
| VM factory 未登録 (型指定 show) | `DialogException.ViewModelFactoryNotRegistered` |
| 同一 ViewModel インスタンスの並行 show | `DialogException.ViewModelAlreadyShowing` |
| 値型を ViewModel にした | `DialogException.ValueTypeViewModel` |
| 1 行登録 (`RegisterForDialog`) が結び付けた View を組み立てられない | `DialogException.ViewCreationFailed` (元の失敗は `InnerException`、`ViewTypeName` / `ViewModelTypeName` を公開) |
| DI 解決が要る経路を provider 確立前に呼んだ | `DialogException.ServiceProviderUnavailable` |

`ViewCreationFailed` と `ServiceProviderUnavailable` は MAUI にだけある種別で、DI による View 生成を持たない Native 側に対応物は無い。`ViewCreationFailed` に包まれるのはライブラリ自身が View を生成する 1 行登録の経路だけで、利用者が書いた factory や fallback resolver が投げた例外は包まれずそのまま届く (経路の詳細は [MAUI の DI 連携と登録糖衣](di-registration.md))。

例外名の末尾が `ValueTypeViewModel` なのは C# の語彙 (値型) に合わせたもので、Kotlin 側とは意図的に非対称である。並行 show の `ViewModelAlreadyShowing` と VM factory 未登録の `ViewModelFactoryNotRegistered` は Kotlin と同名である。

**MAUI には呼び出し元キャンセルの経路がない** — `ShowAsync` は呼び出し元のキャンセル手段 (`CancellationToken` に相当する引数) を受け取らないため、「待機側が打ち切った」という観察自体が存在しない。`Cancelled` が返るのはこの経路とは別で、利用者の操作 (キャンセル報告・外側タップ) や画面破棄によってダイアログが閉じた場合である (条件は [結果通知のルール](../../core/api/result-notification-semantics.md) の「どの操作がキャンセルになるか」)。

## 移植元 (AiForms.Maui.Dialogs) の API 名との対応

移植元は結果を返さない表示と結果を返す表示で動詞を分けていた (`ShowAsync` 系 / `ShowResultAsync` 系)。KsDialogs では結果型を ViewModel が宣言するため動詞を分ける理由がなく、表示 API は `ShowAsync` 1 本に統一されている ([core/ADR-0020](../../../decisions/core/0020-show-verb-unification.md))。移植元の `ShowResultAsync` に相当する呼び方は存在しない。

## 関連

- [MAUI の DI 連携と登録糖衣](di-registration.md) — 1行登録 `RegisterForDialog` と fallback resolver
- [MAUI のレイアウト公開面](layout-surface.md) — 添付プロパティと型
- [MAUI のトランジション公開面](transition-surface.md) — 演出の添付とフックの型
