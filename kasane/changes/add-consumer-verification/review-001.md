# レビュー結果: add-consumer-verification (001 回目)

**日付**: 2026-09-09
**判定**: CHANGES_REQUESTED

## サマリー

`verification/` 一式・検査 Python 3 本・消費者検証 workflow 4 本は、デルタスペック 2 本の Requirement / Scenario をほぼ全面的に満たしている。参照先の排他性 (`exclusiveContent` / packageSourceMapping / `path:`)、作業ディレクトリへの隔離、追跡 fixture の smoke 形固定と作業コピーでの再生成、入力検査の位置と `permissions: contents: read` / secrets 不在、SHA 固定、3 つの `--selftest` はいずれも実物で確認でき、自己テストは負の入力を本当に区別している (見せかけではない)。本体ソース (`ios/` `android/` `maui/` `kmp/` `samples/` `README.md`) は無変更で、README 最小例 4 ブロックと消費者 4 ファイルは完全一致する。

一方で、(1) KMP のフィード準備が失敗した場合に本体側の合成 Swift マニフェストが `file://` の絶対パスのまま作業ツリーに残る (deviation のオーナー判断 A が守ろうとした状態が失敗経路で破れる)、(2) Android 消費者だけがプラグイン版をカタログから取らず直書きしており、`ツールチェーンの再現性` の Scenario の保証機構が将来無音で崩れる、(3) `Select Xcode` の実体パス解決が maui 消費者だけにあり、同じ経路を踏む kmp / ios 消費者との差が未実証、の 3 点は commit 前に直したい。いずれも小さい修正で、現在の動作を壊している欠陥ではない。

加えて tasks 5.5 と 5.6 の CI 側 (draft PR での 10 job 起動と `artifact` 経路) が未実施で、verification-ci スペックの Scenario 群には実行証跡がない。証跡にオーナー実施待ちと明記されており実装側の欠落ではないが、change の完了判定はそこまで待つ必要がある。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| cross/comment-policy.md | 常時 (コメント構文を持つ全ファイル) |
| cross/verification-ci.md | `.github/workflows/` を変える・CI の検証範囲を確認する |
| cross/ci-script-deletion.md | `scripts/**`・`.github/workflows/**`・`verification/**` のスクリプトを作る・レビューする |
| cross/local-development-setup.md | Gradle build root のビルドを始める・環境構築 |

参照した実装スキル: kotlin-impl-skill (Gradle / Kotlin)、swift-ui-impl-skill (SwiftPM / xcodeproj)、github-workflow-skill (workflow)、csharp-impl-skill / maui-skill (MAUI)。`kasane/lessons/code-review.md` は不在。`kasane/lessons/process.md` の L-002 (主張の範囲と実証の範囲の一致) を証跡の読み方に適用した。

## 自分で実行した確認

- `python3 scripts/readme-example-lint.py` / `--selftest` → いずれも exit 0 (自己テスト 12 項目 OK、負の入力 3 種を区別)
- `python3 verification/maui/check-dependencies.py --selftest` → exit 0 (負の入力 5 種)
- `python3 verification/kmp/check-dependencies.py --selftest` → exit 0 (負の入力 5 種 = publication 欠落 / core 別版 / metadata 3 種 / URL 不一致 / pin 0 件と 2 件)
- `python3 scripts/identity-lint.py` / `local-path-lint.py` / `comment-policy-lint.py` / `scenario-id-coverage.py` → 違反 0 件。`--advisory` で `verification/` に出る 1 件は非公開 enum の doc コメント内 ADR 参照で、`samples/` の既存 8 件と同型 (配布物の公開面ではないため規約違反にあたらない)
- `actionlint .github/workflows/ci.yml .github/workflows/verify-consumer-*.yml` → 新規の指摘なし (SC2001 / SC2012 は変更前から repo にある同型)
- `shellcheck -x` (SC2034 / SC1091 除外) → `verification/lib/*.sh` の SC2148 のみ (下記 Suggestion)
- `git status --porcelain -uall verification/` → 65 ファイル。`build/` `.gradle/` `.kotlin/` `obj/` `bin/` `Package.resolved` はルート `.gitignore` で除外され追跡対象に入らない
- 追跡 fixture 2 つを実読 — 合成 package の subpackage manifest・`VerificationApp/Package.swift` とも `https://github.com/kamusoft/KsDialogs-SPM` + `exact: "0.0.0-alpha.0"` の smoke 形。`project.pbxproj` に `DEVELOPMENT_TEAM` もローカル絶対パスもなく、参照は `XCLocalSwiftPackageReference` 2 件 (`KotlinMultiplatformLinkedPackage` / `VerificationApp`) + `OTHER_LDFLAGS = -framework VerificationShared` の 3 点のみ
- 65 ファイル全件を `/Users/` `/Volumes/` `/var/folders/` `/private/tmp` で grep → 0 件。`Decision N` `phase-N` `tasks N.N` `spec.md` 等の禁止参照も 0 件
- gradle wrapper 3 本 (`android/` / `verification/android/` / `verification/kmp/`) の jar の sha256・`distributionUrl`・`distributionSha256Sum` が一致
- `ci.yml` の lint job の検査 step が 8 本で、handbook の内訳表と順序まで一致

## 指摘事項

### [🟡 Minor] KMP のフィード準備が失敗すると、本体側の合成 Swift マニフェストが `file://` のまま作業ツリーに残る

**該当箇所**: `verification/kmp/prepare-feed.sh:90-91`、`verification/lib/gradle-publish.sh:37-62`・`64-86`

**問題点**: `ksv_publish_kmp` は kmp/ の発行を通じて追跡ファイル `kmp/.swiftpm-locks/default/swiftImport/subpackages/_ksdialogs-kmp/Package.swift` と `_ksdialogs_kmp/Package.swift` を発行先の `file://` 絶対パスへ書き換える。復元は `ksv_restore_swiftpm_locks` が担うが、この呼び出しは成功経路 (次の行) にしかない。`set -euo pipefail` の下では、Gradle の発行が途中で落ちても、続く `ksv_require_maven_artifact` の 5 件のいずれかが落ちても、その時点で復元されずに終了する。deviation に記録されたオーナー判断 A の狙い —「手元実行のたびに identity lint に掛かる状態が作業ツリーに残るのを避ける」— は、いちばん起こりやすい失敗時に破れる。

もう 1 点、復元に使う `git checkout -- <paths>` は **index から** 復元する。書き換わった内容が誤って `git add` 済みだった場合、汚れた内容をそのまま戻したうえで「追跡状態へ戻した」と成功報告する。

**推奨修正**: `ksv_publish_kmp` を呼ぶ前に `trap ksv_restore_swiftpm_locks EXIT` を張る (または発行を関数で包み、成功・失敗のいずれでも復元を通す)。復元は `git checkout HEAD -- "${locks[@]}"` (もしくは `git restore --source=HEAD --`) にして、index の状態に依存させない。

### [🟡 Minor] Android 消費者だけプラグイン版がカタログ共有から外れており、Kotlin 版一致の保証が無音で崩れる

**該当箇所**: `verification/android/build.gradle.kts:6-10`

**問題点**: ルートの `plugins` ブロックが `com.android.application` を `9.3.0`、`org.jetbrains.kotlin.plugin.compose` を `2.4.10` と直書きし、コメントで「plugins ブロックはバージョンカタログの値を参照できないため … 直書きする」と説明している。しかし同じ change の `verification/kmp/build.gradle.kts` の `plugins` ブロック は、同じ形 (settings の `versionCatalogs { create("libs") { from(files(...)) } }`) で宣言したカタログに対して `alias(libs.plugins.kotlinMultiplatform)` / `alias(libs.plugins.androidKotlinMultiplatformLibrary)` を使えている。ルートの build ファイルでは参照できるので、コメントの説明は現状と食い違う (参照できないのは `samples/android/app/build.gradle.kts` のようなモジュール側の事情)。

実害は spec 側にある。verification-ci デルタスペック Requirement「ツールチェーンの再現性」は「消費者検証の Gradle (AGP / Kotlin) は本体のバージョンカタログ `android/gradle/libs.versions.toml` を共有し」と定め、design の Risks も「本体の AGP / Kotlin を上げると消費者も同時に上がる。消費者の KGP が確認済み版と一致することはこの共有が保証する」と書く。Android 消費者はこの保証の外にあり、カタログの `agp` / `kotlin` を上げても直書きの版が残る。AGP 9 の Kotlin は built-in kotlinc なので、版が取り残されても失敗せず、Scenario「消費者の Kotlin 版が本体と一致する」が誰にも気づかれずに偽になる。カタログに `composeCompiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }` がある以上、Kotlin 側は今すぐ共有へ寄せられる。

**推奨修正**: `org.jetbrains.kotlin.plugin.compose` を `alias(libs.plugins.composeCompiler) apply false` に置き換える。AGP はカタログに `androidApplication` の alias が無いため直書きが残るが、その理由 (カタログに application 用の alias が無い / 本体カタログの変更は本変更のスコープ外) を現在形でコメントに書き直し、「カタログを参照できない」という誤った説明を消す。カタログへ alias を足す判断はオーナーへ上げる。

### [🟡 Minor] `Select Xcode` の実体パス解決が消費者 maui だけにあり、同じ経路を踏む kmp / ios との差が未実証

**該当箇所**: `.github/workflows/verify-consumer-kmp.yml:87`、`.github/workflows/verify-consumer-ios.yml:82`、`.github/workflows/verify-consumer-maui.yml:81` (各 `Select Xcode` step)

**問題点**: consumer-maui だけが `app=$(cd "${app}" && pwd -P)` でシンボリックリンクを解決し、その理由を「消費者アプリの iOS Release ビルドは xcrun 経由で native ツールを探し、xcodebuild が DEVELOPER_DIR 配下の SDK を絶対パスで解決する。リンク経由のパスだと『SDK cannot be located』で落ちる (ライブラリのビルドだけを行う本体の検証はこの経路を踏まない)」と書いている。consumer-kmp は `xcodebuild -project ... -scheme VerificationKmp` でアプリを、consumer-ios は `xcodebuild -scheme VerificationApp` でパッケージを、いずれも Release でビルドするので「ライブラリのビルドだけ」ではない。3 本のどれが解決を必要とするかは、tasks 5.5 が未実施のため CI 上で一度も確かめられていない。手元実行 (evidence 5.1) では `DEVELOPER_DIR` を経由しないため、この差は再現しない。

**推奨修正**: 解決は無害なので 3 本に揃えるのが安全。揃えないなら、maui だけで足りる根拠 (どのツールがどの経路で SDK を引くか) を各 workflow のコメントで自己完結させ、tasks 5.5 の確認項目に「3 job とも Xcode 選択が効いていること」を足す。

### [🔵 Suggestion] source 専用のライブラリスクリプトに shellcheck のシェル指定がない

**該当箇所**: `verification/lib/verification-args.sh:1`、`verification/lib/android-sdk.sh:1`、`verification/lib/gradle-publish.sh:1`

**問題点**: 3 本とも shebang を持たない (source 専用なので正しい) が、`# shellcheck shell=bash` も無いため、単体で `shellcheck` に掛けると SC2148 が error として出る。bash 配列 (`local locks=(...)`) を使っているので、検査時に想定シェルが sh に倒れると誤検出も出る。

**推奨修正**: 各ファイルの先頭に `# shellcheck shell=bash` を 1 行足す。

### [🔵 Suggestion] MAUI 消費者の `uses-sdk` が最低対象 OS を三重に持っている

**該当箇所**: `verification/maui/Platforms/Android/AndroidManifest.xml:5`

**問題点**: `<uses-sdk android:minSdkVersion="24" android:targetSdkVersion="36" />` は、同じ csproj の `SupportedOSPlatformVersion` 24 と、Android / KMP 消費者がカタログから読む `android-minSdk` / `android-compileSdk` と重複する。マニフェストの `uses-sdk` は MSBuild プロパティより優先されるため、本体の最低対象 OS を動かしたときに MAUI 消費者だけが取り残される。他の 3 形態はいずれも値を持たずカタログ / プロパティに従っている。

**推奨修正**: `<uses-sdk>` を削り、csproj の `SupportedOSPlatformVersion` / `TargetPlatformVersion` に任せる。

### [🔵 Suggestion] 残っている受け入れ条件 (オーナー実施待ち) の扱い

**該当箇所**: `tasks.md` 5.5 / 5.6 (CI 部分)、`evidence/ci/4.1-4.4-static-checks.txt`、`evidence/verification/5.6-prepared-reference.txt`

**問題点**: verification-ci デルタスペックの Scenario のうち、「モードと version を与えた呼び出し」「artifact を与えた呼び出し」「smoke はフィード準備を行わない」(CI 側)「不正な入力で失敗する」(CI 側)「main 宛ての PR で起動する」「解決版と取得元が読める」(job summary) は、静的検査 (actionlint・job 名の突き合わせ・`permissions` / `secrets` の grep) までで、実行された証跡がない。証跡には「実際の CI 起動 (draft PR) はこの範囲では行わない」「オーナー実施待ち」と正直に書かれており、主張の範囲は実証の範囲に収まっている (lessons L-002 の観点では適合)。指摘は欠落ではなく、完了判定の前提の確認である。

**推奨修正**: 上記 Minor 3 件を直したうえで、tasks 5.5 / 5.6 をオーナーが通してから蒸留へ進む。5.5 では消費者 4 job の所要時間と、Minor 3 件目 (Xcode 選択) の効きを併せて記録すると、timeout の初期値 (30 / 30 / 40 / 30 分) の妥当性も同じ実行で埋まる。

## 確認して問題がなかった観点 (指摘なし)

- **デルタスペックの充足**: consumer-verification の 10 Requirement / 25 Scenario、verification-ci の 5 Requirement について、実装・スクリプト・workflow の対応を追跡した。手元で実証可能な Scenario はすべて証跡と実装が対応する
- **「配信先へ副作用を残さない」**: `verification/` 全体に push / api-key / token / 署名系の文字列が無く、発行タスクは Maven local 宛てのみ。workflow 4 本とも `permissions: contents: read` だけで `secrets:` / `inherit` を持たない。Central Portal の deployments だけは認証が要るため直接照会せず「経路の不在で示した」と証跡が明示的に範囲を区切っている
- **「本リポジトリ由来の座標はローカル参照先からのみ取得される」**: Android / KMP は `exclusiveContent` で `jp.kamusoft` を 1 リポジトリへ排他割り当て (`mavenLocal()` の宣言なし)、MAUI は `<clear/>` + packageSourceMapping + 実行ごとに空の `RestorePackagesPath`、iOS は `path:` 参照。負ケース (a) が Central / `~/.m2` / global packages folder のいずれにもフォールバックしないことを検索先の列挙で示している
- **「追跡している生成物は実行で変化しない」**: `build-consumer.sh` は `ksv_reset_dir` で作った作業コピーへ rsync し、`iosApp/KotlinMultiplatformLinkedPackage/subpackages/` を除外して 2 段目に作らせる。リポジトリ側の追跡物への書き込み経路は無い
- **削除前の対象検証 (cross/ci-script-deletion.md)**: `ksv_reset_dir` は絶対パス・単一階層・リポジトリ外の 3 点を全件検証してから `rm -rf` する。`verification/maui/build-consumer.sh` の `obj` / `bin` 除去は `SCRIPT_DIR` 由来の固定 2 パスに限定しシンボリックリンクを除外する (引数の誤指定で対象が動く構造ではない)。`rm` の使用自体は規約どおりで指摘しない
- **作業ディレクトリの既定とリポジトリ内拒否**: `ksv_prepare_work` は `pwd -P` で正規化してから `REPO_ROOT` 前方一致で拒否する。既定は `${TMPDIR:-/tmp}/ksdialogs-verification/<形態>`、CI は `${RUNNER_TEMP}/consumer`
- **不正な入力の早期失敗**: `ksv_parse_args` はフィード準備・SDK 解決より前に mode の許可値・smoke の version 必須・smoke への `--reference` を弾く。workflow 側も同じ 3 判定を checkout より前の step に置く
- **`workflow_call` の契約**: inputs 3 種 (`mode` required / `version` / `artifact` の default `""`)、job 名 `verify`、ランナーと timeout は spec のとおり。外部 action 5 種はすべて SHA 固定で、4 種は本体 workflow と同一 SHA、初出の `download-artifact@018cc2cf` は上流タグとの突き合わせが証跡にある
- **`ci.yml` の起動条件**: `on.pull_request.branches: [main]` のみなので、消費者 4 job の `if: github.event_name == 'pull_request'` は `main` 宛て PR に限られ、`develop` への push では起動しない。status check 名 10 件が冒頭コメントと一致する
- **検査 Python の自己テスト**: 3 本とも正の入力 1 件 + 負の入力 3〜5 種を持ち、失敗の理由文字列まで検査している。負の入力は判定ロジックを実際に通しており、見せかけではない
- **README 最小例 4 ブロックとの一致**: lint が通り、README.md・`README_ja.md`・本体 5 ルートに差分が無いことを `git status` で確認した
- **コメント規約**: 新規ファイルに作業文書のパス・変更識別子・ローカル通番・デルタスペック構文キーワード・履歴記述のいずれも無い。ADR 参照は `cross/ADR-0002` `cross/ADR-0019` `cross/ADR-0022` `core/ADR-0012` `maui/ADR-0004` の ID 形式のみ
- **handbook の書式**: `cross/verification-ci.md` は job 表に「起動」列を足し lint の 8 検査を内訳表へ、`cross/local-development-setup.md` は build root の表を 7 件にして「SDK の解決」列を新設。いずれも frontmatter の `description` / `timestamp` を追随させ、`doc-structure-lint` の新規違反 0 件。`concepts/log.md` への 1 行追記も append-only
- **deviation との整合**: 記録された 8 件はすべて実装と一致する (link タスクが再生成しないこと・subpackage 名に version が入ること・NU1102・`.swiftpm-locks` の復元・登録をアプリ target に置くこと・`ksdialogs.catalog` と `KSDIALOGS_*` 環境変数・MAUI の `obj`/`bin` 除去・smoke で metadata を検査しないこと)。記録内容と実装が食い違う箇所は見つからなかった

## アクションプラン

1. `verification/kmp/prepare-feed.sh` の合成 Swift マニフェスト復元を失敗経路でも通す (`trap` + `git checkout HEAD --`) — Minor 1
2. `verification/android/build.gradle.kts` の compose プラグインを `alias(libs.plugins.composeCompiler)` へ寄せ、AGP 直書きの理由コメントを事実に合わせて書き直す — Minor 2
3. `verify-consumer-{ios,kmp}.yml` の `Select Xcode` に実体パス解決を足す (揃えないなら根拠をコメントに残す) — Minor 3
4. `verification/lib/*.sh` に `# shellcheck shell=bash`、`verification/maui/Platforms/Android/AndroidManifest.xml` の `<uses-sdk>` を削除 — Suggestion 2 件
5. 上記の後、オーナーが tasks 5.5 / 5.6 (CI 側) を通し、所要時間と Xcode 選択の効きを証跡へ記録してから蒸留へ
