using System.Runtime.CompilerServices;
using System.Threading;

namespace KsDialogs;

/// <summary>
/// 表示中の ViewModel と結果チャネルを結ぶ紐付け表 (core/ADR-0018)。
/// </summary>
/// <remarks>
/// ViewModel の定義を空のまま保つため、結果報告口の置き場を ViewModel の外に持つ。
/// キーの照合は <see cref="ConditionalWeakTable{TKey, TValue}"/> によるインスタンスの同一性であり、
/// 等価比較 (<see cref="object.Equals(object)"/>) には依存しない —
/// 等価な別インスタンスは別の紐付けとして扱われる。
/// <para>
/// 紐付けは show が中身の View を生成する前に作り、show が終わる全経路で外す。
/// キーの保持は弱参照で、除去漏れが起きても ViewModel の解放とともに紐付けが消える安全網になる。
/// </para>
/// <para>登録・取得は任意のスレッドから行える。</para>
/// </remarks>
internal static class DialogNotifierBindings
{
    // ConditionalWeakTable 自体はスレッド安全だが、「未登録なら入れる」を不可分にするため自前で守る
    private static readonly Lock s_gate = new();
    private static readonly ConditionalWeakTable<object, DialogResultChannel> s_bindings = [];

    /// <summary>ViewModel に結果チャネルを紐付ける。</summary>
    /// <param name="viewModel">紐付ける ViewModel。</param>
    /// <param name="resultChannel">その show の結果チャネル。</param>
    /// <returns>紐付けられたら <see langword="true"/>。同じインスタンスが既に表示中なら <see langword="false"/>。</returns>
    public static bool Bind(object viewModel, DialogResultChannel resultChannel)
    {
        lock (s_gate)
        {
            if (s_bindings.TryGetValue(viewModel, out _))
            {
                return false;
            }

            s_bindings.Add(viewModel, resultChannel);
            return true;
        }
    }

    /// <summary>紐付けを外す。別の show が作った紐付けは外さない。</summary>
    /// <param name="viewModel">紐付けを外す ViewModel。</param>
    /// <param name="resultChannel">その show の結果チャネル。</param>
    public static void Unbind(object viewModel, DialogResultChannel resultChannel)
    {
        lock (s_gate)
        {
            if (s_bindings.TryGetValue(viewModel, out DialogResultChannel? bound)
                && ReferenceEquals(bound, resultChannel))
            {
                s_bindings.Remove(viewModel);
            }
        }
    }

    /// <summary>ViewModel に紐付いている結果チャネル。表示中でなければ <see langword="null"/>。</summary>
    /// <param name="viewModel">紐付けを引く ViewModel。</param>
    /// <returns>紐付いた結果チャネル。表示中でなければ <see langword="null"/>。</returns>
    public static DialogResultChannel? ResultChannel(object viewModel)
    {
        lock (s_gate)
        {
            return s_bindings.TryGetValue(viewModel, out DialogResultChannel? bound) ? bound : null;
        }
    }
}
