package jp.kamusoft.ksdialogs.compose

import android.content.Context
import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.AbstractComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import jp.kamusoft.ksdialogs.ksDialogOptions
import jp.kamusoft.ksdialogs.ksDialogPlacement
import jp.kamusoft.ksdialogs.ksDialogTransition

/**
 * 宣言的 UI の中身をダイアログの器に載せるためのホスト。
 *
 * 器から見ればただの View なので、提示・結果・レイアウトの経路は従来 View 系と同じ1系統に載る
 * (core/ADR-0011)。自分の大きさは中身の composable が要求する大きさになるため、
 * レイアウト規則のいう内容サイズもそのまま成立する。
 *
 * 組み立てに要る owner 2種 (LifecycleOwner / SavedStateRegistryOwner) は、
 * ダイアログ1枚の寿命に合わせてこのホスト自身が持つ。ダイアログのウィンドウは提示先画面とは
 * 別のウィンドウで、画面側の owner が View tree を伝ってこないためである。
 * ウィンドウから外れた時点 — 完了・キャンセル・呼び出し元キャンセル・画面破棄のいずれで閉じても
 * 必ず通る — で組み立てを破棄し、寿命を終える。
 *
 * @param composableContent 中身として組み立てる composable
 */
internal class DialogComposeContentView(
    context: Context,
    private val composableContent: @Composable () -> Unit,
) : AbstractComposeView(context), LifecycleOwner, SavedStateRegistryOwner {

    /** 中身が宣言したメタ属性の受け皿。届いた値はこのホストの添付として反映する。 */
    private val attributeCollector = DialogAttributeCollector(::applyCollectedAttributes)

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    /** owner を据えたウィンドウの根。ウィンドウから外れるときに元へ戻すために覚えておく。 */
    private var ownerHostRoot: View? = null

    /** 根へ自分を据える前にそこに居た owner 2種。据える前が空なら空を覚える。 */
    private var displacedLifecycleOwner: LifecycleOwner? = null
    private var displacedSavedStateRegistryOwner: SavedStateRegistryOwner? = null

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    /**
     * 描画の直前 — 器が実効値をスナップショットとして固定するのと同じ時点 — での取り込み。
     *
     * 宣言は届いた時点で受け皿から即座に添付へ反映されるので通常はここで拾うものはないが、
     * 契約が定める採用時点で必ず最新の宣言が添付に載っている状態にするための取りこぼし防止である。
     */
    private val attributeSettlingObserver = object : ViewTreeObserver.OnPreDrawListener {
        override fun onPreDraw(): Boolean {
            viewTreeObserver.removeOnPreDrawListener(this)
            applyCollectedAttributes()
            return true
        }
    }

    @Composable
    override fun Content() {
        CompositionLocalProvider(LocalDialogAttributeCollector provides attributeCollector) {
            composableContent()
        }
    }

    init {
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        setViewTreeLifecycleOwner(this)
        setViewTreeSavedStateRegistryOwner(this)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    override fun onAttachedToWindow() {
        // 組み立ての土台は、ウィンドウの根から owner をたどって用意される。
        // ダイアログのウィンドウには提示先画面の owner が伝わってこないため、
        // 組み立てが始まる前に、このウィンドウの根へ自分を owner として据える
        val root = rootView
        ownerHostRoot = root
        displacedLifecycleOwner = root.findViewTreeLifecycleOwner()
        displacedSavedStateRegistryOwner = root.findViewTreeSavedStateRegistryOwner()
        root.setViewTreeLifecycleOwner(this)
        root.setViewTreeSavedStateRegistryOwner(this)
        // 先に組み立てを走らせてから受け皿を張る (組み立てが同期実行されれば宣言はここで届く)
        super.onAttachedToWindow()
        viewTreeObserver.addOnPreDrawListener(attributeSettlingObserver)
    }

    override fun onDetachedFromWindow() {
        viewTreeObserver.removeOnPreDrawListener(attributeSettlingObserver)
        super.onDetachedFromWindow()
        disposeComposition()
        restoreDisplacedOwners()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }

    /**
     * ウィンドウの根へ据えた owner を、据える前の状態へ戻す。
     *
     * 自分の寿命が終わったあとも根に自分が残ると、そこから owner をたどる別の View が
     * DESTROYED のまま動かない lifecycle を掴んでしまう。
     * 現状はダイアログ1枚が1つのウィンドウを占めるため根を共有する相手はいないが、
     * その前提に依存せずに済むよう、書き換えた範囲は自分で戻す。
     */
    private fun restoreDisplacedOwners() {
        ownerHostRoot?.let { root ->
            root.setViewTreeLifecycleOwner(displacedLifecycleOwner)
            root.setViewTreeSavedStateRegistryOwner(displacedSavedStateRegistryOwner)
        }
        ownerHostRoot = null
        displacedLifecycleOwner = null
        displacedSavedStateRegistryOwner = null
    }

    /** 集めた宣言値を、このホスト自身への添付として反映する。 */
    private fun applyCollectedAttributes() {
        attributeCollector.options?.let { ksDialogOptions = it }
        attributeCollector.placement?.let { ksDialogPlacement = it }
        attributeCollector.transition?.let { ksDialogTransition = it }
    }
}
