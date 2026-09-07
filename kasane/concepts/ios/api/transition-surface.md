---
type: concept
title: iOS のトランジション公開面
description: iOS Native (Swift) でダイアログの出入りの演出を差し替えるときの公開名と署名 — 演出の型とフックの型・UIKit の添付プロパティと SwiftUI の modifier・プリセット factory と辺の綴り・duration と easing の型・MainActor の注意
tags: [ios, transition, api, surface]
timestamp: 2026-09-06
---

# iOS のトランジション公開面

この文書を読むと、iOS Native (Swift) でダイアログの出入りの演出を差し替えるときに書く型名とプロパティ名、UIKit / SwiftUI での添付の書き方、プリセットの綴りとコード例が分かる。

**この文書は iOS の公開面 (名前・署名・コード例・framework 固有の注意) だけを扱い、挙動の契約は core が正である。** 対応する契約は [トランジションのルール](../../core/api/transition-semantics.md) で、演出の採用時点・既定の演出・結果が返る時点・フックが実行される閉鎖経路・失敗や未完了の扱いはそちらを読む。

## 演出の型とフックの型

| 用途 | 書く名前 |
|---|---|
| 演出の組 | `DialogTransition` (`struct`) |
| フックの型 | `DialogTransition.Hook` = `@MainActor @Sendable (UIView) async throws -> Void` |
| 滑り込みの辺 | `DialogTransitionEdge` — case は `top` / `bottom` / `leading` / `trailing` (呼び出し側では `.bottom` のように書く) |

`DialogTransition` のイニシャライザは `init(presentation:dismissal:overlayDuration:)` で、3 引数とも省略できる (既定は `nil` = 供給なし)。

| プロパティ | 型 |
|---|---|
| `presentation` | `DialogTransition.Hook?` |
| `dismissal` | `DialogTransition.Hook?` |
| `overlayDuration` | `TimeInterval?` |

フックに渡ってくるのは中身のホスト `UIView` で、SwiftUI の中身では SwiftUI を包むホスト View になる。

## UIKit での添付

中身になる `UIView` の extension プロパティに設定する。

| プロパティ | 型 |
|---|---|
| `ksDialogTransition` | `DialogTransition?` |

```swift
// content は registry へ登録する中身の View (登録の書き方は Dialog 公開面)
let content = ConfirmContentView()
content.ksDialogTransition = DialogTransition(
    presentation: { hostView in
        hostView.alpha = 0
        // 演出が終わってから戻る (戻るまで器は「表示中」へ進まない)
        await withCheckedContinuation { continuation in
            UIView.animate(withDuration: 0.2) {
                hostView.alpha = 1
            } completion: { _ in
                continuation.resume()
            }
        }
    }
    // dismissal を書いていないので、閉鎖は器の既定のフェードになる
)
```

## SwiftUI での添付

中身の body ルートに modifier を付ける。

| modifier | 渡す型 |
|---|---|
| `.ksDialogTransition(...)` | `DialogTransition` |

レイアウト属性の modifier (`.ksDialogOptions(...)` / `.ksDialogPlacement(...)`) と同じ `ks` 接頭辞で、UIKit の添付プロパティ `ksDialogTransition` とも綴りが揃う ([iOS のレイアウト公開面](layout-surface.md))。

```swift
Dialog.shared.registry.register(ConfirmViewModel.self) { viewModel, notifier in
    ConfirmContent(message: viewModel.message, notifier: notifier)
        .ksDialogTransition(.slide(from: .bottom, duration: 0.3))   // 下から滑り込み、下へ滑り出す
}
```

## プリセット factory

`DialogTransition` の static メソッドとして公開している。返るのは `presentation` / `dismissal` / `overlayDuration` がすべて埋まった `DialogTransition` である。

| プリセット | 署名 |
|---|---|
| `fade` | `DialogTransition.fade(duration:easing:)` |
| `slide` | `DialogTransition.slide(from:duration:easing:)` |
| `zoom` | `DialogTransition.zoom(duration:easing:)` |
| `none` | `DialogTransition.none()` (引数なし) |

- `duration` は `TimeInterval` (秒単位)、既定は `0.25`
- `easing` は `UITimingCurveProvider` に準拠する値 (署名上は `any UITimingCurveProvider`)、既定は `.standard` (加速して減速する標準の曲線)
- `from` は `DialogTransitionEdge`。`.leading` / `.trailing` はレイアウト方向に追随し、右から左へ読む環境では左右が入れ替わる。`.top` / `.bottom` は物理方向で変わらない

片側だけプリセットにしたいときは、プリセットが返した値からフックを取り出して組み合わせる:

```swift
// 出現は zoom プリセット、閉鎖は自作のフック
DialogTransition(presentation: DialogTransition.zoom().presentation, dismissal: myDismissalHook)
```

## framework 固有の注意

- **プリセット factory は MainActor 上で呼ぶ** (既定の easing 値が MainActor 隔離のため)。添付を書く場所 (`UIView` の初期化・SwiftUI の body) はいずれも MainActor なので、通常の使い方では意識せずに済む
- **フックは `@MainActor` で始まる**ので、フックの中で UIKit の API をそのまま呼べる
- **フックは `throws`** なので、演出の途中で失敗を投げてよい。投げた失敗は器が吸収し、show の結果には影響しない (契約は core)
- **成立しない `duration`** — 0・負値・NaN・無限大 — では演出を省いて最終状態へ直ちに飛ぶ。iOS はそれ以外の長さをすべてアニメーションとして組めるため、他形態にあるような表現上の上限はない

## 関連

- [トランジションのルール](../../core/api/transition-semantics.md) — 演出の採用時点・既定の演出・結果が返る時点 (契約の正)
- [iOS の Dialog 公開面](dialog-surface.md) — 登録・表示・結果の受け取りの公開面
- [iOS のレイアウト公開面](layout-surface.md) — 属性の添付面と型 (演出と同じ添付の規律)
