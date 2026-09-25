---
id: 0038
title: 基準領域に「表示中のページ」を足し、器は登録された現在ページ provider から矩形を得る (iOS / MAUI は既定 provider 内蔵、Android Native は登録制)
status: proposed
date: 2026-09-25
---

## Context

ColorAnalyzer の移行 (AiForms.Maui.Dialogs → KsDialogs.Maui) で、右下に出すメニュー (両軸 End) がタブを持つ画面でタブバーに重なった (知らせ: `../ColorAnalyzer/kasane/outbox/KsDialogs/2026-09-25-page-location-layout-area.md`)。ずれはタブバーの高さそのもの (iOS 約 49pt / Android 約 56dp)。

- 原典の `UseCurrentPageLocation=true` は**表示中ページの PlatformView の矩形** (ナビバー・タブバーの内側) を基準にしていた。効くのは垂直だけで、ページの探索は MainPage から Flyout / Tabbed / Navigation を辿るだけ (Shell・モーダルは未対応)
- KsDialogs の `layoutArea` は `window` / `visibleArea` の 2 値で、`visibleArea` = window − insets (システムバー) はアプリのタブバーを含む。core/ADR-0008 はこの基準を「システムバーを除いた可視領域」と定めたが、それが原典の基準 (ページ領域) からの再定義であることを意図的乖離として明記していない
- core/ADR-0030・0032 は「器はページ構造を知らない」設計を前提に、Toast のタブバー自動検知・回避を却下し、アプリが既定配置を合わせる方針にした。Dialog にその方針を当てると、利用側がページごとにタブバーの高さを測ってずらすことになり、ColorAnalyzer のオーナーはそれを採らない判断をした
- KsDialogs の基準 rect の計算は 4 形態とも「窓の bounds + 4 辺の insets → R」の OS 非依存の純関数で、3 つ目の基準も「窓から見た inset」に変換して渡せば計算部は変わらない。足りないのは、器が基準になる矩形を得る経路だけ
- 呼び出しは KMP commonMain や ViewModel などの共有層から行うのが基本なので、show ごとに基準 View を渡す形は成立しない
- Android には iOS の view controller 階層にあたる OS 標準の画面遷移機構がない。Jetpack Navigation はライブラリで、Compose の NavHost / Scaffold は View を持たず、ライブラリ側から「ページ」の矩形を取る手段がない

## Decision

1. **基準領域 `layoutArea` に 3 つ目の値「表示中のページ (currentPage)」を足す**。基準になる矩形は「表示中ページの矩形のうち、そのページ自身の safe area / システム insets の内側」(UIKit 標準ではページ view がバーの下まで伸びるため、view の矩形ではなく safe area を採る。Android はページの矩形 ∩ 可視領域)。器はこれを窓座標の 4 辺 inset に変換し、既存の rect 決定手順 (基準 rect R → 有効領域 A) にそのまま流す。水平・垂直の両軸に効かせる (原典の垂直のみは ADR-0008 の線で正す)
2. **器はページ構造を自分で探索せず、登録された「現在ページ provider」に問い合わせる**。provider はアプリ (ホスト) が起動時に一度登録し、以後の全表示に効く (`ToastStyle.defaultPlacement` / Loading のアプリ既定 options と同じ「一度設定して各表示の開始時に読む」規律)。利用者向けの登録口は形態ごとに 1 つ — UIKit / Android View は「ページの View を返す provider」、Compose / SwiftUI は「付けた composable / View を現在ページとして名乗らせる modifier」(attach 中のものを台帳に持ち、窓外は除外・入れ子は内側・それ以外は最後に配置されたものが勝ち・detach で残りへ戻る)。取得元の優先順位は modifier の台帳 > 登録 provider > 既定 provider で、上位が空なら下位へ進む。候補は提示先と同じ window / Activity に属するものに限る。「窓座標の矩形を返す provider」は内部の共通型で公開しない
3. **既定 provider**: iOS Native と MAUI は既定を内蔵する — iOS = 提示先 window の view controller 階層を presented → navigation の top → tab の selected と先端まで走査した VC の view (KsDialogs 自身の器は通り抜けて提示元へ戻る。保証は UIKit コンテナまでで、SwiftUI の TabView / NavigationStack は modifier が正規の経路)、MAUI = MAUI 層でページ木を辿る (ModalStack の先頭があればそれ、無ければ `Window.Page` を起点に Shell / FlyoutPage / TabbedPage / NavigationPage を容れ物でなくなるまで降りる。Shell の有無を問わない) 先端ページの PlatformView を Native の provider へ流す。Android Native に既定が無いため MAUI 層で持ち、両 OS で同じ 1 本にする。Android Native は既定を持たない (登録制)。どの形態でもアプリは provider を上書きできる
4. **未解決時は可視領域へ落とす**: provider が未登録、または矩形を返せないときは `visibleArea` と同じ結果にする
5. **ADR-0030・0032 の「器はページ構造を知らない」は Dialog の基準領域に限って例外とする (0030・0032 の amends)**。器がページを見つけるのではなく、教えてもらう (provider) 形なので、Toast のタブバー自動検知の却下 (ADR-0032) はそのまま据え置く

## Alternatives Considered

- **呼び出し側が show ごとに基準 View / 矩形を渡す** — 却下: 共有層 (KMP commonMain / VM) から呼ぶのが基本で、そこに View は無く渡しようがない
- **器が各形態で「表示中のページ」を自力で見つける (provider なし)** — 部分採用: iOS / MAUI は階層を確実に辿れるので既定 provider として内蔵する。Android Native は「ページ」が OS の概念に無く (Compose では View すら無い) 定義できないため、自力探索は採らない
- **Android Native にも既定 provider を持たせる (NavHostFragment があればその view、なければ Activity の content 領域)** — 却下: Compose 主体のアプリでは当たらず、Activity の content はボトムナビを含むため、既定の名に反して外れることが多い
- **全形態で登録必須にして既定を持たない** — 却下: 挙動は揃うが、確実に辿れる iOS / MAUI の利用者にも登録を強いる
- **足さない (アプリが計測して margin / offset で寄せる)** — 却下: ページごとの計測を利用側に負わせることになり、移行先として原典と同じ位置にならない (ColorAnalyzer のオーナーが採らない判断)

## Consequences

- 正: `UseCurrentPageLocation=true` の移行先ができ、MAUI では原典が扱えなかった Shell・モーダルも正しく辿れる
- 正: 基準 rect の計算部は変えず、入力 (inset) の供給元が増えるだけ
- 正: 共有層からは `layoutArea = currentPage` と書くだけで、ホストの Navigation 機構の知識は provider に閉じる
- 負: 公開 enum の値と登録口が増え、MAUI では C# の enum・iOS binding・bridge 2 つ・写像の最低 5 箇所に波及する。KMP は commonMain に layoutArea が無く、持たせるなら別途の設計が要る
- 負: 共通ケース表 `core/layout-spec/cases.json` は任意の矩形を表せず、スキーマ拡張が要る
- 負: Android Native は登録しなければ可視領域と同じで、「currentPage を指定したのに効かない」状態が起こり得る (ドキュメントでの明記が要る)
- ADR-0008 は「visibleArea = システムバー除外領域」が原典の基準 (ページ領域) からの再定義であることを乖離として書いていない。本 ADR がその再定義と、ページ領域を別の値として復活させたことを記録する (ADR-0008 の Decision 3 への注記)

## Revisit When

- KMP commonMain に layoutArea を持たせる設計が決まったとき (provider の登録面をどこに置くか)
- Android に OS またはデファクトの「表示中ページ」機構が定着し、既定 provider を安全に内蔵できるようになったとき
- iOS / MAUI の既定 provider が SwiftUI や Shell の内部構造の変化で外れる事例が出たとき

出典: kasane/changes/add-page-layout-area/exploration.md (2026-09-25 の探索: 論点 A の議論、案 1〜3 の比較、Android の Navigation 機構の整理、既定 provider の 3 案) / second-opinion-spec-001.md (2026-09-26 の相方スペックレビュー: 器の通り抜け・SwiftUI の経路・優先順位・ウィンドウ同一性)
