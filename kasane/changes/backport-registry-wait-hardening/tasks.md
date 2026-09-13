# Tasks: backport-registry-wait-hardening

## 1. 公開レジストリ照会の状態分類

- [x] 1.1 `scripts/release/wait-for-registries.sh` のネットワーク関数を 1 本 (`http_get`) に集約し、
      HTTP のステータスコードと本文を分けて返す形にする (→ Requirement: 公開レジストリ照会の状態分類)
- [x] 1.2 状態の定数 (未照会 / 反映済み / 未反映 / 判定不能 3 種別) と、判定不能の詳細を保持する仕組みを入れる
      (→ Requirement: 公開レジストリ照会の状態分類)
- [x] 1.3 Maven の照会を分類器にする: 非ステータス → 解釈不能、通信失敗 → 通信の失敗、200 → 反映済み、
      404 → 未反映、その他 → 応答が成功を示さない (→ Scenario: 照会できなかった場合も待機を続ける)
- [x] 1.4 NuGet の照会を分類器にする。200 のときの index の解釈で、JSON でない / dict でない /
      `versions` が list でない / 要素に文字列でないものが混ざる、のいずれかを解釈不能にする
      (→ Scenario: 不正な形の応答は判定不能として扱う)
- [x] 1.5 待ち対象 10 件の分類を並びの揃った配列で保持し、反映済みは以後再照会しない (sticky)
      (→ Scenario: 判定不能から回復すれば反映済みになる)
- [x] 1.6 巡回ごとに 10 件それぞれの分類を出力し、上限到達時は対象ごとの最後の分類と種別を失敗の出力に含める
      (→ Scenario: 対象ごとに状態が分かれて残る / Scenario: 上限超過時に対象ごとの最後の状態が分かる)
- [x] 1.7 照会は残り時間が正のときだけ開始する。対象 1 件ごとに残り時間を再判定し、巡回の末尾では
      次の巡回に入る前に期限を判定する。応答上限の切り詰めは行わない
      (→ Scenario: 期限の直前に始まった照会が種別を偽らない)
- [x] 1.8 自己テストを状態分類に合わせて作り直す: Maven と NuGet のそれぞれに 6 入力 (200 かつ版あり /
      200 かつ版なし / 404 / 5xx / 通信の失敗 / 解釈不能) の表、10 対象の混在、判定不能からの回復、
      sticky 性 (照会回数)、上限超過時の対象別出力、期限判定の固定入力
      (→ Requirement: 公開レジストリ照会の状態分類)
- [x] 1.9 自己テストのモックが台本切れのときに待機を続けず、検査として失敗する形にする
      (→ Scenario: 待ちの誤実装が停止ではなく失敗として表れる)

## 2. 待ちの時間予算

- [x] 2.1 `scripts/release/check-time-budget.py` を新設し、publish job の予算を検査する。
      `central-portal.sh` から公開待ちの上限・巡回間隔・照会の応答上限を、`release.yml` から publish の
      `timeout-minutes` を読み、公開待ち + 最後の照会 + 巡回間隔の端数 + 本体処理 ≤ 上限 を検査する
      (→ Requirement: 待ちの時間予算)
- [x] 2.2 同じ検査に反映待ち job の予算を足す。`wait-for-registries.sh` から待機の上限と応答上限を、
      `release.yml` から `wait-for-registries` の `timeout-minutes` を読む
      (→ Scenario: 反映待ちの応答上限を延ばすと予算の超過が検出される)
- [x] 2.3 定数の読み取りを fail-closed にする。0 件なら「読み取れない」、複数に割れていれば「値が割れている」で失敗する
      (→ Scenario: 定数を読み取れない形へ変えると検査が失敗する)
- [x] 2.4 `release.yml` の publish と `wait-for-registries` の `timeout-minutes` の脇に、
      予算の内訳表と「突き合わせは検査が行う」旨のコメントを書く。検証待ちを予算外とする理由も残す
      (→ Requirement: 待ちの時間予算)
- [x] 2.5 `check-time-budget.py` に自己テストを足す。実物 3 ファイルを土台に 1 箇所だけ書き換えた写しで
      終了コードを検査し、置換対象が無ければ「土台が変わっている」として自己申告する
      (→ Requirement: 待ちの時間予算)

## 3. publish の step 順序

- [x] 3.1 publish job の step を、引き継ぎ deployment ID の読み込み (`Download previous deployment ids` →
      `Prepare deployment id files`) が他の成果物の取得より前に来る並びにする
      (→ Scenario: 引き継ぎの読み込みが他の成果物の取得より先に行われる)
- [x] 3.2 引き継ぎの読み込み結果 (読み込み済みか・引き継ぎが存在したか) を output に残し、
      存在しない初回と読み込み失敗を区別できるようにする (→ Scenario: 引き継ぎが無い初回の実行と区別できる)
- [x] 3.3 `scripts/release/check-publish-step-order.py` を新設し、引き継ぎの download と読み込みが
      それぞれ 1 件で、他の成果物の取得より前にあることを検査する (→ Scenario: 順序が崩れると検査が落ちる)
- [x] 3.4 同スクリプトに自己テストを足す。実物の `release.yml` を土台に step を移動・削除した写しで
      終了コードを検査する (→ Requirement: リリース用スクリプトの自己テスト)

## 4. 自己テストの CI 接続

- [x] 4.1 `.github/workflows/ci.yml` の lint job に `scripts/release/` 8 本の `--selftest` を追加する
      (`central-portal.sh` / `central-resume.sh` / `check-distribution-tag.sh` / `check-nuget-version.sh` /
      `check-resume-eligibility.sh` / `check-signatures.sh` / `compare-maven-artifacts.sh` /
      `wait-for-registries.sh`。`set-readme-version.py` は後続 change で撤去するため対象外)
      (→ Requirement: リリース用スクリプトの自己テスト)
- [x] 4.2 同 lint job に `check-time-budget.py` と `check-publish-step-order.py` の本検査と自己テストを追加する
      (自己テストを本検査より先に同じ並びで走らせる) (→ Requirement: 待ちの時間予算 / 保留中 deployment の引き継ぎの読み込み順序)
- [x] 4.3 `scripts/release/central-portal.sh` の複数 ID 待ちの自己テストで、台本を 1 件伸ばしたときに
      誤実装が停止ではなく失敗として表れることを確かめる
      (→ Scenario: 待ちの誤実装が停止ではなく失敗として表れる)

## 5. 決定の記録

- [x] 5.1 [cross/ADR-0022](../../decisions/cross/0022-lint-job-includes-readme-example-lint.md) を amends する
      ADR を `status: proposed` で起票し、lint job の検査の集合を更新する (8 検査 → 本 change の追加分を反映)。
      `kasane/decisions/index.md` も更新する

## 6. 検証

- [ ] 6.1 `scripts/release/` 全スクリプトの `--selftest` が手元で全件通ることを確認する
- [ ] 6.2 `check-time-budget.py` と `check-publish-step-order.py` の検査が、意図的に定数・step 順序を
      壊したときに失敗することを確認する (→ Scenario: 公開待ちを延ばすと予算の超過が検出される /
      Scenario: 順序が崩れると検査が落ちる)
- [ ] 6.3 lint job が手元と同じ結果になることを、`develop` への push で確認する
- [ ] 6.4 `dry-run` で release を起動し、validate から消費者検証までが通ることを確認する
      (publish 内部は `dry-run` では到達しない — Impact のリスクに記載のとおり)

## 蒸留への申し送り (実装タスクではない)

- `kasane/concepts/cross/architecture/release-workflow.md` — 「公開確認」に相当する節へ状態分類を反映する
- `kasane/handbook/cross/verification-ci.md` — lint job の「8 検査」の記述と検査表を、ADR-0026 が accepted になった時点で 11 検査へ追随させる
- `kasane/handbook/cross/ci-script-deletion.md` — 「関連」節が lint job の自己テストとして cross/ADR-0020 だけを挙げているため、ADR-0026 への言及を検討する
- `kasane/handbook/cross/release-procedure.md` — 「失敗したとき」節に、反映待ちの失敗出力から
  レジストリ障害と未反映を読み分ける手掛かりを足す
- 反映待ち job の予算式 (待機の上限 + 期限を跨げる照会 1 件の応答上限 + job の前後 < `timeout-minutes`) と、
  「期限を跨げる照会は常に 1 件まで」の根拠が巡回の構造に依存することを concepts へ
- 時間予算の検査はスクリプト側の定数リテラルだけを読み、workflow の `env:` による上書きは見ない。現状 `release.yml` に `KSR_` の上書きは無いが、上書きを導入するなら検査もそれを読む必要がある
- 簡易起票 `add-release-script-selftests-to-lint` に残る項目 2 (`actionlint`) と 3 (tag 照合のスクリプト化)
