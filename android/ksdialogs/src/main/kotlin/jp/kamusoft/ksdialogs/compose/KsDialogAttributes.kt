package jp.kamusoft.ksdialogs.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import jp.kamusoft.ksdialogs.DialogOptions
import jp.kamusoft.ksdialogs.DialogPlacement
import jp.kamusoft.ksdialogs.DialogTransition

/**
 * この composable をダイアログとして表示するときのメタ属性を添付する (core/ADR-0015)。
 *
 * 中身の composable の**冒頭**で宣言する。器は初回のネイティブレイアウトパス完了時点の値を
 * 実効値として採用するため、表示が始まった後に状態変化で宣言値が変わっても、
 * そのダイアログの配置・覆いの色・外側タップの扱いは追随しない。
 *
 * 引数を省略した面は供給なしとして扱い、show の引数 (置き場所) か契約の既定値が使われる。
 * 優先順は「show 引数 > コンテンツ添付 > 契約既定値」で、従来 View 系の添付と同一である。
 *
 * **初回の組み立てで実行されない場所に書くと効かない。** `LazyColumn` / `LazyRow` のように
 * 必要になるまで評価を遅らせるスコープの中に書いた宣言は、初回の組み立てで実行されないため
 * 初回表示に反映されない。必ず初回の組み立てで通る位置に書くこと。
 *
 * ダイアログの中身以外で呼び出した場合は何も起こらない。
 *
 * @param options 大きさ・基準領域・覆い・外側タップの扱いといった静的メタ属性
 * @param placement 置き場所 (整列と移動量)
 * @param transition 出入りの演出 (core/ADR-0017)
 */
@Composable
public fun KsDialogAttributes(
    options: DialogOptions? = null,
    placement: DialogPlacement? = null,
    transition: DialogTransition? = null,
) {
    val collector = LocalDialogAttributeCollector.current ?: return
    // SideEffect は組み立てが確定した後に走るため、投機的に組み立てられた値を拾わない
    SideEffect {
        collector.supply(options, placement, transition)
    }
}
