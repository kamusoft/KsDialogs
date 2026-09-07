using Microsoft.Maui.Controls;

namespace KsDialogs;

/// <summary>
/// 中身の MAUI View を MAUI の要素ツリーへ載せるための、その View だけを抱える親。
/// </summary>
/// <remarks>
/// 中身は ViewModel ごとの factory が新規生成し、器が持つのはその platform view だけなので、
/// MAUI 側の親は放っておくと誰にもならない。ところが MAUI の iOS 実装は、親を持たない View には
/// 移動・回転・拡大縮小 (platform view の transform に落ちる値) を反映しない — 透明度だけは
/// 別経路で反映されるため、親がないままだと演出のうち位置と大きさだけが画面に出ない。
/// ここで親を与えて要素ツリーの一員にすることで、プリセットも利用者のフックも
/// MAUI の通常のアニメーション API がそのまま効く状態になる。
/// <para>
/// この親はスタイルも BindingContext も持たないため、中身の見た目と結び付きには影響しない
/// (アプリのリソースを中身へ流し込む意図はない)。
/// </para>
/// <para>
/// MAUI は親を弱参照で持つため、この親は中身と同じ寿命を持つ側が強く持ち続ける必要がある。
/// </para>
/// </remarks>
internal sealed class DialogContentHost : Element
{
    /// <summary>中身を論理上の子として抱える。</summary>
    /// <param name="contentView">中身の MAUI View。</param>
    public DialogContentHost(View contentView)
    {
        ContentView = contentView;
        AddLogicalChild(contentView);
    }

    /// <summary>抱えている中身の MAUI View。</summary>
    public View ContentView { get; }
}
