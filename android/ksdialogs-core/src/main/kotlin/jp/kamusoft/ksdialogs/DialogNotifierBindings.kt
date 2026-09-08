package jp.kamusoft.ksdialogs

import java.lang.ref.Reference
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference

/**
 * 表示中の ViewModel と結果チャネルを結ぶ紐付け表 (core/ADR-0018)。
 *
 * ViewModel の定義を空のまま保つため、結果報告口の置き場を ViewModel の外に持つ。
 * キーはインスタンスの同一性であり、等価比較 (equals) には依存しない —
 * 等価な別インスタンスは別の紐付けとして扱われる。
 * したがって `WeakHashMap` は使えない (キーの照合が equals で行われ、
 * 等価な data class の ViewModel どうしが衝突するため)。
 *
 * 紐付けは show が中身の View を生成する前に作り、show が終わる全経路で外す。
 * キーの保持は弱参照で、除去漏れが起きても ViewModel の解放とともに紐付けが消える安全網になる。
 * 解放されたキーは [ReferenceQueue] 経由で表から掃除する。
 *
 * 登録・取得は任意のスレッドから行える。
 */
internal object DialogNotifierBindings {
    private val lock = Any()
    private val collectedKeys = ReferenceQueue<Any>()
    private val bindings = HashMap<ViewModelKey, DialogResultChannel>()

    /**
     * ViewModel に結果チャネルを紐付ける。
     *
     * @return 紐付けられたら true。同じインスタンスが既に表示中なら false (並行 show の検出)
     */
    fun bind(viewModel: Any, resultChannel: DialogResultChannel): Boolean = synchronized(lock) {
        purgeCollectedKeys()
        val key = ViewModelKey(viewModel, collectedKeys)
        if (bindings.containsKey(key)) {
            // 表に入らないキーは掃除の対象にしない
            key.clear()
            return false
        }
        bindings[key] = resultChannel
        true
    }

    /** 紐付けを外す。別の show が作った紐付けは外さない。 */
    fun unbind(viewModel: Any, resultChannel: DialogResultChannel): Unit = synchronized(lock) {
        purgeCollectedKeys()
        val key = ViewModelKey(viewModel, collectedKeys)
        if (bindings[key] === resultChannel) {
            bindings.remove(key)
        }
        key.clear()
    }

    /** ViewModel に紐付いている結果チャネル。表示中でなければ null。 */
    fun resultChannel(viewModel: Any): DialogResultChannel? = synchronized(lock) {
        purgeCollectedKeys()
        val key = ViewModelKey(viewModel, collectedKeys)
        val resultChannel = bindings[key]
        key.clear()
        resultChannel
    }

    /** 解放済み ViewModel のキーを表から取り除く。ロックを保持した状態で呼ぶ。 */
    private fun purgeCollectedKeys() {
        while (true) {
            val collected: Reference<out Any> = collectedKeys.poll() ?: return
            // 取り出せるのは表に入れたキーそのものなので、同一性で照合して取り除ける
            bindings.remove(collected as ViewModelKey)
        }
    }

    /**
     * ViewModel インスタンスを指す弱参照のキー。
     *
     * ハッシュは生成時の identity hash に固定し、照合は参照の同一性で行う。
     * 参照先が解放されたキーは、掃除で渡されるキーそのもの以外とは一致しなくなる。
     */
    private class ViewModelKey(
        viewModel: Any,
        collectedKeys: ReferenceQueue<Any>,
    ) : WeakReference<Any>(viewModel, collectedKeys) {
        private val identityHash = System.identityHashCode(viewModel)

        override fun hashCode(): Int = identityHash

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ViewModelKey || identityHash != other.identityHash) return false
            val viewModel = get() ?: return false
            return viewModel === other.get()
        }
    }
}
