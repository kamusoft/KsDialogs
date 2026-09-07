---
scope: code-review
kind: pain
severity: normal
count: 1
first-seen: 2026-08-22
last-seen: 2026-08-22
evidence:
  - add-presentation-behavior (iOS の器が design Decision 10「attached 中は中身を非表示にし、presenting で表示とフック開始を同時に行う (最終位置が一瞬見えるちらつきを構造的に防ぐ)」を、既存テストの hitTest 都合で「alpha を同期復帰 → フックは次の MainActor ホップで開始」に緩めて deviation に暫定記録。review-001 / 002 / 003 (ホスト) は指摘せず、相方は「やや脆弱」止まり。オーナーが実機で確認すると 100% ちらつきが再現し、修正差し戻しになった)
---

## ルール文

deviation.md の暫定項目や実装メモに「design が『構造的に防ぐ』『必ず』と明記した性質を弱める変更」が含まれていたら、レビューはそれを合意済み差分として素通りさせず、その性質が守られなくなる具体的な条件 (何フレーム・どの操作で観察できるか) を書き出し、観察できるなら Major として指摘する。「理屈上は起こり得るが観測されていない」は、観測手段 (連写 0.27 秒/コマ) が 1 フレーム級の事象を捕まえられない場合、安全の根拠にならない。

## 経緯

- 2026-08-22 add-presentation-behavior: 暫定 deviation は「テストを壊さないための妥協」で、design の狙い (ちらつき防止) を直接崩していた。レビュー 3 周とも「契約 (PB-TR-01: フック開始時にホスト View はウィンドウ上) は満たす」という字義で通し、design の意図 (見た目) に照らさなかった
