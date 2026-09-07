---
id: 0032
title: Toast の既定 View は OS 慣習寄せのピルとし、既定配置は下部中央 + ボトムバー回避オフセット、styling とアプリ既定配置は ToastStyle で受ける
status: accepted
date: 2026-08-27
---

## Context

Toast は既定 View を持つと決めた (ADR-0028)。その見た目・既定配置・styling の受け口を決める必要がある。

- Loading の既定コンテンツ (ADR-0023) は背後に覆い (overlay) がある前提で透明背景だが、Toast は覆いを持たない (非モーダル — ADR-0030・0031) ため、既定 View が自分で背景を描く必要がある
- 配置の座標系はページではなくスクリーン (Window) 基準で、既定の配置基準 visibleArea (window − insets) はホームインジケータや OS ナビゲーションバーは避けるが、**アプリのタブバー / ボトムナビは visibleArea の内側にあるアプリ側 UI のため避けられない**。器はページ構造を知らない設計 (ADR-0030) であり、タブバーの存在も高さもライブラリからは原理的に分からない

## Decision

- **見た目**: 半透明ダークグレー背景 + 白文字の角丸ピル。テキストは中央寄せ・複数行折り返し可。Android の OS Toast の見え方に寄せた中立デザインとし、ブランド色は出さない
- **既定配置**: visibleArea 基準の下部中央に、標準的なボトムバー1本ぶんを避ける上方向オフセットを既定値として持つ (具体値は論理単位 **80** の単一値 — iOS タブバー 49pt + 余白 / Material ボトムナビ 80dp をカバーする値としてレイアウト共通ケース表 `core/layout-spec/cases.json` に固定。OS 別の値にはしない — レイアウト共通仕様 ADR-0007・0009 は OS 差を挙動に持ち込まない方針)。既定配置が Dialog の既定 (中央) と異なるのは意図的乖離として明示する (ADR-0008 の線)。呼び出しごとの DialogPlacement 上書きは従来どおり可能
- **styling の受け口**: ToastStyle 値オブジェクト1つに一括設定する (背景色・文字色・フォントサイズ・角丸半径・既定 duration)。規律は ADR-0023 と同型 — show 引数にはしない・各表示の開始時に読む (変更は次の表示から)・色を含むため KMP commonMain からは設定できず各 OS 側で設定する
- **アプリ既定の配置**: ToastStyle に「アプリ既定の配置」を持たせる。アプリは一度、自分のクローム (タブバー高さ等) に合わせた既定を設定すれば以後の全 Toast に効く。LoadingStyle にない Toast 固有の項目だが、「画面クロームと共存する常時最前面 UI」という Toast だけの性質が根拠

## Alternatives Considered

- **タブバー等の自動検知・回避** — 却下: 器はページ構造を知らない設計 (ADR-0030) に反し、UIKit / Compose / MAUI / SwiftUI 各 UI 系のボトムバー検知を3形態で網羅するのは脆すぎる。検知漏れ時にアプリ側の逃げ道もない
- **既定を中央にして被りを回避** — 却下: 被りは消えるが Toast らしさ (下部が慣習) を失い、Dialog と見分けがつかない
- **Material Snackbar 風の見た目 (全幅・左寄せ)** — 却下: Snackbar は対話コンポーネントの見た目であり、完全非対話 (ADR-0031) とちぐはぐになる
- **styling 受け口なし (見た目固定)** — 却下: 変えたければカスタム View 一択になり、LoadingStyle (ADR-0023) と規律が非対称になる

## Consequences

- 正: 標準的なタブバー構成のアプリなら既定値のままで被らず、特殊なクロームのアプリも ToastStyle の一括設定で対処できる
- 正: styling の規律が Loading と同型で、実装型紙 (各表示開始時に読む・OS 側設定) を再利用できる
- 負: 既定オフセット値はヒューリスティックであり、すべてのアプリ構成で被りを保証するものではない (逃げ道は ToastStyle / DialogPlacement)
- 負: 既定見た目・既定配置値が公開 API 表面になり、変更に互換性の配慮が必要 (ADR-0028 の懸念と同じ性質)
- 実装で確定した帰結 (出典: 実装結果): ToastStyle の項目の適用範囲は2種に分かれる — 視覚項目 (背景色・文字色・フォントサイズ・角丸) はデフォルト View にのみ効き、既定値項目 (既定 duration・アプリ既定配置) はカスタム View を含むすべての Toast に効く (該当引数・添付の省略時の既定として)

出典: kasane/roadmaps/library-foundation/phases/phase-8-toast-rebuild/history.md (2026-08-27 デフォルト View の見た目と styling の受け口) / kasane/changes/archive/2026-08-28-add-toast/design.md (Decision 6) / kasane/changes/archive/2026-08-28-add-toast/specs/dialog-contract/spec.md (配置属性と ToastStyle)
