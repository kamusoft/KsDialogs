# Loading のスコープ内で処理する

`Loading.Instance` と、DI で `IKsLoading` として受け取ったものは同じ実体を指す。表示はプロセス内に 1 つで、どちらの入口から呼んでも同じ表示に合流する。

Loading は Dialog や Toast より前面にあり、ユーザー操作では閉じず、背後の content へのタッチを遮る。

## 合流のふるまい

並行する要求はプロセス内の 1 つの表示に合流し、最初の参加者の content がその世代の終わりまで残る。最後でない参加者は自分の action が終わった時点で戻り、合流数を 0 にする参加者だけが器の撤去を待つ。失敗した action も参加を解放し、その失敗は呼び出し元へ伝播する。

進捗値は任意のスレッドから受け付け、`IProgress<double>` として渡り、0 以上 1 以下へ丸められる。

## 表示メソッドを選ぶ

`IKsLoading` (`Loading.Instance` と DI で受け取ったもののどちらも同じ実体) は次のメソッドを公開する。選ぶ軸は、処理の生存期間を Loading に握らせる (`StartAsync`) か表示と非表示を自分で対にする (`ShowAsync` / `HideAsync`) か、content を組み込みにするか custom にするか、custom content の factory を登録済みのものに任せるかその場で渡すか、そして custom content の ViewModel を自分で組み立てて渡すか型だけを渡してライブラリに作らせるかである。

| シグネチャ | 何をする | いつ選ぶ | 必要な登録 |
|---|---|---|---|
| `Task ShowAsync(string? message = null, DialogPlacement? placement = null)` | 組み込み content を表示し、合流を 1 件増やす。入力遮断が有効になった時点で戻り、入口 transition の完了は待たない | 表示と非表示を自分のコードで対にするとき | 不要 |
| `Task ShowAsync(ILoadingViewModel viewModel, DialogPlacement? placement = null)` | 登録済みの View factory が作った custom content を表示する | 見た目を差し替えた Loading を手動で出すとき | View factory ([DI 登録](di-registration.md)) |
| `Task ShowAsync<TViewModel>(TViewModel viewModel, Func<TViewModel, View> factory, DialogPlacement? placement = null)` | content factory をその場で渡して表示する。レジストリには触れない | 1 回だけ使う content | 不要 |
| `Task HideAsync()` | 現在世代の表示だけを撤去し、撤去完了後に戻る | `ShowAsync` と対にするとき | 不要 |
| `void SetMessage(string? message)` | 表示中の組み込み content の message を差し替える。唯一の同期操作で、待たない | 表示したまま文言だけ変えるとき | 不要 |
| `Task StartAsync(Func<IProgress<double>, Task> action, string? message = null, DialogPlacement? placement = null)` | 組み込み content を出したまま action を実行し、終わったら撤去する | 処理の生存期間と表示を対にするとき | 不要 |
| `Task<T> StartAsync<T>(Func<IProgress<double>, Task<T>> action, string? message = null, DialogPlacement? placement = null)` | 上の戻り値つき版 | 同上で、処理が値を返すとき | 不要 |
| `Task StartAsync(ILoadingViewModel viewModel, Func<IProgress<double>, Task> action, DialogPlacement? placement = null)` | 登録済みの custom content を出したまま action を実行する | 進捗を自前の見た目で見せるとき | View factory ([DI 登録](di-registration.md)) |
| `Task<T> StartAsync<T>(ILoadingViewModel viewModel, Func<IProgress<double>, Task<T>> action, DialogPlacement? placement = null)` | 上の戻り値つき版 | 同上で、処理が値を返すとき | View factory ([DI 登録](di-registration.md)) |
| `Task StartAsync<TViewModel>(TViewModel viewModel, Func<TViewModel, View> factory, Func<IProgress<double>, Task> action, DialogPlacement? placement = null)` | content factory をその場で渡し、それを出したまま action を実行する | 1 回だけ使う content で処理を包むとき | 不要 |
| `Task<T> StartAsync<TViewModel, T>(TViewModel viewModel, Func<TViewModel, View> factory, Func<IProgress<double>, Task<T>> action, DialogPlacement? placement = null)` | 上の戻り値つき版 | 同上で、処理が値を返すとき | 不要 |
| `Task ShowAsync<TViewModel>(Action<TViewModel>? configure = null, DialogPlacement? placement = null)` | 登録済みの ViewModel factory が作った ViewModel を `configure` してから、登録済みの custom content を表示する | ViewModel の組み立てを DI に任せるとき | View factory と ViewModel factory ([DI 登録](di-registration.md)) |
| `Task ShowAsync<TViewModel>(Func<TViewModel, Task> configure, DialogPlacement? placement = null)` | 上の `configure` を非同期にした版 | 同上で、表示前に非同期のロードが要るとき | View factory と ViewModel factory ([DI 登録](di-registration.md)) |
| `Task StartAsync<TViewModel>(Func<IProgress<double>, Task> action, Action<TViewModel>? configure = null, DialogPlacement? placement = null)` | 型を渡して作らせた custom content を出したまま action を実行する | 型を渡す形をスコープ形にするとき | View factory と ViewModel factory ([DI 登録](di-registration.md)) |
| `Task StartAsync<TViewModel>(Func<IProgress<double>, Task> action, Func<TViewModel, Task> configure, DialogPlacement? placement = null)` | 上の `configure` を非同期にした版 | 同上で、表示前に非同期のロードが要るとき | View factory と ViewModel factory ([DI 登録](di-registration.md)) |
| `Task<T> StartAsync<TViewModel, T>(Func<IProgress<double>, Task<T>> action, Action<TViewModel>? configure = null, DialogPlacement? placement = null)` | 上の戻り値つき版 | 同上で、処理が値を返すとき | View factory と ViewModel factory ([DI 登録](di-registration.md)) |
| `Task<T> StartAsync<TViewModel, T>(Func<IProgress<double>, Task<T>> action, Func<TViewModel, Task> configure, DialogPlacement? placement = null)` | 上の戻り値つき版で `configure` が非同期の形 | 同上で、処理が値を返し表示前に非同期のロードも要るとき | View factory と ViewModel factory ([DI 登録](di-registration.md)) |

末尾の `placement` を省略すると、custom content では View への添付、添付もなければライブラリ既定の配置が使われる。以下の例では、「処理を包んで表示する」が 7 行目、「手動で表示・更新・非表示にする」が 1・4・5 行目、「custom Loading content を登録する」が 8 行目、「登録せずに custom content を表示する」が 3・10 行目に当たる。ViewModel の型を渡す 12〜17 行目は「型を渡して Loading を表示する」で扱う。残る 2・6・9・11 行目は次節の最小例で示す。

## 残りの overload の最小例

後続の節のサンプルに現れない 4 つは次の形で呼ぶ。使う型は後続の節で宣言する `SyncLoadingViewModel` (登録済み) と `NoticeLoadingViewModel` (登録しない) である。

登録済みの custom content を手動で表示し、`HideAsync` と対にする形。

```csharp
await _loading.ShowAsync(new SyncLoadingViewModel());
```

組み込み content を出したまま action を実行し、値を返さない形。

```csharp
await _loading.StartAsync(
    async progress =>
    {
        progress.Report(1);
        await Task.Yield();
    },
    message: "Saving");
```

登録済みの custom content を出したまま action を実行し、その戻り値を受け取る形。

```csharp
var items = await _loading.StartAsync<IReadOnlyList<string>>(
    new SyncLoadingViewModel(),
    async progress =>
    {
        progress.Report(1);
        await Task.Yield();
        return new[] { "Ready" };
    });
```

content factory をその場で渡し、それを出したまま実行した action の戻り値を受け取る形。

```csharp
var count = await _loading.StartAsync(
    new NoticeLoadingViewModel { Message = "Counting" },
    model => new Label { Text = model.Message },
    async progress =>
    {
        progress.Report(1);
        await Task.Yield();
        return 1;
    });
```

## 処理を包んで表示する

処理の生存期間と Loading 表示を対にするには `StartAsync` を使う。呼び出し元は Page でも ViewModel でもよく、`Loading.Instance` を使うか、`IKsLoading` として受け取ったものを使う。

```csharp
using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using KsDialogs;
using Microsoft.Maui.Controls;

namespace MyApp;

public partial class SyncPage : ContentPage
{
    private readonly IKsLoading _loading;

    public SyncPage(IKsLoading loading)
    {
        InitializeComponent();
        _loading = loading;
    }

    private async void OnLoadClicked(object? sender, EventArgs e)
    {
        var items = await _loading.StartAsync<IReadOnlyList<string>>(
            async progress =>
            {
                progress.Report(0.5);
                await Task.Yield();
                progress.Report(1);
                return new[] { "Ready" };
            },
            message: "Loading");
        StatusLabel.Text = $"{items.Count} items";
    }
}
```

## 手動で表示・更新・非表示にする

処理の生存期間を別の場所で管理する場合は命令形の経路を使う。複数の呼び出しが合流していても `HideAsync` は現在世代の Loading 表示だけを撤去し、撤去完了後に戻る。後続の show は新世代を開始し、非表示にした旧世代の action 完了や遅延した進捗が新世代を終了・更新することはない。

```csharp
private async void OnRunClicked(object? sender, EventArgs e)
{
    await _loading.ShowAsync("Starting");
    await Task.Delay(500);
    _loading.SetMessage("Finishing");
    await _loading.HideAsync();
}
```

## custom Loading content を登録する

`Loading.Instance.Registry` の型は `LoadingViewRegistry` で、実体は `LoadingViewRegistry.Shared` である。Dialog / Toast のレジストリとは独立しているため、同じ ViewModel 型を複数のレジストリに登録できる。class の `ILoadingViewModel` をここへ `Register` で登録するか、DI から `RegisterForLoading<TView, TViewModel>` で配線する ([DI 登録](di-registration.md))。1 つのエントリは View factory と ViewModel factory の 2 slot からなり、`Register` が埋めるのは View factory の slot である。instance を渡す表示にはこの slot だけあればよく、ViewModel の型を渡す表示には両方が要る。`RegisterForLoading` は 1 行で両方を埋める。

content が進捗更新を必要とするときは `ILoadingProgressReceiver` と `INotifyPropertyChanged` を実装し、通知するプロパティへ View を bind する。`OnProgress` は進捗報告後に non-null の値で UI thread 上から呼ばれる。interface を実装していない ViewModel には転送されないだけである。

返す View へのレイアウトや transition の添付は、Dialog と同じ `Dialog` の attached property を使う ([レイアウト](layout.md))。ただし `Dialog.IsCanceledOnTouchOutside` は Loading では効かない。

### ViewModel を宣言する

```csharp
using System.ComponentModel;
using KsDialogs;

namespace MyApp;

public sealed class SyncLoadingViewModel : ILoadingViewModel, ILoadingProgressReceiver, INotifyPropertyChanged
{
    private double _progress;

    public event PropertyChangedEventHandler? PropertyChanged;

    public double Progress
    {
        get => _progress;
        private set
        {
            if (_progress == value)
            {
                return;
            }

            _progress = value;
            PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(nameof(Progress)));
        }
    }

    public void OnProgress(double progress) => Progress = progress;
}
```

### View を XAML で書く

覆いと配置はライブラリの器が受け持つため、XAML にはカード自体だけを書く。`BindingContext` には表示対象の ViewModel が入る。

```xml
<?xml version="1.0" encoding="utf-8" ?>
<ContentView xmlns="http://schemas.microsoft.com/dotnet/2021/maui"
             xmlns:x="http://schemas.microsoft.com/winfx/2009/xaml"
             xmlns:local="clr-namespace:MyApp"
             x:Class="MyApp.SyncLoadingView"
             x:DataType="local:SyncLoadingViewModel">

    <Border WidthRequest="210" Padding="20" StrokeThickness="0">
        <Border.StrokeShape>
            <RoundRectangle CornerRadius="14" />
        </Border.StrokeShape>

        <VerticalStackLayout Spacing="12">
            <Label Text="Syncing" HorizontalTextAlignment="Center" />
            <ProgressBar Progress="{Binding Progress}" />
        </VerticalStackLayout>
    </Border>

</ContentView>
```

```csharp
using Microsoft.Maui.Controls;

namespace MyApp;

public partial class SyncLoadingView : ContentView
{
    public SyncLoadingView() => InitializeComponent();
}
```

### 登録して呼び出す

`RegisterForLoading` は起動時に 1 回だけ呼ぶ。この 1 行登録では、表示対象の ViewModel が View の `BindingContext` に設定される。

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
            .AddSingleton<IKsLoading>(_ => Loading.Instance)
            .AddTransient<SyncPage>()
            .RegisterForLoading<SyncLoadingView, SyncLoadingViewModel>();

        return builder.Build();
    }
}
```

```csharp
private async void OnSyncClicked(object? sender, EventArgs e)
{
    await _loading.StartAsync(
        new SyncLoadingViewModel(),
        async progress =>
        {
            progress.Report(0.5);
            await Task.Yield();
            progress.Report(1);
        });
}
```

## 型を渡して Loading を表示する

ViewModel の instance ではなく型だけを渡すと、登録済みの ViewModel factory が作った instance を `configure` してから表示する。`RegisterForLoading` はこの ViewModel factory も配線するので、1 行登録しておけば追加の手数はない。低水準で書くときは `Loading.Instance.Registry.RegisterViewModel` で ViewModel factory の slot だけを埋める。View factory の slot とは独立しているため、片方を登録し直してももう片方は残る。

順序は「ViewModel の生成 → `configure` の完了 → 進捗受け口の紐付け → content View の生成 → 表示」に固定されている。したがって `configure` が入れた状態を content factory から読め、進捗は生成された ViewModel が `ILoadingProgressReceiver` を実装していればそこへ届く。

合流の判定に入るのは `configure` の完了後である。非同期の `configure` を待っている間に別の呼び出しが表示を確定させていれば、この呼び出しは合流側になり、生成した ViewModel は表示に使われない (出ている content は最初の参加者のものである)。

ViewModel factory が未登録なら `DialogException.ViewModelFactoryNotRegistered` で失敗する。ViewModel factory と `configure` が投げた失敗は表示にも合流にも進まず呼び出し元へ伝播し、`StartAsync` の action も実行されない。fallback 解決は Dialog レジストリだけの機構なので、Loading では効かない ([DI 登録](di-registration.md))。

`StartAsync` では action が先頭の引数なので、message を取る既存の overload と型を渡す overload は実引数の型で区別される。以下は前節で登録した `SyncLoadingViewModel` を型で表示する例である。

```csharp
private async void OnSyncByTypeClicked(object? sender, EventArgs e)
{
    await _loading.StartAsync<SyncLoadingViewModel>(
        async progress =>
        {
            progress.Report(0.5);
            await Task.Yield();
            progress.Report(1);
        },
        viewModel => viewModel.OnProgress(0));
}
```

非同期の `configure` と戻り値つきのスコープ形を組み合わせるときは、次の形になる。

```csharp
private Task<int> CountAsync() =>
    _loading.StartAsync<SyncLoadingViewModel, int>(
        async progress =>
        {
            progress.Report(1);
            await Task.Yield();
            return 1;
        },
        async viewModel =>
        {
            await Task.Yield();
            viewModel.OnProgress(0);
        });
```

表示と非表示を自分で対にするときは `ShowAsync` の型を渡す形を使う。`configure` を省略すると ViewModel factory が作ったままの状態で表示する。

```csharp
private async void OnHoldClicked(object? sender, EventArgs e)
{
    await _loading.ShowAsync<SyncLoadingViewModel>();
    await Task.Delay(500);
    await _loading.HideAsync();
}
```

## 登録せずに custom content を表示する

一度だけ使う custom content では、View factory を `ShowAsync` または `StartAsync` へ直接渡す。これらの inline 経路は同じ ViewModel 型の登録済み factory を使わず、レジストリのエントリを追加・置換・削除しない。

```csharp
using KsDialogs;

namespace MyApp;

public sealed class NoticeLoadingViewModel : ILoadingViewModel
{
    public string Message { get; init; } = string.Empty;
}
```

```csharp
private async void OnPrepareClicked(object? sender, EventArgs e)
{
    var viewModel = new NoticeLoadingViewModel { Message = "Preparing" };
    await _loading.ShowAsync(viewModel, model => new Label { Text = model.Message });
    await Task.Delay(500);
    await _loading.HideAsync();
}

private Task SynchronizeAsync() =>
    _loading.StartAsync(
        new NoticeLoadingViewModel { Message = "Synchronizing" },
        model => new Label { Text = model.Message },
        async progress =>
        {
            progress.Report(1);
            await Task.Yield();
        });
```

## 組み込み content を設定する

表示開始前に `Loading.Instance.Style` (`LoadingStyle` record) と `Loading.Instance.Options` (`DialogOptions` record) を設定する。show メソッドに style 引数はない。

| `LoadingStyle` のプロパティ | 型 | 既定値 |
|---|---|---|
| `IndicatorColor` | `Color` | `Colors.White` |
| `MessageFontSize` | `double` | `14` |
| `MessageColor` | `Color` | `Colors.White` |
| `DefaultMessage` | `string?` | `null` |
| `ProgressFormat` | `Func<string?, double?, string>` | `LoadingStyle.DefaultProgressFormat` |

`ProgressFormat` は UI thread で呼ばれ、進捗が最初に報告されるまで progress 引数は `null` になる。公開された組み込み formatter に戻すには `LoadingStyle.DefaultProgressFormat` を設定する。この formatter は進捗報告前は message だけを返し、報告後は丸めた百分率を加える。

style と options は表示開始時に読み取られるため、変更は次の表示から効く。組み込み content はライブラリ固定の cross-fade を使い custom transition を選べず、custom View は `Loading.Instance.Options` ではなく自身への layout と transition の添付値を使う。

```csharp
using KsDialogs;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Maui.Graphics;
using Microsoft.Maui.Hosting;

namespace MyApp;

public static class MauiProgram
{
    public static MauiApp CreateMauiApp()
    {
        var builder = MauiApp.CreateBuilder();
        builder.UseMauiApp<App>();
        builder.Services
            .AddSingleton<IKsLoading>(_ => Loading.Instance)
            .AddTransient<SyncPage>();

        Loading.Instance.Style = new LoadingStyle
        {
            IndicatorColor = Colors.White,
            MessageColor = Colors.White,
            MessageFontSize = 16,
            DefaultMessage = "Working",
            ProgressFormat = static (message, progress) =>
                progress is double value
                    ? $"{message}\n{value:P0}"
                    : message ?? string.Empty,
        };
        Loading.Instance.Options = new DialogOptions
        {
            LayoutArea = DialogLayoutArea.Window,
            OverlayColor = Colors.Black.WithAlpha(0.6f),
        };

        return builder.Build();
    }
}
```

## 構成ミスの失敗を扱う

構成ミスは入れ子クラスの `DialogException` で失敗する。`ShowAsync` / `StartAsync` に渡した ViewModel またはその型をその場で解決できない失敗は呼び出し時点で同期に投げられ、View は生成も表示もされず、`StartAsync` の action も実行されない (fail-fast)。content を作る段階で起きる失敗は、呼び出し元がまだ待っているため `Task` の失敗として届く。

`ViewModelTypeName` を持つ例外は、解決できなかった ViewModel の型名をそのプロパティからも読める。

| 例外 | メッセージ | 原因と対処 |
|---|---|---|
| `DialogException.ViewFactoryNotRegistered` | `No View factory is registered for ViewModel type {TypeName}.` | custom Loading の ViewModel 型に View factory がない。`Loading.Instance.Registry.Register` か `RegisterForLoading` をその型に対して呼ぶ |
| `DialogException.ViewModelFactoryNotRegistered` | `No ViewModel factory is registered for ViewModel type {TypeName}.` | 型を渡す表示に ViewModel factory がない。`Loading.Instance.Registry.RegisterViewModel` か `RegisterForLoading` をその型に対して呼ぶ (Loading には fallback がない) |
| `DialogException.ValueTypeViewModel` | `ViewModel type {TypeName} is a value type and cannot be used as a ViewModel.` | 値型の ViewModel が表示の入口に届いた。ViewModel を `class` にする (`struct` / `record struct` は使えない) |
| `DialogException.ServiceProviderUnavailable` | `The app's IServiceProvider is not available yet.` | `RegisterForLoading` で配線した content を、startup が service provider を捕捉する前に表示した。`MauiApp` の構築完了後に表示する ([DI 登録](di-registration.md)) |
| `DialogException.PresentationHostUnavailable` | `No screen is available to present the Dialog.` | custom content を作る時点で提示できる画面がない。最初の Page が表示された後に表示する |

表のメッセージは現在の実装が返す値であり、安定した API ではない (変わらないのは例外型と throw される条件であり、文言は予告なく変わりうる)。

`ServiceProviderUnavailable` は、`RegisterForLoading` が配線した ViewModel factory (service から ViewModel を引く) が起動前に呼ばれた場合にも起きる。

たとえば起動時に `RegisterForLoading` も `Registry.Register` も呼ばないまま `SyncLoadingViewModel` を表示すると、`SyncLoadingView` を解決できず `DialogException.ViewFactoryNotRegistered` になる。

```csharp
public static MauiApp CreateMauiApp()
{
    var builder = MauiApp.CreateBuilder();
    builder.UseMauiApp<App>();
    builder.Services
        .AddSingleton<IKsLoading>(_ => Loading.Instance)
        .AddTransient<SyncPage>();

    return builder.Build();
}
```

呼び出し元では入れ子の例外型でそのまま `catch` でき、`ViewModelTypeName` から解決できなかった ViewModel の型名を読める。構成ミスは実行時に直せるものではないので、ログに残したうえで再 throw し、開発中に気づける形にする。

```csharp
private async void OnSyncClicked(object? sender, EventArgs e)
{
    try
    {
        await _loading.StartAsync(
            new SyncLoadingViewModel(),
            async progress =>
            {
                progress.Report(1);
                await Task.Yield();
            });
    }
    catch (DialogException.ViewFactoryNotRegistered ex)
    {
        Debug.WriteLine($"Loading view factory is not registered for {ex.ViewModelTypeName}.");
        throw;
    }
}
```

`ViewModelTypeName` を持たない `ServiceProviderUnavailable` / `PresentationHostUnavailable` も含めてまとめて扱いたい場合は、基底型の `DialogException` で `catch` する。
