---
type: concept
title: iOS の Loading 公開面
description: iOS Native (Swift) で Loading を使うときの公開名と署名 — 契約と既定エントリ・show / hide / setMessage / スコープ形の署名・進捗報告口と進捗受け口・カスタム View の登録 (UIKit / SwiftUI)・型指定 show / start と VM factory 登録・LoadingStyle と器メタ属性・MainActor と throws の注意
tags: [ios, loading, api, surface]
timestamp: 2026-09-07
---

# iOS の Loading 公開面

この文書を読むと、iOS Native (Swift) で Loading を表示・カスタマイズするときに書く型名・プロパティ名・メソッドの署名とコード例が分かる。

**この文書は iOS の公開面 (名前・署名・コード例・framework 固有の注意) だけを扱い、挙動の契約は core が正である。** 対応する契約は [Loading のルール](../../core/api/loading-semantics.md) で、合流の数え方・世代・器の性質・保証と禁止はそちらを読む。

## 契約と入口

| 用途 | 書く名前 |
|---|---|
| DI で注入する契約 | `KsLoading` (`protocol`。実体は `Loading`) |
| 既定の表示エントリ | `Loading.shared` |
| レジストリのハンドル | `Loading.shared.registry` (型は `LoadingViewRegistry`。実体は `LoadingViewRegistry.shared`) |
| 既定ローディングの styling | `Loading.shared.style` (型は `LoadingStyle`) |
| 既定ローディングの器メタ属性 | `Loading.shared.options` (型は `DialogOptions`) |

`Loading()` を自分で作って `KsLoading` として注入しても、既定エントリと同じ表示状態・同じレジストリを共有する。状態の正はプロセス内に 1 つある内部層の `LoadingCoordinator` で、これは公開面ではない (利用者が名前で触ることはない)。

## 操作の署名

| 操作 | 署名 |
|---|---|
| 既定ローディングの表示 | `show(message:placement:)` (`async`。2 引数とも省略可) |
| 登録済みカスタム View の表示 | `show(_:placement:)` (`async throws`) |
| インライン factory での表示 | `show(_:placement:factory:)` (`async throws`) |
| 型指定 show (VM の型を渡す) | `show(_:placement:configure:)` (第 1 引数は `VM.self`。`async throws`。configure は `async throws` 可・省略可) |
| 閉鎖 | `hide()` (`async`) |
| メッセージの差し替え | `setMessage(_:)` (`async`) |
| スコープ形 | `start(message:placement:_:)` / `start(_:placement:_:)` / `start(_:placement:factory:_:)` (`async throws`。最後の引数が処理) |
| 型指定 start | `start(_:placement:configure:_:)` (第 1 引数は `VM.self`。`async throws`。最後の引数が処理) |

スコープ形の処理に渡る**進捗報告口**は `@Sendable (Double) -> Void` のクロージャで、任意スレッドから呼べる。

## カスタム View の登録

カスタム Loading の ViewModel は `LoadingViewModel` (`AnyObject` と `Sendable` に準拠する protocol) に準拠させる。登録はレジストリの `register(_:factory:)` で、UIKit の `UIView` を返す factory と SwiftUI の View を返す factory の同名オーバーロードがある。

```swift
// UIKit の中身
Loading.shared.registry.register(UploadViewModel.self) { viewModel in
    UploadLoadingView(viewModel: viewModel)
}

// SwiftUI の中身 (同名で戻り値の型だけが違う)
Loading.shared.registry.register(UploadViewModel.self) { viewModel in
    UploadLoadingContent(viewModel: viewModel)
}

let uploaded = try await Loading.shared.start(UploadViewModel()) { report in
    try await upload(progress: report)   // report(0.0〜1.0) で進捗を報告する
}
```

進捗を中身へ届けたい ViewModel は `LoadingProgressReceiver` (`@MainActor` の protocol) に準拠し、`onProgress(_:)` で `Double` を受け取る。準拠しない ViewModel では転送されない。

## 型指定 show / start と VM factory

ViewModel のインスタンスではなく型を渡す形で、`Loading.shared.show(UploadViewModel.self) { vm in ... }` / `Loading.shared.start(UploadViewModel.self, configure: { vm in ... }) { report in ... }` と書く。VM factory の登録は View factory と同じ `register` のオーバーロード `register(_:viewModel:)` で行う (Dialog のレジストリと同じ綴り — [iOS の Dialog 公開面](dialog-surface.md))。configure なし・placement なしの省略形は Dialog の型指定 show と同じ形でそろえてある。挙動 (生成 → configure → 進捗受け口の紐付け → 中身の生成の順序、合流との関係、失敗の扱い) は [Loading のルール](../../core/api/loading-semantics.md) と [ViewModel 主導の呼び出しのルール](../../core/api/model-binding-semantics.md) が正である。

```swift
@MainActor                                         // 可変の状態を持つので MainActor に閉じる
final class UploadViewModel: LoadingViewModel, LoadingProgressReceiver {
    var title = ""                                 // configure から書き換えるので var
    func onProgress(_ progress: Double) { /* 中身が読む状態を更新する */ }

    init() {}                                      // VM factory は MainActor で呼ばれる
}

Loading.shared.registry.register(UploadViewModel.self) { viewModel in
    UploadLoadingContent(viewModel: viewModel)     // SwiftUI の中身
}
Loading.shared.registry.register(UploadViewModel.self, viewModel: { UploadViewModel() })

let uploaded = try await Loading.shared.start(UploadViewModel.self, configure: { vm in
    vm.title = "アップロード中"                    // configure: 中身の生成前に必ず完了する
}) { report in
    try await upload(progress: report)
}
```

VM factory のクロージャは `@MainActor @Sendable () -> VM` で **非 throwing** である (Dialog の VM factory と同じ)。失敗を表明したいときは configure (`throws`) で投げる。

## `LoadingStyle` のプロパティ

| プロパティ | 型 | 既定値 |
|---|---|---|
| `indicatorColor` | `UIColor` | `.white` |
| `messageFontSize` | `Double` | `14` |
| `messageColor` | `UIColor` | `.white` |
| `defaultMessage` | `String?` | `nil` |
| `progressFormat` | `LoadingStyle.ProgressFormat` = `@Sendable (String?, Double?) -> String` | `LoadingStyle.defaultProgressFormat` |

器メタ属性は `DialogOptions` をそのまま使う ([iOS のレイアウト公開面](layout-surface.md) の属性の型)。styling も器メタ属性も、値を作り直して `Loading.shared.style` / `Loading.shared.options` に代入する形で設定する。

```swift
Loading.shared.style = LoadingStyle(indicatorColor: .systemBlue, defaultMessage: "処理中…")
```

## framework 固有の注意

- **factory は `throws`** — 中身の組み立てで失敗を投げられる。投げた失敗は表示の開始そのものの失敗として呼び出し元へ返る
- **未登録の ViewModel 型で表示すると `DialogError.viewFactoryNotRegistered` が throw される** (Dialog と同じ enum の case — [iOS の Dialog 公開面](dialog-surface.md))
- **型指定 show / start は VM factory と View factory の両方を呼び出し時点で解決する** — 欠けているスロットに応じて別の case で失敗する (下表)。VM factory と configure は `@MainActor` で実行される
- **factory と進捗受け口は `@MainActor`** なので、中で UIKit の API をそのまま呼べる
- **`show` / `start` は任意スレッドから `await` できる**。内部で main へ移して受理順に直列化される
- **カスタム View 版の演出**は中身への `DialogTransition` 添付で差し替える ([iOS のトランジション公開面](transition-surface.md))。既定ローディングにはこの口が無い

型指定 show / start が呼び出し時点で失敗する構成ミスは次の 2 つで、判定は VM factory が先である。

| 欠けているスロット | 失敗 |
|---|---|
| VM factory | `DialogError.viewModelFactoryNotRegistered` |
| View factory | `DialogError.viewFactoryNotRegistered` |

## 関連

- [Loading のルール](../../core/api/loading-semantics.md) — 合流・世代・器の性質・保証と禁止 (契約の正)
- [ViewModel 主導の呼び出しのルール](../../core/api/model-binding-semantics.md) — 型指定 show の順序保証と VM factory 解決 (3 機能共通の契約)
- [iOS の Dialog 公開面](dialog-surface.md) — 登録・表示の書き方 (Loading と同型の呼び分け)
- [iOS のレイアウト公開面](layout-surface.md) — `DialogOptions` / `DialogPlacement` の型と添付面
- [iOS の Toast 公開面](toast-surface.md) — もう 1 つの非ダイアログ表示の公開面
