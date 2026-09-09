# 一致検証: add-consumer-verification (001 回目)

**日付**: 2026-09-09
**判定**: **VALID**
**対象**: デルタスペック 2 本 (`specs/consumer-verification/spec.md` — ADDED 11 Requirement / 28 Scenario、`specs/verification-ci/spec.md` — ADDED 1 + MODIFIED 4 Requirement / 20 Scenario)。合計 16 Requirement / 48 Scenario

❌ (未記録の欠落・乖離) は 0 件。虚偽チェックなし、足場の逆流なし、自己テスト 3 本と README 一致 lint は実行して成功を確認した。

## 判定記号

| 記号 | 意味 |
|---|---|
| ✅ | 実装があり、証跡または自己テストで Scenario の GIVEN/WHEN/THEN が確かめられている |
| ✅※ | 実装はあり静的に確認できるが、実行証跡が無い (CI 側はオーナー実施待ち / 実証範囲が証跡に明記されて限定されている)。内訳は「✅※ の内訳」節 |
| ⚠️ | `deviation.md` に記録済みの合意済み差分を含む |
| ❌ | 未記録の欠落・乖離 |

---

## 対応表 1: consumer-verification (ADDED)

### Requirement: 消費者プロジェクトの構成

| Scenario | 実装 | 検証手段 | 状態 |
|---|---|---|---|
| 本体ソースへの参照を持たない | `verification/android/settings.gradle.kts:47-71` / `verification/kmp/settings.gradle.kts:45-69` (`exclusiveContent` のみ、`includeBuild` 無し) / `verification/maui/VerificationApp.csproj:57-59` (`PackageReference` 1 行) / `verification/ios/Package.swift.template:25-35` | 静的確認 (本検証で `includeBuild` / `dependencySubstitution` / `ProjectReference` を全走査 → 該当 0 件。`../../android/gradle/libs.versions.toml` はスペックが要求する共有カタログでソース参照ではない)、`evidence/consumer/2.2〜2.5` | ✅ |
| README の最小例がそのままビルド対象になる | `verification/ios/Sources/VerificationApp/ContentView.swift` / `verification/android/readme-example/Notifications.kt` (`app/build.gradle.kts:50`・`app-core/build.gradle.kts:42` の `srcDir`) / `verification/maui/Notifications.cs` / `verification/kmp/shared/src/commonMain/kotlin/Confirm.kt` | `evidence/verification/5.1` (4 形態のビルド成功)、`scripts/readme-example-lint.py` を本検証で実行 (exit 0) | ✅ |
| KMP のホスト側は利用者向け手順の 3 点だけを参照する | `verification/kmp/iosApp/VerificationKmp.xcodeproj/project.pbxproj:216-217,231,258-259,266`(framework) / `:352-361`(`XCLocalSwiftPackageReference` 2 件) | 静的確認 (本検証で pbxproj を実読。参照は framework 検索パス + 合成 package + `VerificationApp` の 3 点のみ、`ios/` や monorepo パスへの参照 0 件)。登録コードの置き場は deviation 記録済み | ⚠️ |
| 追跡している生成物は実行で変化しない | `verification/kmp/build-consumer.sh:63-73` (作業コピー + subpackages 除外) / `verification/lib/gradle-publish.sh:66-129` (合成 Swift マニフェスト 2 本の trap 付き復元) / 追跡 fixture は smoke 形 (`iosApp/VerificationApp/Package.swift`・`KotlinMultiplatformLinkedPackage/**` とも `https://github.com/kamusoft/KsDialogs-SPM` + `exact("0.0.0-alpha.0")`) | `evidence/verification/5.1` (実行前後で `git status` 同一、追跡 28 本の shasum 一致、`kmp/.swiftpm-locks/` 差分なし)、`evidence/review-fix-001/1`。本検証で fixture 2 本の smoke 形を実読 | ⚠️ |
| 本体 build root の SDK 設定だけで消費者が動く | `verification/lib/android-sdk.sh:15-45`、`verification/android/prepare-feed.sh:28`・`build-consumer.sh:70`、`verification/kmp/prepare-feed.sh:41`・`build-consumer.sh:41` | `evidence/verification/5.1` (env 無しで `android/local.properties` から解決)、`5.2 (h)` (両方無い環境で 4 経路とも準備前に失敗) | ✅ |

### Requirement: モードと version の指定

| Scenario | 実装 | 検証手段 | 状態 |
|---|---|---|---|
| 引数なしで dry-run が動く | `verification/lib/verification-args.sh:26,130-191` (合成 version `0.0.0-alpha.0` の宣言は 1 か所、iOS は `KSV_DEFAULT_VERSION=""`) | `evidence/verification/5.1` (4 形態とも引数なしで exit 0) | ✅ |
| version を与えると全形態に同じ文字列が流れる | 同上 + `verification/lib/render-template.py` / 各 `build-consumer.sh` の `-Pksdialogs.version` / `-p:KsDialogsVersion=` | `evidence/verification/5.3` (`0.1.0-rc.1` が iOS 生成物・Android 座標・KMP 5 publication と POM の `ksdialogs-core`・metadata の exact・MAUI assets のすべてで一致) | ⚠️ (rsync 除外の修正が deviation 記録済み) |
| 不正な入力は早期に失敗する | `verification/lib/verification-args.sh:169-184` (mode 許可値 / smoke の version 必須 / smoke への `--reference` を作業ディレクトリ作成・SDK 解決の前に判定) | `evidence/verification/5.2 (f)` (4 形態 × 2 スクリプト × 3 種 = 24 通りすべて exit 1) | ✅ |

### Requirement: dry-run の参照先

| Scenario | 実装 | 検証手段 | 状態 |
|---|---|---|---|
| 本リポジトリ由来の座標はローカル参照先からのみ取得される | `verification/android/settings.gradle.kts:48-61` / `verification/kmp/settings.gradle.kts:48-59` (`exclusiveContent`) / `verification/maui/nuget.dry-run.config` (packageSourceMapping) + `build-consumer.sh:52` (空の `RestorePackagesPath`) / `verification/ios/build-consumer.sh:47` (`path:`) / `verification/lib/gradle-publish.sh` (`-Dmaven.repo.local`) | `evidence/verification/5.1` (4 形態とも取得元が作業ディレクトリ内。`~/.m2` の別版は現れない) | ✅ |
| ローカル参照先に無ければ公開済みの版でも失敗する | 同上 | `evidence/verification/5.2 (a)` (Android は検索先がローカル 2 URL のみ、MAUI は NU1102、KMP は Central を検索せず失敗。iOS は path 参照で version を持たないため参照先欠落で確認) | ⚠️ (失敗コード NU1102 が deviation 記録済み) |
| KMP の Swift 参照がローカル clone を指す | `verification/kmp/prepare-feed.sh:54-94` (clone + tag + `file://` 上書き発行) / `build-consumer.sh:57,87-90` / `check-dependencies.py:119-222` | `evidence/verification/5.1` (metadata・合成 package・ホスト側の 3 者が同じ `file://` URL + exact、`xcodebuild` の pin 1 件) | ✅ |
| 配信先へ副作用を残さない | 実行経路に push / upload / 認証情報が無い (`verification/**` 全走査で該当なし)、発行は `publishToMavenLocal` のみ | `evidence/verification/5.1` (配信リポジトリの tag 一覧・nuget.org のパッケージ一覧は実行前後同一。Central Portal の deployments は認証が要るため直接照会せず、経路とログでの確認に限る旨を証跡に明記 — lessons L-002 に適合) | ✅※ |

### Requirement: smoke の参照先

| Scenario | 実装 | 検証手段 | 状態 |
|---|---|---|---|
| 参照先が公開レジストリを指す | `verification/ios/build-consumer.sh:29,49` / `verification/kmp/build-consumer.sh:35,59-60` / `verification/android`・`verification/kmp` の `settings.gradle.kts` (smoke は `mavenCentral()`) / `verification/maui/nuget.smoke.config` | `evidence/verification/5.4` (4 形態とも生成物と検索先が公開レジストリのみ、ローカル参照先を含まない)。公開レジストリからの解決成功は proposal Non-Goals と 5.4 のとおり受け入れ条件外 (phase-9) | ✅ |

### Requirement: フィード準備と消費者ビルドの分離

| Scenario | 実装 | 検証手段 | 状態 |
|---|---|---|---|
| 外部で準備した配布物を消費者に渡す | 各 `prepare-feed.sh` / `build-consumer.sh` の `--reference` 分岐 (`verification/ios/build-consumer.sh:36-40`、`android:73-79`、`maui:32-37`、`kmp/prepare-feed.sh:81-94`)。配置は design Decision 3 の表と一致 | `evidence/verification/5.6` (4 形態ともフィード準備のログ 0 件で 5.1 と同結果。KMP は android/ の発行を飛ばし kmp/ の発行と tag だけ行う) | ✅ |

### Requirement: 消費者ビルドの成立条件

| Scenario | 実装 | 検証手段 | 状態 |
|---|---|---|---|
| 4 形態の Release ビルド | `verification/ios/build-consumer.sh:67-73` (`CODE_SIGNING_ALLOWED=NO`) / `android/build-consumer.sh:93` (2 app の `assembleRelease`) / `maui/build-consumer.sh:101-118` (`net10.0-android` と Simulator RID の `net10.0-ios`) / `kmp/build-consumer.sh:92-116` | `evidence/verification/5.1` (4 形態とも exit 0)、`evidence/scripts/3.1` | ✅ |
| KMP の 3 段は順に通る | `verification/kmp/build-consumer.sh:92-116` (1 段目 → `integrateLinkagePackage` + link → `xcodebuild`) | `evidence/verification/5.1` (3 段の順の成功)、`5.2 (g)` (1 段目・2 段目を落とすと後段の見出しが出力に現れない) | ⚠️ (再生成を担うタスクの差し替えが deviation 記録済み) |

### Requirement: Android 消費者の依存検査

| Scenario | 実装 | 検証手段 | 状態 |
|---|---|---|---|
| 推移の本体が同版で届く | `verification/android/build-consumer.sh:111-124,131-141` | `evidence/verification/5.1`・`5.3` (`ksdialogs` と推移の `ksdialogs-core` が同版、両者の行が証跡に出る) | ✅ |
| 本体だけの消費者に Compose が届かない | `verification/android/build-consumer.sh:126-129`、`verification/android/app-core/build.gradle.kts:53-56` (依存 1 行のみ) | `evidence/verification/5.1` (0 件)、`5.2 (d)` (Compose を 1 行足すと 63 件を検出して失敗) | ✅ |

### Requirement: MAUI 消費者の依存検査

| Scenario | 実装 | 検証手段 | 状態 |
|---|---|---|---|
| 依存警告で失敗する | `verification/maui/VerificationApp.csproj:43-45` (`WarningsAsErrors` に NU1605 / NU1608 / NU1107) | 静的確認のみ (負ケースの実行証跡は無い。tasks 5.2 の (a)〜(i) にも含まれていない。`5.2 (b)` で NU1603 が警告のまま出ることから restore 警告が表面化する経路自体は確認できる) | ✅※ |
| binding の version 不一致を検出する | `verification/maui/check-dependencies.py:146-208` | `evidence/verification/5.2 (b)` (binding だけ別版のフィードで exit 1、facade と binding の解決版が出力に出る)、本検証で `--selftest` 実行 (10 項目 OK) | ✅ |
| platform 中立アセットへのフォールバックを検出する | `verification/maui/check-dependencies.py:84-143` | `evidence/verification/5.2 (c)` (TFM を下げて exit 1)、`--selftest` の該当項目 | ✅ |
| ビルド警告は失敗にしない | `verification/maui/build-consumer.sh:106-114` (XA4301 は数だけ証跡へ)、`WarningsAsErrors` は 3 コードに限定 | `evidence/verification/5.1` (Release ビルド成功、XA4301 検出なし) | ✅ |

### Requirement: KMP 消費者の依存検査

| Scenario | 実装 | 検証手段 | 状態 |
|---|---|---|---|
| 全 publication が解決される | `verification/kmp/check-dependencies.py:154-193`、`build-consumer.sh:119-149` | `evidence/verification/5.1`・`5.3` (androidApp と iOS 3 ターゲットで 5 publication + `ksdialogs-core` が同版) | ✅ |
| 検査は負の入力で失敗する | `verification/kmp/check-dependencies.py:288-397` (`--selftest`) | 本検証で実行 (正 1 + 負 5 種 = publication 欠落 / core 別版 / metadata 3 種 / URL 不一致 / pin 0 件・2 件、すべて OK)、`evidence/verification/5.2 (i)` | ✅ |
| Swift 参照がモードと一致する | `verification/kmp/check-dependencies.py:119-151` (URL / exact / deployment target `17.0`) | dry-run は `evidence/verification/5.1`・`5.3` で実測。smoke は生成物の静的確認 (`5.4`) で、metadata 検査は行わない旨が deviation 記録済み | ⚠️ |

### Requirement: 解決結果の証跡

| Scenario | 実装 | 検証手段 | 状態 |
|---|---|---|---|
| 解決版と取得元が読める | `verification/lib/verification-args.sh:37-55` (`ksv_evidence`、`GITHUB_STEP_SUMMARY` へも出力)、iOS `build-consumer.sh:62-82` / Android `:131-141` / MAUI `:94-99` / KMP `:151` | 手元は `evidence/verification/5.1`・`5.3`・`5.6` で 4 形態とも解決版と取得元が特定できる。CI の job summary 側は実行証跡がオーナー実施待ち | ✅※ |

### Requirement: README 最小例との一致

| Scenario | 実装 | 検証手段 | 状態 |
|---|---|---|---|
| 例の変更が消費者に追随していなければ失敗する | `scripts/readme-example-lint.py:24-34,116-192` | `evidence/verification/5.2 (e)` (片側変更で exit 1、不一致のファイルが出力)、本検証で `--selftest` 実行 (16 項目 OK。節の重複・ブロックの重複・対応先不在も検出) | ✅ |
| 一致していれば通る | 同上 | 本検証で `python3 scripts/readme-example-lint.py` を実行 → exit 0「README の最小例 4 件が消費者検証のソースと一致する」 | ✅ |

---

## 対応表 2: verification-ci

### ADDED Requirement: 消費者検証 workflow の再利用契約

| Scenario | 実装 | 検証手段 | 状態 |
|---|---|---|---|
| モードと version を与えた呼び出し | `.github/workflows/verify-consumer-{ios,android,maui,kmp}.yml` の `on.workflow_call.inputs` と `Verify consumer` step | 本検証で 4 本を YAML パース: トリガーは `workflow_call` のみ、inputs は `mode`(required) / `version` / `artifact`、job は `verify` 1 つ、`permissions: contents: read`、`secrets` 無し、runs-on = macos-26 / ubuntu-24.04 / macos-26 / macos-26、timeout = 30 / 30 / 40 / 30。`evidence/ci/4.1-4.4` | ✅※ |
| artifact を与えた呼び出し | 各 workflow の `Download prepared distribution` (`if: inputs.artifact != ''`) と `--reference` 受け渡し。配置は design Decision 3 の表と一致 (iOS は `.../reference/KsDialogs-SPM`、他は `.../reference`) | 静的確認 (本検証で download パスと design 表の突き合わせ)。手元側の等価な経路は `evidence/verification/5.6` で実測。**CI 上の実行証跡はオーナー実施待ち (tasks 5.6)** | ✅※ |
| smoke はフィード準備を行わない | 各 `prepare-feed.sh` の smoke 早期 return、`build-consumer.sh` の smoke 分岐 | `evidence/verification/5.4` (4 形態とも準備物が作られない)。CI 側は未実行 | ✅ |
| 不正な入力で失敗する | 各 workflow の `Validate inputs` step (checkout より前、3 判定) | 静的確認 (本検証で 4 本とも step 順が Validate → Checkout であることを確認)。手元スクリプトの同じ 3 判定は `5.2 (f)` で実測 | ✅※ |
| 配信先への書き込み経路を持たない | 4 本の `permissions: contents: read`、`secrets:` 宣言なし、`ci.yml` の呼び出し側も `with:` のみ | 本検証の YAML パースで確認。`evidence/ci/4.1-4.4` | ✅ |

### MODIFIED Requirement: CI の起動条件

| Scenario | 実装 | 検証手段 | 状態 |
|---|---|---|---|
| develop への push で本体検証と lint が起動する | `.github/workflows/ci.yml` の `on` (push: develop / pull_request: main)、消費者 4 job の `if: github.event_name == 'pull_request'` | 静的確認 (消費者 job は push で起動しない条件式)。**実行証跡はオーナー実施待ち (tasks 5.5)** | ✅※ |
| main 宛ての PR で起動する | `ci.yml` の消費者 4 job (`consumer-ios` / `consumer-android` / `consumer-maui` / `consumer-kmp`、`mode: dry-run`) と本体 5 job | 静的確認 + `evidence/ci/4.1-4.4` (status check 名 10 件が冒頭コメントと機械的に一致)。**実行証跡はオーナー実施待ち** | ✅※ |
| 消費者検証は dry-run で動く | `ci.yml` の `with: mode: dry-run` (version / artifact を渡さない)、workflow 側の `permissions` と secrets 不在 | 静的確認 + 手元の dry-run 実測 (`5.1`)。**CI 実行証跡はオーナー実施待ち** | ✅※ |
| 記録だけの push では lint だけが走る | `ci.yml` の `changes` job (本変更で未改変) | 静的確認 (本変更は当該ロジックに触れていない) | ✅ |
| 記録とソースが混ざった push では全 job が走る | 同上 | 同上 | ✅ |
| 連続する push で古い実行が打ち切られる | `ci.yml` の `concurrency` (未改変) | 同上 | ✅ |

### MODIFIED Requirement: platform workflow の再利用契約

| Scenario | 実装 | 検証手段 | 状態 |
|---|---|---|---|
| 別 workflow からの呼び出し | 本体 5 本 + 消費者 4 本とも `workflow_call` | 本検証の YAML パース (消費者 4 本)、`evidence/ci/4.1-4.4` | ✅ |
| status check 名が固定される | `ci.yml` の呼び出し側 job 名 4 件 + 呼ばれる側 `name: verify`、冒頭コメントの 10 件一覧 | `evidence/ci/4.1-4.4` (jobs から機械的に組み立てた 10 件がコメントと一致)。本検証でも job 名を再確認 | ✅ |

### MODIFIED Requirement: lint の検証

| Scenario | 実装 | 検証手段 | 状態 |
|---|---|---|---|
| 違反の検出 | `ci.yml` lint job の 8 step (`README example lint` を追加) | 本検証で lint job の検査 step が 8 本であることを確認。`evidence/ci/4.1-4.4` (identity / local-path の正の対照、comment-policy / scenario-id-coverage の 0 件)、`5.2 (e)` (README 一致の負ケース) | ✅ |
| ソースルート配下の識別子検出 | `kasane/config.yaml` の `lint.identity.scope` に `verification` を追加 | `evidence/ci/4.1-4.4` (scope 別件数に verification 65、`DEVELOPMENT_TEAM` を仕込んだ正の対照で検出 → 除去後 0 件) | ✅ |
| secret scan の空振り検出 | `ci.yml` の gitleaks step (未改変) | 静的確認 (本変更は当該 step に触れていない) | ✅ |

### MODIFIED Requirement: ツールチェーンの再現性

| Scenario | 実装 | 検証手段 | 状態 |
|---|---|---|---|
| 版の変更が diff に現れる | 消費者 4 workflow の `KS_XCODE_VERSION: "26.5"` / `runs-on` 版指定 / `setup-java` Temurin 17 / `global-json-file: global.json`、`verification/maui/VerificationApp.csproj:53` (`$(MauiVersion)`)、Gradle は共有カタログ、外部 action は SHA 固定 | 静的確認 + `evidence/ci/4.1-4.4` (SHA 5 種の突き合わせ、初出の download-artifact は上流 tag と照合) | ✅ |
| 手元のビルドが repo の設定で固定される | repo 直下 `global.json` (未改変) を消費者 MAUI がそのまま使う | `evidence/verification/5.1` (手元で MAUI 消費者の Release ビルドが成功) | ✅ |
| 指定した Xcode が無ければ失敗する | `verify-consumer-{ios,maui,kmp}.yml` の `Select Xcode` step (不在時に `::error::` + `ls -d /Applications/Xcode_*.app`) | 静的確認 + `evidence/review-fix-001/3` (3 本の step が逐語一致)。**CI 実行証跡はオーナー実施待ち** | ✅※ |
| 消費者の Kotlin 版が本体と一致する | `verification/android/settings.gradle.kts:76-80` / `verification/kmp/settings.gradle.kts:77-83` (カタログ共有)、`verification/android/build.gradle.kts:5-10` | `evidence/verification/5.7` (Android / KMP とも KGP・stdlib が 2.4.10) | ✅ |

---

## ✅※ の内訳 (実行証跡の限定)

| Scenario | 限定の内容 | 扱い |
|---|---|---|
| main 宛ての PR で起動する / develop への push で本体検証と lint が起動する / 消費者検証は dry-run で動く / モードと version を与えた呼び出し / artifact を与えた呼び出し / 不正な入力で失敗する (CI 側) / 解決版と取得元が読める (job summary) / 指定した Xcode が無ければ失敗する | draft PR での CI 起動が tasks 5.5・5.6 として未実施 | オーナー実施待ち。証跡に明記されており実装側の欠落ではないため ❌ にしない (`evidence/ci/4.1-4.4` の冒頭・`5.6` の末尾・`review-fix-001/3` の末尾に明記) |
| 参照先が公開レジストリを指す | 公開レジストリからの解決成功は受け入れ条件外 (proposal Non-Goals・`5.4` 冒頭・spec 本文で phase-9 へ明示) | 仕様どおり。生成物と検索先の静的確認までで成立 |
| 配信先へ副作用を残さない | Central Portal の deployments は認証が要るため直接照会していない (tag 一覧と nuget.org は実測) | 証跡が観測していない面を明示 (lessons L-002 に適合)。THEN の後半 (書き込み権限・認証情報を持たない) は経路の全走査で成立 |
| 依存警告で失敗する (MAUI) | NU1605 / NU1608 / NU1107 を故意に起こす負ケースの実行証跡が無い (tasks 5.2 の計画にも無い) | 宣言 (`WarningsAsErrors`) は spec の記述どおりで乖離ではない。実証が欲しければ phase-9 か別変更で 1 ケース足すのが自然 |

---

## 追加検査

### tasks.md の完了状況と虚偽チェック

- 全 29 タスク中 27 が `[x]`、未完は **5.5**(CI の起動確認) と **5.6**(CI の `artifact` 経路) の 2 件で、いずれも「オーナー実施待ち」と本文・証跡が一致する。5.6 の手元分だけを先行して通した記録は `evidence/verification/5.6` にあり、タスク自体は未チェックのまま — 過大申告になっていない
- `[x]` の 27 件はすべて対応表の実装・証跡に対応する。**未実装なのにチェック済みのものは無い**

### 逆流検査 (足場アーティファクトの書き換え)

- `git status` で change ディレクトリの変更は `tasks.md` のみ。`proposal.md` / `design.md` / `specs/**` に未 commit の変更は無い
- `tasks.md` の diff は 27 行の増減で、本検証でチェックボックス正規化後に突き合わせた結果 **差分ゼロ** (`- [ ]` → `- [x]` の切り替えのみ。タスク本文の書き換えは無い)

### 付随修正・deviation との突き合わせ

- `deviation.md` は 9 項目 (うち `[付随修正]` 1 件 = MAUI の残存 `obj/` `bin/` 除去)。diff 中で Scenario に直接対応しない実装判断 (`ksdialogs.catalog` プロパティ、`KSDIALOGS_*` 環境変数、`EXCLUDED_ARCHS`、rsync の subpackages 除外、`kmp/.swiftpm-locks/` の復元) はいずれも deviation に記録済み
- 記録が無いのに Scenario にも対応しない変更: **0 件**。`kasane/concepts/log.md` の 1 行追記は tasks 4.4 (handbook 追随) の記録で、ハーネス側の記帳

### テスト・検査の実行 (本検証で実行)

| 実行 | 結果 |
|---|---|
| `python3 verification/maui/check-dependencies.py --selftest` | exit 0 (正 1 + 負 5 種、10 項目 OK) |
| `python3 verification/kmp/check-dependencies.py --selftest` | exit 0 (正 1 + 負 5 種、13 項目 OK) |
| `python3 scripts/readme-example-lint.py --selftest` | exit 0 (16 項目 OK) |
| `python3 scripts/readme-example-lint.py` | exit 0 (README 最小例 4 件が消費者ソースと一致) |
| 消費者 4 workflow の YAML パース (トリガー / inputs / permissions / secrets / runs-on / timeout / job 名) | spec の再利用契約と全項目一致 |
| `verification/` の本体ソース参照走査 (`includeBuild` / `dependencySubstitution` / `ProjectReference` / `package(path:`) | 該当 0 件 (合成 package 内部の相対 path 参照を除く) |

4 形態のフルビルドは本検証の範囲外 (指示どおり)。ビルド成立は `evidence/verification/5.1`・`5.3`・`5.6` に依拠した。

### UI 変更

- 本変更に `ui/` アーティファクトは無い (UI 変更なし)。該当なし

---

## 参考: レビューとの関係 (判定には用いない)

`review-001.md` は CHANGES_REQUESTED (Minor 3 + Suggestion 3) で、`evidence/review-fix-001/1〜5` に修正の記録がある。本検証で実装を実読した限り、Minor 3 件 (合成 Swift マニフェストの trap 付き復元 / Android 消費者の compose プラグインのカタログ寄せ / `Select Xcode` の実体パス解決の 3 本揃え) と Suggestion 2 件 (`# shellcheck shell=bash`、README 節の重複検出) はいずれも実装に反映されている。再レビュー (review-002) の有無はオーケストレーターの判断領域であり、本検証の判定には含めない。
