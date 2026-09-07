package jp.kamusoft.ksdialogs.support

import android.content.Context
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.FrameLayout
import jp.kamusoft.ksdialogs.LoadingViewModel
import jp.kamusoft.ksdialogs.ToastAccessibilityAnnouncer
import jp.kamusoft.ksdialogs.ToastViewModel
import java.util.concurrent.atomic.AtomicInteger

/** カスタム Toast の表示に使う最小の ViewModel。 */
internal class ToastTestViewModel(val message: String = "カスタム Toast") : ToastViewModel

/** View factory を登録しない ViewModel。未登録の経路を確認するために使う。 */
internal class UnregisteredToastTestViewModel : ToastViewModel

/** 型を渡す表示で使う、configure で状態を書き換えられる ViewModel。 */
internal class ConfigurableToastTestViewModel(var title: String = "factory の既定") : ToastViewModel

/** value class の ViewModel。参照型限定の拒否を確かめるために使う。 */
@JvmInline
internal value class ValueClassToastTestViewModel(val label: String) : ToastViewModel

/**
 * 型を渡す表示の観察。ViewModel の生成と、中身の生成時に見えた状態を記録する。
 *
 * 中身の生成のたびに、そのとき ViewModel が持っていた表題を控えるので、
 * 「configure の完了後に中身が作られる」順序をそのまま読み取れる。
 */
internal class ToastTypedShowRecorder {
    private val lock = Any()
    private val created = mutableListOf<ConfigurableToastTestViewModel>()
    private val observed = mutableListOf<String>()
    private val views = mutableListOf<View>()

    /** ViewModel factory が作った ViewModel を生成順に並べたもの。 */
    val createdViewModels: List<ConfigurableToastTestViewModel>
        get() = synchronized(lock) { created.toList() }

    /** 中身の生成時に見えた表題を生成順に並べたもの。 */
    val observedTitles: List<String>
        get() = synchronized(lock) { observed.toList() }

    /** 中身が生成された回数。 */
    val viewCreationCount: Int
        get() = synchronized(lock) { views.size }

    /** 最後に生成された中身の View。 */
    val lastView: View?
        get() = synchronized(lock) { views.lastOrNull() }

    fun recordCreation(viewModel: ConfigurableToastTestViewModel) {
        synchronized(lock) { created.add(viewModel) }
    }

    fun recordView(title: String, view: View) {
        synchronized(lock) {
            observed.add(title)
            views.add(view)
        }
    }
}

/** Toast と Loading の両方のレジストリへ登録して、互いの独立を確かめるための ViewModel。 */
internal class DualRegistryToastTestViewModel :
    ToastViewModel,
    LoadingViewModel

/**
 * 支援技術への通知を書き留める観測用の通知口。
 *
 * 発行されたイベントの種類まで残すので、フォーカスを動かす通知が混じっていないかを見られる。
 */
internal class ToastTestAnnouncer : ToastAccessibilityAnnouncer {
    /** 発行された1件分。 */
    class Post(val eventType: Int, val text: CharSequence)

    private val lock = Any()
    private val recorded = mutableListOf<Post>()

    /** 発行された通知を発行順に並べたもの。 */
    val posts: List<Post>
        get() = synchronized(lock) { recorded.toList() }

    /** 読み上げへ流された文言。並びは発行順。 */
    val announcedMessages: List<String>
        get() = posts.filter { it.eventType == AccessibilityEvent.TYPE_ANNOUNCEMENT }
            .map { it.text.toString() }

    /** 支援技術のフォーカスを移動させるイベントが発行されたか。 */
    val didMoveFocus: Boolean
        get() = posts.any {
            it.eventType == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED ||
                it.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                it.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED
        }

    override fun announce(source: View, eventType: Int, text: CharSequence) {
        synchronized(lock) { recorded.add(Post(eventType, text)) }
    }
}

/** タップされた回数を数える面。背後の要素が反応したかどうかの判定に使う。 */
internal class TapCountingView(context: Context) : FrameLayout(context) {
    private val taps = AtomicInteger(0)

    /** これまでに受け取ったタップの数。 */
    val tapCount: Int
        get() = taps.get()

    init {
        isClickable = true
        setOnClickListener { taps.incrementAndGet() }
    }
}

/**
 * タップされた回数を数えるボタンを内側に持つ、カスタム Toast の中身。
 *
 * 対話可能な部品を置いても反応しないこと (完全非対話) の確認に使う。
 *
 * @param contentWidth 要求する幅 (px)
 * @param contentHeight 要求する高さ (px)
 */
internal class InteractiveToastContentView(
    context: Context,
    private val contentWidth: Int,
    private val contentHeight: Int,
) : FrameLayout(context) {

    private val taps = AtomicInteger(0)

    /** 内側のボタンが受け取ったタップの数。 */
    val tapCount: Int
        get() = taps.get()

    init {
        addView(
            Button(context).apply {
                text = "タップ"
                setOnClickListener { taps.incrementAndGet() }
            },
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT),
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(
                resolveSize(contentWidth, widthMeasureSpec),
                MeasureSpec.EXACTLY,
            ),
            MeasureSpec.makeMeasureSpec(
                resolveSize(contentHeight, heightMeasureSpec),
                MeasureSpec.EXACTLY,
            ),
        )
    }
}
