package jp.kamusoft.ksdialogs.compose.support

import jp.kamusoft.ksdialogs.DialogViewModel
import jp.kamusoft.ksdialogs.SimpleDialogViewModel

/** 宣言的 UI の中身を登録して表示する検証に使う ViewModel。結果は真偽値。 */
internal class ComposeContentTestDialogViewModel(val message: String = "こんにちは") : SimpleDialogViewModel

/** カスタム結果型の導出を確かめるための ViewModel。 */
internal class TypedComposeTestDialogViewModel : DialogViewModel<String>

/** 宣言的 UI と従来 View 系の外形をつき合わせる検証で、宣言的 UI 側に使う ViewModel。 */
internal class ComposeLayoutTestDialogViewModel : SimpleDialogViewModel

/** 同じ検証で、従来 View 系側に使う ViewModel。 */
internal class ViewLayoutTestDialogViewModel : SimpleDialogViewModel

/** 添付 DSL の検証に使う ViewModel。 */
internal class ComposeAttributeTestDialogViewModel : SimpleDialogViewModel

/** 登録経路と混ざらないよう、インライン表示の検証だけに使う ViewModel。 */
internal class ComposeInlineTestDialogViewModel : SimpleDialogViewModel

/** ViewModel 主導の呼び出し (VM 経由の報告口・型指定 show) の検証に使う ViewModel。 */
internal class ComposeModelBindingTestDialogViewModel(var message: String = "既定") : SimpleDialogViewModel

/** 出入りの演出の添付の検証に使う ViewModel。 */
internal class ComposeTransitionTestDialogViewModel : SimpleDialogViewModel
