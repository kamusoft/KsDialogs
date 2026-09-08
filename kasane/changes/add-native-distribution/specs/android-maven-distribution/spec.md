# android-maven-distribution (delta)

## ADDED Requirements

### Requirement: Android module の座標と構成

Android ビルド (`android/`) は、View 系本体を Gradle module `:ksdialogs-core` (ディレクトリ `android/ksdialogs-core`、Maven `jp.kamusoft:ksdialogs-core`)、Compose 系を `:ksdialogs` (ディレクトリ `android/ksdialogs`、Maven `jp.kamusoft:ksdialogs`、本体に `api` で依存) として構成する (SHALL)。Kotlin パッケージ (本体 `jp.kamusoft.ksdialogs`、Compose 系 `jp.kamusoft.ksdialogs.compose`) と AGP namespace は変更しない (SHALL)。本体の Compose 非依存を検査するタスクは `:ksdialogs-core` で引き続き `test` / `check` に結線される (SHALL)。`:api-surface-check` は公開対象に含まれない (SHALL)。

#### Scenario: 改名後の aar 生成
- **GIVEN** 改名後の `android/`
- **WHEN** `./gradlew :ksdialogs-core:assembleRelease :ksdialogs:assembleRelease` を実行する
- **THEN** `ksdialogs-core-release.aar` と `ksdialogs-release.aar` が生成され、それぞれ `jp.kamusoft.ksdialogs` / `jp.kamusoft.ksdialogs.compose` パッケージの公開クラスを改名前と同じ構成で含む

#### Scenario: ユニットテストと API 形状検査の無改変実行
- **GIVEN** 改名後の `android/`
- **WHEN** `./gradlew test --rerun-tasks` を実行し、`:api-surface-check` を肯定ケース (プロパティなし) と否定ケース (各 `ksdialogs.negativeCheck.*` プロパティを 1 つずつ付けた個別ビルド) で実行する
- **THEN** module ごとのユニットテスト実行件数は改名前と一致してすべて成功し、本体の Compose 非依存検査が実行され、肯定ケースはビルド成功、否定ケースはそれぞれ改名前と同じ診断で失敗する

#### Scenario: instrumented test の無改変実行
- **GIVEN** 改名後の `android/`
- **WHEN** API レベル 36 のエミュレータ (検証 CI の instrumented job と同じ条件) で `./gradlew connectedDebugAndroidTest` を実行する
- **THEN** `:ksdialogs-core` / `:ksdialogs` それぞれの実行件数が改名前 (294 / 39) と一致し、すべて成功する

### Requirement: version の単一ソースと注入

Android ビルドの全 module の `group` は `jp.kamusoft`、`version` は Gradle プロパティ `version` (`-Pversion=`) の注入値があればそれ、無ければバージョンカタログの `ksdialogs` (`0.1.0-SNAPSHOT`) から導出される (SHALL)。注入値は `X.Y.Z` または `X.Y.Z-{alpha|beta|rc}.N` の形式でなければならず、空文字・空白を含む値・形式外の値ではビルド設定が失敗する (SHALL)。version が `-SNAPSHOT` で終わるとき、Maven Central へ向く発行タスクは失敗し、ローカル発行 (`publishToMavenLocal`) は妨げられない (SHALL)。

#### Scenario: 注入なしの開発既定値
- **GIVEN** `-Pversion=` を渡さない Android ビルド
- **WHEN** `:ksdialogs-core` / `:ksdialogs` を `publishToMavenLocal` する
- **THEN** 両 artifact の version は `0.1.0-SNAPSHOT` である

#### Scenario: リリース version の注入
- **GIVEN** `-Pversion=0.1.0-beta.1` を渡した Android ビルド
- **WHEN** `:ksdialogs-core` / `:ksdialogs` を `publishToMavenLocal` する
- **THEN** 両 artifact の version は `0.1.0-beta.1` で、`ksdialogs` の POM が依存として記す `ksdialogs-core` の version も `0.1.0-beta.1` である

#### Scenario: 不正な注入値の拒否
- **GIVEN** `-Pversion=` に空文字・空白を含む値・形式外の値 (`v1.0.0`、`1.0.0-pre`) のいずれかを渡した Android ビルド
- **WHEN** 任意のタスクを実行する
- **THEN** ビルド設定が失敗し、許容する形式を示すメッセージが出る

#### Scenario: SNAPSHOT の Central 発行拒否
- **GIVEN** `-Pversion=` を渡さない (SNAPSHOT の) Android ビルド
- **WHEN** Maven Central へ向く発行タスクを実行する
- **THEN** タスクは失敗し、`publishToMavenLocal` は成功する

### Requirement: Maven 発行物の座標と内容

`:ksdialogs-core` と `:ksdialogs` は Maven Central へ発行できる設定を持ち、発行物は release variant の aar・sources jar・空の javadoc jar・POM・Gradle Module Metadata (`.module`) で構成される (SHALL)。POM は name / description / url / MIT license / developers / scm / inceptionYear を含み、name と description は module ごとに異なり、その他は両 artifact で同一である (SHALL)。sources jar には各 module の公開パッケージの Kotlin ソースが含まれる (SHALL)。

発行メタデータの依存は次のとおりである (SHALL)。compile スコープ (`.module` の API variant) は公開シグネチャに現れる外部型の提供元だけ — `ksdialogs-core` は `androidx.annotation:annotation`、`ksdialogs` は `jp.kamusoft:ksdialogs-core` (同版) と `androidx.compose.runtime:runtime` — とし、ビルドツールが自動で加える Kotlin 標準ライブラリはこの制約の対象外とする。runtime スコープ (`.module` の runtime variant) は上記に加えて実装が要する依存 — `ksdialogs-core` は `kotlinx-coroutines-android`、`ksdialogs` は `androidx.compose.ui:ui` / `androidx.lifecycle:lifecycle-runtime` / `androidx.savedstate:savedstate` — を含む。テスト専用ライブラリ (JUnit / AndroidX Test / kotlinx-coroutines-test) はいずれのスコープにも含まれない。

#### Scenario: ローカル発行での発行物検証
- **GIVEN** 発行設定を導入した `android/`
- **WHEN** `./gradlew publishToMavenLocal` を実行する
- **THEN** ローカル Maven リポジトリの `jp/kamusoft/ksdialogs-core/<version>/` と `jp/kamusoft/ksdialogs/<version>/` に aar・sources jar・javadoc jar・POM・`.module` が配置され、javadoc jar は空、sources jar は `jp/kamusoft/ksdialogs/` (core) / `jp/kamusoft/ksdialogs/compose/` (Compose 系) 配下の `.kt` を含み、POM には MIT license・scm・developers・inceptionYear と module ごとの name / description が記載されている

#### Scenario: 発行メタデータの依存スコープ
- **GIVEN** `publishToMavenLocal` の発行物
- **WHEN** POM と `.module` の依存をスコープ (variant) ごとに検査する
- **THEN** compile スコープは Requirement の列挙 (Kotlin 標準ライブラリを除く) と一致し、runtime スコープは Requirement の列挙をすべて含み、テスト専用ライブラリはいずれのスコープにも存在しない

#### Scenario: release aar の公開シグネチャ検算
- **GIVEN** `publishToMavenLocal` の release aar
- **WHEN** aar 内 classes の公開 (public) シグネチャを機械走査し、現れる外部の非プラットフォーム型を列挙する
- **THEN** 列挙された型の提供 artifact はすべて発行メタデータの compile スコープに存在する

#### Scenario: api-surface-check の非公開
- **GIVEN** 発行設定を導入した `android/`
- **WHEN** `./gradlew :api-surface-check:tasks --group publishing` を実行する
- **THEN** Maven Central へ向く発行タスクが存在しない

### Requirement: 発行物の署名

発行物の署名は署名鍵 (`signingInMemoryKey` 系のプロパティ) が渡されたときにだけ必須であり、鍵が無いローカル発行は未署名のまま成功する (SHALL)。鍵が渡されたときは両 module の全 publication に署名成果物 (`.asc`) が生成される (SHALL)。

#### Scenario: 鍵なしのローカル発行
- **GIVEN** 署名鍵のプロパティを渡さない Android ビルド
- **WHEN** `publishToMavenLocal` を実行する
- **THEN** 成功し、署名成果物は要求されず生成されない

#### Scenario: 鍵ありのローカル発行
- **GIVEN** 検証用に一時生成した鍵を `signingInMemoryKey` 系のプロパティで渡した Android ビルド
- **WHEN** `publishToMavenLocal` を実行する (ネットワーク送信なし)
- **THEN** 両 module の aar・sources jar・javadoc jar・POM・`.module` それぞれに対応する `.asc` が生成される

### Requirement: monorepo 内消費者の座標追随

Android Sample (`samples/android`) と KMP Sample の Android アプリ (`samples/kmp/androidApp`) は、Compose 系の配布物 `jp.kamusoft:ksdialogs` への依存 1 行だけを直接宣言し、本体 `ksdialogs-core` は推移的依存で解決される (SHALL)。両 Sample は composite build の明示 dependencySubstitution (`jp.kamusoft:ksdialogs-core` → `:ksdialogs-core`、`jp.kamusoft:ksdialogs` → `:ksdialogs`) で本体 project へ置換される (SHALL)。`kmp/ksdialogs-kmp` と `maui/android/native/ksdialogs-maui-bridge` は本体を `jp.kamusoft:ksdialogs-core` で参照する (SHALL)。MAUI binding (`maui/android/KsDialogs.Binding.Android`) は改名後の aar (`ksdialogs-core-release.aar` を同梱、bridge の aar を束縛) からビルドされる (SHALL)。

#### Scenario: Sample のビルドとソース参照
- **GIVEN** 追随後の `samples/android` と `samples/kmp`
- **WHEN** 各 Sample の Android アプリをビルドする
- **THEN** アプリの直接依存は `jp.kamusoft:ksdialogs` 1 行で、`ksdialogs-core` は推移的依存として本体 project に置換され、ビルドが成功し、本体ソースの変更が Sample の再ビルドに反映される

#### Scenario: KMP と MAUI bridge のビルド
- **GIVEN** 本体依存の座標を追随した `kmp/` と `maui/android/native`
- **WHEN** `kmp/` のテストと bridge の `assembleRelease` を実行する
- **THEN** いずれも成功し、本体は `ksdialogs-core` として解決される

#### Scenario: MAUI binding のビルド
- **GIVEN** aar パスと gradlew task 名を追随した binding csproj
- **WHEN** binding プロジェクトをビルドする
- **THEN** gradlew 経由で改名後の aar が生成・取り込まれ、ビルドが成功する
