---
scope: code-review
kind: pain
severity: normal
count: 1
first-seen: 2026-09-06
last-seen: 2026-09-06
evidence:
  - add-loading-toast-typed-show (相方 second-opinion-code-001 の Major「公開 contract (KsLoading / KsToast / IKsLoading / IKsToast) への抽象メンバー追加は、契約を自前で実装する利用者のテストダブルを壊すので proposal の『破壊的変更なし』と矛盾する」を、オーナーが「Dialog の型指定 show (core/ADR-0019〜0021) と同じ形であり、非破壊は呼ぶ側の互換を指す」として却下 (A を選択)。指摘は事実としては正しく、判断依頼 1 往復とオーナー向け解説の作成が発生した)
---

## ルール文

ライブラリの表向きの契約 (利用者が呼ぶ protocol / interface) にメンバーが追加されている diff をレビューするとき、「契約を自前で実装する利用者 (テストダブル) が壊れるので破壊的変更」という指摘を出す前に、同じ契約に同じ形でメンバーを足した accepted ADR (`kasane/decisions/`) と、proposal の「破壊的変更なし」が何の互換 (呼ぶ側 / 実装する側) を指しているかを確認する。先例が accepted で proposal が呼ぶ側の互換を指しているなら、Major ではなく「既定実装で吸収する案は別 change の設計判断」として Suggestion に留める。守れたかは、指摘の本文に参照した ADR ID と proposal の該当行が書かれていることから判定する。

## 経緯

- 2026-09-06 add-loading-toast-typed-show: 相方 (codex) が契約への抽象メンバー追加を Major (CHANGES_REQUESTED の根拠) とした。ホスト review-001 は指摘せず、突き合わせで降格したが、指摘自体は事実として正しいため指揮側がオーナー判断に上げた。オーナーは Dialog の先例と同型として A (現状維持) を選択し、「非破壊 = 呼ぶ側の互換」を蒸留で concepts に残すことにした
