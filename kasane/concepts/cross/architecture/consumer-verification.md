---
type: concept
title: 消費者検証 (verification/)
description: 配布物を利用者と同じ経路で解決して Release ビルドする消費者プロジェクト 4 形態の構成、dry-run / smoke の参照先の排他、フィード準備と消費者ビルドの 2 段スクリプト、KMP の dry-run の組み立て (合成 version・tag 付きローカル clone・smoke 形 fixture)、release workflow が渡す artifact の配置
tags: [cross, verification, distribution, ci, swiftpm, maven, nuget, kmp]
timestamp: 2026-09-10
---

# 消費者検証 (verification/)

この文書を読むと、`verification/` に置かれた 4 形態の消費者プロジェクトが何を検証し、dry-run と smoke で参照先がどう切り替わり、release workflow がどの形で配布物を渡せばよいかが分かる。配布経路そのもの (SwiftPM 配信リポジトリ・Maven 発行・NuGet pack) は cross/ADR-0008 と各形態の ADR が、CI の job 構成と起動条件は [検証 CI の範囲と実行条件](../../../handbook/cross/verification-ci.md) が持つ。KMP の iOS ホスト側の依存経路は [KMP 利用者の iOS ホスト統合](../../kmp/api/ios-host-integration.md) を先に読むと分かりやすい。

## 役割と検証範囲

消費者検証は「配布物を利用者と同じ経路で解決し、Release 構成でビルドできる」ことを、リポジトリ内に再実行できる形で持つ仕組みである。消費者プロジェクトは本体のソースを参照せず、公開座標 (`jp.kamusoft:*` / `KsDialogs.*` / SwiftPM product `KsDialogs`) だけを参照する。検証範囲は「解決 + Release ビルド」までで、Simulator / Emulator での起動・`dotnet publish`・実機は含まない (実行時挙動は本体の検証 CI とリポジトリ内の Sample アプリ `samples/` が担う)。

モードは 2 つある。**dry-run** は公開前の検証で、その実行で作ったフィード (または release workflow の package 段が渡す成果物) を参照先にする。**smoke** は公開後の検証で、公開レジストリだけを参照先にする。iOS の配布物は配信リポジトリ `KsDialogs-SPM` (SwiftPM 専用の別リポジトリ。release が `ios/` の `Package.swift` / `Sources/` / `Tests/` のスナップショットを commit して version と同名の tag を打つ) から解決されるため、dry-run ではスナップショットを作業ディレクトリ内に同期して配信リポジトリの代わりにする。

| 形態 | 消費者の形 | 参照する配布物 | ビルド |
|---|---|---|---|
| iOS (`verification/ios/`) | SwiftPM パッケージ 1 target (`platforms: iOS 17`) | product `KsDialogs` | `xcodebuild` Release、署名なし |
| Android (`verification/android/`) | `com.android.application` 2 モジュール — Compose 系 `jp.kamusoft:ksdialogs` 1 行の `:app` と、View 系本体 `jp.kamusoft:ksdialogs-core` 1 行の `:app-core` | 2 artifact (cross/ADR-0019) | 両モジュールの `assembleRelease` |
| MAUI (`verification/maui/`) | `dotnet new maui` 相当 (`net10.0-android;net10.0-ios`、`Microsoft.Maui.Controls` の版は書かない) | facade `KsDialogs.Maui` 1 行 (binding 2 件は推移) | 両 TFM の Release (iOS は Simulator RID) |
| KMP (`verification/kmp/`) | 利用者と同じ 1 プロジェクト `shared` + `androidApp` + `iosApp` | `jp.kamusoft:ksdialogs-kmp` 1 行 + iOS ホスト側の 3 点参照 | 3 段 (後述) |

ルート README (英語) の「Minimal examples」節の 4 コードブロックは、各消費者のソースとして逐語で同梱され、lint job の一致検査 (`scripts/readme-example-lint.py`、cross/ADR-0022) が README との完全一致を守る。README の例が変わるのは docs-refresh 経由で、lint が赤になったら依頼者が消費者側のソースも直す。KMP の iOS / Android ホスト側の登録コードは README に無く、lint の対象外である。

Android / KMP の消費者は本体のバージョンカタログ `android/gradle/libs.versions.toml` を共有し、AGP / Kotlin を上げると消費者も同時に上がる。消費者の KGP 版で走る段が、サポートする Kotlin 版 (同 minor、確認済み版はカタログの `kotlin`) の実証を兼ねる。Android SDK は `verification/lib/android-sdk.sh` が本体 build root の設定から引き継ぐ ([ローカル開発環境の準備](../../../handbook/cross/local-development-setup.md))。

## 2 段のスクリプトと引数

各形態は `prepare-feed.sh` (フィード準備) と `build-consumer.sh` (消費者ビルド) の 2 段で、共通の引数解釈は `verification/lib/verification-args.sh` が持つ。引数の検査 (許可値以外の `--mode`・smoke の `--version` 省略・smoke への `--reference`) はフィード準備や SDK 解決より前に行われ、CI の workflow も同じ 3 判定を checkout より前に置く。

| 引数 | 意味 |
|---|---|
| `--mode dry-run \| smoke` | 参照先の切り替え (次節)。既定は dry-run |
| `--version` | 全形態に同じ文字列で流す version。dry-run で省略すると検証用の合成 version `0.0.0-alpha.0` (宣言は `verification-args.sh` の 1 か所。iOS の dry-run は `path:` 参照で version を持たない)。smoke では必須 |
| `--reference` | 準備済みの参照先 (フィード準備の出力、または CI の artifact)。dry-run 専用で、与えるとフィード準備を飛ばす |
| `--work` | 作業ディレクトリ。既定は `${TMPDIR:-/tmp}/ksdialogs-verification/<形態>`、CI は `${RUNNER_TEMP}/consumer`。リポジトリ内は拒否する |

合成 version は本体の注入値の形式検査 (`X.Y.Z` または `X.Y.Z-{alpha|beta|rc}.N`) に適合し、リリースしない値なので実在の配布物と衝突しない。dry-run は「リリース版 × 署名鍵なし」の発行経路を毎回踏む (Maven 発行の成果物署名は鍵が無ければ skip され、未署名のまま通る)。解決結果の証跡 (解決版・取得元・依存ツリー) は標準出力に出し、CI では job summary にも書く。

## モードと参照先の排他

dry-run の参照先は本リポジトリ由来の座標について**排他的**で、ローカル参照先に無い version は公開レジストリやユーザー環境のキャッシュ (`~/.m2`・global packages folder) へ静かにフォールバックせず失敗する。smoke は公開レジストリだけを参照し、フィード準備を行わない (公開レジストリからの解決成功は release workflow の smoke 段で確かめる)。

| 形態 | dry-run の参照先 | smoke の参照先 | 排他の機構 |
|---|---|---|---|
| iOS | 同期スクリプトの出力を作業ディレクトリ内 `KsDialogs-SPM` に置いた `path:` 参照 (identity はディレクトリ名から決まる) | `https://github.com/kamusoft/KsDialogs-SPM` + `exact:` | `Package.swift` をテンプレートから生成 |
| Android / KMP | 作業ディレクトリ内のローカル Maven リポジトリ (`<work>/maven`) | `mavenCentral()` | `exclusiveContent` で `jp.kamusoft` を 1 リポジトリに割り当て (`mavenLocal()` は宣言しない) |
| MAUI | 本リポジトリ由来の `KsDialogs.*` はフォルダフィードのみ (MAUI テンプレートの依存は nuget.org から) | nuget.org | `nuget.config` 2 枚を `-p:RestoreConfigFile=` で選び、packageSourceMapping で `KsDialogs.*` を 1 つに固定、実行ごとに空の `RestorePackagesPath` |

Maven のフィードは `~/.m2` ではなく作業ディレクトリ内に隔離する。発行は既存の `publishToMavenLocal` に `-Dmaven.repo.local=<work>/maven` を与えて行い、実行ごとに空から始まるため前回の残留物が解決に混ざらず、取得元がディレクトリ 1 つに確定する。

## フィード準備と artifact の配置

フィード準備の出力は、release workflow の package 段が upload する artifact と同じ構造で、消費者ビルドは両者を区別なく `--reference` で受ける。

| 形態 | フィード準備 | artifact のルート (必須の内容) |
|---|---|---|
| iOS | `scripts/spm-snapshot/sync-snapshot.sh` でスナップショットを配置 | 配信リポジトリのスナップショット (`Package.swift` / `Sources/` / `Tests/` / `LICENSE` / `README.md`)。`KsDialogs-SPM` の名前で展開する |
| Android | android/ を `-Pversion=` で発行 | ローカル Maven リポジトリのルート (`jp/kamusoft/...`)。`ksdialogs-core` と `ksdialogs` の POM・aar・`.module` |
| MAUI | 3 csproj を `-p:Version=` で `pack` | フォルダフィードのルート。`KsDialogs.Maui` / `KsDialogs.Binding.Android` / `KsDialogs.Binding.iOS` の `.nupkg` |
| KMP | 配信リポジトリのスナップショットを git 初期化して commit + tag (push はしない) → android/ の発行 (artifact があれば飛ばす) → kmp/ の `file://` 上書き付き発行 (artifact があっても行う) | Android と同じルート (kmp/ の 5 publication は job 内で同じルートへ発行する) |

KMP の artifact が Android 分だけなのは、package 段で作る kmp/ の成果物は Swift 参照が既定の https + exact で、その tag は publish 段まで存在せず dry-run では解決できないためである。

## KMP の dry-run の組み立て

KMP の dry-run は SNAPSHOT では成立しない。SNAPSHOT の Swift 参照は monorepo 内 `ios/` へのローカルパスで、消費者の合成 package が本体ソースを指してしまう ([発行 metadata の Swift 参照は version で決まる](../../kmp/api/ios-host-integration.md))。そのため dry-run はリリース版の合成 version で発行し、Swift 参照は配信リポジトリと同じ配置のスナップショットに version と同名の tag を打ったローカル clone の `file://` URL + exact にする。合成 package (Kotlin Gradle Plugin の `integrateLinkagePackage` が発行 metadata から生成し、Xcode project に組み込まれる SwiftPM package `KotlinMultiplatformLinkedPackage`) と iOS ホスト側のローカル package `VerificationApp` の依存が同じ URL を指すため、SwiftPM は 1 つの pin にまとめる。

消費者ビルドは (1) `:androidApp:assembleRelease`、(2) `XCODEPROJ_PATH` を与えた `:shared:integrateLinkagePackage` による合成 package の再生成と `linkReleaseFrameworkIosSimulatorArm64` による framework 生成、(3) `xcodebuild` Release (`CODE_SIGNING_ALLOWED=NO`、Simulator の arm64 のみ) を順に通し、前段の失敗で止まる。framework の link タスクは合成 package を生成も更新もしない。

追跡している `verification/kmp/iosApp/KotlinMultiplatformLinkedPackage/` と `VerificationApp/Package.swift` は、Swift 参照が https + `exact("0.0.0-alpha.0")` の **smoke 形の非解決 fixture** である。合成 version の tag は配信リポジトリに公開されないため、Xcode で開いて参照構造を確認できるだけで、そのままでは解決が通らない。消費者ビルドは `verification/kmp/` を作業ディレクトリへコピーし (合成 package の `subpackages/` はコピーせず再生成に作らせる — subpackage 名が version を含み、既定以外の version で 2 つ残ると検査が失敗する)、コピーの中で再生成してから走らせる。追跡している側は実行で変化せず、`file://` の絶対パスが作業ツリーに残らない。

kmp/ のリリース版発行は本体側の合成 Swift マニフェスト 2 本 (`kmp/.swiftpm-locks/default/swiftImport/subpackages/` 配下) を発行先の `file://` 絶対パスへ書き換える副作用を持つ。フィード準備は発行前にこの 2 本が HEAD から未変更であること (Git 管理下・追跡済み・作業ツリーと index に差分なし) を検査して変更済みなら発行せず失敗し、成否によらず復元する (異常終了は `EXIT` trap、成功時は trap を解いてから直接。復元は `git checkout HEAD --`)。

Xcode project (`VerificationKmp.xcodeproj`) は共有モジュールの static framework `VerificationShared`・合成 package・`VerificationApp` の 3 点を相対パスで参照し、`integrateLinkagePackage` が要求するシェルスクリプトのビルドフェーズ ("Build shared framework") が mode / reference / version / カタログを `KSDIALOGS_*` 環境変数で受ける。作業コピーでカタログを解決するため Gradle プロパティ `ksdialogs.catalog` (既定は `../../android/gradle/libs.versions.toml`) を持つ。登録と show はアプリ target (`KmpDialogRegistration.swift`) に置き、`VerificationApp` は配布物の公開面 (`@_exported import KsDialogs`) と View を持つ — SwiftPM の package target からは共有モジュールの framework を import できないため。

## 検査

消費者ビルドは Release ビルドの成否に加えて、形態ごとに配布物が意図どおり解決されたことを検査する。検査スクリプトは `--selftest` で負の入力を区別できることを示し、CI では自己テストを本検査より先に走らせる。

| 形態 | 検査するもの | 主体 |
|---|---|---|
| Android | Compose 側 `:app` で `ksdialogs-core` が `ksdialogs` と同版で推移解決されたこと、`:app-core` の `releaseRuntimeClasspath` に `androidx.compose` が 1 つも無いこと | `build-consumer.sh` |
| MAUI | facade と binding 2 件の解決版の一致、`.nupkg.metadata` の取得元が参照先と一致すること、platform TFM の target に binding が platform 固有アセットとして入っていること (API 版付き TFM を下回る消費者では警告なく platform 中立アセットにフォールバックし、版が一致していても native の binding が入らないため)、`XA4301` の件数 | `check-dependencies.py`、`WarningsAsErrors` (NU1605 / NU1608 / NU1107) |
| KMP | 5 publication (root / android / iOS 3 ターゲット) と推移の `ksdialogs-core` が同版で実解決されたこと、参照先の `swiftpm-metadata.json` の Swift 参照 (URL が mode どおり・`exact` が version・deployment target `17.0`。smoke では参照先を指せないため検査しない)、合成 package と `VerificationApp` の依存 URL の一致、`xcodebuild` の解決結果に `KsDialogs-SPM` の pin が 1 つだけあること | `check-dependencies.py` |

## CI と release からの呼び出し

形態別の reusable workflow `verify-consumer-{ios,android,maui,kmp}.yml` は `workflow_call` で `mode` (必須) / `version` / `artifact` (dry-run 専用) を受け、job 名は `verify`、権限は `contents: read` だけで secrets を受け取らない。検証 CI の入口 `ci.yml` は `main` 宛て pull_request でだけ `mode: dry-run` で 4 本を呼ぶ (status check 名は `consumer-<形態> / verify`)。release workflow は publish 前の dry-run に package 段の artifact を渡し (「dry-run が見たものと外に出るものが一致する」形)、publish 後の smoke に version を渡して同じ workflow を呼ぶ。

## 保証すること

- dry-run で解決される本リポジトリ由来の配布物は、その実行のフィード準備 (または渡された artifact) の内容だけである。
- 消費者検証の実行は配信先 (配信リポジトリ・Maven Central・nuget.org) へ書き込まない。発行は Maven local 宛てだけで、workflow は書き込み権限も認証情報も持たない。
- 追跡している `verification/` の内容と本体の `kmp/.swiftpm-locks/` は、消費者検証の実行で変化しない。
- README の最小例 4 つは消費者のソースと一致し、`develop` への push のたびに検査される。

## してはいけないこと

- 消費者プロジェクトから本体のソース (`includeBuild`・`ProjectReference`・`path:` で `ios/`) を参照しない: 配布物の解決を検証する意味が失われる。
- dry-run の参照先に `mavenLocal()` や `~/.nuget/packages` を混ぜない: 前回の残留物や別 version へのフォールバックが「解決できた」に見える。
- 追跡している KMP の fixture を直接ビルドしない: 作業コピーで再生成してから走らせる。fixture の Swift 参照を `file://` のまま commit しない。
- 消費者の AGP / Kotlin の版をカタログの外に直書きしない: 本体の版を上げても消費者が取り残され、Kotlin 版一致の実証が無音で崩れる。
- `verification/` 配下に `local.properties` を置かない: SDK は本体 build root の設定から引き継ぐ。

## 用語

| 用語 | 意味 |
|---|---|
| フィード準備 (`prepare-feed.sh`) | dry-run の参照先を作業ディレクトリ内に作る段。最終行に参照先のパスを出す |
| 消費者ビルド (`build-consumer.sh`) | 参照先を受けて消費者を Release ビルドし、検査と証跡を出す段 |
| 参照先 (`--reference`) | 準備済みの配布物の置き場。フィード準備の出力と release の artifact は同じ構造 |
| dry-run / smoke | 公開前 (作った・渡されたフィードを参照) / 公開後 (公開レジストリを参照) の検証モード |
| 配信リポジトリ・スナップショット | SwiftPM 専用の別リポジトリ `KsDialogs-SPM` と、そこへ commit する `ios/` の写し (同期スクリプトの出力) |
| 合成 version | dry-run の既定 version `0.0.0-alpha.0`。リリースしない値 |
| 合成 package | `integrateLinkagePackage` が発行 metadata から生成する SwiftPM package `KotlinMultiplatformLinkedPackage`。「合成 version」「合成 Swift マニフェスト」(本体 `kmp/.swiftpm-locks/` 配下) とは別物 |
| smoke 形 fixture | 追跡している KMP の合成 package と `Package.swift`。https + exact の形で、解決は通らない |

## 関連

- [検証 CI の範囲と実行条件](../../../handbook/cross/verification-ci.md) — 消費者検証 4 job の起動条件と lint job の検査一覧
- [ローカル開発環境の準備](../../../handbook/cross/local-development-setup.md) — 消費者検証を手元で回す手順と Android SDK の引き継ぎ
- [KMP 利用者の iOS ホスト統合](../../kmp/api/ios-host-integration.md) — 発行 metadata の Swift 参照が version で決まること、合成 package の再生成
- [cross/ADR-0008](../../../decisions/cross/0008-distribution-model-standard-channels.md) — 配布チャネルと配信リポジトリ、KMP の Swift 参照の導出
- [cross/ADR-0009](../../../decisions/cross/0009-lockstep-single-version.md) — lockstep 単一バージョンと `-Pversion=` の注入
- [cross/ADR-0017](../../../decisions/cross/0017-verification-ci-structure-and-guarantee.md) — 検証 CI の構成とトリガー
- [cross/ADR-0022](../../../decisions/cross/0022-lint-job-includes-readme-example-lint.md) — README 最小例の一致検査を lint job に加える決定
- 設計判断の出典: `kasane/changes/archive/2026-09-10-add-consumer-verification/design.md` (Decision 1〜6)
