package jp.kamusoft.ksdialogs

import android.content.Context
import android.view.View
import kotlin.reflect.KClass

/**
 * ローディング表示の契約。
 *
 * 既定 singleton エントリ ([Loading.instance]) と DI 注入のどちらからでも同じ契約で呼び出せる
 * (core/ADR-0002)。どちらの入口から呼んでも表示は 1 プロセスに 1 つで、合流状態を共有する
 * (core/ADR-0024)。
 *
 * すべての呼び出しは任意のスレッドから行え、内部で UI スレッドへ移して受理順に直列化される。
 */
public interface KsLoading {
    /** カスタム Loading の ViewModel 型と View factory の紐付け。Dialog のレジストリとは独立している。 */
    public val registry: LoadingViewRegistry

    /** 既定ローディングの見た目の設定 (core/ADR-0023)。各表示の開始時に読まれる。 */
    public var style: LoadingStyle

    /**
     * 既定ローディングの器メタ属性。
     *
     * 利用者が属性を添付する View を持たない既定ローディングでは、これがコンテンツへの添付の代わりになる。
     * 各表示の開始時に読まれる。`isCanceledOnTouchOutside` は Loading では常に無効で、
     * 設定しても効かない (core/ADR-0022)。
     */
    public var options: DialogOptions

    /**
     * 既定ローディングを表示し、合流1件を開始する。
     *
     * 対応する終了は [hide] だけである (合流数によらず即閉じる)。
     * 戻るのは操作ブロックが有効になった時点で、入りの演出の完了は待たない。
     * 既に表示中なら1つの表示に合流し、コンテンツは最初の開始のものが維持される (core/ADR-0024)。
     *
     * @param message 表示するメッセージ。null ならスタイルの既定メッセージ
     * @param placement 配置。null なら契約の既定値 (core/ADR-0015)
     */
    public suspend fun show(message: String? = null, placement: DialogPlacement? = null)

    /**
     * 登録済みのカスタム Loading View を表示し、合流1件を開始する。
     *
     * 未登録の ViewModel 型は構成ミスとして失敗し ([DialogException.ViewFactoryNotRegistered])、
     * 表示は行われない。
     *
     * @param viewModel 表示するカスタム Loading の ViewModel
     * @param placement 配置。null なら View への添付、添付もなければ契約の既定値
     */
    public suspend fun show(viewModel: LoadingViewModel, placement: DialogPlacement? = null)

    /**
     * 登録せずに、その場で渡した factory の中身をカスタム Loading として表示する (core/ADR-0013)。
     *
     * factory の形も合流の数え方も登録経路とまったく同じで、[placement] の意味も変わらない。
     * **レジストリの状態は一切変わらない** — 同じ ViewModel 型の登録があってもそれは使われず、
     * 登録内容もこの呼び出しの前後で変わらない。
     *
     * @param viewModel 表示するカスタム Loading の ViewModel
     * @param placement 配置。null なら View への添付、添付もなければ契約の既定値
     * @param factory 中身の View を生成する関数。レシーバは提示先画面の [Context]
     */
    public suspend fun <VM : LoadingViewModel> show(
        viewModel: VM,
        placement: DialogPlacement? = null,
        factory: Context.(VM) -> View,
    )

    /**
     * ViewModel の**型**を渡して、登録済みのカスタム Loading View を表示する。
     *
     * ViewModel はレジストリに登録された ViewModel factory
     * ([LoadingViewRegistry.registerViewModel]) が作る。
     * 実行順序は「ViewModel 生成 → configure の完了 → 進捗の受け口の紐付け → 中身の生成 → 表示」で
     * 固定されており、configure が設定した状態は中身の初期化から必ず読める。
     * 生成と configure は UI スレッド (Main dispatcher) で実行される。
     *
     * ViewModel factory が未登録の場合と、生成・configure が失敗した場合 (キャンセルを含む) は、
     * 表示に進まずにその失敗が呼び出し元へ伝わり、合流1件としても数えない。
     * 合流・置き場所・進捗転送の意味はインスタンス渡しの [show] と同じである。
     *
     * @param viewModelClass 表示するカスタム Loading の ViewModel のクラス参照
     * @param placement 配置。null なら View への添付、添付もなければ契約の既定値
     * @param configure 生成した ViewModel の状態を整える関数。中断関数として書ける
     */
    public suspend fun <VM : LoadingViewModel> show(
        viewModelClass: KClass<VM>,
        placement: DialogPlacement? = null,
        configure: (suspend (VM) -> Unit)? = null,
    )

    /**
     * 表示を閉じる。合流数によらず即座に閉じ、走行中の処理には干渉しない (core/ADR-0024)。
     *
     * 出の演出と器の撤去が完了してから戻る。表示していなければ何も起こらない。
     */
    public suspend fun hide()

    /**
     * 表示中のメッセージを更新する。合流には関与しない (対応する終了は要らない)。
     *
     * 既定ローディングを表示している間だけ効き、非表示中とカスタム View 表示中は何も起こらない。
     */
    public suspend fun setMessage(message: String?)

    /**
     * 既定ローディングを表示したまま処理を実行し、その戻り値を返す。
     *
     * 合流1件の開始と終了が処理の開始・完了に対応する。処理は表示状態によらず必ず実行され、
     * 失敗 (例外・キャンセル) も合流1件の終了として数えたうえで呼び出し元へ伝播する。
     * 合流最後の1件なら器の撤去まで待ってから戻り、そうでなければ処理の完了時点で戻る。
     *
     * @param message 表示するメッセージ。null ならスタイルの既定メッセージ
     * @param placement 配置。null なら契約の既定値
     * @param action 実行する処理。引数の報告口へ 0〜1 の進捗を報告できる (任意スレッド可)
     */
    public suspend fun <T> start(
        message: String? = null,
        placement: DialogPlacement? = null,
        action: suspend ((Double) -> Unit) -> T,
    ): T

    /**
     * 登録済みのカスタム Loading View を表示したまま処理を実行し、その戻り値を返す。
     *
     * 未登録の ViewModel 型は構成ミスとして失敗し、処理は実行されない (fail-fast)。
     *
     * @param viewModel 表示するカスタム Loading の ViewModel
     * @param placement 配置。null なら View への添付、添付もなければ契約の既定値
     * @param action 実行する処理。引数の報告口へ 0〜1 の進捗を報告できる (任意スレッド可)
     */
    public suspend fun <T> start(
        viewModel: LoadingViewModel,
        placement: DialogPlacement? = null,
        action: suspend ((Double) -> Unit) -> T,
    ): T

    /**
     * 登録せずに、その場で渡した factory の中身を表示したまま処理を実行する (core/ADR-0013)。
     *
     * レジストリの状態は一切変わらない。それ以外は登録経路のスコープ形とまったく同じである。
     *
     * @param viewModel 表示するカスタム Loading の ViewModel
     * @param placement 配置。null なら View への添付、添付もなければ契約の既定値
     * @param factory 中身の View を生成する関数。レシーバは提示先画面の [Context]
     * @param action 実行する処理。引数の報告口へ 0〜1 の進捗を報告できる (任意スレッド可)
     */
    public suspend fun <VM : LoadingViewModel, T> start(
        viewModel: VM,
        placement: DialogPlacement? = null,
        factory: Context.(VM) -> View,
        action: suspend ((Double) -> Unit) -> T,
    ): T

    /**
     * ViewModel の**型**を渡して、登録済みのカスタム Loading View を表示したまま処理を実行する。
     *
     * ViewModel の生成・configure・失敗の扱いは型を渡す [show] と同じで、開始 → 処理の実行 →
     * 終了の対と戻り値の扱いはインスタンス渡しのスコープ形と同じである。
     * ViewModel factory が未登録の場合と、生成・configure が失敗した場合は処理を実行しない。
     *
     * @param viewModelClass 表示するカスタム Loading の ViewModel のクラス参照
     * @param placement 配置。null なら View への添付、添付もなければ契約の既定値
     * @param configure 生成した ViewModel の状態を整える関数。中断関数として書ける
     * @param action 実行する処理。引数の報告口へ 0〜1 の進捗を報告できる (任意スレッド可)
     */
    public suspend fun <VM : LoadingViewModel, T> start(
        viewModelClass: KClass<VM>,
        placement: DialogPlacement? = null,
        configure: (suspend (VM) -> Unit)? = null,
        action: suspend ((Double) -> Unit) -> T,
    ): T
}
