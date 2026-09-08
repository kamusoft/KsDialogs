package jp.kamusoft.ksdialogs

/**
 * カスタム Toast の ViewModel が準拠する契約。
 *
 * Toast は結果を返さず進捗も持たない表示なので、[DialogViewModel] と違い結果型の宣言を持たず、
 * [LoadingViewModel] と違い進捗の受け口も持たない。データの運搬体とレジストリの型キーを兼ねる。
 * レジストリのキーはインスタンスではなく型 (クラス参照) で引くため、Dialog / Loading と同じく
 * 参照型 (class) 限定とする。Kotlin ではこの制限を型システムで表現できないので、
 * 登録の時点と表示の時点で value class を拒否する (core/ADR-0018)。
 *
 * 同じ型を Dialog / Loading / Toast のどれで使ってもよい (レジストリは互いに独立しており、
 * 片方の登録が他方に影響しない)。
 */
public interface ToastViewModel
