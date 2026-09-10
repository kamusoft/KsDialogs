# 消費者検証 (consumer-verification)

配布物を参照する消費者プロジェクト (`verification/`、4 形態) と、それを dry-run / smoke の 2 モードで回す reusable workflow を KsSettingsView から踏襲して整備する change フェーズ。KMP の 4 本目は phase-7 の結論に従って実装する。

## 論点

議論する論点は残っていない (4 件とも 2026-09-09 に決定事項へ)。以下は先行フェーズからの申し送りで、提案化の入力として残す。

### phase-4 からの申し送り (2026-09-08)

入口 `.github/workflows/ci.yml` に消費者検証 job の枠は無い (存在しない workflow を `uses:` できないため)。`verify-consumer-<platform>.yml` を作るときに入口へ job を足し、`if: github.event_name == 'pull_request'` で `main` 宛て PR に限る。job 名は phase-9 の必須 check 名 (`consumer-<platform> / verify`) に合わせる。reusable workflow の書き方 (Xcode 選択・`global.json` 参照・件数検査・SHA 固定) は本体検証 5 本 (cross/ADR-0017 / 0018) を写す。

### phase-6 からの申し送り (2026-09-08)

MAUI 消費者 (`verification/maui`) は phase-6 の一時消費者 (`kasane/changes/archive/2026-09-08-add-maui-nuget-distribution/evidence/consumer-verification/README.md`) の形を写す。`dotnet new maui` の生成物に足すのは `TargetFrameworks` / `RestorePackagesPath` (隔離) / `WarningsAsErrors` (NU1605・NU1608・NU1107) / `SupportedOSPlatformVersion` (Android 24 / iOS 17.0) / facade の PackageReference 1 行だけで、`Microsoft.Maui.Controls` の版は書かない。`nuget.config` は `<clear/>` + ローカルフィード + nuget.org の併記 (ローカルだけだとテンプレート依存が NU1101)。3 パッケージがローカルフィードから取得されたことは `pkgs/<id>/<ver>/.nupkg.metadata` の `source` で確かめる。Android Release で R8 を効かせるには消費者側で `AndroidLinkTool=r8` を明示する (SDK 既定は空)。

確認事項: API 版付き TFM (`net10.0-android36.0` / `net10.0-ios26.0`) を下回る `TargetPlatformVersion` を固定した消費者では、警告なく platform 中立アセットにフォールバックし binding が入らない (既知の落とし穴)。`check-dependencies.py` で binding が facade と同版で解決されたことに加え、platform TFM で binding が実際に入ったことを検査対象にする。消費者ビルドで `XA4301` が 0 件であることも再確認する (phase-6 では自 assembly 用 aar が生成されず 0 件)。

### phase-7 からの申し送り (2026-09-09)

KMP の発行設定は実装済み (`kasane/changes/archive/2026-09-09-add-kmp-maven-distribution/`)。dry-run が前提にする機構は次のとおり確認済み: `-Pversion=` の注入で Swift 参照が `https://github.com/kamusoft/KsDialogs-SPM` + exact(version) のリモート参照になる / `-Pksdialogs.swiftPackageUrl=file:///<tag 付きローカル clone>` で `file://` URL + exact のリモート参照になる (C1 の前提、KGP 2.4.10 で実測) / `swiftpm-metadata.json` の deployment target は参照種別によらず `17.0`。

確認事項: 既定 URL でのリリース版発行は root publication 単独でしか実証していない (iOS publication の発行は配信リポジトリに同版 tag が要るため)。全 publication (root / android / iOS 3 ターゲット) の POM・`.module`・klib・aar が Swift 参照の URL に依存しないことは root の突き合わせまでの実証なので、dry-run (`file://`) と smoke (https) の両方で全 publication を実解決し、Android app の `assembleRelease` と iOS の framework リンクまで通す (C2)。消費者の KGP 版で走る段 (C2 (2)) は動作確認済み Kotlin 版 (カタログ値、現在 2.4.10) の実証を兼ねる。

## 決定事項

踏襲 (解決済み論点)。出典は KsSettingsView phase-7 の決定事項と実測 (`../KsSettingsView/kasane/roadmaps/package-distribution/phases/phase-7-consumer-verification/agenda.md`)。

| 主題 | 踏襲する内容 |
|---|---|
| 検証範囲 | 「解決 + Release ビルド」まで (起動・`dotnet publish`・実機は含めない)。iOS = SwiftPM パッケージ (pbxproj を持たず `DEVELOPMENT_TEAM` 事故が構造的に起きない)、Android = `com.android.application` の app (manifest merger / R8 / dex が app でしか走らない)、MAUI = `dotnet new maui` 相当 |
| dry-run の参照先 | iOS = スナップショットの `path:` (一時ディレクトリ名は実レジストリと同じ `KsDialogs-SPM`。ディレクトリ名から package identity が決まる) / Android = `mavenLocal()` を `exclusiveContent` で / MAUI = ローカルフォルダフィード + packageSourceMapping (nuget.org 併記、実行ごとに空の packages path) |
| スクリプト構成 | 1 構成 + 2 引数 (mode と version)。各 platform に `prepare-feed.sh` (フィード準備、最終行に参照先を出力) と `build-consumer.sh` (消費者ビルド、`--reference` で受け取る) の 2 段、共通引数解釈は `verification/lib/verification-args.sh`。MAUI は `check-dependencies.py` で binding が facade と同版で解決されたかを検査 |
| workflow | `verify-consumer-<platform>.yml` (`workflow_call`、入力 `mode` / `version` / `artifact`)。`permissions: contents: read` のみで secrets を受け取らない。入力検査は checkout より前 |
| release との接続 | dry-run 段では package 段の artifact を渡し「dry-run が見たものと外に出るものが一致する」形にする。smoke は公開レジストリを参照し version 必須、反映待ち job を挟む |
| 落とし穴 (Gradle / NuGet) | Gradle の `content { includeGroup }` は排他でない (`exclusiveContent` を使う) / NuGet の mapping は global packages folder 既存分に効かない / `<clear/>` + ローカルフィードのみだと MAUI テンプレート依存が NU1101。MAUI 消費者は NU1605 / NU1608 / NU1107 を `WarningsAsErrors` |
| 落とし穴 (Xcode / TFM) | Xcode パスがシンボリックリンクだと trimming 後の `install_name_tool` が失敗 (`DEVELOPER_DIR` を `pwd -P` で実体解決) / API 版付き TFM を下回る消費者では警告なく platform 中立アセットにフォールバックし binding が入らない |

- 却下済み: 起動・実機まで検証する / 消費者検証を開発ブランチで毎回 (cross/ADR-0028)

### 1 KMP 消費者は `verification/kmp/` 1 つ、macOS 1 job で 3 段を順に通す (2026-09-09)

KMP 消費者は利用者と同じ 1 つの Gradle プロジェクト (`shared` + `androidApp` + `iosApp`、`VerificationApp` の `Package.swift` はテンプレート生成) を `verification/kmp/` に置き、workflow `verify-consumer-kmp.yml` の job は `verify` 1 つ (`macos-26`) で「フィード準備 (kmp/ を `file://` 上書き付きで `publishToMavenLocal`) → C2 (1) Android app の `assembleRelease` → (2) `linkReleaseFrameworkIosSimulatorArm64` → (3) `xcodebuild` Release」を step で分けて順に通す。KMP のフィード準備は iOS publication (Swift package import の cinterop klib) を伴うため macOS でしか行えず、Android 段を Ubuntu に分けてもフィードを artifact で渡す 2 段になるだけで macOS の占有時間は減らない。1 プロジェクトにすることで shared モジュールと README の依存 1 行の複製 (論点 4 の検査対象の増加) を避け、必須 check 名は phase-4 の申し送りどおり `consumer-kmp / verify` の 1 つで済む。timeout は 30 分を初期値にし、着手時に実測して詰める (見込み 12〜18 分。本体 `kmp / verify` の実測 7 分 + Release ビルドと xcodebuild)。

- 却下: Android 消費者と iOS 消費者を別ディレクトリ・別 job に分ける (shared モジュールが 2 つに複製され、check 名が 2 つになる。主眼は iOS 側 (2)・(3) で切り分けの利点が薄い)
- 却下: 置き場 1 つのまま workflow 内で Android job (Ubuntu) と iOS job (macOS) に分ける (フィードの artifact 受け渡しと check 名 2 つが増えるだけ)

### 2 Android 消費者は Compose 系 app と `-core` 単独 app の 2 モジュールを 1 job で回す (2026-09-09)

`verification/android/` は 1 つの Gradle プロジェクトに 2 つの `com.android.application` モジュールを置く: Compose 系 `jp.kamusoft:ksdialogs` 1 行の app (Sample の androidApp と同じく Compose の基盤を足す) と、View 系本体 `jp.kamusoft:ksdialogs-core` 1 行の app。同じ job (`consumer-android / verify`) で両方を `assembleRelease` する。KsDialogs は 2 artifact を維持し (android/ADR-0001、roadmap の非ゴール)、cross/ADR-0019 が利用者の書き方を 2 通り (Compose は素の名前、View 系だけは `-core`) に定めているため、README の導入例 2 通りを利用者と同じ 1 行で網羅する。検査は翻案元の `releaseRuntimeClasspath` の `jp.kamusoft` 行に加えて 2 点: Compose 側の app で `ksdialogs-core` が facade と同版で推移的に解決されたこと (cross/ADR-0009)、`-core` 側の app の classpath に `androidx.compose` が無いこと (ADR-0019 の「Compose を使わない消費者に Compose が届かない」保証)。README の Android 最小例 (View 系 `Toast` の呼び出し) は 2 モジュールで共有するソースディレクトリに 1 か所だけ置く。追加コストはモジュール 1 つと Release ビルド 1 回 (1〜2 分見込み) で、job と check 名は増えない。

- 却下: Compose 系 1 行の app だけ (翻案元どおり。`-core` 単独の解決と Compose 非混入の保証を直接は確かめられない)
- 却下: `-core` 単独の app だけ (多数派の Compose 経路と推移の同版検査が抜ける)

### 3 トリガーは phase-4 の結論のまま、MAUI 消費者の timeout は 40 分を初期値に実測で詰める (2026-09-09)

消費者検証のトリガーは phase-4 の決定事項 (`develop` への push は lint + 本体検証、`main` 宛て PR で消費者検証 4 本を dry-run で足す。cross/ADR-0028 の逆流) と踏襲の決定事項 (release の dry-run は package 段の artifact、smoke は公開レジストリ) で決まっており、phase-8 で追加の判断はしない。入口 `ci.yml` への追加は phase-4 の申し送りどおり (`if: github.event_name == 'pull_request'`、version と artifact は渡さない)。MAUI 消費者の timeout は翻案元と同じ 40 分を初期値にする: 翻案元の実測 20 分 (workload 導入と Android R8 / iOS trimming の Release ビルドが大半) に対し 2 倍の余裕で、本体 `maui / verify` の 35 分より長い順序が自然。着手時に実測して詰める。`main` 宛て PR では macOS の job が 6 つ (本体 3 + 消費者 3) 並び public リポジトリの同時実行上限 (macOS 5) で 1 つが待ちに入るため、PR 全体の壁時計も実測時に記録する。

- 却下: timeout 30 分 (他の消費者 job と揃うが、実測 20 分に対し 1.5 倍で workload 導入が遅い日に落ちる余地) / 60 分 (ハング時の占有が長い)

### 4 逐語一致 lint の対象は英語 README の最小例 4 ブロックだけ、KMP のホスト側は lint 対象外 (2026-09-09)

翻案元 `scripts/readme-example-lint.py` (英語 README の最小例の節だけを対象に、(小見出し, fence 言語) → `verification/` 配下のファイルの対応表で末尾の改行まで完全一致を検査。`README_ja` は対象外で英日の同期は docs-refresh の責務) を写し、対応表を KsDialogs の見出しに差し替える。README の最小例は「1 行で見せる入口」で完結した手順ではなく、KMP の例 (共有コードの `Dialog.instance.show`) を動かすためのホスト側の View 登録 (iOS = Swift の `Dialog.shared.kmp.register`、Android = Android Native の登録) はスキルが担う分担 (cross/ADR-0011・0012)。KMP 消費者のホスト側の糊は concepts `kmp/api/ios-host-integration.md` の例から書き、lint の対象にしない。C2 (3) の主眼 (Swift 登録 API のリンク) は消費者ビルドが通ることで担保される。README の英語版の最小例 4 つは現行 API と対応が取れていることを確認済み (iOS `Toast.shared.show(message:)` / Android `Toast.instance.show(String)` (interface 側に既定引数) / MAUI `Toast.Instance.Show(string)` / KMP `Dialog.instance.show` + `DialogViewModel<R>`) で、同梱前に README を直す必要はない。

| README の小見出し | fence | 同梱先 |
|---|---|---|
| `### iOS Native` | swift | `verification/ios/Sources/VerificationApp/` 配下の `.swift` |
| `### Android Native` | kotlin | `verification/android/` の 2 モジュールが共有するソースディレクトリの `.kt` (決定 2) |
| `### .NET MAUI` | csharp | `verification/maui/` 配下の `.cs` (翻案元と違い XAML のブロックは無い) |
| `### Kotlin Multiplatform` | kotlin | `verification/kmp/shared/src/commonMain/` 配下の `.kt` |

- 却下: スキル `ksdialogs-kmp/references/ios-host.md` の登録例も lint (スキルは docs-refresh の出力そのもので、翻案・分割で例の形が変わるたびに消費者が追随を迫られる)
- 却下: README に KMP のホスト側 (iOS / Android) の例を足して 6 ブロックを lint (README の構成変更で phase-8 のスコープを超え、phase-9 の docs-refresh と二重に README を触る)
- 運用: README の最小例が変わるのは docs-refresh 経由で、lint が赤になったら docs-refresh の依頼者が消費者側も直す (翻案元と同じ)

### 申し送り (phase-9 へ、蒸留時に転記)

- README に KMP のホスト側の登録例 (iOS / Android) を載せるかは初回リリース前の docs-refresh の論点に含める。載せるなら `readme-example-lint.py` の対応表に 2 行足す
- `main` 宛て PR で macOS の job が 6 つ並ぶ (本体 3 + 消費者 3)。同時実行上限 5 の待ちを含めた PR 全体の壁時計を実測で記録し、phase-9 の release workflow の段構成 (dry-run 段の並走数) の参考にする
- 実測 (2026-09-09、draft PR #1 → 一時 `main`、コールドキャッシュ): 消費者 job の所要は ios 46 秒 / android 2 分 50 秒 / kmp 9 分 20 秒 / maui 15 分 26 秒 (timeout 30 / 30 / 30 / 40 分に対して余裕あり、詰める余地は kmp 20 分・maui 30 分程度)。PR 全体の壁時計は 19 分 46 秒で、macOS 6 job のうち本体の ios / maui が約 7 分 40 秒待ちに入った (消費者側は待ちなし)。artifact 経路 (package 段の upload → `artifact` 指定) は一時 workflow で 4 形態とも成功 (kmp 8 分 10 秒 / maui 13 分 24 秒)。証跡は change の evidence/verification/5.5・5.6
- `main` は phase-4 の決定どおり未作成のまま。5.5 の確認は 36ed37c から一時的に作った `main` で行い、確認後に削除した (add-consumer-verification の deviation)。phase-9 で `main` を作るときは branch protection と併せて必須 status check 10 件を登録する

## 実装結果 (2026-09-10 反映)

change [add-consumer-verification](../../../../changes/archive/2026-09-10-add-consumer-verification/proposal.md) (L 級) で実装完了。レビュー 3 周 (review-003 APPROVED) + 相方レビュー (second-opinion-code-001 の Major 1 / Minor 1 を採用)、verify-001 VALID (Requirement 16 / Scenario 48)。手元で 4 形態の dry-run と負ケース 9 種、CI 側は一時 `main` への draft PR #1 と一時 workflow で 10 job と artifact 経路を実測 (証跡は change の evidence/verification/5.5・5.6)。

決定事項からの乖離 (deviation.md 9 件) のうち判断に関わるもの:

| 項目 | 結果 |
|---|---|
| KMP の合成 package の再生成 (決定 1 の 3 段) | link タスクは合成 package を生成も更新もしない。再生成は `XCODEPROJ_PATH` 付きの `integrateLinkagePackage` が担い、2 段目は「再生成 → link」の 2 手。追跡している fixture は smoke 形の非解決 fixture で、消費者ビルドは作業コピーで再生成する (design Decision 4) |
| 本体側 `kmp/.swiftpm-locks/` の書き換え | kmp/ のリリース版発行が本体側の合成 Swift マニフェスト 2 本を `file://` の絶対パスへ書き換える。フィード準備は発行前に未変更を検査し、成否によらず復元する (オーナー判断 A) |
| `main` 宛て PR の実証 | リモートに `main` が無いため、実装前 commit から一時的に `main` を作って draft PR で確認し、確認後に PR をクローズして `main` を削除 (branch protection なし。オーナー判断 A)。phase-9 の計画は変えない |
| KMP ホスト側の登録コードの置き場 | ローカル package からは共有モジュールの framework を import できないため、登録と show はアプリ target に置き、`VerificationApp` は公開面と View を持つ |

蒸留では cross/ADR-0022 (lint job の 8 検査化、0021 を amends) を accepted に昇格し、cross/ADR-0018 と maui/ADR-0004 の衝突は cross/ADR-0023 (MAUI 本体の版は maui/ADR-0004 に従う、0018 を amends) で解消した。concepts に [消費者検証](../../../../concepts/cross/architecture/consumer-verification.md) を新設 (cross/architecture)、kmp/api/ios-host-integration.md に発行の副作用を追記、handbook local-development-setup.md に手元で回す手順を追加した。

### 申し送り

| 項目 | 受け皿 |
|---|---|
| README に KMP のホスト側の登録例を載せるか (載せるなら `readme-example-lint.py` の対応表に 2 行) | [phase-9 agenda](../phase-9-release-workflow/agenda.md)「phase-8 からの申し送り」 |
| 消費者 4 job の所要と PR 全体の壁時計の実測 (上の「申し送り (phase-9 へ)」の実測行) | 同上 (release の dry-run 段の並走数の参考) |
| `main` 作成時に branch protection と必須 status check 10 件を登録する (5.5 は一時 `main` で確認済み、`main` は未作成のまま) | [phase-9 agenda](../phase-9-release-workflow/agenda.md)「phase-4 からの申し送り: `main` の branch protection」 |
| release の package 段が upload する artifact の配置 (形態別のルート構造) と KMP は Android 分だけ渡す契約 | concepts [消費者検証](../../../../concepts/cross/architecture/consumer-verification.md)「フィード準備と artifact の配置」を phase-9 の実装入力にする |
| MAUI 消費者の `WarningsAsErrors` (NU1605 / NU1608 / NU1107) を故意に起こす負ケースの実行証跡が無い (verify-001 ✅※) | 見送り: 宣言は spec どおりで乖離ではない。release の smoke で警告が表面化する経路は実測済み (5.2 (b) の NU1603)。必要になれば phase-9 の change で 1 ケース足す |
| README lint の fence 判定の非対称 (review-002 Suggestion)・MAUI 消費者の `<uses-sdk>` の三重持ち (review-001 Suggestion) | 見送り: 現行 README に該当記法が無く、`<uses-sdk>` は翻案元と同形で挙動に影響しない |

## TODO

- [x] 論点の解消 (KMP 消費者の置き方・Android の参照形・timeout とトリガー・最小例の範囲、2026-09-09)
- [x] ksn-propose で変更提案を起こす (add-consumer-verification、2026-09-09 完了・2026-09-10 蒸留)
