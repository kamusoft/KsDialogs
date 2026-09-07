package jp.kamusoft.ksdialogs.apicheck

import jp.kamusoft.ksdialogs.DialogTransition
import kotlin.time.Duration.Companion.milliseconds

/**
 * none プリセットは引数を取らない (演出しないものに時間もイージングもない。core/ADR-0017)。
 *
 * このソースは `-Pksdialogs.negativeCheck.noneArguments` を付けたときだけビルドに加わり、
 * **コンパイルエラーで失敗すること**が期待結果になる。
 */
public object RejectsArgumentsOnNonePreset {
    public fun build(): DialogTransition = DialogTransition.none(200.milliseconds)
}
