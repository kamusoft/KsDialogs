package jp.kamusoft.ksdialogs.kmp.support

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * show を始め、失敗も完了もせずに提示先を待っていることを確かめてから、そのコルーチンを打ち切る。
 *
 * このテスト置き場には提示先の画面が無いため、登録済みの ViewModel の show は失敗せずに待ち続ける
 * (core/ADR-0041)。一方、未登録の ViewModel は待たずにその場で失敗する。
 * この違いを、View factory の解決に成功した印として使う。
 *
 * 呼び出し前に `Dispatchers.setMain` でテストのスケジューラに乗る Main を差し込んでおくこと。
 *
 * @param whileWaiting 待っていることを確かめたあと、打ち切る前に行う観察
 * @param show 待たせたい show の呼び出し
 * @return show の呼び出し元へ伝播した [CancellationException]
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun TestScope.cancelWhileWaitingForHost(
    whileWaiting: () -> Unit = {},
    show: suspend () -> Unit,
): CancellationException {
    var returned = false
    var thrown: Throwable? = null
    val showing = launch {
        try {
            show()
            returned = true
        } catch (failure: Throwable) {
            // 構成エラーをテストスコープの失敗にせず、何が伝播したかを判定に使う
            thrown = failure
        }
    }
    runCurrent()

    assertNull(thrown, "登録済みの ViewModel の show が待たずに失敗しました: $thrown")
    assertFalse(returned, "提示先が無いのに show が結果を返しました。")
    assertTrue(showing.isActive, "show が提示先を待っていません。")
    whileWaiting()

    showing.cancel()
    runCurrent()

    assertTrue(showing.isCompleted, "打ち切ったあとも show が終わりません。")
    assertFalse(returned, "打ち切った show が結果を返しました。")
    return assertIs<CancellationException>(
        thrown,
        "打ち切った show の呼び出し元へキャンセルの通知が伝播しませんでした: $thrown",
    )
}
