package jp.kamusoft.ksdialogs.support

import jp.kamusoft.ksdialogs.DialogViewModel
import jp.kamusoft.ksdialogs.SimpleDialogViewModel

/** メッセージ1個を持ち、真偽値の結果を宣言する最小の ViewModel。 */
internal class BasicTestDialogViewModel(val message: String) : DialogViewModel<Boolean>

/** View factory を登録しない ViewModel。未登録の経路を確認するために使う。 */
internal class UnregisteredTestDialogViewModel : DialogViewModel<Boolean>

/**
 * 既定のレジストリを共有する経路の確認に使う ViewModel。
 * 他のテストと干渉しないよう、この型はレジストリ共有のテストからのみ登録する。
 */
internal class SharedRegistryTestDialogViewModel : DialogViewModel<Boolean>

/** 真偽値の顔で宣言した ViewModel。結果型を書かない宣言がそのまま通ることの確認に使う。 */
internal class SimpleFacedTestDialogViewModel(val message: String) : SimpleDialogViewModel

/** configure から状態を設定できる ViewModel。型指定 show の検証に使う。 */
internal class ConfigurableTestDialogViewModel(var message: String = "既定") : SimpleDialogViewModel

/**
 * 同じラベルを持つ別インスタンスどうしが等価になる ViewModel。
 * 結果報告口の紐付けが等価比較ではなくインスタンスの同一性で行われることを確かめるために使う。
 */
internal data class EquatableTestDialogViewModel(val label: String) : SimpleDialogViewModel

/**
 * value class として宣言した ViewModel。
 * 参照型限定の強制が登録・型指定 show の時点で働くことを確かめるために使う。
 */
@JvmInline
internal value class ValueClassTestDialogViewModel(val label: String) : SimpleDialogViewModel
