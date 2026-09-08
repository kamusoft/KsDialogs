# セカンドオピニオン: add-verification-ci (code-001)
**相方**: codex / **label**: so-code-add-verification-ci / **日付**: 2026-09-08 / **対象**: 作業ツリーの未コミット変更 (.github/workflows/ 6 本・global.json・maui/Directory.Packages.props・maui と samples/maui の csproj・handbook 2 文書) と kasane/changes/add-verification-ci/ の成果物
---
# レビュー結果: add-verification-ci

**日付**: 2026-09-08  
**判定**: **NEEDS_DISCUSSION**

Critical 0 / Major 4 / Minor 2 / Suggestion 0。

accepted ADR と仕様が衝突しており、実装だけでは解消できないため `NEEDS_DISCUSSION` とします。加えて、実装側にも未完了ゲート・虚偽チェック・ツールチェーン固定漏れがあり、ADR 問題を除いても現状は承認できません。

静的確認では 6 workflow の YAML 構文と `git diff --check` は正常でした。ビルド・テストは依頼どおり再実行していません。

## 照合した規約

- `comment-policy.md`（always）
- `test-execution.md`（CI・テスト結果・完了判定）
- `local-development-setup.md`（toolchain・SDK 解決）
- `sample-parity.md`（`samples/maui` 変更）
- cross/ADR-0004、0006、0013
- android/ADR-0001
- maui/ADR-0003
- cross/ADR-0016 は proposed のため確定判断としては不使用

## 指摘事項

### [🟠 Major] ルート `global.json` が accepted ADR と衝突している

**該当箇所**: `global.json:1`、`specs/verification-ci/spec.md:153`、`kasane/decisions/cross/0004-monorepo-four-build-roots.md:16`

**問題点**: cross/ADR-0004 は「リポジトリルートには共通ビルドファイルを置かない」と決定していますが、仕様はルート `global.json` を明示的に要求しています。ロードマップ上の合意は確認できますが、accepted ADR の改訂・例外記録はなく、`deviation.md` にもありません。

**推奨修正**: `global.json` を「ビルドルートを作らない toolchain selector」として ADR-0004 の許容例外にするか、ADR を維持して配置を分離するかをオーナー判断してください。仕様は凍結中なので、実装都合で直接書き換えず Kasane の決定経路で解消すべきです。

### [🟠 Major] .NET SDK が完全固定されていない

**該当箇所**: `global.json:2`、`specs/verification-ci/spec.md:153`、`kasane/handbook/cross/local-development-setup.md:73`

**問題点**: `rollForward` を省略すると既定の `patch` ポリシーが使われ、10.0.300 が無い環境では同じ feature band の新しい patch SDK に進み得ます。「10.0.300 の完全指定」「版が diff なしに変わらない」という仕様を満たしません。[Microsoft Learn の仕様](https://learn.microsoft.com/en-us/dotnet/core/tools/global-json)でも、完全一致には `rollForward: disable` が必要です。

**推奨修正**: `global.json` の `sdk` に `"rollForward": "disable"` を追加し、指定 SDK が無ければ明示的に失敗させてください。

### [🟠 Major] tasks 5.4 が証跡に反して完了扱いになっている

**該当箇所**: `tasks.md:36`、`evidence/ci-step-negative-cases.md:92`、`evidence/ci-step-negative-cases.md:96`、`specs/verification-ci/spec.md:111`

**問題点**: task と Scenario は「ターゲット本体は通るが metadata compile だけが失敗する状態」を要求しています。しかし証跡は `compileKotlinIosSimulatorArm64` が先に失敗し、その状態を作れていないと明記しています。metadata task の単体失敗だけでは、job に追加した metadata compile が独立に退行を検出する証明になりません。

**推奨修正**: 5.4 を未完了へ戻し、ターゲットコンパイルを通しつつ metadata compile だけを落とす負ケースを用意してください。現行 Kotlin でその状態を構成できないなら、チェック済みにせず Scenario の成立性をオーナーと再検討してください。

### [🟠 Major] 必須の GitHub 実挙動ゲートが未完了で、instrumented 全件実行も失敗している

**該当箇所**: `tasks.md:32`、`tasks.md:33`、`.github/workflows/verify-android-instrumented.yml:77`、`.github/workflows/verify-android-instrumented.yml:85`

**問題点**: develop push の起動・変更検出・concurrency・実 timeout を確認する 5.1 / 5.2 が未完了です。また提示された `connectedDebugAndroidTest` は `LD_WN_01` が失敗しています。API 35 と CI の API 36 は同一ではありませんが、CI も `-no-window` を使うため、同じ環境要因が再現する可能性があります。`ksn-review` の規律上、全件テストの失敗は承認できません。

**推奨修正**: オーナー push 後に API 36 の実 workflow で再現を確認し、失敗するならテストまたは Emulator 条件を修正してください。そのうえで 5.1 / 5.2 を証跡付きで完了させてください。phase-9 に移した main PR の確認は本指摘の対象外です。

### [🟡 Minor] `changes` が仕様にない追加 status check を生成する

**該当箇所**: `.github/workflows/ci.yml:39`、`specs/verification-ci/spec.md:42`、`specs/verification-ci/spec.md:49`

**問題点**: 仕様は status check 名を6件としていますが、GitHub は各 job に check run を作るため、`changes` も7件目として報告されます。[GitHub Docs](https://docs.github.com/en/actions/how-tos/monitor-workflows/use-workflow-run-logs)にも、check suite は workflow の各 job に check run を持つと記載されています。必須 check が6件という意味なら実害は小さいものの、現行文面の「6件」とは一致しません。

**推奨修正**: 「固定する必須 check 名が6件」であり補助 check は許容するのかを明確にしてください。厳密に全6件なら、変更検出を独立 job にしない構成が必要です。

### [🟡 Minor] proposed ADR を確定判断としてソースコメントから参照している

**該当箇所**: `.github/workflows/ci.yml:3`、`kasane/decisions/cross/0016-branch-model-develop-main.md:4`、`kasane/handbook/cross/comment-policy.md:21`

**問題点**: コメント規約が許容する ADR 参照は「確定した設計判断」ですが、cross/ADR-0016 は `proposed` です。

**推奨修正**: ADR が accepted になるまで参照を外し、既に自己完結している後続コメントだけで説明してください。別経路で ADR を accepted にする場合は、その完了後に参照できます。

## アクションプラン

1. ルート `global.json` と cross/ADR-0004 の扱いをオーナー判断する。
2. `rollForward: disable` を追加する。
3. tasks 5.4 のチェックを戻し、Scenario を満たす負ケースを再検証する。
4. develop push で 5.1 / 5.2 と API 36 headless instrumented を確認する。
5. status check の「6件」が必須集合か全報告集合かを確定する。
6. proposed ADR 参照を整理した後、独立レビューを再実施する。

制約に従い、`review-001.md` は作成していません。

## 突き合わせ結果 (2026-09-08)

ホスト側 review-001 (CHANGES_REQUESTED: Major 2 / Minor 2 / Suggestion 4) と相方 (NEEDS_DISCUSSION: Major 4 / Minor 2) を突き合わせた。

| # | 相方の指摘 | 裏取り | 採否 | 扱い |
|---|---|---|---|---|
| 1 | ルート `global.json` が cross/ADR-0004「ルートに共通ビルドファイルを置かない」と衝突 (Major) | ADR-0004 は accepted で当該文言あり。配置は phase-4 agenda の決定と spec「ツールチェーンの再現性」に基づく | 採用 (決定経路へ) | 実装は変えない。蒸留時に ADR-0004 へ「toolchain selector (`global.json`) は例外」を注記する申し送り |
| 2 | `rollForward` 省略で SDK が patch 単位に進み得る (Major) | 翻案元 `../KsSettingsView/global.json` にも無い。spec は「`global.json` の完全指定」「版が diff なしに変わらない」を要求 | 採用 | `"rollForward": "disable"` を追加 (修正サイクル) |
| 3 | tasks 5.4 が「metadata compile だけが失敗する状態」を作れていないのに完了扱い (Major) | evidence 5.4 の但し書きどおり。現行 Kotlin ではターゲット本体が先に同じ違反を検出する。job の失敗と metadata compile 単体の検出は確認済み | 一部採用 | チェックは維持し、deviation.md に「状態は再現不能、強制失敗で機構を確認」と記録 (lessons/inbox forced-failure-confirms-mechanism-when-repro-fails の扱いに倣う) |
| 4 | 5.1 / 5.2 未完了と instrumented `LD_WN_01` の失敗 (Major) | 既知。5.1 / 5.2 はオーナーの push を要し、CI も `-no-window` のため再現の可能性はある | 降格 (既知のゲート) | push 後の初回実行で確認。コード修正ではない |
| 5 | `changes` が 7 本目の status check になる (Minor) | ホスト Major 2 の付記と一致 | 確定 | phase-9 の必須 check 一覧への申し送り (deviation.md に記録) |
| 6 | proposed な cross/ADR-0016 をソースコメントから参照 (Minor) | comment-policy は確定した設計判断の参照のみ許容。ADR-0016 は proposed | 採用 | `ci.yml` のコメントから参照を外す (修正サイクル)。handbook 側の参照はソースコメントではないため対象外、ADR の accepted 昇格は蒸留の申し送り |

ホスト側の Major 2 件 (`maui/nuget.config` の欠落 / `changes` 失敗時の 5 job skip) と Minor 2 件は相方の指摘に無く、ホスト側のみで確定 (修正サイクル)。ホストの Suggestion 4 件は tasks 5.1 の実測後に採否を判断する。
