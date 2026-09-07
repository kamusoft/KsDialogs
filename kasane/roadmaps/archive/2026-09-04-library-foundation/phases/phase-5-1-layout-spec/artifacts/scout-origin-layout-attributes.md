# scout 調査: 原典レイアウト属性の全量と KsDialogs 現状 (2026-08-17)

phase-5-1 論点「レイアウト属性一式の取捨」の議論素材。ksn-scout による調査報告 (調査2の一部は未確認と明記)。

---

## 調査1: 原典 AiForms.Maui.Dialogs のレイアウト関連属性 全量

**移植元パス**: `../AiForms.Maui.Dialogs`
(特定元: `kasane/concepts/cross/conventions/reference-repositories.md` の対応表)

### 1-A. ExtraView (DialogView / LoadingView / ToastView の共通基底)

定義: `AiForms.Maui.Dialogs/ExtraView.cs` / README 説明: `README.md:583-609`

| 属性 | 型 | 既定値 | 意味 (README ベース) | iOS / Android 実装での扱いの差 |
|---|---|---|---|---|
| ProportionalWidth | double | `-1` | デバイス幅に対する比率 (0-1)。未指定なら WidthRequest または AutoSizing | **差なし** (Measure が行対応のコピー)。基準サイズだけ差: iOS = `window.Bounds` (iOS/DialogHelpers.cs:56-58)、Android = `ContentSize` (=WindowVisibleDisplayFrame、ステータスバー除く) (Android/DialogHelpers.cs:140-141) |
| ProportionalHeight | double | `-1` | デバイス高に対する比率 (0-1)。未指定なら HeightRequest または AutoSizing | 同上 |
| HorizontalLayoutAlignment | LayoutAlignment | `Center` | 水平位置 (Start/Center/End/Fill) | **実装機構が別**。iOS = `DialogPresentationController.GetHorizontalPosition` で Frame 座標計算 (iOS/DialogPresentationController.cs:88-100)、Android = `GravityFlags` へ変換 (Android/DialogHelpers.cs:275-303 `GetGravity`)。`Fill` は Measure 側で「画面幅 − DialogMargin 左右」に化ける |
| VerticalLayoutAlignment | LayoutAlignment | `Center` | 垂直位置 (Start/Center/End/Fill) | 同上 (iOS: `GetVerticalPosition` cs:102-118 / Android: Gravity)。**iOS のみ `UseCurrentPageLocation` が垂直側だけに効く** (後述) |
| OffsetX | int | `0` | 水平レイアウト位置からの相対調整値 | iOS = Frame の X に加算 (DialogPresentationController.cs:79)、Android = `FrameLayout.LayoutParams` のマージン。**Android は End 指定時に符号反転** (`RightMargin = offsetX * -1`) (Android/DialogHelpers.cs:203-226 `SetOffsetMargin`)。dp→px 変換あり |
| OffsetY | int | `0` | 垂直レイアウト位置からの相対調整値 | 同上。Android は VerticalAlignment=End のとき `BottomMargin = offsetY * -1` |
| CornerRadius | float | `0` | ダイアログの角丸 | iOS = `Layer.CornerRadius` + `MasksToBounds` (Dialog/ReusableDialog.iOS.cs:50-54)。Android = `GradientDrawable.SetCornerRadius`、**BorderWidth と併用時のみ `CardView` でラップして二重描画** (内側半径 = CornerRadius − BorderWidth、下限0) (Android/DialogHelpers.cs:228-273 `SetViewAppearance`) |
| BorderWidth | double | `0` | 枠線幅 | iOS = `Layer.BorderWidth` (内側に描画、コンテンツを押し出さない)。Android = `GradientDrawable.SetStroke` + **`SetPadding(borderW)` でコンテンツを内側に押し込む**、CardView 経路では `SetContentPadding`。**iOS/Android で内容領域の実効サイズが変わる差** |
| BorderColor | Color | `Transparent` | 枠線色 | 同上 |
| DialogMargin | Thickness | `default(0)` | ウィンドウとダイアログの間のマージン | **Measure での最大サイズ算出にしか使われない** (iOS/DialogHelpers.cs:62-65, Android:146-149)。Fill 時の幅 = 画面幅−左右、maxHeight = 画面高−上下。**位置決めには一切効かない** (Start/End 時に端から DialogMargin だけ離れる、という挙動にはならない) — iOS/Android 共通の仕様上の穴 |
| AutoRotateForIOS | bool | `true` | iOS で画面自動回転を許可するか | **iOS 専用**。`ContentViewController.ShouldAutorotate()` の戻り値 (Native/iOS/ContentViewController.cs)。Android は無視 |
| WidthRequest (MAUI 標準) | double | `-1` | Proportional 未指定時の固定幅 | 共通。`-1` なら無限大で Measure して内容サイズ採用 |
| HeightRequest (MAUI 標準) | double | `-1` | Proportional 未指定時の固定高 | 共通。内容サイズは `maxHeight` (画面高−上下マージン) でクランプされる |

補足: `ExtraView.OnPropertyChanged` (ExtraView.cs:167-188) が Width/HeightRequest 変更を検知して自前で Measure→Arrange→`LayoutNative()` を呼ぶ = **動的リサイズ経路**。`LayoutNative` の実体は Android 側にしかない (Dialog/ReusableDialog.Android.cs:95-115) — **iOS は動的リサイズ非対応**という差。

Margin / Padding (MAUI 標準の ContentView プロパティ) はレイアウト仕様として原典は特に規定していない (DialogMargin が別物として存在する)。

### 1-B. DialogView 固有

定義: `AiForms.Maui.Dialogs/Dialog/DialogView.cs` / README.md:629-635

| 属性 | 型 | 既定値 | 意味 | iOS / Android 差 |
|---|---|---|---|---|
| IsCanceledOnTouchOutside | bool | `true` | ダイアログ外タップでキャンセルするか | iOS = オーバーレイ view の `TouchBeginGestureRecognizer` (ReusableDialog.iOS.cs:196-202)。Android = `_contentView.Touch` で `GetHitRect` 内外判定 (ReusableDialog.Android.cs:276-294)。**Android のみ加えて Back キーで常時キャンセル** (この属性と無関係、ExtraPlatformDialog.cs:78-88) |
| OverlayColor | Color | `Transparent` | ダイアログ外の背景色 | iOS = 専用 `_overlayView` の背景色 + present/dismiss で Alpha 0↔1。Android = `_contentView.SetBackgroundColor`。**Android のみ「透明/未指定のときステータスバーが暗くなるのを避ける」ための特殊分岐**があり、Gravity=Bottom + 高さ ContentSize に切り替え + top padding 補正が入る (ExtraPlatformDialog.cs:41-50, ReusableDialog.Android.cs:61-70) |
| UseCurrentPageLocation | bool | `false` | Horizontal/Vertical LayoutAlignment の基準を「ウィンドウ全体」ではなく「現在ページ領域」にする | **実装差が大きい**。iOS = `GetVerticalPosition` の中だけで使われ**垂直方向のみ**基準が変わる (DialogPresentationController.cs:104-107)。Android = `_contentView` の上下 padding として実現され (CalcWindowPadding、ReusableDialog.Android.cs:63-70 / Android/DialogHelpers.cs:305-321)、結果的にやはり上下方向のみ。水平方向は両 OS とも常にウィンドウ基準 |

(DialogNotifier はレイアウト外)

### 1-C. LoadingConfig (Loading のグローバル既定。レイアウトに関わる分のみ)

定義: `AiForms.Maui.Dialogs/Loading/LoadingConfig.cs` / README.md:548-577

| 属性 | 型 | 既定値 | 意味 |
|---|---|---|---|
| OffsetX / OffsetY | int | `0` | 既定 Loading の位置調整 |
| OverlayColor | Color | `Rgb(0,0,0)` | オーバーレイ色 |
| Opacity | double | `0.6` | 全体不透明度 |

### 1-D. 原典レイアウト計算のアルゴリズム (iOS/Android 同一)

`iOS/DialogHelpers.cs:52-122` と `Android/DialogHelpers.cs:139-209` が**日本語コメントまで含めほぼ行対応のコピー**。優先順位:

- 幅: `ProportionalWidth >= 0` → 画面幅×比率 / `Horizontal == Fill` → 画面幅 − 左右 DialogMargin / `WidthRequest == -1` → ∞ で Measure (内容サイズ) / それ以外 → WidthRequest
- 高さ: 同型。ただし内容サイズ採用時は `maxHeight = 画面高 − 上下 DialogMargin` で **Min クランプ**
- 位置: Measure 結果を各 OS のレイアウト機構 (iOS=Frame 計算 / Android=Gravity+Margin) に流す

---

## 調査2: KsDialogs 側の現状 (scout が確認できた範囲)

### 確認できたこと

**(a) core 契約にレイアウト属性は「まだ存在しない」**
- `kasane/concepts/core/api/` にあるのは `result-notification-semantics.md` と `multi-display-semantics.md` の2本のみ。レイアウト仕様の concepts は未作成
- `kasane/concepts/index.md` でも ios / android / maui / kmp ドメインは「まだ概念なし」

**(b) 方針だけが ADR で proposed 状態**
`kasane/decisions/core/0007-layout-spec-not-shared-code.md` (status: proposed) が phase-5-1 の直接の前提。「属性の取捨は phase-4/5 の仕様化作業で確定する」と明記された申し送りあり。

**(c) 実装コードにレイアウト属性は入っていない (強い示唆)**
`ios/ android/ maui/ kmp/` 配下を `ProportionalWidth|OverlayColor|CornerRadius` 等で grep してもヒットは `maui/KsDialogs.Maui/obj/` のビルド生成物のみ。phase-4 の縦串 Dialog はレイアウト属性を公開しておらず、サイズ・位置は各 OS の既定 (もしくはハードコード) と考えられる。ただし「どこでどうハードコードしているか」の実ファイル特定は未確認。

**(d) 既存の調査成果物**
`phases/phase-1-architecture-research/artifacts/scout-origin-api-surface.md:55-70` に ExtraView 属性表 (簡易版) と「Measure 3箇所散在 = 主要負債」の所見あり。本調査と矛盾なし (本報告は意味・iOS/Android 差・DialogMargin の穴を追加したもの)。

### 未確認 (別途確認が必要)

- phase-4 縦串 Dialog がサイズ・位置を具体的にどう決めているかの該当ファイル・行
- phase-4 の「レイアウト共通仕様テストの器の初版」の有無・場所・構造
