# 検証結果: add-verification-ci (001 回目)

**日付**: 2026-09-08
**判定**: VALID

デルタスペック `specs/verification-ci/spec.md` の **Requirement 10 件 / Scenario 27 件**すべてについて、実装 (`.github/workflows/` 6 本・`global.json`・`maui/Directory.Packages.props`・`maui/nuget.config`・`maui/` の 4 csproj・`samples/maui` の csproj・handbook 2 文書) との対応を突き合わせた。❌ (未記録の欠落・乖離) は 0 件。⚠️ は `deviation.md` に記録済みの 2 件。

このデルタスペックは CI の構成そのものを契約にしているため、Scenario の担保は「実装 (workflow の定義)」+「負ケースの単体確認 (`evidence/ci-step-negative-cases.md`)」の 2 段で見た。GitHub 上でしか成立を確認できない Scenario 5 件は、実装の一致は取れているが**実動未確認**であり、下表の「実動確認」列と後段の一覧に分けて示す。

## 対応表

「実装」は該当の workflow / 設定ファイルの定義位置、「テスト」は本変更で得られている確認手段 (ステップ単体の負ケース確認は `evidence/ci-step-negative-cases.md` の節番号、実行系の再確認は本検証で実施したもの)。

### Requirement: CI の起動条件

| Scenario | 実装 | テスト | 実動確認 | 状態 |
|---|---|---|---|---|
| develop への push で本体検証と lint が起動する | `.github/workflows/ci.yml:21-23` (push branches)、`125-153` (5 job)、`155-161` (lint、`if:` なし) | — | tasks 5.1 (未) | ✅ |
| main 宛ての PR で起動する | `ci.yml:18-20` (pull_request branches)、`128`・`134`・`140`・`146`・`152` の `github.event_name == 'pull_request'` 分岐 | — | phase-9 申し送り (未) | ✅ |
| 記録だけの push では lint だけが走る | `ci.yml:96-109` (`kasane/*`・`.github/ISSUE_TEMPLATE/*`・`.github/CONTRIBUTING*.md` の case と `source=false`)、5 job の `if:` | review-001 が合成 git リポジトリで 5 ケース実行 (記録だけ → `source=false`) | tasks 5.2 (未) | ✅ |
| 記録とソースが混ざった push では全 job が走る | `ci.yml:102` (`*) emit_true`) | 同上 (混在 → `source=true`) | tasks 5.2 (未) | ✅ |
| 連続する push で古い実行が打ち切られる | `ci.yml:31-33` (`concurrency.group` = workflow + PR 番号 / ref、`cancel-in-progress: true`) | — | tasks 5.2 (未) | ✅ |

補足: 5 job の `if:` は `!cancelled() && (github.event_name == 'pull_request' || needs.changes.outputs.source != 'false')`。`changes` が失敗して `outputs.source` が空文字になる場合も実行側へ倒れるため、「PR では常に実行する」が job グラフの側でも成立する (review-001 Major 2 の修正)。

### Requirement: main 宛て PR の head 制限

| Scenario | 実装 | テスト | 実動確認 | 状態 |
|---|---|---|---|---|
| develop 以外からの PR は失敗する | `ci.yml:178-193` (head リポジトリ検査 → head ブランチ名検査、いずれも `::error::` + `exit 1`) | `evidence/ci-step-negative-cases.md` 5.6 (自リポジトリ develop = 0 / `feature/x` = 1 / fork の develop = 1 / fork の main = 1) | ステップの `if:` (`base_ref == 'main'`) は phase-9 (未) | ✅ |

### Requirement: platform workflow の再利用契約

| Scenario | 実装 | テスト | 実動確認 | 状態 |
|---|---|---|---|---|
| 別 workflow からの呼び出し | `verify-ios.yml:10-11`・`verify-android.yml:9-10`・`verify-android-instrumented.yml:12-13`・`verify-kmp.yml:10-11`・`verify-maui.yml:10-11` の `on: workflow_call` (入力を持たないため呼び出し側の指定は `uses:` のみ) | 6 workflow の YAML パース (本検証で実施) | release workflow は phase-9 (未) | ✅ |
| status check 名が固定される | 呼び出し側の job 名 `ci.yml:126`・`132`・`138`・`144`・`150`・`156`、呼ばれた側の job 名 `verify-*.yml` の `jobs.verify` / `name: verify` | — | phase-9 (未) | ⚠️ deviation 記録済み |

⚠️ の内容: 変更検出 job (`changes`、`ci.yml:39-40`) が 7 本目の check run として報告される。必須 check として固定するのは 6 件のままで `changes` は補助 check、という整理が `deviation.md` に記録され、phase-9 の必須 check 一覧へ申し送られている。

### Requirement: iOS の検証

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Simulator 全件実行 | `verify-ios.yml:50-84` (UDID 解決)、`86-93` (`xcodebuild test -scheme KsDialogs` を Simulator destination で、`swift test` は不使用)、`151-167` (summary 出力) | evidence 5.3 (実ログ 277 件で合算 277 / exit 0) | ✅ |
| 0 件実行の検出 | `verify-ios.yml:141-149` (2 系統とも件数行なし / 合算 0 で `reason` を立てる)、`169-171` (`exit 1`) | evidence 5.3 (両方 0 件 = 1 / 件数行の欠落 = 1 / ログ不在 = 1) | ✅ |
| Swift Testing だけの構成で件数が取れる | `verify-ios.yml:117` (`Test run with N tests`)、`130` (加算)、`141` (XCTest 側 `None` を 0 として合算) | evidence 5.3 (Swift Testing のみ = exit 0、合算 277 件) | ✅ |

### Requirement: Android の検証と実行件数の担保

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 全件実行と件数表示 | `verify-android.yml:64-68` (`./gradlew test`)、`87-122` (settings.gradle.kts の include から `src/test` を持つ module を導出)、`148-158` (module 別表と合計を summary へ) | evidence 5.3 (実リポジトリ = 期待 module `:ksdialogs` / 68 件 / exit 0)、本検証で `./gradlew --no-daemon --console=plain test` = BUILD SUCCESSFUL | ✅ |
| 0 件実行の検出 | `verify-android.yml:131-133` (XML 欠落)、`145-146` (`tests == 0`)、`170-173` (`exit 1`)。導出集合が空なら `104-120` で検査自体を失敗 | evidence 5.3 (XML 欠落 / `tests="0"` / 導出集合が空 / include を導出できない の 4 ケースすべて exit 1) | ✅ |
| 依存グラフ違反の検出 | `verify-android.yml:64-68` (`test` の実行に `:ksdialogs:verifyNoDeclarativeUiDependency` が乗る) | evidence 5.8 (Compose 依存を一時投入 → exit 1 / `verifyNoDeclarativeUiDependency FAILED`)。本検証でも同タスクが `test` に乗ることを再確認 | ✅ |

### Requirement: Android instrumented の検証

| Scenario | 実装 | テスト | 実動確認 | 状態 |
|---|---|---|---|---|
| Emulator で全件実行 | `verify-android-instrumented.yml:52-58` (KVM)、`77-87` (API 36 / `google_apis` / x86_64 で `connectedDebugAndroidTest`)、`102-140` (`src/androidTest` を持つ module の導出)、`177-184` (module 別の tests / skipped / failures / 実行数を summary へ) | evidence 5.3 (合成ツリーで両 module に結果あり → 実行数合計 323 件 / exit 0) | Emulator 実行は tasks 5.1 (未) | ✅ |
| 0 件実行の検出 | `verify-android-instrumented.yml:154-156` (XML 欠落)、`168-175` (`tests - skipped <= 0`)、`196-199` (`exit 1`)。導出集合が空なら `134-138` で失敗 | evidence 5.3 (片方の XML 欠落 / 全件 skip / 導出集合が空 の 3 ケースすべて exit 1) | — | ✅ |

### Requirement: KMP の検証

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 両ターゲットの全件実行と metadata compile | `verify-kmp.yml:83-86` (`allTests compileCommonMainKotlinMetadata compileIosMainKotlinMetadata` を 1 回の Gradle 起動で)、`129-136` (ターゲット別表と合計を summary へ)。ObjC 公開面の検査と `api-surface-check` のコンパイルは同じ実行に含まれる | evidence 5.3 (実 XML で testAndroidHostTest 76 / iosSimulatorArm64Test 75 / 合計 151 件 / exit 0) | ✅ |
| metadata compile の失敗を検出する | `verify-kmp.yml:86` (job のコマンドに 2 つの metadata compile タスクを含む) | evidence 5.4 (job のコマンド全体 = exit 1、`compileIosMainKotlinMetadata` 単体 = exit 1) | ⚠️ deviation 記録済み |
| 0 件実行の検出 | `verify-kmp.yml:112-114` (XML 欠落)、`126-127` (`tests == 0`)、`148-151` (`exit 1`) | evidence 5.3 (片方の XML 欠落 / 片方 `tests="0"` / 両方欠落 の 3 ケースすべて exit 1) | ✅ |

⚠️ の内容: Scenario が求める「ターゲット本体は通るが metadata compile だけが失敗する」状態は、現行 Kotlin (2.4.x) では `compileKotlinIosSimulatorArm64` が先に同じ違反を検出するため構成できなかった。job のコマンドが失敗すること・`compileIosMainKotlinMetadata` 単体が同じ違反を検出することの 2 点で機構を確認した旨が `deviation.md` に記録済み。

### Requirement: MAUI の検証

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| facade テスト・橋渡しテスト・配線のコンパイル検証 | `verify-maui.yml:90-96` (facade の `dotnet test` / TRX)、`143-153` (iOS binding・Android binding・facade の net10.0-ios / net10.0-android の 4 ビルド)、`155-158` (Android 橋渡し `:ksdialogs-maui-bridge:test`)、`246-255` (iOS 橋渡し `xcodebuild test` を Simulator destination で)、`124-131`・`191-197`・`314-324` (3 つの件数を summary へ)。検証ホストの起動は含まない | evidence 5.3 (facade 実 TRX 155 件 / Android 橋渡し実 XML 31 件 / iOS 橋渡しは verify-ios.yml の検査と抜き出し結果が byte 一致)、本検証で `dotnet test` = 155 件成功 | ✅ |
| 0 件実行の検出 | `verify-maui.yml:138-140` (facade total 0)、`186-189` (Android 橋渡しの XML 欠落 / `tests == 0`)、`306-312` (iOS 橋渡しの 2 系統合算 0 / 件数行欠落)、各 `sys.exit(1)` | evidence 5.3 (facade: TRX の total=0 / TRX 不在。Android 橋渡し: XML なし / `tests="0"`。iOS 橋渡し: iOS 側 6 ケースと同一) | ✅ |

補足: 3 本の件数検査は `verify-maui.yml:102`・`163`・`260` で `!cancelled() && steps.<test-step-id>.conclusion != 'skipped'` に条件付けられている。テスト**失敗**時 (`conclusion == 'failure'`) は従来どおり走るため、Scenario「0 件実行の検出」の担保は弱まっていない。

### Requirement: lint の検証

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 違反の検出 | `ci.yml:163-167` (gitleaks の版と SHA-256)、`198-208` (checksum 検証つき導入)、`213-237` (secret scan)、`239-240` (local-path-lint)、`242-243` (identity-lint)、`245-246` (comment-policy-lint)、`250-251` (scenario-id-coverage) | evidence 5.5 (4 検査それぞれに違反を投入して exit 1 と該当行の出力、gitleaks は検証用ダミーで `leaks found: 1`)。本検証でベースライン 4 本 + secret 以外を再実行し全て exit 0 | ✅ |
| ソースルート配下の識別子検出 | `ci.yml:242-243` + `kasane/config.yaml` の `lint.identity.scope` (`samples` / `ios` / `android` / `maui` / `kmp` を含む現行 scope) | evidence 5.5 (`maui/macios/native/` 配下に `DEVELOPMENT_TEAM` を投入 → exit 1 / `team-id` を報告) | ✅ |
| secret scan の空振り検出 | `ci.yml:218` (`set -euo pipefail` でパイプ前段の失敗を握り潰さない)、`228-235` (追跡ファイル数と展開数の突き合わせ、`extracted < tracked` で `::error::` + `exit 1`) | evidence 5.5 (`export-ignore` で展開数を減らし exit 1、gitleaks へ進む前に停止) | ✅ |

### Requirement: ツールチェーンの再現性

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 版の変更が diff に現れる | `global.json:1-7` (SDK 10.0.300 / `rollForward: disable` / workload set 10.0.300.3)、`maui/Directory.Packages.props:16` (`Microsoft.Maui.Controls` 10.0.70)、`verify-ios.yml:19`・`verify-kmp.yml:18`・`verify-maui.yml:18` (`KS_XCODE_VERSION: "26.5"`)、各 workflow の `runs-on: macos-26` / `ubuntu-24.04`、`setup-java` の `java-version: '17'` + `distribution: temurin`、`verify-android-instrumented.yml:81` (`api-level: 36`)、外部 action 6 参照の commit SHA 固定、全 6 workflow の `permissions: contents: read` | 本検証で `grep` により action SHA・`permissions` の全件を再確認 | ✅ |
| 手元のビルドが repo の設定で固定される | `global.json:1-7`、`maui/nuget.config` (`maui/` 配下の restore 元を nuget.org 単一へ固定)、`kasane/handbook/cross/local-development-setup.md:60-79` | 本検証で `dotnet --version` = 10.0.300、`dotnet workload list` が repo の `global.json` の workload set 10.0.300.3 を使う旨を表示、`dotnet nuget list source` が `maui/` 配下で nuget.org 単一 (ルートでは 2 ソース)、`dotnet restore --force` で NU1507 / NU1605 なし、`dotnet test` 155 件成功 | ✅ |
| 指定した Xcode が無ければ失敗する | `verify-ios.yml:33-42`・`verify-kmp.yml:32-41`・`verify-maui.yml:36-45` (一致する Xcode が無ければ `::error::` + `ls -d /Applications/Xcode_*.app` + `exit 1`) | evidence 5.7 (`99.9` で exit 1 と一覧出力、`26.5` でパッチ最新が選ばれる) | ✅ |

## 実動未確認の Scenario (5 件)

実装は spec と一致しているが、GitHub 上でしか成立を確認できないもの。いずれも proposal Non-Goals・tasks 5.1 / 5.2・phase-9 の申し送りに明記されている。

| Scenario | 未確認の理由 | 確認の予定 |
|---|---|---|
| develop への push で本体検証と lint が起動する | リモートへの push がオーナーの操作 | tasks 5.1 |
| 記録だけの push では lint だけが走る | 同上 (判定ロジック自体は合成リポジトリで確認済み) | tasks 5.2 |
| 記録とソースが混ざった push では全 job が走る | 同上 | tasks 5.2 |
| 連続する push で古い実行が打ち切られる | concurrency の打ち切りは GitHub 側の挙動 | tasks 5.2 |
| main 宛ての PR で起動する / status check 名が固定される | `main` ブランチが未作成で実 PR を立てられない | phase-9 の初回リリース PR |

`develop 以外からの PR は失敗する` はステップ本体を単体実行して確認済みだが、ステップの `if:` 条件 (`github.event_name == 'pull_request' && github.base_ref == 'main'`) の評価は GitHub 側のため、こちらも phase-9 で併せて確認する。

Emulator 上の instrumented 実行 (`Emulator で全件実行`) は件数検査を合成データで確認済みで、Emulator の起動と実テストの成否は tasks 5.1 の初回実行が初出になる。

## 追加検査

| 検査 | 結果 |
|---|---|
| tasks.md の全タスク完了 | 1.1〜4.2 と 5.3〜5.8 がチェック済み。5.1 / 5.2 は未チェック (オーナーの push 待ち) で、対応表と矛盾しない |
| 虚偽チェックの有無 | なし。チェック済みタスクはいずれも実装または `evidence/ci-step-negative-cases.md` の記録に対応している |
| 逆流検査 (足場の書き換え) | なし。`proposal.md` / `specs/verification-ci/spec.md` は `git status` で未変更、`git log` でも起票コミット `e3abbe1` 以降の変更なし。`tasks.md` の差分はチェックボックスのみ (本文の変更なし) |
| 未記録乖離 | 0 件。❌ が無く、⚠️ 2 件はいずれも `deviation.md` に記録済み。`deviation.md` の 3 件目 (`changes` が 7 本目の check) は「status check 名が固定される」の ⚠️ に対応 |
| 付随修正 | `deviation.md` に `[付随修正]` 行はなし。diff にあって Scenario に直接対応しない変更は `maui/nuget.config` の新設 (Requirement「ツールチェーンの再現性」の「親ディレクトリの設定に依存しない」を満たすための実装) と `kasane/handbook/cross/index.md` / `kasane/concepts/log.md` の追随 (tasks 4.1 / 4.2 に伴う) で、いずれも既存 Requirement / タスクの範囲内 |
| UI 変更 | なし (`ui/` なし。CI 変更のため対象外) |
| テストの全件成功 | `dotnet test maui/KsDialogs.Maui.Tests` = 155 件成功 / 失敗 0、`android` の `./gradlew test` = BUILD SUCCESSFUL (`verifyNoDeclarativeUiDependency` を含む)、lint 4 本 = exit 0、`doc-structure-lint.py` (改訂 handbook 2 本 + index) = 違反なし。ios / kmp / android-instrumented / maui の platform TFM ビルドは、本 change がこれらのソースを変更していないため未再実行 |

## 検証の限界

- `maui/nuget.config` はローカルの権限設定 (資格情報を持ち得るファイル名の保護) により本文を直接読めなかった。`dotnet nuget list source` の比較 (`maui/` 配下 = nuget.org 単一 / ルート = 2 ソース) と、NU1507 の消失で機能面の一致を確認している。`packageSourceMapping` 節の有無は未確認 (`review-002.md` 参照)
- Scenario の担保のうち GitHub ランナー上でしか成立しないもの (ランナーイメージの Xcode 一覧・Emulator の起動・concurrency・pull_request トリガー) は、ステップ単体または合成データでの確認にとどまる。この範囲は spec の Non-Goals と tasks 5.1 / 5.2 の設計どおり

## 判定

**VALID** — Requirement 10 件 / Scenario 27 件のうち ❌ は 0 件、⚠️ (deviation 記録済み) が 2 件。虚偽チェックなし、足場の逆流なし、実行したテストは全件成功。ただし tasks 5.1 / 5.2 が未完了であり、**アーカイブ (蒸留) は `develop` への push で 5 件の実動未確認 Scenario を確認した後**が妥当。
