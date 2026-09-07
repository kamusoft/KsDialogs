namespace KsDialogs;

/// <summary>
/// カスタム Loading の ViewModel が準拠する契約。
/// </summary>
/// <remarks>
/// Loading は結果を返さないライフサイクルなので、<see cref="IDialogViewModel{TResult}"/> と違い
/// 結果型の宣言を持たない。レジストリのキーはインスタンスではなく型で引くため、Dialog と同じく
/// 参照型 (class) 限定とする (core/ADR-0018)。登録と 1 行登録糖衣は <c>class</c> 制約で
/// コンパイル時に拒否し、interface で受ける表示の入口では実行時に構成ミスとして弾く。
/// <para>
/// 同じ型を Dialog と Loading の両方で使いたい場合は、両方の契約に準拠させる
/// (レジストリは互いに独立しており、片方の登録が他方に影響しない)。
/// </para>
/// </remarks>
public interface ILoadingViewModel
{
}
