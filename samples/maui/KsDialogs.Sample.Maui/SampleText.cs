namespace KsDialogs.Sample.Maui;

/// <summary>4ルートの Sample が一字一句同じものを表示する文言 (cross/ADR-0007)。</summary>
public static class SampleText
{
    /// <summary>メニュー画面のタイトル。</summary>
    public const string MenuTitle = "KsDialogs Sample";

    /// <summary>デモ項目の文言。メニュー項目の文言が 4 ルートの対応を取る単位になる。</summary>
    public const string BasicDialogItem = "Basic Dialog";

    /// <summary>Basic Dialog が表示するメッセージ (デモデータの初期値)。</summary>
    public const string BasicDialogMessage = "こんにちは、KsDialogs!";

    /// <summary>デモ項目の文言。属性調整パネルの画面タイトルも兼ねる。</summary>
    public const string LayoutDialogItem = "Layout Dialog";

    /// <summary>Layout Dialog が表示するメッセージ。</summary>
    public const string LayoutDialogMessage = "レイアウト確認";

    /// <summary>デモ項目の文言。</summary>
    public const string DeclarativeDialogItem = "Declarative Dialog";

    /// <summary>Declarative Dialog が表示するメッセージ。中身の書き方が違うだけなので Basic Dialog と同じ文言にする。</summary>
    public const string DeclarativeDialogMessage = "こんにちは、KsDialogs!";

    /// <summary>デモ項目の文言。</summary>
    public const string TextInputDialogItem = "Text Input Dialog";

    /// <summary>Text Input Dialog が表示するメッセージ。</summary>
    public const string TextInputDialogMessage = "メッセージを入力してください";

    /// <summary>Text Input Dialog の入力欄の下書き文言。入力の初期値は空。</summary>
    public const string TextInputPlaceholder = "ここに入力";

    /// <summary>デモ項目の文言。</summary>
    public const string InlineDialogItem = "Inline Dialog";

    /// <summary>Inline Dialog が表示するメッセージ。</summary>
    public const string InlineDialogMessage = "インライン表示です";

    /// <summary>デモ項目の文言。トランジションデモ画面の画面タイトルも兼ねる。</summary>
    public const string TransitionDialogItem = "Transition Dialog";

    /// <summary>Transition Dialog が表示するメッセージ。</summary>
    public const string TransitionDialogMessage = "トランジションのデモです";

    /// <summary>デモ項目の文言。</summary>
    public const string ModelDialogItem = "Model Dialog";

    /// <summary>Model Dialog が表示するメッセージ。差は実装経路 — ViewModel 主導の呼び出し — のみ。</summary>
    public const string ModelDialogMessage = "ViewModel から表示しています";

    /// <summary>デモ項目の文言。</summary>
    public const string DefaultLoadingItem = "Default Loading";

    /// <summary>ローディングを開始するときのメッセージ。</summary>
    public const string LoadingStartMessage = "Loading...";

    /// <summary>処理の途中で差し替えるメッセージ。</summary>
    public const string LoadingUpdateMessage = "Soon...";

    /// <summary>デモ項目の文言。</summary>
    public const string CustomLoadingItem = "Custom Loading";

    /// <summary>カスタム Loading の中身に出す見出し。</summary>
    public const string CustomLoadingTitle = "カスタムローディング";

    /// <summary>デモ項目の文言。</summary>
    public const string DefaultToastItem = "Default Toast";

    /// <summary>Default Toast が表示するメッセージ。</summary>
    public const string DefaultToastMessage = "Hello Toast!";

    /// <summary>デモ項目の文言。</summary>
    public const string CustomToastItem = "Custom Toast";

    /// <summary>登録経路のカスタム Toast の中身に出す文言。</summary>
    public const string CustomToastMessage = "カスタムトースト";

    /// <summary>インライン経路のカスタム Toast の中身に出す文言。</summary>
    public const string InlineToastMessage = "インライントースト";

    /// <summary>デモ項目の文言。</summary>
    public const string ToastStackItem = "Toast Stack";

    /// <summary>Toast Stack の 1 枚目のメッセージ。</summary>
    public const string ToastStackFirstMessage = "Toast 1";

    /// <summary>Toast Stack の 2 枚目のメッセージ。</summary>
    public const string ToastStackSecondMessage = "Toast 2";

    /// <summary>Toast Stack の 3 枚目のメッセージ。複数行への折り返しを見せるための長文。</summary>
    public const string ToastStackThirdMessage =
        "Toast 3: 長いメッセージは複数行に折り返され、コンテンツの高さが確保されることを確認する";

    /// <summary>デモ項目の文言。</summary>
    public const string ToastPlacementItem = "Toast Placement";

    /// <summary>Toast Placement が表示するメッセージ。</summary>
    public const string ToastPlacementMessage = "Placed Toast";

    /// <summary>デモ項目の文言。</summary>
    public const string ToastOverlapItem = "Toast Overlap";

    /// <summary>Toast Overlap が表示するメッセージ。</summary>
    public const string ToastOverlapMessage = "Overlap Toast";

    /// <summary>Toast Overlap の時系列が終わったときの結果表示。結果値を持たないので固定の文言にする。</summary>
    public const string ToastOverlapCompletedResult = "結果: 完了";

    /// <summary>演出の選び方をまとめた区画の見出し。</summary>
    public const string PresetCaption = "プリセット";

    /// <summary>時間とイージングをまとめた区画の見出し。</summary>
    public const string AdjustCaption = "調整";

    /// <summary>演出の選択肢 (透明度で出入りする)。</summary>
    public const string TransitionFade = "Fade";

    /// <summary>演出の選択肢 (下辺から出入りする)。</summary>
    public const string TransitionSlideUp = "Slide Up";

    /// <summary>演出の選択肢 (上辺から出入りする)。</summary>
    public const string TransitionSlideDown = "Slide Down";

    /// <summary>演出の選択肢 (前端から出入りする)。</summary>
    public const string TransitionSlideStart = "Slide Start";

    /// <summary>演出の選択肢 (後端から出入りする)。</summary>
    public const string TransitionSlideEnd = "Slide End";

    /// <summary>演出の選択肢 (縮小から等倍へ広がる)。</summary>
    public const string TransitionZoom = "Zoom";

    /// <summary>演出の選択肢 (中身の演出なし)。</summary>
    public const string TransitionNone = "None";

    /// <summary>演出の選択肢 (自作のフックを実演する)。</summary>
    public const string TransitionCustomHook = "Custom Hook";

    /// <summary>演出の時間を調整する行の項目名。</summary>
    public const string DurationLabel = "時間";

    /// <summary>演出のイージングを選ぶ行の項目名。</summary>
    public const string EasingLabel = "イージング";

    /// <summary>イージングの選択肢 (加速して減速する)。</summary>
    public const string EasingStandard = "Standard";

    /// <summary>イージングの選択肢 (等速)。</summary>
    public const string EasingLinear = "Linear";

    /// <summary>イージングの選択肢 (加速する)。</summary>
    public const string EasingAccelerate = "Accelerate";

    /// <summary>イージングの選択肢 (減速する)。</summary>
    public const string EasingDecelerate = "Decelerate";

    /// <summary>トランジションデモ画面の表示操作。</summary>
    public const string DisplayAction = "表示";

    /// <summary>水平方向の配置を選ぶ行の項目名。</summary>
    public const string HorizontalLabel = "Horizontal";

    /// <summary>垂直方向の配置を選ぶ行の項目名。</summary>
    public const string VerticalLabel = "Vertical";

    /// <summary>配置の選択肢 (前端)。</summary>
    public const string AlignmentStart = "Start";

    /// <summary>配置の選択肢 (中央)。</summary>
    public const string AlignmentCenter = "Center";

    /// <summary>配置の選択肢 (後端)。</summary>
    public const string AlignmentEnd = "End";

    /// <summary>水平方向の移動量を入れる行の項目名。</summary>
    public const string OffsetXLabel = "OffsetX";

    /// <summary>垂直方向の移動量を入れる行の項目名。</summary>
    public const string OffsetYLabel = "OffsetY";

    /// <summary>基準領域に可視領域を使うかを切り替える行の項目名。</summary>
    public const string UseVisibleAreaLabel = "Use visible area";

    /// <summary>属性調整パネルの表示操作。</summary>
    public const string ShowAction = "Show";

    /// <summary>完了操作の表示。</summary>
    public const string CompleteAction = "OK";

    /// <summary>キャンセル操作の表示。</summary>
    public const string CancelAction = "キャンセル";

    /// <summary>結果表示エリアの見出し。</summary>
    public const string ResultCaption = "直近の結果";

    /// <summary>キャンセルの結果表示。</summary>
    public const string CancelledResult = "結果: cancelled";

    /// <summary>ローディングの処理が終わったときの結果表示。結果値を持たないので書式ではなく固定の文言にする。</summary>
    public const string LoadingCompletedResult = "結果: 完了";

    /// <summary>演出の時間の表示 (ミリ秒)。</summary>
    /// <param name="milliseconds">片道の時間。</param>
    /// <returns>調整部に出す文言。</returns>
    public static string DurationValue(int milliseconds) => $"{milliseconds} ms";

    /// <summary>カスタム Loading の中身に出す進捗の百分率 (小数なし)。</summary>
    /// <param name="progress">0〜1 に丸めた後の進捗値。</param>
    /// <returns>カスタム View に出す文言。</returns>
    public static string ProgressPercentage(double progress) =>
        $"{(int)Math.Round(progress * 100, MidpointRounding.AwayFromZero)}%";

    /// <summary>完了の結果表示 (真偽値の結果)。</summary>
    /// <param name="value">完了操作が報告した結果値。</param>
    /// <returns>結果表示エリアに出す文言。</returns>
    public static string CompletedResult(bool value) => $"結果: completed({(value ? "true" : "false")})";

    /// <summary>完了の結果表示 (文字列の結果)。値は引用符で囲んで真偽値と見分けられるようにする。</summary>
    /// <param name="value">完了操作が報告した結果値。</param>
    /// <returns>結果表示エリアに出す文言。</returns>
    public static string CompletedResult(string value) => $"結果: completed(\"{value}\")";
}
