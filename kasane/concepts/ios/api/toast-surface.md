---
type: concept
title: iOS の Toast 公開面
description: iOS Native (Swift) で Toast を使うときの公開名と署名 — 契約と既定エントリ・4 経路の show の署名・カスタム View の登録 (UIKit / SwiftUI)・型指定 show と VM factory 登録・ToastStyle のプロパティ・factory 閉包が throws である理由と持たない操作
tags: [ios, toast, api, surface]
timestamp: 2026-09-07
---

# iOS の Toast 公開面

この文書を読むと、iOS Native (Swift) で Toast を表示・カスタマイズするときに書く型名・プロパティ名・メソッドの署名とコード例が分かる。

**この文書は iOS の公開面 (名前・署名・コード例・framework 固有の注意) だけを扱い、挙動の契約は core が正である。** 対応する契約は [Toast のルール](../../core/api/toast-semantics.md) で、duration の時間モデル・失敗モデル・多重表示・非対話の保証はそちらを読む。

## 契約と入口

| 用途 | 書く名前 |
|---|---|
| DI で注入する契約 | `KsToast` (`protocol`。実体は `Toast`) |
| 既定の表示エントリ | `Toast.shared` |
| レジストリのハンドル | `Toast.shared.registry` (型は `ToastViewRegistry`。実体は `ToastViewRegistry.shared`) |
| 一括設定 | `Toast.shared.style` (型は `ToastStyle`) |

`Toast()` を自分で作って `KsToast` として注入しても、既定エントリと同じレジストリ・同じ一括設定・同じ表示リストを共有する。

## 4 経路の show

| 経路 | 署名 |
|---|---|
| メッセージ入口 | `show(message:duration:placement:)` |
| 登録経路 | `show(_:duration:placement:)` (`throws`) |
| インライン経路 | `show(_:duration:placement:factory:)` (`throws`) |
| 型指定経路 (VM の型を渡す) | `show(_:duration:placement:configure:)` (第 1 引数は `VM.self`。`throws`。configure は同期・`throws` 可・省略可) |

`duration` は `Int?` (ミリ秒)、`placement` は `DialogPlacement?` で、どちらも省略できる。戻り値は無く `await` もしない — fire-and-forget だからである。

```swift
Toast.shared.show(message: "保存しました")
Toast.shared.show(message: "保存しました", duration: 2500)
```

## カスタム View の登録

カスタム Toast の ViewModel は `ToastViewModel` (`AnyObject` と `Sendable` に準拠する protocol) に準拠させる。登録はレジストリの `register(_:factory:)` で、UIKit の `UIView` を返す factory と SwiftUI の View を返す factory の同名オーバーロードがある。

```swift
Toast.shared.registry.register(NoticeViewModel.self) { viewModel in
    NoticeToastContent(message: viewModel.message)   // SwiftUI の中身
}

try Toast.shared.show(NoticeViewModel(message: "同期が完了しました"), duration: 2000)
```

## 型指定 show と VM factory

ViewModel のインスタンスではなく型を渡す形で、`Toast.shared.show(NoticeViewModel.self) { vm in ... }` と書く。VM factory の登録は View factory と同じ `register` のオーバーロード `register(_:viewModel:)` で行う (Dialog / Loading のレジストリと同じ綴り)。configure なし・duration なし・placement なしの省略形は Dialog の型指定 show と同じ形でそろえてある。`ToastViewModel` は `Sendable` を要求するため、書き換わる状態を持つ ViewModel は `@MainActor` に閉じる。VM factory は `@MainActor` で呼ばれるので、`init` を `nonisolated` にする必要はない。挙動 (生成 → configure → 中身の生成の順序、失敗の分類) は [Toast のルール](../../core/api/toast-semantics.md) と [ViewModel 主導の呼び出しのルール](../../core/api/model-binding-semantics.md) が正である。

```swift
@MainActor                                         // 可変の状態を持つので MainActor に閉じる
final class NoticeViewModel: ToastViewModel {
    var message = ""                               // configure から書き換えるので var

    init() {}                                      // VM factory は MainActor で呼ばれる
}

Toast.shared.registry.register(NoticeViewModel.self) { viewModel in
    NoticeToastContent(message: viewModel.message)
}
Toast.shared.registry.register(NoticeViewModel.self, viewModel: { NoticeViewModel() })

try Toast.shared.show(NoticeViewModel.self, duration: 2000) { vm in
    vm.message = "同期が完了しました"              // configure: 中身の生成前に必ず完了する
}
```

VM factory のクロージャは `@MainActor @Sendable () -> VM` で **非 throwing** である (Dialog の VM factory と同じ)。configure は `throws` で書けるが、投げた失敗は show の呼び出し元へは返らず「受理後の失敗」(警告を残してその 1 枚だけ破棄) になる — show が同期に戻ったあと MainActor で実行されるためである。

## `ToastStyle` のプロパティ

| プロパティ | 型 | 既定値 |
|---|---|---|
| `backgroundColor` | `UIColor` | `ToastStyle.builtinBackgroundColor` (半透明のダークグレー) |
| `textColor` | `UIColor` | `.white` |
| `fontSize` | `Double` | `14` |
| `cornerRadius` | `Double` | `22` |
| `defaultDuration` | `Int` | `ToastStyle.builtinDefaultDuration` (`1500`) |
| `defaultPlacement` | `DialogPlacement?` | `nil` |

```swift
Toast.shared.style = ToastStyle(defaultDuration: 2000, defaultPlacement: myBottomPlacement)
```

## framework 固有の注意

- **factory 閉包は `throws`** — `(VM) throws -> UIView` の形である (理由は下段)
- **未登録の ViewModel 型で表示すると `DialogError.viewFactoryNotRegistered` が throw される** (Dialog と同じ enum の case — [iOS の Dialog 公開面](dialog-surface.md))
- **型指定 show は VM factory と View factory の両方を呼び出し時点で解決する** — 欠けているスロットに応じて別の case を throw する (下表)
- **`hide` に相当する操作は無い** — Loading と違い、Toast の契約には閉じる操作もメッセージ更新もスコープ形も進捗の報告口も無い ([iOS の Loading 公開面](loading-surface.md))
- **器メタ属性の受け口が無い** — 覆いの色や外側タップの扱いを渡すプロパティを Toast は持たない。渡せるのは配置 (`DialogPlacement`) だけである
- **カスタム View の演出**は中身への `DialogTransition` 添付で差し替える ([iOS のトランジション公開面](transition-surface.md))。デフォルト View にはこの口が無い

型指定 show が呼び出し時点で throw する構成ミスは次の 2 つで、判定は VM factory が先である。VM factory / configure の実行時の失敗はここに含まれず、throw されない。

| 欠けているスロット | throw される case |
|---|---|
| VM factory | `DialogError.viewModelFactoryNotRegistered` |
| View factory | `DialogError.viewFactoryNotRegistered` |

factory 閉包だけ `throws` を持たせているのは、Kotlin / C# の factory が自然に例外を投げられるのに対し、Swift は非 throwing のままだと factory が失敗を表明する手段を持たないためである。投げられた失敗は「受理後の失敗」として扱われる ([Toast のルール](../../core/api/toast-semantics.md) の失敗モデル)。

## 関連

- [Toast のルール](../../core/api/toast-semantics.md) — duration・失敗モデル・多重表示・非対話 (契約の正)
- [ViewModel 主導の呼び出しのルール](../../core/api/model-binding-semantics.md) — 型指定 show の順序保証と VM factory 解決 (3 機能共通の契約)
- [iOS の Loading 公開面](loading-surface.md) — 操作の多い側との対比
- [iOS のレイアウト公開面](layout-surface.md) — `DialogPlacement` の型と添付面
- [iOS の Dialog 公開面](dialog-surface.md) — 登録・表示の書き方 (Toast と同型の呼び分け)
