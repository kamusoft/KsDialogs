namespace KsDialogs.Sample.Maui;

/// <summary>起動引数から自動再生を指定できるデモの安定 ID。</summary>
/// <remarks>値はメニュー項目と 1 対 1 に対応し、4ルートの Sample が同じ文字列を各自持つ。</remarks>
internal enum SampleDemoId
{
    /// <summary>Basic Dialog。</summary>
    BasicDialog,

    /// <summary>Declarative Dialog。</summary>
    DeclarativeDialog,

    /// <summary>Model Dialog。</summary>
    ModelDialog,

    /// <summary>Text Input Dialog。</summary>
    TextInputDialog,

    /// <summary>Inline Dialog。</summary>
    InlineDialog,

    /// <summary>Transition Dialog。</summary>
    TransitionDialog,

    /// <summary>Layout Dialog。</summary>
    LayoutDialog,

    /// <summary>Default Loading。</summary>
    DefaultLoading,

    /// <summary>Custom Loading。</summary>
    CustomLoading,

    /// <summary>Default Toast。</summary>
    DefaultToast,

    /// <summary>Custom Toast。</summary>
    CustomToast,

    /// <summary>Toast Stack。</summary>
    ToastStack,

    /// <summary>Toast Placement。</summary>
    ToastPlacement,

    /// <summary>Toast Overlap。</summary>
    ToastOverlap,
}

/// <summary>安定デモ ID の外部表現 (起動引数に書く文字列) との対応。</summary>
internal static class SampleDemoIds
{
    /// <summary>外部表現から安定デモ ID を引くための対応表。</summary>
    private static readonly Dictionary<string, SampleDemoId> Values = new()
    {
        ["basic-dialog"] = SampleDemoId.BasicDialog,
        ["declarative-dialog"] = SampleDemoId.DeclarativeDialog,
        ["model-dialog"] = SampleDemoId.ModelDialog,
        ["text-input-dialog"] = SampleDemoId.TextInputDialog,
        ["inline-dialog"] = SampleDemoId.InlineDialog,
        ["transition-dialog"] = SampleDemoId.TransitionDialog,
        ["layout-dialog"] = SampleDemoId.LayoutDialog,
        ["default-loading"] = SampleDemoId.DefaultLoading,
        ["custom-loading"] = SampleDemoId.CustomLoading,
        ["default-toast"] = SampleDemoId.DefaultToast,
        ["custom-toast"] = SampleDemoId.CustomToast,
        ["toast-stack"] = SampleDemoId.ToastStack,
        ["toast-placement"] = SampleDemoId.ToastPlacement,
        ["toast-overlap"] = SampleDemoId.ToastOverlap,
    };

    /// <summary>外部表現から安定デモ ID を引く。</summary>
    /// <param name="value">起動引数で渡された値。</param>
    /// <returns>対応する安定デモ ID。定義外の値なら null。</returns>
    public static SampleDemoId? From(string? value) =>
        value is not null && Values.TryGetValue(value, out SampleDemoId demo) ? demo : null;
}
