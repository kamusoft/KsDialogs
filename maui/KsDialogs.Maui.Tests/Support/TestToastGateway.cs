using System.Collections.Generic;
using System.Linq;
using Microsoft.Maui.Controls;

namespace KsDialogs.Maui.Tests.Support;

/// <summary>
/// platform 実装の代わりに使う Toast の委譲面。
/// </summary>
/// <remarks>
/// 表示中のリスト・重なり順・計時・duration の丸めはすべて Native 実装の責務なので、
/// この差し替えは<b>再現しない</b>。MAUI 形態に課されるのは「解決した中身と供給値がそのまま
/// 委譲面へ届くこと」だけなので、渡された値を記録する。
/// </remarks>
internal sealed class TestToastGateway : IToastGateway
{
    /// <summary>設定されたスタイルを設定順に記録したもの。</summary>
    public List<ToastStyle> AppliedStyles { get; } = [];

    /// <summary>委譲を受けた表示の要求を呼ばれた順に記録したもの。</summary>
    public List<ToastPresentationRequest> Requests { get; } = [];

    /// <summary>生成された中身とメタ属性の実効値を呼ばれた順に記録したもの。</summary>
    public List<DialogPresentationContent> Contents { get; } = [];

    /// <summary>生成された中身の View を呼ばれた順に記録したもの。</summary>
    public List<View> CreatedViews => [.. Contents.Select(content => content.ContentView)];

    /// <summary>中身を作れずに破棄された表示の枚数。</summary>
    public int DiscardedCount { get; private set; }

    /// <summary>
    /// 受理した表示の中身をその場では作らず、<see cref="DrainDeferredContents"/> まで保留するか。
    /// </summary>
    /// <remarks>UI スレッドの受理キューを止めた状態にあたる。</remarks>
    public bool DefersContentCreation { get; set; }

    private readonly List<ToastPresentationRequest> _deferred = [];

    /// <inheritdoc/>
    public void ApplyStyle(ToastStyle style) => AppliedStyles.Add(style);

    /// <inheritdoc/>
    public void Show(ToastPresentationRequest request)
    {
        Requests.Add(request);
        if (DefersContentCreation)
        {
            _deferred.Add(request);
            return;
        }

        Accept(request);
    }

    /// <summary>保留していた受理を順に進める (止めていた受理キューを動かすことにあたる)。</summary>
    public void DrainDeferredContents()
    {
        foreach (ToastPresentationRequest request in _deferred)
        {
            Accept(request);
        }

        _deferred.Clear();
    }

    /// <summary>器が提示先を確保した後にあたる時点で中身を生成する。</summary>
    /// <remarks>
    /// 供給元の失敗は境界を越えず、その表示 1 枚の破棄になる (core/ADR-0031・0033)。
    /// platform の委譲面と同じ受け止め方をここでも通す。
    /// </remarks>
    /// <param name="request">受理した表示の要求。</param>
    private void Accept(ToastPresentationRequest request)
    {
        if (request.IsBuiltin)
        {
            return;
        }

        DialogPresentationContent? content = BridgeContentSupply.CreateOrDiscard(request.CreateContent);
        if (content is null)
        {
            DiscardedCount++;
        }
        else
        {
            Contents.Add(content);
        }
    }
}
