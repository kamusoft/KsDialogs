---
scope: spec-review
kind: pain
severity: normal
count: 1
first-seen: 2026-09-10
last-seen: 2026-09-10
evidence:
  - add-release-workflow (spec「同じ version での再実行」は「部分 publish の続行は同じ run の再試行に限る」と書き、design Decision 7 も `github.run_attempt` が 2 以上であることを続行の条件にした。実装はそのとおり試行回数で判定したが、外部状態が既にある新規 dispatch が attempt 1 で拒否された後、案内どおりその run を再実行すると attempt 2 になり、外部状態の出所を確かめずに resume が許可されて別 commit の binary に tag を打てた。ホストの review-001 は spec の字面 (同じ run) で通し、second-opinion-code-001 の Critical が検出。修正は「外部状態が無いことを確認した試行だけが印 (version・commit・run id) を残し、印が一致する再試行だけ resume」)
---

## ルール文

spec / design が「同じ run の再試行に限る」「同じ出所に限る」のような出所 (provenance) の制約を書くときは、実行系がその出所を実際に証明できる観測 (自分が残した印の一致・commit と run id の照合) を Requirement / Decision に含める。試行回数・タイムスタンプのような「出所を示唆するが証明しない」値だけで制約を実装できると読める書き方にしない。守れたかは、当該 Requirement / Decision に「何を根拠に出所を判定するか」と、それが偽装・混同できない理由が書かれていることから判定する。

## 経緯

- 2026-09-10 add-release-workflow: spec が守りたかった性質は「公開済みの binary と tag が指す source の対応」であり、「同じ run」はその手段の言い換えだった。手段の字面を Requirement にすると、手段が性質を満たさない実装 (試行回数だけの判定) がレビューを通る。性質そのものを書き、手段に「証明できる観測」を要求する
