---
type: concept
title: Android の Loading 公開面
description: Android Native (Kotlin) で Loading を使うときの公開名と署名 — 契約と既定エントリ・show / hide / setMessage / スコープ形の署名・進捗報告口と進捗受け口・従来 View 系と Compose の登録と表示・型指定 show / start と VM factory 登録・LoadingStyle と器メタ属性・配布モジュールと Context レシーバの注意
tags: [android, loading, api, surface]
timestamp: 2026-09-08
---

# Android の Loading 公開面

この文書を読むと、Android Native (Kotlin) で Loading を表示・カスタマイズするときに書く型名・プロパティ名・関数の署名とコード例が分かる。

**この文書は Android の公開面 (名前・署名・コード例・framework 固有の注意) だけを扱い、挙動の契約は core が正である。** 対応する契約は [Loading のルール](../../core/api/loading-semantics.md) で、合流の数え方・世代・器の性質・保証と禁止はそちらを読む。

## 契約と入口

| 用途 | 書く名前 |
|---|---|
| DI で注入する契約 | `KsLoading` (`interface`。実体は `Loading`) |
| 既定の表示エントリ | `Loading.instance` |
| レジストリのハンドル | `Loading.instance.registry` (型は `LoadingViewRegistry`。実体は `LoadingViewRegistry.shared`) |
| 既定ローディングの styling | `Loading.instance.style` (型は `LoadingStyle`) |
| 既定ローディングの器メタ属性 | `Loading.instance.options` (型は `DialogOptions`) |

`Loading()` を自分で作って `KsLoading` として注入しても、既定エントリと同じ表示状態・同じレジストリを共有する。状態の正はプロセス内に 1 つある内部層の `LoadingCoordinator` で、これは公開面ではない (利用者が名前で触ることはない)。

## 操作の署名

すべて `suspend` 関数である。

| 操作 | 署名 |
|---|---|
| 既定ローディングの表示 | `show(message, placement)` (2 引数とも既定値つき) |
| 登録済みカスタム View の表示 | `show(viewModel, placement)` |
| インライン factory での表示 | `show(viewModel, placement, factory)` |
| 型指定 show (VM の型を渡す) | `show(viewModelClass, placement, configure)` (configure は `suspend` 可・省略可) |
| 閉鎖 | `hide()` |
| メッセージの差し替え | `setMessage(message)` |
| スコープ形 | `start(message, placement, action)` / `start(viewModel, placement, action)` / `start(viewModel, placement, factory, action)` |
| 型指定 start | `start(viewModelClass, placement, configure, action)` |

スコープ形の処理に渡る**進捗報告口**は `(Double) -> Unit` の関数で、任意スレッドから呼べる。

## 従来 View 系と Compose の呼び分け

| 中身の技術 | 登録 | 登録済みの表示 | インライン表示 (中身を直接渡す) |
|---|---|---|---|
| 従来 View 系 (`android.view.View`) | `register(...)` | `show(viewModel, placement)` / `start(viewModel, placement, action)` | `show(viewModel, placement, factory)` / `start(viewModel, placement, factory, action)` |
| Compose | `registerCompose(...)` | `show(viewModel, placement)` / `start(viewModel, placement, action)` | `showCompose(viewModel, placement, content)` / `startCompose(viewModel, placement, content, action)` |

**登録済みの表示はどちらの技術でも同じ `show` / `start`** で、型指定 show / start (`show(VM::class)` / `start(VM::class)`) も `register` / `registerCompose` のどちらで登録した中身にも同じに働く。別名になるのは中身を引数で渡す `registerCompose` / `showCompose` / `startCompose` で、事情は Dialog と同じ (`@Composable` 付きの関数型と通常の関数型を同名で並べると型推論が曖昧になる。中身を関数参照ではなくラムダで書く点も同じ — [Android の Dialog 公開面](dialog-surface.md))。これらは Compose 系の配布物 `jp.kamusoft:ksdialogs` に入っており、これを依存に追加した消費者だけが使える。

```kotlin
// 従来 View 系。factory のレシーバは提示先画面の Context
Loading.instance.registry.register(UploadViewModel::class) { viewModel ->
    UploadLoadingView(this, viewModel)
}

val uploaded = Loading.instance.start(UploadViewModel()) { report ->
    upload(onProgress = report)   // report(0.0〜1.0) で進捗を報告する
}
```

カスタム Loading の ViewModel は `LoadingViewModel` (`interface`) に準拠させる。未登録の型で表示すると `DialogException.ViewFactoryNotRegistered` で失敗する。進捗を中身へ届けたい ViewModel は `LoadingProgressReceiver` を実装し、`onProgress(progress: Double)` で受け取る。準拠しない ViewModel では転送されない。

## 型指定 show / start と VM factory

ViewModel のインスタンスではなくクラス参照を渡す形で、`show(UploadViewModel::class) { vm -> ... }` / `start(UploadViewModel::class, configure = { vm -> ... }) { report -> ... }` と書く。VM factory の登録は View factory と別名の `registerViewModel(VM::class) { ... }` で行う (0 引数ラムダと 1 引数ラムダがオーバーロード解決を曖昧にするため — Dialog のレジストリと同じ事情。[Android の Dialog 公開面](dialog-surface.md))。引数なしコンストラクタの参照 `::UploadViewModel` をそのまま factory にできる。DI コンテナ連携は VM factory の中身として書く。挙動 (生成 → configure → 進捗受け口の紐付け → 中身の生成の順序、合流との関係、失敗の扱い) は [Loading のルール](../../core/api/loading-semantics.md) と [ViewModel 主導の呼び出しのルール](../../core/api/model-binding-semantics.md) が正である。

```kotlin
class UploadViewModel : LoadingViewModel, LoadingProgressReceiver {
    var title = ""                                 // configure から書き換える
    override fun onProgress(progress: Double) { /* 中身が読む状態を更新する */ }
}

Loading.instance.registry.register(UploadViewModel::class, ::UploadLoadingView)   // (Context, VM) のコンストラクタ参照
Loading.instance.registry.registerViewModel(UploadViewModel::class, ::UploadViewModel)

val uploaded = Loading.instance.start(UploadViewModel::class, configure = { vm ->
    vm.title = "アップロード中"                    // configure: 中身の生成前に必ず完了する (suspend 可)
}) { report ->
    upload(onProgress = report)
}
```

VM factory と configure は Main dispatcher で実行される。value class の ViewModel は `registerViewModel` と型指定 show / start の時点で `DialogException.ValueClassViewModel` として拒否される (Dialog と同じ実行時検査)。

## `LoadingStyle` のプロパティ

`data class` なので `copy(...)` で一部だけ差し替えられる。

| プロパティ | 型 | 既定値 |
|---|---|---|
| `indicatorColor` | `@ColorInt Int` | `Color.WHITE` |
| `messageFontSize` | `Double` | `14.0` |
| `messageColor` | `@ColorInt Int` | `Color.WHITE` |
| `defaultMessage` | `String?` | `null` |
| `progressFormat` | `(String?, Double?) -> String` | `LoadingStyle.DEFAULT_PROGRESS_FORMAT` |

器メタ属性は `DialogOptions` をそのまま使う ([Android のレイアウト公開面](layout-surface.md) の属性の型)。

```kotlin
Loading.instance.style = Loading.instance.style.copy(defaultMessage = "処理中…")
```

## framework 固有の注意

- **factory のレシーバは提示先画面の `Context`** (`Context.(VM) -> View`) — View の生成にそのまま使える
- **`show` / `start` は `suspend`** なので、呼び出し元のコルーチンから任意のディスパッチャで呼べる。内部で UI スレッドへ移して受理順に直列化される
- **型指定 show / start は VM factory と View factory の両方を呼び出し時点で解決する** — 欠けているスロットに応じて別の例外で失敗する (下表)
- **Compose の中身で属性や演出を宣言する位置**は Dialog と同じ注意が要る (初回の組み立てで実行されない場所に書くと効かない — [Android の Dialog 公開面](dialog-surface.md))
- **カスタム View 版の演出**は中身への `DialogTransition` 添付で差し替える ([Android のトランジション公開面](transition-surface.md))。既定ローディングにはこの口が無い

型指定 show / start が呼び出し時点で失敗する構成ミスは次の 2 つで、判定は VM factory が先である。

| 欠けているスロット | 失敗 |
|---|---|
| VM factory | `DialogException.ViewModelFactoryNotRegistered` |
| View factory | `DialogException.ViewFactoryNotRegistered` |

## 関連

- [Loading のルール](../../core/api/loading-semantics.md) — 合流・世代・器の性質・保証と禁止 (契約の正)
- [ViewModel 主導の呼び出しのルール](../../core/api/model-binding-semantics.md) — 型指定 show の順序保証と VM factory 解決 (3 機能共通の契約)
- [Android の Dialog 公開面](dialog-surface.md) — 登録・表示の書き方 (Loading と同型の呼び分け)
- [Android のレイアウト公開面](layout-surface.md) — `DialogOptions` / `DialogPlacement` の型と添付面
- [Android の Toast 公開面](toast-surface.md) — もう 1 つの非ダイアログ表示の公開面
