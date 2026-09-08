# kmp-maven-distribution (delta)

## ADDED Requirements

### Requirement: version の単一ソースと注入 (kmp)

KMP ビルド (`kmp/`) の `:ksdialogs-kmp` は `group` を `jp.kamusoft`、`version` を Gradle プロパティ `version` (`-Pversion=`) の注入値があればそれ、無ければバージョンカタログの `ksdialogs` (`0.1.0-SNAPSHOT`) から導出する (SHALL)。`androidMain` が公開依存として宣言する `jp.kamusoft:ksdialogs-core` の版は同じ値である (SHALL)。注入値は `X.Y.Z` または `X.Y.Z-{alpha|beta|rc}.N` の形式でなければならず、空文字・空白を含む値・形式外の値ではビルド設定が失敗する (SHALL)。version が `-SNAPSHOT` で終わるとき、Maven Central へ向く発行タスク (名前に `MavenCentral` を含むもの。`dropMavenCentralDeployment` を除く) は、ネットワークアクセスや認証より前に SNAPSHOT を理由とする診断 (`Refusing to publish a SNAPSHOT version to Maven Central: <version>`) を出して失敗し、ローカル発行 (`publishToMavenLocal`) は妨げられない (SHALL)。

#### Scenario: 注入なしの開発既定値
- **GIVEN** `-Pversion=` を渡さない KMP ビルド
- **WHEN** `:ksdialogs-kmp` を空の一時 Maven local repository へ `publishToMavenLocal` する
- **THEN** 発行物の version は `0.1.0-SNAPSHOT` で、`ksdialogs-kmp-android` の POM が依存として記す `ksdialogs-core` の version も `0.1.0-SNAPSHOT` である

#### Scenario: リリース version の注入と android/ との一致
- **GIVEN** `-Pversion=0.1.0-beta.1` を渡した KMP ビルドと、同じ値を渡した Android ビルド
- **WHEN** 両方を同じ空の一時 Maven local repository へ `publishToMavenLocal` する
- **THEN** `ksdialogs-kmp` の全 publication の version は `0.1.0-beta.1` で、`ksdialogs-kmp-android` の POM が依存として記す `ksdialogs-core` の version は、Android ビルドが発行した `ksdialogs-core` の version と同一文字列である

#### Scenario: 不正な注入値の拒否
- **GIVEN** `-Pversion=` に空文字・空白を含む値 (`' 1.0.0 '`)・形式外の値 (`v1.0.0`、`1.0.0-pre`) のいずれかを渡した KMP ビルド
- **WHEN** 任意のタスクを実行する
- **THEN** ビルド設定が失敗し、許容する形式を示す同じ診断が出る

#### Scenario: SNAPSHOT の Central 発行拒否
- **GIVEN** `-Pversion=` を渡さない (SNAPSHOT の) KMP ビルド
- **WHEN** `--offline` で Maven Central へ向く発行タスク (`publishToMavenCentral`、および `publishAllPublicationsToMavenCentralRepository` 等の名前に `MavenCentral` を含むタスク) を実行し、続けて `publishToMavenLocal` を実行する
- **THEN** Central 向けタスクは SNAPSHOT を理由とする診断を出して失敗し (ネットワーク不通や認証不足を理由としない)、`dropMavenCentralDeployment` にはガードが掛からず、`publishToMavenLocal` は成功する

### Requirement: Swift 参照の version 導出

`:ksdialogs-kmp` の iOS 向け Swift パッケージ参照は version から導出される (SHALL)。version が `-SNAPSHOT` で終わるとき、参照は monorepo 内の `ios/` へのローカル参照 (product `KsDialogs`) である (SHALL)。それ以外のとき、参照は URL とバージョンの厳密指定 (exact = version と同じ文字列) を持つリモート参照であり、URL は Gradle プロパティ `ksdialogs.swiftPackageUrl` の値、未指定なら `https://github.com/kamusoft/KsDialogs-SPM` である (SHALL)。exact の値を変える手段は持たない (SHALL)。SNAPSHOT のときプロパティ `ksdialogs.swiftPackageUrl` は無視される (SHALL)。どちらの参照でも SwiftPM 連携メタデータの iOS deployment target は `17.0` である (SHALL)。SwiftPM 連携メタデータは root publication `jp.kamusoft:ksdialogs-kmp` の成果物 (`ksdialogs-kmp-<version>-swiftpm-metadata.json`) として発行される (SHALL)。

#### Scenario: 開発既定のローカル参照
- **GIVEN** `-Pversion=` を渡さない KMP ビルド
- **WHEN** `:ksdialogs-kmp` を空の一時 Maven local repository へ `publishToMavenLocal` する
- **THEN** root publication の SwiftPM 連携メタデータは monorepo の `ios/` を指すローカル参照で deployment target `17.0` を持ち、`samples/kmp` の `androidApp` と iOS の shared framework をビルドしても `samples/kmp/iosApp/KotlinMultiplatformLinkedPackage/` の追跡ファイルに差分が生じない

#### Scenario: リリース版の配信リポジトリ参照
- **GIVEN** `-Pversion=0.1.0-beta.1` を渡し、`ksdialogs.swiftPackageUrl` を渡さない KMP ビルド
- **WHEN** `:ksdialogs-kmp` を空の一時 Maven local repository へ `publishToMavenLocal` する
- **THEN** root publication の SwiftPM 連携メタデータは URL `https://github.com/kamusoft/KsDialogs-SPM`・exact `0.1.0-beta.1`・deployment target `17.0` のリモート参照であり、発行者環境のローカルパスを含まない

#### Scenario: URL の上書き
- **GIVEN** `-Pversion=0.1.0-beta.1` と `-Pksdialogs.swiftPackageUrl=file:///<配信リポジトリ配置のローカル clone>` を渡した KMP ビルド
- **WHEN** `:ksdialogs-kmp` を空の一時 Maven local repository へ `publishToMavenLocal` する
- **THEN** SwiftPM 連携メタデータは渡した `file://` URL・exact `0.1.0-beta.1`・deployment target `17.0` のリモート参照であり、ビルドは URL のスキームを理由に失敗しない

### Requirement: KMP 発行物の座標と内容

`:ksdialogs-kmp` は Maven Central へ発行できる設定を持ち、root `jp.kamusoft:ksdialogs-kmp` と各ターゲットの publication (`ksdialogs-kmp-android`・`ksdialogs-kmp-iosarm64`・`ksdialogs-kmp-iossimulatorarm64`・`ksdialogs-kmp-iosx64`) を同一 version で発行する (SHALL)。発行物の配置は次のとおりである (SHALL): root は commonMain の metadata jar・POM・`.module`・sources jar・空の javadoc jar・SwiftPM 連携メタデータ (`*-swiftpm-metadata.json`)、Android は release variant の aar・POM・`.module`・sources jar・空の javadoc jar、iOS 各ターゲットは本体 klib・SwiftPM 連携用の cinterop klib・POM・`.module`・sources jar・空の javadoc jar。5 publication すべての POM は name / description / url / MIT license / developers / scm / inceptionYear を含み、url / license / developers / scm / inceptionYear は Android ビルドの `ksdialogs-core` の POM と同一である (SHALL)。各 sources jar は対応する source set の Kotlin ソースを含む (SHALL)。

Gradle Module Metadata (`.module`) は次を満たす (SHALL): root はターゲット publication ごとの variant (各ターゲット publication の座標へ委譲するもの) と、SwiftPM 連携メタデータを artifact に持つ専用 variant を含む / Android の API variant は `jp.kamusoft:ksdialogs-core` (同版) を依存に持つ / iOS 各ターゲットの variant は本体 klib と cinterop klib を artifact として参照し、`org.jetbrains.kotlinx:kotlinx-coroutines-core` を依存に持つ / いずれの publication の POM と `.module` にもテスト専用ライブラリ (`kotlin-test` / `kotlinx-coroutines-test`) は含まれない。`:api-surface-check` は公開対象に含まれない (SHALL)。

#### Scenario: ローカル発行での発行物検証
- **GIVEN** 発行設定を導入した `kmp/`
- **WHEN** `-Pversion=0.1.0-beta.1` で空の一時 Maven local repository へ `publishToMavenLocal` を実行する
- **THEN** `jp/kamusoft/ksdialogs-kmp/0.1.0-beta.1/` と 4 つのターゲット publication のディレクトリに上記の配置どおりの成果物が存在し、root に `ksdialogs-kmp-0.1.0-beta.1-swiftpm-metadata.json` があり、javadoc jar は空で、各 sources jar に対応 source set の代表ファイル (root: `KsDialog.kt`、Android: `androidMain` の actual 宣言、iOS: `iosMain` の actual 宣言) が含まれ、5 つの POM すべてに MIT license・scm・developers・inceptionYear・url と name / description が記載されている

#### Scenario: Gradle Module Metadata の内容
- **GIVEN** 上記の発行物
- **WHEN** root・Android・iOS 各ターゲットの `.module` を読む
- **THEN** root にターゲットごとの委譲 variant と SwiftPM 連携メタデータの variant があり、Android の API variant が `jp.kamusoft:ksdialogs-core:0.1.0-beta.1` を持ち、iOS 各 variant が本体 klib と cinterop klib を参照して `kotlinx-coroutines-core` を持ち、テスト専用ライブラリはどこにも現れない

#### Scenario: api-surface-check の非公開
- **GIVEN** 発行設定を導入した `kmp/`
- **WHEN** `:api-surface-check` のタスク一覧を確認する
- **THEN** 発行タスク (`publish*`) が存在しない

### Requirement: KMP 発行物の署名

署名鍵 (`signingInMemoryKey` 系プロパティ) が与えられたとき、`:ksdialogs-kmp` の全 publication の全成果物 (POM・`.module`・jar・aar・klib・SwiftPM 連携メタデータ) に署名 (`.asc`) が付く (SHALL)。鍵が無いときローカル発行は署名なしで成功する (SHALL)。

#### Scenario: 鍵なしのローカル発行
- **GIVEN** 署名鍵のプロパティを渡さない KMP ビルド
- **WHEN** 空の一時 Maven local repository へ `publishToMavenLocal` を実行する
- **THEN** 成功し、`.asc` は 1 つも生成されない

#### Scenario: 鍵ありのローカル発行
- **GIVEN** 検証用に一時生成した鍵を `signingInMemoryKey` 系プロパティで渡した KMP ビルド
- **WHEN** 別の空の一時 Maven local repository へ `publishToMavenLocal` を実行する
- **THEN** root と 4 つのターゲット publication の全成果物に `.asc` が生成され、`ksdialogs-kmp-<version>-swiftpm-metadata.json.asc` も含まれる

### Requirement: Sample の依存版

`samples/kmp/shared` が宣言する `jp.kamusoft:ksdialogs-kmp` の版はバージョンカタログの `ksdialogs` から取る (SHALL)。Sample はソース参照 (composite build の dependencySubstitution) を維持し、`kmp/` の共有コードの変更が Sample のビルド結果を左右する (SHALL)。`samples/kmp/settings.gradle.kts` の置換の説明は、KMP facade が発行設定を持つ状態でも成立する内容 (公開版への無音フォールバックを避けるため常にローカルソースへ置換する) である (SHALL)。

#### Scenario: Sample のビルドとソース参照
- **GIVEN** 依存版をカタログ値にした `samples/kmp`
- **WHEN** `androidApp` を `assembleDebug` し、続けて `kmp/ksdialogs-kmp/src/commonMain` に意図的なコンパイルエラーを一時注入して再度 `assembleDebug` する (確認後に復元する)
- **THEN** 1 回目は成功し、2 回目は注入したエラーで失敗し (置換が効かなければ公開版やキャッシュで成功してしまう)、復元後の `git diff` は空である
