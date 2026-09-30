package jp.kamusoft.ksdialogs.compose.support

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import jp.kamusoft.ksdialogs.compose.markAsDialogCurrentPage

/**
 * 表示中のページを Compose の modifier で名乗らせる画面。下部ナビゲーションを持つ `Scaffold` を模す。
 *
 * 画面は「中身の枠 (残りの高さいっぱい)」と「下部バー (高さ [BOTTOM_BAR_HEIGHT_DP])」の縦並びで、
 * `Scaffold` の content 枠と bottomBar の配置にあたる。どの枠に modifier を付けるかは [screen] で切り替え、
 * 切り替えは画面遷移 (前の画面が composition から外れる) にあたる。
 *
 * 画面は Compose の組み立てに要る owner 2 種を自分で持つ (素の Activity はそれを持たないため)。
 */
class CurrentPageComposeTestActivity : Activity(), LifecycleOwner, SavedStateRegistryOwner {

    /** 表示する画面。UI スレッドで書き換える。 */
    var screen: CurrentPageTestScreen by mutableStateOf(CurrentPageTestScreen.MARKED)

    /** 入れ子の画面で、外側の枠にも modifier を付けるか。UI スレッドで書き換える。 */
    var marksOuterFrame: Boolean by mutableStateOf(false)

    /** 組み立てが確定した画面と外側の枠の印の組。[screen] と [marksOuterFrame] の反映を待つのに使う。 */
    @Volatile
    var renderedState: Pair<CurrentPageTestScreen, Boolean>? = null
        private set

    /**
     * [CurrentPageTestScreen.WITH_MODAL] のモーダルの中で、modifier を付けた枠と同じ外形を占める View。
     *
     * 枠の画面上の位置を、modifier とは別の経路 (View の画面座標) で読むために置く。モーダルが無ければ null。
     */
    @Volatile
    var modalFrameView: View? = null

    /** 画面全体を組み立てる View。 */
    lateinit var composeView: ComposeView
        private set

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedStateRegistryController.performRestore(savedInstanceState)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        window.decorView.setViewTreeLifecycleOwner(this)
        window.decorView.setViewTreeSavedStateRegistryOwner(this)
        composeView = ComposeView(this).apply {
            setBackgroundColor(Color.WHITE)
            setContent {
                val current = screen
                val outer = marksOuterFrame
                CurrentPageScreenContent(current, outer) { modalFrameView = it }
                // 組み立てが確定した (modifier の配置と離脱が済んだ) ことを知らせる
                SideEffect { renderedState = current to outer }
            }
        }
        setContentView(composeView)
    }

    override fun onStart() {
        super.onStart()
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    override fun onResume() {
        super.onResume()
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    override fun onPause() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        super.onPause()
    }

    override fun onStop() {
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        super.onStop()
    }

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }

    companion object {
        /** 下部バーの高さ (dp)。システムバーの下まで広がる端末でもナビゲーションバーより高くする。 */
        const val BOTTOM_BAR_HEIGHT_DP: Int = 80

        /** 入れ子の画面で、内側の枠が外側の枠から上下に下がる幅 (dp)。 */
        const val NESTED_INSET_DP: Int = 40

        /** モーダルの中の枠の大きさ (dp)。ダイアログの中身と上下左右の dialogMargin が収まる大きさにする。 */
        const val MODAL_WIDTH_DP: Int = 300
        const val MODAL_HEIGHT_DP: Int = 360

        /** 窓の外に置く枠を横へずらす量 (dp)。どの端末の画面幅よりも大きくする。 */
        const val OUTSIDE_OFFSET_DP: Int = 4000
    }
}

/** [CurrentPageComposeTestActivity] が表示する画面。 */
enum class CurrentPageTestScreen {
    /** 中身の枠に modifier を付けた画面。 */
    MARKED,

    /** どこにも modifier を付けていない画面。 */
    UNMARKED,

    /** 中身の枠の内側にもう 1 つ modifier を付けた枠を持つ画面。外側は [CurrentPageComposeTestActivity.marksOuterFrame] で付ける。 */
    NESTED,

    /** 中身の枠と、窓の外に置いた枠の両方に modifier を付けた画面。窓の外の枠のほうが後に配置される。 */
    WITH_OUTSIDE_FRAME,

    /**
     * 中身の枠に modifier を付けたうえで、同じ Activity で出したモーダル (Compose の Dialog。Activity とは
     * 別のウィンドウ) の中の枠にも modifier を付けた画面。モーダルの枠のほうが後に配置される。
     */
    WITH_MODAL,
}

@Composable
private fun CurrentPageScreenContent(
    screen: CurrentPageTestScreen,
    marksOuterFrame: Boolean,
    onModalFrameView: (View?) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        val contentFrame = Modifier.weight(1f).fillMaxWidth()
        when (screen) {
            CurrentPageTestScreen.MARKED -> Box(contentFrame.markAsDialogCurrentPage())
            CurrentPageTestScreen.UNMARKED -> Box(contentFrame)
            CurrentPageTestScreen.NESTED -> Box(
                contentFrame.then(if (marksOuterFrame) Modifier.markAsDialogCurrentPage() else Modifier),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(vertical = CurrentPageComposeTestActivity.NESTED_INSET_DP.dp)
                        .markAsDialogCurrentPage(),
                )
            }

            CurrentPageTestScreen.WITH_MODAL -> {
                Box(contentFrame.markAsDialogCurrentPage())
                Dialog(
                    onDismissRequest = {},
                    properties = DialogProperties(usePlatformDefaultWidth = false),
                ) {
                    // 枠の外形を View にも占めさせ、画面上の位置を View の座標で読めるようにする
                    AndroidView(
                        factory = { context -> View(context).also(onModalFrameView) },
                        modifier = Modifier
                            .size(
                                width = CurrentPageComposeTestActivity.MODAL_WIDTH_DP.dp,
                                height = CurrentPageComposeTestActivity.MODAL_HEIGHT_DP.dp,
                            )
                            .markAsDialogCurrentPage(),
                    )
                }
                DisposableEffect(Unit) { onDispose { onModalFrameView(null) } }
            }

            CurrentPageTestScreen.WITH_OUTSIDE_FRAME -> Box(contentFrame) {
                Box(Modifier.fillMaxSize().markAsDialogCurrentPage())
                Box(
                    Modifier
                        .fillMaxSize()
                        .offset(x = CurrentPageComposeTestActivity.OUTSIDE_OFFSET_DP.dp)
                        .markAsDialogCurrentPage(),
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(CurrentPageComposeTestActivity.BOTTOM_BAR_HEIGHT_DP.dp))
    }
}
