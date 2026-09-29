package jp.kamusoft.ksdialogs.maui

/**
 * 提示1回ごとに1度だけ届く閉鎖の通知先。
 *
 * 呼ばれるのは3つのうちちょうど1つで、いずれも UI スレッドから呼ばれる。
 */
public interface MauiDialogClosureListener {
    /** 利用者の操作 (キャンセル・外側タップ・戻るボタン) か、呼び出し側の打ち切りで閉じた。 */
    public fun onCancelled()

    /** 呼び出し側からの閉鎖要求で閉じた。中身を作る前に閉じた場合も含む。 */
    public fun onDismissed()

    /**
     * それ以外の理由で提示できなかった。
     *
     * @param message 失敗の説明
     */
    public fun onFailed(message: String?)
}
