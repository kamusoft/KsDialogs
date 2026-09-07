---
type: concept
title: Android の Dialog 公開面
description: Android Native (Kotlin) からダイアログを使うときの公開名と署名 — 既定エントリと登録の入口・真偽値の別名・従来 View 系と Compose の別名での呼び分けと配布モジュール・Compose の属性宣言・インライン show・結果報告口の取得・構成ミスの例外型・型指定 show
tags: [android, dialog, api, surface]
timestamp: 2026-09-06
---

# Android の Dialog 公開面

この文書を読むと、Android Native (Kotlin) からダイアログを登録・表示するときに書く名前と署名、コード例、Compose 固有の注意が分かる。

**この文書は Android の公開面 (名前・署名・コード例・framework 固有の注意) だけを扱い、挙動の契約は core が正である。** 対応する契約は次の 4 本で、「何が起きるか」はそちらを読む:

- [登録と表示の呼び出し面のルール](../../core/api/registration-show-semantics.md) — register / show の基本形・結果型の省略形・中身の技術・インライン show
- [結果通知のルール](../../core/api/result-notification-semantics.md) — show が返すもの・キャンセル・構成ミス
- [多段表示のルール](../../core/api/multi-display-semantics.md) — 重ね出しの保証
- [ViewModel 主導の呼び出しのルール](../../core/api/model-binding-semantics.md) — 結果報告口の VM 供給・型指定 show・参照型限定

## 登録と表示の入口

| 用途 | 書く名前 |
|---|---|
| 既定の表示エントリ | `Dialog.instance` |
| DI で注入する表示契約 | `KsDialog` (実体は `Dialog`) |
| 登録の入口 | `Dialog.instance.registry` (実体は `DialogViewRegistry.shared` — エントリは `instance`、レジストリは `shared` という綴りである) |
| 表示 | `show(viewModel, placement)` (`suspend` 関数) |

`Dialog()` を自分で作って `KsDialog` として注入しても、既定と同じ `DialogViewRegistry.shared` を共有する。

## 真偽値の結果の別名

Kotlin には型引数の既定値がないため、宣言を省く代わりに真偽値専用の別名 `SimpleDialogViewModel` (= `DialogViewModel<Boolean>`。結果型は `Boolean` になる) を用意している。この別名で宣言した ViewModel の show は `DialogResult<Boolean>` を返し、factory が受け取る `DialogNotifier` も真偽値に型付く。結果型を明示する形はそのまま併存する。

## 従来 View 系と Compose の呼び分け

| 中身の技術 | 登録 | 登録済みの表示 | インライン表示 (中身を直接渡す) |
|---|---|---|---|
| 従来 View 系 (`android.view.View`) | `register(...)` | `show(viewModel, placement)` | `show(viewModel, placement, factory)` |
| Compose | `registerCompose(...)` | `show(viewModel, placement)` | `showCompose(viewModel, placement, content)` |

**登録済みの表示はどちらの技術でも同じ `show`** である — 中身の作り方はレジストリのエントリが持つので、呼び出し側に技術の区別が要らない。別名になるのは中身を引数で渡す 2 つ (登録の `registerCompose` とインライン表示の `showCompose`) で、Kotlin では `@Composable` 付きの関数型と通常の関数型を同名で並べると呼び出し側の型推論が曖昧になるためである。片方だけ別名にすると覚えにくいので、両方を別名でそろえている。

`registerCompose` / `showCompose` は別モジュール `ksdialogs-compose` に入っており、これを依存に追加した消費者だけが使える。本体 `ksdialogs` は Compose に依存しない — 従来 View 系しか使わない消費者 (特に MAUI Android がバインディング経由で Android 本体を取り込む経路) に Compose の依存を持ち込まないためである。

`registerCompose` / `showCompose` に渡す中身は `@Composable` の**ラムダ**で書く。`@Composable` 関数への関数参照 (`::ConfirmContent` の形) は Compose コンパイラが受け付けないため、関数に切り出した中身もラムダから呼ぶ。

従来 View 系の factory は `Context` をレシーバに取るので、中身の `View` はレシーバの `Context` から組み立てられる。

## Compose での属性の添付

中身の composable の冒頭で `KsDialogAttributes(options = ..., placement = ..., transition = ...)` を宣言する (3 引数とも省略可)。

| 引数 | 渡す型 | 供給するもの |
|---|---|---|
| `options` | `DialogOptions` | 器 (ダイアログを画面に載せる表示コンテナ) の静的な属性 — 大きさ・基準領域 (大きさと位置の基準になる矩形)・背後の覆い・外側タップの扱い |
| `placement` | `DialogPlacement` | 置き場所 (整列と移動量) |
| `transition` | `DialogTransition` | 出入りの演出 |

各型が持つプロパティの意味は、レイアウトとトランジションの公開面が定める (下記「関連」)。

```kotlin
// SimpleDialogViewModel は DialogViewModel<Boolean> の別名
class ConfirmViewModel(val message: String) : SimpleDialogViewModel

Dialog.instance.registry.registerCompose(ConfirmViewModel::class) { viewModel, notifier ->
    KsDialogAttributes(placement = DialogPlacement(verticalAlignment = DialogAlignment.END))
    ConfirmContent(viewModel.message, notifier)
}

// show は suspend 関数なので、コルーチンの中で呼んで結果を待つ
val result: DialogResult<Boolean> = Dialog.instance.show(ConfirmViewModel("削除しますか?"))
```

`KsDialogAttributes` は**初回の組み立てで通る位置に書く**。`LazyColumn` のような遅延評価されるスコープの中に書くと初回の組み立てで実行されず、その表示には効かない。中身以外の場所で呼んでも何も起こらない。

## インライン show

登録せずに factory を `show` / `showCompose` へ直接渡す。引数の並びは登録経由の show と同じで、ViewModel を先に、`placement` を挟んで factory を最後に置く (`show(viewModel, placement = ...) { vm, notifier -> ... }`)。

## 結果報告口の取得

`DialogViewModel` の拡張プロパティ `vm.notifier` で、show 中の ViewModel から `DialogNotifier` を引ける。宣言結果型に型付き、show の前後は null になる。

```kotlin
class ConfirmViewModel : SimpleDialogViewModel {
    fun confirm() { notifier?.complete(true) }
}
```

## 参照型限定の強制

Kotlin にはコンパイル時に value class を弾く制約が書けないため、**実行時検査**で拒否する。登録・VM factory 登録・型指定 show に加え、内部で全 show 経路が合流する提示処理でも検査するので (利用者が直接呼ぶ API ではない)、どの経路からでも提示に至らない。

## 型指定 show と VM factory

型指定 show は `show(ConfirmViewModel::class) { vm -> ... }` の形で、configure は `suspend` でも書ける。VM factory の登録は View factory と別名の `registerViewModel(VM::class) { ... }` で行う (引数を書かないラムダ・0引数ラムダがオーバーロード解決を曖昧にするため別名にしている)。1引数 factory (`register(VM::class) { vm -> ... }`) も使え、中身の View のコンストラクタが `(Context, VM)` ならコンストラクタ参照をそのまま factory にできる。

DI コンテナ連携は VM factory の中身として書く (`registerViewModel(VM::class) { get() }` など)。

## 失敗とキャンセルの形

show は `suspend` 関数で、結果は `DialogResult` の sealed interface (`Completed` / `Cancelled`) として返る。構成ミスは結果ではなく例外で届き、種別は `DialogException` の入れ子クラスで表す。

| 事象 | 例外 |
|---|---|
| View factory 未登録 | `DialogException.ViewFactoryNotRegistered` |
| 提示先の画面が無い | `DialogException.PresentationHostUnavailable` |
| VM factory 未登録 (型指定 show) | `DialogException.ViewModelFactoryNotRegistered` |
| 同一 ViewModel インスタンスの並行 show | `DialogException.ViewModelAlreadyShowing` |
| value class を ViewModel にした | `DialogException.ValueClassViewModel` |

`ValueClassViewModel` という名前は Kotlin の語彙 (value class) に合わせたもので、C# 側とは意図的に非対称である。並行 show の `ViewModelAlreadyShowing` と VM factory 未登録の `ViewModelFactoryNotRegistered` は C# と同名である。

呼び出し元のコルーチンをキャンセルしたときは、コルーチン規約どおり `CancellationException` が伝播する (内部の結果は cancelled で確定済み)。キャンセル済みのコルーチンからでも退出の演出と撤去は最後まで完遂される。

## 関連

- [Android のレイアウト公開面](layout-surface.md) — 属性の添付面と型
- [Android のトランジション公開面](transition-surface.md) — 演出の添付面とフックの型
- [KMP の Dialog 公開面](../../kmp/api/dialog-surface.md) — KMP の共有 ViewModel は Android Native 型の別名なので、この面がそのまま使える
