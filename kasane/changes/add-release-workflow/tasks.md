# Tasks: add-release-workflow

翻案元は KsSettingsView `../KsSettingsView/.github/workflows/release.yml`、`../KsSettingsView/scripts/release/`、`../KsSettingsView/.github/release.yml`、`../KsSettingsView/kasane/handbook/cross/release-procedure.md` (change: `../KsSettingsView/kasane/changes/archive/2026-09-04-add-release-workflow/` と `../KsSettingsView/kasane/changes/archive/2026-09-07-fix-release-central-validation-wait/`)。固有値 (Maven 3 座標・NuGet 3 ID・配信リポジトリ名・artifact 名・必須 status check 10 件) を差し替え、KMP 枠と version 置換の自動化を足す。翻案元の規範・前提は翻案先で確認してから採録する (lessons spec-review L-003)。スクリプト内の削除は `rm` (handbook cross/ci-script-deletion.md)。

## 1. 前提の実測 (机上確定の裏取り。覆ったら実装を進めずエスカレーションする)

- [x] 1.1 Central Portal の実 upload での前提確認 (オーナーの資格情報で手元から): kmp/ を tag 付きローカル clone の `file://` 指定 + `-Pversion=0.0.0-alpha.<N>` で名前指定の発行タスク (`publishAllPublicationsToMavenCentralRepository` 相当) により **公開しない USER_MANAGED deployment として実 upload** し、upload ログの `deployment id:` 抽出、`central-portal.sh status` / `wait-validated` の応答、`drop` による回収を確認する (公開はしない。android/ も同じ手順で 1 回)。`--dry-run` / `--offline` では ID が出ないため代替にしない (→ design Decision 1 / 2、Requirement: Maven Central の 2 枠の deployment)
- [x] 1.2 macOS ランナー相当の環境で android/ を `publishToMavenLocal` した発行物と、同じ commit を 2 回ビルドした発行物が `compare-maven-artifacts.sh` の比較で一致することを確認する (再現性の前提。一致しなければ Decision 3 の見直し) (→ Requirement: Android 成果物の同一性)
- [x] 1.3 `set-readme-version.py` の対象行の列挙: README 2 枚と `skills/{en,ja}/**` から SwiftPM / Maven / NuGet のインストール例の行を `grep` で列挙し、ファイルごとの期待行数を確定する (docs-refresh 1 回目後の状態が正) (→ Requirement: README と Skill の version 置換)
- [x] 1.4 facade を `GenerateDocumentationFile=true` で pack し、CS1591 の有無と 3 TFM の `lib/` に `KsDialogs.Maui.xml` が入ることを確認する。CS1591 が出たら件数を記録してオーナーに諮る (design Open Questions) (→ Scenario: XML ドキュメントは facade の 3 TFM に揃う)
- [x] 1.5 前提が覆った場合 (KMP の発行タスクの名前・ログ形式が違う / Android の再ビルドが一致しない / 対象行の形が揃わない) は結果を記録し、design の該当 Decision の見直しをオーナーへ上げる

## 2. scripts/release/

- [x] 2.1 `central-portal.sh`: 翻案元を写し、`published` の判定 POM を座標引数で選べる形 (`published <version> <artifactId>`、既定は `ksdialogs-core`) にする。サブコマンド (status / wait-validated / release / wait-published / drop / published) と `--selftest` を維持する (→ Requirement: Maven Central の 2 枠の deployment)
- [x] 2.1b `central-resume.sh` (仮称): 枠の前回 deployment ID と状態 (VALIDATED / PUBLISHING / PUBLISHED / FAILED / NOT_FOUND / ID なし) から動作 (upload skip / PUBLISHED 待ち / 全 skip / drop して再 upload / 再 upload) を決める分岐を workflow から切り出し、`--selftest` で 6 状態すべてを判別できることを示す (lessons code-review L-001) (→ Scenario: release の応答が失われても再実行で整合する / 部分 publish を同じ version で埋める)
- [x] 2.2 `check-distribution-tag.sh` / `check-signatures.sh` / `compare-maven-artifacts.sh`: 翻案元を写し、配信リポジトリ名と座標を差し替える。`check-signatures.sh` は署名対象の拡張子集合を Android (aar / pom / jar / module) と KMP (pom / module / jar / klib / json) に広げ、必須 publication 名 (5 件) を列挙して、klib または json の `.asc` を 1 件欠かせた負ケースで失敗することを自己テストに含める (→ Requirement: 署名の生成確認 / Android 成果物の同一性 / 手動起動と入力の検証)
- [x] 2.3 `wait-for-registries.sh`: Maven の Android 2 座標 + KMP 5 publication の POM (artifactId は 1.1 の発行物一覧から確定) + nuget.org 3 ID の計 10 件 (→ Requirement: 反映待ちと smoke)
- [x] 2.4 `set-readme-version.py`: 翻案元を写し、対象ファイル (1.3 の列挙) と正規表現 (SwiftPM の `exact:` / Maven `ksdialogs-core`・`ksdialogs`・`ksdialogs-kmp` / NuGet `KsDialogs.Maui`) を差し替え、`<version>` プレースホルダも実値として受け付ける。`--check` は維持し `--selftest` を持つ (→ Requirement: README と Skill の version 置換、Scenario: 該当行が見つからなければ失敗する)
- [x] 2.5 各スクリプトの自己テストを lint job に載せるかを検討し、載せるなら cross/ADR-0022 の一部改訂 (lint job の検査の集合) を蒸留に申し送る。載せないなら手元実行の手順を handbook に書く (→ 備考)

## 3. MAUI パッケージングと Gradle 発行設定

- [x] 3.0 `android/build.gradle.kts` と `kmp/ksdialogs-kmp/build.gradle.kts` の `publishToMavenCentral()` に `automaticRelease = false` を明示し、Gradle プロパティで自動公開に倒れないことを task graph (`--dry-run`) で確認する (→ Requirement: Maven Central の 2 枠の deployment、Scenario: upload 後は両枠とも保留状態で止まる)

- [x] 3.1 `maui/KsDialogs.Maui/KsDialogs.Maui.csproj` に `GenerateDocumentationFile=true`、binding 2 つの csproj に `false` を明示する (`ADR-0024` ではなく agenda R6 の出典コメント) (→ Scenario: XML ドキュメントは facade の 3 TFM に揃う)
- [x] 3.2 nupkg 名の検査を workflow の step として書く (package 後 / push 前の 2 回。`<ID>.<version>.nupkg` と snupkg の対) (→ Requirement: 発行版の nupkg 名の検査)

## 4. release workflow

- [x] 4.1 `.github/workflows/release.yml` の骨格: `workflow_dispatch` (version / dry-run)、`permissions: contents: read`、concurrency group `release`、冒頭 `env` に固有値 (Maven 座標 3 本・NuGet ID 3 本・配信リポジトリ `kamusoft/KsDialogs-SPM`・artifact 名 4 種 (ios / android / maui / deployment-id)・Portal の URL)。validate job (checkout 前の version 形式と起動ブランチ、monorepo tag、配信リポジトリ tag、artifact 名の outputs。README の `--check` は持たない) (→ Requirement: 手動起動と入力の検証 / 段の構成と順序)
- [x] 4.2 test 段: `verify-{ios,android,android-instrumented,kmp,maui}.yml` を `uses:` で呼ぶ 5 job (→ Requirement: 段の構成と順序)
- [x] 4.3 package 段: `package-ios` (ubuntu、スナップショットを artifact に)、`package-android` (**macos-26**、JDK + Android SDK、署名鍵なしで `publishToMavenLocal -Pversion=`、`jp/` をルートに artifact)、`package-maui` (macos-26、pack の前に 2.4 で作業木の README / Skill を置換、binding 2 件 → facade を `-p:Version=` で pack、nupkg / snupkg の対と名前を検査 (3.2)、フラットに artifact)。`retention-days: 7` / `overwrite: true` (→ Requirement: 段の構成と順序 / 発行版の nupkg 名の検査、Scenario: nupkg の README は入力 version を持つ)
- [x] 4.4 dry-run 段: `consumer-{ios,android,maui,kmp}` を `verify-consumer-*.yml` に `mode: dry-run` / `version` / `artifact` (kmp は Android の artifact) で呼ぶ。`secrets: inherit` を書かない (→ Requirement: 段の構成と順序 / secrets と権限の範囲)
- [x] 4.5 publish job (macos-26、`environment: release`、`contents: write` + `id-token: write`、`needs` は test 5 + dry-run 4 + validate、`if: !dry-run`、timeout 120 分): Xcode 選択・JDK・Android SDK・.NET の準備 → Re-check external state (monorepo tag、`published` を 2 枠、nuget.org の存在、配信 tag。`github.run_attempt == 1` で外部状態があり monorepo tag が無ければ失敗して再実行を案内) → artifact の download (Android / MAUI / deployment ID 2 ファイル) → deploy key の ssh-agent 登録 (known_hosts 固定) → スナップショット commit push (差分なし skip) → 配信 tag の再照合 → Android 再ビルド + 同一性比較 + 署名検査 + upload (状態分岐) + ID 保存 + wait-validated → SPM tag push (`match` skip) → KMP を https + exact で発行 (名前指定タスク) + 署名検査 + upload (状態分岐) + ID 保存 + wait-validated → NuGet login (OIDC) + push (binding → facade、`--skip-duplicate`) → release Android + wait-published → release KMP + wait-published → monorepo tag (同 commit skip / 別 commit 失敗) → GitHub Release (`--generate-notes`、suffix で `--prerelease`、既存なら触らない) → `develop` の clone で 2.4 を実行し、`local-path-lint.py` / `identity-lint.py` / `readme-example-lint.py` を掛けてから commit + rebase + push (lint 失敗・push 拒否は警告) → Summarize (`if: always()`、2 枠の ID と警告) → drop 2 枠 (`if: failure()`) → 保存 ID の破棄 (→ Requirement: publish の順序 / Maven Central の 2 枠の deployment / 署名の生成確認 / nuget.org への push / tag と GitHub Release / 同じ version での再実行 / README と Skill の version 置換 / secrets と権限の範囲)
- [x] 4.6 反映待ち job (ubuntu、timeout 60 分、`if: !dry-run`) と smoke 4 job (`mode: smoke` + `version`、`if: !dry-run`) (→ Requirement: 反映待ちと smoke)
- [x] 4.7 `.github/release.yml` (分類 4 + 除外 2) を置き、ラベル `breaking` / `feature` / `fix` / `docs` / `kasane` / `ci` を `gh label create` で作る (→ Requirement: tag と GitHub Release)
- [x] 4.8 AGENTS.md の docs-refresh の行に「リリース時の version 置換だけは release workflow が行う (docs-refresh を経ない)」を足す (→ Requirement: README と Skill の version 置換)
- [x] 4.9 handbook `kasane/handbook/cross/release-procedure.md` を翻案 (design Decision 8 の構成。version 置換の手作業を外し、KMP 枠の失敗行と tag の手動削除手順を足し、必須 status check 10 件・default branch `develop` を書く)。`kasane/handbook/cross/index.md` に行を足す。`verification-ci.md` の job 表に release からの呼び出しを 1 行添える (→ Requirement: 同じ version での再実行 / main のマージ保護)

## 5. 検証 (Scenario の確認)

- [x] 5.1 スクリプトの自己テスト (`central-portal.sh --selftest`、`set-readme-version.py --selftest`、翻案元に自己テストがあるものはすべて) を手元で通す (→ Requirement: Maven Central の 2 枠の deployment / README と Skill の version 置換)
- [x] 5.2 `set-readme-version.py` を作業木で実行し、README 2 枚と Skill の対象行がすべて同じ値になりそれ以外が変わらないこと、行の形を崩したファイルで失敗することを確認して元に戻す (→ Scenario: 該当行が見つからなければ失敗する / publish 成功後に develop の README と Skill が新 version になる)
- [ ] 5.3 validate の負ケース: 不正 version 6 種・`develop` からの本番起動 (dry-run false) が checkout 前に失敗することを `develop` 上の dry-run とは別に確認する (workflow の `act` が使えなければ dispatch で実測) (→ Scenario: 不正な version 形式は早期に失敗する / main 以外からの本番起動は失敗する)
- [x] 5.3b 外部状態の再検査の負ケース: `github.run_attempt` の値と外部状態の組み合わせ (初回 + 外部状態あり + monorepo tag なし → 失敗) を、判定をスクリプト化して自己テストで示す (実レジストリでの再現はしない) (→ Scenario: 別 commit からの新規 dispatch は部分 publish を引き継がない)
- [ ] 5.4 `develop` から `dry-run: true` で `0.1.0-beta.1` を dispatch し、validate → 本体検証 5 → package 3 → 消費者 dry-run 4 が成功し、publish 以降が skip され、配信先 (配信リポジトリの tag・Central Portal の deployments・nuget.org) が変化しないこと、package-maui の nupkg 内 README が入力 version を持つこと、`develop` に commit が増えないことを確認する。各 job の所要時間を記録する (→ Scenario: dry-run 入力は publish 手前で止まる / dry-run は publish する配布物そのものを検証する / nupkg の README は入力 version を持つ)
- [ ] 5.5 初回リリース (群 7) の実行結果で、publish の順序 (配信 tag が KMP 発行より前に存在する、2 枠の VALIDATED → NuGet → release の順)、prerelease の Release、反映待ちと smoke 4 本の成功、`develop` への置換 commit を確認する。再実行の状態分岐は 2.1b の自己テストで判別し、実レジストリでの再実行は初回で失敗が起きた場合にだけ実測する (起きなければ「実レジストリでは未実測、分岐は自己テストで確認」と deviation に明記する) (→ Scenario: KMP の発行前に配信リポジトリの tag が存在する / NuGet push の後に 2 枠が release される / prerelease の suffix で prerelease になる / 公開レジストリから 4 形態が解決される / publish 成功後に develop の README と Skill が新 version になる)

## 6. GitHub 設定 (オーナーの手作業、手順書 4.9 に従う)

- [ ] 6.1 `main` を `develop` の先端から作成し、直後に branch protection を完全な payload で PUT する (必須 status check 10 件を `{"context": ..., "app_id": 15368}` 形式、PR 必須・承認数 0、force-push / 削除禁止、admin バイパス許容)。`develop` の保護は変えない。証跡 (`gh api` の応答) を evidence に残す (→ Requirement: main のマージ保護)
- [x] 6.2 配信リポジトリ `KsDialogs-SPM` に書き込み可の deploy key を登録し、秘密鍵を Environment secret `SPM_DEPLOY_KEY` へ (鍵ファイルは登録後 `trash`) (→ Requirement: secrets と権限の範囲)
- [x] 6.3 GitHub Environment `release` (deployment branch = `main` のみ、required reviewers なし) と secrets 7 件 (`MAVEN_CENTRAL_USERNAME` / `MAVEN_CENTRAL_PASSWORD` / `SIGNING_KEY` / `SIGNING_KEY_ID` / `SIGNING_PASSWORD` / `NUGET_USER` / `SPM_DEPLOY_KEY`) を登録する (`SIGNING_KEY` は登録直前に export し平文をディスクに残さない) (→ Requirement: secrets と権限の範囲)
- [x] 6.4 nuget.org の Trusted Publisher Policy (Repository `kamusoft/KsDialogs` / Workflow `release.yml` / Environment `release` / Glob `KsDialogs.*` / Scopes は push のみ) を登録する (→ Requirement: nuget.org への push)

## 7. 初回リリース

- [ ] 7.1 リリース PR (`develop` → `main`) を開き、`main` 宛て PR の検証 CI 10 job が起動して status check 名が必須 check と一致し緑になることを確認してマージする (phase-4 / 8 の申し送りの実動確認) (→ Scenario: main 宛て PR で 10 job が起動する / 検査未通過のマージ拒否)
- [ ] 7.2 `main` から `0.1.0-beta.1` を dispatch し (dry-run false)、完了まで見守る。失敗したら手順書「失敗したとき」に従い同じ version で再実行する (→ 群 5.5)
- [ ] 7.3 公開後の確認: GitHub Release (prerelease) / 配信リポジトリと monorepo の tag / Maven Central の 3 座標 / nuget.org の 3 ID / `develop` の README と Skill の version。所要時間 (各 job と壁時計) を agenda の申し送りへ記録する
- [ ] 7.4 蒸留への申し送り: cross/ADR-0024 の accepted 昇格 (Decision 3 の 1 文の追記を含む)、配布構成の concepts 化、docs-refresh 2 回目、KsSettingsView への R4 の逆流 (proposal Non-Goals)

## 備考

- workflow は github-workflow-skill、MAUI は csharp-impl-skill / maui-skill、Gradle は kotlin-impl-skill を参照する
- 翻案元の初回リリース後の修正 (upload 直後の `wait-validated`) は最初から取り込む (design Context)
- 群 6 は手元で検証できない。publish job の先頭 (deploy key・署名鍵・Portal 認証) が取り消せない操作より前に失敗を出す順序で受ける (design Risks)
- 群 5.3 の負ケースは、本番起動の制限を dry-run 側で緩めない形で確認する (dry-run は起動ブランチ制限を免除されるため、本番起動の失敗は `dry-run: false` を `develop` から dispatch して validate で止まることで示す)
