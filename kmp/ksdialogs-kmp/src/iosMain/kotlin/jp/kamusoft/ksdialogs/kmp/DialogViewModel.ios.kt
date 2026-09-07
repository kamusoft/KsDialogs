package jp.kamusoft.ksdialogs.kmp

/**
 * iOS では ViewModel 契約は共有コード側の型のままになる。
 *
 * Native ライブラリの互換面は ViewModel を型消去して受け取るため、契約の準拠を要求しない —
 * 登録キーになるのは共有コードで定義した ViewModel クラスの ObjC クラスそのもの (kmp/ADR-0002)。
 */
public actual interface DialogViewModel<R>
