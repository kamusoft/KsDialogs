package jp.kamusoft.ksdialogs.kmp

/**
 * カスタム Toast の ViewModel が準拠する契約。
 *
 * Toast は結果を返さず進捗も持たない表示なので、[DialogViewModel] と違い結果型の宣言を持たず、
 * [LoadingViewModel] と違い進捗の受け口も持たない。データの運搬体とレジストリの型キーを兼ねる。
 * レジストリのキーはインスタンスではなく型の同一性で引くため、Dialog / Loading と同じく
 * 参照型 (class) 限定とする。
 *
 * 共有コードで定義した ViewModel のクラスがそのままレジストリのキーになるため、
 * Android では Native ライブラリの ViewModel 契約と同一の型として振る舞う (kmp/ADR-0002)。
 */
public expect interface ToastViewModel
