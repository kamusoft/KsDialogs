package jp.kamusoft.ksdialogs.kmp

/**
 * Android では進捗の受け口も Native ライブラリのものと同一の型になる。
 *
 * 共有コードで定義した ViewModel が実装した受け口へ、Native ライブラリが追加の写し替えなしに
 * 進捗を転送できるようにするため、別の型を挟まず同じ面を指す (kmp/ADR-0002)。
 */
public actual typealias LoadingProgressReceiver = jp.kamusoft.ksdialogs.LoadingProgressReceiver
