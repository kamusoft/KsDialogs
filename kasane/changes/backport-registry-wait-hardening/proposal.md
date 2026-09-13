# Proposal: backport-registry-wait-hardening

## Why

sibling の KsSettingsView が、こちらの初回リリースで見つかった不具合と改良の知らせ
(`kasane/outbox/KsSettingsView/2026-09-10-release-workflow-fixes-and-improvements.md`、送信済み) を受けて
publish 機構を改修し、`0.1.0-beta.3` のリリースまで通したうえで結果を返してきた
(`../KsSettingsView/kasane/outbox/KsDialogs/2026-09-12-release-mechanism-hardening-and-notes-rework.md`)。
向こうの実装は `../KsSettingsView/kasane/changes/archive/2026-09-12-backport-release-workflow-hardening/`。

現物を確認した結果、こちらの publish 周辺に 4 つの弱点が残っている。

- **公開レジストリの照会が障害と未反映を区別できない** — `scripts/release/wait-for-registries.sh` は
  Maven の HTTP status を 200 か否かで、NuGet を本文が空か否かで判定し、通信の失敗・5xx・応答の解釈不能を
  すべて「未反映」へ畳み込む。上限 45 分まで待って失敗したとき、レジストリが遅いのか壊れているのかが出力から分からない
- **待ちの時間予算が機械検査の外にある** — 待ちの上限はスクリプト側の定数、job の打ち切りは workflow 側の
  `timeout-minutes` という二重管理で、片方だけ延ばしても平時は緑のまま進む。向こうでは実際にこの関係が実装中に破れ、
  レビューが Major として検出した
- **publish の step 順序に検査が無い** — 引き継ぎ deployment ID の読み込みが、失敗しうる成果物の取得より
  後ろへ動いても気づけない
- **`scripts/release/` 9 本の `--selftest` が CI から一度も呼ばれていない** — リリース経路の判定ロジックは
  リリースのときにしか本番を通らないため、退行を見張るのは自己テストだけ。それが検証 CI に載っていない
  (簡易起票 add-release-script-selftests-to-lint の項目 1・4 と同じ課題。本 change に吸収する)

## What Changes

影響する能力: **release-workflow** (単一)。いずれも publish と反映待ちの内部で、配布物と利用者向けドキュメントには触れない。

1. **反映待ちに状態分類を入れる** — 待ち対象 10 件 (Maven Central の Android 2 座標 + KMP 5 publication、
   nuget.org の 3 Package ID) それぞれについて、未照会・反映済み・未反映・判定不能を区別し、
   判定不能は失敗の種別 (通信の失敗 / 応答が成功を示さない / 応答を解釈できない) まで残す
2. **待ちの時間予算を機械検査に載せる** — publish job と wait-for-registries job の 2 つについて、
   スクリプト側の定数と workflow 側の `timeout-minutes` を突き合わせる検査を新設する
3. **publish の step 順序を機械検査に載せる** — 引き継ぎの読み込みが他の成果物の取得より前にあることを検査する
4. **`scripts/release/` の自己テストを lint job へ接続する** — 8 本 (`set-readme-version.py` を除く) を
   検証 CI で走らせる。あわせて `central-portal.sh` の複数 ID 待ちの自己テストが、誤実装を hang ではなく
   NG で表すようにする

### 時間予算の方針

publish の実行時間の上限は **公開待ちと本体処理を収容し、検証の決着待ちは予算外とする**。
現行 `timeout-minutes: 150` のコメントが宣言している判断 (「検証待ちが枠ごとに上限まで伸びる最悪ケースは
この値を超えるが、その場合は job timeout で止めて再実行に回す」) を維持する。

翻案元は最悪ケース全体を収容する方針だが、こちらは Maven Central の枠が 2 つあるため検証待ちが
枠ごとに 2 回 (引き継ぎ経路と upload 後) 起きうる。全項を収容すると合計が約 324 分となり、
GitHub Actions の job 実行時間の上限 360 分に対して余裕が残らない。検証は実測で枠ごとに数秒〜数分で決着し、
deployment ID は upload の直後に artifact へ保存されて次の試行が引き継ぐため、
超過時は打ち切って再実行に回す方が安全側に倒れる。

## Non-Goals

- **インストール例の version 表記・GitHub Release の prerelease 印・Release ノートの分類**:
  `kasane/changes/install-examples-and-release-notes/` へ分離した。利用者から見える契約に関わり、
  ADR 2 本の起票を伴う別の変更級 (L) になるため
- **`actionlint` の CI 搭載と monorepo tag 照合のスクリプト化**:
  簡易起票 `kasane/changes/add-release-script-selftests-to-lint/` に残した。
  自己テストの接続とは別テーマ (前者は新しい道具の導入、後者は workflow の step のスクリプト化) で、
  それぞれ独立した判断を要する
- **公開待ちの上限の分離 (`KSR_PUBLISHED_TIMEOUT_SECONDS`)**: change fix-release-published-wait で実装済み
- **引き継ぎ判定のスクリプト切り出し (向こうの `deployment-handover.sh` 相当)**:
  こちらは `central-resume.sh` (引き継ぎの分類) と `check-resume-eligibility.sh` (続行可否と印の読み書き) を
  既に持ち、判定は workflow のインラインに残っていない。本 change で満たすべきは読み込みの順序の要件だけで、
  新しいスクリプトを足す理由がない

## Impact

- 破壊的変更: なし。公開 API・配布物・利用者向けドキュメントのいずれも変わらない
- 影響範囲: `.github/workflows/release.yml` (publish job の step 順序と予算コメント)、
  `.github/workflows/ci.yml` (lint job に自己テストと検査の step)、
  `scripts/release/wait-for-registries.sh` (状態分類への大改修)、
  `scripts/release/central-portal.sh` (自己テストの逆対照)、
  新規 `scripts/release/check-time-budget.py` / `scripts/release/check-publish-step-order.py`
- リスク: publish の待機経路は `dry-run` では到達しない (publish job が走らないため)。
  自己テストで判定ロジックを検査し、経路全体は次回の本番リリースで確かめる
- 既存 ADR との関係: [cross/ADR-0022](../../decisions/cross/0022-lint-job-includes-readme-example-lint.md)
  (検証 CI の lint job を 8 検査に固定) に検査が増えるため、amends を 1 本起票する

## 級: M

publish の待機と順序という不可逆操作の周辺に触れ、時間予算の再算出と lint 検査の集合の改訂 (ADR) を伴うため。
公開 API・UI・利用者向け契約の変更は無い。

domain: cross
