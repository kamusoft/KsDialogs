package jp.kamusoft.ksdialogs.kmp

/**
 * show が結果 (completed / cancelled) を返せない構成エラー。
 *
 * 未登録の ViewModel 型・提示先の画面が無い・型消去輸送からの結果値の復元に失敗した・
 * 共有コードの型指定 show で VM factory が未登録か生成物の実行時クラスが登録キーと違う、のいずれか。
 * いずれも利用者の操作結果ではなくプログラミングエラーなので、cancelled に化けさせずに投げる。
 * この例外が投げられた場合、View は生成も表示もされない。
 *
 * 内訳を型で区別する手段は持たない。各 OS の Native ライブラリで起きた失敗はその説明文が素通しで
 * [message] として届き、共有コードの型指定 show の失敗は共有コードが説明文を組み立てる。
 */
public class DialogException internal constructor(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
