# content にレイアウト挙動を添付する

大きさ・位置・覆い・外側タップの扱いは、content View に添付して指定する。添付には `Dialog` クラスの静的メンバを使う。添付しなかった項目はライブラリの既定値になる。

## 添付できる項目

| attached property | 型 | 供給するもの | 既定値 |
|---|---|---|---|
| `Dialog.LayoutArea` | `DialogLayoutArea` (`Window` / `VisibleArea`) | サイズと位置の計算の基準領域 | `VisibleArea` |
| `Dialog.DialogMargin` | `Thickness` | 基準領域の各辺から控除する余白 | 全辺 24 |
| `Dialog.ProportionalWidth` / `Dialog.ProportionalHeight` | `double` | その軸の基準領域に対する比率 | `-1` (未指定) |
| `Dialog.OverlayColor` | `Color?` | Dialog の背後を覆う色 | 黒 40% |
| `Dialog.IsCanceledOnTouchOutside` | `bool` | 外側タップでキャンセルするか | `true` |
| `Dialog.HorizontalAlignment` / `Dialog.VerticalAlignment` | `DialogAlignment` (`Start` / `Center` / `End` / `Fill`) | その軸の配置 | `Center` |
| `Dialog.OffsetX` / `Dialog.OffsetY` | `double` | 配置後の平行移動 | `0` |

`DialogAlignment` の `Start` と `End` は物理方向 (水平軸なら左と右、垂直軸なら上と下) で、書字方向には追随しない。

表の各名前 `X` には `Dialog.GetX` / `Dialog.SetX` の対と、`Dialog.XProperty` という `BindableProperty` がある。たとえば `Dialog.GetLayoutArea`、`Dialog.SetLayoutArea`、`Dialog.LayoutAreaProperty` である。

## サイズと位置の細則

| 事項 | ルール |
|---|---|
| 比率の有効域 | `0` 以下は「未指定」。`1` を超える値は `1` に丸める |
| 軸ごとの優先順位 | 1 つの軸では比率サイズが `DialogAlignment.Fill` に勝つ。そのとき `Fill` は中央配置として働く |
| クランプ | 得られたサイズは `DialogMargin` を控除した領域に収まるよう切り詰める |
| offset | クランプしないため、意図的に画面外へ押し出せる |
| 計算する場所 | MAUI は添付された値を無変換で渡し、rect は Native 側が計算する |

## 実効値が決まる時点

| 出来事 | 結果 |
|---|---|
| 初回の native layout pass が完了 | その時点で添付されていた値が実効値になる |
| 表示中に添付値を変更 | 反映されない |
| window の寸法や system insets が変化 | 実効値はそのままで再配置する |
| ソフトキーボードの開閉 | 動かない |

## XAML から添付する

ライブラリの namespace を宣言し、content View に attached property を書く。XAML で表現できるのはレイアウト属性までで、transition の値は closure を持つため code-behind から添付する ([トランジション](transitions.md))。

以下は、`ContentView` を window 全体を基準に幅 9 割・下寄せで出すための添付をまとめて書いた例である。

```xml
<?xml version="1.0" encoding="utf-8" ?>
<ContentView xmlns="http://schemas.microsoft.com/dotnet/2021/maui"
             xmlns:x="http://schemas.microsoft.com/winfx/2009/xaml"
             xmlns:ksd="clr-namespace:KsDialogs;assembly=KsDialogs.Maui"
             x:Class="MyApp.ConfirmContentView"
             ksd:Dialog.LayoutArea="Window"
             ksd:Dialog.DialogMargin="16"
             ksd:Dialog.ProportionalWidth="0.9"
             ksd:Dialog.OverlayColor="#88000000"
             ksd:Dialog.IsCanceledOnTouchOutside="False"
             ksd:Dialog.HorizontalAlignment="Center"
             ksd:Dialog.VerticalAlignment="End"
             ksd:Dialog.OffsetY="-24">
    <Label Text="Delete this item?" />
</ContentView>
```

## code-behind から添付する

content をコードで組み立てるときは、同じ値を `Dialog.SetX` メソッドで設定する。content View の構築時に添付すれば初回の native layout より前になるので必ず効く。

以下は、上の XAML と同じ添付を、コードで組み立てた content View の constructor で行う例である。

```csharp
using System.Windows.Input;
using KsDialogs;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Graphics;

namespace MyApp;

public sealed class ConfirmCardView : ContentView
{
    public ConfirmCardView(string message, ICommand closeCommand)
    {
        Content = new VerticalStackLayout
        {
            Spacing = 16,
            Children =
            {
                new Label { Text = message },
                new Button { Text = "OK", Command = closeCommand },
            },
        };

        Dialog.SetLayoutArea(this, DialogLayoutArea.Window);
        Dialog.SetDialogMargin(this, new Thickness(16));
        Dialog.SetProportionalWidth(this, 0.9);
        Dialog.SetOverlayColor(this, Color.FromArgb("#88000000"));
        Dialog.SetIsCanceledOnTouchOutside(this, false);
        Dialog.SetHorizontalAlignment(this, DialogAlignment.Center);
        Dialog.SetVerticalAlignment(this, DialogAlignment.End);
        Dialog.SetOffsetY(this, -24);
    }
}
```

表示ごとに値を変えたい場合は、登録した View factory の中で `Dialog.SetX` を呼ぶ。

## show の引数で置き場所を渡す

配置の 4 項目は `DialogPlacement` にも束ねられている。`ShowAsync` の末尾にある省略可能な `placement` 引数に渡すと、content に添付された配置をまるごと置換する。

- 置換は 4 項目まとめて起きる。添付した `OffsetY` は、それを書いていない `placement` を渡した時点で使われなくなる
- `placement` を渡さなければ添付値を使い、添付もなければ既定値になる
- `OverlayColor` や `IsCanceledOnTouchOutside` のような静的 options に対応する show 引数はない

以下は、同じ content を呼び出しごとに違う位置へ出す例である。ViewModel の宣言と登録は [Dialog](dialogs.md) と同じで、`_dialogs` は注入した `IKsDialog` である。

```csharp
private async void OnDeleteClicked(object? sender, EventArgs e)
{
    var placement = new DialogPlacement
    {
        HorizontalAlignment = DialogAlignment.Center,
        VerticalAlignment = DialogAlignment.Start,
        OffsetY = 16,
    };

    var result = await _dialogs.ShowAsync(new ConfirmDialogViewModel("Delete this item?"), placement);
    StatusLabel.Text = result is DialogResult<bool>.Completed ? "Deleted" : "Kept";
}
```

## 値オブジェクトにまとめる

| 型 | 束ねる項目 | 供給経路 |
|---|---|---|
| `DialogPlacement` | `HorizontalAlignment`、`VerticalAlignment`、`OffsetX`、`OffsetY` | 項目ごとの添付、または `ShowAsync` の `placement` 引数 |
| `DialogOptions` | `LayoutArea`、`DialogMargin`、`ProportionalWidth`、`ProportionalHeight`、`OverlayColor`、`IsCanceledOnTouchOutside` | 項目ごとの添付 |

どちらも record なので `with` で一部だけ差し替えられる。組み込み Loading content は添付先の View を持たないため、そこだけは `Loading.Instance.Options` が `DialogOptions` を直接受け取る ([Loading](loading.md))。
