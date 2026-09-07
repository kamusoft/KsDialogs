using System;
using System.Threading.Tasks;
using Microsoft.Maui.Controls;

namespace KsDialogs;

/// <summary>
/// Native ライブラリの Bridge への委譲面 (maui/ADR-0002)。
/// </summary>
/// <remarks>
/// platform 非依存の facade ロジック (レジストリ解決・結果型の復元) と platform 実装の継ぎ目で、
/// 結果値は型を消したまま運ぶ。差し替え可能にしてあるため、
/// platform 実装なしで結果経路と委譲の契約 (引数・結果・show との 1 対 1) を検証できる。
/// </remarks>
internal interface IDialogGateway
{
    /// <summary>
    /// 中身の MAUI View を組み立ててダイアログとして提示し、結果が配送されるまで待つ。
    /// </summary>
    /// <remarks>
    /// 提示処理は実装側で UI スレッドへマーシャリングされるため、任意のスレッドから呼び出せる。
    /// アクティブな提示先が存在しない場合は結果を返さずに
    /// <see cref="DialogException.PresentationHostUnavailable"/> で失敗し、View の生成・表示も行わない。
    /// 結果は最初の報告で確定する (ラッチ) が、この呼び出しが返るのは退出の演出と覆いの消滅が終わり
    /// 器が撤去された後になる (core/ADR-0017)。返った時点でダイアログはもう画面にない。
    /// </remarks>
    /// <param name="request">その show が提示する内容。</param>
    /// <returns>型を消した結果。</returns>
    Task<DialogOutcome> PresentAsync(DialogPresentationRequest request);
}

/// <summary>
/// 1 回の show が提示する内容。
/// </summary>
/// <param name="createContentView">中身の MAUI View を新規生成する関数。提示先が確保できた後に UI スレッドで呼ばれる。</param>
/// <param name="resultChannel">その show の結果チャネル。器はキャンセル操作をここへ報告する。</param>
/// <param name="showPlacement">show の引数で渡された置き場所。指定なしは <see langword="null"/>。</param>
internal sealed class DialogPresentationRequest(
    Func<View> createContentView,
    DialogResultChannel resultChannel,
    DialogPlacement? showPlacement)
{
    /// <summary>その show の結果チャネル。</summary>
    public DialogResultChannel ResultChannel { get; } = resultChannel;

    /// <summary>show の引数で渡された置き場所。中身への添付より優先される。</summary>
    public DialogPlacement? ShowPlacement { get; } = showPlacement;

    /// <summary>
    /// 中身の MAUI View を新規生成する。
    /// </summary>
    /// <remarks>メタ属性は生成時には読まない。読み取り時点は <see cref="DialogPresentationContent"/> が持つ。</remarks>
    /// <returns>提示する中身。</returns>
    public DialogPresentationContent CreateContent() =>
        new(createContentView(), ShowPlacement);
}

/// <summary>
/// 提示する中身と、その提示に効くメタ属性。
/// </summary>
/// <remarks>
/// 実効値は「show 引数 &gt; 中身への添付 &gt; 契約既定値」の優先順で合成し、show 引数の置き場所は
/// 添付された置き場所をオブジェクトまるごと置換する (項目単位では合成しない。core/ADR-0015)。
/// 出入りの演出は中身への添付でしか渡せないが、採用時点は他のメタ属性と同じ (core/ADR-0017)。
/// 値の丸めもレイアウト計算も Native 実装の責務なので、ここでは束ねるだけにする (core/ADR-0001)。
/// <para>
/// 契約が定める採用時点は<b>初回のネイティブレイアウトパス完了時点</b>であり、中身を生成した時点ではない。
/// そのため添付は読むたびに合成し直し、そのパスの完了時に <see cref="FreezeAttributes"/> で固定する。
/// 固定した後の添付変更は実効値を変えない。
/// </para>
/// </remarks>
/// <param name="contentView">中身の MAUI View。</param>
/// <param name="showPlacement">show の引数で渡された置き場所。指定なしは <see langword="null"/>。</param>
internal sealed class DialogPresentationContent(View contentView, DialogPlacement? showPlacement)
{
    private readonly DialogContentHost _host = new(contentView);
    private DialogAttributes? _frozenAttributes;

    /// <summary>中身の MAUI View。</summary>
    /// <remarks>
    /// 中身は提示 1 回分の親 (<see cref="DialogContentHost"/>) に抱えられた状態で運ぶ。
    /// 親を持たない View では MAUI の移動・拡大縮小が platform view に届かないためで、
    /// 親の寿命はこの入れ物が握る。
    /// </remarks>
    public View ContentView => _host.ContentView;

    /// <summary>その時点のメタ属性の実効値。固定後は固定した値。</summary>
    public DialogAttributes Attributes => _frozenAttributes ?? Compose();

    /// <summary>静的メタ属性の実効値。</summary>
    public DialogOptions Options => Attributes.Options;

    /// <summary>動的メタ属性 (置き場所) の実効値。</summary>
    public DialogPlacement Placement => Attributes.Placement;

    /// <summary>出入りの演出の実効値。未添付なら <see langword="null"/>。</summary>
    public DialogTransition? Transition => Attributes.Transition;

    /// <summary>実効値を固定済みか。</summary>
    public bool AreAttributesFrozen => _frozenAttributes is not null;

    /// <summary>その時点の実効値をスナップショットとして固定する。2 回目以降の呼び出しは何もしない。</summary>
    public void FreezeAttributes() => _frozenAttributes ??= Compose();

    private DialogAttributes Compose() => new(
        Dialog.AttachedOptions(ContentView),
        showPlacement ?? Dialog.AttachedPlacement(ContentView),
        Dialog.GetTransition(ContentView));
}

/// <summary>
/// 1 回の提示に効くメタ属性の実効値。
/// </summary>
/// <param name="Options">静的メタ属性の実効値。</param>
/// <param name="Placement">動的メタ属性 (置き場所) の実効値。</param>
/// <param name="Transition">出入りの演出。未添付なら <see langword="null"/> で、器の既定が使われる。</param>
internal sealed record DialogAttributes(
    DialogOptions Options,
    DialogPlacement Placement,
    DialogTransition? Transition);
