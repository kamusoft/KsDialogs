---
scope: process
kind: pain
severity: normal
count: 1
first-seen: 2026-09-29
last-seen: 2026-09-29
evidence:
  - wait-for-host-appearance (完了報告で「直した後は 4 形態 × 5 デモのすべての起動で表示された」と書いたが、MAUI Android の実測は Sample 側の回避 (`Dispatch` で受理を最初の描画の後まで遅らせる待ち) が残った状態で、Android と KMP の Android の Sample は測っていなかった。続く align-sample-autoplay-start で Android 系の待ちを外すと起動直後の Toast が出ない回が多くなり、オーナーから「それ wait-for-host-appearance で直したんじゃなかったっけ？」と問われた。実際には Android の Toast はスコープ外で、表示時間は受理の時点から数える契約のままだった)
---

## ルール文

ライブラリの修正を「直した後の実測」で裏付けて完了報告に書くときは、実測した経路のうち、Sample や呼び出し側に回避 (待ち・遅延・再試行) が残っていたものと、測っていない形態を分けて書き、「直った」と言う範囲をそれ以外に限る。回避が残った経路は「回避ありで表示された」と書き、ライブラリの修正の裏付けに数えない。守れたかは、完了報告の実測の要約に、回避の有無と測っていない形態が書かれていることから判定する。

## 経緯

- 2026-09-29 wait-for-host-appearance / align-sample-autoplay-start: iOS 系の Sample からは回避を外して測ったが、MAUI の Sample は Android 側の回避を持ったままで、その違いを報告に書かなかった。関連: process L-002 (互換の主張の範囲を実証した範囲に限る。こちらは修正の効果の主張)
