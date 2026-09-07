package jp.kamusoft.ksdialogs.kmp

/**
 * カスタム Loading の ViewModel が準拠する契約。
 *
 * Loading は結果を返さないライフサイクルなので、[DialogViewModel] と違い結果型の宣言を持たない。
 * レジストリのキーはインスタンスではなく型の同一性で引くため、Dialog と同じく参照型 (class) 限定とする。
 *
 * 共有コードで定義した ViewModel のクラスがそのままレジストリのキーになるため、
 * Android では Native ライブラリの ViewModel 契約と同一の型として振る舞う (kmp/ADR-0002)。
 */
public expect interface LoadingViewModel
