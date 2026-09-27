---
scope: code-review
timestamp: 2026-09-27
---

# lessons: code-review

## 重点観点

- [L-001] レビュー (コードレビュー・スペックレビュー) で証跡の取り方 (A/B・強制再現) や Scenario の受け入れ条件・検査スクリプトの受け入れ条件を推奨・承認するときは、その手順が「証明したい命題の真偽で結果が分かれるか」を先に確かめて書く — 操作する箇所 (強制する判定) と観測する箇所 (修正が見ている述語) が同じ入力を見ているかを追い、Scenario や検査なら「その機構を丸ごと外しても同じ観測が得られないか」を問う。分かれない手順は推奨せず、機構固有の印 (診断文言・生成物の内容・負の入力で失敗すること) を条件に入れる。守れたかは、推奨した証跡手順・受け入れ条件の直後に「機構が無いと何が変わるか」が書かれていることから判定する。([経緯](details/review-suggested-evidence-lacks-discriminating-power.md)) (昇格: 2026-09-10、出典: fix-kmp-ios-unhandled-exception-crash / add-kmp-maven-distribution / add-consumer-verification)
- [L-002] 実装報告・tasks のチェックが「テスト全件成功」「全ルート実行」を主張していたら、レビューはその主張を `kasane/handbook/cross/test-execution.md` の全件実行表と**行ごとに**突き合わせる — 表の各ビルドルート (JVM とは別に走る instrumented ルート、対象 API レベルごとの端末を含む) の件数・failures・skipped が報告に載っているかを確認し、載っていない行は「未実行」として指摘する。修正サイクルの再実行にも同じ行数で当てる。表の行が 1 つでも欠けた報告を全件成功として承認しない。守れたかは、review の実行表の行数が handbook の全件実行表の行数と一致することから判定する。([経緯](details/full-suite-claim-must-cover-every-handbook-route.md)) (昇格: 2026-09-27、出典: add-loading-toast-typed-show / rename-dialog-contract-singular / fix-android-container-system-bar-appearance)

## 指摘しないこと

(まだなし)
