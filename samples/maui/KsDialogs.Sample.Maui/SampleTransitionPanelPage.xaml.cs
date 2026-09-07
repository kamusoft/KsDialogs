#if ANDROID
using Microsoft.Maui.Platform;
#endif

namespace KsDialogs.Sample.Maui;

/// <summary>出入りの演出を選んでからダイアログを表示する画面。</summary>
/// <remarks>初期値はプリセット <c>Fade</c>・時間 250 ms・イージング <c>Standard</c>。</remarks>
public partial class SampleTransitionPanelPage : ContentPage
{
    /// <summary>調整できる時間の下限 (ミリ秒)。</summary>
    private const int MinDurationMilliseconds = 100;

    /// <summary>調整できる時間の刻み (ミリ秒)。</summary>
    private const int DurationStepMilliseconds = 10;

    /// <summary>時間の初期値 (ミリ秒)。</summary>
    private const int InitialDurationMilliseconds = 250;

    /// <summary>調整部を操作できないときの濃さ。値が読める濃さを残す。</summary>
    private const double DisabledOpacity = 0.4d;

    /// <summary>演出を選ぶチップを並べる列の数。</summary>
    private const int PresetColumns = 2;

    /// <summary>演出を選ぶチップの高さの下限。各 OS の推奨タップ領域に合わせる。</summary>
#if ANDROID
    private const double PresetChipMinHeight = 48d;
#else
    private const double PresetChipMinHeight = 44d;
#endif

    /// <summary>演出を選ぶチップの文言の大きさ。</summary>
    private const double PresetChipFontSize = 14d;

    /// <summary>イージングを選ぶチップの高さの下限。</summary>
    private const double EasingChipMinHeight = 36d;

    /// <summary>イージングを選ぶチップの文言の大きさ。</summary>
    private const double EasingChipFontSize = 12d;

    private static readonly SampleTransitionChoice[] s_transitionChoices = Enum.GetValues<SampleTransitionChoice>();
    private static readonly SampleEasingChoice[] s_easingChoices = Enum.GetValues<SampleEasingChoice>();

    private readonly Action<string> _onResult;
    private readonly SampleChipsView _presetChips;
    private readonly SampleChipsView _easingChips;

    /// <summary>デモ画面を組み立てる。</summary>
    /// <param name="onResult">確定した結果をメニュー画面へ渡す。</param>
    public SampleTransitionPanelPage(Action<string> onResult)
    {
        InitializeComponent();
        _onResult = onResult;

        _presetChips = new SampleChipsView(
            [.. s_transitionChoices.Select(choice => choice.Label())],
            PresetColumns,
            PresetChipMinHeight,
            PresetChipFontSize,
            _ => RefreshAdjustAvailability());
        PresetChipsHost.Content = _presetChips;

        _easingChips = new SampleChipsView(
            [.. s_easingChoices.Select(choice => choice.Label())],
            s_easingChoices.Length,
            EasingChipMinHeight,
            EasingChipFontSize,
            _ => { });
        EasingChipsHost.Content = _easingChips;

        DurationSlider.Value = InitialDurationMilliseconds;
        DurationValueLabel.Text = SampleText.DurationValue(InitialDurationMilliseconds);
        RefreshAdjustAvailability();
    }

    /// <summary>選択中の演出。</summary>
    private SampleTransitionChoice TransitionChoice => s_transitionChoices[_presetChips.SelectedIndex];

    /// <summary>選択中のイージング。</summary>
    private SampleEasingChoice EasingChoice => s_easingChoices[_easingChips.SelectedIndex];

    /// <summary>選択中の時間 (ミリ秒)。</summary>
    private int DurationMilliseconds => Snap(DurationSlider.Value);

    /// <summary>戻る導線に、読み上げ用の名前と役割を与える。</summary>
    /// <param name="sender">対象の導線。</param>
    /// <param name="e">未使用。</param>
    /// <remarks>
    /// MAUI には役割 (role) を指定する共通の API がない。加えて Android では、文字を持つ platform の TextView に
    /// <see cref="SemanticProperties" /> の名前が乗らない。どちらも platform 側の読み上げ属性を直接与えて補い、
    /// Native の2ルートと同じく「戻る」という名前のボタンとして知らせる。属性は読み上げにだけ効き、見た目は変わらない。
    /// 表示が組み上がったあとに与えるのは、MAUI 自身の読み上げ属性の設定より後に置くため。
    /// </remarks>
    private void OnBackLoaded(object? sender, EventArgs e)
    {
        if (sender is not Label back)
        {
            return;
        }

        string description = SemanticProperties.GetDescription(back);

#if IOS
        if (back.Handler?.PlatformView is UIKit.UILabel platformLabel)
        {
            platformLabel.IsAccessibilityElement = true;
            platformLabel.AccessibilityLabel = description;
            platformLabel.AccessibilityTraits = UIKit.UIAccessibilityTrait.Button;
        }
#elif ANDROID
        if (back.Handler?.PlatformView is Android.Widget.TextView platformLabel)
        {
            platformLabel.ContentDescription = description;
            platformLabel.SetAccessibilityDelegate(new ButtonRoleDelegate(GoBack));
        }
#endif
    }

    /// <summary>時間を選ぶスライダの摘みを、共有の配色に合わせる。</summary>
    /// <param name="sender">対象のスライダ。</param>
    /// <param name="e">未使用。</param>
    /// <remarks>
    /// Android の既定の摘みは塗りだけで縁を持たないため、地の色と同じ塗りにすると見えなくなる。
    /// 縁のある摘みを自前で与えて、Native の2ルートと同じ見た目に揃える。
    /// </remarks>
    private void OnDurationSliderHandlerChanged(object? sender, EventArgs e)
    {
#if ANDROID
        if (sender is Slider { Handler.PlatformView: Android.Widget.SeekBar platformSlider } slider)
        {
            ApplyThumbSkin(platformSlider, slider);
        }
#endif
    }

    /// <summary>スライダの位置を刻みに合わせ、表示中の時間を更新する。</summary>
    /// <param name="sender">未使用。</param>
    /// <param name="e">変化後の値。</param>
    /// <remarks>
    /// MAUI のスライダは刻みを持たないため、値そのものを刻みへ寄せる。
    /// 寄せた値を書き戻すと同じ通知がもう一度来るが、二度目は寄せ先と一致するので止まる。
    /// </remarks>
    private void OnDurationChanged(object? sender, ValueChangedEventArgs e)
    {
        int snapped = Snap(e.NewValue);
        if (Math.Abs(e.NewValue - snapped) > double.Epsilon)
        {
            DurationSlider.Value = snapped;
            return;
        }

        DurationValueLabel.Text = SampleText.DurationValue(snapped);
    }

    /// <summary>
    /// 調整部を操作できるかを選択中の演出に合わせる。
    /// </summary>
    /// <remarks>無演出と自作フックは調整値を使わないため、値は保ったまま操作できなくする。</remarks>
    private void RefreshAdjustAvailability()
    {
        bool allowsAdjustments = TransitionChoice.UsesAdjustments();
        AdjustBlock.IsEnabled = allowsAdjustments;
        AdjustBlock.Opacity = allowsAdjustments ? 1d : DisabledOpacity;
    }

    /// <summary>戻る導線が押されたときにメニュー画面へ戻る。</summary>
    /// <param name="sender">未使用。</param>
    /// <param name="e">未使用。</param>
    private void OnBackSelected(object? sender, TappedEventArgs e) => GoBack();

    /// <summary>メニュー画面へ戻る。</summary>
    private async void GoBack()
    {
        await Navigation.PopModalAsync();
    }

    /// <summary>選んだ演出でダイアログを表示し、結果をデモ画面とメニューの両方へ出す。</summary>
    /// <param name="sender">未使用。</param>
    /// <param name="e">未使用。</param>
    private async void OnShowSelected(object? sender, EventArgs e)
    {
        // 演出は Show の引数では渡せないため、中身へ添付する組として ViewModel に載せて運ぶ
        TransitionDialogViewModel viewModel = new(
            SampleText.TransitionDialogMessage,
            TransitionChoice.Transition(DurationMilliseconds, EasingChoice));

        DialogResult<bool> result = await Dialog.Instance.ShowAsync(viewModel);
        string text = result switch
        {
            DialogResult<bool>.Completed completed => SampleText.CompletedResult(completed.Value),
            _ => SampleText.CancelledResult,
        };

        ResultValueLabel.Text = text;
        ResultArea.IsVisible = true;
        _onResult(text);
    }

    /// <summary>スライダの値を刻みに合わせる。</summary>
    /// <param name="value">スライダの値。</param>
    /// <returns>刻みに合わせた時間 (ミリ秒)。</returns>
    private static int Snap(double value)
    {
        int steps = (int)Math.Round((value - MinDurationMilliseconds) / DurationStepMilliseconds);
        return MinDurationMilliseconds + (steps * DurationStepMilliseconds);
    }

#if ANDROID
    /// <summary>指で押す仕掛けしか持たない TextView を、ボタンとして読み上げ・操作できるようにする委譲。</summary>
    /// <param name="onClick">読み上げから起動されたときに行う操作。</param>
    /// <remarks>
    /// MAUI の <see cref="TapGestureRecognizer" /> は platform の View を押せる状態にしないため、
    /// 役割だけを与えても読み上げからは起動できない。役割と併せて起動の道も開く。
    /// 指での操作は従来どおり <see cref="TapGestureRecognizer" /> が受けるので、二重には起動しない。
    /// </remarks>
    private sealed class ButtonRoleDelegate(Action onClick) : Android.Views.View.AccessibilityDelegate
    {
        /// <summary>読み上げに渡す情報へ、ボタンとしての役割と起動の道を書き入れる。</summary>
        /// <param name="host">情報の取得元の View。</param>
        /// <param name="info">読み上げに渡す情報。</param>
        public override void OnInitializeAccessibilityNodeInfo(
            Android.Views.View host,
            Android.Views.Accessibility.AccessibilityNodeInfo info)
        {
            base.OnInitializeAccessibilityNodeInfo(host, info);
            info.ClassName = Java.Lang.Class.FromType(typeof(Android.Widget.Button)).Name;
            info.Clickable = true;
            info.AddAction(Android.Views.Accessibility.AccessibilityNodeInfo.AccessibilityAction.ActionClick!);
        }

        /// <summary>読み上げからの起動を受けて操作を行う。</summary>
        /// <param name="host">操作の対象の View。</param>
        /// <param name="action">要求された操作の種別。</param>
        /// <param name="args">操作の引数。</param>
        /// <returns>操作を引き受けたか。</returns>
        public override bool PerformAccessibilityAction(
            Android.Views.View host,
            [Android.Runtime.GeneratedEnum] Android.Views.Accessibility.Action action,
            Android.OS.Bundle? args)
        {
            if (action == Android.Views.Accessibility.Action.Click)
            {
                onClick();
                return true;
            }

            return base.PerformAccessibilityAction(host, action, args);
        }
    }

    /// <summary>区切り線の色の資源キー。摘みの縁に使う。</summary>
    private const string DividerColorKey = "SampleDivider";

    /// <summary>摘みの直径。</summary>
    private const float ThumbSizeDp = 20f;

    /// <summary>摘みの縁の太さ。</summary>
    private const float ThumbStrokeWidthDp = 1f;

    /// <summary>スライダの摘みに、縁のある塗りを与える。</summary>
    /// <param name="platformSlider">塗りを与える platform のスライダ。</param>
    /// <param name="slider">配色の指定元。</param>
    private static void ApplyThumbSkin(Android.Widget.SeekBar platformSlider, Slider slider)
    {
        Android.Content.Context context = platformSlider.Context!;
        Android.Graphics.Color strokeColor =
            ((Color)Application.Current!.Resources[DividerColorKey]).ToPlatform();

        // 自前の塗りに色合いが重ならないよう、既定の色合いを外す
        platformSlider.ThumbTintList = null;

        Android.Graphics.Drawables.GradientDrawable thumb = new();
        thumb.SetShape(Android.Graphics.Drawables.ShapeType.Oval);
        thumb.SetColor(slider.ThumbColor.ToPlatform());
        thumb.SetStroke(Dp(context, ThumbStrokeWidthDp), strokeColor);
        thumb.SetSize(Dp(context, ThumbSizeDp), Dp(context, ThumbSizeDp));
        platformSlider.SetThumb(thumb);
        platformSlider.SplitTrack = false;
    }

    /// <summary>密度非依存の寸法を実際のピクセル数へ変換する。</summary>
    /// <param name="context">変換に使う context。</param>
    /// <param name="value">密度非依存の寸法。</param>
    /// <returns>実際のピクセル数。</returns>
    private static int Dp(Android.Content.Context context, float value) =>
        (int)(value * context.Resources!.DisplayMetrics!.Density);
#endif
}
