---
scope: process
kind: pain
severity: normal
count: 2
first-seen: 2026-09-05
last-seen: 2026-09-26
evidence:
  - rollout-user-docs (廃止した README を名指ししていた accepted の cross/ADR-0006・0007 は本文不変のため書き換えられず、現行照合 footer で現在の所在を示すにとどまった。cross/ADR-0012 の Consequences に「実装で判明」として記録されていた)
  - 2026-09-26 ksn-drift (concepts cross/reference/reference-repositories.md の廃止で、accepted の cross/ADR-0011 の Decision が manifest の excluded の初期値として同文書を名指ししていた。廃止後に footer の関連行で廃止を示した。同じ作業で cross/ADR-0003 の Decision 2 も同文書を経由先として名指ししていたことを見つけ、同様に対処)
---

## ルール文

長命層の文書 (concepts / handbook / skills の源泉) を廃止・改名する前に、`kasane/decisions/` をその文書のパスとタイトルで検索し、名指ししている accepted ADR を列挙する。見つかった ADR は本文を書き換えず、廃止と同じ作業で footer に `関連:` 行を足して廃止後の所在 (または廃止の事実) を示す。

## 経緯

- 2026-09-05 rollout-user-docs: README の廃止で accepted ADR 2 本が旧パスを名指ししたまま残り、残存参照 0 件の検査が達成不能になった (cross/ADR-0012 の Consequences に観測として混入していたものを 2026-09-26 の ksn-drift で移送)
- 2026-09-26 ksn-drift: reference-repositories.md の廃止時、cross/ADR-0011 の Decision が同文書を名指ししていることを廃止作業の途中で見つけ、footer の関連行で対処した。規則どおり decisions/ を検索して cross/ADR-0003 の名指しも見つけた
