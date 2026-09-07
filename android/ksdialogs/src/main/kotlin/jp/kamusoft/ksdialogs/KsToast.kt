package jp.kamusoft.ksdialogs

import android.content.Context
import android.view.View
import kotlin.reflect.KClass

/**
 * Toast 表示の契約。
 *
 * 既定 singleton エントリ ([Toast.instance]) と DI 注入のどちらからでも同じ契約で呼び出せる
 * (core/ADR-0002)。どちらの入口から呼んでもレジストリと一括設定は 1 プロセスで共有される。
 *
 * Toast は fire-and-forget の表示である (core/ADR-0031)。show は戻り値を持たず、
 * 表示の終了を待つ手段も、閉じる・書き換える手段も契約に無い。消滅の契機は duration の経過だけで、
 * 表示中はいかなる入力も奪わない。
 *
 * すべての呼び出しは任意のスレッドから行え、内部で UI スレッドへ移して受理順に直列化される。
 */
public interface KsToast {
    /**
     * カスタム Toast の ViewModel 型と View factory の紐付け。
     * Dialog / Loading のレジストリとは独立している。
     */
    public val registry: ToastViewRegistry

    /** Toast の一括設定 (core/ADR-0032)。各表示の受理時に読まれる。 */
    public var style: ToastStyle

    /**
     * デフォルト View でメッセージを表示する。
     *
     * 表示は受理された時点から数え、[durationMs] の経過で自動的に消える。
     * メッセージの内容は制限しない — 空文字は内容が空のまま表示され、長文は複数行に折り返す。
     *
     * @param message 表示する文言
     * @param durationMs 表示するミリ秒。null なら [ToastStyle] の既定 duration。
     *   0 以下は既定へ丸める
     * @param placement 配置。null なら [ToastStyle] のアプリ既定配置、それも無ければ契約の既定値
     */
    public fun show(message: String, durationMs: Int? = null, placement: DialogPlacement? = null)

    /**
     * 登録済みのカスタム Toast View を表示する (core/ADR-0029 レジストリ経路)。
     *
     * 未登録の ViewModel 型は構成ミスとして呼び出し時点で失敗し
     * ([DialogException.ViewFactoryNotRegistered])、表示は行われない。
     *
     * @param viewModel 表示するカスタム Toast の ViewModel
     * @param durationMs 表示するミリ秒。null なら [ToastStyle] の既定 duration
     * @param placement 配置。null なら View への添付、添付もなければ style・契約の既定値
     */
    public fun show(
        viewModel: ToastViewModel,
        durationMs: Int? = null,
        placement: DialogPlacement? = null,
    )

    /**
     * 登録せずに、その場で渡した factory の中身をカスタム Toast として表示する (core/ADR-0013)。
     *
     * factory の形も duration・配置の意味も登録経路とまったく同じである。
     * **レジストリの状態は一切変わらない** — 同じ ViewModel 型の登録があってもそれは使われず、
     * 登録内容もこの呼び出しの前後で変わらない。
     *
     * @param viewModel 表示するカスタム Toast の ViewModel
     * @param durationMs 表示するミリ秒。null なら [ToastStyle] の既定 duration
     * @param placement 配置。null なら View への添付、添付もなければ style・契約の既定値
     * @param factory 中身の View を生成する関数。レシーバは提示先画面の [Context]
     */
    public fun <VM : ToastViewModel> show(
        viewModel: VM,
        durationMs: Int? = null,
        placement: DialogPlacement? = null,
        factory: Context.(VM) -> View,
    )

    /**
     * ViewModel の**型**を渡して、登録済みのカスタム Toast View を表示する。
     *
     * ViewModel はレジストリに登録された ViewModel factory
     * ([ToastViewRegistry.registerViewModel]) が作る。
     * 実行順序は「ViewModel 生成 → configure の完了 → 中身の生成 → 表示」で固定されており、
     * configure が設定した状態は中身の初期化から必ず読める。
     * 生成と configure は UI スレッドで、受理順に実行される。
     *
     * ViewModel factory が未登録の場合は構成ミスとして呼び出し時点で失敗し、表示は行われない。
     * 生成・configure が失敗した場合は、show が既に戻っているため呼び出し元へは返せない —
     * 警告を記録に残してその表示1枚だけを破棄し、他の表示と後続の show には影響しない。
     * duration・配置・多重表示の意味はインスタンス渡しの [show] と同じである。
     *
     * @param viewModelClass 表示するカスタム Toast の ViewModel のクラス参照
     * @param durationMs 表示するミリ秒。null なら [ToastStyle] の既定 duration
     * @param placement 配置。null なら View への添付、添付もなければ style・契約の既定値
     * @param configure 生成した ViewModel の状態を整える関数
     */
    public fun <VM : ToastViewModel> show(
        viewModelClass: KClass<VM>,
        durationMs: Int? = null,
        placement: DialogPlacement? = null,
        configure: ((VM) -> Unit)? = null,
    )
}
