# message Toast を表示する

`Toast.Instance` と、DI で `IKsToast` として受け取ったものは同じ実体を指す。持つ動詞は `Show` の 1 つだけで、同期に `void` を返す fire-and-forget である。閉じる呼び出しも、message の更新も、スコープ形も、進捗の報告口も持たない。

Toast は覆いの色も外側タップの指定も持たず、受理順にかかわらず Loading が Toast より前面に残る。Toast content は非対話で、タッチは背後のページへ素通しされる。

## duration の決まり方

`durationMs` はミリ秒の `int?` で、呼び出しの受理時点から開始してアプリが背面にいる間も消費される。省略または 0 以下なら正の `ToastStyle.DefaultDuration` を使い、その既定値も 0 以下なら `ToastStyle.BuiltinDefaultDuration` の組み込み fallback 1500 ミリ秒を使う。上限 clamp は行わない。

## 重なりのふるまい

各 Toast は独立 timer で並存し、同じ配置では重なり得る。ライブラリは queue、置換、自動 offset を行わない。満了まで器が現れない Toast は一度も表示せず破棄される。

## `Show` を選ぶ

`IKsToast` (`Toast.Instance` と DI で受け取ったもののどちらも同じ実体) は `Show` を次の overload で公開する。選ぶ軸は、組み込みのメッセージ Toast に message を載せるか custom content を出すか、custom content の factory を登録済みのものに任せるかその場で渡すか、そして custom content の ViewModel を自分で組み立てて渡すか型だけを渡してライブラリに作らせるかである。

| シグネチャ | 何をする | いつ選ぶ | 必要な登録 |
|---|---|---|---|
| `void Show(string message, int? durationMs = null, DialogPlacement? placement = null)` | 組み込みのメッセージ Toast に message を載せて表示する。空文字はそのまま、長文は複数行に折り返す | 文言だけ伝えれば足りるとき | 不要 |
| `void Show(IToastViewModel viewModel, int? durationMs = null, DialogPlacement? placement = null)` | 登録済みの View factory が作った custom content を表示する | アイコン付きなど自前の見た目を繰り返し使うとき | View factory ([DI 登録](di-registration.md)) |
| `void Show<TViewModel>(TViewModel viewModel, Func<TViewModel, View> factory, int? durationMs = null, DialogPlacement? placement = null)` | content factory をその場で渡して表示する。レジストリには触れない | 1 回だけ使う content | 不要 |
| `void Show<TViewModel>(Action<TViewModel>? configure = null, int? durationMs = null, DialogPlacement? placement = null)` | 登録済みの ViewModel factory が作った ViewModel を `configure` してから、登録済みの custom content を表示する | ViewModel の組み立てを DI に任せるとき | View factory と ViewModel factory ([DI 登録](di-registration.md)) |

`durationMs` と `placement` の意味はどの overload でも同じである。以下の例では、「message を表示する」が 1 行目、「custom Toast content を登録する」が 2 行目、「登録せずに custom content を表示する」が 3 行目、「型を渡して Toast を表示する」が 4 行目に当たる。

## message を表示する

呼び出し元は Page でも ViewModel でもよく、`Toast.Instance` を使うか、`IKsToast` として受け取ったものを使う。`Show` は戻り値を待たないので `await` しない。

```csharp
using System;
using KsDialogs;
using Microsoft.Maui.Controls;

namespace MyApp;

public partial class ItemPage : ContentPage
{
    private readonly IKsToast _toast;

    public ItemPage(IKsToast toast)
    {
        InitializeComponent();
        _toast = toast;
    }

    private void OnSaveClicked(object? sender, EventArgs e) =>
        _toast.Show("Saved", durationMs: 2500);
}
```

## Toast の style と配置を設定する

表示前に `Toast.Instance.Style` を設定する。`ToastStyle` record は表示開始時に読み取られるため、変更は次の Toast から効く。

| `ToastStyle` のプロパティ | 型 | 既定値 |
|---|---|---|
| `BackgroundColor` | `Color` | `ToastStyle.BuiltinBackgroundColor` |
| `TextColor` | `Color` | `Colors.White` |
| `FontSize` | `double` | `14` |
| `CornerRadius` | `double` | `22` |
| `DefaultDuration` | `int` | `ToastStyle.BuiltinDefaultDuration` |
| `DefaultPlacement` | `DialogPlacement?` | `null` |

視覚項目が効くのは組み込みのメッセージ Toast だけで、`DefaultDuration` と `DefaultPlacement` は custom content にも効く。`Show` に渡した placement は style の既定値より優先される。どちらも無いときのライブラリ既定の配置は、可視領域の下部中央から上方向へ論理単位 80 である。

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
            .AddSingleton<IKsToast>(_ => Toast.Instance)
            .AddTransient<ItemPage>();

        Toast.Instance.Style = new ToastStyle
        {
            BackgroundColor = Colors.Black.WithAlpha(0.92f),
            TextColor = Colors.White,
            FontSize = 16,
            CornerRadius = 20,
            DefaultDuration = 2000,
            DefaultPlacement = new DialogPlacement
            {
                VerticalAlignment = DialogAlignment.End,
                OffsetY = -80,
            },
        };

        return builder.Build();
    }
}
```

## custom Toast content を登録する

`Toast.Instance.Registry` の型は `ToastViewRegistry` で、実体は `ToastViewRegistry.Shared` である。Dialog / Loading のレジストリとは独立しているため、同じ ViewModel 型を複数のレジストリに登録できる。class の `IToastViewModel` をここへ `Register` で登録するか、DI から `RegisterForToast<TView, TViewModel>` で配線する ([DI 登録](di-registration.md))。1 つのエントリは View factory と ViewModel factory の 2 slot からなり、`Register` が埋めるのは View factory の slot である。instance を渡す表示にはこの slot だけあればよく、ViewModel の型を渡す表示には両方が要る。`RegisterForToast` は 1 行で両方を埋める。

Toast には覆いも組み込みの地もないため、custom content は自分で背景を描く。配置は Dialog と同じ `Dialog` の attached property で添付でき、closure を持つ transition だけは code-behind から添付する ([レイアウト](layout.md))。

### ViewModel を宣言する

Toast は結果も進捗も持たないので、ViewModel が担うのはデータの運搬とレジストリの型キーだけである。

```csharp
using KsDialogs;

namespace MyApp;

public sealed class SavedToastViewModel : IToastViewModel
{
    public string Message { get; set; } = string.Empty;
}
```

### View を XAML で書く

```xml
<?xml version="1.0" encoding="utf-8" ?>
<ContentView xmlns="http://schemas.microsoft.com/dotnet/2021/maui"
             xmlns:x="http://schemas.microsoft.com/winfx/2009/xaml"
             xmlns:ksd="clr-namespace:KsDialogs;assembly=KsDialogs.Maui"
             xmlns:local="clr-namespace:MyApp"
             x:Class="MyApp.SavedToastView"
             x:DataType="local:SavedToastViewModel"
             ksd:Dialog.VerticalAlignment="End"
             ksd:Dialog.OffsetY="-80">

    <Border Padding="18,12" StrokeThickness="0">
        <Border.StrokeShape>
            <RoundRectangle CornerRadius="12" />
        </Border.StrokeShape>

        <Label Text="{Binding Message}" />
    </Border>

</ContentView>
```

```csharp
using KsDialogs;
using Microsoft.Maui.Controls;

namespace MyApp;

public partial class SavedToastView : ContentView
{
    public SavedToastView()
    {
        InitializeComponent();
        Dialog.SetTransition(this, DialogTransition.Fade());
    }
}
```

### 登録して呼び出す

`RegisterForToast` は起動時に 1 回だけ呼ぶ。この 1 行登録では、表示対象の ViewModel が View の `BindingContext` に設定される。

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
            .AddSingleton<IKsToast>(_ => Toast.Instance)
            .AddTransient<ItemPage>()
            .RegisterForToast<SavedToastView, SavedToastViewModel>();

        return builder.Build();
    }
}
```

```csharp
private void OnSaveClicked(object? sender, EventArgs e) =>
    _toast.Show(new SavedToastViewModel { Message = "Saved" });
```

## 登録せずに custom content を表示する

1 回だけ使う content は View factory を `Show` へ直接渡す。この経路は Toast レジストリを変更しない。

```csharp
using KsDialogs;

namespace MyApp;

public sealed class NoticeToastViewModel : IToastViewModel
{
    public string Message { get; init; } = string.Empty;
}
```

```csharp
private void OnConnectedClicked(object? sender, EventArgs e) =>
    _toast.Show(
        new NoticeToastViewModel { Message = "Connected" },
        viewModel => new Label { Text = viewModel.Message },
        durationMs: 1800,
        placement: new DialogPlacement
        {
            VerticalAlignment = DialogAlignment.Start,
            OffsetY = 80,
        });
```

## 型を渡して Toast を表示する

ViewModel の instance ではなく型だけを渡すと、登録済みの ViewModel factory が作った instance を `configure` してから表示する。`RegisterForToast` はこの ViewModel factory も配線するので、1 行登録しておけば追加の手数はない。低水準で書くときは `Toast.Instance.Registry.RegisterViewModel` で ViewModel factory の slot だけを埋める。

`configure` は同期の `Action<TViewModel>` だけで、非同期の形はない。`Show` が同期に戻る fire-and-forget だからである。順序は「ViewModel の生成 → `configure` の完了 → content View の生成 → 表示」に固定されているため、`configure` が入れた状態を content factory から読める。

```csharp
private void OnSyncedClicked(object? sender, EventArgs e) =>
    _toast.Show<SavedToastViewModel>(
        viewModel => viewModel.Message = "Synced",
        durationMs: 2000);
```

## 構成ミスの失敗を扱う

`Show` に渡した ViewModel またはその型をその場で解決できない構成ミスは、入れ子クラスの `DialogException` として呼び出し時点で同期に投げられ、その Toast は表示されない。fire-and-forget でも失敗が握り潰されないのは、この表の失敗だけである。

`ViewModelTypeName` から、解決できなかった ViewModel の型名を読める。

| 例外 | メッセージ | 原因と対処 |
|---|---|---|
| `DialogException.ViewFactoryNotRegistered` | `No View factory is registered for ViewModel type {TypeName}.` | custom Toast の ViewModel 型に View factory がない。`Toast.Instance.Registry.Register` か `RegisterForToast` をその型に対して呼ぶ |
| `DialogException.ViewModelFactoryNotRegistered` | `No ViewModel factory is registered for ViewModel type {TypeName}.` | 型を渡す `Show` に ViewModel factory がない。`Toast.Instance.Registry.RegisterViewModel` か `RegisterForToast` をその型に対して呼ぶ (Toast には fallback がない) |
| `DialogException.ValueTypeViewModel` | `ViewModel type {TypeName} is a value type and cannot be used as a ViewModel.` | box された値型の ViewModel が表示の入口に届いた。ViewModel を `class` にする (`struct` / `record struct` は使えない) |

表のメッセージは現在の実装が返す値であり、安定した API ではない (変わらないのは例外型と throw される条件であり、文言は予告なく変わりうる)。

受理より後の失敗 — factory の失敗や器への取付失敗、型を渡す `Show` の ViewModel factory と `configure` が投げた失敗 — は例外にならない。警告を記録してその Toast 1 枚だけを破棄し、既に戻っている呼び出し元へは返らない。他の Toast は影響を受けない。Dialog と Loading の型を渡す表示ではこれらの失敗が呼び出し元へ伝播するので、Toast だけ扱いが違う。

たとえば起動時に `RegisterForToast` も `Registry.Register` も呼ばないまま `SavedToastViewModel` を表示すると、`SavedToastView` を解決できず `DialogException.ViewFactoryNotRegistered` になる。

```csharp
public static MauiApp CreateMauiApp()
{
    var builder = MauiApp.CreateBuilder();
    builder.UseMauiApp<App>();
    builder.Services
        .AddSingleton<IKsToast>(_ => Toast.Instance)
        .AddTransient<ItemPage>();

    return builder.Build();
}
```

呼び出し元では入れ子の例外型でそのまま `catch` できる。構成ミスは実行時に直せるものではないので、ログに残したうえで再 throw し、開発中に気づける形にする。

```csharp
private void OnSaveClicked(object? sender, EventArgs e)
{
    try
    {
        _toast.Show(new SavedToastViewModel { Message = "Saved" });
    }
    catch (DialogException.ViewFactoryNotRegistered ex)
    {
        Debug.WriteLine($"Toast view factory is not registered for {ex.ViewModelTypeName}.");
        throw;
    }
}
```

まとめて扱いたい場合は、基底型の `DialogException` で `catch` する。
