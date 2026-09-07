package jp.kamusoft.ksdialogs.kmp.support

import jp.kamusoft.ksdialogs.kmp.DialogViewModel
import jp.kamusoft.ksdialogs.kmp.LoadingViewModel
import jp.kamusoft.ksdialogs.kmp.ToastViewModel

/**
 * 型を渡す表示で使う検証用のダイアログ ViewModel。
 *
 * 生成後に configure で状態を入れる形を見るため、文言を書き換えられるようにしてある。
 */
internal open class ConfigurableTestDialogViewModel(
    /** ダイアログに出す文言。 */
    var message: String = "既定",
    /**
     * この ViewModel を作った登録の世代。
     *
     * 並行して再登録する検証で、生成物が 1 つの登録単位に対応する (別々の登録の値が混ざらない) ことを
     * 文言と突き合わせて確かめるために持つ。
     */
    val generation: Int = 0,
) : DialogViewModel<Boolean>

/** 登録キーのサブクラス。factory がキーと違うクラスを返す場合を作るために使う。 */
internal class DerivedTestDialogViewModel : ConfigurableTestDialogViewModel()

/** 型を渡す表示で使う検証用のカスタム Loading の ViewModel。 */
internal open class ConfigurableTestLoadingViewModel(
    /** ローディングに出す文言。 */
    var message: String = "既定",
) : LoadingViewModel

/** 登録キーのサブクラス。factory がキーと違うクラスを返す場合を作るために使う。 */
internal class DerivedTestLoadingViewModel : ConfigurableTestLoadingViewModel()

/** 型を渡す表示で使う検証用のカスタム Toast の ViewModel。 */
internal open class ConfigurableTestToastViewModel(
    /** Toast に出す文言。 */
    var message: String = "既定",
) : ToastViewModel

/** 登録キーのサブクラス。factory がキーと違うクラスを返す場合を作るために使う。 */
internal class DerivedTestToastViewModel : ConfigurableTestToastViewModel()
