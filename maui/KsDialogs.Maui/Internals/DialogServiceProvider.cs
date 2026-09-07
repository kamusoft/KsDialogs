using System;

namespace KsDialogs;

/// <summary>
/// アプリの <see cref="IServiceProvider"/> をライブラリ内に保持するホルダ。
/// </summary>
/// <remarks>
/// 登録は builder の組み立て時、解決は show の時点という時間差があるため、DI チェーン上では
/// provider をクロージャに captured できない。<c>AddKsDialogs</c> / <c>RegisterForDialog</c> が
/// 登録する初期化サービスがアプリ起動時にここへ provider を預け、1 行登録と fallback resolver は
/// show の時点でここから引く。
/// <para>
/// 保持するのは最後に初期化されたアプリの provider 1 つで、複数のアプリの並存は対象外。
/// </para>
/// </remarks>
internal static class DialogServiceProvider
{
    /// <summary>いま保持している provider。未初期化なら <see langword="null"/>。</summary>
    public static IServiceProvider? Current { get; set; }

    /// <summary>保持している provider を返す。未初期化なら構成ミスとして失敗する。</summary>
    /// <returns>アプリの provider。</returns>
    public static IServiceProvider Require() =>
        Current ?? throw new DialogException.ServiceProviderUnavailable();
}
