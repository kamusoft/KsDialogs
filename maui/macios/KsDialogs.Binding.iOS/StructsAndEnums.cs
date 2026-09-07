using ObjCRuntime;

namespace KsDialogs.Bridge;

/// <summary>ダイアログが閉じた理由の判別。</summary>
[Native]
public enum MauiDialogClosureKind : long
{
    /// <summary>利用者の操作 (キャンセル・外側タップ) で閉じた。</summary>
    Cancelled = 0,

    /// <summary>呼び出し側からの閉鎖要求で閉じた。</summary>
    Dismissed = 1,

    /// <summary>提示できる画面が無く、提示に入れなかった。</summary>
    PresentationHostUnavailable = 2,

    /// <summary>それ以外の理由で提示できなかった。理由は error に入る。</summary>
    Failed = 3,
}

/// <summary>ダイアログを基準領域のどこへ寄せるか。</summary>
[Native]
public enum MauiDialogAlignment : long
{
    /// <summary>有効領域の前端 (左 / 上) に寄せる。</summary>
    Start = 0,

    /// <summary>有効領域の中央に置く。</summary>
    Center = 1,

    /// <summary>有効領域の後端 (右 / 下) に寄せる。</summary>
    End = 2,

    /// <summary>位置だけでなくサイズも有効領域いっぱいに広げる。</summary>
    Fill = 3,
}

/// <summary>サイズと位置の計算を行う基準領域。</summary>
[Native]
public enum MauiDialogLayoutArea : long
{
    /// <summary>ダイアログを載せるウィンドウの全体。</summary>
    Window = 0,

    /// <summary>ウィンドウからシステムバーなどが占める余白を控除した可視領域。</summary>
    VisibleArea = 1,
}
