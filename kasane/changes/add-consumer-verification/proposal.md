# Proposal: add-consumer-verification

## Why

4 形態のパッケージング (SwiftPM 配信リポジトリへのスナップショット同期 / Android と KMP の Maven 発行構成 / MAUI の NuGet pack) は phase-5〜7 で整ったが、「配布物を利用者と同じ経路で解決して Release ビルドできるか」の確認は、各フェーズで一時プロジェクトを手で作って通した証跡 (`kasane/changes/archive/2026-09-08-add-maui-nuget-distribution/evidence/consumer-verification/` 等) が残るだけで、リポジトリ内に再実行できる形では存在しない。ルート README の最小コード例も、実際にビルドが通るかは未検証である。phase-9 の release workflow は publish 前の dry-run と publish 後の smoke を必要とし、その器がこの変更である。

KMP 形態は翻案元 (KsSettingsView) に無い。phase-7 の結論 (C1: dry-run は Maven を mavenLocal、Swift 参照は tag 付きローカル clone への `file://` + exact / C2: Android `assembleRelease` → framework リンク → `xcodebuild` の 3 段) と、phase-7 の申し送り (全 publication を dry-run と smoke で実解決する、消費者の KGP 版で走る段が確認済み Kotlin 版の実証を兼ねる) をここで実装する。

設計判断はフェーズ議論で決着済み ([agenda](../../roadmaps/package-distribution/phases/phase-8-consumer-verification/agenda.md) の踏襲 7 項目と決定 4 件)。本提案はそれをアーティファクトに落とす。

## What Changes

- **`verification/` の新設**: 配布物を参照する消費者プロジェクトを 4 形態分置く。いずれも本体のソースを参照せず、公開座標だけを参照する
  - `verification/ios/`: SwiftPM パッケージ (`Package.swift` はテンプレートから生成、`platforms: iOS 17`、1 target)。README の iOS 最小例を同梱
  - `verification/android/`: 1 つの Gradle プロジェクトに `com.android.application` を 2 モジュール — Compose 系 `jp.kamusoft:ksdialogs` 1 行の app と、View 系本体 `jp.kamusoft:ksdialogs-core` 1 行の app (agenda 決定 2)。バージョンカタログは `android/gradle/libs.versions.toml` を共有 (Sample と同じ)。README の Android 最小例は 2 モジュールが共有するソースディレクトリに 1 か所
  - `verification/maui/`: `dotnet new maui` 相当のアプリ (`TargetFrameworks` は `net10.0-android;net10.0-ios`、`SupportedOSPlatformVersion` Android 24 / iOS 17.0、`Microsoft.Maui.Controls` の版は書かない、`WarningsAsErrors` に NU1605 / NU1608 / NU1107、Android Release は `AndroidLinkTool=r8` を明示)。facade `KsDialogs.Maui` の PackageReference 1 行だけを足し、binding 2 件は推移で入る。README の MAUI 最小例 (C# 1 ブロック) を同梱
  - `verification/kmp/`: 利用者と同じ 1 つの KMP プロジェクト (`shared` + `androidApp` + `iosApp`) (agenda 決定 1)。`shared` の commonMain に `jp.kamusoft:ksdialogs-kmp` 1 行と README の KMP 最小例、`androidApp` は shared 経由の推移だけで View を登録、`iosApp` は Xcode project で shared の static framework・生成される linkage package・README 最小例を動かす Swift 登録コードを包むローカル package `VerificationApp` (Package.swift はテンプレート生成) の 3 点を参照する。ホスト側の登録コードは concepts `kmp/api/ios-host-integration.md` の例に倣う (lint 対象外、agenda 決定 4)
  - application ID は既存規則から導出する: `jp.kamusoft.ksdialogs.verification.{android,androidcore,maui,kmp}` (Sample の `jp.kamusoft.ksdialogs.samples.*` と同じ階層)
- **参照先とモードの切り替え**: 各消費者はモード (`dry-run` / `smoke`) と version の 2 引数を受け取る (共通引数解釈は `verification/lib/verification-args.sh`)
  - iOS: dry-run はスナップショット (`scripts/spm-snapshot/sync-snapshot.sh` の出力を一時ディレクトリ `KsDialogs-SPM` に配置) への `path:` 参照、smoke は `https://github.com/kamusoft/KsDialogs-SPM` + `exact:`
  - Android: `jp.kamusoft` をモードに応じてローカル Maven リポジトリ / mavenCentral のどちらか 1 つに `exclusiveContent` で排他割り当て
  - MAUI: `nuget.config` 2 枚 (dry-run はローカルフォルダフィード + nuget.org、smoke は nuget.org のみ。packageSourceMapping で `KsDialogs.*` の取得元を 1 つに固定)、実行ごとに空の `RestorePackagesPath`
  - KMP: Maven は Android と同じ排他割り当て。Swift 参照は dry-run がスナップショットを同期して commit + tag したローカル clone の `file://` URL + exact、smoke が `https://github.com/kamusoft/KsDialogs-SPM` + exact。linkage package と `VerificationApp` の依存 1 行が同じ URL を指す (phase-7 C1)
  - dry-run で version 未指定なら検証用の既定 version、smoke は version 必須。許可値以外のモードは早期に失敗する。KMP の dry-run は SNAPSHOT では成立しない (SNAPSHOT の Swift 参照は monorepo 内 `ios/` へのローカルパスで、消費者が本体ソースを参照してしまう) ため、既定 version の扱いは design.md の Decision で決める
- **実行スクリプト**: `verification/<platform>/` に「フィード準備」(`prepare-feed.sh`: スナップショット配置 / `publishToMavenLocal` / `pack` / KMP は clone + tag と android/・kmp/ の発行) と「消費者ビルド」(`build-consumer.sh`、`--reference` で準備済みの参照先を受ける) の 2 段。release では準備段を package 段の artifact で置き換えられる (KMP は Android 側のみ。kmp/ の発行は Swift 参照が `file://` を要するため dry-run job 内で行う、phase-7 C3)
- **検査**: 消費者ビルドは Release 構成 (KMP は Android `assembleRelease` → `linkReleaseFrameworkIosSimulatorArm64` → `xcodebuild` Release の 3 段、MAUI iOS は Simulator RID を明示し署名情報を要求しない)。Android は Compose 側 app で `ksdialogs-core` が facade と同版で推移解決されたこと、`-core` 側 app の classpath に `androidx.compose` が無いことを依存ツリーで検査。MAUI は `check-dependencies.py` で binding 2 件の解決版が facade と一致し、platform TFM で binding のアセットが実際に入った (platform 中立アセットへのフォールバックでない) ことを検査。KMP は 5 publication (root / android / iOS 3 ターゲット) が消費者の Gradle で実解決されたことと、`swiftpm-metadata.json` の Swift 参照が mode どおりの URL + exact であることを証跡に残す
- **README 一致 lint**: `scripts/readme-example-lint.py` が README.md (英語) の「Minimal examples」節の 4 コードブロック (iOS / Android / MAUI / KMP) と `verification/` の対応 4 ファイルの完全一致を検査し、lint job の 8 検査目として加える (agenda 決定 4。cross/ADR-0021 の一部改訂を伴う — design「ADR 候補」)
- **CI**: platform 別の再利用可能 workflow 4 本 `verify-consumer-{ios,android,maui,kmp}.yml` (`workflow_call`、入力 `mode` / `version` (任意) / `artifact` (任意、dry-run 専用)) を新設し、`ci.yml` から `main` 宛て pull_request でだけ `mode=dry-run` で呼ぶ (status check 名 `consumer-<platform> / verify`、agenda 決定 3 / phase-4 申し送り)。`permissions: contents: read` のみで secrets を受け取らない。timeout は ios / android / kmp 30 分、maui 40 分を初期値にし実測で詰める。`kasane/config.yaml` の `lint.identity.scope` に `verification` を足す

影響する能力: consumer-verification (新設)、verification-ci (job 構成と lint の拡張)

## Non-Goals

- **release workflow からの呼び出し (publish 前 dry-run / publish 後 smoke の job 化) と smoke 正ケース (公開レジストリからの解決成功) の実証** — phase-9 の守備範囲。配布物が未公開のため本変更で実証できるのは smoke の参照先設定の生成まで
- **`main` の必須 status check への消費者 4 job の登録** — phase-9 (初回リリース前の branch protection 更新) で本体 5 job と併せて行う (phase-4 の決定事項: `main` は必須 status check + PR 経由必須)
- **Simulator / Emulator での起動、`dotnet publish`、実機** — agenda 踏襲 (検証範囲)。実行時挙動は本体の検証 CI と Sample が担う
- **README に KMP のホスト側 (iOS / Android) の登録例を足すこと、`README_ja` の一致検査** — README の構成変更は phase-9 の docs-refresh の論点、英日同期は docs-refresh の責務 (agenda 決定 4)
- **README の Android 導入例の旧座標 (`ksdialogs-compose` 等) の追随** — docs-refresh の責務 (phase-9 TODO)。最小例の節は影響を受けない (確認済み)
- **AndroidX 等の推移依存の解決版の期待値照合** — 翻案元と同じく CPM 更新との二重管理になる。競合の不在は NU1107 / NU1608 で担保
- **Kotlin の次 minor での消費者検証 (A4 のサポート範囲拡張)** — 次 minor が出た時点の別変更

## Impact

- 破壊的変更なし。ライブラリのコード・テスト・発行構成には触れない (フィード準備は既存の `publishToMavenLocal` / `pack` / 同期スクリプトを呼ぶだけ)
- `main` 宛て PR の CI に macOS 3 job (ios / maui / kmp) + ubuntu 1 job (android) が並列で増える。macOS job は本体と合わせて 6 つになり、public リポジトリの同時実行上限 (5) で 1 つが待ちに入る。PR 全体の壁時計は実測して agenda の申し送り (phase-9) に記録する。`develop` への push には影響しない
- README の最小例がそのままビルド対象になる。4 例は現行 API と対応が取れていることを確認済みだが、実装中に壊れていると分かった場合は README の修正が docs-refresh 経由になるため、本変更の完了が docs-refresh 依頼に依存する
- 提案作成時に見つけた長命層の衝突: cross/ADR-0018 の MAUI 本体の版 (10.0.70) は maui/ADR-0004 と現行コード (10.0.20) と食い違う。本変更はコードに合わせて書き、ADR-0018 の改訂は蒸留に申し送る (design「ADR 候補」)
- リスク: KMP の dry-run の組み立て (`file://` + exact の clone、`-Dmaven.repo.local` によるフィードの隔離、生成される linkage package の扱い) は phase-7 の PoC と発行検証で個々に実証済みだが、消費者からの実解決は本変更が初めて。tasks の冒頭で実測する (lessons process / spec-review L-003 の「翻案先で観測してから足す」)

## 級: L

2 能力 (consumer-verification / verification-ci) にまたがり、翻案元に無い KMP 消費者の設計判断 (dry-run の既定 version、フィードの隔離、linkage package の扱い、artifact 入力の意味) が複数あるため。設計判断と却下した代替案は design.md に集約する。翻案元の同名 change も同じ理由で L (KsSettingsView `kasane/changes/archive/2026-09-02-add-consumer-verification/`)。

domain: cross
roadmap: package-distribution/phase-8-consumer-verification
