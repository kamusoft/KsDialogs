---
scope: spec-review
timestamp: 2026-09-06
---

# lessons: spec-review

- [L-001] 複数形態 (プラットフォーム・言語境界) に共通の公開契約を design の Decision として確定するとき、形態ごとに「その契約を言語機構・既存 interop 面・表示階層で実現する経路」を特定して Decision の根拠に書く (既存配管なら該当ファイル・API 名の明示、新規機構なら最小プローブのコンパイル確認、配置要件なら既存実装の表示階層との突き合わせ)。実現経路を示せない形態がある場合、その形態の Requirement/Scenario を spec に入れず、設計論点として proposal の Open Questions か Non-Goals に置く。([経緯](details/design-decision-needs-platform-feasibility-probe.md)) (昇格: 2026-08-26、出典: add-layout-spec / add-presentation-behavior / add-loading)
- [L-002] デルタスペック・design が「存在する」と前提にする API (遮断や区別の対象となる報告経路、移行対応表の移植元 API、全形態に同綴りで存在するとする名前) は、契約を確定する前に、対象となる各形態の公開面 (ソースの public 宣言、移植元の README と公開ソース) で実在を確かめる。実在しない主体や名前を持つ契約は、経路を足すか、契約から落とすか、「実装時に確定する」と条件付けしてから確定する。守れたかは、design / spec の当該箇所に確認先 (ファイル・宣言) が添えてあることから判定する。([経緯](details/spec-contract-references-nonexistent-api-path.md)) (昇格: 2026-09-06、出典: add-loading / rollout-user-docs / split-concepts-platform-surface)
