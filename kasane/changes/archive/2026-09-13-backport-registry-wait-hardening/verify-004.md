# 検証結果: backport-registry-wait-hardening (004 回目)

**日付**: 2026-09-13
**判定**: INVALID (❌ 1 件 — tasks 6.3 / 6.4 が未実行。仕様対応の ❌ は 0 件)

今サイクルで動いたのは `scripts/release/central-portal.sh` の自己テスト 3 検査の追加とコメント 1 ブロックの
書き換え、および `deviation.md` の記述だけ (mtime で確認)。デルタスペックの Requirement / Scenario と実装の
対応はすべて「✅ 一致」または「⚠️ deviation 記録済み / 自動検証不能」で、**実装とデルタスペックの不一致は無い**。
残る ❌ は tasks 6.3 / 6.4 の未実行のみで、これは実装の欠落ではなく検証手順の未了 (push と `dry-run` を伴い、
オーナーの操作が要る)。

## 実行したテスト

| 実行 | 結果 |
|---|---|
| `scripts/release/central-portal.sh --selftest` | exit 0 / 68 件 / NG 0 件 |
| `scripts/release/central-resume.sh --selftest` | exit 0 / NG 0 件 |
| `scripts/release/check-distribution-tag.sh --selftest` | exit 0 / NG 0 件 |
| `scripts/release/check-nuget-version.sh --selftest` | exit 0 / NG 0 件 |
| `scripts/release/check-resume-eligibility.sh --selftest` | exit 0 / NG 0 件 |
| `scripts/release/check-signatures.sh --selftest` | exit 0 / NG 0 件 |
| `scripts/release/compare-maven-artifacts.sh --selftest` | exit 0 / NG 0 件 |
| `scripts/release/wait-for-registries.sh --selftest` | exit 0 / NG 0 件 |
| `python3 scripts/release/check-time-budget.py --selftest` / 本検査 | 15 件 / 失敗なし、exit 0 |
| `python3 scripts/release/check-publish-step-order.py --selftest` / 本検査 | 8 件 / 失敗なし、exit 0 |
| `python3 scripts/comment-policy-lint.py` | exit 0 (禁止 0 件 / 1004 ファイル) |
| `python3 scripts/local-path-lint.py` / `identity-lint.py` | 両方 exit 0 |

自己テストの検査数は 65 件 → **68 件**。増分 3 件は `central-portal.sh:580-581` `:621-622` `:641-642`。

## 対応表

### Requirement: 待ちの時間予算 (今サイクル変更なし)

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Requirement 本文 (publish の予算表 4 項 + 余裕 = 150) | `scripts/release/check-time-budget.py:143-186` / `.github/workflows/release.yml:548-579` | 本検査の出力 (90 / 5 / 1 / 16、余裕 38) | ✅ 一致 |
| Requirement 本文 (反映待ちの予算表 3 項 + 余裕 = 60) | `check-time-budget.py:188-221` / `release.yml:1494-1511` | 本検査の出力 (45 / 2 / 5、余裕 8) | ✅ 一致 |
| Requirement 本文 (定数を検査から読める形で持つ / 読めなければ失敗 / 値が割れても失敗) | `check-time-budget.py:84-98,146-152` | selftest 4 件 | ✅ 一致 |
| Scenario: 公開待ちを延ばすと予算の超過が検出される | `check-time-budget.py:143-186` | selftest (5400→9000 で exit 1) | ✅ 一致 |
| Scenario: 反映待ちの応答上限を延ばすと予算の超過が検出される | `check-time-budget.py:188-221` | selftest (120→900 で exit 1) | ✅ 一致 |
| Scenario: 定数を読み取れない形へ変えると検査が失敗する | `check-time-budget.py:84-98` | selftest 4 件 | ✅ 一致 |
| Scenario: 時間予算の検査が lint job で走る | `.github/workflows/ci.yml:342-347` | ci.yml の静的確認 (`--selftest` → 本検査の順) | ✅ 一致 (CI 実行は tasks 6.3 未了) |

`[付随修正]` の接続上限検査 (`check-time-budget.py:223-253`) は deviation.md 記録済み → ⚠️ deviation 記録済み。

今サイクルで自己テストに追加された 3 検査は `KSR_PUBLISHED_TIMEOUT_SECONDS` を `=5` / `=0` / `=1` の形で
呼び出し行に与えるだけで、`check-time-budget.py:147` の正規表現は `KSR_PUBLISHED_TIMEOUT_SECONDS:-(\d+)` と
既定値の形に限定されているため「値が割れている」に当たらない (本検査 exit 0 で実測)。

### Requirement: 公開レジストリ照会の状態分類 (今サイクル変更なし)

`scripts/release/wait-for-registries.sh` は mtime 09/13 15:12 で review-001 以前から動いていない。
verify-002 / verify-003 で ✅ とした全行を据え置きで再確認し、あわせて**誤実装 7 種を注入して
判別力が健在であることを実測した** (sticky の撤去 / 期限判定の撤去 / 判定不能 3 種の畳み込み /
未照会を未反映に畳む / 判定不能を反映済み扱い / 判定不能で即失敗 / 対象一覧を 1 件落とす —
7 種すべて NG、停止 0 件)。

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Requirement 本文 (照会できたうえで未反映 / 照会できなかったを区別) | `wait-for-registries.sh:78-85,206-296` | selftest の Maven / NuGet 6 入力表 | ✅ 一致 |
| Requirement 本文 (対象 10 件それぞれ独立に保つ) | `wait-for-registries.sh:75,325-360` | selftest (10 件) + 混在の巡回 | ✅ 一致 |
| Requirement 本文 (判定不能を 3 種別に分ける) | `wait-for-registries.sh:83-85,214-296` | selftest の 5xx / 通信失敗 / 解釈不能 | ✅ 一致 |
| Requirement 本文 (いずれも待機を継続する) | `wait-for-registries.sh:362-380,395-405` | selftest の判定不能からの回復 | ✅ 一致 |
| Requirement 本文 (反映済みは再照会しない) | `wait-for-registries.sh:352-361` | selftest (照会回数) | ✅ 一致 |
| Requirement 本文 (未照会と未反映を区別) | `wait-for-registries.sh:82,325-351` | selftest (初期値が未照会) | ✅ 一致 |
| Requirement 本文 (上限到達時に対象ごとの分類を出力) | `wait-for-registries.sh:381-394,433` | selftest の上限超過時の出力 | ✅ 一致 |
| Requirement 本文 (残り時間が正のときだけ照会を開始) | `wait-for-registries.sh:137-158,369` | selftest (固定入力) | ✅ 一致 |
| Scenario: 未反映は待機を続ける | `wait-for-registries.sh:214-296,395-405` | selftest (404 → 未反映で継続) | ✅ 一致 |
| Scenario: 照会できなかった場合も待機を続ける | `wait-for-registries.sh:159-205,214-296` | selftest (通信失敗 / 5xx) | ✅ 一致 |
| Scenario: 判定不能から回復すれば反映済みになる | `wait-for-registries.sh:352-361` | selftest (回復後は再照会しない) | ✅ 一致 |
| Scenario: 対象ごとに状態が分かれて残る | `wait-for-registries.sh:381-394,422` | selftest (10 対象の混在) | ✅ 一致 |
| Scenario: 上限超過時に対象ごとの最後の状態が分かる | `wait-for-registries.sh:433` | selftest (上限超過時の出力) | ✅ 一致 |
| Scenario: 期限の直前に始まった照会が種別を偽らない | `wait-for-registries.sh:147-158,369` | selftest (期限を過ぎていれば 1 件も照会しない) | ✅ 一致 |
| Scenario: 不正な形の応答は判定不能として扱う | `wait-for-registries.sh:234-258` | selftest (文字列でない要素 / null / 数値混在) | ✅ 一致 |

### Requirement: 保留中 deployment の引き継ぎの読み込み順序 (今サイクル変更なし)

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Requirement 本文 (引き継ぎの読み込みが他の成果物の取得より前) | `.github/workflows/release.yml:752-789` → `:791` 以降 | `check-publish-step-order.py` 本検査 | ✅ 一致 |
| Requirement 本文 (読み込んだか / 引き継ぎが存在したかを区別) | `release.yml:771-789` (`handover` / `ready`) + `:1471` + `:1484` | 自動テストなし | ⚠️ 自動検証不能 (publish job の実行時のみ観測可能) |
| Requirement 本文 (順序を機械的に検査できる) | `scripts/release/check-publish-step-order.py:90-160` | selftest 8 件 | ✅ 一致 |
| Scenario: 引き継ぎの読み込みが他の成果物の取得より先に行われる | `release.yml` の step 並び | selftest で担保 (THEN の観測は publish job 実行時のみ) | ✅ 一致 |
| Scenario: 引き継ぎが無い初回の実行と区別できる | `release.yml:777,784,788` (`handover=absent` / `present`) | 自動テストなし | ⚠️ 自動検証不能 |
| Scenario: 順序が崩れると検査が落ちる | `check-publish-step-order.py:109-160` | selftest 7 件が exit 1 | ✅ 一致 |

### Requirement: リリース用スクリプトの自己テスト

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Requirement 本文 (自己テストを持ち、日常の検証 CI で実行される) | `.github/workflows/ci.yml:328-340` (8 本) + `:342-347` + `:349-352` | 8 本すべて exit 0 (実行済み) | ✅ 一致 |
| Requirement 本文 (誤った実装を与えたときに待機が終わらない形ではなく失敗として表す) | `central-portal.sh:304-308` (`""` 分岐) / `:445-451` (台本切れを HTTP ステータスで返す) / `:580-581` `:621-622` `:641-642` (照会回数の表明) / `cmd_wait_published` を呼ぶ自己テスト 10 箇所すべての短い上限 / `wait-for-registries.sh:495,697-699,733-735,746-748,778` | 誤実装注入 18 種 (`central-portal.sh`) + 7 種 (`wait-for-registries.sh`) | ✅ 一致 |
| Scenario: リリース用スクリプトの自己テストが lint job で走る | `ci.yml:328-340` | ci.yml の静的確認 | ✅ 一致 (CI 実行は tasks 6.3 未了) |
| Scenario: 待ちの誤実装が停止ではなく失敗として表れる | 同上 | 25 種いずれも停止せず、うち 19 種が NG として表れた | ✅ 一致 |

**Requirement 本文の再判定**: 今サイクルの 3 検査の追加で、`central-portal.sh` の誤実装 18 種のうち
NG として表れるものが 10 種 → 12 種に増えた (公開待ちの上限判定の撤去 → NG 2 件、`""` 分岐の撤去 → NG 1 件。
どちらも verify-003 / review-003 の時点では緑で通っていた)。**停止 (待機が終わらない) は 0 件**で、
Requirement 本文が禁じる形は今回も現れない。既存の検査が検出力を失った例も 0 件 (発火表で確認。
詳細は review-004.md「判別力の吸収を測った結果」)。

## 追加検査

| 検査 | 結果 |
|---|---|
| tasks.md の全タスク完了 | ❌ 未完了。1〜5 は完了。**6.1〜6.4 が未チェック**。6.1 / 6.2 は本検証で実行して通ることを確認済み (6.2 は `check-time-budget.py` / `check-publish-step-order.py` の自己テストが定数・step を 1 箇所ずつ壊した写しで exit 1 になることを検査している。加えて `central-portal.sh` / `wait-for-registries.sh` に誤実装 25 種を注入して確認)。6.3 / 6.4 は未実行 |
| 虚偽チェック | 無し。チェック済みの 1.1〜5.1 はすべて実装・テストに対応が取れた |
| 逆流検査 (足場の書き換え) | **逆流なし**。`specs/release-workflow/spec.md` (09/13 15:05) と `proposal.md` (15:04) は review-001 (15:47) より前から動いていない。今サイクルで動いたのは `central-portal.sh` (17:01) と `deviation.md` (17:02) のみ (review-003 は 16:48) |
| 未記録乖離 | 無し (下記「Scenario に対応しない変更」参照) |
| 付随修正 | deviation.md の `[付随修正]` 1 件 (`check-time-budget.py` の接続上限検査)。Requirement を持たないため対応表の対象外 |
| UI 変更 | 無し (該当なし) |
| テスト全件成功 | ✅ 上表の実行がすべて exit 0 |

### Scenario に対応しない変更の仕分け

diff にあって Scenario に対応しない変更のうち、deviation.md に記録の無いものを洗い出した。

| 変更 | 判定 |
|---|---|
| `scripts/release/central-portal.sh` の自己テスト 3 検査の追加 | Requirement「リリース用スクリプトの自己テスト」本文 (誤実装を失敗として表す) の実装。乖離ではない |
| `central-portal.sh:560-565` のコメント書き換え | 同 Requirement の説明。挙動に影響しない。乖離ではない |
| `kasane/outbox/KsSettingsView/2026-09-10-*.md` の削除 + `kasane/relations/KsSettingsView.md` の新設 | **乖離ではない**。ksn-core `references/relations.md` の握手手順どおり (受け手の台帳に記録されたら送り手は outbox から片付ける)。相手側 `../KsSettingsView/kasane/relations/KsDialogs.md:5` に当該ファイル名の受領記録があることを確認した。コード変更ではないため Requirement を持たない |

## ❌ の詳細と見立て

### ❌ tasks 6.3 / 6.4 が未実行

- **6.3** `develop` への push で lint job が手元と同じ結果になることの確認
- **6.4** `dry-run` で release を起動し、validate から消費者検証までが通ることの確認

**見立て**: **実装を直す話ではない。** どちらも手元では代替できない検証で、実行するか、実行しないまま
進めることを合意して deviation に残すかの二択。6.3 は `.github/workflows/ci.yml` の lint job に今回
追加した 10 step (`--selftest` 8 本 + python 2 本の自己テストと本検査) が CI 上の環境 (python3 / bash の
版数) でも同じ結果になるかを見るもので、ローカルでの全件緑は前提条件にすぎない。6.4 は proposal の Impact が
「publish の待機経路は `dry-run` では到達しない」と明記しているため、確かめられるのは publish 手前までである。
どちらもオーナーの操作 (push / workflow の dispatch) が要る。

## 判定

**INVALID** — ❌ 1 件 (tasks 6.3 / 6.4 の未実行)。

**実装とデルタスペックの不一致は 0 件**。16 Scenario と 4 Requirement 本文はすべて「✅ 一致」または
「⚠️ deviation 記録済み / 自動検証不能」で、虚偽チェック・逆流・未記録乖離・テスト失敗のいずれも無い。
verify-003 まで議論が続いていた Requirement「リリース用スクリプトの自己テスト」本文の箇条書きは、
今サイクルで検出力の面でも実測上の裏付けが取れた (誤実装 25 種で停止 0 件、うち 19 種が NG)。

INVALID の理由は**ひとえに tasks 6.3 / 6.4 が未実行であること**で、実装の欠落ではない。
この 2 件は push と `dry-run` の起動を伴うためオーナーの操作が要り、エージェント側では完了させられない。
6.3 / 6.4 を実行して通すか、実行しないまま進めることを deviation に合意として残すか、いずれかで
アーカイブ可能な状態になる。

あわせて review-004.md が Minor 1 件 (根治が入る前の挙動を語るコメントが `central-portal.sh:634-637` と
`:445-448` に残存 — review-003 Minor-3 の適用漏れ) を挙げている。こちらは仕様との一致には影響しない。
