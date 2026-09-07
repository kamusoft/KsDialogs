---
scope: process
kind: success
severity: normal
count: 1
first-seen: 2026-09-07
last-seen: 2026-09-07
evidence:
  - add-kmp-typed-show (実装中に main で localize-dialog-error-messages が cross/ADR-0015 (診断文言は英語固定) を accepted にして実装・マージした。add-kmp-typed-show の spec は文言の言語を定めておらず、共有コードが新設した診断文言 2 件は日本語のまま review-003 APPROVED に達していた。オーナー指示で main 取り込み時に 2 件を英語化し、文言 assertion 11 件を追随させ、合流後に kmp allTests 151 / 0 failures と日本語リテラルの静的 grep 0 行を確認、deviation.md に記録して review-004 (合流分の限定レビュー) で APPROVED。ADR-0015 違反を main に持ち込まなかった)
---

## ルール文

change を main に合流させるとき (合流の直前)、着手後に `kasane/decisions/` で accepted になった ADR を `git log` (decisions/ 配下の追加・status 変更) で列挙し、自 change の成果物 (新設した文言・API・テスト・handbook の追記) がその決定に反していないかを 1 件ずつ確かめる。反していれば合流時に追随し、deviation.md に「合流時に ADR-NNNN へ追随」として記録して、合流分だけを対象にした限定レビューを受ける。守れたかは、合流コミットの本文または deviation.md に確認した ADR ID が書かれていることから判定する。

## 経緯

- 2026-09-07 add-kmp-typed-show: 並行して進んだ change が長命層の決定を先に確定させたケース。自 change の spec とレビューは合流前の時点では正しく、合流の瞬間にだけ検査点がある。ADR-0015 は静的 grep (`DM-KM-04`) で機械的に検出できたため、合流後の再実行で違反ゼロを確認できた
