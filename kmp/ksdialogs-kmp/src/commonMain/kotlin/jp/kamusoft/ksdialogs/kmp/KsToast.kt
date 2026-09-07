package jp.kamusoft.ksdialogs.kmp

import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.HiddenFromObjC
import kotlin.reflect.KClass

/**
 * 共有コードから使う Toast 表示の契約。
 *
 * 既定 singleton エントリ ([Toast.instance]) と DI 注入のどちらからでも同じ契約で呼び出せる。
 * View factory の紐付けと一括設定、表示中の重なりの管理は各 OS の Native ライブラリが持ち、
 * 共有コードからの表示と純 Kotlin / 純 Swift 利用者の表示は同じ重なりの管理に載る。
 * ViewModel factory の登録と、型を渡す表示の前段 (ViewModel の生成と configure) は
 * この共有コード側が担う。テストでは Native 実装なしにこの interface を差し替えられる。
 *
 * Toast は fire-and-forget の表示である。show は戻り値を持たず、
 * 表示の終了を待つ手段も、閉じる・書き換える手段も契約に無い。消滅の契機は duration の経過だけで、
 * 表示中はいかなる入力も奪わない。
 *
 * すべての呼び出しは任意のスレッドから行え、各 OS 側で受理順に直列化される。
 *
 * 見た目の設定 (色・フォントを含むスタイル) とアプリ既定配置はこの面に無い。
 * 色が共有コードの境界を渡らないため、それらは各 OS の Native 側の設定プロパティで行う
 * (静的メタ属性を共有コードから供給しないのと同じ非対称)。
 * カスタム Toast の View factory の登録も同じ理由で各 OS 側にある — 共有コードから登録できるのは
 * ViewModel の作り方だけである。
 */
public interface KsToast {
    /** カスタム Toast の ViewModel 型と作り方の紐付け。全ての入口が同じレジストリを共有する。 */
    public val registry: ToastViewRegistry

    /**
     * デフォルト View でメッセージを表示する。
     *
     * 表示は受理された時点から数え、[durationMs] の経過で自動的に消える。
     * メッセージの内容は制限しない — 空文字は内容が空のまま表示され、長文は複数行に折り返す。
     *
     * @param message 表示する文言
     * @param durationMs 表示するミリ秒。null なら各 OS 側で設定されたスタイルの既定 duration。
     *   0 以下は既定へ丸める
     * @param placement 配置。null なら各 OS 側のスタイルのアプリ既定配置、
     *   それも無ければ契約の既定値 (core/ADR-0015)
     */
    public fun show(message: String, durationMs: Int? = null, placement: DialogPlacement? = null)

    /**
     * 登録済みのカスタム Toast View を表示する (core/ADR-0029 レジストリ経路)。
     *
     * 中身の View の登録は各 OS 側で行う — Android は Kotlin ライブラリのレジストリ、
     * iOS は Swift ライブラリの KMP 面が受け口になる (kmp/ADR-0002・0003)。
     * 未登録の ViewModel 型は構成ミスとして [DialogException] になり、表示は行われない。
     * Swift から呼ぶ場合、この例外は NSError として届く。
     *
     * @param viewModel 表示するカスタム Toast の ViewModel
     * @param durationMs 表示するミリ秒。null なら各 OS 側で設定されたスタイルの既定 duration
     * @param placement 配置。null なら View への添付、添付もなければスタイル・契約の既定値
     */
    @Throws(DialogException::class)
    public fun show(
        viewModel: ToastViewModel,
        durationMs: Int? = null,
        placement: DialogPlacement? = null,
    )

    /**
     * ViewModel の型を渡してカスタム Toast を表示する。
     *
     * ViewModel は [registry] に登録した ViewModel factory で作られ、[configure] の完了後に表示へ進む。
     * 生成と [configure] は呼び出し元のスレッドでそのまま実行される。
     * duration と置き場所の扱いは ViewModel を渡す表示と同じ。
     *
     * ViewModel factory が未登録のとき、および factory が登録キーと違うクラスを返したときは、
     * 構成エラーとして [DialogException] を投げて表示を行わない。
     * ViewModel factory と [configure] が投げた例外は表示へ進まず、その場で呼び出し元へ伝わる。
     *
     * この呼び出しは共有 Kotlin コード専用で、Swift / Objective-C からは見えない。
     *
     * @param viewModelClass 表示する ViewModel のクラス参照。ViewModel factory の登録キーと同じもの
     * @param durationMs 表示するミリ秒。省略時の扱いは ViewModel を渡す表示と同じ
     * @param placement 配置。省略時の扱いは ViewModel を渡す表示と同じ
     * @param configure 生成した ViewModel を表示前に整える処理。省略すれば生成物をそのまま表示する
     */
    @OptIn(ExperimentalObjCRefinement::class)
    @HiddenFromObjC
    public fun <VM : ToastViewModel> show(
        viewModelClass: KClass<VM>,
        durationMs: Int? = null,
        placement: DialogPlacement? = null,
        configure: ((VM) -> Unit)? = null,
    )
}
