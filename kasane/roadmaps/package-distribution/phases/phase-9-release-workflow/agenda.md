# release workflow と初回リリース (release-workflow)

KsSettingsView の `release.yml` をコピー + 固有値の差し替えで逆流させ、4 形態一斉の release workflow (validate → test ∥ package → dry-run → publish → 反映待ち → smoke) と外部設定 (Trusted Publisher / Environment / secrets / deploy key) を整備し、初回リリースまで到達する change フェーズ。

## 論点

- 4 本化: package / dry-run / smoke の各段に KMP を足す (phase-7・8 の結論)。publish 段の順序 (Maven upload 保留 → NuGet push → Maven release → SPM tag → monorepo tag) に KMP の Maven artifact をどう並べるか (同じ Central deployment に `ksdialogs-core` / `ksdialogs` / `ksdialogs-kmp` を同梱できるか)
- 初回リリースの version (KsSettingsView は `0.1.0-beta.1`) と GitHub Release の prerelease 扱い
- README の version 置換 (`scripts/release/set-readme-version.py`) を置く位置はブランチモデル (phase-3 の結論) に従う。AGENTS.md の「README は docs-refresh 経由のみ」への例外 1 行
- 初回リリース前の docs-refresh 依頼 (phase-2 で作った Skills / README を最新の concepts に追従させる) のタイミング

### KsSettingsView phase-8 からの申し送り (2026-09-04、library-foundation phase-11 から移送)

コピーして値を差し替える形で逆流する (共有 workflow 化はしない):

- `.github/workflows/release.yml` — 固有値 (Maven 座標・NuGet Package ID・配信リポジトリ名・artifact 名・Portal の URL) は冒頭の `env` に集約されている。段構成は validate → (test ∥ package) → dry-run (消費者検証に artifact を渡す) → publish (直列 1 job、`environment: release`) → 反映待ち → smoke
- `scripts/release/` — `central-portal.sh` (Central Portal Publisher API の status / release / wait-published / drop / published)、`set-readme-version.py`、`wait-for-registries.sh`、`check-signatures.sh`、`compare-maven-artifacts.sh` (再ビルドの同一性)、`check-distribution-tag.sh` (配信リポジトリの tag 判定)、`scripts/spm-snapshot/sync-snapshot.sh` (phase-5 で先に取り込む)
- `.github/release.yml` (Release ノートの分類) と `kasane/handbook/cross/release-procedure.md` (GitHub 設定とリリース手順の guide) を KsDialogs 側にも翻案する
- KsDialogs 側で別途作るもの: nuget.org の Trusted Publisher Policy (Repository `KsDialogs` / Workflow `release.yml` / Environment `release` / Glob `KsDialogs.*`、Scopes は push のみ)、GitHub Environment `release` (deployment branch policy = リリース対象ブランチ) と secrets 7 件 (`MAVEN_CENTRAL_USERNAME` / `MAVEN_CENTRAL_PASSWORD` = Central Portal の User Token、`SIGNING_KEY` / `SIGNING_KEY_ID` / `SIGNING_PASSWORD`、`NUGET_USER`、`SPM_DEPLOY_KEY`)、配信リポジトリの deploy key、branch protection
- 実測で分かった前提: Central Portal に「座標 + version が公開済みか」の API は無く `repo1.maven.org` の HEAD で判定する / drop 済み deployment の status は 404 / 初回リリースの所要は 39 分 (validate 9 秒 / test ∥ package 7 分 / consumer-maui dry-run 12 分 / publish 11 分 / smoke-maui 9 分)

### phase-4 からの申し送り: `main` の branch protection (2026-09-08)

`main` を作成した直後に branch protection を付ける (REST は実在するブランチにしか PUT できない)。内容は KsSettingsView の `main` と同じ形 (必須 status check・PR 経由必須 (承認数 0)・force-push 禁止・削除禁止・admin バイパスは緊急時の逃げ道として許容。`gh api -X PUT` は全体置換なので完全な payload を送る)。必須 status check は `{"context": ..., "app_id": 15368}` 形式で次の 10 件。`develop` には必須 check を付けない (cross/ADR-0028 翻案元の決定、phase-4 で確認済み)。

| 種別 | check 名 |
|---|---|
| lint | `lint` |
| 本体検証 5 本 (phase-4) | `ios / verify`、`android / verify`、`android-instrumented / verify`、`kmp / verify`、`maui / verify` |
| 消費者検証 4 本 (phase-8 で job 名確定) | `consumer-ios / verify`、`consumer-android / verify`、`consumer-maui / verify`、`consumer-kmp / verify` |

初回リリースの PR (`develop` → `main`) で、検証 CI の `main` 宛て PR に関する Scenario (pull_request トリガーでの起動・head 制限・status check 名の固定) を実動で確認する。phase-4 の change (add-verification-ci) では `main` が無いためステップ単体の確認までしか行えない (2026-09-08 申し送り)。

phase-4 の実装結果 (2026-09-08): 入口 `ci.yml` は変更検出 job `changes` を持ち、7 本目の補助 check として報告される。必須 check は上表の 10 件のままで `changes` は含めない (cross/ADR-0017)。`develop` push の実動 (lint + 5 job の起動・記録だけの push でのスキップ・連続 push の打ち切り) は確認済みで、残るのは `main` 宛て PR 側だけ。

### phase-7 からの申し送り (2026-09-09)

KMP の発行設定は実装済み (`kasane/changes/archive/2026-09-09-add-kmp-maven-distribution/`、cross/ADR-0008 / 0009 は accepted)。phase-7 の C3 (release の 4 本目) に対して、実装で判明した制約を論点として足す。

**SPM tag の先行 (論点)**: KGP は発行時に SwiftPM パッケージを解決するため、既定 URL (`https://github.com/kamusoft/KsDialogs-SPM`) でリリース版の iOS publication (cinterop klib 付き) を `publishToMavenLocal` するには、配信リポジトリに同版の tag が実在する必要がある (未 push だと `no versions of 'ksdialogs-spm' match the requirement` で失敗。root publication だけなら通る)。C3 の package 段「kmp/ を `-Pversion=` で publishToMavenLocal (Swift 参照は既定の https + exact)」は publish 段より前では成立しない。どの形でも「SPM tag が KMP の Maven artifact より先に存在する」ことが必須になる。

| 取りうる形 | 影響 |
|---|---|
| (a) package 段の kmp は `file://` のスナップショット clone で発行し、publish 段で SPM tag push 後に既定 URL で発行し直す | 再ビルドの同一性検査 (`compare-maven-artifacts.sh`) の対象と、dry-run 用成果物との関係を決め直す |
| (b) publish 順序を SPM tag → Maven upload にする | C3 が却下した「Maven release が失敗すると配信リポジトリに tag だけが残る」を、tag の削除運用か prerelease tag で受ける |

| 項目 | 内容 |
|---|---|
| SNAPSHOT ガードの経路 | ガードは設定段階の要求タスク名照合 + task graph 確定時の 2 段で、名前を直接指定した Central 向けタスクは SNAPSHOT 診断のみで止まる。集約タスク `publish` 経由だけは認証情報未解決と同時に報告される。workflow は発行タスクを名前で直接呼ぶ |
| docs-refresh の KMP 分 | Skill `ksdialogs-kmp` の依存スコープを `implementation` から `api` へ / Kotlin サポート範囲 (同 minor 2.4.x、確認済み版はカタログの `kotlin`) / 「予定している公開 coordinate」の状態表記 / SNAPSHOT を Maven local へ発行した成果物は同一マシンでしか動かない旨。phase-5 / 6 分と同じ依頼にまとめる |
| 配布構成の concepts 化 (KMP 分) | 5 publication (root + android + iOS 3 ターゲット) の内容と SwiftPM 連携メタデータ・version 導出と Swift 参照導出・`@Throws` 回帰検査の位置づけは、「phase-6 からの申し送り」の配布構成 concepts 化の行に含めて置き場を決める |

### phase-8 からの申し送り (2026-09-10)

消費者検証は実装済み (`kasane/changes/archive/2026-09-10-add-consumer-verification/`、仕組みは concepts [消費者検証](../../../../concepts/cross/architecture/consumer-verification.md))。release からは `verify-consumer-{ios,android,maui,kmp}.yml` を dry-run (package 段の artifact を `artifact` 入力で渡す) と smoke (`version` 必須) で呼ぶ。

| 項目 | 内容 |
|---|---|
| artifact の配置 | package 段が upload するルート構造は concepts「フィード準備と artifact の配置」の表のとおり。KMP は Android 分のローカル Maven リポジトリだけを渡し、kmp/ の発行とスナップショット clone の tag は消費者 job 内で行う (package 段で作る kmp/ の成果物は Swift 参照が既定の https + exact で、tag が無い dry-run では解決できない) |
| 所要時間の実測 (2026-09-09、コールドキャッシュ) | 消費者 job: ios 46 秒 / android 2 分 50 秒 / kmp 9 分 20 秒 / maui 15 分 26 秒 (timeout 30 / 30 / 30 / 40 分)。artifact 入力時: kmp 8 分 10 秒 / maui 13 分 24 秒。`main` 宛て PR の壁時計は 19 分 46 秒で、macOS 6 job のうち本体の ios / maui が約 7 分 40 秒待った。release の dry-run 段の並走数はこの待ちを見込んで決める |
| `main` の作成 | 一時 `main` で `main` 宛て PR の Scenario (10 job の起動・status check 名・consumer job の dry-run) は確認済み。phase-9 で `main` を作るときは branch protection と必須 status check 10 件 (「phase-4 からの申し送り」の表) を併せて登録する |
| README の KMP ホスト側の例 | README に KMP のホスト側 (iOS / Android) の登録例を載せるかは初回リリース前の docs-refresh の論点に含める。載せるなら `scripts/readme-example-lint.py` の対応表に 2 行足す (cross/ADR-0022 の Revisit When) |
| MAUI の依存警告の負ケース | `WarningsAsErrors` (NU1605 / NU1608 / NU1107) を故意に起こす実行証跡は無い (宣言のみ)。smoke で警告が表面化する経路は実測済み。必要なら release の change で 1 ケース足す |

## 決定事項

踏襲 (解決済み論点)。出典は cross/ADR-0009 (lockstep)、KsSettingsView cross/ADR-0019・cross/ADR-0020 (dispatch 起動・tag は最後・version 注入) と同 phase-8 の決定事項 (`../KsSettingsView/kasane/roadmaps/package-distribution/phases/phase-8-release-workflow/agenda.md`)。

- 起動は `workflow_dispatch` (version 入力、正規表現 `^[0-9]+\.[0-9]+\.[0-9]+(-(alpha|beta|rc)\.[0-9]+)?$` のみ通す)。`dry-run` 入力で publish 以降を行わないリハーサル。本番はリリース対象ブランチからのみ、secrets は Environment `release`
- version の SSoT は dispatch 入力 (= tag)。CI が `-Pversion=` / `-p:Version=` で注入し、ファイルは開発用既定値のまま (bump コミットを積まない)。tag は接頭辞なし `X.Y.Z`
- publish は取り消せる順で直列: SPM commit push (deploy key の認証失敗を不可逆操作の前に出す) → Maven upload 保留 (`.asc` 生成確認) → NuGet push (Trusted Publishing / OIDC、API key を保管しない) → Maven release (Publisher API、deployment ID は plugin ログから抽出し artifact で attempt をまたいで引き継ぐ) → SPM tag → monorepo tag + GitHub Release。失敗時は `if: failure()` で保留 deployment を drop
- 再実行は同じ version で「失敗した job から再実行」。publish 段の全ステップを存在検査で冪等化 (commit 差分ゼロなら skip / `--skip-duplicate` / tag は同 commit なら skip・別 commit なら失敗 / Release は既存なら触らない)
- Release ノートは GitHub 自動生成 + `.github/release.yml` のラベル分類。CHANGELOG ファイルは持たない
- concurrency group は `release`、cancel-in-progress は false
- 却下済み: tag push トリガー / ファイルを version の正にして tag と照合 / `vX.Y.Z` 表記 / 開発ブランチから起動 / publish 済み version の再実行禁止 / smoke 成功後に tag / workflow が README をリリース対象ブランチに commit / 共有 workflow 化 (理由は cross/ADR-0020 と KsSettingsView phase-8 の Alternatives)

### phase-5 からの申し送り (2026-09-08)

利用者向け成果物 (`skills/{en,ja}/ksdialogs-android/**`・`skills/{en,ja}/ksdialogs-kmp/references/android-host.md`・`README.md`・`README_ja.md`) に旧座標 `jp.kamusoft:ksdialogs-compose` と本体としての `jp.kamusoft:ksdialogs` が計 16 箇所残る。cross/ADR-0019 の新座標 (Compose 側 `ksdialogs` / 本体 `ksdialogs-core`) への追随は docs-refresh の責務で、初回リリースより前に走らせる (release が先だと存在しない座標を案内する README が公開される)。「docs-refresh のタイミング」の論点はこの残存を潰す前提で決める。

version の注入と SNAPSHOT ガードは android/ に配線済み (cross/ADR-0009 に記載)。release workflow は `-Pversion=` を android/ と kmp/ の両ビルドに渡す。SwiftPM のスナップショット同期は `scripts/spm-snapshot/sync-snapshot.sh` (配置まで。commit / tag / push は workflow 側)。検証用の `verify-https-resolution.sh` は手動検証専用で release から呼ばない。

### phase-6 からの申し送り (2026-09-08)

| 項目 | 内容 |
|---|---|
| MAUI の発行版ガード | `maui/Directory.Build.props` の開発既定値 `0.0.0-dev` のまま pack した nupkg は成立する (pack 検算で実測)。release workflow 側で MAUI 3 パッケージの注入値の形式検査と `0.0.0-dev` 発行の禁止を持つ。android/ の SNAPSHOT ガード (cross/ADR-0009) に相当するものが MAUI 側には無い (review-001 Suggestion 2) |
| facade nupkg の XML ドキュメント | `GenerateDocumentationFile` の指定がリポジトリに無く、.NET Android SDK の既定で `lib/net10.0-android36.0/KsDialogs.Maui.xml` だけが入る非対称 (`net10.0` / `net10.0-ios26.0` には無い)。初回発行前に「全 TFM で生成して同梱する (日本語 doc コメントが公開物に載る)」か「pack から外す」かを決め、`maui/Directory.Build.props` に明示して SDK 既定への暗黙依存を消す (review-001 Minor 2) |
| docs-refresh の内容 (MAUI 分) | README 互換表と導入節、skills `ksdialogs-maui` / `ksdialogs-aiforms-migration` に、MAUI 10.0.20 以上 (同じ workload set なら版を書かなくてよい・古い版を書くと NU1605)・最低 OS 版 iOS 17 / Android 24 とガード `KSDLG0001`・API 版付き TFM の SDK 要件・失敗種別 `ViewCreationFailed` を反映する。phase-5 の Android 座標分と 1 回の依頼にまとめる |
| 配布構成の concepts 化 | MAUI の 3 パッケージ構成・pack 経路・最低 OS ガード・SDK 更新時の再検証箇所 (manifest 絶対パス・API 版付き TFM・自 assembly 用 aar の有無) は、phase-5 の Native 配布経路 (SwiftPM スナップショット・Maven 発行・版の導出) と併せて phase-9 の蒸留で置き場 (`<platform>/api/` に収めるか新カテゴリか) を決める。現時点の記述は maui/ADR-0004 の現行照合 footer と handbook local-development-setup.md が持つ |

## TODO

- [ ] 論点の解消 (4 本化・SPM tag の先行 (phase-7 申し送り)・初回 version・README 置換の位置・docs-refresh のタイミング)
- [ ] 初回リリース前に docs-refresh を走らせ、skills / README の旧 Android 座標 (16 箇所) を cross/ADR-0019 の新座標へ追随させる (phase-5 申し送り)。MAUI 分 (phase-6 申し送りの表) と KMP 分 (phase-7 申し送りの表) は同じ依頼に含める
- [ ] MAUI 3 パッケージの `0.0.0-dev` 発行ガードと XML ドキュメントの方針を release の change に含める (phase-6 申し送り)
- [ ] ksn-propose で変更提案を起こす
