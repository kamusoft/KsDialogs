---
id: 0038
title: 基準領域に「表示中のページ」を足し、器はページを自分で探さずアプリが登録した現在ページ provider から矩形を得る
status: accepted
date: 2026-09-25
amends: 0030, 0032
---

## Context

ColorAnalyzer の移行 (AiForms.Maui.Dialogs → KsDialogs.Maui) で、右下に出すメニュー (両軸 End) がタブを持つ画面でタブバーに重なった (知らせ: `../ColorAnalyzer/kasane/outbox/KsDialogs/2026-09-25-page-location-layout-area.md`)。ずれはタブバーの高さそのもの (iOS 約 49pt / Android 約 56dp)。

- 原典の `UseCurrentPageLocation=true` は**表示中ページの PlatformView の矩形** (ナビバー・タブバーの内側) を基準にしていた。効くのは垂直だけで、ページの探索は MainPage から Flyout / Tabbed / Navigation を辿るだけ (Shell・モーダルは未対応)
- KsDialogs の `layoutArea` は `window` / `visibleArea` の 2 値で、`visibleArea` = window − insets (システムバー) はアプリのタブバーを含む。core/ADR-0008 はこの基準を「システムバーを除いた可視領域」と定めたが、それが原典の基準 (ページ領域) からの再定義であることを意図的乖離として明記していない
- core/ADR-0030・0032 は「器はページ構造を知らない」設計を前提に、Toast のタブバー自動検知・回避を却下し、アプリが既定配置を合わせる方針にした。Dialog にその方針を当てると、利用側がページごとにタブバーの高さを測ってずらすことになり、ColorAnalyzer のオーナーはそれを採らない判断をした
- KsDialogs の基準 rect の計算は 4 形態とも「窓の bounds + 4 辺の insets → R」の OS 非依存の純関数で、3 つ目の基準も「窓から見た inset」に変換して渡せば計算部は変わらない。足りないのは、器が基準になる矩形を得る経路だけ
- 呼び出しは KMP commonMain や ViewModel などの共有層から行うのが基本なので、show ごとに基準 View を渡す形は成立しない
- Android には iOS の view controller 階層にあたる OS 標準の画面遷移機構がない。Jetpack Navigation はライブラリで、Compose の NavHost / Scaffold は View を持たず、ライブラリ側から「ページ」の矩形を取る手段がない

前提: ダイアログの呼び出しは View を持たない共有層から行われる。基準 rect の計算は窓からの 4 辺の inset を入力とする純関数のまま保たれる。

## Decision

基準領域 `layoutArea` に 3 つ目の値「表示中のページ (currentPage)」を足す。基準は表示中ページの矩形のうち、そのページ自身のバー (ナビゲーションバー・タブバー) とシステムバーを除いた内側で、水平・垂直の両軸に効かせる (原典の垂直のみは ADR-0008 の線で正す)。器はこの矩形を窓からの inset に変換し、既存の rect 決定手順にそのまま流す。

器はページ構造を自分で探索しない。アプリ (ホスト) が一度登録した「現在ページ provider」に表示のたびに問い合わせて矩形を教えてもらう。利用者向けの登録口は各 UI 技術の慣用に合わせ、従来 View 系は「ページの View を返す関数」、宣言的 UI 系は「付けた View を現在ページとして名乗らせる modifier」とする。窓座標の矩形そのものを返す口は利用者に開かない (座標系の変換を利用者に負わせないため)。

ページの階層を確実に辿れる形態は既定の provider を内蔵し、登録なしで効くようにする。iOS Native は UIKit のコンテナ階層、MAUI は MAUI 層のページ木 (両 OS で同じ辿り方) を辿る。Android Native は「ページ」が OS の概念に無いため既定を持たず、登録制とする。どの形態でもアプリは provider を上書きできる。

provider から矩形が得られないとき (未登録・未描画・候補なし) は `visibleArea` と同じ結果にし、表示は失敗させない。

ADR-0030・0032 の「器はページ構造を知らない」は、Dialog の基準領域に限って例外とする (0030・0032 の amends)。器がページを見つけるのではなく教えてもらう形なので、Toast のタブバー自動検知の却下 (ADR-0032) はそのまま据え置く。

## Alternatives Considered

- **呼び出し側が show ごとに基準 View / 矩形を渡す** — 却下: 共有層 (KMP commonMain / VM) から呼ぶのが基本で、そこに View は無く渡しようがない
- **器が各形態で「表示中のページ」を自力で見つける (provider なし)** — 部分採用: iOS / MAUI は階層を確実に辿れるので既定 provider として内蔵する。Android Native は「ページ」が OS の概念に無く (Compose では View すら無い) 定義できないため、自力探索は採らない
- **Android Native にも既定 provider を持たせる (NavHostFragment があればその view、なければ Activity の content 領域)** — 却下: Compose 主体のアプリでは当たらず、Activity の content はボトムナビを含むため、既定の名に反して外れることが多い
- **全形態で登録必須にして既定を持たない** — 却下: 挙動は揃うが、確実に辿れる iOS / MAUI の利用者にも登録を強いる
- **足さない (アプリが計測して margin / offset で寄せる)** — 却下: ページごとの計測を利用側に負わせることになり、移行先として原典と同じ位置にならない (ColorAnalyzer のオーナーが採らない判断)

## Consequences

- 正: `UseCurrentPageLocation=true` の移行先ができ、MAUI では原典が扱えなかった Shell・モーダルも辿れる
- 正: 基準 rect の計算部は変えず、入力 (inset) の供給元が増えるだけ
- 正: 共有層からは `layoutArea = currentPage` と書くだけで、ホストの画面遷移の知識は provider に閉じる
- 負: 公開 enum の値と登録口が全形態に増え、MAUI では C# の enum から binding・bridge・写像まで波及する。KMP は commonMain に layoutArea が無く、持たせるなら別途の設計が要る
- 負: 共通ケース表は窓とシステムの insets しか表せなかったため、任意のページ矩形を入力に持つスキーマ拡張が要る
- 負: Android Native は登録しなければ可視領域と同じ結果になり、「currentPage を指定したのに効かない」状態が起こり得る (診断ログとドキュメントで知らせる)
- 負: provider が返す View と器のウィンドウは別物になり得るため、どの View を候補として受け付けるか (提示先との同一性・座標の原点) の規則を形態ごとに持つ必要がある

## Revisit When

- KMP commonMain に layoutArea を持たせる設計が決まったとき (provider の登録面をどこに置くか)
- Android に OS またはデファクトの「表示中ページ」機構が定着し、既定 provider を安全に内蔵できるようになったとき
- iOS / MAUI の既定 provider が SwiftUI や Shell の内部構造の変化で外れる事例が出たとき

出典: kasane/changes/archive/2026-09-27-add-page-layout-area/exploration.md (2026-09-25 の探索: 論点 A の議論、案 1〜3 の比較、Android の Navigation 機構の整理、既定 provider の 3 案) / second-opinion-spec-001.md (2026-09-26 の相方スペックレビュー: 器の通り抜け・SwiftUI の経路・優先順位・ウィンドウ同一性) / deviation.md (実装時の判断: Android の同じ Activity のウィンドウの解釈・台帳の非表示除外)
関連: core/ADR-0008 (Decision 3 の基準領域「可視領域」は原典のページ領域からの再定義であり、本 ADR がページ領域を別の値として復活させた)
