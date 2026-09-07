using System;
using Microsoft.Maui.Hosting;

namespace KsDialogs;

/// <summary>
/// アプリ起動時に <see cref="IServiceProvider"/> をライブラリへ預ける初期化サービス。
/// </summary>
/// <remarks>
/// 登録は builder の組み立て時、解決は show の時点という時間差を、MAUI 標準の初期化機構で埋める。
/// </remarks>
internal sealed class KsDialogsInitializer : IMauiInitializeService
{
    /// <inheritdoc/>
    public void Initialize(IServiceProvider services) => DialogServiceProvider.Current = services;
}
