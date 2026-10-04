# Exploration: align-toolchain-xcode27-jdk21-dotnet-10-0-401

## 課題 / 動機

オーナーの開発機が新しい Mac に替わり、開発環境が次に変わった (KsDialogs で 2026-10-04 に実測)。この環境でビルド・テストが回るようにプロジェクトを合わせる。

進め方は、sibling の KsSettingsView が同じ変更で決めた内容を踏襲する (オーナーの指示)。KsDialogs の作りが KsSettingsView と違う箇所だけを論点にする。

| 対象 | 新環境 | リポジトリが要求している版 |
|---|---|---|
| .NET SDK / workload | SDK 10.0.401 のみ、workload set 10.0.401.1 (android 36.1.69 / ios 27.0.10722 / maui 10.0.110) | `global.json` が SDK 10.0.300 / workload 10.0.300.3 (`rollForward: disable`) |
| Xcode | 27.0 (27A266a) のみ、Simulator runtime は iOS 27.0 のみ | CI が `KS_XCODE_VERSION: "26.5"` を 8 か所、`runs-on: macos-26` を 9 か所で固定 |
| JDK | Microsoft OpenJDK 21.0.12.1 のみ | CI の `setup-java` temurin 17 が 10 か所。Gradle 側にコンパイルに使う JDK の指定 (`jvmToolchain`) は無い |

出典 (KsSettingsView 側の change): `../KsSettingsView/kasane/changes/archive/2026-10-04-align-toolchain-xcode27-jdk21-dotnet-10-0-401/` (`exploration.md`・`deviation.md`・`evidence/`)。外部の事実 (workload set 10.0.401.1 は Xcode 27.0 が必須 / `macos-26` イメージに Xcode 27.0 は無く、専用イメージ `xcode-27` が持つ / `xcode-27` イメージでは Xcode 27.0 が `Xcode_27.0.0.app` の名前で選べる) は同 exploration.md の「外部の事実」と実測を正とし、ここには写さない。

### いま起きていること (2026-10-04 実測)

Gradle の実測は、`android/` `kmp/` `ios/` `core/` を `local.properties` 抜きで作業用の場所へ写し、`ANDROID_HOME` と `JAVA_HOME` (JDK 21) を渡して行った (リポジトリには何も書いていない)。

| 形態 | 結果 |
|---|---|
| MAUI | リポジトリ内で `dotnet` が起動しない (`global.json` の SDK 10.0.300 が無い) |
| iOS Native | Xcode 27.0 でビルドは通る。iOS 27.0 の Simulator (iPhone 17) で 372 件中 1 件が失敗する (論点 4) |
| Android Native | 何も変えずに JDK 21 で `./gradlew test` が通る (109 件、失敗 0)。生成 class のバイトコード版は Java 11 相当 (major version 55) のまま |
| KMP | iOS 側のビルドが Xcode 27.0 で失敗する (論点 5) |

- `local.properties` 5 枚 (`android/` `kmp/` `samples/android/` `samples/kmp/` `maui/android/native/`。いずれも git 管理外) の `sdk.dir` が、旧 Mac の実在しない場所を指している。この行がある限り、リポジトリ内の Gradle は `ANDROID_HOME` より先にこれを読んで失敗する

### 版を固定している箇所

| 箇所 | 現在の値 |
|---|---|
| `global.json` | SDK 10.0.300 / workloadVersion 10.0.300.3 / `rollForward: disable` |
| `maui/Directory.Packages.props` の `Microsoft.Maui.Controls` | 10.0.20 (workload set 10.0.300.3 の同梱版) |
| `samples/maui/KsDialogs.Sample.Maui/KsDialogs.Sample.Maui.csproj` の `MauiVersion` | 10.0.20 |
| `android/gradle/libs.versions.toml` の `kotlin` | 2.4.10 (全 Gradle ルートがこの catalog を読む。`samples/android/app/build.gradle.kts:8` だけ直書き) |
| Gradle 各モジュールの `compileOptions` / `jvmTarget` | `VERSION_11` / `JVM_11` (配布物の対象。`jvmToolchain` は無い) |

CI の固定:

| workflow | `macos-26` | Xcode "26.5" | temurin 17 |
|---|---|---|---|
| `.github/workflows/verify-ios.yml` | 1 | 1 | — |
| `.github/workflows/verify-consumer-ios.yml` | 1 | 1 | — |
| `.github/workflows/verify-kmp.yml` | 1 | 1 | 1 |
| `.github/workflows/verify-consumer-kmp.yml` | 1 | 1 | 1 |
| `.github/workflows/verify-maui.yml` | 1 | 1 | 1 |
| `.github/workflows/verify-consumer-maui.yml` | 1 | 1 | 1 |
| `.github/workflows/release.yml` | 3 (package-android / package-maui / publish) | 2 | 3 |
| `.github/workflows/verify-android.yml` / `verify-android-instrumented.yml` / `verify-consumer-android.yml` | — (ubuntu-24.04) | — | 各 1 |

- .NET はすべて `global.json` 参照 (`setup-dotnet` の `global-json-file`)。`global.json` を変えれば CI も同じ版になる
- Xcode は `KS_XCODE_VERSION` に前方一致する `/Applications/Xcode_<版>*.app` をイメージ内から選ぶ方式。Simulator は UDID で解決しており、機種名・OS 版の名指しは workflow に無い
- `release.yml` の package-android は Xcode を使わないが、publish と同じランナー OS で発行物を作り直して比べるために macOS で走る (同 workflow のコメント。cross/ADR-0024)。publish が移るなら一緒に移す
- `xcodebuild test` を回すのは `verify-ios.yml` と `verify-maui.yml` (iOS 橋渡し) の 2 か所

### KsSettingsView との違い

| 項目 | KsSettingsView | KsDialogs |
|---|---|---|
| Android の JDK 指定 | `jvmToolchain(17)` が 4 か所にあり、JDK 17 が無いとビルドできなかった | `jvmToolchain` が無く、Gradle を動かす JDK でコンパイルする。JDK 21 でそのまま通る (論点 2) |
| 配布物の対象 Java | 17 | 11 |
| MAUI 本体の下限 | 10.0.71 に固定 (workload set と切り離して決める) | workload set の同梱版に合わせる決まり (maui/ADR-0004、`kasane/handbook/cross/local-development-setup.md:81`)。workload set を上げると下限も動く (論点 3) |
| toolchain の版を決めた ADR | 無い | cross/ADR-0018 が固定境界を表で持ち、値 (`macos-26`・Xcode 26.5・Temurin 17・SDK 10.0.300 / 10.0.300.3) を本文に書いている (論点 6) |
| KMP | 無い | ある。iOS 側が Xcode を使う CI が 2 本とリリースの publish |
| iOS 27.0 で落ちるテスト | 入力面の通知を待つテスト 1 件 | 入力面の通知を使うテストは無い。別の 1 件が落ちる (論点 4) |

### 関連する既存の決定・記述

- cross/ADR-0018 — toolchain の版はリポジトリ内で固定する。Revisit When に「ランナーイメージから固定した Xcode のメジャー.マイナーが消えたとき」「.NET SDK / workload set / MAUI 本体 / Kotlin を更新するとき (更新は toolchain 更新の変更として扱う)」がある
- cross/ADR-0023 — cross/ADR-0018 の MAUI 本体の行を maui/ADR-0004 の決定で置き換えた
- maui/ADR-0004 — MAUI 本体の下限は workload set 同梱版とし、ビルド・テストする版もその版に揃える。却下案に「下限を検証済みの最新版に置く」(同じ workload set の素の利用者が NU1605 で止まる) と「下限を実測できた最低版と宣言し、ビルド版は別に置く」(下限が常時検証されない) がある
- cross/ADR-0002 — Kotlin は「2.4 系最新 (2.4.10)」

## 検討した選択肢 (却下案と理由を含む)

### 論点 1: CI の macOS job を Xcode 27 へどこまで揃えるか

KsSettingsView の採用案を踏襲する: 全 job を Xcode 27.0 / `xcode-27` イメージに揃え、job 名は変えない (必須 status check 名を保つ)。開発機には Xcode 27 しか無く、CI が別の Xcode で落ちると手元で再現できないため。KsDialogs では KMP 系の job (本体検証・消費者検証) とリリースの package-android / publish も対象に入る (KMP を Xcode 27 で通す手当ては論点 5)。

帰結も同じ: Xcode 26 でのビルドは CI で確かめなくなり、検証もリリースも public preview のランナーに依存する。却下案 (MAUI 系だけ Xcode 27 に移す / `global.json` を上げず現状維持) と理由は、KsSettingsView の exploration.md「論点 1」を正とする。

### 論点 2: Android のコンパイルに使う JDK を Gradle で指定するか

KsDialogs の Gradle には `jvmToolchain` が無く、Gradle を動かしている JDK でコンパイルする。配布物の対象は `compileOptions` と `jvmTarget` で Java 11 に明示してある。この作りは KsSettingsView が却下した「使う JDK を指定しない」形に当たる (同リポジトリでの却下理由: テストが走る JDK が環境ごとに変わる)。

- **採用 — Gradle は触らない** (オーナーが確定、2026-10-04): 何も変えずに JDK 21 で全件通り、配布物は Java 11 向けのまま (実測)。次に JDK が替わっても直す箇所が無い。テストが走る JDK は Gradle を動かす JDK に従う (開発機と CI は 21)。手元と CI を揃えるのは CI の JDK を 21 にするだけで足りる
- 却下 — KsSettingsView と同じく `jvmToolchain(21)` を足す: どの環境でもテストが JDK 21 で走るが、Gradle の 13 モジュールを直すことになり、JDK 21 が手元に必須という今は無い制約が増える。次に JDK が替わると同じ修正がまた要る

### 論点 3: workload set を上げるとき、MAUI 本体の下限をどうするか

現在の下限は 10.0.20 (workload set 10.0.300.3 の同梱版)。workload set 10.0.401.1 の同梱版は 10.0.110 (導入済み workload の定義 `Microsoft.NET.Sdk.Maui` 10.0.110 から読んだ値。`dotnet` が起動しないため `$(MauiVersion)` の評価は未実測)。maui/ADR-0004 と `kasane/handbook/cross/local-development-setup.md:81` は、workload set を上げるときに下限を同梱版へ合わせると定めている。

- **採用 — 10.0.20 のまま据え置く** (オーナーが確定、2026-10-04): 利用者要件が動かず、Xcode 26 のままの利用者がそのまま更新できる。本変更の目的は開発環境を合わせることで、利用者要件を動かすことではない。ライブラリが検証する MAUI の版は下限の 10.0.20 のままで、新しい workload set の既定値 (10.0.110) との組み合わせは、版を書かない消費者検証アプリ (`verification/maui/VerificationApp.csproj`) のビルドだけが確かめる。maui/ADR-0004 の一部改訂が要る
- 却下 — 決まりどおり 10.0.110 へ上げる: Xcode 26 のままの利用者が、次の版から `MauiVersion` を 10.0.110 以上に明記しないと復元エラー (NU1605) で止まる。MAUI 10.0.110 を Xcode 26 の環境で指定して使えるかも未確認。検証する版が新環境の既定値と一致し、ADR の改訂が要らない利点はある

TFM の platform 版は KsSettingsView と同じく明示せず、SDK の既定に任せる (KsSettingsView は SDK 10.0.401 でもライブラリの既定が iOS 26.0 / Android 36.0 のままで、nupkg の TFM group が変わらないことを pack して実測済み)。

### 論点 4: iOS 27.0 の Simulator で落ちる 1 件の扱い

- 対象: `ios/Tests/KsDialogsTests/DialogCurrentPageSwiftUITests.swift` の「SwiftUI の TabView + NavigationStack で content 枠の modifier が基準になり、タブバーとナビゲーションバーを避ける」
- 実測: ダイアログの上端が 104、テストが測ったナビゲーションバーの下端が 156 で、52 ずれる (許容は 1)。下端側 (タブバー) の検査は通る。UIKit で同じ構成を組んだテスト (`ios/Tests/KsDialogsTests/DialogCurrentPageTests.swift`) は通る
- 手がかり: テストの画面 (`ios/Tests/KsDialogsTests/Support/CurrentPageSwiftUITabHostView.swift`) はタイトルを inline 表示にしており、テストは window 内で最初に見つかった `UINavigationBar` の下端を測る。iOS 27 で、測っているバーまたはその枠が以前と違う可能性がある

テストの測り方が iOS 27 に合っていないのか、製品の表示位置が実際にバーへ重なるのかは未調査。扱いは KsSettingsView (iOS 27.0 で落ちるテストの原因調査と修正を同じ変更に含めた) を踏襲する。

- **採用 — 原因調査と修正を本変更に含める**: 完了条件「iOS Native のテストが全件通る」と CI の iOS 本体検証を満たすため。テストの測り方の問題なら本変更で直す。製品の表示位置が実際にバーへ重なる (挙動の不具合) と分かったら、止めて報告し、同梱するか別の change にするかをそのとき決める
- 却下 — 別の change に切り出す: CI の iOS 本体検証が Xcode 27 で赤のままになり、本変更を完了できない

### 論点 5: KMP の iOS 側を Xcode 27.0 でビルドできるようにする

Kotlin 2.4.10 では、KMP の iOS 側のビルドが Xcode 27.0 で失敗する。

- 失敗するタスク: `:ksdialogs-kmp:convertSyntheticImportProjectIntoDefFileIphonesimulator` と同 `Iphoneos`、`:api-surface-check` の同名タスク (Swift パッケージの取り込み)
- 症状: Kotlin の Gradle plugin がリンカを自前の記録用スクリプトに差し替えて `xcodebuild` を呼ぶが、Xcode 27.0 はこのリンクを Swift コンパイラ経由の引数 (`-emit-library` `-sdk` など) で呼ぶため、`clang: error: unknown argument` で落ちる
- 上流の修正 (Web 調査、2026-10-04): YouTrack KT-87196「Ld override falls apart in Xcode 27」が 2.4.20 で修正済みで、説明が実測の症状と一致する — https://youtrack.jetbrains.com/issue/KT-87196 。2.4.20 (2026-09-07 公開) が最新の安定版。公式の互換表は Xcode 27 をまだ載せていない — https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html

Kotlin を 2.4.20 にした写しでの実測 (2026-10-04):

| 確かめたこと | 結果 |
|---|---|
| `kmp/` の `allTests compileCommonMainKotlinMetadata compileIosMainKotlinMetadata` | 成功。`iosSimulatorArm64Test` 86 件・`testAndroidHostTest` 83 件が失敗 0 |
| `android/` の `./gradlew test --rerun-tasks` | 109 件が失敗 0 |
| klib の manifest | `abi_version=2.4.0` / `metadata_version=2.4.0`。2.4.10 で作ったものと同じで、`compiler_version` だけが変わる |

- **採用 — 本変更で Kotlin を 2.4.20 に上げる** (オーナーが確定、2026-10-04): 開発機と CI が Xcode 27 の 1 種類で回る。cross/ADR-0002 の「Kotlin は 2.4 系最新」の方向どおりで、直すのは版の一元宣言 1 行と Sample の直書き 1 か所。全 Gradle ルートが同じ catalog を読むため、Android Native・KMP・MAUI の Bridge の配布物を作るコンパイラが 2.4.20 になる
- 却下 — Kotlin は上げず、KMP 系の CI job だけ `macos-26` / Xcode 26.5 に残す: 配布物を作るコンパイラは変わらないが、開発機で KMP の iOS 側をビルドできないままになり、CI のランナーと Xcode が 2 種類に分かれる。Kotlin を上げる変更が別に要る

残る確認 (完了条件に含める):

- klib の前方互換は公式には保証されない — https://kotlinlang.org/docs/native-lib-import-stability.html 。互換を示す版が同じであることは上の実測のとおり
- MAUI の binding が参照する `Xamarin.Kotlin.StdLib` は 2.4.0.1 が最新で、2.4.10 / 2.4.20 相当の版は公開されていない (Web 調査)。Kotlin の版との差は現在 (2.4.10 対 2.4.0.1) もあり、1 段広がる。実害の有無は MAUI Android のビルド・テストで確かめる

### 論点 6: 版の値を本文に持つ ADR をどう扱うか

cross/ADR-0018 は固定境界の表に値 (`macos-26`・Xcode 26.5・Temurin 17・SDK 10.0.300 / 10.0.300.3) を、cross/ADR-0002 は Kotlin の版 (2.4.10) を本文に書いている。本変更でこれらの値は古くなるが、決定の方向 (版をリポジトリ内で固定する・固定の粒度と置き場・Kotlin は 2.4 系最新) は変わらない。

- 一部改訂の ADR は起こさない。値は決定ではなく設定に当たる (値を書き換えても ADR のタイトルの 1 文は変わらない — ksn-core `references/decisions.md`「決定の粒度」)。cross/ADR-0018 の Revisit When も、版の更新を「toolchain 更新の変更として扱う」としている。KsSettingsView も版の数値とランナーの付け替えを ADR にしていない
- 値の現在は、コード (workflow・`global.json`・catalog) と handbook が持つ。ADR には蒸留時に footer の現行照合を足す

MAUI 本体の下限だけは、値ではなく決め方が変わるため ADR で改訂する (論点 3、maui/ADR-0007)。

## 決定事項

S 級のため、この節と下の「完了条件」がそのまま実装に渡すスコープになる。

### 実装するもの

- `global.json` を SDK 10.0.401 / workloadVersion 10.0.401.1 に上げる (`rollForward: disable` は変えない)
- CI の macOS job 9 か所 (`runs-on: macos-26`) をすべて `xcode-27` イメージにし、Xcode の版指定 8 か所を 27.0 にする。job 名は変えない
- CI の JDK 導入 (`setup-java` temurin 17 の 10 か所) を 21 にする
- CI で `xcodebuild test` を回す 2 か所 (`verify-ios.yml` と、`verify-maui.yml` の iOS 橋渡し) に、失敗時の診断収集を止める指定 (`-collect-test-diagnostics never`) を足す
- Kotlin を 2.4.20 に上げる (`android/gradle/libs.versions.toml` の `kotlin` と、`samples/android/app/build.gradle.kts` の直書き)。`Xamarin.Kotlin.StdLib` は 2.4.0.1 のまま
- Gradle の JDK 指定は足さない (`jvmToolchain` を入れない)。配布物の対象 (`VERSION_11` / `JVM_11`) も変えない
- MAUI 本体の下限 (`maui/Directory.Packages.props` の `Microsoft.Maui.Controls`) と Sample の `MauiVersion` は 10.0.20 のまま据え置く。10.0.20 で新しい workload set のビルド・テストが通らないときは、版を上げる前に止めて報告する
- iOS 27.0 の Simulator で落ちるテスト 1 件の原因を調べて直す (論点 4)。製品の挙動の不具合と分かったら止めて報告する
- 版を書いているソースコメントと workflow のコメント (例: `maui/Directory.Packages.props` の「workload set 10.0.300.3 が同梱する版に合わせる」) を、変更後の内容と maui/ADR-0007 に合わせる

### 触らないもの

- TFM の platform 版は明示しない (SDK の既定に任せる)
- Xcode project の形式 (`objectVersion` / `LastUpgradeCheck`) と `ios/Package.swift` の `swift-tools-version` は触らない。Xcode 27 が形式の更新を勧めても、ビルドとテストが通る限り据え置く
- README 2 枚と `skills/` の記述は本変更では触らない。蒸留後にオーナーが docs-refresh を依頼して追従させる
- KsSettingsView からの未処理の知らせ `../KsSettingsView/kasane/outbox/KsDialogs/2026-09-30-maui-android-a11y-crash-min-version.md` は本変更では扱わない (受信台帳に記録が無く、中身は未読。題名は MAUI の最低版に関わる)

### 蒸留への申し送り

- 蒸留時に反映: `kasane/handbook/cross/local-development-setup.md` — 「workload set を上げるときは MAUI 本体と Sample の版を同梱版に合わせる」を、据え置きの決まり (maui/ADR-0007) へ改める。`global.json` の例と版 (SDK・workload set・.NET for iOS・既定の Xcode) を変更後の値へ改める
- 蒸留時に反映: `kasane/handbook/cross/local-development-setup.md` — `local.properties` の `sdk.dir` が実在しない場所を指していると、`ANDROID_HOME` より先に読まれて失敗することを足す
- 蒸留時に反映: `kasane/handbook/cross/test-execution.md` — Xcode 27.0 はテスト失敗時に診断収集で約 10 分待つため、失敗を見込む実行には `-collect-test-diagnostics never` を付けること
- 蒸留時に反映: `kasane/handbook/cross/verification-ci.md` — ランナー・Xcode・JDK の値、Xcode 26 でのビルドを CI で確かめなくなったこと、検証とリリースが public preview のランナーに依存すること
- 蒸留時に反映: `kasane/concepts/cross/architecture/distribution-artifacts.md` — 下限 10.0.20 の説明 (workload set の同梱版) を、据え置いた下限であることへ改める
- 蒸留時に反映: concepts — KMP の Swift パッケージ取り込みは、Kotlin 2.4.20 以上でないと Xcode 27.0 でビルドできない (KT-87196)。置き場は蒸留時に決める
- 蒸留時に反映: cross/ADR-0023 の footer に `関連:` 行を足す (委ね先 maui/ADR-0004 の下限版の決め方が maui/ADR-0007 で変わった)
- 蒸留時に反映: cross/ADR-0018 と cross/ADR-0002 の footer に現行照合を足す (方向は維持、値は本変更で更新)

### 完了条件 (実装とレビューが確かめること)

- `android/` の `./gradlew test` が全件実行で失敗 0 (109 件)。`kmp/` の `./gradlew allTests compileCommonMainKotlinMetadata compileIosMainKotlinMetadata` が失敗 0 (iOS 86 件・Android host 83 件)。件数の確かめ方は `kasane/handbook/cross/test-execution.md`
- `maui/android/native/` の Gradle テスト、Sample 4 形態のビルド、消費者検証 4 本 (`verification/<platform>/build-consumer.sh`) が手元で通る
- 配布物の対象が変わっていない: 生成 class のバイトコード版が Java 11 相当 (major version 55)、klib の manifest が `abi_version=2.4.0`
- iOS Native のテストが Xcode 27.0 / iOS 27.0 の Simulator で全件通る (372 件)
- MAUI の binding・facade のビルドとテスト (iOS 橋渡しの `xcodebuild test` を含む) が、MAUI 本体 10.0.20 のまま通る
- pack した nupkg の TFM group が変更前と同じで、自 assembly 用 aar の除去が効いている (maui/ADR-0004 の Revisit When「`global.json` の SDK を上げたとき」の検算)
- develop への push で、lint と 5 形態の本体検証が `xcode-27` イメージで通る
- 消費者検証とリリースの経路は、`main` 宛ての pull request とリリースの dry-run でしか走らない。本変更の完了時点では未検証として申し送る

### 手元の環境 (リポジトリの変更ではない。git 管理外)

- 対応済み (2026-10-04、オーナーの承認): `local.properties` 5 枚の `sdk.dir` 行を消した (KsSettingsView と同じ対応)。 Android SDK の場所は、オーナーのシェル設定にある `ANDROID_HOME` で解決する
- 新しい場所を `sdk.dir` に書く方法は採れない。ローカル絶対パスの書き込みを止める hook が、git 管理外の `local.properties` にも掛かるため (KsSettingsView の記録)
- エージェントのコマンド実行環境は、シェル設定の環境変数を読まない (`ANDROID_HOME` / `JAVA_HOME` が未設定に見える)。エージェントが Gradle を呼ぶときはコマンドに両方を付ける

## ADR 候補 (作成済み: maui/ADR-0007 / 未起票: なし)

- maui/ADR-0007 (proposed) — MAUI 本体の下限版は workload set を上げても据え置き、同梱版には合わせない (maui/ADR-0004 を一部改訂)
- 論点 1・2・4・5・6 は、版の数値・ランナーの付け替え・現状維持のいずれかで、後から戻せる局所的な判断のため ADR にしない

## 未決の論点

なし

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: S — オーナーが確定 (2026-10-04) (公開 API と利用者要件を変えず、挙動の仕様として書くものが無い。版の数値とランナーの指定に閉じ、後から戻せる。ただし 5 形態のビルドルートと CI・リリース経路に及び、Kotlin の版の更新と ADR の一部改訂を含むため、完了条件を上の節に明示した)
