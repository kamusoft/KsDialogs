---
id: 0020
title: 検証 CI の lint job に SwiftPM スナップショット同期スクリプトの自己テストを加え、6 検査とする (0017 を一部改訂)
status: accepted
date: 2026-09-08
amends: [cross/0017]
---

## Context

SwiftPM の配信リポジトリ `KsDialogs-SPM` へスナップショットを置く同期スクリプト (`scripts/spm-snapshot/sync-snapshot.sh`) は、同期先の `.git/` 以外を除去する破壊的操作を持ち、引数の誤指定で無関係な作業ツリーを消さないための事前検証 4 段 (コピー元の存在・git top-level・origin の完全一致・monorepo 自身や祖先でないこと) を備える。この検証は自己テスト (`sync-snapshot-test.sh`) が確かめるが、テストの置き場は `scripts/` であり、検証 CI の platform 別 job がビルド構成から導出するテストルートには入らない。実装時に 1 度走らせるだけでは、スクリプトを後から変えたときに事前検証の退行が検出されない (提案段階の相方レビューの指摘)。

cross/ADR-0017 は lint job の検査を 5 つに固定しているため、検査を無断で足すこともできない。

前提: 自己テストは一時ディレクトリに配信リポジトリを模した git リポジトリを作って走り、monorepo は読むだけでネットワークにも出ない。global の git identity を持たないランナーでも自分で identity を与えて動く。

## Decision

cross/ADR-0017 の決定のうち「lint job は secret scan・ローカル絶対パス・個体情報・コメント規約・仕様の Scenario ID とテスト名の網羅の 5 検査を持つ」を、これに **SwiftPM スナップショット同期スクリプトの自己テストを加えた 6 検査**へ置き換える。他の決定 (構成・緑の意味・トリガー・件数検査) は維持する。

自己テストは lint job の step として起動のたびに実行する (lint job は変更検出のスキップ対象にならないため、スクリプトだけを変えた push でも走る)。

## Alternatives Considered

- **手元で実装時に実行するだけにする** — 却下。スクリプトを後から変えたときに事前検証の退行を検出する経路がない (相方レビューの指摘理由)
- **専用の status check (別 job) にする** / **release workflow の必須事前処理として実行する** — 相方レビューが継続実行経路の候補として挙げた形。オーナー裁定 (2026-09-08) で lint job への追加を採り、個別の却下理由は出典に記載なし

## Consequences

- 正: 同期スクリプトの事前検証の退行が、`develop` への push のたびに検出される
- 正: 検証 CI の status check の集合 (`lint` と 5 job) は変わらず、マージ保護の設定に影響しない
- 負: lint job の所要時間に自己テスト分が乗る (一時 git リポジトリの作成を含む)
- 負: 「lint」という job 名の下に lint ではない検査が 1 つ入り、job 名から検査の内容を推し量れなくなる

## Revisit When

- 前提 (Context) が崩れたとき。特に自己テストがネットワークや monorepo への書き込みを要するようになったとき、または `scripts/` 配下のテストが増えて lint job に載せ切れなくなったとき

---
出典: kasane/changes/archive/2026-09-08-add-native-distribution/design.md (Open Questions — 2026-09-08 のオーナー裁定、ADR 候補) / 同 second-opinion-spec-001.md (Minor「破壊的同期スクリプトのテストが継続的に実行されない」) / 同 tasks.md (6.4)
関連: cross/ADR-0017 (検証 CI の構成と lint job の検査。本決定が検査の集合を置き換える)
