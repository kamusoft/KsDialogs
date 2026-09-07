using System;
using Foundation;
using ObjCRuntime;
using UIKit;

namespace KsDialogs.Bridge;

/// <summary>演出が終わったときに呼ぶ完了通知。</summary>
delegate void MauiDialogTransitionCompletion();

/// <summary>
/// MAUI 側の演出を実行する口。ホスト View と、演出が終わったときに呼ぶ完了通知を受け取る。
/// </summary>
/// <remarks>
/// 完了通知は互換面 (ObjC) の側で作られた block としてこの実行口へ渡ってくる。
/// 名前付きの delegate 型にして <see cref="BlockCallbackAttribute"/> を付けないと、
/// binding 生成器は block を C の関数ポインタとみなして呼び出し、実行時に落ちる。
/// </remarks>
delegate void MauiDialogTransitionRunner(
    UIView hostView,
    [BlockCallback] MauiDialogTransitionCompletion completion);

/// <summary>ダイアログの中身とメタ属性を新規に供給する口。</summary>
/// <remarks>
/// この口は互換面 (ObjC) から呼ばれる。managed / native の境界を例外が越えると未処理の障害になるため、
/// 中身を作れなかった場合は例外を返さず null を返す取り決めにしてある。
/// null を受けた提示は結果を返さずに失敗し、その失敗が閉鎖の通知として届く。
/// </remarks>
[return: NullAllowed]
delegate MauiDialogContent MauiDialogContentProvider();

/// <summary>MAUI 形態のための ObjC 互換面。</summary>
[BaseType(typeof(NSObject), Name = "KSDMauiDialogBridge")]
interface MauiDialogBridge
{
    /// <summary>既定の共有インスタンス。</summary>
    [Static]
    [Export("sharedBridge")]
    MauiDialogBridge Shared { get; }

    /// <summary>中身の View をダイアログとして提示し、閉じたときに通知をちょうど 1 回返す。</summary>
    [Export("presentContentProvider:completion:")]
    MauiDialogPresentation Present(
        MauiDialogContentProvider contentProvider,
        Action<MauiDialogClosure> completion);
}

/// <summary>提示 1 回分の中身と、その中身に効くメタ属性。</summary>
[BaseType(typeof(NSObject), Name = "KSDMauiDialogContent")]
[DisableDefaultCtor]
interface MauiDialogContent
{
    /// <summary>中身と、それに効くメタ属性の組を作る。</summary>
    [Export("initWithView:options:placement:")]
    NativeHandle Constructor(UIView view, MauiDialogOptions options, MauiDialogPlacement placement);

    /// <summary>ダイアログの中身になる View。</summary>
    [Export("view")]
    UIView View { get; }

    /// <summary>その中身に添付された静的メタ属性。</summary>
    [Export("options")]
    MauiDialogOptions Options { get; }

    /// <summary>その提示の置き場所。</summary>
    [Export("placement")]
    MauiDialogPlacement Placement { get; }

    /// <summary>MAUI 側が読み直したメタ属性を、この中身の View の添付面へ写す。</summary>
    [Export("applyAttributesWithOptions:placement:")]
    void ApplyAttributes(MauiDialogOptions options, MauiDialogPlacement placement);

    /// <summary>
    /// MAUI 側の演出をこの中身へ結び付ける。渡さなかった側には器の既定が適用される。
    /// 覆いのフェード時間は秒で渡し、渡さなければ器の既定値が使われる。
    /// </summary>
    [Export("installTransitionWithPresentation:dismissal:overlayDuration:")]
    void InstallTransition(
        [NullAllowed] MauiDialogTransitionRunner presentation,
        [NullAllowed] MauiDialogTransitionRunner dismissal,
        [NullAllowed] NSNumber overlayDuration);

    /// <summary>出現の演出を実行し、終わったら完了を 1 回だけ返す。</summary>
    [Export("runPresentationOnView:completion:")]
    void RunPresentation(UIView hostView, MauiDialogTransitionCompletion completion);

    /// <summary>退出の演出を実行し、終わったら完了を 1 回だけ返す。</summary>
    [Export("runDismissalOnView:completion:")]
    void RunDismissal(UIView hostView, MauiDialogTransitionCompletion completion);
}

/// <summary>中身に添付された静的メタ属性。</summary>
[BaseType(typeof(NSObject), Name = "KSDMauiDialogOptions")]
interface MauiDialogOptions
{
    /// <summary>サイズと位置の計算の基準になる領域。</summary>
    [Export("layoutArea")]
    MauiDialogLayoutArea LayoutArea { get; set; }

    /// <summary>基準 rect の上辺から控除する余白。</summary>
    [Export("marginTop")]
    double MarginTop { get; set; }

    /// <summary>基準 rect の左辺から控除する余白。</summary>
    [Export("marginLeft")]
    double MarginLeft { get; set; }

    /// <summary>基準 rect の下辺から控除する余白。</summary>
    [Export("marginBottom")]
    double MarginBottom { get; set; }

    /// <summary>基準 rect の右辺から控除する余白。</summary>
    [Export("marginRight")]
    double MarginRight { get; set; }

    /// <summary>基準 rect の幅に対する比率。0 以下は未指定。</summary>
    [Export("proportionalWidth")]
    double ProportionalWidth { get; set; }

    /// <summary>基準 rect の高さに対する比率。0 以下は未指定。</summary>
    [Export("proportionalHeight")]
    double ProportionalHeight { get; set; }

    /// <summary>ダイアログの背後を覆う色 (ARGB 32bit)。</summary>
    [Export("overlayColorArgb")]
    int OverlayColorArgb { get; set; }

    /// <summary>外側タップをキャンセルと同じ経路で閉じる操作として扱うか。</summary>
    [Export("isCanceledOnTouchOutside")]
    bool IsCanceledOnTouchOutside { get; set; }
}

/// <summary>その提示の置き場所。</summary>
[BaseType(typeof(NSObject), Name = "KSDMauiDialogPlacement")]
interface MauiDialogPlacement
{
    /// <summary>水平方向の配置。</summary>
    [Export("horizontalAlignment")]
    MauiDialogAlignment HorizontalAlignment { get; set; }

    /// <summary>垂直方向の配置。</summary>
    [Export("verticalAlignment")]
    MauiDialogAlignment VerticalAlignment { get; set; }

    /// <summary>配置を決めた後に加える水平方向の移動量。正の値で右へ動く。</summary>
    [Export("offsetX")]
    double OffsetX { get; set; }

    /// <summary>配置を決めた後に加える垂直方向の移動量。正の値で下へ動く。</summary>
    [Export("offsetY")]
    double OffsetY { get; set; }
}

/// <summary>提示 1 回ごとに 1 度だけ届く閉鎖の通知。</summary>
[BaseType(typeof(NSObject), Name = "KSDMauiDialogClosure")]
[DisableDefaultCtor]
interface MauiDialogClosure
{
    /// <summary>閉じた理由。</summary>
    [Export("kind")]
    MauiDialogClosureKind Kind { get; }

    /// <summary>失敗したときの理由。それ以外では null。</summary>
    [NullAllowed]
    [Export("error")]
    NSError Error { get; }
}

/// <summary>提示したダイアログ 1 枚を閉じるための handle。</summary>
[BaseType(typeof(NSObject), Name = "KSDMauiDialogPresentation")]
interface MauiDialogPresentation
{
    /// <summary>提示した 1 枚を閉じる。</summary>
    [Export("dismiss")]
    void Dismiss();
}

/// <summary>既定ローディングの表示テキストを組み立てる口。</summary>
/// <remarks>進捗が未報告のときは <c>progress</c> が null になる。</remarks>
delegate string MauiLoadingProgressFormat(
    [NullAllowed] string message,
    [NullAllowed] NSNumber progress);

/// <summary>MAUI 側の ViewModel へ進捗を届ける口。</summary>
delegate void MauiLoadingProgressReceiver(double progress);

/// <summary>カスタム Loading の中身とメタ属性を新規に供給する口。</summary>
/// <remarks>
/// この口は互換面 (ObjC) から呼ばれる。managed / native の境界を例外が越えると未処理の障害になるため、
/// 中身を作れなかった場合は例外を返さず null を返す取り決めにしてある。
/// null を受けた開始は成立せず、その失敗が完了の通知として届く。
/// </remarks>
[return: NullAllowed]
delegate MauiDialogContent MauiLoadingContentProvider();

/// <summary>スコープ形の処理が進捗を報告する口。</summary>
delegate void MauiLoadingProgressReport(double progress);

/// <summary>スコープ形の処理が終わったときに呼ぶ完了通知。</summary>
delegate void MauiLoadingActionCompletion();

/// <summary>
/// MAUI 側が持つスコープ形の処理。進捗の報告口と完了通知を受け取る。
/// </summary>
/// <remarks>
/// 報告口も完了通知も互換面 (ObjC) の側で作られた block として渡ってくる。
/// 名前付きの delegate 型にして <see cref="BlockCallbackAttribute"/> を付けないと、
/// binding 生成器は block を C の関数ポインタとみなして呼び出し、実行時に落ちる。
/// </remarks>
delegate void MauiLoadingAction(
    [BlockCallback] MauiLoadingProgressReport report,
    [BlockCallback] MauiLoadingActionCompletion completion);

/// <summary>表示 1 回ごとに 1 度だけ届く完了の通知。失敗したときだけ理由が入る。</summary>
delegate void MauiLoadingCompletion([NullAllowed] NSError error);

/// <summary>MAUI 側で設定された既定ローディングの見た目。</summary>
[BaseType(typeof(NSObject), Name = "KSDMauiLoadingStyle")]
interface MauiLoadingStyle
{
    /// <summary>回転インジケータの色 (ARGB 32bit)。</summary>
    [Export("indicatorColorArgb")]
    int IndicatorColorArgb { get; set; }

    /// <summary>メッセージの文字の大きさ (pt)。</summary>
    [Export("messageFontSize")]
    double MessageFontSize { get; set; }

    /// <summary>メッセージの文字色 (ARGB 32bit)。</summary>
    [Export("messageColorArgb")]
    int MessageColorArgb { get; set; }

    /// <summary>メッセージを省略して表示したときに使う文言。</summary>
    [NullAllowed]
    [Export("defaultMessage")]
    string DefaultMessage { get; set; }

    /// <summary>表示テキストの組み立て方。null なら Native ライブラリの既定が使われる。</summary>
    [NullAllowed]
    [Export("progressFormat")]
    MauiLoadingProgressFormat ProgressFormat { get; set; }
}

/// <summary>表示 1 回分の中身の指定。</summary>
[BaseType(typeof(NSObject), Name = "KSDMauiLoadingContent")]
[DisableDefaultCtor]
interface MauiLoadingContent
{
    /// <summary>既定ローディングの表示を指定する。</summary>
    [Export("initWithMessage:placement:")]
    NativeHandle Constructor([NullAllowed] string message, [NullAllowed] MauiDialogPlacement placement);

    /// <summary>カスタム Loading の表示を指定する。</summary>
    [Export("initWithContentProvider:placement:progressReceiver:")]
    NativeHandle Constructor(
        MauiLoadingContentProvider contentProvider,
        [NullAllowed] MauiDialogPlacement placement,
        [NullAllowed] MauiLoadingProgressReceiver progressReceiver);
}

/// <summary>MAUI 形態の Loading のための ObjC 互換面。</summary>
[BaseType(typeof(NSObject), Name = "KSDMauiLoadingBridge")]
interface MauiLoadingBridge
{
    /// <summary>既定の共有インスタンス。</summary>
    [Static]
    [Export("sharedBridge")]
    MauiLoadingBridge Shared { get; }

    /// <summary>既定ローディングの見た目を設定する。器は各表示の開始時にこの値を読む。</summary>
    [Export("applyStyle:")]
    void ApplyStyle(MauiLoadingStyle style);

    /// <summary>既定ローディングの器メタ属性を設定する。器は各表示の開始時にこの値を読む。</summary>
    [Export("applyOptions:")]
    void ApplyOptions(MauiDialogOptions options);

    /// <summary>合流 1 件を開始して表示する。</summary>
    [Export("showContent:completion:")]
    void Show(MauiLoadingContent content, MauiLoadingCompletion completion);

    /// <summary>合流 1 件を握ったまま MAUI 側の処理を走らせる。</summary>
    [Export("startContent:action:completion:")]
    void Start(MauiLoadingContent content, MauiLoadingAction action, MauiLoadingCompletion completion);

    /// <summary>合流数によらず表示を閉じる。撤去の完了で通知が届く。</summary>
    [Export("hideWithCompletion:")]
    void Hide(MauiLoadingCompletion completion);

    /// <summary>表示中のメッセージを更新する。合流には関与しない。</summary>
    [Export("setLoadingMessage:")]
    void SetMessage([NullAllowed] string message);
}

/// <summary>カスタム Toast の中身とメタ属性を新規に供給する口。</summary>
/// <remarks>
/// この口は互換面 (ObjC) から呼ばれる。managed / native の境界を例外が越えると未処理の障害になるため、
/// 中身を作れなかった場合は例外を返さず null を返す取り決めにしてある。
/// null を受けた表示は、受理後の失敗として Native 側でその 1 枚だけが破棄される。
/// </remarks>
[return: NullAllowed]
delegate MauiDialogContent MauiToastContentProvider();

/// <summary>MAUI 側で設定された Toast の一括設定。</summary>
[BaseType(typeof(NSObject), Name = "KSDMauiToastStyle")]
interface MauiToastStyle
{
    /// <summary>デフォルト View のピルの地色 (ARGB 32bit)。</summary>
    [Export("backgroundColorArgb")]
    int BackgroundColorArgb { get; set; }

    /// <summary>デフォルト View のメッセージの文字色 (ARGB 32bit)。</summary>
    [Export("textColorArgb")]
    int TextColorArgb { get; set; }

    /// <summary>デフォルト View のメッセージの文字の大きさ (pt)。</summary>
    [Export("fontSize")]
    double FontSize { get; set; }

    /// <summary>デフォルト View のピルの角丸半径 (pt)。</summary>
    [Export("cornerRadius")]
    double CornerRadius { get; set; }

    /// <summary>duration を省略した表示に使うミリ秒。</summary>
    [Export("defaultDuration")]
    int DefaultDuration { get; set; }

    /// <summary>アプリ全体の既定配置。null なら Toast の契約既定値が使われる。</summary>
    [NullAllowed]
    [Export("defaultPlacement")]
    MauiDialogPlacement DefaultPlacement { get; set; }
}

/// <summary>表示 1 回分の中身の指定。</summary>
[BaseType(typeof(NSObject), Name = "KSDMauiToastContent")]
[DisableDefaultCtor]
interface MauiToastContent
{
    /// <summary>デフォルト View の表示を指定する。</summary>
    [Export("initWithMessage:duration:placement:")]
    NativeHandle Constructor(
        string message,
        [NullAllowed] NSNumber duration,
        [NullAllowed] MauiDialogPlacement placement);

    /// <summary>カスタム Toast の表示を指定する。</summary>
    [Export("initWithContentProvider:duration:placement:")]
    NativeHandle Constructor(
        MauiToastContentProvider contentProvider,
        [NullAllowed] NSNumber duration,
        [NullAllowed] MauiDialogPlacement placement);
}

/// <summary>MAUI 形態の Toast のための ObjC 互換面。</summary>
[BaseType(typeof(NSObject), Name = "KSDMauiToastBridge")]
interface MauiToastBridge
{
    /// <summary>既定の共有インスタンス。</summary>
    [Static]
    [Export("sharedBridge")]
    MauiToastBridge Shared { get; }

    /// <summary>Toast の一括設定を反映する。器は各表示の受理時にこの値を読む。</summary>
    [Export("applyStyle:")]
    void ApplyStyle(MauiToastStyle style);

    /// <summary>表示 1 枚を受理する。結末の通知は無い。</summary>
    [Export("showContent:")]
    void Show(MauiToastContent content);
}
