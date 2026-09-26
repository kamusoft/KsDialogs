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
