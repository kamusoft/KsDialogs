namespace KsDialogs;

/// <summary>
/// カスタム Toast の ViewModel が準拠する契約。
/// </summary>
/// <remarks>
/// Toast は結果を返さず進捗も持たない表示なので、<see cref="IDialogViewModel{TResult}"/> と違い
/// 結果型の宣言を持たず、<see cref="ILoadingViewModel"/> と違い進捗の受け口も持たない。
/// データの運搬体とレジストリの型キーを兼ねる。レジストリのキーはインスタンスではなく型で引くため、
/// Dialog / Loading と同じく参照型 (class) 限定とする (core/ADR-0018)。
/// 登録と 1 行登録糖衣は <c>class</c> 制約でコンパイル時に拒否し、interface で受ける表示の入口では
/// 実行時に構成ミスとして弾く。
/// <para>
/// 同じ型を Dialog / Loading / Toast のどれで使ってもよい (レジストリは互いに独立しており、
/// 片方の登録が他方に影響しない)。
/// </para>
/// </remarks>
public interface IToastViewModel
{
}
