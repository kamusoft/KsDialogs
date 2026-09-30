---
from: KsDialogs
to: ColorAnalyzer
kind: change
date: 2026-09-27
source: kasane/changes/archive/2026-09-27-add-page-layout-area/
---

# 基準領域に「表示中のページ」(CurrentPage) を追加した (知らせ 2026-09-25-page-location-layout-area への返事)

## 何を変えたか

知らせ `../ColorAnalyzer/kasane/outbox/KsDialogs/2026-09-25-page-location-layout-area.md` (両軸 End のメニューがタブを持つ画面でタブバーに重なる) を受けて、基準領域 `DialogLayoutArea` に 3 つ目の値 `CurrentPage` を足した (change add-page-layout-area、決定は `kasane/decisions/core/0038-current-page-layout-area-via-registered-provider.md`)。

- **希望 1 (基準領域に表示中のページ)**: `DialogLayoutArea.CurrentPage` として入った。基準は表示中のページのうちナビゲーションバー・タブバーとシステムバーを除いた内側で、**水平・垂直の両軸**に効く (原典の `UseCurrentPageLocation=true` は垂直だけだった)
- **MAUI での書き方**: 中身の View に `Dialog.SetLayoutArea(view, DialogLayoutArea.CurrentPage)` (XAML なら `ksd:Dialog.LayoutArea="CurrentPage"`) を添付するだけ。MAUI 層がダイアログを出すウィンドウのページ木を辿る (モーダルがあればいちばん上、無ければ `Window.Page` から、Shell / FlyoutPage / TabbedPage / NavigationPage を降りる) ので、**標準のページ構成なら何も登録せずに**タブバー・ナビゲーションバーの内側が基準になる。原典が扱えなかった Shell とモーダルも辿る
- 独自の切り替えでページを組んでいる画面だけ、`DialogCurrentPage.Provider` (`Func<VisualElement?>`) に基準にしたい要素を返す関数を登録すれば、既定の辿り方より優先される
- ページが得られないとき (描画前など) は `VisibleArea` と同じ結果になり、表示は失敗しない
- 両 OS の実配置 (TabbedPage + NavigationPage・Shell・モーダル・FlyoutPage・素の ContentPage) をテストホストで確かめた
- **希望 2 (移行スキルの対応表)**: KsDialogs の利用者向けスキルの更新 (docs-refresh) で、`UseCurrentPageLocation` の `true` → `CurrentPage` (`false` → `Window`) に直す。暫定の注記は入れない

## 相手に関係する理由

ColorAnalyzer から受けた知らせへの返事。ColorAnalyzer は KsDialogs.Maui の利用側で、移行時に基準領域の差 (原典はページ領域、KsDialogs の既定は可視領域) でメニューの位置がずれていた。

## 提案する対応

取り込めるのは、この change を含む KsDialogs のリリース以降 (2026-09-27 時点では未公開)。リリース後に、タブを持つ画面のメニューのダイアログに `LayoutArea = CurrentPage` を指定し、offset などの回避策を入れていれば外せるか確かめる。原典で `UseCurrentPageLocation` を既定 (false) のまま使っていた呼び出しは、KsDialogs では基準領域を指定しないと可視領域で出る (原典と同じ見えにするなら `Window`)。採るかどうか・時期は ColorAnalyzer 側の判断に任せる。
