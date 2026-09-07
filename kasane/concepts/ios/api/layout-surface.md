---
type: concept
title: iOS のレイアウト公開面
description: iOS Native (Swift) でダイアログの大きさと位置を指定するときの公開名と署名 — 属性の型とプロパティ・UIKit の添付プロパティと SwiftUI の modifier・show 引数での置き場所指定・論理単位と色型
tags: [ios, layout, api, surface]
timestamp: 2026-09-05
---

# iOS のレイアウト公開面

この文書を読むと、iOS Native (Swift) でダイアログの大きさ・位置・背後の覆い・外側タップの扱いを指定するときに書く型名とプロパティ名、UIKit / SwiftUI での添付の書き方、コード例が分かる。

**この文書は iOS の公開面 (名前・署名・コード例・framework 固有の注意) だけを扱い、挙動の契約は core が正である。** 対応する契約は [レイアウトのルール](../../core/api/layout-semantics.md) で、属性の意味・既定値・優先順位・rect の決まり方はそちらを読む。

## 属性の型

| 用途 | 書く名前 |
|---|---|
| 静的メタ属性 (基準領域・余白・比率・覆い・外側タップ) | `DialogOptions` |
| 置き場所 (整列と移動量) | `DialogPlacement` |
| 基準領域の選択肢 | `DialogLayoutArea` (`.window` / `.visibleArea`) |
| 配置の選択肢 | `DialogAlignment` (`.start` / `.center` / `.end` / `.fill`) |
| 4 辺の余白 | `DialogEdgeInsets` (`init(top:left:bottom:right:)` と全辺同値の `init(all:)`、`.zero`) |

`DialogOptions` / `DialogPlacement` / `DialogEdgeInsets` は `struct`、`DialogLayoutArea` / `DialogAlignment` は `enum` である。`DialogOptions` と `DialogPlacement` はすべてのイニシャライザ引数に既定値があり、`DialogEdgeInsets` は 4 辺を明示するか `init(all:)` / `.zero` を使う。数値は `Double` (論理単位 pt)。

### `DialogOptions` のプロパティ

| プロパティ | 型 |
|---|---|
| `layoutArea` | `DialogLayoutArea` |
| `dialogMargin` | `DialogEdgeInsets` |
| `proportionalWidth` / `proportionalHeight` | `Double` |
| `overlayColor` | `UIColor` |
| `isCanceledOnTouchOutside` | `Bool` |

### `DialogPlacement` のプロパティ

`horizontalAlignment` / `verticalAlignment` (`DialogAlignment`) と `offsetX` / `offsetY` (`Double`)。

## UIKit での添付

中身になる `UIView` の extension プロパティに設定する。設定しなかったほう (nil) は供給なしとして扱う (優先順位は core が定める)。

| プロパティ | 型 |
|---|---|
| `ksDialogOptions` | `DialogOptions?` |
| `ksDialogPlacement` | `DialogPlacement?` |

```swift
// content は registry へ登録する中身の View (登録の書き方は Dialog 公開面)
let content = ConfirmContentView()
content.ksDialogOptions = DialogOptions(
    layoutArea: .window,
    dialogMargin: DialogEdgeInsets(all: 16),
    proportionalWidth: 0.9,
    isCanceledOnTouchOutside: false
)
content.ksDialogPlacement = DialogPlacement(verticalAlignment: .end, offsetY: -24)
```

## SwiftUI での添付

中身の body ルートに modifier を付ける。

| modifier | 渡す型 |
|---|---|
| `.ksDialogOptions(...)` | `DialogOptions` |
| `.ksDialogPlacement(...)` | `DialogPlacement` |

```swift
Dialog.shared.registry.register(ConfirmViewModel.self) { viewModel, notifier in
    // notifier は結果 (completed / cancelled) を返す口 (Dialog 公開面)
    ConfirmContent(message: viewModel.message, notifier: notifier)
        .ksDialogOptions(DialogOptions(proportionalWidth: 0.8))
        .ksDialogPlacement(DialogPlacement(verticalAlignment: .end))
}
```

添付は View の階層を子から親へ遡って合流するため、入れ子の内側と外側の両方に同じ属性を添付した場合は**外側 (より上位の View) が勝つ**。添付は初回表示までに評価される位置に書く。

## show 引数での置き場所指定

`show(_:placement:)` の `placement:` に `DialogPlacement` を渡すと、添付された置き場所をオブジェクトまるごと置換する。静的メタ属性を渡す引数は無い。

```swift
let result = try await Dialog.shared.show(
    ConfirmViewModel(message: "削除しますか?"),
    placement: DialogPlacement(horizontalAlignment: .fill)
)
```

## framework 固有の注意

- **覆いの色は `UIColor`** で渡す。既定は黒 40% (`UIColor(white: 0, alpha: 0.4)` に相当)
- **可視領域は safe area** である。`DialogLayoutArea.visibleArea` を選んだときに控除される insets は UIKit の safe area insets にあたる
- **数値の単位は pt**。Android の dp と同じ論理単位の役割だが、値の意味は各 OS の論理座標系に従う
- レイアウト計算は iOS 側が実 frame として行う。`UIView` の制約や frame を中身の側で上書きしても、器が決めた外形 rect は変わらない

## 関連

- [レイアウトのルール](../../core/api/layout-semantics.md) — 属性の意味・既定値・優先順位・rect の決まり方 (契約の正)
- [iOS の Dialog 公開面](dialog-surface.md) — 登録・表示・結果の受け取りの公開面
- [iOS のトランジション公開面](transition-surface.md) — 出入りの演出の添付面とフックの型
