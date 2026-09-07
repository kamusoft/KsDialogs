package jp.kamusoft.ksdialogs.compose.support

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import jp.kamusoft.ksdialogs.DialogNotifier
import jp.kamusoft.ksdialogs.DialogOptions
import jp.kamusoft.ksdialogs.DialogPlacement
import jp.kamusoft.ksdialogs.compose.KsDialogAttributes
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * 指定した内容サイズを要求する、宣言的 UI の中身。
 *
 * 冒頭で器のメタ属性を宣言し、そのあとに大きさだけを持つ器を置く。
 * 属性を1つも渡さなければ宣言そのものを書かない中身になる。
 *
 * @param options 冒頭で宣言する静的メタ属性
 * @param placement 冒頭で宣言する動的メタ属性
 * @param observation 中身を載せた View と最初に描かれた外形の観察先
 */
@Composable
internal fun FixedSizeComposeContent(
    width: Dp,
    height: Dp,
    options: DialogOptions? = null,
    placement: DialogPlacement? = null,
    observation: ComposeContentObservation? = null,
) {
    if (options != null || placement != null) {
        KsDialogAttributes(options = options, placement = placement)
    }
    ObserveHostView(observation)
    Box(Modifier.size(width, height))
}

/**
 * 中身へ渡された結果報告口を、組み立ての外から使えるように取り出す。
 *
 * 取り出しは組み立てが確定した後に行い、投機的に組み立てられた回のものを拾わない。
 */
@Composable
internal fun <R> CaptureNotifier(
    notifier: DialogNotifier<R>,
    destination: CompletableDeferred<DialogNotifier<R>>,
) {
    SideEffect { destination.complete(notifier) }
}

/**
 * どの中身が組み立てられたかを、組み立て1回につき1度だけ記録する。
 *
 * @param destination 記録先。組み立て順に名前が並ぶ
 * @param label この中身の名前
 */
@Composable
internal fun RecordOnce(destination: MutableList<String>, label: String) {
    DisposableEffect(Unit) {
        destination.add(label)
        onDispose { }
    }
}

/**
 * 中身を載せている View を掴み、最初に描かれた時点の外形を記録する。
 *
 * 掴むのは組み立ての土台になっている View で、これはホストの中に隙間なく置かれるため、
 * ダイアログの中身そのものの外形として測れる。
 */
@Composable
internal fun ObserveHostView(observation: ComposeContentObservation?) {
    if (observation == null) {
        return
    }
    val view = LocalView.current
    DisposableEffect(Unit) {
        observation.contentView.set(view)
        view.observeFirstDrawnRect(observation.firstDrawnRect)
        onDispose { observation.disposals.incrementAndGet() }
    }
}

/**
 * 宣言的 UI の中身についての観察先。
 *
 * @property contentView 中身を載せている View
 * @property firstDrawnRect 最初に描かれた時点の外形 (画面座標・px)
 * @property disposals 組み立てが破棄された回数
 */
internal class ComposeContentObservation {
    val contentView: AtomicReference<View> = AtomicReference()
    val firstDrawnRect: CompletableDeferred<Rect> = CompletableDeferred()
    val disposals: AtomicInteger = AtomicInteger()
}

/**
 * 指定した内容サイズを要求する、従来 View 系の中身。
 *
 * 宣言的 UI の中身と外形をつき合わせるための対照として使う。
 *
 * @param contentWidth 要求する幅 (px)
 * @param contentHeight 要求する高さ (px)
 */
internal class FixedSizeContentView(
    context: Context,
    private val contentWidth: Int,
    private val contentHeight: Int,
) : View(context) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            resolveSize(contentWidth, widthMeasureSpec),
            resolveSize(contentHeight, heightMeasureSpec),
        )
    }
}
