# 検証結果: backport-registry-wait-hardening (002 回目)

**日付**: 2026-09-13
**判定**: INVALID (❌ 1 件 — verify-001 の ❌ が未解消)

verify-001 で ❌ だった 1 件 (Requirement「リリース用スクリプトの自己テスト」本文の箇条書き) の
解消可否を再判定し、あわせて今回の修正が ✅ だった項目を壊していないことを確認した。
Scenario 単位の欠落は前回と同じく無い。

## 実行したテスト

| 実行 | 結果 |
|---|---|
| `scripts/release/central-portal.sh --selftest` | exit 0 / 失敗なし |
| `scripts/release/central-resume.sh --selftest` | exit 0 / 失敗なし |
| `scripts/release/check-distribution-tag.sh --selftest` | exit 0 / 失敗なし |
| `scripts/release/check-nuget-version.sh --selftest` | exit 0 / 失敗なし |
| `scripts/release/check-resume-eligibility.sh --selftest` | exit 0 / 失敗なし |
| `scripts/release/check-signatures.sh --selftest` | exit 0 / 失敗なし |
| `scripts/release/compare-maven-artifacts.sh --selftest` | exit 0 / 失敗なし |
| `scripts/release/wait-for-registries.sh --selftest` | exit 0 / 失敗なし |
| `python3 scripts/release/check-time-budget.py --selftest` / 本検査 | exit 0 / exit 0 |
| `python3 scripts/release/check-publish-step-order.py --selftest` / 本検査 | exit 0 / exit 0 |
| `python3 scripts/comment-policy-lint.py` | exit 0 (禁止 0 件) |
| `python3 scripts/local-path-lint.py` / `identity-lint.py` | 両方 exit 0 |

8 本の shell 自己テストの合計所要は 15 秒。

## 対応表 (前回からの変化のみ状態を更新)

### Requirement: 待ちの時間予算

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Requirement 本文 (publish の予算表 4 項 + 余裕 = 150) | `scripts/release/check-time-budget.py:52-57,143-186` / `.github/workflows/release.yml:548-567` | `check-time-budget.py --selftest` + 本検査の出力 (90/5/1/16、余裕 38) | ✅ 一致 (内訳コメントの追記で数値は不変) |
| Requirement 本文 (反映待ちの予算表 3 項 + 余裕 = 60) | `check-time-budget.py:60-65,188-221` / `release.yml:1494-1512` | 同上 (45/2/5、余裕 8) | ✅ 一致 |
| Requirement 本文 (定数を検査から読める形で持つ) | `check-time-budget.py:84-98` | selftest 4 件 | ✅ 一致 |
| Scenario: 公開待ちを延ばすと予算の超過が検出される | `check-time-budget.py:143-186` | selftest (5400→9000 で exit 1) | ✅ 一致 |
| Scenario: 反映待ちの応答上限を延ばすと予算の超過が検出される | `check-time-budget.py:188-221` | selftest (120→900 で exit 1) | ✅ 一致 |
| Scenario: 定数を読み取れない形へ変えると検査が失敗する | `check-time-budget.py:88-98` | selftest 4 件 | ✅ 一致 |
| Scenario: 時間予算の検査が lint job で走る | `.github/workflows/ci.yml:342-347` | ci.yml の静的確認 (`--selftest` → 本検査の順) | ✅ 一致 (CI 実行は tasks 6.3 未了) |

`[付随修正]` の接続上限検査 (`check-time-budget.py:223-252`) は deviation.md 記録済み → ⚠️ deviation 記録済み。

### Requirement: 公開レジストリ照会の状態分類

本 change の今回の修正は `wait-for-registries.sh` に一切触れていない (diff なし)。
前回 ✅ だった 12 行はすべて据え置きで再確認した (自己テスト exit 0 / 失敗なし)。
加えて新たな 3 種類の誤実装 (期限判定の撤去 / 反映済みの完了判定の撤去 / 未照会の初期値を未反映へ) を
与え、いずれも 4〜14 秒で NG として現れ停止しないことを確認した → 全行 ✅ 一致を維持。

### Requirement: 保留中 deployment の引き継ぎの読み込み順序

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Requirement 本文 (引き継ぎの読み込みが他の成果物の取得より前) | `.github/workflows/release.yml:752-792` → `:794` 以降 | `check-publish-step-order.py --selftest` + 本検査 (出力で並びを確認) | ✅ 一致 |
| Requirement 本文 (読み込んだか / 引き継ぎが存在したかを区別) | `release.yml:777-792` (`handover` / `handover-<slot>` / `ready`) + `:1474` (step summary の「引き継ぎ」行) + `:1485` (`KS_HANDOVER`)。`Summarize` は `if: always()` (`:1454`) なので読み込み失敗時も行が残り、値が空 = `未実行` になる | 自動テストなし | ⚠️ 自動検証不能 (publish job の実行時のみ観測可能。実装は前回より出力に近づいた) |
| Requirement 本文 (順序を機械的に検査できる) | `scripts/release/check-publish-step-order.py:90-160` | selftest 8 件 | ✅ 一致 |
| Scenario: 引き継ぎの読み込みが他の成果物の取得より先に行われる | `release.yml` の step 並び | selftest で担保 (THEN の観測は publish job 実行時のみ) | ✅ 一致 |
| Scenario: 引き継ぎが無い初回の実行と区別できる | `release.yml:777,784-791` (`handover=absent`) + step summary の「引き継ぎ」行 | 自動テストなし | ⚠️ 自動検証不能 |
| Scenario: 順序が崩れると検査が落ちる | `check-publish-step-order.py:109-160` | selftest 7 件が exit 1 | ✅ 一致 |

### Requirement: リリース用スクリプトの自己テスト

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Requirement 本文 (自己テストを持ち、日常の検証 CI で実行される) | `.github/workflows/ci.yml:328-340` (8 本) + `:342-347` + `:349-354` | 8 本すべて exit 0 (実行済み) | ✅ 一致 |
| Requirement 本文 (誤った実装を与えたときに待機が終わらない形ではなく失敗として表す) | `central-portal.sh:422-448` (台本切れを HTTP ステータスで返す) / `:559` `:574` `:586` `:614` (上限 5) / `:624` (上限 1) / `wait-for-registries.sh:676-679,697,733,746` | 下記の誤実装注入 5 種 | ❌ 乖離 (下記) |
| Scenario: リリース用スクリプトの自己テストが lint job で走る | `ci.yml:328-340` | ci.yml の静的確認 | ✅ 一致 (CI 実行は tasks 6.3 未了) |
| Scenario: 待ちの誤実装が停止ではなく失敗として表れる | 同上 | 「対象の一部しか見ない」「公開済みの枠を外さない」「直列化」の 3 種でいずれも失敗 (6〜23 秒) | ✅ 一致 (Scenario の GIVEN「対象の一部しか見ない」の範囲では成立) |

## ❌ の詳細と見立て

### ❌ Requirement「リリース用スクリプトの自己テスト」本文の箇条書き (verify-001 から継続)

> 自己テストは、誤った実装を与えたときに待機が終わらない形ではなく、失敗として表す SHALL。

**前回からの変化**: `cmd_wait_published` を呼ぶ**成功を期待する 4 検査**
(`central-portal.sh:559` `:574` `:586` `:614`) に `KSR_PUBLISHED_TIMEOUT_SECONDS=5` が与えられた。
これにより verify-001 が実測した誤実装 (`PUBLISHED` の枠を `remaining` から外さない) は
**停止 → 失敗 (23 秒 / NG 8 件)** に変わった。この 1 点は解消している。

**残る乖離**: `cmd_wait_published` を呼ぶ検査のうち 3 件 (`:544` `:564` `:600`、いずれも失敗を期待するもの)
は上限を持たないままで、そこで停止する誤実装が別に 2 種類ある。

| 与えた誤実装 (1 箇所のみ) | 現在の成果物での結果 |
|---|---|
| `FAILED)` 分岐を落とす (FAILED を終端として扱わない) | **120 秒でも終わらず `:564`「FAILED になれば失敗する」で停止** (NG 0 件) |
| `"${DEPLOYMENT_NOT_FOUND}")` 分岐を落とす (NOT_FOUND を終端として扱わない) | **60 秒でも終わらず `:544`「存在しない deployment の公開待ちは失敗する」で停止** (NG 0 件) |

いずれも、停止した検査自身が捕まえるべき誤実装である。停止の仕組みは前回と同一
(`state="$(deployment_state "${id}")"` のコマンド置換で `fail` がサブシェル内に閉じ、
空の状態が `*)` に落ちて待機対象に残る。検査は `if ( ... )` の条件で呼ばれるため `set -e` も効かない)。
lint job は `timeout-minutes: 10` なので、job ごと打ち切られて出力が残らない。

**deviation.md との関係**: deviation.md の `[自己テストの構造]` は
「台本切れが起きうる検査には公開待ちの上限を短く与えた」と記録しているが、上限が与えられたのは
成功期待の 4 件と新設の `:624` のみで、台本切れが起きうる残り 3 件は対象外。**未記録の乖離**のまま。

**見立て**: **実装を直す**のが妥当。残り 3 件への環境変数の追加で停止は消える (実測: 正しい実装で
5 秒 / 失敗なし、FAILED 非終端で 13 秒 NG 1 件)。ただし NOT_FOUND 非終端の誤実装は上限を足しただけでは
「上限で失敗した」として緑になり検出できない (実測: 10 秒 / NG 0 件 / exit 0) ため、
`:544` `:564` に照会回数の表明を添えると両方を検出できる (実測: 10 秒 NG 1 件 / 13 秒 NG 2 件)。
詳細は review-002.md の Major を参照。

## 追加検査

| 検査 | 結果 |
|---|---|
| tasks.md の全タスク完了 | ❌ 未完了。1〜5 は完了。**6.1〜6.4 が未チェック**。6.1 (全 `--selftest` が手元で通る) と 6.2 (定数・step 順序を壊すと落ちる) は本検証で実行して通ることを確認済み。6.3 (`develop` への push で lint job) と 6.4 (`dry-run` の release) は未了 |
| 虚偽チェック | 無し。チェック済みの 1.1〜5.1 はすべて実装・テストに対応が取れた。修正で追加された「蒸留への申し送り」2 行 (`verification-ci.md` / `ci-script-deletion.md`) は実装タスクではない |
| 逆流検査 (足場の書き換え) | 判定不能 (前回と同じ)。`kasane/changes/backport-registry-wait-hardening/` 一式が未追跡 (`??`) のため git 履歴が無い。`specs/` `proposal.md` の内容は実装と整合しており、後追いで spec を実装へ合わせた形跡は見当たらない。今回の修正でも `specs/` `proposal.md` の mtime は変わっていない (更新されたのは `tasks.md` の申し送りのみ) |
| 未記録乖離 | 上記 ❌ 1 件 |
| 付随修正 | deviation.md の `[付随修正]` 1 件 (`check-time-budget.py` の接続上限検査)。Requirement を持たないため対応表の対象外。diff にあって Scenario に対応しない変更は他に見当たらない (今回追加の `handover-android` / `handover-kmp` は Requirement「読み込んだか / 引き継ぎが存在したか」の実装の一部であり、粒度が spec より細かい点は review-002 の Minor で扱う) |
| UI 変更 | 無し (該当なし) |
| テスト全件成功 | ✅ 上表の実行がすべて exit 0 |

## 判定

**INVALID** — ❌ 1 件。verify-001 の ❌ (Requirement「リリース用スクリプトの自己テスト」本文の箇条書き) は
1 種類の誤実装については解消したが、同型の停止が別に 2 種類残っており、Requirement は満たされていない。
16 Scenario 単位の欠落は無い。加えて tasks 6.3 / 6.4 が未了のため、アーカイブ可能な状態ではない。
