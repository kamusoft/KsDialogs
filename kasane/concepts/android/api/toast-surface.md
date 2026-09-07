---
type: concept
title: Android の Toast 公開面
description: Android Native (Kotlin) で Toast を使うときの公開名と署名 — 契約と既定エントリ・4 経路の show と durationMs 引数・従来 View 系と Compose の登録と表示・型指定 show と VM factory 登録・ToastStyle のプロパティ・持たない操作と OS の Toast API との違い
tags: [android, toast, api, surface]
timestamp: 2026-09-07
---

# Android の Toast 公開面

この文書を読むと、Android Native (Kotlin) で Toast を表示・カスタマイズするときに書く型名・プロパティ名・関数の署名とコード例が分かる。

**この文書は Android の公開面 (名前・署名・コード例・framework 固有の注意) だけを扱い、挙動の契約は core が正である。** 対応する契約は [Toast のルール](../../core/api/toast-semantics.md) で、duration の時間モデル・失敗モデル・多重表示・非対話の保証はそちらを読む。

## 契約と入口

| 用途 | 書く名前 |
|---|---|
| DI で注入する契約 | `KsToast` (`interface`。実体は `Toast`) |
| 既定の表示エントリ | `Toast.instance` |
| レジストリのハンドル | `Toast.instance.registry` (型は `ToastViewRegistry`。実体は `ToastViewRegistry.shared`) |
| 一括設定 | `Toast.instance.style` (型は `ToastStyle`) |

`Toast()` を自分で作って `KsToast` として注入しても、既定エントリと同じレジストリ・同じ一括設定・同じ表示リストを共有する。

## 4 経路の show

いずれも `suspend` ではない同期の関数で、戻り値を持たない。

| 経路 | 署名 |
|---|---|
| メッセージ入口 | `show(message, durationMs, placement)` |
| 登録経路 | `show(viewModel, durationMs, placement)` |
| インライン経路 | `show(viewModel, durationMs, placement, factory)` |
| 型指定経路 (VM の型を渡す) | `show(viewModelClass, durationMs, placement, configure)` (configure は同期のみ・省略可) |

**表示時間の引数名は `durationMs`** (`Int?`、ミリ秒) である。`placement` は `DialogPlacement?` で、どちらも省略できる。

```kotlin
Toast.instance.show("保存しました")
Toast.instance.show("保存しました", durationMs = 2500)
```

## 従来 View 系と Compose の呼び分け

| 中身の技術 | 登録 | 登録済みの表示 | インライン表示 (中身を直接渡す) |
|---|---|---|---|
| 従来 View 系 (`android.view.View`) | `register(...)` | `show(viewModel, durationMs, placement)` | `show(viewModel, durationMs, placement, factory)` |
| Compose | `registerCompose(...)` | `show(viewModel, durationMs, placement)` | `showCompose(viewModel, durationMs, placement, content)` |

**登録済みの表示はどちらの技術でも同じ `show`** で、型指定 show (`show(VM::class)`) も `register` / `registerCompose` のどちらで登録した中身にも同じに働く。別名になるのは中身を引数で渡す `registerCompose` / `showCompose` で、事情は Dialog / Loading と同じ (中身を関数参照ではなくラムダで書く点も同じ)。これらは別モジュール `ksdialogs-compose` に入っている ([Android の Dialog 公開面](dialog-surface.md))。

```kotlin
Toast.instance.registry.registerCompose(NoticeViewModel::class) { viewModel ->
    NoticeToastContent(viewModel.message)
}

Toast.instance.show(NoticeViewModel("同期が完了しました"), durationMs = 2000)
```

カスタム Toast の ViewModel は `ToastViewModel` (`interface`) に準拠させる。未登録の型で表示すると `DialogException.ViewFactoryNotRegistered` で失敗する。

## 型指定 show と VM factory

ViewModel のインスタンスではなくクラス参照を渡す形で、`show(NoticeViewModel::class) { vm -> ... }` と書く。VM factory の登録は View factory と別名の `registerViewModel(VM::class) { ... }` で行う (Dialog / Loading のレジストリと同じ事情・同じ綴り)。引数なしコンストラクタの参照をそのまま factory にできる。挙動 (生成 → configure → 中身の生成の順序、失敗の分類) は [Toast のルール](../../core/api/toast-semantics.md) と [ViewModel 主導の呼び出しのルール](../../core/api/model-binding-semantics.md) が正である。

```kotlin
class NoticeViewModel : ToastViewModel {
    var message = ""                               // configure から書き換える
}

Toast.instance.registry.registerCompose(NoticeViewModel::class) { viewModel ->
    NoticeToastContent(viewModel.message)
}
Toast.instance.registry.registerViewModel(NoticeViewModel::class, ::NoticeViewModel)

Toast.instance.show(NoticeViewModel::class, durationMs = 2000) { vm ->
    vm.message = "同期が完了しました"              // configure: 中身の生成前に必ず完了する (同期のみ)
}
```

VM factory と configure は Main dispatcher で実行される。型指定 show は VM factory と View factory の両方を呼び出し時点で解決するため、VM factory 未登録は `DialogException.ViewModelFactoryNotRegistered`、View factory 未登録は `DialogException.ViewFactoryNotRegistered` として同期に失敗し (判定は VM factory が先)、VM factory / configure が投げた例外は呼び出し元へは返らず「受理後の失敗」(警告ログを残してその 1 枚だけ破棄) になる。value class の ViewModel は `registerViewModel` と型指定 show の時点で `DialogException.ValueClassViewModel` として拒否される。

## `ToastStyle` のプロパティ

`data class` なので `copy(...)` で一部だけ差し替えられる。

| プロパティ | 型 | 既定値 |
|---|---|---|
| `backgroundColor` | `@ColorInt Int` | `ToastStyle.BUILTIN_BACKGROUND_COLOR` (半透明のダークグレー) |
| `textColor` | `@ColorInt Int` | `Color.WHITE` |
| `fontSize` | `Double` | `14.0` |
| `cornerRadius` | `Double` | `22.0` |
| `defaultDuration` | `Int` | `ToastStyle.BUILTIN_DEFAULT_DURATION` (`1500`) |
| `defaultPlacement` | `DialogPlacement?` | `null` |

```kotlin
Toast.instance.style = Toast.instance.style.copy(defaultDuration = 2000)
```

## framework 固有の注意

- **OS の `android.widget.Toast` は使っていない** — 自前の全画面透過 Window の器に載せている。API 30 以降のカスタム View 非推奨・実質 3.5 秒のクランプ・重力指定の配置・多重の順番待ちといった OS Toast の制約は、この面には無い
- **`hide` に相当する操作は無い** — Loading と違い、Toast の契約には閉じる操作もメッセージ更新もスコープ形も進捗の報告口も無い ([Android の Loading 公開面](loading-surface.md))
- **器メタ属性の受け口が無い** — 覆いの色や外側タップの扱いを渡すプロパティを Toast は持たない。渡せるのは配置 (`DialogPlacement`) だけである
- **factory のレシーバは提示先画面の `Context`** (`Context.(VM) -> View`)。中身の生成は提示先の Context を確保してから行うため、型指定 show の VM factory と configure もその時点で走る — 提示先 (resumed Activity) が現れないまま duration が満了した表示では、どちらも呼ばれずに破棄される
- **カスタム View の演出**は中身への `DialogTransition` 添付で差し替える ([Android のトランジション公開面](transition-surface.md))。デフォルト View にはこの口が無い

## 関連

- [Toast のルール](../../core/api/toast-semantics.md) — duration・失敗モデル・多重表示・非対話 (契約の正)
- [ViewModel 主導の呼び出しのルール](../../core/api/model-binding-semantics.md) — 型指定 show の順序保証と VM factory 解決 (3 機能共通の契約)
- [Android の Loading 公開面](loading-surface.md) — 操作の多い側との対比
- [Android のレイアウト公開面](layout-surface.md) — `DialogPlacement` の型と添付面
- [Android の Dialog 公開面](dialog-surface.md) — 登録・表示の書き方 (Toast と同型の呼び分け)
