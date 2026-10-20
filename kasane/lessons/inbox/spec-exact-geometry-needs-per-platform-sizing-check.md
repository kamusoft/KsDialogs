---
scope: spec-review
kind: pain
severity: normal
count: 2
first-seen: 2026-09-27
last-seen: 2026-10-20
evidence:
  - revisit-dialog-margin-default (Scenario TS-AT-05 の THEN「デフォルト View の左右の端は可視領域の端からそれぞれちょうど 24 内側」が、Android の TextView (複数行で使える幅いっぱいに広がる) の振る舞いを前提にしていた。iOS のデフォルト View は折り返した最長行の幅まで縮んで中央に寄るため、端は 24 + 1 字幅未満の余りの半分に来る。実装ワーカーが実測 (期待 124 / 実測 127) で検出し、オーナー判断で iOS は現状の見た目を保ち「24 以上・余りは 1 字幅の半分未満」と読み替える deviation にした)
  - deliver-swiftui-content-safe-area-ios (ksn-autopilot ios-safe-area-and-kmp-int-result の観測。Scenario PB-IS-15 の THEN「幅は窓の幅 852 で頭打ちになる」は、横の安全領域で折り返す本文の器の幅を実測せずに書いた値だった。実装ワーカーが実測 733.33 で停止し、同じ Requirement の他の条件に合わせる deviation で判定した)
---

## ルール文

デルタスペックの Scenario の THEN が、View の端・幅・位置を「ちょうど N」「一致する」のような厳密な幾何で書いていて、その View の大きさが中身 (文字の折り返し・画像) で決まるなら、提案の確定前に各 Native (対象の Native が 1 つでも) で、その値を実装で実測し、その View が余った幅をどう扱うか (使える幅いっぱいに広がるか、中身の幅に縮むか) を確かめる。振る舞いが Native ごとに違えば、THEN を両方で成り立つ形 (「N より外へ出ない」「余りは 1 字幅未満」など) に書くか、どちらかにそろえる作業を tasks に入れる。守れたかは、実装・verify で Native ごとの許容差の読み替え (deviation) が出ないことから判定する。

## 経緯

- 2026-09-27 revisit-dialog-margin-default: 余白の添付漏れを見分けるための Scenario (TS-AT-05) で、Android の実装の振る舞いを暗黙の前提に「ちょうど 24」と書いた。iOS の既存の縮み方は proposal の「既定 Toast は変わらない」とも結びついていたため、実装で iOS を合わせることもできず、deviation での読み替えになった
- 2026-10-20 deliver-swiftui-content-safe-area-ios: 単一の Native (iOS) でも同じ型で起きた。中身 (折り返す本文) で大きさが決まる器の幅を、THEN に実測なしの「ちょうど 852」で書き、実装で食い違った。Native が 1 つでも、THEN の幾何は実装の実測で確かめてから確定する
