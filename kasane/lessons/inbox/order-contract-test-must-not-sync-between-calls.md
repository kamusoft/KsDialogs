---
scope: test
kind: pain
severity: normal
count: 1
first-seen: 2026-09-27
last-seen: 2026-09-27
evidence:
  - wait-for-host-appearance (iOS と Android の PB-HW-06「待っている Dialog は呼んだ順に表示される」のテストが、A と B の呼び出しの間に「列に 1 件並ぶまで待つ」を挟んでいたため、iOS の show が大域の実行器を経由して MainActor へ戻る間に順番が入れ替わる穴を検出できず、全件成功のまま進んだ。穴は MAUI の iOS ブリッジのテストの 11 回中 1 回の間欠失敗から見つかった。書き直したテストは修正前の実装で 300 回中 16 回落ちた。実測は kasane/changes/archive/2026-09-29-wait-for-host-appearance/evidence/ios-dialog-sequential-presentation.md)
---

## ルール文

「呼んだ順」「先に呼んだものが先」のように呼び出しの順序を契約にした振る舞いのテストを書くときは、呼び出しの間に同期点 (列に並ぶまでの待ち・結果の観測・`waitUntil`) を挟まず、同じ実行文脈 (UI スレッド) から続けて呼ぶ。そのうえで、呼び出しから戻った直後の待ちの数と、最後に観測される順序の両方を確かめる。守れたかは、テストの本文で 2 つの呼び出しの間に待ちや観測が無いことと、入口に実行文脈の乗り換えを一時的に入れるとテストが落ちること (検出力) の報告から判定する。

## 経緯

- 2026-09-27 wait-for-host-appearance: 同期点を挟んだテストは「列に並んだ後の順序」しか見ておらず、列の入口までの経路 (nonisolated async の関数の行き帰り・Task の開始順) で起きる入れ替わりが原理的に見えなかった。姉妹面の照合で、KMP の iOS 互換面と MAUI の iOS ブリッジにも同じ穴 (Task の開始順に頼る) が見つかった
