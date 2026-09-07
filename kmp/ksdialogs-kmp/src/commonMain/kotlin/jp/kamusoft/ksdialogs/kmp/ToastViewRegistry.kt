package jp.kamusoft.ksdialogs.kmp

import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.HiddenFromObjC
import kotlin.reflect.KClass

/**
 * カスタム Toast の ViewModel 型と、中身の View / ViewModel の作り方の紐付け。
 *
 * View の型は OS ごとに異なるため、**View factory の登録は各 OS のネイティブ API で行う** —
 * Android は Kotlin ライブラリのレジストリ、iOS は Swift ライブラリの受け口が担う。
 * 共有コードから登録できるのは ViewModel factory だけで、その表は共有コードが持つ。
 *
 * どの入口から取得しても同じレジストリが返る。登録・解決は任意のスレッドから行える。
 */
public interface ToastViewRegistry {
    /**
     * ViewModel 型に対する ViewModel factory を登録する。
     *
     * 型を渡して表示する呼び出し ([KsToast.show] のクラス参照を渡す形) はこの factory で ViewModel を作る。
     * factory は表示のたびに呼ばれ、同じ型への再登録は後勝ちになる。
     * factory は登録キーと同じクラスの ViewModel を返さなければならない
     * (サブクラスを返すと中身の View が引けなくなるため、表示は構成ミスとして失敗する)。
     *
     * この登録口は共有 Kotlin コード専用で、Swift / Objective-C からは見えない。
     *
     * @param viewModelClass 登録キーになる ViewModel のクラス参照
     * @param factory ViewModel を生成する関数
     */
    @OptIn(ExperimentalObjCRefinement::class)
    @HiddenFromObjC
    public fun <VM : ToastViewModel> registerViewModel(
        viewModelClass: KClass<VM>,
        factory: () -> VM,
    )
}

/** ViewModel factory の表を持つ共有コード側のレジストリ実体 (kmp/ADR-0006)。 */
internal class SharedToastViewRegistry : ToastViewRegistry {
    /** ViewModel 型 → ViewModel factory の表。 */
    val viewModelFactories: ViewModelFactoryStore = ViewModelFactoryStore()

    override fun <VM : ToastViewModel> registerViewModel(
        viewModelClass: KClass<VM>,
        factory: () -> VM,
    ) {
        viewModelFactories.put(viewModelClass, factory)
    }
}
