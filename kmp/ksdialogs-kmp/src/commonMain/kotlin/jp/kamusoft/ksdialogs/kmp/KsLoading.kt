package jp.kamusoft.ksdialogs.kmp

import kotlin.coroutines.cancellation.CancellationException
import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.HiddenFromObjC
import kotlin.reflect.KClass

/**
 * 共有コードから使うローディング表示の契約。
 *
 * 既定 singleton エントリ ([Loading.instance]) と DI 注入のどちらからでも同じ契約で呼び出せる。
 * どちらの入口から呼んでも表示は 1 プロセスに 1 つで、合流状態を共有する — 表示状態の実体は
 * 各 OS の Native ライブラリが持つ。ViewModel factory の登録と、型を渡す表示の前段
 * (ViewModel の生成と configure) はこの共有コード側が担い、中身の View の作り方は Native 側にある。
 * テストでは Native 実装なしにこの interface を差し替えられる。
 *
 * すべての呼び出しは任意のスレッドから行える。
 *
 * 見た目の設定 (色・フォントを含むスタイル) と既定ローディングの器メタ属性はこの面に無い。
 * 色が共有コードの境界を渡らないため、それらは各 OS の Native 側の設定プロパティで行う
 * (静的メタ属性を共有コードから供給しないのと同じ非対称)。
 */
public interface KsLoading {
    /** カスタム Loading の ViewModel 型と作り方の紐付け。全ての入口が同じレジストリを共有する。 */
    public val registry: LoadingViewRegistry

    /**
     * 既定ローディングを表示し、合流1件を開始する。
     *
     * 対応する終了は [hide] だけである (合流数によらず即閉じる)。
     * 戻るのは操作ブロックが有効になった時点で、入りの演出の完了は待たない。
     * 既に表示中なら1つの表示に合流し、コンテンツは最初の開始のものが維持される (core/ADR-0024)。
     *
     * @param message 表示するメッセージ。null なら各 OS 側で設定されたスタイルの既定メッセージ
     * @param placement 配置。null なら契約の既定値 (core/ADR-0015)
     */
    public suspend fun show(message: String? = null, placement: DialogPlacement? = null)

    /**
     * 登録済みのカスタム Loading View を表示し、合流1件を開始する。
     *
     * 中身の View の登録は各 OS 側で行う — Android は Kotlin ライブラリのレジストリ、
     * iOS は Swift ライブラリの KMP 面が受け口になる (kmp/ADR-0002・0003)。
     * 未登録の ViewModel 型は構成ミスとして [DialogException] になり、表示は行われない。
     * Swift から呼ぶ場合、この例外は NSError として届く。
     *
     * @param viewModel 表示するカスタム Loading の ViewModel
     * @param placement 配置。null なら View への添付、添付もなければ契約の既定値
     */
    @Throws(DialogException::class, CancellationException::class)
    public suspend fun show(viewModel: LoadingViewModel, placement: DialogPlacement? = null)

    /**
     * ViewModel の型を渡してカスタム Loading を表示し、合流1件を開始する。
     *
     * ViewModel は [registry] に登録した ViewModel factory で作られ、[configure] の完了後に表示へ進む。
     * 生成と [configure] は呼び出し元の文脈 (呼び出し元のコルーチン文脈) でそのまま実行される。
     * 合流と置き場所の扱いは ViewModel を渡す表示と同じ。
     *
     * ViewModel factory が未登録のとき、および factory が登録キーと違うクラスを返したときは、
     * 構成エラーとして [DialogException] を投げて表示を行わない。
     * ViewModel factory と [configure] が投げた例外は表示へ進まずそのまま呼び出し元へ伝わる。
     *
     * この呼び出しは共有 Kotlin コード専用で、Swift / Objective-C からは見えない。
     *
     * @param viewModelClass 表示する ViewModel のクラス参照。ViewModel factory の登録キーと同じもの
     * @param placement 配置。省略時の扱いは ViewModel を渡す表示と同じ
     * @param configure 生成した ViewModel を表示前に整える処理。省略すれば生成物をそのまま表示する
     */
    @OptIn(ExperimentalObjCRefinement::class)
    @HiddenFromObjC
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
     * @param message 表示するメッセージ。null なら各 OS 側で設定されたスタイルの既定メッセージ
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
     * 未登録の ViewModel 型は構成ミスとして [DialogException] で失敗し、処理は実行されない (fail-fast)。
     * Swift から呼ぶ場合、この例外は NSError として届く。
     * 報告した進捗は、ViewModel が [LoadingProgressReceiver] を実装していればそこへ転送される。
     *
     * @param viewModel 表示するカスタム Loading の ViewModel
     * @param placement 配置。null なら View への添付、添付もなければ契約の既定値
     * @param action 実行する処理。引数の報告口へ 0〜1 の進捗を報告できる (任意スレッド可)
     */
    @Throws(DialogException::class, CancellationException::class)
    public suspend fun <T> start(
        viewModel: LoadingViewModel,
        placement: DialogPlacement? = null,
        action: suspend ((Double) -> Unit) -> T,
    ): T

    /**
     * ViewModel の型を渡してカスタム Loading を表示したまま処理を実行し、その戻り値を返す。
     *
     * ViewModel は [registry] に登録した ViewModel factory で作られ、[configure] の完了後に表示へ進む。
     * 生成と [configure] は呼び出し元の文脈 (呼び出し元のコルーチン文脈) でそのまま実行される。
     * 合流の数え方・進捗の転送・戻り値の扱いは ViewModel を渡すスコープ形と同じ。
     *
     * ViewModel factory が未登録のとき、および factory が登録キーと違うクラスを返したときは、
     * 構成エラーとして [DialogException] で失敗し、処理は実行されない。
     * ViewModel factory と [configure] が投げた例外も同じく処理を実行せずに呼び出し元へ伝わる。
     *
     * この呼び出しは共有 Kotlin コード専用で、Swift / Objective-C からは見えない。
     *
     * @param viewModelClass 表示する ViewModel のクラス参照。ViewModel factory の登録キーと同じもの
     * @param placement 配置。省略時の扱いは ViewModel を渡すスコープ形と同じ
     * @param configure 生成した ViewModel を表示前に整える処理。省略すれば生成物をそのまま表示する
     * @param action 実行する処理。引数の報告口へ 0〜1 の進捗を報告できる (任意スレッド可)
     */
    @OptIn(ExperimentalObjCRefinement::class)
    @HiddenFromObjC
    public suspend fun <VM : LoadingViewModel, T> start(
        viewModelClass: KClass<VM>,
        placement: DialogPlacement? = null,
        configure: (suspend (VM) -> Unit)? = null,
        action: suspend ((Double) -> Unit) -> T,
    ): T
}
