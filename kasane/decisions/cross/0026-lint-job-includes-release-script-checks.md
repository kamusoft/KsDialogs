---
id: 0026
title: 検証 CI の lint job にリリース用スクリプトの自己テストと待ちの時間予算・publish の step 順序の検査を加え、11 検査とする (0022 を一部改訂)
status: proposed
date: 2026-09-13
amends: [cross/0022]
amended-by: [cross/0029]
---

## Context

リリースで使うスクリプト (`scripts/release/`) の判定ロジックは、本番の経路を通るのがリリースのときだけで、しかもその経路の大半は不可逆な公開の後にしか到達しない。`dry-run` では publish job そのものが走らないため、待機・引き継ぎ・署名検査の判定が壊れていても起動前には分からない。各スクリプトはネットワークに出ずに判定ロジックだけを検査する `--selftest` を持つが、その自己テストが検証 CI から一度も呼ばれていない。

待ちの時間予算はさらに見えにくい。待ちの上限はスクリプト側の定数、job の打ち切りは workflow 側の `timeout-minutes` という二重管理になっており、片方だけを延ばしてももう片方が置いていかれたまま平時の実行は緑で進む。sibling の KsSettingsView では実際にこの関係が実装中に破れ、レビューが Major として検出した。publish の step 順序 (保留中 deployment の引き継ぎの読み込みが、失敗しうる他の成果物の取得より前にあること) も、step を並べ替えるだけで崩れ、崩れても平時は緑のままになる。

cross/ADR-0022 (cross/ADR-0021・0020・0017 の系譜) は lint job を 8 検査に固定しており、検査を無断で足すことはできない。

前提: lint job は `ubuntu-24.04` で `timeout-minutes: 10`。`scripts/release/` の自己テスト 8 本の所要は手元実測で合計 13 秒台、時間予算と step 順序の検査は 1 秒未満で、いずれもネットワークに出ない。`set-readme-version.py` は後続の変更で撤去する予定のため対象に含めない。

## Decision

cross/ADR-0022 の決定のうち「検証 CI の lint job に README 最小例と消費者ソースの一致検査を加えた 8 検査」を、これに **リリース用スクリプトの自己テスト・待ちの時間予算の検査・publish の step 順序の検査の 3 つを加えた 11 検査**へ置き換える。他の決定 (README 最小例の検査対象・違反・対象外・追随の運用) は維持する。

| 要素 | 決定 |
|---|---|
| リリース用スクリプトの自己テスト | `scripts/release/` の 8 本 (`central-portal.sh` / `central-resume.sh` / `check-distribution-tag.sh` / `check-nuget-version.sh` / `check-resume-eligibility.sh` / `check-signatures.sh` / `compare-maven-artifacts.sh` / `wait-for-registries.sh`) の `--selftest` を 1 検査として数える。既存の SwiftPM スナップショット同期スクリプトの自己テストが同種の検査を 1 つと数えている前例に揃える |
| 待ちの時間予算 | `scripts/release/check-time-budget.py` が、`central-portal.sh` / `wait-for-registries.sh` の定数と `release.yml` の `timeout-minutes` を突き合わせる。定数を読み取れない形への退化と、同じ定数の値が複数に割れている状態も失敗とする (fail-closed) |
| publish の step 順序 | `scripts/release/check-publish-step-order.py` が、引き継ぎの download と読み込みが他の成果物の取得より前にあることを検査する |
| 実行 | 3 検査とも lint job で、自己テストを本検査より先に同じ step で走らせる (cross/ADR-0020・0021・0022 と同じ理屈)。`develop` への push のたびに落とす |
| 対象外 | `scripts/release/set-readme-version.py` (後続の変更で撤去するため) |

## Alternatives Considered

- **自己テストを lint job へ接続せず、リリース経路の判定は本番のリリースで確かめる** — 却下。判定ロジックの誤りに気づくのが不可逆な公開の後になる。`dry-run` では publish job が走らないため、起動前の検査手段が自己テスト以外に無い
- **時間予算を検査ではなく `release.yml` の内訳コメントだけで管理する** — 却下。定数と `timeout-minutes` の二重管理は片方だけを延ばしても緑のまま進む。sibling ではこの関係が実装中に実際に破れた
- **`scripts/release/` 8 本を個別の検査として数え、18 検査とする** — 却下。既存の SwiftPM スナップショット同期スクリプトの自己テストが同種の検査を 1 つと数えており、数え方を揃えないとスクリプトの増減のたびに ADR の改訂が要る
- **`set-readme-version.py` も対象に含める** — 却下。後続の変更で撤去するため、接続しても短命な追随を生むだけになる

## Consequences

- 正: リリース経路の判定ロジックの退行が、`develop` への push のたびに検出される
- 正: 待ちの上限と job の打ち切りのどちらを動かしても、合計が収まらなければ lint が落ちる
- 正: 引き継ぎの読み込み順序が、step の並べ替えだけで静かに崩れることがなくなる
- 負: lint job の検査が 3 つ増え、所要が十数秒伸びる (`timeout-minutes: 10` に対しては余裕がある)
- 負: 時間予算の検査は定数の綴りを正規表現で読むため、スクリプト側で定数の持ち方を変えるときに追随を要する。fail-closed にしてあるので、追随を怠れば検査が落ちて気づける
- 負: `scripts/release/` のスクリプトを増減しても検査の数え方は変わらないが、lint job の step の列挙は手で追随させる必要がある

## Revisit When

- `scripts/release/` の構成が大きく変わったとき (自己テストを持たないスクリプトが増える・`set-readme-version.py` が撤去される)
- 待ちの構造が変わり、時間予算の式 (待機の上限 + 期限を跨げる照会 + 本体処理) が成り立たなくなったとき
- lint job の所要が `timeout-minutes: 10` に対して余裕を失ったとき

---
出典: kasane/changes/backport-registry-wait-hardening/proposal.md (Why の弱点 4 点・時間予算の方針・既存 ADR との関係) / ../KsSettingsView/kasane/outbox/KsDialogs/2026-09-12-release-mechanism-hardening-and-notes-rework.md (sibling の改修結果と時間予算の破れ)
関連: cross/ADR-0022 (lint job の検査の集合。本決定で 8 → 11 検査に置き換え) / cross/ADR-0020 (自己テストを本検査と同じ step で先に走らせる理屈) / cross/ADR-0017 (検証 CI の構成とトリガー) / cross/ADR-0024 (publish が develop へインストール例を反映する前に掛ける 3 検査)
