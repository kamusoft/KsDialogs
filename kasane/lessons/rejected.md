# lessons 却下台帳

| 日付 | scope | 要約 | 却下理由 |
|---|---|---|---|
| 2026-09-09 | test | 否定形 Scenario の観測は履歴と settle 待ちを揃える (negative-scenario-observation-needs-history-and-settle, count 1) | handbook 化済み (→ handbook/cross/ci-flaky-test-policy.md「観測は終端状態の合意を待つ」。fix-android-instrumented-toast-ime-hide-flake の蒸留) |
| 2026-09-09 | impl | CI ランナーの並列スイートの飢餓を待ち不足と読まない (ci-runner-parallel-suites-starve-main-actor, count 1) | handbook 化済み (→ handbook/cross/verification-ci.md「CI の Swift テストはスイートを直列で回す」の見分け表。同蒸留) |
| 2026-09-26 | impl | テストを増やした change は handbook の件数表を同じ change で更新する (test-count-table-must-follow-in-change, count 3) | 前提の撤去 (オーナー判断: handbook に実測件数を載せること自体が誤り。define-loading-action-thread の蒸留で test-execution.md から件数の実測値を外し、件数は各変更の証跡に残す形にした。表が無いので更新漏れも起きない) |
