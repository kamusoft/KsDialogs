using System;
using Microsoft.Maui.Controls;

namespace KsDialogs;

/// <summary>
/// Toast 表示の契約。
/// </summary>
/// <remarks>
/// 既定 singleton エントリ (<see cref="Toast.Instance"/>) と DI 注入のどちらからでも同じ契約で
/// 呼び出せる (core/ADR-0002)。どちらの入口から呼んでもレジストリと一括設定は 1 プロセスで共有される。
/// <para>
/// Toast は fire-and-forget の表示である (core/ADR-0031)。<c>Show</c> は戻り値を持たず、
/// 表示の終了を待つ手段も、閉じる・書き換える手段も契約に無い。消滅の契機は duration の経過だけで、
/// 表示中はいかなる入力も奪わない。
/// </para>
/// <para>
/// すべての呼び出しは任意のスレッドから行え、Native 側で UI スレッドへ移して受理順に直列化される。
/// 器メタ属性 (覆い・外側タップ) は Toast には存在しないため、この契約は
/// <c>Options</c> にあたるプロパティを持たない。カスタム View への配置と演出の添付は
/// ダイアログと同じ添付プロパティ (<c>ksd:Dialog.*</c>) を使う。
/// </para>
/// </remarks>
public interface IKsToast
{
    /// <summary>カスタム Toast の ViewModel 型と、MAUI View factory・ViewModel factory の紐付け。</summary>
    /// <remarks>Dialog / Loading のレジストリとは独立しており、全ての入口が同じレジストリを共有する。</remarks>
    ToastViewRegistry Registry { get; }

    /// <summary>Toast の一括設定 (core/ADR-0032)。各表示の受理時に読まれる。</summary>
    ToastStyle Style { get; set; }

    /// <summary>デフォルト View でメッセージを表示する。</summary>
    /// <remarks>
    /// 表示は受理された時点から数え、<paramref name="durationMs"/> の経過で自動的に消える。
    /// メッセージの内容は制限しない — 空文字は内容が空のまま表示され、長文は複数行に折り返す。
    /// </remarks>
    /// <param name="message">表示する文言。</param>
    /// <param name="durationMs">
    /// 表示するミリ秒。<see langword="null"/> なら <see cref="ToastStyle"/> の既定 duration。
    /// 0 以下は既定へ丸める。
    /// </param>
    /// <param name="placement">
    /// 配置。<see langword="null"/> なら <see cref="ToastStyle"/> のアプリ既定配置、
    /// それも無ければ契約の既定値。
    /// </param>
    void Show(string message, int? durationMs = null, DialogPlacement? placement = null);

    /// <summary>登録済みのカスタム Toast View を表示する (core/ADR-0029)。</summary>
    /// <remarks>
    /// 未登録の ViewModel 型は構成ミスとして呼び出し時点で失敗し
    /// (<see cref="DialogException.ViewFactoryNotRegistered"/>)、表示は行われない。
    /// </remarks>
    /// <param name="viewModel">表示するカスタム Toast の ViewModel。</param>
    /// <param name="durationMs">
    /// 表示するミリ秒。<see langword="null"/> なら <see cref="ToastStyle"/> の既定 duration。
    /// </param>
    /// <param name="placement">
    /// 配置。<see langword="null"/> なら View への添付、添付もなければ一括設定・契約の既定値。
    /// </param>
    void Show(IToastViewModel viewModel, int? durationMs = null, DialogPlacement? placement = null);

    /// <summary>
    /// 登録せずに、その場で渡した factory の中身をカスタム Toast として表示する (core/ADR-0013)。
    /// </summary>
    /// <remarks>
    /// factory の形も duration・配置の意味も登録経路とまったく同じである。
    /// <b>レジストリの状態は一切変わらない</b> — 同じ ViewModel 型の登録があってもそれは使われず、
    /// 登録内容もこの呼び出しの前後で変わらない。
    /// </remarks>
    /// <typeparam name="TViewModel">表示する ViewModel の型。</typeparam>
    /// <param name="viewModel">表示するカスタム Toast の ViewModel。</param>
    /// <param name="factory">中身の MAUI View を生成する関数。</param>
    /// <param name="durationMs">
    /// 表示するミリ秒。<see langword="null"/> なら <see cref="ToastStyle"/> の既定 duration。
    /// </param>
    /// <param name="placement">この呼び出しでの配置。</param>
    void Show<TViewModel>(
        TViewModel viewModel,
        Func<TViewModel, View> factory,
        int? durationMs = null,
        DialogPlacement? placement = null)
        where TViewModel : class, IToastViewModel;

    /// <summary>ViewModel の型を渡して、登録済みのカスタム Toast を表示する。</summary>
    /// <remarks>
    /// ViewModel の実体は登録済みの ViewModel factory が作る。実行順序は
    /// 「生成 → <paramref name="configure"/> → 中身の生成 → 表示」で固定され、
    /// <paramref name="configure"/> で整えた状態を中身の初期化から必ず読める。
    /// duration・配置・多重表示の意味はインスタンス渡しの表示と同じ。
    /// <para>
    /// ViewModel factory が登録されていない型は構成ミスとして呼び出し時点で失敗し
    /// (<see cref="DialogException.ViewModelFactoryNotRegistered"/>)、表示は行われない。
    /// 生成と <paramref name="configure"/> は表示が受理されたあとの UI スレッドで走るため、
    /// そこでの失敗は呼び出し元へ返らず、警告を残してその 1 枚だけが表示されずに終わる
    /// (他の表示と後続の表示には影響しない)。
    /// </para>
    /// </remarks>
    /// <typeparam name="TViewModel">表示する ViewModel の型。</typeparam>
    /// <param name="configure">生成した ViewModel の状態を整える処理。</param>
    /// <param name="durationMs">
    /// 表示するミリ秒。<see langword="null"/> なら <see cref="ToastStyle"/> の既定 duration。
    /// </param>
    /// <param name="placement">
    /// 配置。<see langword="null"/> なら View への添付、添付もなければ一括設定・契約の既定値。
    /// </param>
    void Show<TViewModel>(
        Action<TViewModel>? configure = null,
        int? durationMs = null,
        DialogPlacement? placement = null)
        where TViewModel : class, IToastViewModel;
}
