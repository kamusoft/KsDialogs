# Proposal: add-verification-ci

## Why

public 化した KsDialogs には push / PR を検証する CI が無く、4 形態 (iOS / Android / MAUI / KMP) のテスト・lint はローカル実行頼みである。`develop` へ直 push する運用 (cross/ADR-0016) では壊れた commit が `develop` に載り得るため、「`develop` の先端は機械検査済みか」を事後検証で可視化し、`main` 宛て PR ではマージ条件として保証する仕組みが要る。後続のパッケージング (phase-5〜7)・消費者検証 (phase-8)・release workflow (phase-9) は、ここで定義する検証 job を土台として再利用する。

設計判断はフェーズ議論で決着済み ([agenda](../../roadmaps/package-distribution/phases/phase-4-verification-ci/agenda.md) の決定事項: KsSettingsView からの踏襲 6 件 + KsDialogs 固有 4 件)。本提案はそれをアーティファクトに落とす。翻案元は KsSettingsView の `add-verification-ci` (`../KsSettingsView/kasane/changes/archive/2026-08-31-add-verification-ci/`) と `.github/workflows/` の現行 (ADR-0028 反映後)。

## What Changes

- **GitHub Actions workflow の新設** (`.github/workflows/`):
  - platform 別 reusable workflow (`workflow_call`) 5 本 — ios / android / android-instrumented / kmp / maui
  - CI 入口 workflow 1 本 (`ci.yml`) — `develop` への push と `main` 宛て pull_request (head は `develop` のみ、lint job が検査) で起動する。消費者検証 job は phase-8 で追加予定で本変更では置かない。`on.push.paths-ignore` は使わない (gitleaks / identity-lint / scenario-id-coverage は `kasane/**` も入力にするため、lint は毎 push 必ず走らせる)。本体検証 5 job は `develop` push に限り、軽量な変更検出 job が「ビルド・テストに入力されないファイル (`kasane/**`・Issue テンプレート・貢献案内) だけの変更」と判定したときスキップする (PR では常に全 job)。concurrency はブランチ / PR 単位で cancel-in-progress
- **ios job**: `macos-26` + Xcode 26.5 (メジャー.マイナーを変数で明示選択)。`xcodebuild test -scheme KsDialogs` を iOS Simulator destination で実行、件数は Swift Testing (`Test run with N tests`) と XCTest (`Executed N tests`) の 2 系統を合算して検査 (0 件で fail。現行テストは全て Swift Testing 製で `Executed` 行だけ見ると 0 件に見える)
- **android job**: `ubuntu-24.04` + Temurin 17。`./gradlew test` (JVM テストを持つ module の debug variant — 現構成で `test` が起動するのは `testDebugUnitTest` だけ。本体の Compose 非依存の依存グラフ検査 `verifyNoDeclarativeUiDependency` を含む)。期待する module の集合を `android/settings.gradle.kts` の include のうち `src/test` を持つものから導出し、module ごとに 0 件 / 結果 XML 欠落で fail、合計を job summary へ
- **android-instrumented job** (KsDialogs 固有): `ubuntu-24.04` + API 36 の Emulator 1 台で `connectedDebugAndroidTest`。module ごとに実行数 (tests − skipped) 0 で fail。API 29 固有の旧経路は手元の完了条件のまま (handbook cross/test-execution.md)
- **kmp job** (KsDialogs 固有): `macos-26` + Xcode 26.5 + Temurin 17。`./gradlew allTests compileCommonMainKotlinMetadata compileIosMainKotlinMetadata` を 1 回の Gradle 起動で回す (androidHostTest + iosSimulatorArm64Test + ObjC 公開面検査 + `api-surface-check` のコンパイル + 階層化 source set の metadata compile)。ターゲットごとに 0 件で fail
- **maui job**: `macos-26` + Xcode 26.5 + Temurin 17 + `setup-dotnet` (`global.json`)。facade のユニットテスト (TRX の件数検査、0 件で fail) + binding 2 本と facade platform TFM のビルド + **橋渡しのテスト 2 本** (KsDialogs 固有: `maui/android/native` の JVM テスト、`maui/macios/native` の Swift テストを iOS Simulator で。件数は ios job と同じ 2 系統合算)。NuGet キャッシュ、workload は毎回インストール
- **lint job**: `ubuntu-24.04` で gitleaks (版 + SHA-256 固定、`git archive` 展開に対して `dir` モード)・`scripts/local-path-lint.py`・`scripts/identity-lint.py`・`scripts/comment-policy-lint.py`・`scripts/scenario-id-coverage.py` (仕様の Scenario ID とテスト名の網羅検査、handbook の完了判定の一部) を実行。`main` 宛て PR では head が自リポジトリの `develop` であることを検査
- **toolchain 固定** (KsDialogs 固有): repo 直下に `global.json` を新設 (SDK 10.0.300 / workload set 10.0.300.3 = .NET for iOS 26.5.10284)。`Microsoft.Maui.Controls` を 10.0.70 に固定 (facade / Tests / ApiSurfaceCheck の 3 csproj。宣言の一元化は KsSettingsView の `Directory.Packages.props` を参考に実装時に決める)。手元でも `DEVELOPER_DIR` の付け替えなしに MAUI iOS がビルドできるようになる
- **handbook の更新**: cross/local-development-setup.md (repo の `global.json` で SDK が固定されること)、cross/test-execution.md (CI が回す範囲と手元に残る範囲: API 29 の instrumented・負のコンパイル検証の手動フラグ)

影響する能力: verification-ci (新設)

## Non-Goals

- **消費者検証 job** (`consumer-*`) — phase-8 の守備範囲。本変更では入口 workflow に job 枠を置かず、phase-8 が `if: github.event_name == 'pull_request'` 付きで足す (存在しない workflow を `uses:` できないため)
- **`main` 宛て PR の実 PR での確認** — `main` は初回リリースの PR で作るため、pull_request トリガー・head 制限・status check 名の実動確認は phase-9 の初回リリース PR で行う (phase-9 の agenda に申し送り)。本変更ではステップ単体の確認まで
- **`samples/maui` の CI ビルドと `MauiVersion` の変更** — Sample は利用者が真似する形 (`$(MauiVersion)` 直書き) を保ち、CI でも組まない (翻案元と同じ)。10.0.70 への固定で Sample の解決版が推移的に上がることは手元の iOS / Android ビルドで確認する (tasks 1.2)
- **release workflow** — phase-9 の守備範囲 (本変更の reusable workflow を呼ぶ側)
- **`main` の branch protection** — `main` は初回リリースの PR で作るため phase-9 で作成直後に付ける (必須 check 名 10 件は phase-9 の agenda に申し送り済み)。`develop` には必須 status check を付けない (cross/ADR-0028 翻案元の決定、agenda 論点 4)
- **実行ホスト起動の E2E** (Sample の通し・実機確認) — handbook cross/runtime-behavior-verification.md の手元手順のまま (翻案元 cross/ADR-0026)
- **Android instrumented の API 29 (旧経路)** — Emulator 2 台目は所要時間が倍のため載せない (agenda 論点 3)
- **負のコンパイル検証 (`-P ksdialogs.negativeCheck.*` / MAUI の同等フラグ)** — 「成功したら失敗」の判定を要し、フラグごとに 1 ビルドが要るため CI に載せない。手元の完了判定のまま
- **workload / SDK の丸ごとキャッシュ等の高速化** — まず素直な構成で実測し、許容できない長さなら別 change (翻案元と同じ)
- **`doc-structure-lint.py` の CI 化** — archive 済み文書に既存の指摘が残っており、CI に載せると即 fail する。整理は別途
- **KMP の Kotlin 2.5 への更新と iosMain の `@Throws` 書き戻し** — kmp/ADR-0001 の現行照合で条件付き。toolchain 更新の change で扱う

## Impact

- 破壊的変更なし。ライブラリの公開 API には触れない。`Microsoft.Maui.Controls` の下限が 10.0.1 → 10.0.70 に上がる (消費者側の要求版が上がる。初回リリース前なので利用者影響なし)
- 開発フローは変わらない (`develop` 直 push のまま。CI は事後検証)。`main` 宛て PR は本変更の時点では発生しない
- リスク: 各 job の所要時間が未実測 (kmp は composite build で android/ を巻き込み、instrumented は Emulator 起動を伴う)。timeout は実測に基づいて置き、許容できなければ最適化は別 change
- リスク: `Microsoft.Maui.Controls` 10.0.70 / workload set 10.0.300.3 で KsDialogs の facade・binding・テストがそのまま通るかは実装時の実測。通らなければ実装を止めて報告 (spec は toolchain の版を固定境界として書き、個別の修正は含めない)

## 級: M

1 能力 (検証 CI) の新設で、公開 API・アーキテクチャ・スキーマに触れず、設計判断は agenda で決着済み。翻案元 (KsSettingsView) も同じ理由で M と裁定された (job が 3 → 5 本に増えるが構成は同型)。

domain: cross
roadmap: package-distribution/phase-4-verification-ci
