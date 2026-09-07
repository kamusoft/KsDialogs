# 出入りの演出を添付する

出現と閉鎖の演出は `DialogTransition` にまとめ、コンテンツに添付する。`show` の引数で渡す経路はない。この文書はプリセットの選び方と、添付から表示までの書き方を扱う。

## プリセットを選ぶ

| プリセット | 署名 | 演出 |
|---|---|---|
| fade | `DialogTransition.fade(duration, easing)` | 透明度で出入りする |
| slide | `DialogTransition.slide(from, duration, easing)` | `from` の辺から滑り込み、同じ辺へ滑り出す |
| zoom | `DialogTransition.zoom(duration, easing)` | 少し縮んだ状態から等倍へ広がり、同じ倍率へ縮んで消える |
| none | `DialogTransition.none()` | コンテンツ側は無演出 |

プリセットは `presentation`、`dismissal`、`overlayDuration` がすべて埋まった `DialogTransition` を返す。

| 引数 | 型 | 既定 | 意味 |
|---|---|---|---|
| `from` | `DialogTransitionEdge` | なし (`slide` でのみ必須) | 滑り込み・滑り出しの辺 |
| `duration` | `kotlin.time.Duration` | 250 ミリ秒 | 片道の時間 |
| `easing` | `Interpolator` | `AccelerateDecelerateInterpolator` | 時間に対する進み方 |

| `from` に渡す値 | 向き |
|---|---|
| `TOP` / `BOTTOM` | 物理方向のまま変わらない |
| `START` / `END` | レイアウト方向に追随する (右から左へ読む環境では左右が入れ替わる) |

## 既定の挙動と細則

- **未添付時** — コンテンツと覆いが 250 ミリ秒でクロスフェードする
- **片側だけ添付** — 書いた側は完全に差し替わり、書かなかった側だけが既定のクロスフェードになる。既定と合成はしない
- **`none()`** — コンテンツ側だけを無演出にする。覆いは既定どおりフェードする
- **覆い** — コンテンツとは別のレイヤで器が常に出入りさせる。フェード時間は `overlayDuration` に従い、`duration` を取るプリセットは自分の `duration` をそこにも入れる
- **並行実行** — コンテンツの演出と覆いのフェードは並行に走り、出現も退出も両方の完了を待つ
- **成立しない `duration`** — `0`・負値・無限大に加えて、ミリ秒に落とすと `0` になる正の微小値も例外にせず、演出を省いて最終状態へ直ちに移る
- **添付値の採用時点** — 初回のネイティブレイアウトパスの後。表示中に書き換えても現在の表示には反映されない

## Compose コンテンツへ添付する

`KsDialogAttributes` の `transition` に渡す。レイアウト属性と同じ宣言なので、`options` や `placement` と一緒に書ける。

以下は下辺から 300 ミリ秒で滑り込むコンテンツである。

```kotlin
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import jp.kamusoft.ksdialogs.DialogNotifier
import jp.kamusoft.ksdialogs.DialogTransition
import jp.kamusoft.ksdialogs.DialogTransitionEdge
import jp.kamusoft.ksdialogs.compose.KsDialogAttributes
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SlidingConfirmContent(viewModel: ConfirmViewModel, notifier: DialogNotifier<Boolean>) {
    KsDialogAttributes(
        transition = DialogTransition.slide(
            from = DialogTransitionEdge.BOTTOM,
            duration = 300.milliseconds,
        ),
    )
    Column {
        Text(viewModel.message)
        Button(onClick = { notifier.complete(true) }) { Text("OK") }
    }
}
```

## View コンテンツへ添付する

`View` の拡張プロパティ `ksDialogTransition` に設定する。View を組み立てるときに設定しておけばよい。

以下は同じ滑り込みを View コンテンツで添付した例である。

```kotlin
import android.content.Context
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import jp.kamusoft.ksdialogs.DialogNotifier
import jp.kamusoft.ksdialogs.DialogTransition
import jp.kamusoft.ksdialogs.DialogTransitionEdge
import jp.kamusoft.ksdialogs.ksDialogTransition
import kotlin.time.Duration.Companion.milliseconds

class SlidingConfirmCardView(
    context: Context,
    viewModel: ConfirmViewModel,
    notifier: DialogNotifier<Boolean>,
) : LinearLayout(context) {
    init {
        orientation = VERTICAL
        addView(TextView(context).apply { text = viewModel.message })
        addView(
            Button(context).apply {
                text = "OK"
                setOnClickListener { notifier.complete(true) }
            },
        )
        ksDialogTransition = DialogTransition.slide(
            from = DialogTransitionEdge.BOTTOM,
            duration = 300.milliseconds,
        )
    }
}
```

## 登録して呼び出す

演出はコンテンツ側の記述なので、登録も呼び出しも演出を持たない Dialog と変わらない ([Dialog](dialogs.md))。

```kotlin
import android.app.Application
import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.compose.registerCompose

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Dialog.instance.registry.registerCompose(ConfirmViewModel::class) { viewModel, notifier ->
            SlidingConfirmContent(viewModel, notifier)
        }
    }
}
```

`show` を呼ぶと、添付した演出で滑り込み、結果が報告されると同じ辺へ滑り出す。結果は最初の報告で確定するが、`show` が戻るのは退出の演出・覆いのフェード・器の撤去がすべて終わった後である。したがって次の画面遷移を `show` の後に書けば、Dialog が消えきってから走る。

```kotlin
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import jp.kamusoft.ksdialogs.Dialog
import jp.kamusoft.ksdialogs.DialogResult
import kotlinx.coroutines.launch

class ItemActivity : ComponentActivity() {
    fun onDeleteClicked() {
        lifecycleScope.launch {
            val result = Dialog.instance.show(ConfirmViewModel("Delete this item?"))
            if (result is DialogResult.Completed && result.value) {
                deleteItem()
            }
        }
    }

    private fun deleteItem() {
    }
}
```

呼び出し元のコルーチンをキャンセルした場合は `CancellationException` が伝播する。退出の演出と器の撤去はそのまま完遂される。

## 独自のフックを渡す

コンストラクタは `DialogTransition(presentation, dismissal, overlayDuration)` で、3 引数とも省略できる。

- フックの型は `suspend (View) -> Unit` である
- Main dispatcher で始まり、レイアウト済みのホスト View を受け取る。位置と大きさが決まった状態から演出を始められる
- 自身のアニメーションが完了した時点で戻る必要がある。器はフックの完了を待ち、タイムアウトは設けない。戻らないフックは Dialog を閉じられなくする
- フックが例外を投げても器が吸収してログに残すだけで、`show` は結果を返す。フックが途中まで変えた見た目はライブラリが元へ戻さない
- デバッグ可能なアプリでは、フックが 5 秒を超えたときに警告ログが出る

以下は下から持ち上げながら現れ、透明度だけで消える演出である。アニメーションの完了待ちは `suspendCancellableCoroutine` で書く。

```kotlin
import android.view.ViewPropertyAnimator
import jp.kamusoft.ksdialogs.DialogTransition
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

fun slideUpTransition() = DialogTransition(
    presentation = { hostView ->
        hostView.translationY = 80f
        hostView.alpha = 0f
        hostView.animate().translationY(0f).alpha(1f).awaitEnd(300L)
    },
    dismissal = { hostView ->
        hostView.animate().alpha(0f).awaitEnd(200L)
    },
    overlayDuration = 200.milliseconds,
)

suspend fun ViewPropertyAnimator.awaitEnd(durationMillis: Long) {
    suspendCancellableCoroutine { continuation ->
        setDuration(durationMillis)
            .withEndAction { if (continuation.isActive) continuation.resume(Unit) }
            .start()
        continuation.invokeOnCancellation { cancel() }
    }
}
```

添付の仕方はプリセットと同じで、Compose なら `KsDialogAttributes(transition = slideUpTransition())`、View なら `ksDialogTransition = slideUpTransition()` と書く。登録も呼び出し元も変わらない。

## プリセットと独自のフックを組み合わせる

プリセットから片側のフックだけを取り出し、自作のフックと一緒にコンストラクタへ渡す。

以下は出現をプリセットの zoom に任せ、閉鎖だけを自作のフェードにした組み合わせである。

```kotlin
import jp.kamusoft.ksdialogs.DialogTransition
import kotlin.time.Duration.Companion.milliseconds

fun zoomInFadeOutTransition() = DialogTransition(
    presentation = DialogTransition.zoom(200.milliseconds).presentation,
    dismissal = { hostView -> hostView.animate().alpha(0f).awaitEnd(140L) },
    overlayDuration = 200.milliseconds,
)
```

`overlayDuration` だけをプリセットに揃えたいときは `DialogTransition.zoom(200.milliseconds).overlayDuration` を、閉鎖だけをプリセットにしたいときは `dismissal` を同じように渡す。
