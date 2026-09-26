---
scope: spec-review
kind: pain
severity: normal
count: 1
first-seen: 2026-09-26
last-seen: 2026-09-26
evidence:
  - add-page-layout-area (samples の Scenario は「ダイアログはナビゲーションバーの下端から dialogMargin 分内側に出る」と書き、Layout Dialog パネルのダイアログは既定の dialogMargin (全辺 24) のまま実装・撮影された。実装・独立レビュー 3 周・verify を通過した後、オーナーが撮影証跡を見て「ぴったりくっつかないのでぱっと見検証しにくい。DialogMargin は 0 指定だと良い」と指摘し、4 ルートの設定変更と証跡 30 枚の撮り直しが発生した)
---

## ルール文

Sample を検証装置として使うデモ項目 (handbook cross/sample-parity.md) の brief / spec を書くとき、そのデモで見せたい判定基準 (基準領域の端・境界・重なりの有無など) が、画面を見ただけで判定できる設定になっているかを確かめる。基準と実際の位置の間に既定値の余白・オフセット・アニメーションなど判定を曖昧にする量が入るなら、デモでは 0 や無効にする設定を brief の「画面と状態」か spec の GIVEN に明記する。守れたかは、承認モックと撮影証跡で、判定の対象 (例: カードの端とバーの端) が接しているか重なっているかが一目で読めることから判定する。

## 経緯

- 2026-09-26 add-page-layout-area: Scenario は「dialogMargin 分内側」と書いて余白の存在を前提にしていたため、仕様どおりの実装・レビュー・verify はどれも通った。問題は仕様の正しさではなく、検証装置としての見やすさで、仕様の段階でしか決められない種類のものだった
