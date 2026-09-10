# 一致検証: add-release-workflow (001 回目)

**日付**: 2026-09-10
**対象**: 作業木の未コミット変更 (`.github/workflows/release.yml`・`.github/release.yml`・`scripts/release/` 9 本・`android/build.gradle.kts`・`kmp/ksdialogs-kmp/build.gradle.kts`・MAUI csproj 3 本・`maui/KsDialogs.Maui/Platforms/iOS/PlatformDialogContent.cs`・`AGENTS.md`・`kasane/handbook/cross/` 4 本)
**デルタスペック**: `specs/release-workflow/spec.md` (12 Requirement / 38 Scenario)、`specs/verification-ci/spec.md` (1 / 4)、`specs/maui-nuget-distribution/spec.md` (2 / 5)。計 **15 Requirement / 47 Scenario**

**判定**: **VALID**

- ✅ 一致 40 / ⚠️ deviation 記録済み 1 / ⏳ 実装あり・実行証跡は後続 6 / ❌ 0
- 実装の対応が無い Scenario は 1 件も無い。⏳ は GitHub Actions 上 (dispatch・実レジストリ・branch protection) でしか観測できないもので、対応する未完了タスク (5.3 / 5.4 / 5.5 / 6.1 / 群 7) が tasks.md に残っている

## 状態記号

| 記号 | 意味 |
|---|---|
| ✅ | 実装があり、自己テスト・実測証跡・構造の読み取りのいずれかで対応が確認できる |
| ⚠️ | 実装が spec と異なるが `deviation.md` に記録済み (合意済み差分) |
| ⏳ | 実装はあるが、実行証跡は GitHub Actions 上でしか取れず後続タスクに割り当てられている |
| ❌ | 実装の対応が無い、または未記録の乖離 |

## 本検証で実行したもの

| 実行 | 結果 |
|---|---|
| `scripts/release/*.sh --selftest` 8 本 | 全件「失敗なし」(central-portal 52 / central-resume 20 / check-distribution-tag 9 / check-nuget-version 16 / check-resume-eligibility 38 / check-signatures 12 / compare-maven-artifacts 12 / wait-for-registries 10) |
| `python3 scripts/release/set-readme-version.py --selftest` | 失敗なし |
| `python3 scripts/release/set-readme-version.py --check 9.9.9` | 実物 14 ファイル 26 行をすべて検出 (「見つからない」は 0 件。対象行の列挙が現在の作業木と一致) |
| `actionlint .github/workflows/release.yml` | error / warning 0 件。shellcheck の info / style 3 件のみ (SC2012 × 2、SC2001 × 1)。同種の指摘は既存 workflow 7 本にもあり (`ci.yml` の SC2001、`verify-*.yml` の SC2012 × 6)、release.yml 固有の退行ではない |
| lint 7 本 (`local-path` / `identity` / `comment-policy` / `scenario-id-coverage` / `ci-skip` / `readme-example` / `doc-structure`) + `scripts/spm-snapshot/sync-snapshot-test.sh` | CI lint job の 8 検査に相当する 7 本はすべて exit 0。`doc-structure-lint.py` は既存の助言指摘のみで、本 change が触れたファイルへの指摘は 0 件 (CI lint job の検査集合にも含まれない) |
| `android/gradlew help` / `kmp/gradlew help` (`--offline`) | 両方 exit 0。`publishToMavenCentral(automaticRelease = false)` を含む build script が評価できる |
| `dotnet build maui/KsDialogs.Maui/KsDialogs.Maui.csproj -c Release -f net10.0-ios -t:Rebuild` | 成功・**警告 0 件**、`bin/Release/net10.0-ios/KsDialogs.Maui.xml` を生成。CS1572 (deviation の付随修正の対象) と CS1591 がいずれも出ないことを本検証で再現 |
| GitHub の読み取り照会 (`gh api` / `gh label list`) | ラベル 6 件 (`breaking` / `feature` / `fix` / `docs` / `kasane` / `ci`) 実在、Environment `release` が deployment branch policy = `main` のみ + secrets 7 件、配信リポジトリの deploy key が書き込み可、`main` は未作成 (6.1 未実施と整合)、既定ブランチは `develop` |

MAUI / Android / KMP の単体テスト一式は再実行していない。本 change の製品コードへの変更は doc コメントの移動 1 箇所とパッケージング / 発行設定のみで、挙動を持つコードの差分が無いため (判別力のある確認として、その doc コメントを含む TFM の Rebuild を上表のとおり実行した)。

## 対応表 1: release-workflow (12 Requirement / 38 Scenario)

### Requirement: 手動起動と入力の検証

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| 不正な version 形式は早期に失敗する | `.github/workflows/release.yml:103-116` (checkout より前の step。正規表現 `:112-113` が先頭ゼロと 3 種の prerelease だけを通す) | 実行証跡は後続 (tasks 5.3) | ⏳ |
| main 以外からの本番起動は失敗する | `.github/workflows/release.yml:117-120` (`dry-run` が true 以外かつ `github.ref` が `refs/heads/main` でなければ失敗。checkout より前) | 実行証跡は後続 (tasks 5.3) | ⏳ |
| 別 commit を指す同名 tag があれば失敗する | `.github/workflows/release.yml:129-143` | 実行証跡は後続 (tasks 5.4 / 7.2 は正常系)。所見 (1) 参照 | ⏳ |
| 配信リポジトリの同名 tag は publish の前に内容で判定する | `.github/workflows/release.yml:148-170` (validate job 内、package 段より前)、`scripts/release/check-distribution-tag.sh:41-` | 自己テスト 9 件 (「内容が同じ tag は match」「remote の tag が別の内容を指していれば失敗する」「未追跡ファイルが増えていれば失敗する」) | ✅ |
| dry-run 入力は publish 手前で止まる | `release.yml:535` (publish `if: !dry-run`)、`:1416` (反映待ち)、`:1435` `:1444` `:1453` `:1462` (smoke 4 本)。validate / package / dry-run 段は条件なしで実行 | 実行証跡は後続 (tasks 5.4) | ⏳ |

### Requirement: 段の構成と順序

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| テストか dry-run が 1 つでも失敗すれば publish しない | `release.yml:524-534` (`needs` に validate + 本体検証 5 + 消費者 dry-run 4)。書き込み手段を持つ step はすべて publish job 内 | 構造の読み取り。`needs` の既定 (成功時のみ実行) による | ✅ |
| 同時に起動した 2 つの実行は直列になる | `release.yml:47-49` (`concurrency: group: release` / `cancel-in-progress: false`)。後続の実行は `Re-check external state` (`:622-716`) が monorepo tag = 同 commit を検出して `skip-all` になる (`scripts/release/check-resume-eligibility.sh:101-105`) | `check-resume-eligibility.sh --selftest` 38 件に `same-commit → skip-all` を含む。待ち行列の挙動は GitHub Actions 側の契約 | ✅ |
| dry-run は publish する配布物そのものを検証する | 消費者 dry-run は package 段の artifact を受け取る (`release.yml:483-519`)。publish は同じ artifact を download して push (`:770-775` → `:1158-1172`)。iOS は同じ同期経路 (`:227-238` / `:851-867`)、Android は再ビルド + 同一性比較 (`:940-946`)、KMP は publish 段で生成 (`:1081-1101`) | `compare-maven-artifacts.sh --selftest` 12 件、`evidence/android-rebuild-identity.txt` | ✅ |

### Requirement: Android 成果物の同一性

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| 再ビルドの差異で upload を止める | `release.yml:944-945` (upload step `:948` の前に `compare-maven-artifacts.sh` を実行)、`scripts/release/compare-maven-artifacts.sh` (pom / module は byte 比較、aar / jar はエントリ名と内容、`.asc` / checksum / `maven-metadata*` は対象外)。package 段 (`:256` macos-26 / JDK 17) と publish job (`:539` macos-26 / JDK 17) は同じランナー OS・JDK・commit | 自己テスト 12 件 (差異の検出・出力、無視すべき差異、空ツリーを緑にしない)、`evidence/android-rebuild-identity.txt` (同一 commit 2 回のビルドで 10 成果物一致) | ✅ |

### Requirement: publish の順序

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| KMP の発行前に配信リポジトリの tag が存在する | step 順: 配信 tag push `:1011-1029` → KMP 発行 `:1033`。KMP の Swift 参照は既定の https URL + 入力 version の exact (`:1089-1093` のコメントと `kmp/ksdialogs-kmp/build.gradle.kts` の既定) | 実行証跡は後続 (tasks 5.5 / 7.2) | ⏳ |
| KMP の検証失敗では取り消せない操作に進まない | KMP の `wait-validated` (`:1133-1146`) が VALIDATED 以外で失敗 → 後続の NuGet push (`:1158`) / release (`:1177` `:1188`) / monorepo tag (`:1199`) / Release (`:1219`) は実行されず、`if: failure()` の drop (`:1338-1361`) が 2 枠を drop する。配信 tag は既に push 済みで残る | `central-portal.sh --selftest` (wait-validated の状態返却と上限、drop は VALIDATED / FAILED のみ)、`evidence/central-portal-upload-probe.txt` (実 upload → status → drop → NOT_FOUND) | ✅ |
| 途中で失敗すれば monorepo の tag は作られない | monorepo tag は NuGet push と Maven release の後 (`:1199`)。前段の失敗で step に到達しない | 構造の読み取り (step 順序と `set -euo pipefail`) | ✅ |
| スナップショット commit は Android の upload より前に push される | commit push `:851-867` → Android upload `:948`。tag は `:1011` (Android upload より後) | 構造の読み取り | ✅ |

### Requirement: Maven Central の 2 枠の deployment

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| upload 後は両枠とも保留状態で止まる | `android/build.gradle.kts:118` / `kmp/ksdialogs-kmp/build.gradle.kts:46` の `publishToMavenCentral(automaticRelease = false)`。upload 直後に `wait-validated` (`:992` / `:1133`) を置き、release は NuGet push の後 (`:1177` `:1188`) | `evidence/gradle-publish-tasks.txt` (task graph に `publishAndRelease` 系が入らないこと)、`evidence/central-portal-upload-probe.txt` (実 upload が USER_MANAGED で VALIDATED 停止)、本検証の `gradlew help` (両 build script が評価できる) | ✅ |
| NuGet push の後に 2 枠が release される | `:1158-1172` (push) → `:1177-1186` (Android release + wait-published) → `:1188-1197` (KMP release + wait-published) | 実行証跡は後続 (tasks 5.5 / 7.2) | ⏳ |
| 失敗時に保留 deployment が残らない | `:1338-1361` (`if: failure()` で 2 枠を drop し、`status` が NOT_FOUND のときだけ引き継ぎを断つ)、`scripts/release/central-portal.sh` の `cmd_drop` (VALIDATED / FAILED のみ削除) | `central-portal.sh --selftest` (drop の状態別分岐)、`evidence/central-portal-upload-probe.txt` (drop 後に NOT_FOUND) | ✅ |
| release の応答が失われても再実行で整合する | `scripts/release/central-resume.sh` (`PUBLISHING → wait-published`、release を再送しない)、`release.yml:897-929` / `:1049-1079` の `case`、ID の引き継ぎ (`:737-761` / `:978-986` / `:1124-1131` / `:1366-1376`) | `central-resume.sh --selftest` 20 件 (8 状態すべての写像と resume の照会経路)、`evidence/deployment-id-handoff-guard.txt` | ✅ |
| 検証中のまま release に進まない | `central-portal.sh` の `wait-validated` (PENDING / VALIDATING を待ち、上限超過で失敗)、呼び出し側 `:1000-1005` / `:1141-1146` が VALIDATED 以外を失敗にする | `central-portal.sh --selftest` (「PENDING / VALIDATING を経て VALIDATED を返す」「上限を過ぎれば失敗する」) | ✅ |

補足 (Requirement 本文のうち Scenario に紐づかない SHALL): 「両枠の deployment ID は upload の時点で得られなければ失敗し」は `:955-959` / `:1103-1107`、「同じ実行の再実行から枠ごとに参照できる形で保存する」は artifact `central-deployment-id` の 2 ファイル構成 (`:750-761`)、「再実行時の状態別分岐」は `central-resume.sh` の対応表と一致する。

### Requirement: 署名の生成確認

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| KMP の署名が 1 件欠けても upload しない | `:1096` (upload `:1099` の前)、`scripts/release/check-signatures.sh` (対象拡張子 aar / pom / jar / module / klib / json、必須 publication 5 件) | 自己テスト 12 件 (「cinterop klib の `.asc` が 1 件欠ければ失敗する」「SwiftPM 連携メタデータ (json) の `.asc` が欠ければ失敗する」「欠けたファイル名を出力する」) | ✅ |
| 署名鍵が渡っていなければ upload しない | `:943` (Android 枠の署名検査。upload `:948` と配信 tag push `:1011` の前) | 自己テスト (「Android の aar の `.asc` が欠ければ失敗する」)。step 順序から tag push に到達しないことを読み取り | ✅ |

### Requirement: nuget.org への push

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| 3 パッケージが同じ version で公開される | `:1149-1154` (`NuGet/login` の OIDC。長期 API key の secret を持たない) → `:1158-1172` (binding iOS → binding Android → facade の順に nupkg / snupkg を対で push、`--skip-duplicate`) | 実行証跡は後続 (tasks 7.2 / 7.3)。Trusted Publisher Policy の登録は tasks 6.4 済み | ⏳ |
| binding の push 失敗で facade は公開されない | `:1165-1172` の直列ループ (`set -euo pipefail`) により facade は最後。再実行時は `--skip-duplicate` で成功済みを skip | 構造の読み取り | ✅ |

### Requirement: tag と GitHub Release

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| prerelease の suffix で prerelease になる | `:1229-1232` (`case "${KS_VERSION}" in *-*)` で `--prerelease`)、`:1199-1214` (monorepo tag)、`:1011-1029` (配信リポジトリ tag) | 実行証跡は後続 (tasks 7.2 / 7.3)。分類設定は `.github/release.yml:6-28` (除外 `kasane` / `ci`、分類 `breaking` / `feature` / `fix` / `docs` + `*`)、ラベル 6 件の実在は本検証で確認 | ⏳ |
| 正式版は prerelease にならない | `:1231` (suffix 無しの分岐) | 同上 (初回は prerelease のため、正式版の実行証跡は将来のリリース) | ⏳ |

### Requirement: 同じ version での再実行

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| KMP の失敗後に同じ version で埋める | 配信 tag は `check-distribution-tag.sh` の `match` で skip (`:1018-1022`)、Android 枠は `central-resume.sh` の状態分岐 (`:897-968`)、KMP は `:1033` から続行 | `check-distribution-tag.sh` / `central-resume.sh` の自己テスト。実レジストリでの再実行は tasks 5.5 の但し書き (初回で失敗が起きた場合のみ実測) | ✅ |
| 部分 publish を同じ version で埋める | 配信 commit / tag は差分なし・`match` で skip (`:861-866` / `:1018-1022`)、NuGet は `--skip-duplicate` (`:1170`)、Maven は `central-resume.sh` の `VALIDATED → skip-upload` (release へ)、以降 monorepo tag → Release → `develop` 反映 | `central-resume.sh --selftest` 20 件、`check-distribution-tag.sh --selftest` 9 件 | ✅ |
| 別 commit からの新規 dispatch は部分 publish を引き継がない | `:622-716` (外部状態の再検査) と `scripts/release/check-resume-eligibility.sh` の `decide` (初回 + 外部状態あり + tag 無し → `fail-new-dispatch`、印の無い再試行 → `fail-no-marker`)。判定は書き込みの前で、失敗時は `set -e` により step が落ちる | `check-resume-eligibility.sh --selftest` 38 件 (試行回数 × tag 状態 × 外部状態 × 印の組み合わせ)。tasks 5.3b 済み | ✅ |
| 放棄した version の番号は再利用されない | `kasane/handbook/cross/release-procedure.md` の「version を放棄するとき」節 (欠番にする・tag 削除は任意の後片付け)。実装側の歯止めは validate / publish の tag 照合 (`:129-143` / `:1203-1211`) | 運用規約としての記述。実測対象ではない | ✅ |
| 全て完了済みの再実行は何も重複させない | `Re-run failed jobs` では publish job は再実行されない。再実行された場合も `decide` が `skip-all` を返し、publish の書き込み step (`if: steps.state.outputs.publish-needed == 'true'`) がすべて skip される | `check-resume-eligibility.sh --selftest` (`same-commit → skip-all`)。tag より後の後処理 2 step を走らせる点は deviation 5 件目に記録済み | ✅ |

### Requirement: README と Skill の version 置換

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| nupkg の README は入力 version を持つ | `:395-398` (pack `:403` の前に作業木で置換、commit しない)、`maui/KsDialogs.Maui/KsDialogs.Maui.csproj:34` (ルート `README.md` を同梱) | `set-readme-version.py --selftest`、`evidence/readme-version-worktree.txt`。nupkg 内 README の実測は後続 (tasks 5.4) | ✅ |
| publish 成功後に develop の README と Skill が新 version になる | `:1243-1300` (Release 作成 `:1219` の後。`git worktree` で `develop` の先端を作り、置換 → `local-path-lint.py` / `identity-lint.py` / `readme-example-lint.py` → commit → rebase → push) | `set-readme-version.py --selftest` / `--check` で 26 行の検出を確認。worktree を使う点は deviation 3 件目に記録済み。実行証跡は後続 (tasks 5.5 / 7.3) | ✅ |
| 該当行が見つからなければ失敗する | `scripts/release/set-readme-version.py` の `TARGET_FILES` (14 ファイル・種別ごとの期待本数) と、本数不一致で何も書き換えずに失敗する経路 | 自己テスト 37 項目 (行の形を崩したファイルで失敗すること・検出できなかった対象の出力を含む)、`evidence/readme-version-worktree.txt`、本検証の `--check 9.9.9` (26 行すべて検出) | ✅ |
| develop への push の競合は release を失敗にしない | `:1248-1252` の `warn()` (警告を出して `exit 0`)、`:1294-1299` (fetch / rebase / push の各失敗を warn 化)、`:1401-1403` (Summarize に警告行) | 構造の読み取り。`handbook cross/release-procedure.md:175` に同じ契約 | ✅ |

### Requirement: 反映待ちと smoke

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| 反映を待ってから smoke する | `:1413-1428` (`wait-for-registries` job、timeout 60 分) → smoke 4 本が `needs: wait-for-registries` (`:1434` ほか)。`scripts/release/wait-for-registries.sh` の待ち対象は Maven 7 件 (Android 2 + KMP 5 publication) + nuget.org 3 件 = 10 件 | 自己テスト 10 件 (URL の組み立て、部分反映で未反映の対象を出力、上限で失敗) | ✅ |
| 公開レジストリから 4 形態が解決される | smoke 4 job が `verify-consumer-*.yml` を `mode: smoke` + version で呼ぶ (`:1432-1466`)。参照先の切り替えは既存の消費者検証 workflow (本 change では未変更) | 実行証跡は後続 (tasks 5.5 / 7.2 / 7.3) | ⏳ |
| smoke 失敗でも tag は残る | tag / Release の作成は publish job (`:1199` / `:1219`) で完了し、smoke 側に取り消し step は無い | 構造の読み取り (取り消し経路の不在) | ✅ |

### Requirement: secrets と権限の範囲

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| publish 以外の job は書き込み手段を持たない | `:40-41` (workflow 既定 `contents: read`)、`:542-547` (publish だけが `environment: release` と `contents: write` + `id-token: write`)。`secrets: inherit` は workflow 全体で 0 件 (コメント `:482` のみ)。呼ばれる `verify-*.yml` 9 本もすべて `permissions: contents: read` | 本検証の grep (secrets 指定なし・再利用 workflow 9 本の permissions)、`handbook cross/release-procedure.md:89-113` (secrets 7 件の置き場) | ✅ |
| main 以外から Environment は参照できない | GitHub の Environment `release` の deployment branch policy = `main` のみ (tasks 6.3 済み)。workflow 側は `:542` の `environment: release` | 本検証の読み取り照会で `{"name":"main","type":"branch"}` と secrets 7 件を確認 | ✅ |

## 対応表 2: verification-ci (1 Requirement / 4 Scenario)

Requirement「main のマージ保護」の実体は GitHub の branch protection 設定 (tasks 6.1、未実施) であり、リポジトリ側の対応物は **手順書** `kasane/handbook/cross/release-procedure.md` の「main の作成と保護」節 (`:32-75`。必須 status check 10 件を `{"context": ..., "app_id": 15368}` 形式で PUT する完全な payload) と、**既存の** `.github/workflows/ci.yml` の 10 job (本 change では未変更) である。

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| 検査未通過のマージ拒否 | `release-procedure.md:32-75` (必須 status check 10 件 + PR 必須・承認数 0 の payload) / `.github/workflows/ci.yml` の 10 job (変更なし) | 実行証跡は後続 (tasks 6.1 の `gh api` 応答、7.1 のリリース PR) | ⏳ |
| main への直 push の拒否 | 同上 (payload の `enforce_admins` / `allow_force_pushes` などブランチ保護一式) | 実行証跡は後続 (tasks 6.1) | ⏳ |
| main 宛て PR で 10 job が起動する | `.github/workflows/ci.yml` の job 名 (`lint` と、`ios` / `android` / `android-instrumented` / `kmp` / `maui` / `consumer-{ios,android,maui,kmp}`) と、呼ばれる `verify-*.yml` の内部 job 名 `verify`。status check 名は `<job> / verify` となり、手順書の必須 check 10 件と一致することを本検証で照合した | 実行証跡は後続 (tasks 7.1) | ⏳ |
| develop は保護を変えない | `release-procedure.md:28` (`develop` には必須 status check も PR 必須も付けない)。既定ブランチが `develop` であることは本検証の読み取り照会で確認 | 実行証跡は後続 (tasks 6.1) | ⏳ |

## 対応表 3: maui-nuget-distribution (2 Requirement / 5 Scenario)

### MODIFIED Requirement: 3 パッケージの構成と内容

本 change の差分は XML ドキュメントに関する 2 文のみ。他の全文 (facade の TFM group 依存・同梱物・binding の中身と Description) は `2026-09-08-add-maui-nuget-distribution` で導入・検証済みで、本 change の diff は触れていない。

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| 3 パッケージのローカル pack | `maui/Directory.Build.props` と 3 csproj (本 change の差分は `GenerateDocumentationFile` の 3 行のみ) | `evidence/maui-xml-doc-pack.txt` (facade の pack rc=0、3 TFM の `lib/` 構成)。全文の照合は `kasane/changes/archive/2026-09-08-add-maui-nuget-distribution/verify-001.md` | ✅ |
| binding パッケージの同梱物と説明 | 同上 (binding 2 件の csproj の差分は `GenerateDocumentationFile>false` の 1 行のみ) | 同上 | ✅ |
| XML ドキュメントは facade の 3 TFM に揃う | `maui/KsDialogs.Maui/KsDialogs.Maui.csproj:29` (`true`)、`maui/macios/KsDialogs.Binding.iOS/KsDialogs.Binding.iOS.csproj:30` / `maui/android/KsDialogs.Binding.Android/KsDialogs.Binding.Android.csproj:35` (`false` の明示) | `evidence/maui-xml-doc-pack.txt` (facade 3 TFM に `KsDialogs.Maui.xml`、iOS binding に `.xml` 無し、Android binding は SDK の binding 用 targets により `.xml` が残る)。本検証で facade iOS TFM の Rebuild が警告 0 件・`.xml` 生成を再現 | ⚠️ deviation 記録済み (`deviation.md` 2 件目: Android binding の `.xml` 残留を受け入れ) |

### ADDED Requirement: 発行版の nupkg 名の検査

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| 開発既定値のままの nupkg は push されない | `release.yml:425-468` (pack 直後。`<ID>.<version>.nupkg` / `.snupkg` の 6 ファイルの存在と、別 version の紛れ込みを検査) | 構造の読み取り。実行証跡は後続 (tasks 5.4) | ✅ |
| artifact の取り違えを push 前に止める | `release.yml:780-822` (publish job の artifact download 直後。push は `:1158`) | 同上。検査位置は deviation 4 件目に記録済み (「push 直前」→「download 直後 = 配信リポジトリへの push より前」) | ✅ |

## 追加検査

| 検査 | 結果 |
|---|---|
| tasks.md の完了状況 | 37 タスク中 30 が `[x]`。未完了は 5.3 / 5.4 / 5.5 / 6.1 / 7.1〜7.4 の 8 件で、いずれも GitHub Actions 上または GitHub 設定・初回リリースを要するもの |
| 虚偽チェックの有無 | **無し**。`[x]` のうち手元で確認できるものをすべて実物と突き合わせた (1.1〜1.5 → `evidence/` 4 本、2.1〜2.5 → スクリプト 9 本の自己テストと handbook の新設節、3.0〜3.2 → Gradle / csproj / workflow の該当行、4.1〜4.9 → workflow・`.github/release.yml`・AGENTS.md・handbook 4 本、4.7 のラベル作成 → GitHub 上に 6 件実在、5.1 / 5.2 / 5.3b → 自己テストと証跡、6.2 / 6.3 / 6.4 → deploy key・Environment・secrets 7 件を読み取りで確認) |
| 逆流検査 (足場の凍結) | **無し**。`proposal.md` / `design.md` / `specs/**` は propose 時の commit `48e5cba` 以降変更されておらず、作業木にも差分が無い (`git status` で change 配下の変更は `tasks.md` のみ、新規は `deviation.md` / `evidence/` / review 3 本) |
| 未記録乖離 | **無し**。diff の全ファイルが Scenario または `deviation.md` の記録に対応する。`kasane/lessons/inbox/tool-premise-decision-needs-existence-probe.md` の count 更新は教訓機構 (ksn-lesson) の捕捉で、Requirement を持たない運用記録 |
| 付随修正 | 1 件 (`maui/KsDialogs.Maui/Platforms/iOS/PlatformDialogContent.cs` の `<param>` タグ移動)。`deviation.md` 1 件目に記録済み。本検証の Rebuild で警告 0 件を再現し、公開面に触れていないことを diff で確認 (`:139-141` の削除と `:152-153` の追加のみ) |
| UI 変更 | 無し (`ui/` アーティファクトを持たない change) |
| テストの成功 | 本 change のテストである自己テスト 9 本と CI lint 相当 7 本 + 同期スクリプト自己テストがすべて成功。製品コードのビルド (facade iOS TFM Rebuild) も警告 0 件で成功 |
| 追跡外の生成物 | `scripts/release/__pycache__/` は `.gitignore:71` に該当し追跡されない |

## 所見 (判定の根拠ではない)

1. **monorepo tag の別 commit 検出だけが自己テストを持たない**。同種の負ケースのうち、配信リポジトリ tag の内容差異は `check-distribution-tag.sh --selftest`、再ビルドの差異は `compare-maven-artifacts.sh --selftest`、再実行の可否は `check-resume-eligibility.sh --selftest` で判別できる。monorepo tag の照合 (`release.yml:129-143` と `:1203-1211`) は workflow に直書きの shell で、対応する自己テストも実行計画 (tasks) も無い。実装は存在するため INVALID の根拠にはしないが、蒸留時の申し送り候補。
2. **⏳ 6 件は tasks 5.3 / 5.4 / 5.5 / 6.1 / 群 7 に割り当て済み**で、宙に浮いた Scenario は無い。tasks 5.5 は「実レジストリでの再実行は初回で失敗が起きた場合にだけ実測し、起きなければ deviation に明記する」と定めているため、初回リリース後にその追記が必要になる可能性がある。
3. `actionlint` の shellcheck info / style 3 件は既存 workflow と同じ種別で、CI lint job の検査集合にも含まれない (指摘としては扱わない)。
