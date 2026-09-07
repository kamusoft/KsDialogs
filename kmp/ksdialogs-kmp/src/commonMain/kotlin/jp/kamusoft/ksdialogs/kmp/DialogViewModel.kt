package jp.kamusoft.ksdialogs.kmp

/**
 * ダイアログの ViewModel が準拠する契約。
 *
 * 結果型は呼び出し側ではなく ViewModel 自身が [R] として宣言し、
 * show の戻り値と結果報告部品の型はここから導出される。
 * 宣言と異なる結果型で受け取ったり報告したりする書き方はコンパイルできない (core/ADR-0003)。
 *
 * 共有コードで定義した ViewModel のクラスがそのままレジストリのキーになるため、
 * Android では Native ライブラリの ViewModel 契約と同一の型として振る舞う (kmp/ADR-0002)。
 *
 * @param R この ViewModel が宣言する結果値の型
 */
public expect interface DialogViewModel<R>
