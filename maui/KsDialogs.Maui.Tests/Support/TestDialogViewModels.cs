namespace KsDialogs.Maui.Tests.Support;

/// <summary>結果型に <see cref="bool"/> を宣言する検証用 ViewModel。</summary>
/// <param name="Message">ダイアログに出す文言。どの 1 枚かを見分けるために使う。</param>
internal sealed record BooleanTestDialogViewModel(string Message = "テスト") : IDialogViewModel<bool>;

/// <summary>結果型に <see cref="string"/> を宣言する検証用 ViewModel。</summary>
internal sealed record StringTestDialogViewModel(string Message = "テスト") : IDialogViewModel<string>;

/// <summary>View factory を登録しないままにしておく検証用 ViewModel。</summary>
internal sealed record UnregisteredTestDialogViewModel : IDialogViewModel<bool>;

/// <summary>レジストリ共有の検証だけに使う ViewModel。既定のレジストリを他の検証と取り合わないよう型を分ける。</summary>
internal sealed record SharedRegistryTestDialogViewModel : IDialogViewModel<bool>;

/// <summary>結果型を書かない真偽値の顔で宣言した検証用 ViewModel。</summary>
/// <param name="Message">ダイアログに出す文言。どの 1 枚かを見分けるために使う。</param>
internal sealed record SimpleFacedTestDialogViewModel(string Message = "テスト") : IDialogViewModel;

/// <summary>インライン表示だけに使う、View factory を登録しないままの真偽値の顔の ViewModel。</summary>
internal sealed record InlineOnlyTestDialogViewModel(string Message = "テスト") : IDialogViewModel;
