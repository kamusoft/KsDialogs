# Exploration: add-page-layout-area

## 課題 / 動機

ColorAnalyzer からの知らせ (`../ColorAnalyzer/kasane/outbox/KsDialogs/2026-09-25-page-location-layout-area.md`、kind: change) を起点とする。

ColorAnalyzer は AiForms.Maui.Dialogs から KsDialogs.Maui へ移行した。移行後、右下に出すメニュー (水平・垂直とも End 配置。AiForms では `UseCurrentPageLocation=True`) が、タブを持つ画面でタブバーに重なるようになった。

- ずれの大きさ (ColorAnalyzer の実機計測): iOS (iPhone 11) で約 49pt、Android (Pixel 6a) で約 56dp。どちらもタブバーの高さに一致する
- AiForms の `UseCurrentPageLocation=True` は、表示中のページの矩形を基準にしていた (知らせによる)。この矩形はタブバー・ナビゲーションバーの内側で終わる
  - iOS: `DialogHelpers.GetCurrentPageRect` がアクティブページの PlatformView の Bounds を使う
  - Android: `ReusableDialog.Android.cs` がページ位置から余白を計算する
- KsDialogs の `DialogLayoutArea.VisibleArea` は「ウィンドウから insets (システムバー等) を除いた領域」で、タブバーを含む (`kasane/concepts/core/api/layout-semantics.md`)
- 移行スキル `ksdialogs-aiforms-migration` の `references/api-mapping.md` は、`UseCurrentPageLocation=true` の写し先を `VisibleArea` としている。このため、タブバーやナビゲーションバーを持つ画面では同じ位置にならない

ColorAnalyzer のオーナーの希望は次の 2 つ。

- 基準領域に「表示中のページの矩形」を選べるようにしてほしい (MAUI 面なら `DialogLayoutArea` に 3 つ目の値を足す等)
- 移行スキルの対応表の写し先を、足した値に直してほしい。足すまでの間は「`VisibleArea` とは位置が一致しない」旨の注記を入れてほしい

ColorAnalyzer は、足されるまで `VisibleArea` のまま (タブバーに重なる状態で) 置き、足された版を取り込むときに切り替える。

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: なし / 未起票: ...)

## 未決の論点

**未探索 (簡易起票)**。起票時 (2026-09-25) に分かっている疑問点は次のとおり。

- **core/ADR-0008 の前提要確認**
  - 同 ADR は `UseCurrentPageLocation` を enum `LayoutArea { window, visibleArea }` に作り直した
  - 列挙にした理由の 1 つに「将来、3 つ目の基準を今の指定を壊さずに足せるようにする」がある。3 つ目の値を足すことは想定の範囲内
  - ただし同 ADR は `UseCurrentPageLocation` を「システムバーを除いた可視領域」として写しており、知らせの実測 (AiForms はページの矩形を使っていた) と前提が食い違う。原典の実装を確かめ、ADR-0008 を改訂するか判断する
- **core/ADR-0030・0032 との衝突**
  - 両 ADR は「器はページ構造を知らない設計」を前提とする。Toast ではこれを理由に、タブバーの自動検知・回避を却下した (UIKit / Compose / MAUI / SwiftUI の全部でボトムバーを見つけ出すのは壊れやすい)
  - ページの矩形を基準にするには、ページ構造を知る必要がある。この原則を Dialog にも当てはめるか、Dialog だけ例外にするかを決める
- **どの形態に持たせるか**
  - MAUI はページを持つ。iOS Native (UIKit / SwiftUI)、Android Native (従来 View / Compose)、KMP の共有コードで「表示中のページ」にあたるものが何かは決まっていない
  - MAUI 限定の上乗せにするか、全形態の契約にするか
- **移行スキルの対応表**
  - `UseCurrentPageLocation=true` → `VisibleArea` の写しは、位置が一致しない
  - 値を足すまでの間、注記を入れるか。`skills/` の更新は docs-refresh 経由
- `fix-layout-contract-gaps` (layout 契約の既存の穴 3 件) とは性質が違うため別の change とした。ただし同じ `layout-semantics.md` に触れるので、着手順は調整が要る

## UI 素材 (ui/references/ の一覧と注釈)

## 変更級の推奨: 未判定

暫定: 公開 API に値を足すこと、ADR-0008・0030・0032 との整理が要ること、複数の形態に関わることから、M 以上の見込み。
