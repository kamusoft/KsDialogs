using System;
using Microsoft.Maui;
using Microsoft.Maui.Controls;

namespace KsDialogs;

/// <summary>
/// Native 実装が提示先に選ぶ画面と、その画面の文脈の組。
/// </summary>
/// <remarks>
/// platform view 化に使う文脈と、基準領域「表示中のページ」を辿る起点の画面は、同じ解決で一組に決める。
/// 別々に選ぶと、文脈は提示先の画面のものなのにページは別の画面から辿る、というずれが起こり得る。
/// </remarks>
/// <param name="Window">提示先の画面。</param>
/// <param name="MauiContext">その画面の文脈。</param>
internal sealed record DialogPresentationTarget(Window Window, IMauiContext MauiContext);

/// <summary>
/// 中身の供給の時点で、Native の提示先に対応する画面の文脈を取り出す面。
/// </summary>
/// <remarks>
/// 中身の供給は Native が提示先を確保した後に呼ばれるため、本来は文脈が取れる。取れないのは
/// Native の提示先の追跡と MAUI の画面の状態がずれた場合で、そのときは中身の生成の失敗として扱う
/// (Dialog は show の失敗、Loading は開始の失敗または表示の諦め、Toast はその 1 枚の破棄)。
/// 提示の仕組みそのものが無いことを表す <see cref="DialogException.PresentationHostUnavailable"/> は使わない。
/// </remarks>
internal static class DialogPresentationContext
{
    /// <summary>文脈が取れなかったときの失敗の文言。</summary>
    public const string UnavailableMessage =
        "No MAUI context is available for the screen that presents the content.";

    /// <summary>解決した提示先から文脈を取り出す。</summary>
    /// <param name="target">解決した提示先。見つからなければ <see langword="null"/>。</param>
    /// <returns>提示先の画面の文脈。</returns>
    /// <exception cref="InvalidOperationException">提示先の画面の文脈が取れない。</exception>
    public static IMauiContext Require(DialogPresentationTarget? target) =>
        target?.MauiContext ?? throw new InvalidOperationException(UnavailableMessage);
}
