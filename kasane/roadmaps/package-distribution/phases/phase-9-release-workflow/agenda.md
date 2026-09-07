# release workflow と初回リリース (release-workflow)

KsSettingsView の `release.yml` をコピー + 固有値の差し替えで逆流させ、4 形態一斉の release workflow (validate → test ∥ package → dry-run → publish → 反映待ち → smoke) と外部設定 (Trusted Publisher / Environment / secrets / deploy key) を整備し、初回リリースまで到達する change フェーズ。

## 論点

- 4 本化: package / dry-run / smoke の各段に KMP を足す (phase-7・8 の結論)。publish 段の順序 (Maven upload 保留 → NuGet push → Maven release → SPM tag → monorepo tag) に KMP の Maven artifact をどう並べるか (同じ Central deployment に `ksdialogs` / `ksdialogs-compose` / `ksdialogs-kmp` を同梱できるか)
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

## 決定事項

踏襲 (解決済み論点)。出典は cross/ADR-0009 (lockstep)、KsSettingsView cross/ADR-0019・cross/ADR-0020 (dispatch 起動・tag は最後・version 注入) と同 phase-8 の決定事項 (`../KsSettingsView/kasane/roadmaps/package-distribution/phases/phase-8-release-workflow/agenda.md`)。

- 起動は `workflow_dispatch` (version 入力、正規表現 `^[0-9]+\.[0-9]+\.[0-9]+(-(alpha|beta|rc)\.[0-9]+)?$` のみ通す)。`dry-run` 入力で publish 以降を行わないリハーサル。本番はリリース対象ブランチからのみ、secrets は Environment `release`
- version の SSoT は dispatch 入力 (= tag)。CI が `-Pversion=` / `-p:Version=` で注入し、ファイルは開発用既定値のまま (bump コミットを積まない)。tag は接頭辞なし `X.Y.Z`
- publish は取り消せる順で直列: SPM commit push (deploy key の認証失敗を不可逆操作の前に出す) → Maven upload 保留 (`.asc` 生成確認) → NuGet push (Trusted Publishing / OIDC、API key を保管しない) → Maven release (Publisher API、deployment ID は plugin ログから抽出し artifact で attempt をまたいで引き継ぐ) → SPM tag → monorepo tag + GitHub Release。失敗時は `if: failure()` で保留 deployment を drop
- 再実行は同じ version で「失敗した job から再実行」。publish 段の全ステップを存在検査で冪等化 (commit 差分ゼロなら skip / `--skip-duplicate` / tag は同 commit なら skip・別 commit なら失敗 / Release は既存なら触らない)
- Release ノートは GitHub 自動生成 + `.github/release.yml` のラベル分類。CHANGELOG ファイルは持たない
- concurrency group は `release`、cancel-in-progress は false
- 却下済み: tag push トリガー / ファイルを version の正にして tag と照合 / `vX.Y.Z` 表記 / 開発ブランチから起動 / publish 済み version の再実行禁止 / smoke 成功後に tag / workflow が README をリリース対象ブランチに commit / 共有 workflow 化 (理由は cross/ADR-0020 と KsSettingsView phase-8 の Alternatives)

## TODO

- [ ] 論点の解消 (4 本化・初回 version・README 置換の位置・docs-refresh のタイミング)
- [ ] ksn-propose で変更提案を起こす
