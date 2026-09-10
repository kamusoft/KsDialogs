# Proposal: add-release-workflow

## Why

4 形態のパッケージング (phase-5〜7) と消費者検証 (phase-8) は揃ったが、配布物を公開レジストリへ出す経路が無い。ロードマップ package-distribution のゴール「単一 version で全形態を 1 回の手動起動で一斉リリースでき、tag は publish 全成功後にのみ生まれる」を満たす release workflow と、その外部設定 (GitHub Environment / secrets / nuget.org の Trusted Publisher / 配信リポジトリの deploy key / `main` の branch protection) を整え、初回リリース `0.1.0-beta.1` まで到達する。

姉妹ライブラリ KsSettingsView の `release.yml` と `scripts/release/` をコピーして固有値を差し替える形で逆流させる (共有 workflow 化はしない)。翻案元に無いのは KMP 形態で、KMP の Maven artifact は SwiftPM 連携メタデータに配信リポジトリの URL を焼き込み、iOS publication の発行には同版 tag の実在が要る。この制約が publish の順序と runner を変える (cross/ADR-0024 proposed)。設計判断はフェーズ議論で決着済み ([agenda](../../roadmaps/package-distribution/phases/phase-9-release-workflow/agenda.md) の踏襲 7 項目と決定 R1〜R7)。本提案はそれをアーティファクトに落とす。

## What Changes

- **release workflow `.github/workflows/release.yml`** (新設): `workflow_dispatch` (`version` 入力は `^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)(-(alpha|beta|rc)\.(0|[1-9][0-9]*))?$` のみ、`dry-run` 入力で publish 以降を行わない)。段構成は validate → (本体検証 5 本 ∥ package 3 job) → 消費者 dry-run 4 job (package 段の artifact を渡す。KMP は Android 分だけ) → publish (macOS 1 job、`environment: release`) → 反映待ち → 消費者 smoke 4 job。本番は `main` からのみ起動。concurrency group `release`、cancel-in-progress なし。固有値 (Maven 座標 3 本・NuGet ID 3 本・配信リポジトリ名・artifact 名・Portal の URL) は冒頭の `env` に集約
- **publish の順序** (R1 / R2、cross/ADR-0024): SPM スナップショット commit push → Android の Maven upload 保留と validated 待ち → SPM tag push → KMP を https + exact で発行して upload 保留と validated 待ち → NuGet push (Trusted Publishing / OIDC) → Maven release 2 件 (Android → KMP) と published 待ち → monorepo tag + GitHub Release (suffix 付きは prerelease) → README / Skill の version 置換 commit を `develop` へ push。各ステップは存在検査で冪等化し、同じ version で「失敗した job から再実行」できる。失敗時は保留 deployment 2 件を drop。deployment ID は枠 (Android / KMP) ごとに artifact で attempt をまたいで引き継ぐ
- **version の注入と発行ガード** (踏襲 + R6): CI が `-Pversion=` を android/ と kmp/ に、`-p:Version=` を MAUI に渡す。ファイルは開発用既定値のまま。MAUI は package 段の pack 後と publish 段の push 直前に、3 つの nupkg が `<ID>.<version>.nupkg` の名前で存在することを検査する (`0.0.0-dev` は名前で弾かれる)
- **README / Skill の version 置換** (R4): `scripts/release/set-readme-version.py` を KsDialogs の対象 (README 2 枚と Skill の導入例。行の形で対象を見つけ、`<version>` プレースホルダも実値も置き換える) に翻案し、package-maui job が pack の前に作業木で実行 (nupkg 同梱 README が実値になる)、publish job が成功後に `develop` へ置換 commit を push する (競合しても release は失敗にしない)。人がリリース PR で version を書く手順は無い。AGENTS.md に例外 1 行を足す
- **`scripts/release/`** (新設、翻案): `central-portal.sh` (Central Portal Publisher API の status / wait-validated / release / wait-published / drop / published)、`check-distribution-tag.sh`、`check-signatures.sh`、`compare-maven-artifacts.sh` (Android 分の package 段と publish 段の同一性。KMP は対象外)、`wait-for-registries.sh` (Maven 3 座標の POM + NuGet 3 ID)、`set-readme-version.py`。削除操作は handbook ci-script-deletion に従い `rm`
- **Release ノート**: `.github/release.yml` (ラベル分類。CHANGELOG ファイルは持たない) と、分類に使うラベル (`breaking` / `feature` / `fix` / `docs` / `kasane` / `ci`) の作成
- **MAUI の XML ドキュメント** (R6): facade は 3 TFM で `GenerateDocumentationFile=true` を明示して nupkg に同梱、binding 2 つは false を明示
- **手順書と外部設定**: `kasane/handbook/cross/release-procedure.md` を翻案 (ブランチの役割・初回だけ行う設定・リリースのたびに行うこと・失敗したとき・リハーサル・version 放棄時の配信リポジトリ tag の削除)。オーナーが行う外部設定: `main` の作成と branch protection (必須 status check 10 件、`app_id: 15368`、PR 経由必須、force-push / 削除禁止)、配信リポジトリ `KsDialogs-SPM` の deploy key、GitHub Environment `release` (deployment branch = `main`) と secrets 7 件、nuget.org の Trusted Publisher Policy (Repository `KsDialogs` / Workflow `release.yml` / Environment `release` / Glob `KsDialogs.*`)。実行はオーナー、記録は tasks
- **初回リリース** (R3): `dry-run` でのリハーサル → リリース PR (`develop` → `main`。`main` 宛て PR の検証 CI 10 job の実動確認を兼ねる、phase-4 / 8 申し送り) → `0.1.0-beta.1` の dispatch → 反映待ちと smoke 4 本の成功 → GitHub Release (prerelease) の確認

影響する能力: release-workflow (新設)、maui-nuget-distribution (XML ドキュメントの明示、発行ガード)

## Non-Goals

- **MAUI の依存警告 (`WarningsAsErrors`) を故意に起こす負ケース** — R7 で見送り (Skill / README が MAUI の版を書くようになったら簡易起票)
- **docs-refresh 2 回目 (「未配信」表記の解除と配布構成の反映)** — 初回リリース後の蒸留の後に走らせる (R5、handbook docs-refresh-timing)
- **配布構成の concepts 化** (Native / MAUI / KMP の配布経路・5 publication・version 導出・`KSDLG0001`・KMP の Android ホストへの推移依存) — 蒸留 (ksn-distill) の責務。phase-6 / 7 の申し送りと docs-refresh の drift 所見が置き場を決める材料
- **KsSettingsView への R4 (workflow による version 置換) の逆流** — 別リポジトリの作業。phase-9 の実装後にロードマップの外で起票
- **共有 workflow 化・private 配信経路・iOS の binary 配布・Android の module 統合** — ロードマップの非ゴール
- **検証 CI (`ci.yml` / `verify-*.yml`) の変更** — 触らない。release は既存の reusable workflow を `uses:` で呼ぶだけ

## Impact

- 破壊的変更なし。ライブラリのコードには触れない (MAUI の props への XML ドキュメント指定と `ADR-0024` の参照コメントだけ)
- `main` ブランチと branch protection が初めて作られる。以後 `main` への変更は `develop` からの PR に限られる
- publish job は macOS runner で走る (KsSettingsView は ubuntu)。public リポジトリなので課金は無い
- リスク 1: KMP の https 発行は dry-run で予行できず本番でしか通らない (`file://` 発行と URL 以外は同じ経路)。失敗しても残るのは配信リポジトリの tag と保留 deployment (drop) だけで、第一の受け皿は翻案元と同じ「同じ version で再実行して埋める」。ロードマップのゴール「tag は publish 全成功後にのみ生まれる」は monorepo の tag について成り立ち、配信リポジトリの tag は KMP の発行に先立って生まれる例外になる (ゴール文の読み替えは蒸留時に ksn-roadmap で行う)。version を放棄するときはその番号を欠番にして再利用しない (公開済みの tag は削除しても clone 済みの利用者からは回収できない。削除は任意の後片付け)
- リスク 2: 外部設定 (secrets・Trusted Publisher・deploy key) は手元で検証できず、初回の dispatch で初めて通る。publish の先頭 (SPM commit push・Android upload の署名検査) が取り消せない操作より前に失敗を出す順序で受ける
- リスク 3: `develop` への version 置換 commit がオーナーの push と競合する余地。release は失敗にせず、次回のリリースで追いつく
- 提案作成時の長命層との突き合わせ: cross/ADR-0016 (accepted) は「README の version 置換はリリース PR の中の commit として行う」と定めており、R4 (workflow が publish 後に `develop` へ commit) と衝突する。cross/ADR-0024 (proposed) がこの 1 文を置き換える形で ADR-0016 を一部改訂する (`amends: [cross/0016]`)。0008 / 0009 / 0017 / 0022 とは衝突なし。ADR-0024 は本提案の design Decision と同内容で、蒸留時に accepted へ
- `develop` への version 置換 commit は `GITHUB_TOKEN` の push のため検証 CI を起動しない。ADR-0017 / 0022 の「`develop` への push ごとに lint を回す」保証を保つため、publish job が push の前に同じ lint (ローカル絶対パス・識別子・README 最小例一致) を置換後のファイルに掛ける (design Decision 4)

## 級: L

release workflow 1 本 (翻案元 900 行超) + スクリプト 6 本 + 外部設定 + 初回リリースの実行にまたがり、翻案元に無い KMP 分の順序・runner・2 deployment の設計判断を design.md に集約する必要がある。翻案元の同名 change も L (KsSettingsView `kasane/changes/archive/*-add-release-workflow/`)。

domain: cross
roadmap: package-distribution/phase-9-release-workflow
