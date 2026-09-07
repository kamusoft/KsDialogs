# transition preset を添付する

出入りの演出は `DialogTransition` にまとめ、content View に attached property で添付する。show の引数で渡す経路はない。

- `DialogTransition` は closure を持つため **XAML には書けない**。添付は code-behind から行う ([レイアウト](layout.md) の属性は XAML に書けるが、transition だけは書けない)
- 添付は `Dialog.SetTransition(this, DialogTransition.Slide(DialogTransitionEdge.Bottom))` のように content View 自身へ行う
- 添付された値を読むのは `Dialog.GetTransition(view)`。未添付なら `null` が返る
- `BindableProperty` そのものを扱うときは `Dialog.TransitionProperty` を使う

## preset を選ぶ

| preset | 署名 | 演出 |
|---|---|---|
| Fade | `DialogTransition.Fade(duration, easing)` | 透明度で出入りする |
| Slide | `DialogTransition.Slide(from, duration, easing)` | `from` の辺から滑り込み、同じ辺へ滑り出す |
| Zoom | `DialogTransition.Zoom(duration, easing)` | 少し縮んだ状態から等倍へ広がり、同じ倍率へ縮んで消える |
| None | `DialogTransition.None()` | content 側は無演出 |

preset は `Presentation`、`Dismissal`、`OverlayDuration` がすべて埋まった `DialogTransition` を返す。

| 引数 | 型 | 既定 | 意味 |
|---|---|---|---|
| `from` | `DialogTransitionEdge` | なし (`Slide` でのみ必須) | 滑り込み・滑り出しの辺 |
| `duration` | `TimeSpan` | 250 ミリ秒 | 片道の時間 |
| `easing` | `Easing` | `Easing.CubicInOut` | 時間に対する進み方 |

| `from` に渡す値 | 向き |
|---|---|
| `Top` / `Bottom` | 物理方向のまま変わらない |
| `Start` / `End` | layout 方向に追随する |

## 既定の挙動と細則

- **未添付時** — content と overlay が 250 ミリ秒で cross-fade する
- **`None`** — content だけを即時化する。overlay は fade する
- **並行実行** — content と overlay は並行に走り、表示・退出・結果配送はいずれも両方の完了を待つ
- **成立しない `duration`** — 0、負値、総ミリ秒が `uint` 上限を超える値は例外にせず、即時に最終状態へ移る
- **添付値の採用時点** — 初回 native layout の後。表示中の Dialog で変更しても現在の表示には反映されない

## ViewModel を宣言する

演出は content 側の関心なので、ViewModel には何も足さない。

```csharp
using System.Windows.Input;
using KsDialogs;
using Microsoft.Maui.Controls;

namespace MyApp;

public sealed class NoticeDialogViewModel : IDialogViewModel
{
    public NoticeDialogViewModel(string message)
    {
        Message = message;
        CloseCommand = new Command(() => this.Notifier?.Complete(true));
    }

    public string Message { get; }

    public ICommand CloseCommand { get; }
}
```

## View を XAML で書く

```xml
<?xml version="1.0" encoding="utf-8" ?>
<ContentView xmlns="http://schemas.microsoft.com/dotnet/2021/maui"
             xmlns:x="http://schemas.microsoft.com/winfx/2009/xaml"
             xmlns:local="clr-namespace:MyApp"
             x:Class="MyApp.NoticeDialogView"
             x:DataType="local:NoticeDialogViewModel">

    <Border WidthRequest="272" Padding="20" StrokeThickness="0">
        <Border.StrokeShape>
            <RoundRectangle CornerRadius="16" />
        </Border.StrokeShape>

        <VerticalStackLayout Spacing="16">
            <Label Text="{Binding Message}" HorizontalTextAlignment="Center" />
            <Button Text="OK" Command="{Binding CloseCommand}" />
        </VerticalStackLayout>
    </Border>

</ContentView>
```

## code-behind で transition を添付する

content View の構築時に添付すれば、初回 native layout より前になるので必ず効く。

```csharp
using System;
using KsDialogs;
using Microsoft.Maui.Controls;

namespace MyApp;

public partial class NoticeDialogView : ContentView
{
    public NoticeDialogView()
    {
        InitializeComponent();
        Dialog.SetTransition(
            this,
            DialogTransition.Slide(DialogTransitionEdge.Bottom, TimeSpan.FromMilliseconds(300)));
    }
}
```

show ごとに演出を変えたい場合は、View factory の中で `Dialog.SetTransition` を呼ぶ。

## 登録して呼び出す

登録と呼び出しは transition を使わない Dialog と同じで、添付した演出は `ShowAsync` の表示・退出でそのまま走る ([Dialog](dialogs.md))。

```csharp
Dialog.Instance.Registry.Register<NoticeDialogViewModel>(
    viewModel => new NoticeDialogView { BindingContext = viewModel });
```

```csharp
private async void OnSavedClicked(object? sender, EventArgs e)
{
    var result = await _dialogs.ShowAsync(new NoticeDialogViewModel("Saved"));
    StatusLabel.Text = result is DialogResult<bool>.Completed ? "Closed" : "Dismissed";
}
```

## custom 非同期 hook を渡す

constructor は `DialogTransition(presentation, dismissal, overlayDuration)` で、3 引数とも省略できる。

- 省略した側の hook はライブラリ既定のままになる
- どちらの hook も `Func<VisualElement, Task>` で、layout 済みの content を載せた MAUI View を受け取り、UI thread 上で開始される
- transition が保持する値は `DialogTransition.Presentation`、`DialogTransition.Dismissal`、`DialogTransition.OverlayDuration` から読める
- hook の fault や cancellation はログへ吸収され、Dialog の結果には伝播しない
- ライブラリは timeout を設けず hook の完了を待つため、完了しない Task は器の撤去と結果配送を止める

添付の仕方は preset と同じで、content View の code-behind から `Dialog.SetTransition` に渡す。

```csharp
using System;
using KsDialogs;
using Microsoft.Maui.Controls;

namespace MyApp;

public partial class NoticeDialogView : ContentView
{
    public NoticeDialogView()
    {
        InitializeComponent();
        Dialog.SetTransition(
            this,
            new DialogTransition(
                presentation: async view =>
                {
                    view.Opacity = 0;
                    await view.FadeToAsync(1, 180, Easing.CubicOut);
                },
                dismissal: view => view.FadeToAsync(0, 140, Easing.CubicIn),
                overlayDuration: TimeSpan.FromMilliseconds(180)));
    }
}
```

これで `ShowAsync(new NoticeDialogViewModel("Saved"))` の表示時に `presentation` が、閉じるときに `dismissal` が呼ばれ、overlay は 180 ミリ秒で fade する。登録も呼び出し元も preset のときから変わらない。

## preset と custom hook を組み合わせる

preset から片方の hook だけを取り出し、自作の hook と一緒に constructor へ渡す。組んだ値の添付も preset と同じである。

```csharp
using System;
using KsDialogs;
using Microsoft.Maui.Controls;

namespace MyApp;

public partial class NoticeDialogView : ContentView
{
    public NoticeDialogView()
    {
        InitializeComponent();
        var zoomIn = DialogTransition.Zoom(TimeSpan.FromMilliseconds(200));
        Dialog.SetTransition(
            this,
            new DialogTransition(
                presentation: zoomIn.Presentation,
                dismissal: view => view.FadeToAsync(0, 140, Easing.CubicIn),
                overlayDuration: TimeSpan.FromMilliseconds(140)));
    }
}
```

`OverlayDuration` だけを preset に揃えたい場合は `zoomIn.OverlayDuration` を、閉鎖だけを preset にしたい場合は `zoomIn.Dismissal` を同じように渡す。
