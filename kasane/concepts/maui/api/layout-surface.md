---
type: concept
title: MAUI のレイアウト公開面
description: .NET MAUI (C#) でダイアログの大きさと位置を指定するときの公開名と署名 — 項目ごとの添付プロパティと Get / Set・束ねた値オブジェクト・show 引数での置き場所指定・XAML から書ける範囲・移植元の属性名との対応
tags: [maui, layout, api, surface]
timestamp: 2026-09-06
---

# MAUI のレイアウト公開面

この文書を読むと、.NET MAUI (C#) でダイアログの大きさ・位置・背後の覆い・外側タップの扱いを指定するときに書く添付プロパティ名と型、XAML と code-behind での書き方、移植元 (AiForms.Maui.Dialogs) の属性名との対応が分かる。

**この文書は MAUI の公開面 (名前・署名・コード例・framework 固有の注意) だけを扱い、挙動の契約は core が正である。** 対応する契約は [レイアウトのルール](../../core/api/layout-semantics.md) で、属性の意味・既定値・優先順位・rect の決まり方はそちらを読む。

## 添付プロパティ (項目ごとのスカラー)

MAUI では属性を**項目ごとの添付プロパティ**として `Dialog` クラスに置いている。XAML では中身の View に `ksd:Dialog.OverlayColor="#80000000"` のように書き、code-behind からは `Dialog.SetOverlayColor(view, color)` で設定する。添付しなかった項目は契約の既定値になる。

| 添付プロパティ | 型 | 供給するもの |
|---|---|---|
| `Dialog.LayoutArea` | `DialogLayoutArea` (`Window` / `VisibleArea`) | 基準領域 |
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

## framework 固有の注意

- **XAML から書けるのはレイアウト属性まで**である。値がクロージャを持つ出入りの演出だけは XAML に書けず、code-behind から添付する ([MAUI のトランジション公開面](transition-surface.md))
- **余白は `Thickness`** で渡す。MAUI の標準型をそのまま使い、契約の 4 辺の余白へ写す
- **覆いの色は `Color`**。添付そのものが無ければ契約の既定値 (黒 40%) で、`null` を明示的に添付したときだけ透明になる。境界を渡るときは ARGB 32bit 整数になる
- **MAUI 層はレイアウト計算を行わない**。添付された値を無変換で Native へ渡すだけで、実際の rect は iOS / Android の実装が決める ([レイアウトのルール](../../core/api/layout-semantics.md))

## 移植元 (AiForms.Maui.Dialogs) の属性名との対応

移植元は基準領域を `UseCurrentPageLocation` という真偽値の View プロパティで持っていた。KsDialogs では基準領域を列挙で表し、水平・垂直の両軸に効かせるため、対応する書き方は `Dialog.SetLayoutArea` (XAML なら `ksd:Dialog.LayoutArea`) になる ([core/ADR-0008](../../../decisions/core/0008-layout-attributes-deliberate-deviations.md))。

## 関連

- [レイアウトのルール](../../core/api/layout-semantics.md) — 属性の意味・既定値・優先順位・rect の決まり方 (契約の正)
- [MAUI の Dialog 公開面](dialog-surface.md) — 登録・表示・結果の受け取りの公開面
- [MAUI の DI 連携と登録糖衣](di-registration.md) — 1行登録と fallback resolver
- [MAUI のトランジション公開面](transition-surface.md) — 出入りの演出の添付とフックの型
