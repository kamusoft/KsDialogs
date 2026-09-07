# AiForms.Maui.Dialogs からの API 対応表

以下の表は AiForms.Maui.Dialogs の README が掲載する利用者向け API と、同じ公開型が追加で露出するメンバーを対象にする。各旧メンバーは一度だけ現れる。「直接の対応先なし」は旧抽象が廃止されたことを表し、最後の列に移行方法を示す。KsDialogs.Maui の完全なレシピは `ksdialogs-maui` Skill を使う。

## package setup と IoC 設定を置き換える

| 旧メンバー | 新しい対応先または状態 | 移行方法 |
|---|---|---|
| `AiForms.Maui.Dialogs` package | `KsDialogs.Maui` package | package reference を置き換え、`AiForms.Dialogs` import を `KsDialogs` に変える |
| `Dialog.Instance` / `IDialog` | `Dialog.Instance` / `IKsDialog` | 既定エントリを維持するか、新しい契約を注入する |
| `Loading.Instance` / `ILoading` | `Loading.Instance` / `IKsLoading` | 既定エントリを維持するか、新しい契約を注入する |
| `Toast.Instance` / `IToast` | `Toast.Instance` / `IKsToast` | 新しい型は obsolete ではなく、message 通知も提供する |
| `Configurations.LoadingConfig` | `Loading.Instance.Style` と `Loading.Instance.Options` | Loading の視覚 style と container options に分ける |
| `Configurations.SetIocConfig(viewTypeGetter, viewResolver)` | `RegisterForDialog`、または `options.UseViewFallback(...)` と `options.UseViewModelFallback()` を伴う `AddKsDialogs` | 明示登録を優先し、命名規約による解決で MAUI DI から View や ViewModel を得る場合だけ fallback を使う |
| `LoadingConfig` | `LoadingStyle` と `DialogOptions` | 旧来の複合 settings object を、役割を絞った 2 つの値へ置き換える |

`RegisterForDialog<TView, TViewModel>()` は View factory と ViewModel factory の 2 スロットを 1 回で配線し、両方の型を transient としてサービス登録する。結果型を宣言する ViewModel には `RegisterForDialog<TView, TViewModel, TResult>()` を使う。`RegisterForLoading<TView, TViewModel>()` と `RegisterForToast<TView, TViewModel>()` は Loading / Toast のレジストリに対する同じ糖衣で、こちらも 2 スロットを配線するため、この 1 行だけで ViewModel の instance を渡す表示と型を渡す表示の双方が使える。Loading / Toast は結果を返さないので、結果型を取る形はどちらにもない。

登録と fallback は process 全体のレジストリ (`DialogViewRegistry.Shared`、`LoadingViewRegistry.Shared`、`ToastViewRegistry.Shared`) に載り、`Dialog.Instance.Registry`・`Loading.Instance.Registry`・`Toast.Instance.Registry` からも辿れる。`AddKsDialogs` の fallback は Dialog のレジストリだけの機構で、Loading / Toast は明示登録か 1 行登録のみを受け付け、解決できない型はそこで失敗する。明示登録は fallback より優先され、`AddKsDialogs` の再呼び出しは設定したスロットだけを合成する (引数なしの呼び出しは設定済みの fallback を消さない)。設定した fallback を公開 API から解除する手段はない。DI を使う登録と fallback 解決には、MAUI startup で捕捉する application service provider が必要である。それより前にこの経路を呼ぶと、無関係な生成経路へ切り替わらず `DialogException.ServiceProviderUnavailable` で失敗する。

```csharp
using KsDialogs;
using Microsoft.Extensions.DependencyInjection;

namespace MyApp;

public static class MauiProgram
{
    public static MauiApp CreateMauiApp()
    {
        MauiAppBuilder builder = MauiApp.CreateBuilder();
        builder.UseMauiApp<App>();

        builder.Services
            .AddKsDialogs(options => options.UseViewModelFallback())
            .RegisterForDialog<ConfirmDialogView, ConfirmViewModel>()
            .RegisterForLoading<UploadLoadingView, UploadViewModel>()
            .RegisterForToast<NoticeToastView, NoticeViewModel>();

        Loading.Instance.Style = Loading.Instance.Style with
        {
            IndicatorColor = Colors.White,
            MessageFontSize = 14d,
            DefaultMessage = "Working",
        };
        Loading.Instance.Options = Loading.Instance.Options with
        {
            OverlayColor = Color.FromRgba(0, 0, 0, 128),
        };

        return builder.Build();
    }
}
```

## Dialog の表示と再利用を移行する

| 旧メンバー | 新しい対応先または状態 | 移行方法 |
|---|---|---|
| `IDialog.ShowAsync<TView>(object viewModel = null)` | factory 登録後の `IKsDialog.ShowAsync(viewModel)` | ViewModel から View への factory を登録する。ViewModel が別の結果型を宣言しない限り結果は `DialogResult<bool>` |
| `IDialog.ShowAsync(DialogView view, object viewModel = null)` | inline の `IKsDialog.ShowAsync(viewModel, factory)` | 呼び出しスコープの factory から新しい通常の MAUI `View` を返す |
| `IDialog.ShowAsync(object viewModel)` | `IKsDialog.ShowAsync<TResult>(IDialogViewModel<TResult>)` | 新しい型付き ViewModel 契約を実装し、View factory を登録する |
| `IDialog.ShowFromModelAsync<TViewModel>()` | `IKsDialog.ShowAsync<TViewModel>()` | View と ViewModel の factory を登録して型指定 overload を使う |
| `IDialog.ShowFromModelAsync<TViewModel, TParameter>(TParameter parameter)` | `IKsDialog.ShowAsync<TViewModel>(configure)` | 同期または非同期の configure callback で parameter を適用する |
| `IDialog.ShowResultAsync<TView, TResult>(object viewModel = null)` | factory 登録後の `IKsDialog.ShowAsync<TResult>(viewModel)` | ViewModel に `TResult` を宣言し、`DialogResult<TResult>` を判別する |
| `IDialog.ShowResultAsync<TResult>(object viewModel)` | `IKsDialog.ShowAsync<TResult>(viewModel)` | 結果型は呼び出しだけでなく `IDialogViewModel<TResult>` に移る |
| `IDialog.ShowResultFromModelAsync<TViewModel, TParameter, TResult>(TParameter parameter)` | `IKsDialog.ShowAsync<TViewModel, TResult>(configure)` | 両 factory を登録し、提示前に parameter を適用する |
| `IDialog.Create<TView>(object viewModel = null)` | 直接の対応先なし | 再利用 Dialog の所有をやめ、factory を登録して表示ごとに `ShowAsync` を呼ぶ |
| `IDialog.Create(DialogView view, object viewModel = null)` | 直接の対応先なし | 一度限りなら inline factory、繰り返すなら登録済み factory を使う |
| `IReusableDialog.ShowAsync()` | 直接の対応先なし | 新しい ViewModel で通常の `ShowAsync` 経路を呼ぶ |
| `IReusableDialog.ShowResultAsync<TResult>()` | 直接の対応先なし | 型付き `ShowAsync` を呼び、`DialogResult<TResult>` を処理する |
| `IReusableDialog.Dispose()` | 直接の対応先なし | 再利用 Dialog の手動 dispose を削除する。KsDialogs は show ごとに新しい content を作る |

`DialogViewRegistry` は View と ViewModel の factory slot を持つ。instance 渡しの show の前に View factory を登録し、型指定 show の前には ViewModel factory (`RegisterViewModel` または `RegisterForDialog`) も登録する。View factory が無い場合は `DialogException.ViewFactoryNotRegistered`、ViewModel factory と fallback の両方が無い場合は `DialogException.ViewModelFactoryNotRegistered`、提示先の画面が無い場合は `DialogException.PresentationHostUnavailable` で失敗する。いずれも faulted Task として届き、cancelled result には変換されない。

`ShowResultFromModelAsync<TViewModel, TParameter, TResult>` の書き換え — 結果型は ViewModel が宣言し、報告は notifier で行う:

```csharp
public sealed class EditViewModel : IDialogViewModel<string>
{
    public string Text { get; set; } = string.Empty;

    public void Submit() => this.Notifier?.Complete(Text);

    public void Dismiss() => this.Notifier?.Cancel();
}
```

対を一度登録したら、型指定 show を呼び、旧 parameter は configure callback で適用する:

```csharp
builder.Services.RegisterForDialog<EditDialogView, EditViewModel, string>();

DialogResult<string> result = await Dialog.Instance.ShowAsync<EditViewModel, string>(
    viewModel => viewModel.Text = "initial");

string text = result is DialogResult<string>.Completed completed
    ? completed.Value
    : string.Empty;
```

`ShowAsync(DialogView view, object viewModel)` の書き換え — 生成済みの View ではなく factory を呼び出しに渡す。inline 経路はレジストリの状態を変えない:

```csharp
DialogResult<bool> result = await Dialog.Instance.ShowAsync(
    new ConfirmViewModel("Delete this item?"),
    (viewModel, notifier) => new ConfirmContentView(viewModel, notifier));
```

## ViewModel と結果報告を移行する

| 旧メンバー | 新しい対応先または状態 | 移行方法 |
|---|---|---|
| `IDialogViewModel<T>.DialogInitializeAsync(T parameter)` | `IDialogViewModel<TResult>` と型指定 `ShowAsync(configure)` | `T` は結果型の宣言に変わる。初期化は configure callback へ移す |
| `IDialogViewModelDestroy.Destroy()` | 直接の対応先なし | 旧 lifecycle interface を削除し、show を待ち終えた後にアプリ所有 resource を解放する |
| `IDialogNotifier.Complete()` | `DialogNotifier<bool>.Complete(true)` | factory に渡る notifier または拡張 property `viewModel.Notifier` を使う |
| `IDialogNotifier.Complete<T>(T result)` | `DialogNotifier<TResult>.Complete(TResult value)` | `IDialogViewModel<TResult>` に同じ結果型を宣言する |
| `IDialogNotifier.Cancel()` | `DialogNotifier<TResult>.Cancel()` | cancel は `DialogResult<TResult>.Cancelled` になる |
| `DialogNotifier` | `DialogNotifier<TResult>` | notifier は型付きで、1 回の show に限定される |
| `DialogView.DialogNotifier` | factory の notifier または `viewModel.Notifier` | View の notifier property を bind しない |

OK とキャンセルだけを返す ViewModel は、型引数のない `IDialogViewModel` (= `IDialogViewModel<bool>`) を宣言して結果型の記述を省ける。`viewModel.Notifier` はその instance を表示している間だけ notifier を返し、show の前後は `null` になる。ViewModel 自身から引く場合は `this.Notifier` と書く。

ViewModel は class にする。登録と型指定 show は値型をコンパイル時に弾き、instance 渡しの show は `DialogException.ValueTypeViewModel` で拒否する。同じ表示中 instance で 2 回目の show を開始すると、最初の show の notifier identity を保つため `DialogException.ViewModelAlreadyShowing` で失敗する。

## 既定 Loading と custom Loading を移行する

旧 reusable Loading object は自身の表示を所有していたが、KsDialogs.Maui に同じ所有権を持つ handle はない。重なった呼び出しは 1 つの表示に合流し、最初の開始から最後の終了まで続く。`IKsLoading.HideAsync()` は合流利用数によらず process 全体で共有する現在の Loading generation を閉じる一方、すでに走行中の action はキャンセルしない。そのため機械的に置き換えると、別の処理が所有する Loading まで隠し得る。旧 show/hide の組が処理を囲む場合は、処理スコープ付きの `IKsLoading.StartAsync` 経路を優先する。

| 旧メンバー | 新しい対応先または状態 | 移行方法 |
|---|---|---|
| `ILoading.StartAsync(action, message, isCurrentScope)` | `IKsLoading.StartAsync(action, message, placement)` | `isCurrentScope` を削除し、位置を変えるときは placement を使う |
| `ILoading.Show(message, isCurrentScope)` | `IKsLoading.ShowAsync(message, placement)` | 新しい提示開始を await し、`isCurrentScope` を削除する |
| `ILoading.Hide()` | `IKsLoading.HideAsync()` | dismissal と撤去を await する |
| `ILoading.SetMessage(message)` | `IKsLoading.SetMessage(message)` | 名前は同じ。表示中の built-in Loading content に効く |
| `ILoading.Create<TView>(object viewModel = null)` | 直接の対応先なし | custom Loading factory (`RegisterForLoading` または `Loading.Instance.Registry.Register<TViewModel>`) を登録し、その ViewModel を `ShowAsync` または `StartAsync` に渡す |
| `ILoading.Create(LoadingView view, object viewModel = null)` | 直接の対応先なし | inline factory、または新しい通常の MAUI `View` を返す登録済み factory を使う |
| `ILoading.Create(object viewModel)` | 直接の対応先なし | `ILoadingViewModel` を実装して factory を登録し、新しいエントリに instance を渡す |
| `ILoading.CreateFromModel<TViewModel>()` | `IKsLoading.ShowAsync<TViewModel>(configure, placement)` または `StartAsync<TViewModel>(action, configure, placement)` | `RegisterForLoading` で 2 スロットを配線し、ViewModel の型を渡す。instance は登録済みの ViewModel factory が解決する |
| debug 限定の `ILoading.Dispose()` | 直接の対応先なし | test 限定 dispose 呼び出しを削除する。新しいエントリに public disposal 契約はない |
| `IReusableLoading.Show(bool isCurrentScope = false)` | `IKsLoading.ShowAsync(viewModel, placement)` | 登録済み custom content を表示する。再利用 handle はない |
| `IReusableLoading.StartAsync(action, bool isCurrentScope = false)` | `IKsLoading.StartAsync(viewModel, action, placement)` | 再利用 handle を保持せず、表示を処理のスコープに合わせる |
| `IReusableLoading.Hide()` (公開 interface と実装では `Task`) | 所有 handle として直接同等の対応先なし。`IKsLoading.HideAsync()` は共有中の現在 generation に作用する | 機械的に置き換えない。`IKsLoading.StartAsync` を優先し、共有表示を明示的に閉じる意図がある場合だけ `IKsLoading.HideAsync()` を await する。旧 README の `void` 表記は誤り |
| `IReusableLoading.Dispose()` | 直接の対応先なし | 再利用 handle の dispose を削除する |

custom Loading の ViewModel は `ILoadingViewModel` を実装し、進捗を受け取るには `ILoadingProgressReceiver` も実装する:

```csharp
public sealed class UploadViewModel : ILoadingViewModel, ILoadingProgressReceiver
{
    public string Title { get; set; } = string.Empty;

    public double Progress { get; private set; }

    public void OnProgress(double progress) => Progress = progress;
}
```

再利用 custom Loading handle の書き換え — 表示は処理のスコープに合う。ViewModel 型が解決できれば提示先が無くても action は実行され、未登録の型なら action の開始前に失敗する:

```csharp
await Loading.Instance.StartAsync(
    new UploadViewModel(),
    async progress =>
    {
        progress.Report(0.5);
        await UploadAsync();
        progress.Report(1);
    });
```

`CreateFromModel<TViewModel>` の書き換え — ViewModel の型を渡し、登録済みの ViewModel factory が DI から instance を解決する。`configure` は中身の View を作る前に完了する。ViewModel factory が未登録なら `DialogException.ViewModelFactoryNotRegistered` で失敗し、これは View factory 未登録とは別の失敗である。すでに出ている表示に合流した呼び出しでも ViewModel の生成と `configure` は行われるが、その instance は画面に出ない:

```csharp
builder.Services.RegisterForLoading<UploadLoadingView, UploadViewModel>();

await Loading.Instance.StartAsync<UploadViewModel>(
    async progress => await UploadAsync(progress),
    viewModel => viewModel.Title = "Uploading");
```

## LoadingConfig の値を移す

| 旧メンバー | 新しい対応先または状態 | 移行方法 |
|---|---|---|
| `LoadingConfig.OffsetX` | `DialogPlacement.OffsetX` | 呼び出しごとに placement を渡す。Loading の global offset はない |
| `LoadingConfig.OffsetY` | `DialogPlacement.OffsetY` | 呼び出しごとに placement を渡す。Loading の global offset はない |
| `LoadingConfig.IndicatorColor` | `LoadingStyle.IndicatorColor` | `Loading.Instance.Style` から設定する |
| `LoadingConfig.FontSize` | `LoadingStyle.MessageFontSize` | `Loading.Instance.Style` から設定する |
| `LoadingConfig.FontColor` | `LoadingStyle.MessageColor` | `Loading.Instance.Style` から設定する |
| `LoadingConfig.OverlayColor` | `DialogOptions.OverlayColor` | `Loading.Instance.Options` から設定する |
| `LoadingConfig.Opacity` | 単一の対応先なし | 旧値は overlay、indicator、message をまとめて透過した。近似する場合は同じ alpha を `DialogOptions.OverlayColor`、`LoadingStyle.IndicatorColor`、`LoadingStyle.MessageColor` へ反映する。overlay だけを変えると indicator と message は不透明のままになる |
| `LoadingConfig.DefaultMessage` | `LoadingStyle.DefaultMessage` | `Loading.Instance.Style` から設定する |
| `LoadingConfig.ProgressMessageFormat` | `LoadingStyle.ProgressFormat` | format string を `Func<string?, double?, string>` の delegate に置き換える。組み込みは `LoadingStyle.DefaultProgressFormat` |
| `LoadingConfig.IsReusable` | 直接の対応先なし | View reuse を削除する。重なった Loading 呼び出しは app content を再利用せず、1 つの native 表示へ合流する |

`LoadingStyle` と `DialogOptions` は record なので `with` 式で一部だけ差し替えられる。器はどちらも各表示の開始時に読むため、変更は次の表示から効き、表示中のものには効かない。`IsCanceledOnTouchOutside` は Loading では効かない — 表示が閉じるのは `HideAsync` か、合流した処理の終了によるものだけである。

## ExtraView の layout と lifecycle を移す

旧来の見た目を保つ場合は、変わった3つの既定値を確認する。overlay は透明から黒 40%、dialog margin は 0 から全辺 24、layout area は window 全体から visible area に変わる。

| 旧メンバー | 新しい対応先または状態 | 移行方法 |
|---|---|---|
| `ExtraView` | 通常の MAUI `View` と `Dialog` 添付 property | library 基底 class からの Dialog content の継承をやめる |
| `ExtraView.ProportionalWidth` | `Dialog.SetProportionalWidth` | 通常の MAUI content View に値を添付する |
| `ExtraView.ProportionalWidthProperty` | `Dialog.ProportionalWidthProperty` | XAML style またはコードの直接 `BindableProperty` 参照を新しい添付 property に置き換える |
| `ExtraView.ProportionalHeight` | `Dialog.SetProportionalHeight` | 通常の MAUI content View に値を添付する |
| `ExtraView.ProportionalHeightProperty` | `Dialog.ProportionalHeightProperty` | XAML style またはコードの直接 `BindableProperty` 参照を新しい添付 property に置き換える |
| `ExtraView.VerticalLayoutAlignment` | `DialogAlignment` を使う `Dialog.SetVerticalAlignment` | content View に placement を添付する |
| `ExtraView.VerticalLayoutAlignmentProperty` | `DialogAlignment` を使う `Dialog.VerticalAlignmentProperty` | 直接 `BindableProperty` 参照を置き換え、改名された添付 property を使う |
| `ExtraView.HorizontalLayoutAlignment` | `DialogAlignment` を使う `Dialog.SetHorizontalAlignment` | content View に placement を添付する |
| `ExtraView.HorizontalLayoutAlignmentProperty` | `DialogAlignment` を使う `Dialog.HorizontalAlignmentProperty` | 直接 `BindableProperty` 参照を置き換え、改名された添付 property を使う |
| `ExtraView.OffsetX` | `Dialog.SetOffsetX` | content View に placement を添付する |
| `ExtraView.OffsetXProperty` | `Dialog.OffsetXProperty` | XAML style またはコードの直接 `BindableProperty` 参照を新しい添付 property に置き換える |
| `ExtraView.OffsetY` | `Dialog.SetOffsetY` | content View に placement を添付する |
| `ExtraView.OffsetYProperty` | `Dialog.OffsetYProperty` | XAML style またはコードの直接 `BindableProperty` 参照を新しい添付 property に置き換える |
| `ExtraView.CornerRadius` | library の対応先なし | content View 自身で角丸を描く |
| `ExtraView.CornerRadiusProperty` | 直接の対応先なし | library の `BindableProperty` 参照を削除し、content View 自身で角丸を style する |
| `ExtraView.BorderColor` | library の対応先なし | content View 自身で border を描く |
| `ExtraView.BorderColorProperty` | 直接の対応先なし | library の `BindableProperty` 参照を削除し、content View 自身で border color を style する |
| `ExtraView.BorderWidth` | library の対応先なし | content View 自身で border を描く |
| `ExtraView.BorderWidthProperty` | 直接の対応先なし | library の `BindableProperty` 参照を削除し、content View 自身で border width を style する |
| `ExtraView.AutoRotateForIOS` | 直接の対応先なし | 削除する。回転は host に従い、Dialog は再 layout される |
| `ExtraView.AutoRotateForIOSProperty` | 直接の対応先なし | library の `BindableProperty` 参照を削除する。回転は host に従い、Dialog は再 layout される |
| `ExtraView.DialogMargin` | `Thickness` を使う `Dialog.SetDialogMargin` | content View に margin を添付する |
| `ExtraView.DialogMarginProperty` | `Dialog.DialogMarginProperty` | XAML style またはコードの直接 `BindableProperty` 参照を新しい添付 property に置き換える |
| `ExtraView.RunPresentationAnimation()` | presentation hook または preset を持つ `Dialog.SetTransition` | presentation animation を `DialogTransition` へ移す |
| `ExtraView.RunDismissalAnimation()` | dismissal hook または preset を持つ `Dialog.SetTransition` | dismissal animation を `DialogTransition` へ移す |
| `ExtraView.Destroy()` | 直接の対応先なし | View lifecycle override を削除する。content は表示ごとに新しく作られる |

alignment の行が使う `DialogAlignment` は `Start`・`Center`・`End`・`Fill` を持つ。`Start` と `End` は軸の物理的な前端 / 後端を指し、右から左へ読む環境でも入れ替わらない。`Center` は有効領域の中央に置き、`Fill` は位置だけでなくサイズも有効領域いっぱいに広げる — 比率サイズが指定されている軸では `Fill` は `Center` として扱われる。

animation の2行はいずれも、新しい hook が content View を受け取る `Func<VisualElement, Task>` になり、`DialogTransition(presentation, dismissal, overlayDuration)` が両 hook と overlay の fade 時間 (`OverlayDuration`) を運ぶ。preset の `DialogTransition.Fade(duration, easing)`、`DialogTransitionEdge` を取る `DialogTransition.Slide(from, duration, easing)`、`DialogTransition.Zoom(duration, easing)`、`DialogTransition.None()` は埋まった組を返すので、片側だけ preset にしたい場合は `.Presentation` または `.Dismissal` を取り出して自作 hook と組み合わせる。`DialogTransitionEdge` は `Top`・`Bottom`・`Start`・`End` を持ち、`Start` と `End` はレイアウト方向に追随して右から左へ読む環境では入れ替わり、`Top` と `Bottom` は物理方向のまま変わらない。KsDialogs は戻された `Task` の完了まで待ち、暗黙 timeout を持たない。dismissal の報告後、Dialog result は dismissal hook と overlay の撤去が完了してから配送される。transition の値は closure を持つため、XAML ではなく code-behind から添付する (`Dialog.SetTransition` / `Dialog.GetTransition` / `Dialog.TransitionProperty`)。

移した設定は code-behind で content View に添付する:

```csharp
View content = new ConfirmContentView(viewModel, notifier);

Dialog.SetLayoutArea(content, DialogLayoutArea.Window);
Dialog.SetDialogMargin(content, new Thickness(16d));
Dialog.SetProportionalWidth(content, 0.9d);
Dialog.SetIsCanceledOnTouchOutside(content, false);
Dialog.SetOverlayColor(content, Color.FromRgba(0, 0, 0, 102));
Dialog.SetTransition(content, DialogTransition.Slide(DialogTransitionEdge.Bottom));
```

layout 属性は content View 自身の XAML にも書ける:

```xml
<ContentView xmlns="http://schemas.microsoft.com/dotnet/2021/maui"
             xmlns:x="http://schemas.microsoft.com/winfx/2009/xaml"
             xmlns:ksd="clr-namespace:KsDialogs;assembly=KsDialogs.Maui"
             x:Class="MyApp.ConfirmContentView"
             ksd:Dialog.OverlayColor="#80000000"
             ksd:Dialog.ProportionalWidth="0.9"
             ksd:Dialog.VerticalAlignment="End">
</ContentView>
```

## DialogView の挙動を移す

| 旧メンバー | 新しい対応先または状態 | 移行方法 |
|---|---|---|
| `DialogView` | Dialog factory が返す通常の MAUI `View` | library View 型からの Dialog content の継承をやめる |
| `DialogView.IsCanceledOnTouchOutside` | `Dialog.SetIsCanceledOnTouchOutside` | 通常の MAUI content View に添付する |
| `DialogView.IsCanceledOnTouchOutsideProperty` | `Dialog.IsCanceledOnTouchOutsideProperty` | XAML style またはコードの直接 `BindableProperty` 参照を新しい添付 property に置き換える |
| `DialogView.OverlayColor` | `Dialog.SetOverlayColor` | 通常の MAUI content View に添付する |
| `DialogView.OverlayColorProperty` | `Dialog.OverlayColorProperty` | XAML style またはコードの直接 `BindableProperty` 参照を新しい添付 property に置き換える |
| `DialogView.UseCurrentPageLocation` | `Dialog.SetLayoutArea` と `DialogLayoutArea.VisibleArea` または `Window` | `true` は `DialogLayoutArea.VisibleArea`、`false` は `DialogLayoutArea.Window` へ対応させる。新しい既定は `VisibleArea` |
| `DialogView.UseCurrentPageLocationProperty` | `Dialog.LayoutAreaProperty` と `DialogLayoutArea.VisibleArea` または `Window` | 旧 bool `BindableProperty` を置き換え、値は `DialogView.UseCurrentPageLocation` と同じ規則で対応させる |
| `DialogView.DialogNotifierProperty` | 直接の対応先なし | View に bind した `BindableProperty` を削除し、factory notifier または `viewModel.Notifier` を使う |
| `DialogView.SetUp()` | 直接の対応先なし | factory で新しい View を初期化するか、提示前に ViewModel を configure する |
| `DialogView.TearDown()` | 直接の対応先なし | 再利用 View の reset 処理を削除し、通常の resource cleanup を使う |

`ShowAsync` に `DialogPlacement` を渡すと、添付された placement object — `HorizontalAlignment`・`VerticalAlignment`・`OffsetX`・`OffsetY` — がまるごと置換される。したがって `Dialog.SetOffsetY` だけを添付していても、placement 引数に合成されることはない。上の表の静的な属性には show 引数が無く、添付だけで供給する。

## LoadingView の挙動を移す

| 旧メンバー | 新しい対応先または状態 | 移行方法 |
|---|---|---|
| `LoadingView` | Loading factory が返す通常の MAUI `View` | custom Loading の state を `ILoadingViewModel` に置く |
| `LoadingView.Progress` | `ILoadingProgressReceiver.OnProgress(double)` | progress state を custom Loading の ViewModel に置く |
| `LoadingView.ProgressProperty` | 直接の対応先なし | View 所有の `BindableProperty` を削除し、`ILoadingProgressReceiver.OnProgress(double)` で進捗を受けて bind 可能な ViewModel state を公開する |
| `LoadingView.OverlayColor` | `Dialog.SetOverlayColor` | custom content View に添付する。built-in content は `Loading.Instance.Options` を使う |
| `LoadingView.OverlayColorProperty` | `Dialog.OverlayColorProperty` | custom content の直接 `BindableProperty` 参照を置き換える。built-in content は `Loading.Instance.Options` を使う |

## obsolete の Toast 面を置き換える

調査した旧公開 API では、旧 Toast 型は obsolete で custom `ToastView` 経路だけを提供する。KsDialogs.Maui には新しい built-in message 経路もある。これは旧 message overload の対応先ではなく、新たに選べる代替である。

| 旧メンバー | 新しい対応先または状態 | 移行方法 |
|---|---|---|
| `ToastView` | Toast factory が返す通常の MAUI `View` | state の運び手に `IToastViewModel` を実装し、View は `RegisterForToast` または `Toast.Instance.Registry.Register<TViewModel>` で登録する |
| `IToast.Show<TView>(object viewModel = null)` | 直接の対応先なし | generic View 生成を削除し、ViewModel を `RegisterForToast` で登録して instance を渡す形 (`Toast.Instance.Show(viewModel, durationMs, placement)`) か型を渡す形 (`Toast.Instance.Show<TViewModel>(configure, durationMs, placement)`) で表示する |
| 具象 `Toast.Show(ToastView view, object viewModel = null)` 経路 (DEBUG build 限定) | 直接の対応先なし | 旧 View instance 経路を削除し、登録経路または inline の `Show<TViewModel>(viewModel, factory, durationMs, placement)` を使う |
| 旧 message overload なし。新しい代替 | `Toast.Instance.Show(message, durationMs, placement)` | 新 message 経路は duration の上限クランプがなく、同時呼び出しは queue ではなく重なって表示され、非対話なのでタッチは背後の page へ素通しされる |
| `ToastView.Duration` | `durationMs` または `ToastStyle.DefaultDuration` | 新しい show 呼び出しまたは app-wide style に duration を設定する |
| `ToastView.DurationProperty` | 直接の対応先なし | View 所有の `BindableProperty` を削除し、show ごとに `durationMs` を渡すか app-wide の `ToastStyle.DefaultDuration` を設定する |

`Show` は `void` を返し、表示を待つ・更新する・閉じる手段はない — Toast は duration の経過で消える。0 以下の `durationMs` は未指定として扱われ、`ToastStyle.DefaultDuration` (`ToastStyle.BuiltinDefaultDuration`、1500 ミリ秒) に落ちる。`ToastStyle` は built-in の message View 向けに `BackgroundColor`・`TextColor`・`FontSize`・`CornerRadius` を持ち、`DefaultPlacement` は built-in と custom の双方に効く app-wide の配置になる。`BackgroundColor` の出発点は組み込みのメッセージ Toast の既定色 `ToastStyle.BuiltinBackgroundColor` (半透明のダークグレー) で、`DefaultDuration` に対する `ToastStyle.BuiltinDefaultDuration` と同じ位置づけである。未登録の ViewModel 型で表示すると `DialogException.ViewFactoryNotRegistered` で失敗する。

型を渡す表示は、`RegisterForToast` か `Toast.Instance.Registry.RegisterViewModel<TViewModel>` で ViewModel factory を配線すれば使える。`Show` が即座に戻るため `configure` は同期の形だけである。ViewModel factory が未登録なら呼び出し時点で `DialogException.ViewModelFactoryNotRegistered` を投げる一方、ViewModel factory や `configure` が投げた例外は呼び出し元へ届かず、警告を残してその 1 枚だけを破棄する (他の表示と後続の表示には影響しない):

```csharp
Toast.Instance.Style = Toast.Instance.Style with { DefaultDuration = 2000 };

Toast.Instance.Show("Saved");
Toast.Instance.Show(new NoticeViewModel("Sync finished"), durationMs: 3000);
Toast.Instance.Show<NoticeViewModel>(viewModel => viewModel.Message = "Sync finished");
```
