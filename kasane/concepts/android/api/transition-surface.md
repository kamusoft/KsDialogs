---
type: concept
title: Android のトランジション公開面
description: Android Native (Kotlin) でダイアログの出入りの演出を差し替えるときの公開名と署名 — 演出の型とフックの型・従来 View 系の拡張プロパティと Compose の宣言・プリセット factory と辺の綴り・duration と easing の型・ミリ秒に落ちる微小値の扱い
tags: [android, transition, api, surface]
timestamp: 2026-09-05
---

# Android のトランジション公開面

この文書を読むと、Android Native (Kotlin) でダイアログの出入りの演出を差し替えるときに書く型名とプロパティ名、従来 View 系 / Compose での添付の書き方、プリセットの綴りとコード例が分かる。

**この文書は Android の公開面 (名前・署名・コード例・framework 固有の注意) だけを扱い、挙動の契約は core が正である。** 対応する契約は [トランジションのルール](../../core/api/transition-semantics.md) で、演出の採用時点・既定の演出・結果が返る時点・フックが実行される閉鎖経路・失敗や未完了の扱いはそちらを読む。

## 演出の型とフックの型

| 用途 | 書く名前 |
|---|---|
| 演出の組 | `DialogTransition` (`class`) |
| フックの型 | `suspend (View) -> Unit` (専用の型名は持たず、関数型をそのまま書く) |
| 滑り込みの辺 | `DialogTransitionEdge` (`TOP` / `BOTTOM` / `START` / `END`) |

`DialogTransition` のコンストラクタは `DialogTransition(presentation, dismissal, overlayDuration)` で、3 引数とも省略できる (既定は `null` = 供給なし)。

| プロパティ | 型 |
|---|---|
| `presentation` | `(suspend (View) -> Unit)?` |
| `dismissal` | `(suspend (View) -> Unit)?` |
| `overlayDuration` | `kotlin.time.Duration?` |

フックに渡ってくるのは中身のホスト `View` で、Compose の中身では composable を包む `AbstractComposeView` 派生のホストになる。

## 従来 View 系での添付

中身になる `View` の拡張プロパティに設定する。

| プロパティ | 型 |
|---|---|
| `ksDialogTransition` | `DialogTransition?` |

```kotlin
// content は registry へ登録する中身の View (登録の書き方は Dialog 公開面)
val content = ConfirmContentView(context)
content.ksDialogTransition = DialogTransition(
    // 出現だけ自作の演出にする。閉鎖は書いていないので、中身は器の既定のフェードで消える
    // (オーバーレイのフェードは、どちらの場合も器が行う)
    presentation = { hostView ->
        hostView.alpha = 0f
        // 演出が終わってから戻る (戻るまで器は「表示中」へ進まない)
        suspendCancellableCoroutine { continuation ->
            hostView.animate().alpha(1f).setDuration(200L)
                .withEndAction { continuation.resume(Unit) }
                .start()
        }
    },
)
```

## Compose での添付

中身の composable の冒頭で `KsDialogAttributes(transition = ...)` を宣言する (レイアウト属性と同じ宣言で、引数はすべて省略可)。

```kotlin
Dialog.instance.registry.registerCompose(ConfirmViewModel::class) { viewModel, notifier ->
    KsDialogAttributes(transition = DialogTransition.slide(from = DialogTransitionEdge.BOTTOM))
    ConfirmContent(viewModel.message, notifier)
}
```

`KsDialogAttributes` は**初回の組み立てで通る位置に書く**。`LazyColumn` のような遅延評価されるスコープの中に書いた宣言は初回の組み立てで実行されず、その表示には効かない。この宣言は別モジュール `ksdialogs-compose` に入っており、これを依存に追加した消費者だけが使える ([Android のレイアウト公開面](layout-surface.md))。

## プリセット factory

`DialogTransition` の companion object のメソッドとして公開している。返るのは `presentation` / `dismissal` / `overlayDuration` がすべて埋まった `DialogTransition` である。

| プリセット | 署名 |
|---|---|
| `fade` | `DialogTransition.fade(duration, easing)` |
| `slide` | `DialogTransition.slide(from, duration, easing)` |
| `zoom` | `DialogTransition.zoom(duration, easing)` |
| `none` | `DialogTransition.none()` (引数なし) |

- `duration` は `kotlin.time.Duration`、既定は 250 ミリ秒
- `easing` は `Interpolator`、既定は `AccelerateDecelerateInterpolator` (加速して減速する標準の曲線)
- `from` は `DialogTransitionEdge`。`START` / `END` はレイアウト方向に追随し、右から左へ読む環境では左右が入れ替わる。`TOP` / `BOTTOM` は物理方向で変わらない

片側だけプリセットにしたいときは、プリセットが返した値からフックを取り出して組み合わせる:

```kotlin
// 出現は zoom プリセット、閉鎖は自作のフック
DialogTransition(presentation = DialogTransition.zoom().presentation, dismissal = myDismissalHook)
```

## framework 固有の注意

- **フックは Main ディスパッチャで開始される**ので、フックの中で View の API をそのまま呼べる
- **フックは `suspend` 関数**なので、`suspendCancellableCoroutine` などでアニメーションの完了を待ってから戻す。戻るまで器は次の状態へ進まない (契約は core)
- **成立しない `duration`** — 0・負値・無限大に加えて、**ミリ秒に落とすと 0 になる正の微小値** (0.5 ミリ秒など) — では演出を省いて最終状態へ直ちに飛ぶ。アニメーションの時間をミリ秒で扱うためである
- **デバッグビルドの警告ログ**は、ライブラリではなく**提示先アプリ**がデバッグ可能かどうかで出るかが決まる

## 関連

- [トランジションのルール](../../core/api/transition-semantics.md) — 演出の採用時点・既定の演出・結果が返る時点 (契約の正)
- [Android の Dialog 公開面](dialog-surface.md) — 登録・表示・結果の受け取りの公開面
- [Android のレイアウト公開面](layout-surface.md) — 属性の添付面と型 (演出と同じ添付の規律)
