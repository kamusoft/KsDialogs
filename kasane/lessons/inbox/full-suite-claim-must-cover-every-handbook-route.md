---
scope: code-review
kind: pain
severity: normal
count: 2
first-seen: 2026-09-06
last-seen: 2026-09-06
evidence:
  - add-loading-toast-typed-show (レビュー指摘の修正サイクルで、オーケストレーターがワーカーへの指示に「instrumented は接続済み実機 1 台以上で可・API 29 エミュレータの起動は不要」と書き、API 33 / 36 の 2 台だけで修正後の全件成功と判定した。handbook の「対象 API レベルを 1 台ずつ」に反し、API 29 でしか走らない Scenario 1 本が未実行のまま。修正後の独立再確認 (review-002) が handbook の行と突き合わせて検出し、API 29 で追加実行して閉じた。初回の完了ゲート (5.1) は 3 台で回っていたため、欠けたのは修正サイクル側)
  - rename-dialog-contract-singular (実装報告・review-001・verify-001 のいずれも handbook/cross/test-execution.md の全件実行表にある android/ (instrumented) ルート (`connectedDebugAndroidTest`、305 tests) を実行せず、JVM の 67 件だけで「4 ルートのテスト全件成功」と判定した。相方レビュー (codex) の Major が handbook の該当行を引いて検出し、verify-002 で instrumented を API 29 / 36 で実行して既存の環境依存の失敗 2 件を切り分けた)
---

## ルール文

実装報告が「テスト全件成功」「全ルート実行」を主張していたら、レビューはその主張を `kasane/handbook/cross/test-execution.md` の全件実行表と**行ごとに**突き合わせる — 表の各ビルドルート (JVM とは別に走る instrumented ルートを含む) に対応する件数・failures・skipped が報告に載っているかを確認し、載っていないルートがあれば「未実行」として指摘する。表にあるルートの結果が 1 行でも欠けた報告を全件成功として承認しない。守れたかは、review の実行表と handbook の全件実行表の行数が一致することから判定する。

## 経緯

- 2026-09-06 add-loading-toast-typed-show: 初回ゲートは規約どおり 3 台で回したが、修正サイクルの再実行で指揮側が「1 台以上で可」と緩めた。ルートの表は 1 周目だけでなく修正サイクルの再実行にも同じ行数で適用される — 指揮側が再実行の指示を書く時点で表の行を落とさないことも同じルールの範囲

- 2026-09-06 rename-dialog-contract-singular: 実装・レビュー・検証の 3 者が同じ見落とし (`./gradlew test` を Android の全件とみなす) をしており、ホスト側の多層チェックでは検出できなかった。handbook は「`./gradlew test` では instrumented が 1 件も走らない」と明記済みで、規約の側に不足はなく、報告を表と突き合わせる動作が欠けていた
