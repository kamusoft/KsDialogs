package jp.kamusoft.ksdialogs.kmp

/**
 * Android では ViewModel 契約は Native ライブラリのものと同一の型になる。
 *
 * 共有コードで定義した ViewModel のクラスをそのまま Native レジストリの登録キーに使えるようにするため、
 * 別の型を挟まず同じ契約を指す (kmp/ADR-0002)。
 */
public actual typealias LoadingViewModel = jp.kamusoft.ksdialogs.LoadingViewModel
