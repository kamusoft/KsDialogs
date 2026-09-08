---
type: concept
title: Android のレイアウト公開面
description: Android Native (Kotlin) でダイアログの大きさと位置を指定するときの公開名と署名 — 属性の型とプロパティ・従来 View 系の拡張プロパティと Compose の宣言・show 引数での置き場所指定・論理単位と色の表現
tags: [android, layout, api, surface]
timestamp: 2026-09-08
---

# Android のレイアウト公開面

この文書を読むと、Android Native (Kotlin) でダイアログの大きさ・位置・背後の覆い・外側タップの扱いを指定するときに書く型名とプロパティ名、従来 View 系 / Compose での添付の書き方、コード例が分かる。

**この文書は Android の公開面 (名前・署名・コード例・framework 固有の注意) だけを扱い、挙動の契約は core が正である。** 対応する契約は [レイアウトのルール](../../core/api/layout-semantics.md) で、属性の意味・既定値・優先順位・rect の決まり方はそちらを読む。

## 属性の型

| 用途 | 書く名前 |
|---|---|
| 静的メタ属性 (基準領域・余白・比率・覆い・外側タップ) | `DialogOptions` |
| 置き場所 (整列と移動量) | `DialogPlacement` |
| 基準領域の選択肢 | `DialogLayoutArea` (`WINDOW` / `VISIBLE_AREA`) |
| 配置の選択肢 | `DialogAlignment` (`START` / `CENTER` / `END` / `FILL`) |
| 4 辺の余白 | `DialogEdgeInsets` (4 辺を個別に取るコンストラクタと全辺同値の `DialogEdgeInsets(all)`、`DialogEdgeInsets.ZERO`) |

`DialogOptions` / `DialogPlacement` / `DialogEdgeInsets` は `data class`、`DialogLayoutArea` / `DialogAlignment` は `enum class` である。`DialogOptions` と `DialogPlacement` はコンストラクタ引数にすべて既定値があり、`DialogEdgeInsets` は 4 辺を明示するか `DialogEdgeInsets(all)` / `DialogEdgeInsets.ZERO` を使う。数値は `Double` (論理単位 dp)。

### `DialogOptions` のプロパティ

| プロパティ | 型 |
|---|---|
| `layoutArea` | `DialogLayoutArea` |
| `dialogMargin` | `DialogEdgeInsets` |
| `proportionalWidth` / `proportionalHeight` | `Double` |
| `overlayColor` | `Int` (`@ColorInt` の ARGB 32bit) |
| `isCanceledOnTouchOutside` | `Boolean` |

### `DialogPlacement` のプロパティ

`horizontalAlignment` / `verticalAlignment` (`DialogAlignment`) と `offsetX` / `offsetY` (`Double`)。

## 従来 View 系での添付

中身になる `View` の拡張プロパティに設定する。設定しなかったほう (null) は供給なしとして扱う (優先順位は core が定める)。

| プロパティ | 型 |
|---|---|
| `ksDialogOptions` | `DialogOptions?` |
| `ksDialogPlacement` | `DialogPlacement?` |

```kotlin
// content は registry へ登録する中身の View (登録の書き方は Dialog 公開面)
val content = ConfirmContentView(context)
content.ksDialogOptions = DialogOptions(
    layoutArea = DialogLayoutArea.WINDOW,
    dialogMargin = DialogEdgeInsets(all = 16.0),
    proportionalWidth = 0.9,
    isCanceledOnTouchOutside = false,
)
content.ksDialogPlacement = DialogPlacement(verticalAlignment = DialogAlignment.END, offsetY = -24.0)
```

拡張プロパティの実体は View のタグなので、View を作った直後に設定しておけばよい。

## Compose での添付

中身の composable の冒頭で `KsDialogAttributes(options = ..., placement = ...)` を宣言する (引数はすべて省略可で、省略した引数は供給なしとして扱う)。

```kotlin
Dialog.instance.registry.registerCompose(ConfirmViewModel::class) { viewModel, notifier ->
    // notifier は結果 (completed / cancelled) を返す口 (Dialog 公開面)
    KsDialogAttributes(
        options = DialogOptions(proportionalWidth = 0.8),
        placement = DialogPlacement(verticalAlignment = DialogAlignment.END),
    )
    ConfirmContent(viewModel.message, notifier)
}
```

`KsDialogAttributes` は**初回の組み立てで通る位置に書く**。`LazyColumn` のような遅延評価されるスコープの中に書いた宣言は初回の組み立てで実行されず、その表示には効かない。ダイアログの中身以外で呼び出しても何も起こらない。

`KsDialogAttributes` は Compose 系の配布物 `jp.kamusoft:ksdialogs` (Gradle module `:ksdialogs`) に入っており、これを依存に追加した消費者だけが使える。

## show 引数での置き場所指定

`show(viewModel, placement = ...)` に `DialogPlacement` を渡すと、添付された置き場所をオブジェクトまるごと置換する。静的メタ属性を渡す引数は無い。

```kotlin
val result = Dialog.instance.show(
    ConfirmViewModel("削除しますか?"),
    placement = DialogPlacement(horizontalAlignment = DialogAlignment.FILL),
)
```

## framework 固有の注意

- **覆いの色は ARGB 32bit の `Int`** で渡す (`@ColorInt`)。既定は `0x66000000` (黒 40%)
- **可視領域はシステムバーを除外した領域**である。`DialogLayoutArea.VISIBLE_AREA` を選んだときに控除される insets はシステムバーなどが占める幅にあたる
- **数値の単位は dp**。器は dp で受け取った値を実 View の px へ変換して配置するため、`Double` の値をピクセルとして渡さない
- レイアウト計算は Android 側が実 View の rect として行う。中身の `View` に `LayoutParams` を設定しても、器が決めた外形 rect は変わらない

## 関連

- [レイアウトのルール](../../core/api/layout-semantics.md) — 属性の意味・既定値・優先順位・rect の決まり方 (契約の正)
- [Android の Dialog 公開面](dialog-surface.md) — 登録・表示・結果の受け取りの公開面
- [Android のトランジション公開面](transition-surface.md) — 出入りの演出の添付面とフックの型
