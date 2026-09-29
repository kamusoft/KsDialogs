---
scope: code-review
kind: pain
severity: normal
count: 1
first-seen: 2026-09-28
last-seen: 2026-09-28
evidence:
  - wait-for-host-appearance (ホストの review-001 が APPROVED を出した後、相方 second-opinion-code-001 が Major で検出: iOS の提示面が提示先を取り直せない分岐は完了通知だけを呼び、器が提示されないまま show の結果待ちが残る。修正後の再レビューでも相方 second-opinion-code-002 が、UIKit の遷移の完了を見張る登録 `animate(alongsideTransition:completion:)` の戻り値を見ていない分岐で同じく結果待ちと列の番が残ることを Major で検出。修正サイクルが 2 周発生)
---

## ルール文

呼び出し元を待たせる仕組み (結果待ち・待ちの列の番・完了通知) を足した変更をレビューするときは、提示や登録の各分岐 (OS の API が false / nil を返す場合、再取得に失敗する場合、完了通知が呼ばれない場合を含む) ごとに、呼び出し元の結果待ちと列の番が必ず解放されるかを実装で追い、分岐ごとのテストが無ければ指摘する。守れたかは、レビュー結果に分岐ごとの解放の確認が書かれていることから判定する。

## 経緯

- 2026-09-28 wait-for-host-appearance: ホスト側のレビューは、正常な提示の経路と打ち切りの経路の解放は確かめたが、OS の API が提示や登録を受け付けなかった分岐まで視線が届かなかった。同じ change の中の同じ型なので count は 1
