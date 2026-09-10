---
type: concept
title: 配布物の構成 (4 形態)
description: 4 形態 (SwiftPM 配信リポジトリ / Android Maven 2 座標 / MAUI NuGet 3 パッケージ / KMP Maven 5 publication) の配布物が何を含み、version がどこで決まって注入され、開発版が公開レジストリへ流れないためのガードがどこにあるか
tags: [cross, distribution, swiftpm, maven, nuget, kmp, version]
timestamp: 2026-09-10
---

# 配布物の構成 (4 形態)

この文書を読むと、利用者が公開レジストリから受け取る配布物が形態ごとに何でできていて、その version がリポジトリ内のどの値から決まり、開発版 (SNAPSHOT / `0.0.0-dev`) が公開レジストリへ流れないためのガードがどこにあるかが分かる。配布物を公開レジストリへ出す手順 (release workflow の段構成・publish の順序・再実行) は [release workflow](release-workflow.md) が、公開前後に配布物を利用者と同じ経路で解決して検証する仕組みは [消費者検証](consumer-verification.md) が持つ。配布チャネルを選んだ理由は [cross/ADR-0008](../../../decisions/cross/0008-distribution-model-standard-channels.md)、全形態を同じ version で一斉に出す理由は [cross/ADR-0009](../../../decisions/cross/0009-lockstep-single-version.md) にある。

本文で使う語のうち、monorepo (4 形態のソースを持つ本リポジトリ)・配信リポジトリ・facade / binding・publication・deployment・注入は末尾の用語表で定義する。

## 全体像

4 形態はすべて同じ version 文字列で一斉に公開され、利用者が手で入れる依存は形態ごとに 1 点 (KMP の iOS ホストだけ 2 点) である。「自動で入るもの」は、利用者が書いた依存の推移的依存としてパッケージマネージャが自動で解決するものを指す。

| 形態 | チャネル | 利用者が入れるもの | 自動で入るもの (推移的依存) |
|---|---|---|---|
| Native iOS | SwiftPM (配信リポジトリ `KsDialogs-SPM`) | product `KsDialogs` を https + `exact` で 1 点 | なし |
| Native Android | Maven Central | View 系だけなら `jp.kamusoft:ksdialogs-core`、Compose で中身を書くなら `jp.kamusoft:ksdialogs` の 1 点 | Compose 系を入れると本体 `ksdialogs-core` が同版で自動 |
| .NET MAUI | nuget.org | facade `KsDialogs.Maui` 1 点 | binding 2 件 (`KsDialogs.Binding.iOS` / `KsDialogs.Binding.Android`) が platform TFM でだけ同版で自動 |
| KMP | Maven Central + SwiftPM | 共有コードに `jp.kamusoft:ksdialogs-kmp` 1 点、iOS アプリ側に SwiftPM の `KsDialogs` 1 点 | Android ホストに `ksdialogs-core` が同版で自動 (Compose 系は自動では届かない) |

利用者向けの導入手順 (どの依存をどこに書くか) は README 2 枚と `skills/` が持ち、この文書は配布物の側から「何が届くか」を記述する。

## version の表現と注入

version の値は 1 回のリリースにつき 1 つで、release workflow の起動入力がそのまま tag と各レジストリの version になる。リポジトリ内のファイルは開発用の既定値のままで、リリースのたびに bump する commit は積まない。

| 項目 | 内容 |
|---|---|
| 受け付ける形 | `X.Y.Z` または `X.Y.Z-{alpha\|beta\|rc}.N` (先頭ゼロなし)。`-pre` / `-preview` は Maven の版比較で正式版より新しいと判定されるため使わない |
| tag | 接頭辞なしの version そのもの。monorepo と配信リポジトリに同じ値の tag が打たれる |
| Gradle (android/ と kmp/) | 開発既定値はバージョンカタログ `android/gradle/libs.versions.toml` の `ksdialogs` (`X.Y.Z-SNAPSHOT`)。kmp/ の `settings.gradle.kts` はこの同じファイルをカタログとして読む。リリース時は `-Pversion=` で注入する |
| MAUI (maui/) | 開発既定値は `maui/Directory.Build.props` の `0.0.0-dev`。リリース時は `-p:Version=` で注入する |
| SwiftPM | version はファイルに無い。配信リポジトリの tag が version を表す |

Gradle 側の version は次の 1 つの導出式で決まり、android/ と kmp/ のルートビルドファイルが同じ式を持つ (別の build root なので意図的に 2 度書く。[cross/ADR-0004](../../../decisions/cross/0004-monorepo-four-build-roots.md) はルートに共通ビルドファイルを置かない)。

```
version = -Pversion= の注入値 (形式検査に通ったもの)、無ければカタログの ksdialogs
```

KMP はこの 1 つの値から自分の version・本体 `ksdialogs-core` への依存版・Swift 参照の `exact` を出すため、形態をまたいだ版の突き合わせを手で揃える箇所が無い。注入値は形式検査に通らなければ設定段階で失敗する (空文字が「注入あり」と解釈されて次節のガードを迂回することを防ぐ)。

### 開発版を公開レジストリへ流さないガード

Gradle は version が `-SNAPSHOT` のあいだ、名前に `MavenCentral` を含む発行タスク (`dropMavenCentralDeployment` を除く) を 2 段で止める。設定段階で実行要求されたタスク名を照合し、task graph の確定時に間接的に含まれた対象も捕まえる。`publishToMavenLocal` は止めないので、開発版の発行物は手元と消費者検証で作れる。

MAUI は `dotnet nuget push` が MSBuild の外にあるため同じ場所にガードを置けず、release workflow が pack 直後と publish job の artifact download 直後の 2 回、3 つの nupkg が `<Package ID>.<入力 version>.nupkg` の名前で存在することを検査する。`0.0.0-dev` のまま pack した nupkg は名前の時点で弾かれる。

署名は Gradle の signing プラグインが、署名鍵のプロパティ (`signingInMemoryKey`) があるときだけ必須にする。鍵の無い手元・消費者検証の発行は未署名のまま通り、公開時の発行物には release workflow が upload 前に `.asc` の対が揃っていることを検査する。

## Native iOS (SwiftPM)

配布物は monorepo の `ios/` のスナップショットで、SwiftPM 専用の配信リポジトリ `KsDialogs-SPM` のルートに置かれる。monorepo のルートに `Package.swift` は無い。

| 項目 | 内容 |
|---|---|
| スナップショットの中身 | `ios/Package.swift` → `Package.swift`、`ios/Sources/`、`ios/Tests/`、`LICENSE`、`scripts/spm-snapshot/README.template.md` → `README.md` の 5 点。それ以外は同期のたびに除去される。`Tests/` を含めるのは `Package.swift` が testTarget を宣言しており、無いとマニフェストが解決できないため |
| 同期 | `scripts/spm-snapshot/sync-snapshot.sh` が作業コピーへ配置する (冪等・git 操作なし)。commit / tag / push は release workflow が行い、人は配信リポジトリへ commit しない |
| identity と product | 利用者の `Package.swift` に書く identity は `KsDialogs-SPM`、product は `KsDialogs`。最低対象 OS は iOS 17、Swift 6 言語モード |
| 参照の形 | https の Package URL + `exact(<version>)`。配信リポジトリの tag が version と同名で実在することが解決の前提 |

配信リポジトリの Issues / PR は無効で、README が monorepo へ誘導する。

## Native Android (Maven Central)

android/ の 2 モジュールがそれぞれ 1 座標として発行される。groupId は `jp.kamusoft`。View 系は Android の従来の `View` を中身にする API、Compose 系は Jetpack Compose の composable を中身にする API (`registerCompose` / `showCompose`) を指す ([Android の Dialog 公開面](../../android/api/dialog-surface.md))。

| 座標 | 中身 | 依存 |
|---|---|---|
| `jp.kamusoft:ksdialogs-core` | View 系本体 (Compose 非依存) | Kotlin 標準ライブラリ・coroutines |
| `jp.kamusoft:ksdialogs` | Compose 系 | 本体 `ksdialogs-core` に `api` で同版依存し、推移で届く |

発行するのは release variant だけで、sources jar を同梱し、javadoc jar は Maven Central の必須要件を満たすための空 jar である (IDE の KDoc 表示は sources jar が担う)。POM の共通項 (license / developer / scm) は android/ のルートビルドファイルが 1 か所で持つ。最低対象 OS は Android 7.0 (API 24)。

Maven Central への upload は Central Portal の deployment として、自動公開せず手動で release を要求するモード (USER_MANAGED) で保留され、公開の可否は release workflow が決める (`publishToMavenCentral(automaticRelease = false)`)。

## .NET MAUI (nuget.org)

facade 1 件と binding 2 件の 3 パッケージで、利用者が参照するのは facade だけである。binding は iOS / Android の Native ライブラリの成果物 (xcframework / aar) を .NET から呼べるように包む中間パッケージで、公開 API を持たず、facade が推移的に参照する。TFM (target framework moniker) は `net10.0-ios` のような対象フレームワーク名で、platform TFM は iOS / Android の実体を持つ 2 つを指す。

| Package ID | 中身 | 利用者から見た位置づけ |
|---|---|---|
| `KsDialogs.Maui` | 公開面 (namespace `KsDialogs`)。TFM は `net10.0` / `net10.0-ios` / `net10.0-android` の 3 つで、platform TFM の依存に binding を同版で持つ | 参照する 1 点 |
| `KsDialogs.Binding.iOS` | iOS Native ライブラリの ObjC 互換面を包む binding。device / simulator 両スライスの xcframework を binding resource package として同梱 | 推移で届く。直接参照しない (Description に明記) |
| `KsDialogs.Binding.Android` | Android Native ライブラリを包む binding。Gradle 由来の aar 2 本 (互換面 + Android 本体) と Kotlin 系の実行時依存 (`Xamarin.Kotlin.StdLib` 等) | 同上 |

### facade に同梱されるもの

| 同梱物 | 内容 |
|---|---|
| README | monorepo ルートの `README.md` (nuget.org のパッケージページに表示される)。release workflow が pack の前にインストール例の version を入力値へ置き換えるため、同梱される README は公開する version を指す |
| XML ドキュメント | `KsDialogs.Maui.xml` を 3 TFM すべての `lib/<TFM>/` に同梱する (日本語の doc コメント)。binding 2 件は生成しない指定だが、Android binding には .NET Android SDK の binding 用ビルド定義が Resource designer の説明だけの小さな `.xml` を残す (csproj からは止められず、利用者に害は無い) |
| `buildTransitive/` | 最低 OS 版の定数を持つ props と、利用者ビルドで検査する targets。TFM ごとに走る内側のビルド (inner build) のうち platform TFM で、`SupportedOSPlatformVersion` が Android API 24 / iOS 17.0 未満ならエラー `KSDLG0001` で止める。素の `net10.0` と、複数 TFM を束ねる外側のビルド (outer build) では何もしない |
| snupkg / SourceLink | 3 パッケージとも symbol package を対で発行し、SourceLink で public リポジトリのソースへ辿れる |

### MAUI 本体の版と TFM

facade は `Microsoft.Maui.Controls` に下限 10.0.20 で依存する。この値は repo 直下の `global.json` が固定する workload set が同梱する版そのものなので、同じ SDK の利用者はプロジェクトに MAUI の版を書かずに導入できる。それより古い版を明示すると NuGet がダウングレードを拒む (NU1605)。ライブラリの CI はこの下限で常にビルド・テストする ([maui/ADR-0004](../../../decisions/maui/0004-nuget-three-package-structure.md))。

nupkg 内の platform TFM は SDK 既定の API 版付き (`net10.0-android36.0` / `net10.0-ios26.0`) をそのまま受け入れている。この名前は SDK 更新で変わるため、利用者向け文書には書かない。利用者側の TFM の API 版がこれより低いと、警告なく platform 中立のアセットにフォールバックして native の binding が入らない。消費者検証がこの取り違えを検査する ([消費者検証](consumer-verification.md))。

### 自 assembly 用 aar の除去 (現状は発生していない)

KsDialogs の pack では発生していないが、SDK の挙動が変わったときに備えた後処理が `maui/Directory.Build.targets` にある。.NET Android SDK は class library の自 assembly 用 aar を nupkg の `lib/` へ入れることがあり、入ると利用者の Android Release ビルドで native ライブラリの重複 (`XA4301`) になる。後処理は aar が存在するときだけ動き、pack の同梱物から除く。

## KMP (Maven Central + SwiftPM)

配布物は `jp.kamusoft:ksdialogs-kmp` の 5 publication で、1 回の Gradle ビルドから 1 つの deployment として upload される。この節の「ホスト」は共有モジュールを取り込む各プラットフォームのアプリ (Android アプリ / iOS アプリ) を指す。

| artifactId | 内容 |
|---|---|
| `ksdialogs-kmp` | root publication (Gradle module metadata で target 別 publication へ振り分ける) |
| `ksdialogs-kmp-android` | Android ターゲット。`androidMain` は Android Native 本体 `jp.kamusoft:ksdialogs-core` に `api` で同版依存する |
| `ksdialogs-kmp-iosarm64` / `ksdialogs-kmp-iossimulatorarm64` / `ksdialogs-kmp-iosx64` | iOS 3 ターゲットの klib (cinterop klib を含む) と SwiftPM 連携メタデータ |

### 利用者側に届くもの

Android ホストには `ksdialogs-core` が推移で自動的に届く。共有コードの ViewModel 契約が Native ライブラリの型そのものであるため、依存は `implementation` ではなく `api` で公開されている。Compose 系 (`jp.kamusoft:ksdialogs`) は自動では届かず、Android ホストで Compose の中身を書く利用者が別途足す。

iOS ホストには Swift 実体が届かない。KMP の framework (`KsDialogsKmp`) は static で Swift 実体を同梱しないため、iOS アプリは SwiftPM の `KsDialogs` を自分でリンクする。発行 metadata (`swiftpm-metadata.json`) には Swift 参照の URL・`exact(<version>)`・deployment target `17.0` が焼き込まれ、Kotlin Gradle Plugin の SwiftPM 連携がこの参照を iOS アプリの合成 package に展開する (経路は [KMP 利用者の iOS ホスト統合](../../kmp/api/ios-host-integration.md))。

### 発行の制約

Swift 参照は version から導出される。`-SNAPSHOT` なら monorepo 内 `ios/` へのローカル参照、リリース版なら配信リポジトリの https URL + `exact` で、切替スイッチは無い。リリース版の iOS publication (cinterop klib 付き) を発行するには、Kotlin Gradle Plugin が発行時に SwiftPM パッケージを解決するため、配信リポジトリに同版の tag が実在し、発行環境に Xcode が要る。この制約が release workflow の publish の順序 (配信リポジトリの tag を KMP の発行より前に置く) を決めている ([cross/ADR-0024](../../../decisions/cross/0024-release-dispatch-serial-publish-spm-tag-before-kmp.md))。

### 利用者側の Kotlin の版

klib の互換の都合で、利用者側の Kotlin Gradle Plugin は本ライブラリと同じ minor である必要がある (異なる minor はサポートしない)。実際に検証した版はバージョンカタログ `android/gradle/libs.versions.toml` の `kotlin` が固定する値である。Kotlin 側の SwiftPM 連携 (発行 metadata から iOS アプリの合成 package を作る機構) は Kotlin Gradle Plugin の Alpha 機能で、消費側の最低版と metadata 形式の互換に公式の記述が無い ([KMP 利用者の iOS ホスト統合](../../kmp/api/ios-host-integration.md)、[cross/ADR-0008](../../../decisions/cross/0008-distribution-model-standard-channels.md) の Revisit When)。

## 保証すること

- 4 形態の配布物は同じ version 文字列で公開され、KMP → Android Native は `api` の同版依存、KMP → Swift package は `exact` で、版のずれはビルド時に検出される。
- 開発版 (`-SNAPSHOT` / `0.0.0-dev`) は公開レジストリへ届かない。Gradle は発行タスクで、MAUI は nupkg 名の検査で止まる。
- 利用者が手で入れる依存は形態ごとに 1 点 (KMP の iOS ホストは 2 点) で、binding・本体は推移で同版が届く。
- MAUI 利用者の最低 OS 版の不足はビルド時にエラー `KSDLG0001` で止まり、実行時クラッシュとして出荷されない。

## してはいけないこと

- リリースのために version をファイルへ書き込む commit を積まない: version の正は release workflow の起動入力で、ファイルは開発既定値のまま保つ。
- 配信リポジトリへ手で commit しない: 配信リポジトリの内容は monorepo の `ios/` のスナップショットで、release workflow だけが書く。
- 利用者向け文書に SDK 更新で変わる値を手で直書きしない: API 版付き TFM 名は書かず、toolchain の版と MAUI 本体の下限は docs-refresh がコードを正として転記する (次の段落)。
- `-Pversion=` の値を形式検査なしに受け入れない: 空文字が「注入あり」と解釈されると SNAPSHOT ガードを迂回する。

docs-refresh (利用者向け文書を concepts とコードへ追従させる更新手順) が転記する値の取得元は、Kotlin / AGP がバージョンカタログ `android/gradle/libs.versions.toml`、Gradle が 2 つの build root の wrapper、Swift tools が `ios/Package.swift`、`Microsoft.Maui.Controls` の下限が `maui/Directory.Packages.props` である。README の対応プラットフォーム表と各 Skill の導入節に載る版はこの転記の結果であり、手で直さない。

## 用語

| 用語 | 意味 |
|---|---|
| monorepo | 4 形態のソース (`ios/` `android/` `kmp/` `maui/`) を持つ本リポジトリ `KsDialogs` |
| 配信リポジトリ | SwiftPM 専用の公開リポジトリ `KsDialogs-SPM`。`ios/` のスナップショットと version と同名の tag だけを持つ |
| スナップショット | `ios/` の 5 点を配信リポジトリのルートへ写した内容。同期スクリプトの出力 |
| facade / binding | MAUI の公開面パッケージ `KsDialogs.Maui` と、Native ライブラリの成果物を .NET から呼べるように包む中間パッケージ 2 件 (`KsDialogs.Binding.iOS` / `KsDialogs.Binding.Android`)。binding は公開 API を持たず facade が推移的に参照する |
| TFM / platform TFM | target framework moniker (`net10.0-ios` のような対象フレームワーク名) と、そのうち iOS / Android の実体を持つ 2 つ |
| ホスト | KMP の共有モジュールを取り込む各プラットフォームのアプリ |
| publication | Gradle の発行単位。KMP は root + Android + iOS 3 ターゲットの 5 つ、Android Native はモジュールごとに 1 つ |
| deployment | Central Portal 上の upload 1 件。1 回の Gradle ビルドの全 publication がまとまる。USER_MANAGED は自動公開せず release 要求を待つモード |
| 注入 | release workflow が `-Pversion=` / `-p:Version=` で version をビルドに渡すこと |
| docs-refresh | 利用者向け文書 (`skills/` と README 2 枚) を concepts とコードへ追従させる更新手順 |

## 関連

- [release workflow](release-workflow.md) — この配布物を公開レジストリへ出す段構成・publish の順序・再実行
- [消費者検証](consumer-verification.md) — 配布物を利用者と同じ経路で解決する dry-run / smoke
- [KMP 利用者の iOS ホスト統合](../../kmp/api/ios-host-integration.md) — 発行 metadata の Swift 参照と合成 package
- [Android の Dialog 公開面](../../android/api/dialog-surface.md) — Compose 系 `ksdialogs` と本体 `ksdialogs-core` の使い分け
- [cross/ADR-0008](../../../decisions/cross/0008-distribution-model-standard-channels.md) — 標準 3 チャネル・配信リポジトリ・KMP の Swift 参照の導出
- [cross/ADR-0009](../../../decisions/cross/0009-lockstep-single-version.md) — lockstep 単一 version・版の単一ソースと注入・SNAPSHOT ガード
- [cross/ADR-0019](../../../decisions/cross/0019-android-maven-coordinates-core-suffix.md) — Android の座標名
- [maui/ADR-0004](../../../decisions/maui/0004-nuget-three-package-structure.md) — 3 パッケージ構成・MAUI 本体の下限・最低 OS 版のガード
- [cross/ADR-0024](../../../decisions/cross/0024-release-dispatch-serial-publish-spm-tag-before-kmp.md) — KMP の発行制約が決めた publish の順序
