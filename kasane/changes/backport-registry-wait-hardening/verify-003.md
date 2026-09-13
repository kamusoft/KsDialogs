# 検証結果: backport-registry-wait-hardening (003 回目)

**日付**: 2026-09-13
**判定**: INVALID (❌ 1 件 — tasks 6.3 / 6.4 が未実行。仕様対応の ❌ は 0 件)

verify-001 / verify-002 で ❌ だった 1 件 (Requirement「リリース用スクリプトの自己テスト」本文の箇条書き)
を再判定し、**解消を確認**した。あわせて今回の修正が ✅ だった行を壊していないことを確認した。
残る ❌ は tasks の未実行 2 件のみで、これは実装の欠落ではなく検証手順の未了。

## 実行したテスト

| 実行 | 結果 |
|---|---|
| `scripts/release/central-portal.sh --selftest` | exit 0 / 失敗なし (5 秒) |
| `scripts/release/central-resume.sh --selftest` | exit 0 / 失敗なし |
| `scripts/release/check-distribution-tag.sh --selftest` | exit 0 / 失敗なし |
| `scripts/release/check-nuget-version.sh --selftest` | exit 0 / 失敗なし |
| `scripts/release/check-resume-eligibility.sh --selftest` | exit 0 / 失敗なし |
| `scripts/release/check-signatures.sh --selftest` | exit 0 / 失敗なし |
| `scripts/release/compare-maven-artifacts.sh --selftest` | exit 0 / 失敗なし |
| `scripts/release/wait-for-registries.sh --selftest` | exit 0 / 失敗なし |
| `python3 scripts/release/check-time-budget.py --selftest` / 本検査 | 15 件 / 失敗なし、exit 0 |
| `python3 scripts/release/check-publish-step-order.py --selftest` / 本検査 | 8 件 / 失敗なし、exit 0 |
| `python3 scripts/comment-policy-lint.py` | exit 0 (禁止 0 件) |
| `python3 scripts/local-path-lint.py` / `identity-lint.py` | 両方 exit 0 |

shell 自己テスト 8 本の合計所要は 14 秒。

## 対応表

### Requirement: 待ちの時間予算 (今回の修正で変更なし)

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Requirement 本文 (publish の予算表 4 項 + 余裕 = 150) | `scripts/release/check-time-budget.py:143-186` / `.github/workflows/release.yml:548-579` | 本検査の出力 (90 / 5 / 1 / 16、余裕 38) | ✅ 一致 |
| Requirement 本文 (反映待ちの予算表 3 項 + 余裕 = 60) | `check-time-budget.py:188-221` / `release.yml:1494-1511` | 本検査の出力 (45 / 2 / 5、余裕 8) | ✅ 一致 |
| Requirement 本文 (定数を検査から読める形で持つ) | `check-time-budget.py:84-98,146-152` | selftest 4 件 | ✅ 一致 |
| Scenario: 公開待ちを延ばすと予算の超過が検出される | `check-time-budget.py:143-186` | selftest (5400→9000 で exit 1) | ✅ 一致 |
| Scenario: 反映待ちの応答上限を延ばすと予算の超過が検出される | `check-time-budget.py:188-221` | selftest (120→900 で exit 1) | ✅ 一致 |
| Scenario: 定数を読み取れない形へ変えると検査が失敗する | `check-time-budget.py:84-98` | selftest 4 件 | ✅ 一致 |
| Scenario: 時間予算の検査が lint job で走る | `.github/workflows/ci.yml:342-347` | ci.yml の静的確認 (`--selftest` → 本検査の順) | ✅ 一致 (CI 実行は tasks 6.3 未了) |

`[付随修正]` の接続上限検査 (`check-time-budget.py:223-253`) は deviation.md 記録済み → ⚠️ deviation 記録済み。

今回の修正 (自己テストへの `KSR_PUBLISHED_TIMEOUT_SECONDS=5` の追加) が定数の読み取りを壊していないことを
確認した。`check-time-budget.py:147` の正規表現は `KSR_PUBLISHED_TIMEOUT_SECONDS:-(\d+)` と既定値の形に
限定されているため、自己テストの `=5` は「値が割れている」に当たらない (本検査 exit 0 で実測)。

### Requirement: 公開レジストリ照会の状態分類 (今回の修正で変更なし)

`scripts/release/wait-for-registries.sh` は今回も変更されていない (前回検証以降 mtime が動いていない)。
verify-002 で ✅ とした全行を据え置きで再確認した (自己テスト exit 0 / 失敗なし)。

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Requirement 本文 (照会できたうえで未反映 / 照会できなかったを区別) | `wait-for-registries.sh:78-85,206-296` | selftest の Maven / NuGet 6 入力表 | ✅ 一致 |
| Requirement 本文 (対象 10 件それぞれ独立に保つ) | `wait-for-registries.sh:75,325-360` | selftest `:571-572` (10 件) + 混在の巡回 | ✅ 一致 |
| Requirement 本文 (判定不能を 3 種別に分ける) | `wait-for-registries.sh:83-85,214-296` | selftest の 5xx / 通信失敗 / 解釈不能 | ✅ 一致 |
| Requirement 本文 (いずれも待機を継続する) | `wait-for-registries.sh:362-380,395-405` | selftest の判定不能からの回復 | ✅ 一致 |
| Requirement 本文 (反映済みは再照会しない) | `wait-for-registries.sh:352-361` | selftest `:737` `:752` (照会回数) | ✅ 一致 |
| Requirement 本文 (未照会と未反映を区別) | `wait-for-registries.sh:82,325-351` | selftest (初期値が未照会) | ✅ 一致 |
| Requirement 本文 (上限到達時に対象ごとの分類を出力) | `wait-for-registries.sh:381-394,433` | selftest の上限超過時の出力 | ✅ 一致 |
| Requirement 本文 (残り時間が正のときだけ照会を開始) | `wait-for-registries.sh:137-158,369` | selftest `:589-597` (固定入力) | ✅ 一致 |
| Scenario: 未反映は待機を続ける | `wait-for-registries.sh:214-296,395-405` | selftest (404 → 未反映で継続) | ✅ 一致 |
| Scenario: 照会できなかった場合も待機を続ける | `wait-for-registries.sh:159-205,214-296` | selftest (通信失敗 / 5xx) | ✅ 一致 |
| Scenario: 判定不能から回復すれば反映済みになる | `wait-for-registries.sh:352-361` | selftest `:752` | ✅ 一致 |
| Scenario: 対象ごとに状態が分かれて残る | `wait-for-registries.sh:381-394,422` | selftest (10 対象の混在) | ✅ 一致 |
| Scenario: 上限超過時に対象ごとの最後の状態が分かる | `wait-for-registries.sh:433` | selftest (上限超過時の出力) | ✅ 一致 |
| Scenario: 期限の直前に始まった照会が種別を偽らない | `wait-for-registries.sh:147-158,369` | selftest `:589-597` | ✅ 一致 |
| Scenario: 不正な形の応答は判定不能として扱う | `wait-for-registries.sh:234-258` | selftest (文字列でない要素) | ✅ 一致 |

### Requirement: 保留中 deployment の引き継ぎの読み込み順序

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Requirement 本文 (引き継ぎの読み込みが他の成果物の取得より前) | `.github/workflows/release.yml:752-789` → `:791` 以降 | `check-publish-step-order.py` 本検査の出力 (`Download previous deployment ids` → `Prepare deployment id files` → `Download Android artifacts`) | ✅ 一致 |
| Requirement 本文 (読み込んだか / 引き継ぎが存在したかを区別) | `release.yml:771-789` (`handover` / `ready`) + `:1471` (step summary の「引き継ぎ」行) + `:1484` (`KS_HANDOVER`)。`Summarize` は `if: always()` なので読み込み失敗時も行が残り、値が空 = `未実行` になる | 自動テストなし | ⚠️ 自動検証不能 (publish job の実行時のみ観測可能) |
| Requirement 本文 (順序を機械的に検査できる) | `scripts/release/check-publish-step-order.py:90-160` | selftest 8 件 | ✅ 一致 |
| Scenario: 引き継ぎの読み込みが他の成果物の取得より先に行われる | `release.yml` の step 並び | selftest で担保 (THEN の観測は publish job 実行時のみ) | ✅ 一致 |
| Scenario: 引き継ぎが無い初回の実行と区別できる | `release.yml:777,784,788` (`handover=absent` / `present`) + step summary の「引き継ぎ」行 | 自動テストなし | ⚠️ 自動検証不能 |
| Scenario: 順序が崩れると検査が落ちる | `check-publish-step-order.py:109-160` | selftest 7 件が exit 1 | ✅ 一致 |

今回の修正で `handover-android` / `handover-kmp` の 2 output が削除された。spec は枠ごとの粒度を求めて
いないため要求の欠落にはならず、枠ごとの内訳は `release.yml:782` のログ行に残る。
`steps.deployment-ids.outputs` の参照は `ready` 3 箇所と `handover` 1 箇所で、未参照の output は無い。
step の並びは動いておらず、`check-publish-step-order.py` の本検査・自己テストとも exit 0。

### Requirement: リリース用スクリプトの自己テスト

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Requirement 本文 (自己テストを持ち、日常の検証 CI で実行される) | `.github/workflows/ci.yml:328-340` (8 本) + `:342-347` + `:349-352` | 8 本すべて exit 0 (実行済み) | ✅ 一致 |
| Requirement 本文 (誤った実装を与えたときに待機が終わらない形ではなく失敗として表す) | `scripts/release/central-portal.sh:304-308` (状態を取り出せなかった ID を待機対象に残さない) / `:429,433-452` (台本切れを HTTP ステータスで返す) / `:551-552` `:574-575` (照会回数の表明) / `:549` `:567` `:572` `:578` `:584` `:596` `:610` `:616` `:624` `:634` (`cmd_wait_published` を呼ぶ 10 箇所すべてに短い上限) / `wait-for-registries.sh:495,697-699,733-735,746-748,778` | 誤実装注入 16 種 (下記) | ✅ 一致 |
| Scenario: リリース用スクリプトの自己テストが lint job で走る | `ci.yml:328-340` | ci.yml の静的確認 | ✅ 一致 (CI 実行は tasks 6.3 未了) |
| Scenario: 待ちの誤実装が停止ではなく失敗として表れる | 同上 | 16 種いずれも停止せず、うち 10 種が NG として表れた | ✅ 一致 |

## verify-002 の ❌ の再判定 → 解消

> 自己テストは、誤った実装を与えたときに待機が終わらない形ではなく、失敗として表す SHALL。

実物の `scripts/release/central-portal.sh` を写して**1 箇所だけ**書き換え、`--selftest` を上限 100 秒で
実行した。verify-002 が停止を実測した 2 種を含め、**停止したものは 1 件も無い**。

| 与えた誤実装 | verify-002 の結果 | 今回の結果 |
|---|---|---|
| `FAILED` 非終端 | 120 秒でも終わらず停止 | **NG 2 件 / 4 秒** |
| `NOT_FOUND` 非終端 | 60 秒でも終わらず停止 | **NG 1 件 / 5 秒** |
| `PUBLISHED` の枠を `remaining` から外さない | NG 8 件 (解消済み) | NG 8 件 / 5 秒 |
| 枠ごとに待ち切る (直列化) | NG 1 件 | NG 4 件 / 5 秒 |

新たに与えた 12 種でも停止は 0 件 (先頭 ID しか見ない → NG 6 件、`remaining` を毎周リセットしない → NG 8 件、
公開待ちの上限を検証待ちと同じ変数にする → NG 1 件、404 で空を返す → NG 3 件、残り 0 件でなくても成功で
返す → NG 8 件、`PUBLISHING` も公開済みと見なす → NG 8 件。残り 6 種は緑で通過するが停止はしない)。
緑で通過する 6 種のうち 2 種 (上限判定の撤去・根治の `""` 分岐の撤去) は検出力の問題として
review-003.md の Major / Minor で扱う。Requirement 本文が禁じているのは「待機が終わらない形」であり、
その条件は満たされたため、本検証では ✅ とする。

deviation.md との関係: `[自己テストの構造]` の「台本切れが起きうる検査には公開待ちの上限を短く与えた」は、
`cmd_wait_published` を呼ぶ全 10 箇所に上限が与えられたことで記述と実装が一致した。
`[オーナー指示]` の `""` 分岐 (`central-portal.sh:304-308`) は spec が規定しない production の変更として
記録済み → ⚠️ deviation 記録済み。本番経路に到達しないという記述も、`http_request` を常に HTTP 500 に
差し替えた写しで `wait-published` を起動して確認した (`::error::deployment の状態を照会できない (HTTP 500)` /
exit 1 で、`""` 分岐の文言は出ない)。

## 追加検査

| 検査 | 結果 |
|---|---|
| tasks.md の全タスク完了 | ❌ 未完了。1〜5 は完了。**6.1〜6.4 が未チェック**。6.1 (全 `--selftest` が手元で通る) と 6.2 (定数・step 順序を壊すと落ちる) は本検証で実行して通ることを確認済み。6.3 (`develop` への push で lint job) と 6.4 (`dry-run` の release) は未実行 |
| 虚偽チェック | 無し。チェック済みの 1.1〜5.1 はすべて実装・テストに対応が取れた。今回追加された申し送り 1 行 (`env:` 上書きの前提) は実装タスクではない |
| 逆流検査 (足場の書き換え) | 逆流なし。`kasane/changes/backport-registry-wait-hardening/` 一式が未追跡 (`??`) のため git 履歴は無いが、`specs/release-workflow/spec.md` と `proposal.md` の mtime は前回検証時から動いていない (更新されたのは `deviation.md` / `tasks.md` と実装のみ) |
| 未記録乖離 | 無し。verify-002 が挙げた未記録乖離 (上限を与えていない 3 検査) は、上限の追加と deviation の `[オーナー指示]` 追記で解消した |
| 付随修正 | deviation.md の `[付随修正]` 1 件 (`check-time-budget.py` の接続上限検査)。Requirement を持たないため対応表の対象外。diff にあって Scenario に対応しない変更は他に見当たらない |
| UI 変更 | 無し (該当なし) |
| テスト全件成功 | ✅ 上表の実行がすべて exit 0 |

## ❌ の詳細と見立て

### ❌ tasks 6.3 / 6.4 が未実行

- **6.3** `develop` への push で lint job が手元と同じ結果になることの確認
- **6.4** `dry-run` で release を起動し、validate から消費者検証までが通ることの確認

**見立て**: **実装を直す話ではない**。どちらも手元では代替できない検証 (CI 上での実行・workflow の起動) で、
実行するか、実行しないまま進めることを合意して deviation に残すかの二択。なお 6.4 については proposal の
Impact が「publish の待機経路は `dry-run` では到達しない」と明記しており、`dry-run` で確かめられるのは
publish 手前までである点も判断材料になる。

## 判定

**INVALID** — ❌ 1 件 (tasks 6.3 / 6.4 の未実行)。

verify-001 から 2 サイクル継続していた ❌ (Requirement「リリース用スクリプトの自己テスト」本文の箇条書き)
は**解消**し、16 Scenario と各 Requirement 本文はすべて「✅ 一致」または「⚠️ deviation 記録済み /
自動検証不能」になった。仕様と実装の対応としては揃っているが、tasks の検証が 2 件残っているため
アーカイブ可能な状態ではない。あわせて review-003.md が Major 1 件 (上限判定の検出力) を挙げている。
