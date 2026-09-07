---
type: concept
title: MAUI のトランジション公開面
description: .NET MAUI (C#) でダイアログの出入りの演出を差し替えるときの公開名と署名 — 演出の型とフックのデリゲート型・code-behind からの添付プロパティ・プリセット factory と辺の綴り・duration と easing の型・ミリ秒表現に収まらない時間の扱い
tags: [maui, transition, api, surface]
timestamp: 2026-09-06
---

# MAUI のトランジション公開面

この文書を読むと、.NET MAUI (C#) でダイアログの出入りの演出を差し替えるときに書く型名とプロパティ名、code-behind からの添付の書き方、プリセットの綴りとコード例が分かる。

**この文書は MAUI の公開面 (名前・署名・コード例・framework 固有の注意) だけを扱い、挙動の契約は core が正である。** 対応する契約は [トランジションのルール](../../core/api/transition-semantics.md) で、演出の採用時点・既定の演出・結果が返る時点・フックが実行される閉鎖経路・失敗や未完了の扱いはそちらを読む。

## 演出の型とフックの型

| 用途 | 書く名前 |
|---|---|
| 演出の組 | `DialogTransition` (`sealed class`) |
| フックの型 | `Func<VisualElement, Task>` (専用の型名は持たず、デリゲート型をそのまま書く) |
| 滑り込みの辺 | `DialogTransitionEdge` (`Top` / `Bottom` / `Start` / `End`) |

コンストラクタは `DialogTransition(presentation, dismissal, overlayDuration)` で、3 引数とも省略できる (既定は `null` = 供給なし)。

| プロパティ | 型 |
|---|---|
| `Presentation` | `Func<VisualElement, Task>?` |
| `Dismissal` | `Func<VisualElement, Task>?` |
| `OverlayDuration` | `TimeSpan?` |

フックに渡ってくるのは中身の MAUI View そのものである。演出は MAUI 側のアニメーション API で書き、その結果がネイティブの器に反映される。

## code-behind での添付

添付プロパティは `Dialog` クラスの静的メンバとして置いている ([MAUI のレイアウト公開面](layout-surface.md) と同じ形)。

| メンバ | 役割 |
|---|---|
| `Dialog.SetTransition(view, transition)` | 添付する |
| `Dialog.GetTransition(view)` | 添付された値を読む |
| `Dialog.TransitionProperty` | 対応する `BindableProperty` |

**値がクロージャを持つため XAML には書けない。** 添付は code-behind からに限られる。

```csharp
var content = new ConfirmContentView(viewModel, notifier);
Dialog.SetTransition(content, DialogTransition.Zoom(TimeSpan.FromMilliseconds(200d)));
```

自分でフックを書くときは、アニメーションの完了を待つ `Task` を返す:

```csharp
Dialog.SetTransition(content, new DialogTransition(
    // 出現だけ自作の演出にする。閉鎖は書いていないので、中身は器の既定のフェードで消える
    presentation: async view =>
    {
        view.Opacity = 0d;
        await view.FadeToAsync(1d, 200u);
    }));
```

## プリセット factory

`DialogTransition` の static メソッドとして公開している。返るのは `Presentation` / `Dismissal` / `OverlayDuration` がすべて埋まった `DialogTransition` である。

| プリセット | 署名 |
|---|---|
| `Fade` | `DialogTransition.Fade(duration, easing)` |
| `Slide` | `DialogTransition.Slide(from, duration, easing)` |
| `Zoom` | `DialogTransition.Zoom(duration, easing)` |
| `None` | `DialogTransition.None()` (引数なし) |

- `duration` は `TimeSpan` (省略可)、省略時は 250 ミリ秒
- `easing` は `Easing` (省略可)、省略時は `Easing.CubicInOut` (加速して減速する標準の曲線)
- `from` は `DialogTransitionEdge`。`Start` / `End` はレイアウト方向に追随し、右から左へ読む環境では左右が入れ替わる。`Top` / `Bottom` は物理方向で変わらない

片側だけプリセットにしたいときは、プリセットが返した値からフックを取り出して組み合わせる:

```csharp
// 出現は Zoom プリセット、閉鎖は自作のフック
var transition = new DialogTransition(DialogTransition.Zoom().Presentation, myDismissalHook);
```

## framework 固有の注意

- **成立しない `duration`** — 0 以下に加えて、**ミリ秒表現に収まらない大きさ** (総ミリ秒が `uint` の上限を超える値。`TimeSpan.MaxValue` を含む) — では演出を省いて最終状態へ直ちに飛ぶ。MAUI のアニメーション API が時間を `uint` のミリ秒で受け取るためで、丸めも例外もしない
- **フックは UI スレッドで開始される**ので、フックの中で MAUI の API をそのまま呼べる
- **MAUI には呼び出し元キャンセルの経路がない**ため、終わらないフックからの脱出口は OS 発の器の消失だけである (契約は core)
- **フックの失敗** (返した `Task` が fault で終わること) は器が吸収し、show の結果には影響しない

## 関連

- [トランジションのルール](../../core/api/transition-semantics.md) — 演出の採用時点・既定の演出・結果が返る時点 (契約の正)
- [MAUI の Dialog 公開面](dialog-surface.md) — 登録・表示・結果の受け取りの公開面
- [MAUI のレイアウト公開面](layout-surface.md) — 添付プロパティと型 (演出と同じ添付の規律)
- [MAUI の DI 連携と登録糖衣](di-registration.md) — 1行登録と fallback resolver
