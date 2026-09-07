# Dialog を表示する

class の ViewModel に結果型を宣言し、表示ごとに新しい MAUI View を作る factory を登録してから `DialogResult` を待つ。値を伴う完了とキャンセルは `DialogResult<TResult>.Completed` と `DialogResult<TResult>.Cancelled` で分岐する。非 generic の `IDialogViewModel` を実装すると結果型は `bool` になり、独自の結果型には `IDialogViewModel<TResult>` を実装する。

`DialogViewRegistry.Register` は 2 引数 factory `Func<TViewModel, DialogNotifier<TResult>, View>` と 1 引数 factory `Func<TViewModel, View>` を受け取る。factory は show のたびに呼ばれ、その show の結果を完了またはキャンセルする `DialogNotifier<TResult>` を受け取る。1 引数 factory を使う場合は、ViewModel 自身が拡張プロパティ `Notifier` から報告する ([ViewModel](view-models.md))。最初の notifier 報告だけが結果をラッチし、以後の報告は何もしない。結果は退出と器の撤去が完了した後にだけ配送される。

`ShowAsync` は `CancellationToken` を受け取らず、呼び出し元からのキャンセル経路はない。`Cancelled` になるのは、notifier のキャンセル報告、外側タップ、Android の戻るボタン、Dialog を載せていた画面の破棄である。

content は普通の MAUI View なので、XAML で書いた `ContentView` をそのまま中身にできる。

## `ShowAsync` を選ぶ

`IKsDialog` (`Dialog.Instance` と DI で注入したもののどちらも同じ実体) は `ShowAsync` を 7 つの overload で公開する。選ぶ軸は 2 つで、ViewModel をインスタンスで渡すか型だけ渡すか、そして content の factory を登録済みのものに任せるかその場で渡すかである。どの overload も末尾に省略可能な `placement` 引数を取り、渡すと content に添付された配置をまるごと置換する ([レイアウト](layout.md))。戻り値は `Task<DialogResult<TResult>>` で、非 generic の `IDialogViewModel` で宣言した ViewModel では `TResult` が `bool` になる。

| シグネチャ | 何をする | いつ選ぶ | 必要な登録 |
|---|---|---|---|
| `ShowAsync<TResult>(IDialogViewModel<TResult> viewModel)` | 作った ViewModel インスタンスを渡し、登録済みの View factory が content を作る | 呼び出し元で ViewModel を組み立てる (コンストラクタ引数を渡す) とき | View factory ([DI 登録](di-registration.md)) |
| `ShowAsync<TViewModel, TResult>(TViewModel viewModel, Func<TViewModel, DialogNotifier<TResult>, View> factory)` | ViewModel インスタンスと content factory の両方をその場で渡す。レジストリには触れない | 1 回だけ使う content。結果型を明示した ViewModel | 不要 |
| `ShowAsync<TViewModel>(TViewModel viewModel, Func<TViewModel, DialogNotifier<bool>, View> factory)` | 上の `IDialogViewModel` 用の省略形 (結果は `bool`) | 同上で、結果型を型引数に書かないとき | 不要 |
| `ShowAsync<TViewModel, TResult>(Action<TViewModel>? configure = null)` | ViewModel の型だけを渡し、登録済みの ViewModel factory が作ったインスタンスを `configure` してから表示する | ViewModel を DI で解決させたいとき。1 行登録済みのとき | View factory + ViewModel factory ([ViewModel](view-models.md)) |
| `ShowAsync<TViewModel, TResult>(Func<TViewModel, Task> configure)` | 上の非同期 `configure` 版。`configure` の完了を待ってから content を作る | 表示前に非同期で状態を用意するとき | 同上 |
| `ShowAsync<TViewModel>(Action<TViewModel>? configure = null)` | 型指定 show の `IDialogViewModel` 用の省略形 (結果は `bool`) | 同上で、結果型を型引数に書かないとき | 同上 |
| `ShowAsync<TViewModel>(Func<TViewModel, Task> configure)` | 上の非同期 `configure` 版 | 同上で、非同期に状態を用意するとき | 同上 |

以下の例では、「登録して呼び出す」の `ShowAsync(new ConfirmDialogViewModel(...))` が 1 行目、「bool 以外の結果型を返す」の `ShowAsync<ItemEditDialogViewModel, ItemEdit>(configure)` が 4 行目、「登録せずに content を表示する」の factory 直渡しが 3 行目に当たる。残る 2・5・6・7 行目は次節の最小例で示す。

## 残りの overload の最小例

後続の節のサンプルに現れない 4 つは次の形で呼ぶ。使う型は後続の節で宣言する `ConfirmDialogViewModel` (結果は `bool`) と `ItemEditDialogViewModel` (結果は `ItemEdit`) である。以降の節に現れる `NoticeDialogViewModel` / `NoticeDialogView` のように、同じ形で利用者が書く型もある。

ViewModel インスタンスと 2 引数 factory をその場で渡し、結果型を型引数で明示する形。

```csharp
var result = await _dialogs.ShowAsync<ItemEditDialogViewModel, ItemEdit>(
    new ItemEditDialogViewModel(),
    (viewModel, _) => new ItemEditDialogView { BindingContext = viewModel });
```

ViewModel の型だけを渡し、非同期の `configure` で状態を用意してから表示する形。

```csharp
var result = await _dialogs.ShowAsync<ItemEditDialogViewModel, ItemEdit>(
    async viewModel =>
    {
        viewModel.Name = await LoadDefaultNameAsync();
    });
```

真偽値 ViewModel の型だけを渡す形。`configure` は省略でき、ViewModel factory が作った状態のまま表示する。

```csharp
var result = await _dialogs.ShowAsync<ConfirmDialogViewModel>();
```

真偽値 ViewModel の型だけを渡し、非同期の `configure` を適用してから表示する形。

```csharp
var result = await _dialogs.ShowAsync<ConfirmDialogViewModel>(
    async viewModel =>
    {
        await PrepareAsync(viewModel);
    });
```

以下は ViewModel・View・呼び出し元の 3 つに分けた最小構成である。

## ViewModel を宣言する

ViewModel は結果型を宣言し、完了・キャンセルの操作を command として公開する。報告は `this.Notifier` から行い、show の前後は `null` になるので `?.` で呼ぶ。

```csharp
using System.Windows.Input;
using KsDialogs;
using Microsoft.Maui.Controls;

namespace MyApp;

public sealed class ConfirmDialogViewModel : IDialogViewModel
{
    public ConfirmDialogViewModel(string message)
    {
        Message = message;
        CompleteCommand = new Command(() => this.Notifier?.Complete(true));
        CancelCommand = new Command(() => this.Notifier?.Cancel());
    }

    public string Message { get; }

    public ICommand CompleteCommand { get; }

    public ICommand CancelCommand { get; }
}
```

## View を XAML で書く

覆いと配置はライブラリの器が受け持つため、XAML にはカード自体だけを書く。`BindingContext` には表示対象の ViewModel が入る。

```xml
<?xml version="1.0" encoding="utf-8" ?>
<ContentView xmlns="http://schemas.microsoft.com/dotnet/2021/maui"
             xmlns:x="http://schemas.microsoft.com/winfx/2009/xaml"
             xmlns:local="clr-namespace:MyApp"
             x:Class="MyApp.ConfirmDialogView"
             x:DataType="local:ConfirmDialogViewModel">

    <Border WidthRequest="272" Padding="20" StrokeThickness="0">
        <Border.StrokeShape>
            <RoundRectangle CornerRadius="16" />
        </Border.StrokeShape>

        <VerticalStackLayout Spacing="16">
            <Label Text="{Binding Message}" HorizontalTextAlignment="Center" />

            <Grid ColumnDefinitions="*,10,*">
                <Button Text="Cancel" Command="{Binding CancelCommand}" />
                <Button Grid.Column="2" Text="OK" Command="{Binding CompleteCommand}" />
            </Grid>
        </VerticalStackLayout>
    </Border>

</ContentView>
```

```csharp
using Microsoft.Maui.Controls;

namespace MyApp;

public partial class ConfirmDialogView : ContentView
{
    public ConfirmDialogView() => InitializeComponent();
}
```

## 登録して呼び出す

`Register` は起動時に 1 回だけ呼ぶ。明示登録の factory が返した View には、ライブラリは `BindingContext` を設定しない — factory 側で表示対象の ViewModel を入れる (`RegisterForDialog` による 1 行登録なら設定される。[DI 登録](di-registration.md))。

呼び出し側は `Dialog.Instance` を使うか、`IKsDialog` として注入したものを使う。どちらも同じレジストリと同じ状態を指す。

```csharp
using KsDialogs;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Maui.Hosting;

namespace MyApp;

public static class MauiProgram
{
    public static MauiApp CreateMauiApp()
    {
        var builder = MauiApp.CreateBuilder();
        builder.UseMauiApp<App>();
        builder.Services
            .AddSingleton<IKsDialog>(_ => Dialog.Instance)
            .AddTransient<ItemPage>();

        Dialog.Instance.Registry.Register<ConfirmDialogViewModel>(
            viewModel => new ConfirmDialogView { BindingContext = viewModel });

        return builder.Build();
    }
}
```

```csharp
using System;
using KsDialogs;
using Microsoft.Maui.Controls;

namespace MyApp;

public partial class ItemPage : ContentPage
{
    private readonly IKsDialog _dialogs;

    public ItemPage(IKsDialog dialogs)
    {
        InitializeComponent();
        _dialogs = dialogs;
    }

    private async void OnDeleteClicked(object? sender, EventArgs e)
    {
        var result = await _dialogs.ShowAsync(new ConfirmDialogViewModel("Delete this item?"));
        StatusLabel.Text = result switch
        {
            DialogResult<bool>.Completed { Value: true } => "Deleted",
            _ => "Kept",
        };
    }
}
```

## bool 以外の結果型を返す

`IDialogViewModel<TResult>` を実装すると、`TResult` に record でも `string` でも任意の型を置ける。`Notifier` と `DialogResult<TResult>` の型はその宣言から導出されるので、別の型で報告する書き方はコンパイルできない。

`RegisterForDialog<TView, TViewModel, TResult>` は View factory と ViewModel factory の両方を 1 行で配線するため、ViewModel の型だけを渡す型指定 `ShowAsync` も使えるようになる。View は上の `ConfirmDialogView` と同じ形の `ContentView` で、`x:DataType` を `ItemEditDialogViewModel` にして `SaveCommand` / `CancelCommand` を bind する。

```csharp
using System.Windows.Input;
using KsDialogs;
using Microsoft.Maui.Controls;

namespace MyApp;

public sealed record ItemEdit(string Name, int Quantity);

public sealed class ItemEditDialogViewModel : IDialogViewModel<ItemEdit>
{
    public ItemEditDialogViewModel()
    {
        SaveCommand = new Command(() => this.Notifier?.Complete(new ItemEdit(Name, Quantity)));
        CancelCommand = new Command(() => this.Notifier?.Cancel());
    }

    public string Name { get; set; } = string.Empty;

    public int Quantity { get; set; }

    public ICommand SaveCommand { get; }

    public ICommand CancelCommand { get; }
}
```

```csharp
builder.Services.RegisterForDialog<ItemEditDialogView, ItemEditDialogViewModel, ItemEdit>();
```

```csharp
private async void OnEditClicked(object? sender, EventArgs e)
{
    var result = await _dialogs.ShowAsync<ItemEditDialogViewModel, ItemEdit>(
        viewModel => viewModel.Name = "Apple");
    if (result is DialogResult<ItemEdit>.Completed { Value: var edit })
    {
        StatusLabel.Text = $"{edit.Name} x{edit.Quantity}";
    }
}
```

## 登録せずに content を表示する

1 回だけ使う content は factory を `ShowAsync` へ直接渡す。この経路はレジストリを追加・置換・削除せず、同じ ViewModel 型の登録済み factory も使わない。View は登録経路と同じものを使えるので、XAML で書いた `ContentView` をそのまま渡してよい。

```csharp
private async void OnAboutClicked(object? sender, EventArgs e)
{
    var result = await _dialogs.ShowAsync(
        new NoticeDialogViewModel("Ready"),
        (viewModel, _) => new NoticeDialogView { BindingContext = viewModel });
    StatusLabel.Text = result is DialogResult<bool>.Completed ? "Read" : "Dismissed";
}
```

## 独立した Dialog を重ねて表示する

並行する show には別の ViewModel instance を使う。後の Dialog が前面に出て各呼び出しは固有の結果を持つ。下側の Dialog が先に報告した場合、iOS では上下とも消えて上側の結果は cancelled になり、Android では下側だけが閉じて上側は自身の結果を報告するまで操作できる。下側が先に報告するのはアプリ側が下側の報告口を保持している場合だけで、ユーザー操作 (完了・キャンセル・外側タップ・戻るボタン) は常に手前の 1 枚にしか届かない。

```csharp
private async void OnShowTwoClicked(object? sender, EventArgs e)
{
    var first = _dialogs.ShowAsync(new ConfirmDialogViewModel("First"));
    var second = _dialogs.ShowAsync(new ConfirmDialogViewModel("Second"));
    var results = await Task.WhenAll(first, second);
    StatusLabel.Text = $"{results[0]} / {results[1]}";
}
```

## 構成ミスの失敗を扱う

構成ミスは `Cancelled` を返さず、入れ子クラスの `DialogException` で `Task` を fault させる。登録漏れをエンドユーザーのキャンセルと取り違えないためであり、この場合 View は生成も表示もされない。`await` した呼び出し元にそのまま throw されるので、`try` / `catch` で握り潰さず、開発中に気づける形で扱う。

`ViewModelTypeName` を持つ例外は、解決できなかった ViewModel の型名をそのプロパティからも読める。

| 例外 | メッセージ | 原因と対処 |
|---|---|---|
| `DialogException.ViewFactoryNotRegistered` | `No View factory is registered for ViewModel type {TypeName}.` | 明示登録も fallback も content View を解決できない。`Register` / `RegisterForDialog` をその ViewModel 型に対して呼ぶか、`UseViewFallback` で規約解決を設定する |
| `DialogException.ViewModelFactoryNotRegistered` | `No ViewModel factory is registered for ViewModel type {TypeName}.` | 型指定 show に ViewModel factory がない。`RegisterViewModel` / `RegisterForDialog` を呼ぶか、`UseViewModelFallback` を設定する ([ViewModel](view-models.md)) |
| `DialogException.PresentationHostUnavailable` | `No screen is available to present the Dialog.` | 提示できる画面がない。最初の Page が表示された後に show する。キューイングはせず即座に失敗する |
| `DialogException.ServiceProviderUnavailable` | `The app's IServiceProvider is not available yet.` | startup が service provider を捕捉する前に DI 経路 (1 行登録・fallback 解決) を使った。`MauiApp` の構築完了後に show する ([DI 登録](di-registration.md)) |
| `DialogException.ViewModelAlreadyShowing` | `This ViewModel instance of type {TypeName} is already being shown.` | 同じ ViewModel instance を既に表示している。重ねる show ごとに新しい instance を作る |
| `DialogException.ValueTypeViewModel` | `ViewModel type {TypeName} is a value type and cannot be used as a ViewModel.` | 値型の ViewModel が提示入口に届いた。ViewModel を `class` にする (`struct` / `record struct` は使えない) |

表のメッセージは現在の実装が返す値であり、安定した API ではない (変わらないのは例外型と throw される条件であり、文言は予告なく変わりうる)。

たとえば起動時に `Register` / `RegisterForDialog` / `UseViewFallback` のどれも呼ばないまま `ConfirmDialogViewModel` を show すると、`ConfirmDialogView` を解決できず `DialogException.ViewFactoryNotRegistered` になる。

```csharp
public static MauiApp CreateMauiApp()
{
    var builder = MauiApp.CreateBuilder();
    builder.UseMauiApp<App>();
    builder.Services
        .AddSingleton<IKsDialog>(_ => Dialog.Instance)
        .AddTransient<ItemPage>();

    return builder.Build();
}
```

呼び出し元では入れ子の例外型でそのまま `catch` でき、`ViewModelTypeName` から解決できなかった ViewModel の型名を読める。構成ミスは実行時に直せるものではないので、ログに残したうえで再 throw し、開発中に気づける形にする。

```csharp
private async void OnDeleteClicked(object? sender, EventArgs e)
{
    try
    {
        var result = await _dialogs.ShowAsync(new ConfirmDialogViewModel("Delete this item?"));
        StatusLabel.Text = result switch
        {
            DialogResult<bool>.Completed { Value: true } => "Deleted",
            _ => "Kept",
        };
    }
    catch (DialogException.ViewFactoryNotRegistered ex)
    {
        Debug.WriteLine($"Dialog view factory is not registered for {ex.ViewModelTypeName}.");
        throw;
    }
}
```

`ViewModelTypeName` を持たない `PresentationHostUnavailable` / `ServiceProviderUnavailable` も含めてまとめて扱いたい場合は、基底型の `DialogException` で `catch` する。
