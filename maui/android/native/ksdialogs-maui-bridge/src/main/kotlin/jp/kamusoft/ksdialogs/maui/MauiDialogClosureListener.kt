package jp.kamusoft.ksdialogs.maui

/**
 * 提示1回ごとに1度だけ届く閉鎖の通知先。
 *
 * 呼ばれるのは4つのうちちょうど1つで、いずれも UI スレッドから呼ばれる。
 */
public interface MauiDialogClosureListener {
    /** 利用者の操作 (キャンセル・外側タップ・戻るボタン) で閉じた。 */
    public fun onCancelled()

    /** 呼び出し側からの閉鎖要求で閉じた。 */
    public fun onDismissed()

    /**
     * 提示できる画面が無く、提示に入れなかった。
     *
     * @param message 失敗の説明
     */
    public fun onPresentationHostUnavailable(message: String?)

    /**
     * それ以外の理由で提示できなかった。
     *
     * @param message 失敗の説明
     */
    public fun onFailed(message: String?)
}
