using System.Globalization;
#if ANDROID
using Microsoft.Maui.Platform;
#endif

namespace KsDialogs.Sample.Maui;

/// <summary>レイアウト属性を調整してからダイアログを表示する画面。</summary>
/// <remarks>初期値は契約の既定値 (中央配置・移動なし・可視領域基準) に揃える。</remarks>
public partial class SampleLayoutPanelPage : ContentPage
{
    private readonly Action<string> _onResult;

    /// <summary>パネルを組み立てる。</summary>
    /// <param name="onResult">確定した結果をメニュー画面へ渡す。</param>
    public SampleLayoutPanelPage(Action<string> onResult)
    {
        InitializeComponent();
        _onResult = onResult;
    }

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

    /// <summary>移動量欄のキーボードを符号付きの数値入力に切り替える。</summary>
    /// <param name="sender">対象の入力欄。</param>
    /// <param name="e">未使用。</param>
    /// <remarks>
    /// MAUI の <see cref="Keyboard" /> には符号付き数値の選択肢がなく、
    /// <see cref="Keyboard.Numeric" /> は各 OS の符号なし数値キーボードに写像されるため負値を打てない。
    /// ハンドラー接続後に platform 側のキーボード種別を上書きし、Native の2ルート
    /// (iOS は記号を含む数値キーボード、Android は符号付き数値) と同じ入力手段に揃える。
    /// </remarks>
    private void OnOffsetEntryHandlerChanged(object? sender, EventArgs e)
    {
#if IOS
        if (sender is Entry { Handler.PlatformView: UIKit.UITextField textField })
        {
            textField.KeyboardType = UIKit.UIKeyboardType.NumbersAndPunctuation;

            // 枠は外側の Border が描くため、入力欄そのものに付く角丸枠は消す。残すと枠が二重に見える
            textField.BorderStyle = UIKit.UITextBorderStyle.None;
        }
#elif ANDROID
        if (sender is Entry { Handler.PlatformView: Android.Widget.EditText editText })
        {
            editText.InputType = Android.Text.InputTypes.ClassNumber | Android.Text.InputTypes.NumberFlagSigned;

            // InputType の指定は書体を既定へ戻すため、等幅の指定をあとから掛け直す
            editText.SetTypeface(Android.Graphics.Typeface.Monospace, Android.Graphics.TypefaceStyle.Normal);

            // 枠は外側の Border が描くため、入力欄そのものに付く下線は消す
            editText.BackgroundTintList =
                Android.Content.Res.ColorStateList.ValueOf(Android.Graphics.Color.Transparent);
        }
#endif
    }

    /// <summary>基準領域のトグルの塗りを共有の配色に合わせる。</summary>
    /// <param name="sender">対象のトグル。</param>
    /// <param name="e">未使用。</param>
    /// <remarks>
    /// Android の既定のトラックは半透明で寸法も異なり、指定した配色どおりに出ない。
    /// 状態ごとのトラックとノブを自前で与えて、他ルートと同じ寸法・配色に揃える。
    /// </remarks>
    private void OnVisibleAreaSwitchHandlerChanged(object? sender, EventArgs e)
    {
#if ANDROID
        if (sender is Switch { Handler.PlatformView: AndroidX.AppCompat.Widget.SwitchCompat platformSwitch } toggle)
        {
            ApplySwitchSkin(platformSwitch, toggle);
        }
#endif
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

    /// <summary>区切り線の色の資源キー。トグルの off 側のトラックに使う。</summary>
    private const string DividerColorKey = "SampleDivider";

    /// <summary>トグルのトラックの幅。</summary>
    private const float TrackWidthDp = 46f;

    /// <summary>トグルのトラックの高さ。</summary>
    private const float TrackHeightDp = 28f;

    /// <summary>トグルのノブの直径。</summary>
    private const float ThumbSizeDp = 24f;

    /// <summary>トグルのトラックとノブに、状態ごとの塗りを与える。</summary>
    /// <param name="platformSwitch">塗りを与える platform のトグル。</param>
    /// <param name="toggle">配色の指定元。</param>
    private static void ApplySwitchSkin(AndroidX.AppCompat.Widget.SwitchCompat platformSwitch, Switch toggle)
    {
        Android.Content.Context context = platformSwitch.Context!;
        Android.Graphics.Color offColor = ((Color)Application.Current!.Resources[DividerColorKey]).ToPlatform();

        // 自前の塗りに色合いが重ならないよう、既定の色合いを外す
        platformSwitch.TrackTintList = null;
        platformSwitch.ThumbTintList = null;

        Android.Graphics.Drawables.StateListDrawable track = new();
        track.AddState([Android.Resource.Attribute.StateChecked], SwitchTrack(context, toggle.OnColor.ToPlatform()));
        track.AddState([], SwitchTrack(context, offColor));
        platformSwitch.TrackDrawable = track;

        Android.Graphics.Drawables.GradientDrawable thumb = new();
        thumb.SetShape(Android.Graphics.Drawables.ShapeType.Oval);
        thumb.SetColor(toggle.ThumbColor.ToPlatform());
        thumb.SetSize(Dp(context, ThumbSizeDp), Dp(context, ThumbSizeDp));
        platformSwitch.ThumbDrawable = thumb;

        // 塗りを差し替えたあとの寸法で置き場所を測り直す
        platformSwitch.SwitchMinWidth = Dp(context, TrackWidthDp);
        platformSwitch.ThumbTextPadding = 0;
        toggle.WidthRequest = TrackWidthDp;
        toggle.HeightRequest = TrackHeightDp;
        platformSwitch.RequestLayout();
    }

    /// <summary>トグルのトラックの塗りを作る。</summary>
    /// <param name="context">寸法の変換に使う context。</param>
    /// <param name="color">トラックの色。</param>
    /// <returns>作ったトラックの塗り。</returns>
    private static Android.Graphics.Drawables.GradientDrawable SwitchTrack(
        Android.Content.Context context,
        Android.Graphics.Color color)
    {
        Android.Graphics.Drawables.GradientDrawable drawable = new();
        drawable.SetShape(Android.Graphics.Drawables.ShapeType.Rectangle);
        drawable.SetCornerRadius(Dp(context, TrackHeightDp / 2));
        drawable.SetColor(color);
        drawable.SetSize(Dp(context, TrackWidthDp), Dp(context, TrackHeightDp));
        return drawable;
    }

    /// <summary>密度非依存の寸法を実際のピクセル数へ変換する。</summary>
    /// <param name="context">変換に使う context。</param>
    /// <param name="value">密度非依存の寸法。</param>
    /// <returns>実際のピクセル数。</returns>
    private static int Dp(Android.Content.Context context, float value) =>
        (int)(value * context.Resources!.DisplayMetrics!.Density);
#endif

    /// <summary>戻る導線が押されたときにメニュー画面へ戻る。</summary>
    /// <param name="sender">未使用。</param>
    /// <param name="e">未使用。</param>
    private void OnBackSelected(object? sender, TappedEventArgs e) => GoBack();

    /// <summary>メニュー画面へ戻る。</summary>
    private async void GoBack()
    {
        await Navigation.PopModalAsync();
    }

    /// <summary>調整した属性でダイアログを表示し、結果をパネルとメニューの両方へ出す。</summary>
    private async void OnShowSelected(object? sender, EventArgs e)
    {
        LayoutDialogViewModel viewModel = new(SampleText.LayoutDialogMessage, VisibleAreaSwitch.IsToggled);

        // 置き場所は呼び出しごとに変わるので Show の引数で渡す
        DialogPlacement placement = new()
        {
            HorizontalAlignment = HorizontalSegments.Selection,
            VerticalAlignment = VerticalSegments.Selection,
            OffsetX = Offset(OffsetXEntry.Text),
            OffsetY = Offset(OffsetYEntry.Text),
        };

        DialogResult<bool> result = await Dialog.Instance.ShowAsync(viewModel, placement);
        string text = result switch
        {
            DialogResult<bool>.Completed completed => SampleText.CompletedResult(completed.Value),
            _ => SampleText.CancelledResult,
        };

        ResultValueLabel.Text = text;
        ResultArea.IsVisible = true;
        _onResult(text);
    }

    /// <summary>入力された移動量を読む。数値として読めない入力は 0 として扱う。</summary>
    /// <param name="text">入力中の文字列。</param>
    /// <returns>移動量。</returns>
    private static double Offset(string? text) =>
        double.TryParse(text, NumberStyles.Float, CultureInfo.InvariantCulture, out double value) ? value : 0;
}
