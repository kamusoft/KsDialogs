# 演出の選択を host へ運ぶ

出入りの演出は content に添付するもので、content は host 側で組み立てられる。だから共有コードは animation の型を持たない。

- `show` の引数で演出を渡す経路は無い
- 共有コードにできるのは「どの演出を使うか」を ViewModel に載せて運ぶことだけ
- 各 host が、その選択を Native の `DialogTransition` へ写して content に添付する

## 選択を共有 ViewModel に載せる

次は、選択肢を enum で宣言し、それを持つ ViewModel を表示する呼び出し元である。共有コードに演出そのものは現れない。

```kotlin
package com.example.shared

import jp.kamusoft.ksdialogs.kmp.Dialog
import jp.kamusoft.ksdialogs.kmp.DialogException
import jp.kamusoft.ksdialogs.kmp.DialogResult
import jp.kamusoft.ksdialogs.kmp.DialogViewModel
import jp.kamusoft.ksdialogs.kmp.KsDialog
import kotlin.coroutines.cancellation.CancellationException

enum class TransitionChoice {
    FADE,
    SLIDE_UP,
    ZOOM,
    NONE,
}

class NoticeViewModel(
    val message: String,
    val transition: TransitionChoice,
) : DialogViewModel<Boolean>

class ItemEditor(private val dialogs: KsDialog = Dialog.instance) {
    @Throws(DialogException::class, CancellationException::class)
    suspend fun notifySaved(): Boolean =
        dialogs.show(NoticeViewModel("Saved", TransitionChoice.SLIDE_UP)) is DialogResult.Completed
}
```

## プリセットを選ぶ

プリセットは `presentation`・`dismissal`・`overlayDuration` を一度に埋めるので、出と入りが対称になる。

| プリセット | 演出 | Android | iOS |
|---|---|---|---|
| フェード | 透明度で出入りする | `DialogTransition.fade(duration, easing)` | `DialogTransition.fade(duration:easing:)` |
| スライド | 指定した辺から滑り込み、同じ辺へ滑り出す | `DialogTransition.slide(from, duration, easing)` | `DialogTransition.slide(from:duration:easing:)` |
| ズーム | 0.8 倍から等倍へ広がり、同じ倍率へ縮んで消える | `DialogTransition.zoom(duration, easing)` | `DialogTransition.zoom(duration:easing:)` |
| なし | 中身は無演出。覆いはフェードする | `DialogTransition.none()` | `DialogTransition.none()` |

引数はどのプリセットでも同じ意味で、`from` だけがスライド専用である。

| 引数 | 何を決めるか | Android | iOS |
|---|---|---|---|
| `from` | 滑り込み・滑り出しの辺 (スライドでのみ必須) | `DialogTransitionEdge` | `DialogTransitionEdge` |
| `duration` | 片道の時間 | `kotlin.time.Duration`、既定は 250 ミリ秒 | 秒の `TimeInterval`、既定は `0.25` |
| `easing` | 時間に対する進み方 | `Interpolator`、既定は `AccelerateDecelerateInterpolator` | `any UITimingCurveProvider`、既定は `.standard` |

辺は 4 つで、左右に当たる 2 つだけがレイアウト方向に追随する。

| 辺 | Android | iOS | 向き |
|---|---|---|---|
| 上 | `DialogTransitionEdge.TOP` | `.top` | 物理方向のまま変わらない |
| 下 | `DialogTransitionEdge.BOTTOM` | `.bottom` | 物理方向のまま変わらない |
| 行の始まり側 | `DialogTransitionEdge.START` | `.leading` | レイアウト方向に追随し、右から左へ読む環境では入れ替わる |
| 行の終わり側 | `DialogTransitionEdge.END` | `.trailing` | レイアウト方向に追随し、右から左へ読む環境では入れ替わる |

## host で content に添付する

演出は静的 option・置き場所に続く第 3 の添付スロットで、規律も同じである — 初回表示までに添付し、初回のネイティブレイアウトパスの時点で添付されている値が実行される ([レイアウト](layout.md))。

| 添付先 | Android | iOS |
|---|---|---|
| 従来 View 系 | 拡張プロパティ `ksDialogTransition` | extension プロパティ `ksDialogTransition` |
| 宣言的 UI | `KsDialogAttributes(transition = …)` | modifier `.ksDialogTransition(…)` |

次は Android の登録で、ViewModel が運んだ選択を Android の演出へ写し、返す content に添付している。

```kotlin
Dialog.instance.registry.register(NoticeViewModel::class) { viewModel, notifier ->
    NoticeContentView(this, viewModel, notifier).apply {
        ksDialogTransition = when (viewModel.transition) {
            TransitionChoice.FADE -> DialogTransition.fade()
            TransitionChoice.SLIDE_UP -> DialogTransition.slide(from = DialogTransitionEdge.BOTTOM)
            TransitionChoice.ZOOM -> DialogTransition.zoom()
            TransitionChoice.NONE -> DialogTransition.none()
        }
    }
}
```

同じ登録を iOS で書くとこうなる。共有 enum は Swift から見ると網羅性が閉じないため、`default` が要る。

```swift
Dialog.shared.kmp.register(NoticeViewModel.self) { viewModel, notifier in
    let content = NoticeContentView(message: viewModel.message, notifier: notifier)
    content.ksDialogTransition = switch viewModel.transition {
    case .fade: DialogTransition.fade()
    case .slideUp: DialogTransition.slide(from: .bottom)
    case .zoom: DialogTransition.zoom()
    case .none: DialogTransition.none()
    default: DialogTransition.fade()
    }
    return content
}
```

この登録があると、共有コードの `show(NoticeViewModel("Saved", TransitionChoice.SLIDE_UP))` は次の順に進む。

1. host の factory が content を作り、器がそれを最終的な位置とサイズにレイアウトする
2. 添付された出現のフックが UI スレッドで始まり、覆いのフェードが並行に走る
3. 両方が終わって表示中になり、利用者の操作で content が結果を報告する
4. 閉鎖のフックと覆いのフェードが並行に走る
5. 器が撤去され、`show` が結果を返す

## 既定の挙動と細則

- **未添付時** — 器の既定のクロスフェードで出入りする
- **覆いのフェード** — `overlayDuration` が時間を決める。覆いは常にフェードし、中身の演出と並行に走り、フックには渡らない。プリセットは自分の duration をここにも設定するので、指定なしで時間がそろう
- **成立しない `duration`** — 0・負値・非有限値、Android ではミリ秒に落とすと 0 になる正の微小値も、失敗させず演出を省いて最終状態へ直ちに飛ぶ
- **結果が届く時点** — 閉鎖のフックと覆いのフェードが両方終わり、器が撤去された後である
- **閉鎖の演出が走る経路** — 外側タップ・Android の戻るボタン・呼び出し元のキャンセルを含む、ライブラリが実行するすべての閉鎖経路。OS が器を外した場合だけは、演出する対象が残っていないため走らない
- **Loading と Toast** — カスタム content には同じ添付を受け付ける。内蔵 content は器の既定のクロスフェード固定である

## フックを自作する

プリセットの代わりに、自分で書いたフックを持つ `DialogTransition` を添付できる。

| 部品 | Android | iOS |
|---|---|---|
| 演出の組 | `DialogTransition(presentation, dismissal, overlayDuration)` | `DialogTransition(presentation:dismissal:overlayDuration:)` |
| フックの型 | `suspend (View) -> Unit` | `DialogTransition.Hook` |

- フックは片側だけ書いてもよい。書かなかった側は器の既定のクロスフェードになる
- 添付したフックは既定と合成されず、その側を完全に置き換える。混ぜたいときは、プリセットが返した値からフックを取り出して自作の `DialogTransition` に渡す
- フックは UI スレッドで開始され、その時点でホスト View は最終的な位置とサイズにレイアウト済みである。器はフックの完了を待つ
- フックが投げた失敗は器が吸収する。出現なら表示中へ進み、閉鎖なら器の撤去を続行し、`show` の結果は影響を受けない。中途半端に変わった見た目をライブラリが元へ戻すことはない
- ライブラリはフックにタイムアウトを設けないので、戻らないフックは Dialog を画面に残す。デバッグビルドで閾値を超えたときに警告ログを出すだけである
