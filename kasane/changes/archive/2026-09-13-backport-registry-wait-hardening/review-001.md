# レビュー結果: backport-registry-wait-hardening (001 回目)

**日付**: 2026-09-13
**判定**: CHANGES_REQUESTED

## サマリー

待ち対象 10 件の状態分類・時間予算の機械検査・publish の step 順序検査・自己テストの CI 接続は、
いずれも KsDialogs 固有の構造 (Maven Central 2 枠 / KMP 5 publication / nuget.org 3 Package ID) に
正しく対応しており、予算の式も spec の表と数値まで一致する (実行して確認)。検査群は負の入力で
落ちることまで自己テストで担保されており、判別力のある作りになっている。

一方で、本 change が新設した Requirement「リリース用スクリプトの自己テスト」の
「誤った実装を与えたときに待機が終わらない形ではなく、失敗として表す」は、`central-portal.sh` の
自己テストで満たし切れていない。deviation.md で採った対策 (台本切れを応答として返す + 上限を短く与える)
のうち後半が、新設した 1 検査にしか適用されておらず、同じ危険を持つ既存の検査 4 件が既定 90 分の
公開待ちのまま残っている。実際に誤実装を与えて停止することを実測で確認した (Major-1)。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — ソースコメント規約
- `kasane/handbook/cross/verification-ci.md` (`.github/workflows/` を変えるとき) — 検証 CI の範囲と lint job の検査
- `kasane/handbook/cross/ci-script-deletion.md` (`scripts/**` `.github/workflows/**` を作る・翻案するとき) — 削除の扱い
- `kasane/handbook/cross/release-procedure.md` (release workflow を触るとき) — リリース手順
- `kasane/decisions/cross/0022` / `0020` / `0021` / `0017` / `0024` (lint job の検査の集合と系譜)
- `kasane/lessons/code-review.md` L-001 (受け入れ条件の判別力) — 本レビューでは指摘・推奨の直後に
  「機構が無いと何が変わるか」を実測で添えた

機械検査は全件緑を確認した: `scripts/comment-policy-lint.py` (禁止 0 件 / advisory なし)、
`scripts/local-path-lint.py` (exit 0)、`scripts/identity-lint.py` (exit 0)。
ci-script-deletion については、新規スクリプトの削除は `tempfile.TemporaryDirectory` と
自己テスト作業ディレクトリの `rm -rf` のみで、引数の誤指定で回復困難な消失が起きる構造ではない。

## ビルド・テストの実行結果

| 実行 | 結果 |
|---|---|
| `scripts/release/*.sh --selftest` 8 本 | 全件 exit 0 (合計 12.5 秒。ADR-0026 の「13 秒台」と整合) |
| `python3 scripts/release/check-time-budget.py --selftest` | 15 件 / 失敗なし |
| `python3 scripts/release/check-time-budget.py` | exit 0 (publish 112/150、wait-for-registries 52/60) |
| `python3 scripts/release/check-publish-step-order.py --selftest` | 8 件 / 失敗なし |
| `python3 scripts/release/check-publish-step-order.py` | exit 0 |

予算の出力は spec の表と一致する (公開待ち 90 / 最後の照会 5 / 巡回間隔の端数 1 / 本体処理 16、
余裕 38、上限 150。待機の上限 45 / 期限を跨げる照会 2 / job の前後 5、余裕 8、上限 60)。

## 指摘事項

### [🟠 Major] 自己テストが停止する誤実装が残っている (対策が新設の 1 検査にしか掛かっていない)

**該当箇所**: `scripts/release/central-portal.sh:555` / `:570` / `:582` / `:610`
(成功を期待する `cmd_wait_published` の検査のうち `KSR_PUBLISHED_TIMEOUT_SECONDS` を与えていないもの)

**問題点**:

deviation.md は、台本切れを「戻り値 1」から「応答が返らなかったことを示す HTTP ステータス」へ
変えたうえで、**台本切れが起きうる検査には公開待ちの上限を短く与えた**と記録している。
後者は本 change で新設した検査 (`:620`、`KSR_PUBLISHED_TIMEOUT_SECONDS=1`) にしか適用されていない。

`cmd_wait_published` は、台本切れの応答 (`000`) を `*)` 分岐で「未知の状態」として扱い、その ID を
pending に残し続ける。したがって**余分に照会する誤実装**を与えると、上限を与えていない検査は
既定の 90 分 (`KSR_PUBLISHED_TIMEOUT_SECONDS:-5400`) を `KSR_POLL_INTERVAL_SECONDS=0` で
空回りする。これは lint job の `timeout-minutes: 10` に当たって job ごと打ち切られ、
どの検査が落ちたかの出力を残さない — Requirement が禁じている「待機が終わらない形」そのものになる。

実測 (本レビューで確認):

| 与えた誤実装 | 結果 |
|---|---|
| `pending=("$1")` (対象の一部しか見ない) | 4.1 秒で NG 6 件 — Scenario の GIVEN どおり失敗する |
| `PUBLISHED` の枠を `remaining` から外さない | **120 秒でも終わらず `[wait-published]` の最初の検査で停止** |

後者は偶然の誤実装ではない。本 change が新設した検査のコメント自身が
「余分まで使い切る実装 (**公開済みの枠を外さない**・枠ごとに待ち切る)」を狙う対象として名指ししており
(`scripts/release/central-portal.sh:605`)、その誤実装を与えたときに先に停止するのは、
同じ危険を持つ**既存**の検査の側である。

なお `wait-for-registries.sh` の自己テストは同じ危険に対して正しく守られている
(巡回を回す 3 検査すべてが `KSR_POLL_TIMEOUT_SECONDS=5`)。sticky を外す誤実装で 12.4 秒 NG 6 件、
1 件しか照会しない誤実装で 7.7 秒 NG 11 件と、いずれも停止せず失敗した。
`cmd_wait_validated` は決着しない状態でループしない構造のため影響を受けない。

**推奨修正**:

`cmd_wait_published` を呼ぶ検査のうち、成功を期待する 4 件 (`:555` `:570` `:582` `:610`) にも
短い `KSR_PUBLISHED_TIMEOUT_SECONDS` を与える。効果は実測で確認済み:

| 写し | 結果 |
|---|---|
| 正しい実装 + 4 件に `KSR_PUBLISHED_TIMEOUT_SECONDS=3` | 4.3 秒 / 失敗なし (緑のまま) |
| 「公開済みの枠を外さない」誤実装 + 同上 | 15.0 秒で NG 8 件 (停止せず失敗) |

あわせて、`wait-for-registries.sh` の巡回検査が持つ「巡回を回す検査には必ず短い上限を与える」
という趣旨のコメントと同等の注意を `central-portal.sh` の `[wait-published]` 節にも置き、
以後の検査追加で同じ穴が空かないようにすることを推奨する。

---

### [🟡 Minor] `set-readme-version.py` を除外する理由が事実と食い違っている

**該当箇所**: `.github/workflows/ci.yml:327` (`Release script selftests` の直前のコメント)

**問題点**: コメントは「`set-readme-version.py` は自己テストを持たない。」と書いているが、
同スクリプトは `--selftest` を持つ (`scripts/release/set-readme-version.py:36` の usage、
`:441` の `selftest()`)。除外の本当の理由は tasks 4.1 と cross/ADR-0026 の「対象外」行が書いている
「後続の変更で撤去するため」であり、コメントだけがそれと違う理由を述べている。
コメントはそのファイルだけを読む人にとって正である必要がある (handbook `comment-policy.md`) ため、
将来の読み手が「自己テストが無いスクリプト」と誤認したまま lint への接続を検討しなくなる。

**推奨修正**: コメントを ADR-0026 と同じ理由に直す
(例: 「`set-readme-version.py` は後続の変更で撤去するため接続しない」)。

---

### [🟡 Minor] `handover` 系の step output に使用箇所が無い

**該当箇所**: `.github/workflows/release.yml:771-793` (`Prepare deployment id files` の
`handover` / `handover-android` / `handover-kmp`)

**問題点**: 3 つの output はどこからも参照されていない
(`steps.deployment-ids.outputs` の参照は `ready` の 3 箇所のみ)。
GITHUB_OUTPUT に書いた値は job のログには現れないため、spec が要求する
「引き継ぎが存在したかどうかが実行の出力から区別できる」を実際に満たしているのは
すぐ上の `echo "${slot} 枠の引き継ぎ: '${deployment_id}'"` と
`echo "引き継ぎの有無: ${handover}"` の側であり、output は消費者のいない拡張点になっている
(ksn-core「スコープが要求しない拡張ポイント」)。枠ごとの 2 つはとくに粒度が spec より細かい。

**推奨修正**: 枠ごとの `handover-android` / `handover-kmp` は落とし、
集約の `handover` も消費する step が無いなら output ではなくログ出力だけに寄せる。
残すなら「どの step が読むか」を決めてから残す。

---

### [🔵 Suggestion] 新規 2 スクリプトの実行権限が揃っていない

**該当箇所**: `scripts/release/check-publish-step-order.py` (644) /
`scripts/release/check-time-budget.py` (755)

**問題点**: どちらも CI からは `python3 <path>` で起動するため動作に影響しないが、
同ディレクトリの既存 `set-readme-version.py` は 755 で、2 本の間でも揃っていない。

**推奨修正**: 755 に揃える (shebang を持つため既存の慣習に合う)。

---

### [🔵 Suggestion] 予算の検査は `env:` による上限の上書きを見ていない

**該当箇所**: `scripts/release/check-time-budget.py:84-98` (`single_value` によるスクリプト定数の読み取り)

**問題点**: 検査はスクリプト側の**既定値リテラル**だけを読む。現状 `release.yml` に `KSR_*` の
`env:` 指定は無いので予算は成立している (確認済み) が、将来 workflow 側で
`KSR_POLL_TIMEOUT_SECONDS` や `KSR_PUBLISHED_TIMEOUT_SECONDS` を上書きすると、検査は既定値を読んだまま
緑で通る。さらに `KSR_POLL_TIMEOUT_SECONDS` は `central-portal.sh` の検証待ち (既定 1800) と
`wait-for-registries.sh` の反映待ち (既定 2700) で名前が衝突しているため、job 単位の `env:` が
意図せず両方に効く余地もある。

**推奨修正**: `release.yml` に `KSR_` で始まる `env:` が現れたら検査を失敗させる (fail-closed) か、
現れた場合はその値を予算に使う。いずれも本 change のスコープ外と判断するなら、
蒸留の申し送りに「予算の検査が読むのは既定値リテラルのみ」という前提として残す。

---

### [🔵 Suggestion] 蒸留の申し送りに `handbook/cross/verification-ci.md` が漏れている

**該当箇所**: `kasane/changes/backport-registry-wait-hardening/tasks.md` の「蒸留への申し送り」

**問題点**: `kasane/handbook/cross/verification-ci.md` は frontmatter の description と本文 2 箇所で
lint job を「8 検査」と書いており、その一覧表も持つ。本 change で 11 検査になるため追随が要るが、
申し送りには concepts の release-workflow.md と handbook の release-procedure.md しか挙がっていない。

**推奨修正**: 申し送りに `kasane/handbook/cross/verification-ci.md` (lint の検査数と一覧表、
description の「cross/ADR-0022 (lint の 8 検査)」の参照) を足す。

## 確認して問題が無かった観点

- **待ち対象 10 件**: `MAVEN_ARTIFACT_IDS` 7 件 (Android 2 + KMP root/android/iosarm64/iossimulatorarm64/iosx64)
  + `NUGET_PACKAGE_IDS` 3 件。`TOTAL_TARGETS` を自己テストが 10 で固定し、配列長も別途検査している
- **Maven Central 2 枠との整合**: 予算は公開待ちを「2 枠まとめて 1 本」で 1 回だけ数え、
  `PUBLISH_WAITS = 1` と `central-portal.sh` の複数 ID 対応 (`cmd_wait_published aaa bbb`) が対応している
- **時間予算の式**: spec の 2 つの表と出力が全項一致。定数の読み取りは 0 件 = 「読み取れない」、
  複数値 = 「値が割れている」で fail-closed。自己テストが 4 種の退化 (綴り変更・コマンド置換化・
  `${{ }}` 化・値の二重定義) で落ちることを確かめている
- **`timeout-minutes` を変えない判断**: publish 150 / wait-for-registries 60 のまま。検証の決着待ちを
  予算外とする理由が release.yml のコメント・`check-time-budget.py` の docstring・ADR-0026 の 3 箇所で
  同じ内容になっている
- **応答上限を残り時間で切り詰めていないこと**: `request_max_time` は定数を返すだけで、期限は
  `can_start_probe` 側だけに掛かる。自己テストは「期限を過ぎていても / 目前でも応答上限は変わらない」と
  「上限に達する実行でも照会は応答上限いっぱいを使う」(記録した全照会の `max-time` を照合) の両方で見ている
- **不正な形の応答**: `versions` に `null` / 数値 / object が混ざる 3 パターンと、JSON でない本文・
  最上位が配列・`versions` が文字列の計 6 入力がすべて `unknown-parse` になることを確認
- **`Download eligibility marker` を除外する判断**: 引き継ぎ download の `if` は
  `steps.state.outputs.publish-needed` に依存し、その `steps.state` は印の download を前提にするため、
  引き継ぎ download は構造として印より前へ動かせない。かつ失敗経路の書き戻し 3 step はすべて
  `steps.deployment-ids.outputs.ready == 'true'` で守られており、印の download が失敗しても
  引き継ぎを空で消す経路には入らない。除外は妥当。自己テストの
  「続行可否の印の download は他の成果物に数えない」が、除外を外すと検査が落ちることまで確かめている
  (除外が飾りでないことの判別力がある)
- **ADR-0026 の数え方**: lint job の実検査は 8 → 11 (`ci.yml` の step を数えて一致)。
  8 本の自己テストを 1 検査と数える根拠 (cross/ADR-0020 の SwiftPM 同期スクリプトの前例) が
  Alternatives に明記され、0017 → 0020(6) → 0021(7) → 0022(8) → 0026(11) の系譜と
  `amends` / `amended-by` / index 3 箇所の更新が揃っている。status は proposed
- **deviation.md の `[付随修正]`**: 接続上限 vs 応答上限の検査は本務で触るファイル内・局所的・
  自己テスト 3 件で担保されており、同梱条件に収まっている

## アクションプラン

1. **Major-1**: `central-portal.sh` の成功期待の `cmd_wait_published` 検査 4 件に短い
   `KSR_PUBLISHED_TIMEOUT_SECONDS` を与え、`[wait-published]` 節に趣旨のコメントを添える
2. **Minor-1**: `ci.yml` の `set-readme-version.py` 除外理由のコメントを ADR-0026 と揃える
3. **Minor-2**: `handover-android` / `handover-kmp` を落とし、`handover` の扱いを決める
4. **Suggestion 3 件**: 実行権限を揃える / `env:` 上書きの扱いを決めるか申し送りへ /
   申し送りに `handbook/cross/verification-ci.md` を足す
5. tasks 6.3 (`develop` への push で lint job を確認) と 6.4 (`dry-run` の release) は未了。
   6.1 と 6.2 は本レビューで実行して通ることを確認した
