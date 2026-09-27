---
scope: spec-review
kind: pain
severity: normal
count: 1
first-seen: 2026-09-27
last-seen: 2026-09-27
evidence:
  - fix-android-container-system-bar-appearance (明暗の読み取りに「OS の返す値」と「旧来のフラグ」を合わせて読む設計を proposal / spec に書いたが、両者が食い違う提示先 (テーマの明るい地向け + WindowInsetsController での暗い地向けの明示、旧来のフラグの代入で明るいビットを外した画面) を列挙していなかった。review-001 の Major (OR 読みが退行を入れる) と review-002 の Minor (API 35 以上で代入を取りこぼす) として実装後に見つかり、オーナー判断 2 回と修正サイクル 2 周を要した)
---

## ルール文

1 つの値を複数の情報源 (新旧 2 系統の API・OS の返す値とアプリの設定・テーマとコードでの指定など) を合わせて読む設計を proposal / spec に書くときは、情報源どうしが食い違う組み合わせを表にし、組み合わせごとに「実際の見え (正)」と「設計の読み取り結果」を 1 行ずつ書く。両者が一致しない行は、Scenario にするか、受け入れる取りこぼしとして proposal のリスクに書くかを決めてから spec を確定する。守れたかは、proposal か spec に食い違いの表があり、各行の扱いが決まっていることから判定する。

## 経緯

- 2026-09-27 fix-android-container-system-bar-appearance: proposal のリスク欄は「旧来のフラグ由来の明暗が API 35 以降も OS から返らないこと (未実測)」だけを挙げ、足し合わせが誤る組み合わせを検討していなかった。どの組み合わせも SDK の sources から机上で導けるもので、提案時に表を作れば実装前に扱いを決められた
