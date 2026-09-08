package jp.kamusoft.ksdialogs

/**
 * カスタム Loading の ViewModel が準拠する契約。
 *
 * Loading は結果を返さないライフサイクルなので、[DialogViewModel] と違い結果型の宣言を持たない。
 * レジストリのキーはインスタンスではなく型 (クラス参照) で引くため、Dialog と同じく参照型 (class) 限定とする。
 * Kotlin ではこの制限を型システムで表現できないので、登録の時点と表示の時点で value class を拒否する
 * (core/ADR-0018)。
 *
 * 同じ型を Dialog と Loading の両方で使いたい場合は、両方の契約に準拠させる
 * (レジストリは互いに独立しており、片方の登録が他方に影響しない)。
 */
public interface LoadingViewModel
