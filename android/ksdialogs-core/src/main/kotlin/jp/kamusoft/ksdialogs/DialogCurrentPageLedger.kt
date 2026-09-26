package jp.kamusoft.ksdialogs

import android.graphics.RectF
import android.view.View

/**
 * 宣言的 UI の modifier で「現在ページ」を名乗った枠 1 つ分。
 *
 * KsDialogs の Compose モジュールが実装する、ライブラリ内部の型。
 */
@KsDialogsInternalApi
public interface DialogCurrentPageMarker {
    /** 枠が載っている View。どのウィンドウに属するかの判定に使う。 */
    public val hostView: View

    /** 枠の矩形を、[hostView] が載っているウィンドウの座標 (px) で返す。まだ配置されていなければ null。 */
    public fun boundsInWindow(): RectF?
}

/**
 * 宣言的 UI の modifier で「現在ページ」を名乗った枠の台帳。
 *
 * KsDialogs の Compose モジュールが使う、ライブラリ内部の入口。操作は UI スレッドで行う。
 */
@KsDialogsInternalApi
public object DialogCurrentPageLedger {

    /** 枠が画面へ配置されたことを記録する。配置し直された枠は最新の順番で記録し直す。 */
    public fun attach(marker: DialogCurrentPageMarker) {
        DialogCurrentPageMarkerLedger.shared.attach(marker)
    }

    /** 枠が画面から外れたことを記録する。 */
    public fun detach(marker: DialogCurrentPageMarker) {
        DialogCurrentPageMarkerLedger.shared.detach(marker)
    }
}

/**
 * modifier で名乗った枠を取得元にする台帳の本体。
 *
 * 台帳の規則は iOS の SwiftUI 版 modifier と同じ (core/ADR-0038):
 *
 * 1. 名乗った枠は画面に配置されている間だけ台帳に載る。外れたら台帳から外れ、残りの候補で決め直す
 * 2. 問い合わせでは、ダイアログを出す画面 (Activity) が持つウィンドウ以外に属するもの
 *    (別の Activity のウィンドウ・KsDialogs 自身の器のウィンドウ)、
 *    ウィンドウと重ならない矩形 (隣のページなど画面外に置かれたもの) を候補から外す
 * 3. 候補の矩形が入れ子になっていれば内側 (小さい方) を採る
 * 4. それ以外は最後に画面へ配置されたものを採る
 * 5. 候補が無ければ見つからなかった扱いにし、次の取得元へ進む
 */
@OptIn(KsDialogsInternalApi::class)
internal class DialogCurrentPageMarkerLedger : DialogCurrentPageSource {

    /** 台帳の 1 行。 */
    private class Entry(val marker: DialogCurrentPageMarker, val order: Long)

    private val entries = mutableListOf<Entry>()
    private var nextOrder = 0L

    /** 台帳に載っている枠の数。 */
    val attachedMarkerCount: Int
        get() = entries.size

    fun attach(marker: DialogCurrentPageMarker) {
        entries.removeAll { it.marker === marker }
        entries.add(Entry(marker, nextOrder++))
    }

    fun detach(marker: DialogCurrentPageMarker) {
        entries.removeAll { it.marker === marker }
    }

    override fun lookUpPageRect(pageWindowRoot: View): DialogCurrentPageLookup {
        val windowRect = DialogCurrentPageGeometry.screenRect(pageWindowRoot)
        val candidates = entries.mapNotNull { entry ->
            val host = entry.marker.hostView
            if (!DialogCurrentPageGeometry.isInPresentingActivity(host, pageWindowRoot)) {
                return@mapNotNull null
            }
            val bounds = entry.marker.boundsInWindow() ?: return@mapNotNull null
            // 枠の矩形は枠が載っているウィンドウの座標なので、そのウィンドウの根の画面上の位置を足して
            // 画面座標にする (モーダルのウィンドウは Activity のメインウィンドウと原点が違い得る)
            val windowOrigin = IntArray(2).also(host.rootView::getLocationOnScreen)
            val rect = DialogScreenRect(bounds.left, bounds.top, bounds.right, bounds.bottom)
                .offset(windowOrigin[0].toFloat(), windowOrigin[1].toFloat())
            DialogCurrentPageCandidate(rect, entry.order)
        }
        val chosen = chooseCurrentPage(candidates, windowRect)
            ?: return DialogCurrentPageLookup.NotFound(
                "No view marked with ksDialogCurrentPage() is placed in a window of the activity presenting the dialog.",
            )
        return DialogCurrentPageLookup.Found(chosen)
    }

    companion object {
        /** modifier が書き込む台帳。 */
        val shared: DialogCurrentPageMarkerLedger = DialogCurrentPageMarkerLedger()
    }
}

/**
 * 台帳の候補 1 つ。
 *
 * @property rect 枠の矩形 (画面座標、px)
 * @property order 画面へ配置された順番。大きいほど新しい
 */
internal data class DialogCurrentPageCandidate(val rect: DialogScreenRect, val order: Long)

/**
 * 台帳の候補から表示中のページを 1 つ選ぶ。
 *
 * 空の矩形とウィンドウに重ならない矩形を外し、別の候補を内側に抱えている候補 (外側) を外したうえで、
 * 最後に配置されたものを採る。同じ矩形どうしはどちらも残し、順番で決める。
 *
 * @param windowRect ダイアログを出す画面のウィンドウの矩形 (画面座標)
 * @return 選んだ候補の矩形。候補が残らなければ null
 */
internal fun chooseCurrentPage(
    candidates: List<DialogCurrentPageCandidate>,
    windowRect: DialogScreenRect,
): DialogScreenRect? {
    val placed = candidates.filter { !it.rect.isEmpty && it.rect.intersects(windowRect) }
    val innermost = placed.filter { candidate ->
        placed.none { other -> other.rect != candidate.rect && candidate.rect.contains(other.rect) }
    }
    return innermost.maxByOrNull { it.order }?.rect
}
