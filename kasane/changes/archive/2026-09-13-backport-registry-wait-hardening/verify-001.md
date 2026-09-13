# 検証結果: backport-registry-wait-hardening (001 回目)

**日付**: 2026-09-13
**判定**: INVALID (❌ 1 件)

デルタスペック `specs/release-workflow/spec.md` の 4 Requirement / 16 Scenario を対象に、
実装とテストの対応を突き合わせた。publish job の実行時にしか観測できない Scenario は
「自動検証不能」と明記して扱っている (`dry-run` では publish job が走らない — proposal.md Impact のとおり)。

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
| `python3 scripts/release/check-time-budget.py --selftest` | exit 0 / 15 件 失敗なし |
| `python3 scripts/release/check-time-budget.py` | exit 0 |
| `python3 scripts/release/check-publish-step-order.py --selftest` | exit 0 / 8 件 失敗なし |
| `python3 scripts/release/check-publish-step-order.py` | exit 0 |
| `python3 scripts/comment-policy-lint.py` | exit 0 (禁止 0 件) |
| `python3 scripts/local-path-lint.py` / `identity-lint.py` | 両方 exit 0 |

## 対応表

### Requirement: 待ちの時間予算

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Requirement 本文 (publish の予算表 4 項 + 余裕 = 150) | `scripts/release/check-time-budget.py:52-57,143-186` / `.github/workflows/release.yml:549-567` | `check-time-budget.py --selftest`「現在の定数では両系統とも収まる」+ 本検査の出力 (90/5/1/16、余裕 38) | ✅ 一致 |
| Requirement 本文 (反映待ちの予算表 3 項 + 余裕 = 60) | `check-time-budget.py:60-65,188-221` / `release.yml:1493-1508` | 同上 (45/2/5、余裕 8) | ✅ 一致 |
| Requirement 本文 (定数を検査から読める形で持つ) | `check-time-budget.py:84-98` (`single_value`) | selftest「publish の定数を読み取れなければ落ちる」他 3 件 | ✅ 一致 |
| Scenario: 公開待ちを延ばすと予算の超過が検出される | `check-time-budget.py:143-186` (`check_publish`) | selftest「公開待ちの上限を延ばすと落ちる」(5400→9000 で exit 1) | ✅ 一致 |
| Scenario: 反映待ちの応答上限を延ばすと予算の超過が検出される | `check-time-budget.py:188-221` (`check_registry_wait`) | selftest「反映待ちの応答上限を延ばすと落ちる」(120→900 で exit 1) | ✅ 一致 |
| Scenario: 定数を読み取れない形へ変えると検査が失敗する | `check-time-budget.py:88-98` | selftest 4 件 (綴り変更 / コマンド置換化 / `${{ }}` 化 / 値の二重定義) | ✅ 一致 |
| Scenario: 時間予算の検査が lint job で走る | `.github/workflows/ci.yml:342-347` (`Release time budget check`) | 自動テストなし。ci.yml の静的確認で step の存在と `--selftest` → 本検査の順を確認 | ✅ 一致 (CI 実行は tasks 6.3 未了) |

補足: 接続上限 vs 応答上限の検査 (`check-time-budget.py:223-252`) は spec に無く、
deviation.md に `[付随修正]` として記録済み。selftest 3 件で担保されている → ⚠️ deviation 記録済み。

### Requirement: 公開レジストリ照会の状態分類

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Requirement 本文 (対象は 10 件) | `wait-for-registries.sh:56-73,75` (`MAVEN_ARTIFACT_IDS` 7 + `NUGET_PACKAGE_IDS` 3 = `TOTAL_TARGETS`) | selftest「待ち対象は Maven 7 件 + NuGet 3 件の 10 件」「対象の配列も 10 件」 | ✅ 一致 |
| Requirement 本文 (判定不能を 3 種別に分ける) | `wait-for-registries.sh:78-85,214-229,259-291` | selftest Maven 側 5 入力 / nuget 側 9 入力の分類表 | ✅ 一致 |
| Requirement 本文 (反映済みは sticky) | `wait-for-registries.sh:363-366` (`poll_once` の skip) | selftest「反映済みの対象は再照会しない」(照会 13 件) | ✅ 一致 |
| Requirement 本文 (未照会を未反映と区別) | `wait-for-registries.sh:82,344-348` (`STATE_UNPROBED` 初期値) | selftest「初期値は未照会」「照会していない対象を未反映として出さない」 | ✅ 一致 |
| Requirement 本文 (残り時間で応答上限を切り詰めない) | `wait-for-registries.sh:117-129` (`request_max_time`) | selftest「期限を過ぎていても / 目前でも応答上限は変わらない」「上限に達する実行でも照会は応答上限いっぱいを使う」 | ✅ 一致 |
| Scenario: 未反映は待機を続ける | `wait-for-registries.sh:214-229,259-291` (404→`STATE_PENDING`) + `:395-403` (`has_unreflected`) | selftest「404 は未反映」(Maven / nuget)、巡回検査で待機継続 | ✅ 一致 |
| Scenario: 照会できなかった場合も待機を続ける | `wait-for-registries.sh:214-229,259-291` + `poll_once` は失敗で抜けない | selftest「5xx は判定不能」「通信の失敗は判定不能」+ 混在巡回で待機継続 | ✅ 一致 |
| Scenario: 判定不能から回復すれば反映済みになる | `wait-for-registries.sh:363-371` (再照会して分類を更新) | selftest「判定不能から回復すれば成功する」「回復後は再照会しない (照会 11 件)」 | ✅ 一致 |
| Scenario: 対象ごとに状態が分かれて残る | `wait-for-registries.sh:381-393` (`print_states`) + `:422-423` | selftest 混在巡回の 6 件 (反映済み Maven / 未反映 Maven / 503 の KMP ターゲット / 解釈不能 / 反映済み Package ID / 未反映 Package ID / 通信失敗) | ✅ 一致 |
| Scenario: 上限超過時に対象ごとの最後の状態が分かる | `wait-for-registries.sh:432-435` | selftest「上限に達すれば失敗する」「上限超過の出力に対象ごとの最後の分類が付く」 | ✅ 一致 |
| Scenario: 期限の直前に始まった照会が種別を偽らない | `wait-for-registries.sh:117-129` (応答上限は定数) / `:145-149` (`can_start_probe`) / `:367-371` (対象ごとの再判定) / `:430-435` (巡回の入口) | selftest「[期限の判定]」7 件、「[照会 1 回の応答上限]」2 件、「期限を過ぎていれば 1 件も照会しない」「期限に達した時点で残りの対象を照会しない」「期限後に巡回を始めない」 | ✅ 一致 |
| Scenario: 不正な形の応答は判定不能として扱う | `wait-for-registries.sh:232-254` (`isinstance(v, str)` の全件検査) | selftest「version の一覧に null を含む」「文字列と数値が混在する一覧」「要素が object」の 3 件 | ✅ 一致 |

### Requirement: 保留中 deployment の引き継ぎの読み込み順序

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Requirement 本文 (引き継ぎの読み込みが他の成果物の取得より前) | `.github/workflows/release.yml:752-793` (`Download previous deployment ids` → `Prepare deployment id files`) が `:794` 以降の download より前 | `check-publish-step-order.py --selftest`「実物の workflow は検査を通る」+ 本検査の出力 | ✅ 一致 |
| Requirement 本文 (読み込んだか / 引き継ぎが存在したかを区別) | `release.yml:771-793` (`handover` / `handover-<slot>` / `ready`) | 自動テストなし | ⚠️ 自動検証不能 (publish job の実行時のみ観測可能) |
| Requirement 本文 (順序を機械的に検査できる) | `scripts/release/check-publish-step-order.py:90-160` | selftest 8 件 | ✅ 一致 |
| Scenario: 引き継ぎの読み込みが他の成果物の取得より先に行われる | `release.yml` の step 並び (上記) | 順序そのものは selftest で担保。THEN の「読み込んだ結果が出力に残っている」は publish job の実行ログでのみ観測可能 | ✅ 一致 (THEN の観測は自動検証不能) |
| Scenario: 引き継ぎが無い初回の実行と区別できる | `release.yml:777-793` (`handover=absent` を出力、読み込み失敗時は step ごと失敗) | 自動テストなし | ⚠️ 自動検証不能 (publish job の実行時のみ観測可能) |
| Scenario: 順序が崩れると検査が落ちる | `check-publish-step-order.py:109-160` | selftest「読み込みを download より前へ」「読み込みを他の成果物の取得より後ろへ」「download を他の成果物の取得より後ろへ」「download を消す」「読み込みを消す」「他の成果物の取得が無くなる」「続行可否の印を他の成果物に数えない」の 7 件が exit 1 | ✅ 一致 |

### Requirement: リリース用スクリプトの自己テスト

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Requirement 本文 (自己テストを持ち、日常の検証 CI で実行される) | `.github/workflows/ci.yml:328-340` (8 本) + `:342-347` + `:349-354` | 8 本すべてが手元で exit 0 (実行済み) | ✅ 一致 |
| Requirement 本文 (誤った実装を与えたときに待機が終わらない形ではなく失敗として表す) | `scripts/release/central-portal.sh:422-424,438-448` (台本切れを HTTP ステータスで返す) / `:620` (上限 1 秒) / `wait-for-registries.sh:493-501,676-679` | `central-portal.sh`「台本が尽きても待機が終わらなくなるのではなく失敗する」/ `wait-for-registries.sh`「上限超過の検査で台本を使い切らない」他 | ❌ 乖離 (下記) |
| Scenario: リリース用スクリプトの自己テストが lint job で走る | `ci.yml:328-340` | 自動テストなし。ci.yml の静的確認 | ✅ 一致 (CI 実行は tasks 6.3 未了) |
| Scenario: 待ちの誤実装が停止ではなく失敗として表れる | 同上 | 誤実装「`pending=("$1")` (対象の一部しか見ない)」を与えて実測 → 4.1 秒で NG 6 件 (停止せず失敗)。`wait-for-registries.sh` も sticky 撤去 / 1 件のみ照会の 2 誤実装で 12.4 秒 NG 6 件 / 7.7 秒 NG 11 件 | ✅ 一致 (Scenario の GIVEN の範囲では成立) |

## ❌ の詳細と見立て

### ❌ Requirement「リリース用スクリプトの自己テスト」本文の箇条書き

> 自己テストは、誤った実装を与えたときに待機が終わらない形ではなく、失敗として表す SHALL。

**乖離**: `scripts/release/central-portal.sh` の `cmd_wait_published` を呼ぶ検査のうち、成功を期待する
4 件 (`:555` `:570` `:582` `:610`) が `KSR_PUBLISHED_TIMEOUT_SECONDS` を与えておらず、既定 90 分のまま。
`cmd_wait_published` は台本切れの応答 (`000`) を `*)` 分岐で「未知の状態」として pending に残すため、
余分に照会する誤実装を与えると `KSR_POLL_INTERVAL_SECONDS=0` で 90 分ぶん空回りする。

**実測**: `PUBLISHED` の枠を `remaining` から外さない誤実装を与えたところ、自己テストは
120 秒経過しても `[wait-published]` の最初の検査で停止したまま終わらなかった (NG 0 件)。
同じ誤実装に対して 4 件へ短い上限を与えた写しでは 15.0 秒で NG 8 件となり、正しい実装に同じ写しを
当てても 4.3 秒 / 失敗なしで緑のままだった。

**deviation.md との関係**: deviation.md は対策として「台本切れが起きうる検査には公開待ちの上限を
短く与えた」と記録しているが、上限が与えられているのは本 change で新設した検査 (`:620`) だけで、
既存 4 件への適用は記録にも実装にも無い。よって**未記録の乖離**と判定する。

**見立て**: **実装を直す**のが妥当。修正は 4 行への環境変数の追加で済み、上の実測どおり正しい実装では
緑のまま・誤実装では失敗に変わることが確認できている。deviation として合意する選択肢は薄い —
本 change が新設した Requirement そのものであり、狙う誤実装の型 (「公開済みの枠を外さない」) は
本 change が追加した検査のコメント自身が名指ししている。

## 追加検査

| 検査 | 結果 |
|---|---|
| tasks.md の全タスク完了 | ❌ 未完了。1〜5 は完了、**6.1〜6.4 が未チェック**。6.1 (全 `--selftest` が手元で通る) と 6.2 (定数・step 順序を壊すと落ちる) は本検証で実行して通ることを確認した。6.3 (`develop` への push で lint job) と 6.4 (`dry-run` の release) は未了 |
| 虚偽チェック | 無し。チェック済みの 1.1〜5.1 はすべて実装・テストに対応が取れた |
| 逆流検査 (足場の書き換え) | 判定不能。`kasane/changes/backport-registry-wait-hardening/` 一式が未追跡 (`??`) のため git 履歴が無く、実装期間中の書き換えの有無を機械的に確認できない。spec / proposal / tasks の内容は実装と整合しており、後追いで spec を実装に合わせた形跡 (実装にしか無い要素が spec に書かれている等) は見当たらない |
| 未記録乖離 | 上記 ❌ 1 件 |
| 付随修正 | deviation.md の `[付随修正]` 1 件 (`check-time-budget.py` の接続上限検査)。Requirement を持たないため対応表の対象外。diff にあって Scenario に対応しない変更は他に見当たらない |
| UI 変更 | 無し (該当なし) |
| テスト全件成功 | ✅ 上表の 14 実行がすべて exit 0 |

## 判定

**INVALID** — ❌ 1 件 (Requirement「リリース用スクリプトの自己テスト」本文の箇条書き、未記録の乖離)。
16 Scenario はすべて ✅ または自動検証不能で、Scenario 単位の欠落は無い。
加えて tasks 6.3 / 6.4 が未了のため、アーカイブ可能な状態ではない。
