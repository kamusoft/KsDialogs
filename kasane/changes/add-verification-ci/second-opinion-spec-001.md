# セカンドオピニオン: add-verification-ci (spec-001)
**相方**: codex / **label**: so-spec-add-verification-ci / **日付**: 2026-09-08 / **対象**: kasane/changes/add-verification-ci/ の proposal.md / specs/verification-ci/spec.md / tasks.md (自己レビュー 2 周後の版)
---
# レビュー結果: add-verification-ci

**日付**: 2026-09-08  
**判定**: **NEEDS_DISCUSSION**

## サマリー

Critical 1件、Major 4件、Minor 3件です。特に、`paths-ignore` により秘密情報検査を含む lint 自体が起動しない経路と、Swift Testing の件数を `Executed` 行だけで判定する計画は、実装するとそれぞれ未検査の push と常時失敗する CI を生みます。

静的レビューのため、ビルド・テスト・ファイル作成は行っていません。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md`（always）
- `kasane/handbook/cross/test-execution.md`（テスト実行・結果報告・完了判定）

## 指摘事項

### [🔴 Critical] `paths-ignore` が secret scan と既存 lint を迂回させる

**該当箇所**: `specs/verification-ci/spec.md:6`、`specs/verification-ci/spec.md:125`、`tasks.md:21`、`kasane/config.yaml:20`

**問題点**: `kasane/**`・Issue テンプレート・貢献案内を「lint の入力ではない」として CI 全体から除外していますが、実際には以下の入力です。

- gitleaks: `git archive HEAD` の全追跡ファイル
- local-path-lint: 全追跡ファイル
- identity-lint: `kasane` を明示的に scope に含む
- scenario-id-coverage: `kasane/changes/**/spec.md` を走査

したがって、`kasane/` だけの push で秘密情報・個体識別子・未網羅 Scenario を追加しても CI が起動しません。public な `develop` に秘密が載った場合、後の `main` PR で検出しても既に履歴へ公開済みです。

**推奨修正**: `develop` push の `paths-ignore` を撤去してください。コストを抑える必要があるなら、除外対象の push でも lint だけは必ず動く別入口または job 条件を仕様化し、「CI は起動しない」Scenario を改訂してください。

### [🟠 Major] Swift Testing の全テストを0件と誤判定する

**該当箇所**: `tasks.md:12`、`tasks.md:17`、`proposal.md:14`、`kasane/handbook/cross/test-execution.md:48`、`kasane/handbook/cross/test-execution.md:154`

**問題点**: iOS と MAUI iOS bridge の件数検査を `Executed N tests` 行に限定しています。しかし現行テストはすべて Swift Testing 製で、実件数は `Test run with N tests ...` に出力され、XCTest 側は `Executed 0 tests` です。この計画のままではテストが全件成功しても両 job が失敗します。翻案元 workflow の件数抽出をそのままコピーできない箇所です。

**推奨修正**: Swift Testing と XCTest の2系統を抽出・合算することを proposal・tasks に明記し、各形式のみ、混在、両方0件、件数行欠落の負ケースを task 5.3 に含めてください。

### [🟠 Major] Android の要求する release テストを実行コマンドが起動しない

**該当箇所**: `specs/verification-ci/spec.md:63`、`tasks.md:13`、`kasane/handbook/cross/test-execution.md:57`

**問題点**: spec は debug / release 両 variant を要求し、task は2組の結果 XML を必須にしますが、現行規約では `./gradlew test` が実行するのは `testDebugUnitTest` のみです。したがって release の XML 欠落により、正常な現行構成でも件数検査が失敗します。

**推奨修正**: 次のいずれかを仕様として確定してください。

- 現行どおり debug のみを契約にする。
- `testDebugUnitTest` と `testReleaseUnitTest` を明示的に起動し、両方を契約にする。

後者なら、正の API surface check と Compose 非依存検査が同じ起動に残ることも tasks に含めてください。

### [🟠 Major] MAUI Controls 更新後の Sample 互換性が保証されない

**該当箇所**: `tasks.md:8`、`proposal.md:20`、`samples/maui/KsDialogs.Sample.Maui/KsDialogs.Sample.Maui.csproj:40`

**問題点**: facade・Tests・ApiSurfaceCheck は 10.0.70 へ上げる一方、Sample は変更しない計画です。Sample の PackageReference は `$(MauiVersion)` を使っていますが、プロジェクト内に 10.0.70 の明示値がありません。固定する workload の MAUI SDK は agenda 上 10.0.20であり、Sample と ProjectReference 先で異なる Controls 版が解決される可能性があります。また、この change の検証タスクには Sample の platform build がないため、退行しても検出できません。

**推奨修正**: Sample にも `MauiVersion` 10.0.70 を明示するか、解決版が10.0.70になる別の一元化境界を仕様化してください。少なくとも iOS / Android の Sample build を受け入れ確認へ追加してください。E2E実行を行わない Non-Goalとは両立します。

### [🟠 Major] `main` PR の主要 Scenario を実際には検証できない

**該当箇所**: `specs/verification-ci/spec.md:13`、`specs/verification-ci/spec.md:31`、`specs/verification-ci/spec.md:44`、`tasks.md:38`

**問題点**: spec は `main` PR の起動、head 制限、status check 名を受け入れ条件にしていますが、task 5.6 は環境変数を差し替えた shell ステップの単体確認だけです。これは `pull_request` トリガー、GitHub event からの値取得、reusable workflow の実起動、status check 名を検証しません。`main` が存在しないという前提のままでは、この change 単独で Scenario を完了できません。

**推奨修正**: 実際の `main` PR で検証する時点をこの change 内に設けるか、これらの受け入れを phase-9 に明示的に移し、本 change の仕様をローカルで検証可能な範囲へ狭めるかを決定してください。

### [🟡 Minor] 消費者検証 job の枠を置くかどうかが proposal 内で矛盾する

**該当箇所**: `proposal.md:13`、`proposal.md:27`

**問題点**: line 13 は「本変更では job の枠だけ足す」と読めますが、Non-Goals は「job の枠を置かず phase-8 が足す」としています。tasks と spec は後者です。

**推奨修正**: line 13 を「phase-8 で追加予定であり、本変更では置かない」に統一してください。

### [🟡 Minor] KMP の負のコンパイル検証について背景決定と提案が矛盾する

**該当箇所**: `kasane/roadmaps/package-distribution/phases/phase-4-verification-ci/agenda.md:28`、`proposal.md:32`、`kmp/api-surface-check/build.gradle.kts:9`

**問題点**: agenda は `iosSimulatorArm64Test` に負のコンパイル検証も含まれるとしていますが、実装では負のソースは Gradle property 指定時だけ追加され、proposal も負の検証62本を CI 対象外としています。

**推奨修正**: 意図が現 proposal どおりなら、agenda の記述を「正の公開 API 形状検査」に修正してください。負の検証も含める意図なら、Non-Goal と tasks を全面的に見直す必要があります。

### [🟡 Minor] instrumented テストが全件 skip でも成功し得る

**該当箇所**: `specs/verification-ci/spec.md:81`、`kasane/handbook/cross/test-execution.md:80`

**問題点**: JUnit XML の `tests` は skipped を含みます。現在の条件は `tests > 0` だけなので、module 内のテストがすべて API 条件で skip されても緑になります。

**推奨修正**: module ごとに `tests - skipped > 0` を要求し、summary には tests / skipped / failures を分けて表示してください。

## アクションプラン

1. `paths-ignore` と lint 保証範囲を再決定する。
2. Swift Testing / XCTest の件数抽出を2系統対応にする。
3. Android release variant を実行するか、要求から外すか確定する。
4. MAUI Sample の Controls 解決版と build 検証を追加する。
5. `main` PR Scenario の検証時点を phase-4 または phase-9のどちらに置くか決める。
6. proposal / agenda の軽微な矛盾と skip 判定を修正する。


## 突き合わせ結果 (2026-09-08)

ホスト側の自己レビュー (2 周) は Android の期待集合 (JVM テストを持つ module だけ) の 1 件を修正して通過していた。相方の 8 件はいずれもホスト側の見逃しで、根拠 (該当箇所・実害) を実物で裏取りしたうえで採用した。降格・未解決なし。

| # | 指摘 | 裏取り | 採否 | 反映先 |
|---|---|---|---|---|
| 1 | `paths-ignore` が secret scan と lint を迂回 (Critical) | gitleaks は追跡ファイル全部、identity-lint scope に `kasane`、scenario-id-coverage は `kasane/changes` を走査 | 採用 | `paths-ignore` 撤去。lint 常時、本体検証 5 job は `develop` push に限り変更検出 job で条件付け (proposal / spec「CI の起動条件」/ tasks 3.1・5.2) |
| 2 | Swift Testing の件数を `Executed` 行だけで見る (Major) | ios 55 / 橋渡し 4 ファイル全て Swift Testing、handbook に 2 系統の記述 | 採用 | 2 系統合算 (spec「iOS の検証」「MAUI の検証」/ tasks 2.1・2.6・5.3) |
| 3 | Android の release variant は走らない (Major) | handbook「実行されるのは testDebugUnitTest」 | 採用 | 契約を debug のみに (spec「Android の検証」/ tasks 2.2) |
| 4 | MAUI Controls 更新後の Sample 互換性 (Major) | Sample は `$(MauiVersion)` 直書き | 一部採用 | Sample の版は変えず CI でも組まない (翻案元と同じ)。手元の iOS / Android ビルド確認を tasks 1.2 に追加、Non-Goals に明記 |
| 5 | `main` 宛て PR の Scenario が本 change で検証できない (Major) | `main` 未作成 | 採用 | 実動確認を phase-9 の初回リリース PR に (Non-Goals / tasks 5.6 / phase-9 agenda 申し送り) |
| 6 | proposal の消費者 job 枠の矛盾 (Minor) | 書き損じ | 採用 | proposal What Changes を修正 |
| 7 | agenda の「負のコンパイル検証も含まれる」(Minor) | 負のソースはフラグ指定時のみ | 採用 | agenda 決定事項の文言を修正 |
| 8 | instrumented が全件 skip でも緑 (Minor) | JUnit XML の `tests` は skipped を含む | 採用 | 実行数 = tests − skipped (spec「Android instrumented の検証」/ tasks 2.3) |
