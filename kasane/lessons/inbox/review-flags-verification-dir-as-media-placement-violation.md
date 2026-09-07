---
scope: code-review
kind: pain
severity: normal
count: 1
first-seen: 2026-09-07
last-seen: 2026-09-07
evidence:
  - add-kmp-typed-show (相方 second-opinion-code-001 の Major「Sample 証跡 12 枚が媒体配置のホワイトリスト外の `verification/` にあり `distill.archive-media` の管理から外れる」を、オーナーが「置き場は先例 (add-loading-toast-typed-show 等) どおり `verification/` で、`kasane/config.yaml` の `lint.exclude` もこのパスを前提にしている」として降格・対応なし (review-002 で合意済みとして再指摘しない扱いに)。ksn-distill の媒体削除は置き場を問わず change ディレクトリ配下を対象にするため、実害の主張 (archive 時に削除されない) も成立しなかった)
---

## ルール文

change 配下の証跡の置き場 (`verification/` / `evidence/` 等) が ksn-core の規約と字面で違っていても、レビューは Major にする前に、同じリポジトリの archive 済み change での置き場の先例と、`kasane/config.yaml` の `lint.exclude` がそのパスを前提にしているかを確認する。先例と config がそのパスを採っているなら、置き場の是正は指摘ではなく ksn-drift の棚卸し (規約と運用のどちらを直すか) へ回し、指摘するなら「規約の字面と運用の食い違い」として Suggestion に留める。守れたかは、指摘の本文に確認した先例 change と config の該当行が書かれていることから判定する。

## 経緯

- 2026-09-07 add-kmp-typed-show: 相方が規約 (references/ui-artifacts.md / evidence.md) の字面だけを見て Major にした。ホスト review-001 は指摘せず、突き合わせで降格。置き場の食い違い自体は add-loading-toast-typed-show の蒸留 (2026-09-06) で既に ksn-drift の棚卸し送りになっており、レビューが再発見しても実装側で直すものではない
