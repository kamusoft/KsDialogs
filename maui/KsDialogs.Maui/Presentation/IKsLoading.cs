using System;
using System.Threading.Tasks;
using Microsoft.Maui.Controls;

namespace KsDialogs;

/// <summary>
/// ローディング表示の契約。
/// </summary>
/// <remarks>
/// 既定 singleton エントリ (<see cref="Loading.Instance"/>) と DI 注入のどちらからでも同じ契約で
/// 呼び出せる (core/ADR-0002)。どちらの入口から呼んでも表示は 1 プロセスに 1 つで、合流状態は
/// Native ライブラリ側で共有される (core/ADR-0024)。
/// <para>
/// すべての呼び出しは任意のスレッドから行え、状態を変える操作は UI スレッド上で受理順に
/// 直列化される (「最新 (後勝ち)」はその受理順で定まる)。
/// </para>
/// <para>
/// 器メタ属性の添付はダイアログと同じ添付プロパティ (<c>ksd:Dialog.*</c>) を使う。
/// 既定ローディングには利用者が属性を添付する View が無いため、<see cref="Options"/> が
/// その代わりになる。<c>IsCanceledOnTouchOutside</c> は Loading では常に無効である。
/// </para>
/// </remarks>
public interface IKsLoading
{
    /// <summary>カスタム Loading の ViewModel 型と、MAUI View factory・ViewModel factory の紐付け。</summary>
    /// <remarks>Dialog のレジストリとは独立しており、全ての入口が同じレジストリを共有する。</remarks>
    LoadingViewRegistry Registry { get; }

    /// <summary>既定ローディングの見た目の設定 (core/ADR-0023)。各表示の開始時に読まれる。</summary>
    LoadingStyle Style { get; set; }

    /// <summary>
    /// 既定ローディングの器メタ属性 (core/ADR-0022)。各表示の開始時に読まれる。
    /// </summary>
    /// <remarks>
    /// 利用者が属性を添付する View を持たない既定ローディングでは、これがコンテンツへの添付の
    /// 代わりになる。カスタム View の表示ではこの値は使われず、View への添付が正となる。
    /// </remarks>
    DialogOptions Options { get; set; }

    /// <summary>既定ローディングを表示し、合流 1 件を開始する。</summary>
    /// <remarks>
    /// 対応する終了は <see cref="HideAsync"/> だけである (合流数によらず即閉じる)。
    /// 戻るのは操作ブロックが有効になった時点で、入りの演出の完了は待たない。
    /// 既に表示中なら 1 つの表示に合流し、中身は最初の開始のものが維持される (core/ADR-0024)。
    /// </remarks>
    /// <param name="message">表示するメッセージ。<see langword="null"/> ならスタイルの既定メッセージ。</param>
    /// <param name="placement">置き場所。<see langword="null"/> なら契約の既定値 (core/ADR-0015)。</param>
    /// <returns>開始の完了。</returns>
    Task ShowAsync(string? message = null, DialogPlacement? placement = null);

    /// <summary>登録済みのカスタム Loading View を表示し、合流 1 件を開始する。</summary>
    /// <remarks>
    /// 未登録の ViewModel 型は結果に化けさせず構成ミスとして失敗し
    /// (<see cref="DialogException.ViewFactoryNotRegistered"/>)、表示は行われない。
    /// </remarks>
    /// <param name="viewModel">表示するカスタム Loading の ViewModel。</param>
    /// <param name="placement">
    /// 置き場所。<see langword="null"/> なら View への添付、添付もなければ契約の既定値。
    /// </param>
    /// <returns>開始の完了。</returns>
    Task ShowAsync(ILoadingViewModel viewModel, DialogPlacement? placement = null);

    /// <summary>
    /// 登録せずに、その場で渡した factory の中身をカスタム Loading として表示する (core/ADR-0013)。
    /// </summary>
    /// <remarks>
    /// factory の形も合流の数え方も登録経路とまったく同じで、<paramref name="placement"/> の意味も
    /// 変わらない。<b>レジストリの状態は一切変わらない</b> — 同じ ViewModel 型の登録があっても
    /// それは使われず、登録内容もこの呼び出しの前後で変わらない。
    /// </remarks>
    /// <typeparam name="TViewModel">表示する ViewModel の型。</typeparam>
    /// <param name="viewModel">表示するカスタム Loading の ViewModel。</param>
    /// <param name="factory">中身の MAUI View を生成する関数。</param>
    /// <param name="placement">この呼び出しでの置き場所。</param>
    /// <returns>開始の完了。</returns>
    Task ShowAsync<TViewModel>(
        TViewModel viewModel,
        Func<TViewModel, View> factory,
        DialogPlacement? placement = null)
        where TViewModel : class, ILoadingViewModel;

    /// <summary>
    /// ViewModel の型を渡して、登録済みのカスタム Loading を表示し合流 1 件を開始する。
    /// </summary>
    /// <remarks>
    /// ViewModel の実体は登録済みの ViewModel factory が作る。実行順序は
    /// 「生成 → <paramref name="configure"/> → 進捗受け口の紐付け → 中身の生成 → 表示」で固定され、
    /// <paramref name="configure"/> で整えた状態を中身の初期化から必ず読める。
    /// ViewModel factory が登録されていない型は構成ミスとして失敗し
    /// (<see cref="DialogException.ViewModelFactoryNotRegistered"/>)、表示は行われない。
    /// <paramref name="configure"/> の失敗も表示に進まず呼び出し元へ伝播する。
    /// 合流・置き場所・進捗転送の意味はインスタンス渡しの表示と同じで、既に表示中なら 1 つの表示に
    /// 合流し、生成した ViewModel は表示に使われない。
    /// </remarks>
    /// <typeparam name="TViewModel">表示する ViewModel の型。</typeparam>
    /// <param name="configure">生成した ViewModel の状態を整える処理。</param>
    /// <param name="placement">
    /// 置き場所。<see langword="null"/> なら View への添付、添付もなければ契約の既定値。
    /// </param>
    /// <returns>開始の完了。</returns>
    Task ShowAsync<TViewModel>(
        Action<TViewModel>? configure = null,
        DialogPlacement? placement = null)
        where TViewModel : class, ILoadingViewModel;

    /// <summary>
    /// ViewModel の型を渡して、非同期に状態を整えてから登録済みのカスタム Loading を表示する。
    /// </summary>
    /// <remarks>
    /// <paramref name="configure"/> が完了するまで中身の生成へ進まない点以外は、同期の
    /// <paramref name="configure"/> を渡す形と同じ。待っている間に別の表示が始まっていれば
    /// この呼び出しはその表示へ合流する。
    /// </remarks>
    /// <typeparam name="TViewModel">表示する ViewModel の型。</typeparam>
    /// <param name="configure">生成した ViewModel の状態を非同期に整える処理。</param>
    /// <param name="placement">
    /// 置き場所。<see langword="null"/> なら View への添付、添付もなければ契約の既定値。
    /// </param>
    /// <returns>開始の完了。</returns>
    Task ShowAsync<TViewModel>(
        Func<TViewModel, Task> configure,
        DialogPlacement? placement = null)
        where TViewModel : class, ILoadingViewModel;

    /// <summary>表示を閉じる。合流数によらず即座に閉じ、走行中の処理には干渉しない。</summary>
    /// <remarks>
    /// 出の演出と器の撤去が完了してから戻る。表示していなければ何も起こらない。
    /// 閉じたあとの新しい表示は、旧世代の処理の完了・進捗に影響されない (core/ADR-0024)。
    /// </remarks>
    /// <returns>撤去の完了。</returns>
    Task HideAsync();

    /// <summary>表示中のメッセージを更新する。合流には関与しない (対応する終了は要らない)。</summary>
    /// <remarks>
    /// 既定ローディングを表示している間だけ効き、非表示中とカスタム View 表示中は何も起こらない。
    /// 更新は UI スレッド上で受理順に反映されるため、この呼び出し自体は待たない。
    /// </remarks>
    /// <param name="message">新しいメッセージ。</param>
    void SetMessage(string? message);

    /// <summary>既定ローディングを表示したまま処理を実行する。</summary>
    /// <remarks>
    /// 合流 1 件の開始と終了が処理の開始・完了に対応する。処理は表示状態によらず必ず実行され、
    /// 失敗も合流 1 件の終了として数えたうえで呼び出し元へ伝播する。
    /// 合流最後の 1 件なら器の撤去まで待ってから戻り、そうでなければ処理の完了時点で戻る。
    /// </remarks>
    /// <param name="action">実行する処理。引数の報告口へ 0〜1 の進捗を報告できる (任意スレッド可)。</param>
    /// <param name="message">表示するメッセージ。<see langword="null"/> ならスタイルの既定メッセージ。</param>
    /// <param name="placement">置き場所。<see langword="null"/> なら契約の既定値。</param>
    /// <returns>処理と (最後の 1 件なら) 撤去の完了。</returns>
    Task StartAsync(
        Func<IProgress<double>, Task> action,
        string? message = null,
        DialogPlacement? placement = null);

    /// <summary>既定ローディングを表示したまま処理を実行し、その戻り値を返す。</summary>
    /// <remarks>戻り値を返す点以外は値を返さない形と同じ。</remarks>
    /// <typeparam name="T">処理が返す値の型。</typeparam>
    /// <param name="action">実行する処理。引数の報告口へ 0〜1 の進捗を報告できる (任意スレッド可)。</param>
    /// <param name="message">表示するメッセージ。<see langword="null"/> ならスタイルの既定メッセージ。</param>
    /// <param name="placement">置き場所。<see langword="null"/> なら契約の既定値。</param>
    /// <returns>処理の戻り値。</returns>
    Task<T> StartAsync<T>(
        Func<IProgress<double>, Task<T>> action,
        string? message = null,
        DialogPlacement? placement = null);

    /// <summary>登録済みのカスタム Loading View を表示したまま処理を実行する。</summary>
    /// <remarks>未登録の ViewModel 型は構成ミスとして失敗し、処理は実行されない (fail-fast)。</remarks>
    /// <param name="viewModel">表示するカスタム Loading の ViewModel。</param>
    /// <param name="action">実行する処理。引数の報告口へ 0〜1 の進捗を報告できる。</param>
    /// <param name="placement">置き場所。<see langword="null"/> なら View への添付。</param>
    /// <returns>処理と (最後の 1 件なら) 撤去の完了。</returns>
    Task StartAsync(
        ILoadingViewModel viewModel,
        Func<IProgress<double>, Task> action,
        DialogPlacement? placement = null);

    /// <summary>登録済みのカスタム Loading View を表示したまま処理を実行し、その戻り値を返す。</summary>
    /// <typeparam name="T">処理が返す値の型。</typeparam>
    /// <param name="viewModel">表示するカスタム Loading の ViewModel。</param>
    /// <param name="action">実行する処理。引数の報告口へ 0〜1 の進捗を報告できる。</param>
    /// <param name="placement">置き場所。<see langword="null"/> なら View への添付。</param>
    /// <returns>処理の戻り値。</returns>
    Task<T> StartAsync<T>(
        ILoadingViewModel viewModel,
        Func<IProgress<double>, Task<T>> action,
        DialogPlacement? placement = null);

    /// <summary>
    /// 登録せずに、その場で渡した factory の中身を表示したまま処理を実行する (core/ADR-0013)。
    /// </summary>
    /// <remarks>レジストリの状態は一切変わらない。それ以外は登録経路のスコープ形と同じ。</remarks>
    /// <typeparam name="TViewModel">表示する ViewModel の型。</typeparam>
    /// <param name="viewModel">表示するカスタム Loading の ViewModel。</param>
    /// <param name="factory">中身の MAUI View を生成する関数。</param>
    /// <param name="action">実行する処理。引数の報告口へ 0〜1 の進捗を報告できる。</param>
    /// <param name="placement">この呼び出しでの置き場所。</param>
    /// <returns>処理と (最後の 1 件なら) 撤去の完了。</returns>
    Task StartAsync<TViewModel>(
        TViewModel viewModel,
        Func<TViewModel, View> factory,
        Func<IProgress<double>, Task> action,
        DialogPlacement? placement = null)
        where TViewModel : class, ILoadingViewModel;

    /// <summary>
    /// 登録せずに、その場で渡した factory の中身を表示したまま処理を実行し、その戻り値を返す
    /// (core/ADR-0013)。
    /// </summary>
    /// <typeparam name="TViewModel">表示する ViewModel の型。</typeparam>
    /// <typeparam name="T">処理が返す値の型。</typeparam>
    /// <param name="viewModel">表示するカスタム Loading の ViewModel。</param>
    /// <param name="factory">中身の MAUI View を生成する関数。</param>
    /// <param name="action">実行する処理。引数の報告口へ 0〜1 の進捗を報告できる。</param>
    /// <param name="placement">この呼び出しでの置き場所。</param>
    /// <returns>処理の戻り値。</returns>
    Task<T> StartAsync<TViewModel, T>(
        TViewModel viewModel,
        Func<TViewModel, View> factory,
        Func<IProgress<double>, Task<T>> action,
        DialogPlacement? placement = null)
        where TViewModel : class, ILoadingViewModel;

    /// <summary>
    /// ViewModel の型を渡して、登録済みのカスタム Loading を表示したまま処理を実行する。
    /// </summary>
    /// <remarks>
    /// ViewModel の生成・<paramref name="configure"/>・失敗の扱いは型を渡す表示と同じで、
    /// 生成または <paramref name="configure"/> が失敗した場合は処理を実行しない (fail-fast)。
    /// 開始と終了の対の数え方と戻りの待ち方は、他のスコープ形と同じ。
    /// 進捗は生成した ViewModel が進捗の受け口を実装していればそこへ届く。
    /// </remarks>
    /// <typeparam name="TViewModel">表示する ViewModel の型。</typeparam>
    /// <param name="action">実行する処理。引数の報告口へ 0〜1 の進捗を報告できる (任意スレッド可)。</param>
    /// <param name="configure">生成した ViewModel の状態を整える処理。</param>
    /// <param name="placement">
    /// 置き場所。<see langword="null"/> なら View への添付、添付もなければ契約の既定値。
    /// </param>
    /// <returns>処理と (最後の 1 件なら) 撤去の完了。</returns>
    Task StartAsync<TViewModel>(
        Func<IProgress<double>, Task> action,
        Action<TViewModel>? configure = null,
        DialogPlacement? placement = null)
        where TViewModel : class, ILoadingViewModel;

    /// <summary>
    /// ViewModel の型を渡して非同期に状態を整えてから、登録済みのカスタム Loading を表示したまま
    /// 処理を実行する。
    /// </summary>
    /// <remarks>
    /// <paramref name="configure"/> が完了するまで表示にも処理にも進まない点以外は、同期の
    /// <paramref name="configure"/> を渡す形と同じ。
    /// </remarks>
    /// <typeparam name="TViewModel">表示する ViewModel の型。</typeparam>
    /// <param name="action">実行する処理。引数の報告口へ 0〜1 の進捗を報告できる (任意スレッド可)。</param>
    /// <param name="configure">生成した ViewModel の状態を非同期に整える処理。</param>
    /// <param name="placement">
    /// 置き場所。<see langword="null"/> なら View への添付、添付もなければ契約の既定値。
    /// </param>
    /// <returns>処理と (最後の 1 件なら) 撤去の完了。</returns>
    Task StartAsync<TViewModel>(
        Func<IProgress<double>, Task> action,
        Func<TViewModel, Task> configure,
        DialogPlacement? placement = null)
        where TViewModel : class, ILoadingViewModel;

    /// <summary>
    /// ViewModel の型を渡して、登録済みのカスタム Loading を表示したまま処理を実行し、
    /// その戻り値を返す。
    /// </summary>
    /// <remarks>戻り値を返す点以外は値を返さない形と同じ。</remarks>
    /// <typeparam name="TViewModel">表示する ViewModel の型。</typeparam>
    /// <typeparam name="T">処理が返す値の型。</typeparam>
    /// <param name="action">実行する処理。引数の報告口へ 0〜1 の進捗を報告できる (任意スレッド可)。</param>
    /// <param name="configure">生成した ViewModel の状態を整える処理。</param>
    /// <param name="placement">
    /// 置き場所。<see langword="null"/> なら View への添付、添付もなければ契約の既定値。
    /// </param>
    /// <returns>処理の戻り値。</returns>
    Task<T> StartAsync<TViewModel, T>(
        Func<IProgress<double>, Task<T>> action,
        Action<TViewModel>? configure = null,
        DialogPlacement? placement = null)
        where TViewModel : class, ILoadingViewModel;

    /// <summary>
    /// ViewModel の型を渡して非同期に状態を整えてから、登録済みのカスタム Loading を表示したまま
    /// 処理を実行し、その戻り値を返す。
    /// </summary>
    /// <remarks>戻り値を返す点以外は値を返さない形と同じ。</remarks>
    /// <typeparam name="TViewModel">表示する ViewModel の型。</typeparam>
    /// <typeparam name="T">処理が返す値の型。</typeparam>
    /// <param name="action">実行する処理。引数の報告口へ 0〜1 の進捗を報告できる (任意スレッド可)。</param>
    /// <param name="configure">生成した ViewModel の状態を非同期に整える処理。</param>
    /// <param name="placement">
    /// 置き場所。<see langword="null"/> なら View への添付、添付もなければ契約の既定値。
    /// </param>
    /// <returns>処理の戻り値。</returns>
    Task<T> StartAsync<TViewModel, T>(
        Func<IProgress<double>, Task<T>> action,
        Func<TViewModel, Task> configure,
        DialogPlacement? placement = null)
        where TViewModel : class, ILoadingViewModel;
}
