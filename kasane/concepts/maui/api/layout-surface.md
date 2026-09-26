---
type: concept
title: MAUI のレイアウト公開面
description: .NET MAUI (C#) でダイアログの大きさと位置を指定するときの公開名と署名 — 項目ごとの添付プロパティと Get / Set・束ねた値オブジェクト・show 引数での置き場所指定・表示中のページの決まり方 (MAUI 層の既定の探し方と上書きの登録)・XAML から書ける範囲・移植元の属性名との対応
tags: [maui, layout, api, surface]
timestamp: 2026-09-27
---

# MAUI のレイアウト公開面

この文書を読むと、.NET MAUI (C#) でダイアログの大きさ・位置・背後の覆い・外側タップの扱いを指定するときに書く添付プロパティ名と型、XAML と code-behind での書き方、基準領域「表示中のページ」のページがどう決まり、どう上書きするか、移植元 (AiForms.Maui.Dialogs) の属性名との対応が分かる。

**この文書は MAUI の公開面 (名前・署名・コード例・framework 固有の注意) だけを扱い、挙動の契約は core が正である。** 対応する契約は [レイアウトのルール](../../core/api/layout-semantics.md) で、属性の意味・既定値・優先順位・rect の決まり方はそちらを読む。

## 添付プロパティ (項目ごとのスカラー)

MAUI では属性を**項目ごとの添付プロパティ**として `Dialog` クラスに置いている。XAML では中身の View に `ksd:Dialog.OverlayColor="#80000000"` のように書き、code-behind からは `Dialog.SetOverlayColor(view, color)` で設定する。添付しなかった項目は契約の既定値になる。

| 添付プロパティ | 型 | 供給するもの |
|---|---|---|
| `Dialog.LayoutArea` | `DialogLayoutArea` (`Window` / `VisibleArea` / `CurrentPage`) | 基準領域 |
| `Dialog.DialogMargin` | `Thickness` | 4 辺の余白 |
| `Dialog.ProportionalWidth` / `Dialog.ProportionalHeight` | `double` | 基準 rect に対する比率 |
| `Dialog.OverlayColor` | `Color?` | 背後を覆う色 (null は未添付と同じで契約の既定値) |
| `Dialog.IsCanceledOnTouchOutside` | `bool` | 外側タップの扱い |
| `Dialog.HorizontalAlignment` / `Dialog.VerticalAlignment` | `DialogAlignment` (`Start` / `Center` / `End` / `Fill`) | 配置 |
| `Dialog.OffsetX` / `Dialog.OffsetY` | `double` | 配置後の移動量 |

各項目には項目名を含む静的メソッド (`Dialog.GetLayoutArea(view)` / `Dialog.SetLayoutArea(view, value)` のような対) と、`LayoutAreaProperty` のように項目名 + `Property` の `BindableProperty` がある。添付プロパティの置き場である `Dialog` は表示エントリ `Dialog.Instance` を持つ型と同じで、iOS / Android のように中身の View 側の拡張として添付するのではなく、`Dialog` の静的メンバから添付する。

```csharp
Dialog.SetLayoutArea(contentView, DialogLayoutArea.Window);
Dialog.SetDialogMargin(contentView, new Thickness(16d));
Dialog.SetProportionalWidth(contentView, 0.9d);
Dialog.SetIsCanceledOnTouchOutside(contentView, false);
Dialog.SetVerticalAlignment(contentView, DialogAlignment.End);
Dialog.SetOffsetY(contentView, -24d);
```

## 束ねた値オブジェクト

添付された静的メタ属性は `DialogOptions` (record) に束ねられて Native へ運ばれる。プロパティ名は添付プロパティと同名 (`LayoutArea` / `DialogMargin` / `ProportionalWidth` / `ProportionalHeight` / `OverlayColor` / `IsCanceledOnTouchOutside`) である。

ダイアログの供給面はあくまで `Dialog.*` の添付プロパティだが、既定ローディング (ライブラリが用意する待機表示で、中身の View を利用者が用意しない) は属性を添付する先の View を持たないため、`DialogOptions` を直接受け取る供給経路がある ([core/ADR-0023](../../../decisions/core/0023-default-loading-builtin-content.md))。そのため `DialogOptions` も公開型である。

置き場所は `DialogPlacement` (record) で、`HorizontalAlignment` / `VerticalAlignment` / `OffsetX` / `OffsetY` を持つ。

## show 引数での置き場所指定

`ShowAsync` の `placement` 引数 (省略可、既定は `null`) に `DialogPlacement` を渡すと、添付された置き場所をまるごと置換する。`null` なら添付、添付もなければ契約の既定値が使われる。静的メタ属性を渡す引数は無い。

MAUI は添付が項目ごとのスカラーなので、まるごと置換の帰結に注意する — 置き場所に属する 4 項目 (`Dialog.HorizontalAlignment` / `Dialog.VerticalAlignment` / `Dialog.OffsetX` / `Dialog.OffsetY`) はまとめて置換されるため、`Dialog.SetOffsetY` だけ添付していても `placement` を渡した時点でその値は使われない。

```csharp
DialogResult<bool> result = await Dialog.Instance.ShowAsync(
    new ConfirmViewModel("削除しますか?"),
    new DialogPlacement { HorizontalAlignment = DialogAlignment.Fill });
```

## 表示中のページの決まり方

`DialogLayoutArea.CurrentPage` を添付すると (XAML なら `ksd:Dialog.LayoutArea="CurrentPage"`)、ダイアログを出すウィンドウの表示中のページを基準にする。値は bridge (iOS の `KSDMauiDialogLayoutArea` / Android の `MauiDialogLayoutArea`) を経て Native の値 (iOS は `DialogLayoutArea.currentPage`、Android は `DialogLayoutArea.CURRENT_PAGE`) へそのまま渡る。ページは MAUI 層で決めて Native へ教えるので、**標準のページ構成なら何も登録せずに効く**。

| 順 | 取得元 | 書く名前 |
|---|---|---|
| 1 | 上書きの登録 | `DialogCurrentPage.Provider` (`Func<VisualElement?>?`) |
| 2 | MAUI 層の既定の探し方 | なし (登録不要) |

MAUI 層で決めたページの `Handler.PlatformView` は、Native 側の「登録した関数」の口へ渡る。基準になるのはその platform view の矩形 (iOS は safe area の内側) と可視領域の共通部分で、MAUI の標準のページではタブバー・ナビゲーションバーの内側になる。

### 既定の探し方 (両 OS で同じ)

ダイアログを出す `Window` (ダイアログの提示に使う `IMauiContext` と一組で解決する) を起点に、次のように辿る:

1. `Window.Navigation.ModalStack` にページがあればいちばん上のもの (最後に積んだもの)、無ければ `Window.Page` から始める
2. `Shell` → `CurrentPage`、`FlyoutPage` → `Detail`、`TabbedPage` → `CurrentPage`、`NavigationPage` → `CurrentPage` を、容れ物でなくなるまで降りる
3. 降りきったページの `Handler.PlatformView` をページとする。`Handler` が無い (まだ描画されていない) ページは得られなかった扱いになる

原典が扱えなかった `Shell` とモーダルもこの辿り方で届く。iOS Native が内蔵する view controller 階層の探し方は、MAUI では使わない (MAUI 層の結果が常に優先する)。

### 上書き: `DialogCurrentPage.Provider`

独自の切り替えで画面を組んでいる、あるいはページの一部の領域を基準にしたいときは、基準にするページまたは要素を返す関数を一度登録する。登録は既定の探し方より優先し、`null` を代入すると既定に戻る。表示開始時点で登録されていた関数が、UI スレッドで、その表示の開始時と、表示中にウィンドウの寸法やシステムバーの幅が変わったときに呼ばれる (表示中に登録を差し替えても、そのダイアログには古い関数が使われ続ける)。

```csharp
// 画面内の特定の領域を基準にする
DialogCurrentPage.Provider = () => mainPage.ContentArea;
```

関数が `null` を返す・例外を投げる・要素がまだ描画されていない・その platform view を Native がページとして受け付けないときは、既定の探し方で得たページへ進む。受け付けるかどうかは MAUI 層で Native と同じ条件で先に確かめる — 渡した後で Native に外されると、既定の探し方を経ずに可視領域へ落ちてしまうためである。条件は次のとおりで、Native が登録した関数の View を外す条件と同じである:

| OS | 受け付ける条件 |
|---|---|
| iOS | ダイアログを出す window に載っている・safe area の内側が空でない・window と重なる |
| Android | ダイアログを出す Activity が持つウィンドウに載っている (KsDialogs の器のウィンドウは除く)・矩形が空でない・Activity のメインウィンドウと重なる ([Android のレイアウト公開面](../../android/api/layout-surface.md) の「候補になるウィンドウと座標の突き合わせ」) |

### ページが得られないとき (OS 差)

どちらの取得元からも得られないとき、結果は**両 OS とも `VisibleArea` と同じ**になる。違うのは診断の出先と、そこに至る経路だけである。Android は MAUI 層が見つからなかった理由をログに出して何も返さず、Native が診断つきで可視領域へ落とす。iOS は診断を MAUI 層が `Trace` に出し、Native の view controller 階層の探し方へは進ませない。そのため safe area の内側が可視領域を覆う View (ダイアログを出す window の root から present の連なりを辿った view controller の view のうち最初に該当するもの。通常は root の view、全画面のモーダル中はモーダルの入れ物の view) を Native へ返して、可視領域と同じ結果にする。window 自身は `UIView.Window` が自分を指さず Native に外されるため返さない。

## framework 固有の注意

- **XAML から書けるのはレイアウト属性まで**である。値がクロージャを持つ出入りの演出だけは XAML に書けず、code-behind から添付する ([MAUI のトランジション公開面](transition-surface.md))
- **余白は `Thickness`** で渡す。MAUI の標準型をそのまま使い、契約の 4 辺の余白へ写す
- **覆いの色は `Color`**。添付そのものが無ければ契約の既定値 (黒 40%) で、`null` を明示的に添付したときだけ透明になる。境界を渡るときは ARGB 32bit 整数になる
- **MAUI 層はレイアウト計算を行わない**。添付された値を無変換で Native へ渡すだけで、実際の rect は iOS / Android の実装が決める ([レイアウトのルール](../../core/api/layout-semantics.md))

## 移植元 (AiForms.Maui.Dialogs) の属性名との対応

移植元は基準領域を `UseCurrentPageLocation` という真偽値の View プロパティで持っていた。KsDialogs では基準領域を列挙で表し、水平・垂直の両軸に効かせるため、対応する書き方は `Dialog.SetLayoutArea` (XAML なら `ksd:Dialog.LayoutArea`) になる ([core/ADR-0008](../../../decisions/core/0008-layout-attributes-deliberate-deviations.md))。値は `true` なら `DialogLayoutArea.CurrentPage`、`false` なら `DialogLayoutArea.Window` へ写す。移植元の `true` は垂直方向にしか効かず、ページも `FlyoutPage` / `TabbedPage` / `NavigationPage` しか辿らなかったが、`CurrentPage` は両軸に効き、`Shell` とモーダルも既定の探し方で届く ([core/ADR-0038](../../../decisions/core/0038-current-page-layout-area-via-registered-provider.md))。

## 関連

- [レイアウトのルール](../../core/api/layout-semantics.md) — 属性の意味・既定値・優先順位・rect の決まり方 (契約の記述はこちら)
- [MAUI の Dialog 公開面](dialog-surface.md) — 登録・表示・結果の受け取りの公開面
- [MAUI の DI 連携と登録糖衣](di-registration.md) — 1行登録と fallback resolver
- [MAUI のトランジション公開面](transition-surface.md) — 出入りの演出の添付とフックの型
