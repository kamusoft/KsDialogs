package jp.kamusoft.ksdialogs.kmp

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.reflect.KClass

/**
 * ViewModel 型をキーに ViewModel factory を引く表。
 *
 * 共有コードの型指定 show はこの表から ViewModel を作る。
 * 表そのものは不変の写像として保持し、更新は比較交換で置き換える —
 * 登録も解決も任意のスレッドから行え、1回の解決は呼び出し時点のスナップショットに対して行われるため、
 * 並行する再登録が解決の途中に混ざることがない。
 */
@OptIn(ExperimentalAtomicApi::class)
internal class ViewModelFactoryStore {
    private val factories = AtomicReference<Map<KClass<*>, () -> Any>>(emptyMap())

    /** ViewModel 型に対する factory を入れる。同じ型への再登録は後勝ちになる。 */
    fun put(viewModelClass: KClass<*>, factory: () -> Any) {
        while (true) {
            val current = factories.load()
            val updated = current + (viewModelClass to factory)
            if (factories.compareAndSet(current, updated)) return
        }
    }

    // 失敗の診断文言は英語固定でローカライズしない (cross/ADR-0015)。
    /**
     * 登録済みの factory で ViewModel を作る。
     *
     * @throws DialogException factory が未登録のとき、または生成物の実行時クラスがキーと一致しないとき。
     *   どちらも登録側の構成ミスなので、表示へ進まずに失敗させる
     */
    fun <VM : Any> create(viewModelClass: KClass<VM>): VM {
        val factory = factories.load()[viewModelClass]
            ?: throw DialogException(
                "No ViewModel factory is registered for ViewModel type ${viewModelClass.displayName}.",
            )
        val created = factory()
        // 中身の View は生成物の実行時クラスで解決されるため、キーと違うクラスを返す factory は
        // 表示側で「未登録」に化ける。ここで構成ミスとして止める
        if (created::class != viewModelClass) {
            throw DialogException(
                "The registered ViewModel factory does not produce ViewModel type " +
                    "${viewModelClass.displayName}. It produced ${created::class.displayName} instead. " +
                    "A ViewModel factory must return a ViewModel of the same class as its registration key.",
            )
        }
        @Suppress("UNCHECKED_CAST")
        return created as VM
    }
}

/** 失敗の説明文に載せる型の呼び名。 */
private val KClass<*>.displayName: String
    get() = simpleName ?: toString()
