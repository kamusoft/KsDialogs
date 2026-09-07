---
scope: spec-review
kind: pain
severity: normal
count: 1
first-seen: 2026-08-21
last-seen: 2026-08-21
evidence:
  - add-presentation-behavior (design Decision 3 の公開 API 表は `DialogTransition.overlayDuration` を3形態に置き、Decision 2 で「覆いの時間はコンテンツのトランジションに揃える」を契約にしたが、Decision 8 の MAUI ブリッジ実行口は `runPresentation(view, completion)` / `runDismissal(view, completion)` だけを列挙し overlayDuration を運ぶ口が無かった。相方スペックレビュー4ラウンド通過後、MAUI 実装フェーズで「値は持つが効かない」状態として発覚し、両 Native 互換面 + binding + C# アダプタの追加修正が発生)
---

## ルール文

ブリッジ (ObjC / Java 互換面・binding) を経由する形態を持つ公開 API を design で確定するとき、公開 API 表の各メンバ (値・フック・列挙) に対して「ブリッジのどの口で Native へ運ぶか」を1対1で書いた対応表を Decision に含める。対応表に現れないメンバがあれば、それは (a) ブリッジの口を追加する、(b) その形態では無効と明記して Scenario に反映する、のどちらかに倒してから spec を確定する。

## 経緯

- 2026-08-21 add-presentation-behavior: ブリッジの Decision は「フックの完了をどう待つか」(機構) に集中し、公開 API 表との突き合わせ (何を運ぶか) を行っていなかった。機構の設計と運搬物の列挙は別の検査で、後者は表の突き合わせで機械的に済む
