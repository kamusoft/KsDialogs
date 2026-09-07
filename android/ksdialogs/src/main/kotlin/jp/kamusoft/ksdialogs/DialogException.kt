package jp.kamusoft.ksdialogs

// 各サブクラスの message (診断文言) は英語固定でローカライズしない (cross/ADR-0015)。
/**
 * show が結果 (completed / cancelled) を返せない構成エラー。
 *
 * 利用者の操作結果ではなくプログラミングエラーであり、cancelled に化けさせずに投げる。
 * この例外が投げられた場合、View は生成も表示もされない。
 */
public sealed class DialogException(message: String) : RuntimeException(message) {
    /**
     * ViewModel 型に対する View factory がレジストリに登録されていない。
     *
     * @property viewModelTypeName 解決できなかった ViewModel の型名
     */
    public class ViewFactoryNotRegistered internal constructor(
        public val viewModelTypeName: String,
    ) : DialogException("No View factory is registered for ViewModel type $viewModelTypeName.")

    /**
     * ViewModel 型に対する ViewModel factory がレジストリに登録されていない (型指定 show の構成ミス)。
     *
     * @property viewModelTypeName 解決できなかった ViewModel の型名
     */
    public class ViewModelFactoryNotRegistered internal constructor(
        public val viewModelTypeName: String,
    ) : DialogException("No ViewModel factory is registered for ViewModel type $viewModelTypeName.")

    /**
     * 表示中の ViewModel インスタンスを重ねて show しようとした。
     *
     * 結果報告口はインスタンスに1つだけ紐付くため、同じインスタンスの並行表示は成立しない。
     * 先に表示されているダイアログはこの失敗の影響を受けない。
     *
     * @property viewModelTypeName 重ねて show しようとした ViewModel の型名
     */
    public class ViewModelAlreadyShowing internal constructor(
        public val viewModelTypeName: String,
    ) : DialogException("This ViewModel instance of type $viewModelTypeName is already being shown.")

    /**
     * value class を ViewModel として登録または表示しようとした。
     *
     * 結果報告口はインスタンスの同一性で紐付くため、boxing のたびに別インスタンスになる
     * value class は ViewModel にできない (core/ADR-0018)。
     *
     * @property viewModelTypeName 拒否された ViewModel の型名
     */
    public class ValueClassViewModel internal constructor(
        public val viewModelTypeName: String,
    ) : DialogException(
        "ViewModel type $viewModelTypeName is a value class and cannot be used as a ViewModel.",
    )

    /** アクティブな提示先の画面 (resumed な Activity) が存在しない。キューイングはせず即座に失敗する。 */
    public class PresentationHostUnavailable internal constructor() :
        DialogException("No screen is available to present the Dialog.")
}
