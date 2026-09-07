# public 化の実施手順書 (2026-09-07 ドラフト)

phase-3 の決定事項 (agenda.md) を実行順に並べたチェックリスト。翻案元は KsSettingsView の同名手順書 (`../KsSettingsView/kasane/roadmaps/package-distribution/phases/phase-2-public-readiness/artifacts/publish-procedure.md`)。実施時はチェックを埋め、完了後に「実施記録」節へ結果 (日付・新 repo URL・検査結果) を書く。

前提:

- 着手条件は充足済み: 利用者向け文書 (phase-1・2) は完了、README はルート英日 2 枚、`skills/` は生成済み。public 化は CI 構築 (phase-4) より前に行う
- 点検の実測 (2026-09-07): gitleaks 8.30.1 は全履歴 119 commit で no leaks、ローカルパス lint 0 件、識別子 lint はソース 5 ルート 1025 ファイルを含めて 0 件、`DEVELOPMENT_TEAM` 0 件、author (`user.email`) は local / global とも noreply 済み
- 履歴は引き継がない (cross/ADR-0021 踏襲)。remote が無いため旧リポジトリの rename は不要で、保管先は private リポジトリを新規に作る
- ブランチは `develop` / `main` の 2 本 (cross/ADR-0016 proposed)。公開ツリーの initial commit は `develop` に置き、`main` は初回リリースの PR で作る
- エージェントの実行分類器が `gh repo create` 等を止める場合はオーナーが手で実行する (KsSettingsView では `gh repo rename` だけが止まった)

## 1. 現クローン上での下ごしらえ (commit して記録を残す)

- [x] README 2 枚の画像 URL を `.../kamusoft/KsDialogs/main/assets/...` → `.../develop/...` へ置換 (6 行 × 2 枚)。README は docs-refresh の管轄だが、この置換は決定事項に基づく識別子の差し替えなので手順書内で直接行い、AGENTS.md の運用宣言には触れない
- [x] `kasane/config.yaml` の `lint.identity.scope` に `samples` / `ios` / `android` / `maui` / `kmp` を追加し、「ソース・テストは含めない」のコメントを「正当な UUID 定数が入ったら `allow` に足す」へ書き換える
- [x] `.gitignore` の救済行 `!kasane/**/verification/**/*.log` とその説明コメントを削り、`!kasane/**/evidence/**/*.log` は残す
- [x] 追跡中の `.log` 23 件はこの時点では `git rm` しない (履歴保管先にはそのまま残り、公開ツリーは 2 節で除外する)
- [x] `lint.exclude` の `kasane/**/verification/**/*.log` はここでは外さない。追跡中の `.log` 3 件が識別子 lint に掛かるため、公開ツリーで除外した後 (4 節の新クローン上) で外す
- [x] 再走査 4 種を実行して 0 件を確認する

```bash
python3 scripts/local-path-lint.py
python3 scripts/identity-lint.py
gitleaks git --redact --no-banner .
grep -rn DEVELOPMENT_TEAM samples/ maui/macios/native/ --include=project.pbxproj
```
- [x] 上記を `main` に commit する

## 2. 公開ツリーの作成 (単一 initial commit)

- [x] `git ls-files` の一覧から次を除いて `../KsDialogs-public-tree` へコピーする
  - `kasane/changes/archive/**` の媒体 (png 340 件 / 43 MB)
  - `kasane/changes/archive/**/verification/**/*.log` (23 件 / 136 KB)
  - 試算: 追跡 2150 件のうち公開ツリーに残るのは 1787 件 / 約 15 MB
- [x] symlink 2 件 (`CLAUDE.md` → `AGENTS.md`、`.claude/skills/docs-refresh` → `../../.agents/skills/docs-refresh`) はリンクのままコピーし、リンク先が追跡下にあることを確認
- [x] 新ディレクトリで `git init -b develop` → `git add -A` → 無視されたファイルが 0 件であることを `git status` で確認 → 単一 commit (author は noreply、メッセージ `Initial public snapshot`)
- [x] 新ディレクトリで再走査 4 種 (1 節と同じ) を実行し、すべて 0 件。`git config core.hooksPath .githooks` を設定
- [x] 画像リンクが壊れる `.md` の件数を記録する (archive 媒体を外した既知の帰結。KsSettingsView は 5 ファイル・10 件)

## 3. GitHub: 履歴の保管・新 repo の公開・配信リポジトリ

### 3a. 履歴の保管先 (private)

- [x] `gh repo create kamusoft/KsDialogs-private-archive --private` (description は「KsDialogs の public 化前の履歴保管 (読み取り専用)」相当)
- [x] 現クローンに remote `origin` を追加し、`main` と `spike/phase-10-packaging-poc` を push する
- [x] push 後に `gh repo archive kamusoft/KsDialogs-private-archive` で読み取り専用にする

### 3b. 新 repo `kamusoft/KsDialogs` (public)

- [x] `gh repo create kamusoft/KsDialogs --private` で作成し、2 節のツリーを `develop` として push。default branch を `develop` にする
- [x] GitHub 上で中身を目視 (README の画像表示・ツリー・ファイル数・容量) → visibility を **public** に切り替え
- [x] description は README の Overview 1 文目を短縮した英文。website は空 (配布先が未確定)
- [x] topics は次から 10 個: ios / android / dotnet-maui / kotlin-multiplatform / swift / kotlin / jetpack-compose / dialog / toast / ui-library / cross-platform
- [x] 設定 (gh api): Issues ON / Wiki OFF / Discussions OFF / Projects OFF (作成時の既定が ON なら public 切替の前に OFF)、Actions 有効 (既定)、Secret scanning + Push protection ON、Dependabot alerts ON
- [x] **Pull requests を collaborators only** にする (`pull_request_creation_policy` = `collaborators_only`。cross/ADR-0013、phase-2 からの申し送り)
- [x] `develop` の branch protection = force-push 禁止 + 削除禁止 (必須 status check は phase-4 の CI 後)。`main` の保護は初回リリース PR の前 (phase-9) に完全な payload で PUT する
- [x] ラベル `bug` / `enhancement` / `question` の存在を確認 (Issue Forms の `labels:` は存在しないラベルを自動生成しない)

### 3c. 配信リポジトリ `kamusoft/KsDialogs-SPM` (public)

- [x] `gh repo create kamusoft/KsDialogs-SPM --public` (description: `SwiftPM distribution snapshot of KsDialogs (source: kamusoft/KsDialogs)`、homepage: monorepo の URL)
- [x] 初回 commit は誘導 README と monorepo ルート `LICENSE` のコピーの 2 点。default branch は `main`
  - 誘導 README は KsSettingsView-SPM の `scripts/spm-snapshot/README.template.md` を名前だけ差し替える
  - `Package.swift` / `Sources` / `Tests` は phase-5 の生成スクリプトが初回 push する
- [x] 設定: Issues / Wiki / Projects / Discussions すべて OFF、PR は collaborators only、workflow と branch protection は置かない、GitHub Release は作らない (tag のみ)

## 4. ローカルの切り替え

- [x] 現クローンを `../KsDialogs-private-archive` へ改名する (remote は 3a で設定済み)
- [x] `../KsDialogs-public-tree` を `../KsDialogs` へ移し、remote `origin` を新 repo に設定する (`../<リポジトリ名>/` 規約と Claude Code のパス紐づけを保つ)
- [x] 未追跡の開発ファイルを旧ディレクトリから複製する: `local.properties` 5 件 (`android/` `kmp/` `maui/android/native/` `samples/android/` `samples/kmp/`) と `.claude/settings.local.json`。ビルド生成物は再生成
- [x] 新クローンで `git config core.hooksPath .githooks` を設定し、両 lint の `--selftest` が通ることを確認
- [x] `kasane/config.yaml` の `lint.exclude` から `kasane/**/verification/**/*.log` を外し (`exclude: []`)、識別子 lint が exit 0 のままであることを確認して commit
- [x] 4 ルートのビルドが通ることを確認 (iOS: `ios/` で `swift build` / Android: `android/` で `./gradlew assemble` / KMP: `kmp/` で `./gradlew assemble` / MAUI: `maui/` で `dotnet build`)
- [x] Claude Code のメモリ・セッションが同じパスで引き継がれていることを確認

## 5. 後続 (この手順書の外、別フローで)

- [x] phase-4 (検証 CI) へ (2026-09-07 agenda に追記): ブランチモデル `develop` / `main`、`develop` の必須 status check、識別子 lint の 5 ルート検査の CI 化
- [x] phase-5 (native packaging) へ (2026-09-07 agenda に追記): `KsDialogs-SPM` への初回スナップショット push
- [ ] cross/ADR-0016 のオーナー確認 (proposed → accepted は phase-4 / 9 の蒸留時)
- [x] agenda の「調査結果のまとめ」を書き、ksn-roadmap で research 完了をマーク (2026-09-07)

## 実施記録

### 2026-09-07: 1 節 下ごしらえ (完了)

- README 2 枚の画像 URL 6 行 × 2 を `develop` へ置換。docs-refresh の manifest は README の URL を持たないため追従作業なし
- `lint.identity.scope` に 5 ルートを追加しコメントを書き換え。`--selftest` 全件 OK
- `.gitignore` の verification 救済行を削除 (追跡中の 23 件は追跡のまま)。`lint.exclude` は 4 節まで残す
- 再走査 4 種すべて 0 件 (ローカルパス lint / 識別子 lint / gitleaks 全履歴 no leaks / `DEVELOPMENT_TEAM` 0)

### 2026-09-07: 2 節 公開ツリーの作成 (完了)

- 除外は決定どおり: archive の png 340 件 / 43 MB と `.log` 23 件 / 136 KB。追跡 2150 件のうち**公開ツリーに残るのは 1788 件 / 15 MB** (`.git` は 12 MB)。残る媒体は `assets/` の README 画像 6 枚のみ
- 作成先は `../KsDialogs-public-tree`。symlink 2 件はリンクのままコピーし、リンク先が追跡下にあることを確認
- `git init -b develop` → `git add -A` で 1788 件すべてが追跡され、無視されたファイルは 0 件。単一 commit `Initial public snapshot` (author は noreply)。`core.hooksPath` を設定
- 公開ツリー上で再走査 4 種すべて 0 件
- 既知の帰結: archive の `.md` から png を指す Markdown リンク 5 件 / 1 ファイルが壊れる (想定内)

### 2026-09-07: 3 節 GitHub (完了)

**3a 履歴の保管先**: `kamusoft/KsDialogs-private-archive` を private で作成し、`main` (cd7ca56 まで) と `spike/phase-10-packaging-poc` を push → Archive。push はオーナーが `--no-verify` で手動実行した。エージェントの commit / push 検査 (git-gate の bash hook) は、コマンドが `cd` / `-C` で別リポジトリを指していても現クローンの「どのリモートにも無い commit」を検査するため、過去に直した違反 246 件を含む全履歴の push は必ず deny され、公開ツリー・配信リポジトリの push まで同じ理由で止まった。保管先へ push し終えると未 push commit が無くなり、以降の push はエージェントから実行できた。

**3b 新 repo**: `kamusoft/KsDialogs` を private で作成 → `develop` を push (1 commit・1788 ファイル) → 既定ブランチを `develop` に → オーナー目視 → public へ切替。description は英文 1 文、topics 10 個 (ios / android / dotnet-maui / kotlin-multiplatform / swift / kotlin / jetpack-compose / dialog / toast / ui-library)、website は空。

設定は Issues ON / Wiki・Projects・Discussions OFF、Dependabot alerts ON (204)、PR は `pull_request_creation_policy` = `collaborators_only`。`gh api -f` の文字列 "false" では OFF にならず、`-F` の真偽値で反映した。Secret scanning + Push protection と `develop` の branch protection (force-push 禁止 + 削除禁止、必須 status check なし) は Free プランでは private だと 403 になるため public 切替の直後に設定した。ラベル bug / enhancement / question は既定で存在。

private の間は README の画像 (raw.githubusercontent の絶対 URL) が表示されなかった (目視で指摘)。public 切替後に Browser で確認し、6 枚すべて読み込み済み (iOS 1206 px / Android 1080 px 幅)。

**3c 配信リポジトリ**: `kamusoft/KsDialogs-SPM` を public で作成 (description・homepage は monorepo 向け)、Issues / Wiki / Projects / Discussions OFF、PR は collaborators only。初回 commit は誘導 README (KsSettingsView-SPM のテンプレートを名前だけ差し替え) + LICENSE の 2 点を `main` へ push。workflow・branch protection・Release は置いていない。

- 実行制約: `gh repo create` / `gh repo edit --visibility` / `gh repo archive` / `gh api` はエージェントから実行できた。`curl` による raw URL の疎通確認は実行分類器に止められ、Browser の DOM 検査で代替した

### 2026-09-07: 4 節 ローカルの切り替え (完了)

- 現クローン → `../KsDialogs-private-archive` (remote は保管先)、公開ツリー → `../KsDialogs` (remote は新 repo)。未追跡の `local.properties` 5 件を複製 (`.claude/settings.local.json` と `.claude/plans` は旧側に存在しなかった)
- `core.hooksPath` は公開ツリー作成時に設定済み。3 lint の `--selftest` 全件 OK。`lint.exclude` から verification ログの glob を外し、識別子 lint・ローカルパス lint とも exit 0
- 4 ルートのビルド成功: iOS `swift build` 3.7 秒 / Android `./gradlew assemble` 150 タスク / KMP `./gradlew assemble` 96 タスク / MAUI `dotnet build KsDialogs.slnx` 0 警告 0 エラー
- Claude Code のメモリ 9 件は同じパスで引き継がれている
- 記録の残し方: 1〜2 節の実施記録までが公開スナップショット (initial commit) に入り、3〜4 節のこの記録は公開後の通常 commit として新 repo に入る

