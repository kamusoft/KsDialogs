# セカンドオピニオン: add-kmp-maven-distribution (spec-001)
**相方**: codex / **label**: so-spec-add-kmp-maven-distribution / **日付**: 2026-09-08 / **対象**: kasane/changes/add-kmp-maven-distribution/ の proposal.md / design.md / specs/ / tasks.md (提案一式、実装前)
---
静的レビューのみ実施しました。ビルド・テスト・書き込みは行っていません。

## 指摘事項

### [🟠 Major] SwiftPM メタデータの配置先が KGP 2.4.10 の publication モデルと一致しない

**該当箇所**: `specs/kmp-maven-distribution/spec.md:50`、`specs/kmp-maven-distribution/spec.md:55`、`design.md:103`、`tasks.md:34`

**問題点**: spec/design は、各 iOS ターゲット publication に `swiftpm-metadata.json` と専用 `.module` variant が入るよう記述しています。しかし KGP 2.4.10 は、classifier `swiftpm-metadata` の JSON variant を `multiplatformExtension.publishing.adhocSoftwareComponent`、すなわち root の `kotlinMultiplatform` publication に追加します。iOS ターゲット publication が持つのは主に本体 klib と cinterop klib です。

このままでは、正しい KGP 出力を「iOS publication に metadata がない」と誤って不合格にするか、逆に検査対象を取り違えます。署名 Scenario も root の JSON 成果物を明示していません。

**推奨修正**: 発行物の配置を次のように書き分けてください。

- root `ksdialogs-kmp`: metadata jar、POM、`.module`、`*-swiftpm-metadata.json`
- Android: aar、POM、`.module`、sources/javadoc
- iOS 各ターゲット: 本体 klib、cinterop klib、POM、`.module`、sources/javadoc
- root `.module` の SwiftPM metadata variant が上記 JSON を参照すること
- 鍵ありでは `*-swiftpm-metadata.json.asc` も生成されること

### [🟠 Major] SNAPSHOT ガードの Scenario が、ガード未実装でも成功判定できる

**該当箇所**: `specs/kmp-maven-distribution/spec.md:24`、`tasks.md:8`

**問題点**: 現在の受け入れ条件は「Central 向けタスクが失敗する」だけです。ガードを一切実装していなくても、認証情報不足、ネットワーク制限、Central の拒否などで同じタスクは失敗します。したがって、公開事故を防ぐための `doFirst` ガードが欠落しても Scenario を満たせます。

**推奨修正**: SNAPSHOT 固有の診断文言を Requirement/Scenario に定め、その文言でネットワーク処理より前に失敗したことを確認してください。併せて、対象となる全 `*MavenCentral*` 発行タスクへガードが付いており、`dropMavenCentralDeployment` と `publishToMavenLocal` は対象外であることを検査してください。

### [🟠 Major] Gradle Module Metadata の依存・variant グラフを検証していない

**該当箇所**: `specs/kmp-maven-distribution/spec.md:48`、`specs/kmp-maven-distribution/spec.md:52`、`tasks.md:34`

**問題点**: `.module` は存在だけを確認し、内容を検査していません。KMP 消費者は POM だけでなく Gradle Module Metadata からターゲットを選ぶため、次の欠落があっても phase-7 を完了できます。

- root publication から各ターゲット publication への `available-at` 相当の対応
- SwiftPM metadata 専用 variant と JSON artifact の対応
- Android variant の `ksdialogs-core` API 依存
- iOS 実装が必要とする `kotlinx-coroutines-core`
- cinterop variant と cinterop klib の対応
- テスト専用依存の混入防止

PoC は素の `maven-publish` の出力を実証したもので、vanniktech 0.37.0を加えた今回の実物を保証しません。

**推奨修正**: root・Android・iOS の `.module` について、variant名、属性、参照ファイル、依存座標を期待表として spec に固定し、生成 JSON を機械検査するタスクを追加してください。

### [🟠 Major] iOS 17 の deployment metadata が受け入れ条件から抜けている

**該当箇所**: `design.md:46`、`specs/kmp-maven-distribution/spec.md:29`

**問題点**: design と現行コードは `iosMinimumDeploymentTarget = 17.0` を維持していますが、デルタスペックは SwiftPM 参照種別・URL・exact だけを要求しています。この行が分岐の組み替え時に落ちても全 Scenario が通ります。

KMP metadata の deployment target は配布物側の最低OS保証であり、iOS 17 を対象にした phase-8 の消費者ビルドだけでは欠落を検出できません。

**推奨修正**: `Swift 参照の version 導出` Requirement に `iosMinimumDeploymentTarget = 17.0` を追加し、local・既定remote・URL上書きの各 JSON で serialized deployment version が `17.0` であることを検査してください。

### [🟠 Major] 同一GAVを使う `mavenLocal` 検証が既存・前回成果物に汚染される

**該当箇所**: `design.md:92`、`tasks.md:8`、`tasks.md:14`、`tasks.md:19`、`tasks.md:20`、`tasks.md:21`、`tasks.md:34`

**問題点**: 複数ケースが同じ `0.1.0-beta.1` を `mavenLocal` へ繰り返し発行します。`publishToMavenLocal` は対象ディレクトリを完全初期化しないため、前回の `.asc`、SwiftPM JSON、cinterop klibなどが残り得ます。

その結果、欠けた成果物を古いファイルで存在確認できたり、鍵なしケースが古い `.asc` のため失敗したりします。レビューの再実行でも結果が変わります。

**推奨修正**: 各検証ケースを空の一時 Maven local repositoryへ発行してください。例えばケースごとに異なる `-Dmaven.repo.local=<temp>` を使い、開始時に空であることを確認します。共用する場合は対象GAVを事前に `trash` で除去し、検証後も片付ける手順を明記してください。

### [🟠 Major] `@Throws` の否定側保証と検査対象範囲が確定していない

**該当箇所**: `specs/kmp-api-surface/spec.md:7`、`specs/kmp-api-surface/spec.md:14`、`design.md:70`、`tasks.md:25`

**問題点**: spec は「それ以外の公開関数に `@Throws` が付いても失敗する」としますが、design は `KsDialog`・`KsLoading`・`KsToast` の宣言メソッドだけを列挙します。「公開関数」が3 interface内だけなのか、registry型など commonMain の公開面全体なのかが一致していません。

また、mutation確認は対象4経路から宣言を外す肯定側だけです。非対象関数へ誤って宣言を追加したときに本当に失敗することは検証されません。

**推奨修正**:

- 対象範囲を「3 interface の declared public methods」と限定するか、commonMain公開面全体へ広げるか明記する。
- 非対象メソッド1つへ一時的に `@Throws(DialogException::class)` を付け、関数名を示して失敗する否定側のmutation Scenarioを追加する。
- 一時改変は退避コピーから復元し、復元後のチェックサム一致まで確認する。

### [🟠 Major] vanniktech 設定の配置とルート側 plugin classpath が未設計

**該当箇所**: `proposal.md:11`、`design.md:17`、`tasks.md:12`

**問題点**: proposal は POM共通部とname/descriptionを `kmp/build.gradle.kts` に置く一方、vanniktechを適用するのは `:ksdialogs-kmp` だけとしています。どの設定をルートの `plugins.withId` に置き、どれをmodule側の `mavenPublishing` に置くかが決まっていません。

ルートから `MavenPublishBaseExtension` や `KotlinMultiplatform` を型付き参照する場合、対応プラグインをルートのplugin classpathへ `apply false` で載せる必要があります。特に vanniktech 0.37.0 は KGP型を自身のruntime依存として同梱していないため、配置を誤るとルートスクリプトのコンパイルまたはクラスロードで止まります。

**推奨修正**: ファイル単位で責務を確定してください。例えば次のどちらかです。

- ルート: 必要なplugin aliasを `apply false`、共通POM・署名・Central・version設定。module: plugin適用と `KotlinMultiplatform` 構成。
- module側に発行設定を完結させ、ルートはversion導出だけを持つ。

前者なら、ルートで必要になるKGP/AGP/vanniktechのplugin classpath宣言をtasksへ明記してください。

### [🟡 Minor] `androidVariantsToPublish` の「省略」が0.37.0の実際の既定値と異なる

**該当箇所**: `design.md:17`、`tasks.md:12`

**問題点**: `KotlinMultiplatform(JavadocJar.Empty(), sourcesJar = true)` は、vanniktech 0.37.0では `androidVariantsToPublish = ["release"]` を既定値として使います。引数を書かないことは空リストを渡すことと同じではなく、「省略すれば旧 `KotlinAndroidTarget` のvariant設定へ入らない」という説明は正確ではありません。

新しいAGP KMP targetは旧 `KotlinAndroidTarget` ではないため現状は実害を回避できる可能性が高いものの、設計根拠と実呼び出しが一致していません。

**推奨修正**: 旧target設定を確実に無効化する意図なら `androidVariantsToPublish = emptyList()` を明示し、その状態でもAndroid publicationが1件生成されることを検証してください。

### [🟡 Minor] POMとsources jarの要求を全publicationで判定できない

**該当箇所**: `specs/kmp-maven-distribution/spec.md:50`、`specs/kmp-maven-distribution/spec.md:55`、`tasks.md:34`

**問題点**: Requirementは全POMに共通項目を要求しますが、ScenarioはrootとAndroidだけを確認します。またsources jarは存在だけで、中身が空でも通ります。

**推奨修正**: 5 publicationすべてのPOM項目を検査し、各sources jarについて少なくとも対応source setの代表 `.kt` が含まれることを確認してください。

### [🟡 Minor] Sampleのソース置換確認が観測不能な表現になっている

**該当箇所**: `specs/kmp-maven-distribution/spec.md:80`、`specs/kmp-maven-distribution/spec.md:83`、`tasks.md:30`

**問題点**: 「一時的な変更がビルド出力に反映される」が、何を変更し、どの成果物をどう検査するか決まっていません。実装者ごとに判定方法が変わり、単なる再ビルド成功を置換確認として扱う余地があります。

**推奨修正**: 一意な定数を追加してSampleから参照する、意図的なコンパイル失敗を注入するなど、substitutionが効く場合と効かない場合で結果が分かれる操作を定義してください。退避・復元・チェックサム一致も手順へ含めてください。

### [🟡 Minor] 発行開始後に虚偽になるSampleのコメントが追随対象にない

**該当箇所**: `samples/kmp/settings.gradle.kts:38`、`tasks.md:28`

**問題点**: 現在のコメントは「KMP facade は Maven publication を生成しない」と説明しています。本変更後は直接虚偽になりますが、Sample追随タスクは依存版の変更だけです。

**推奨修正**: コメントを「公開版への無音フォールバックを避け、常にローカルsourceへ置換する」など、発行後も成立する現在形の説明へ更新するタスクを追加してください。

### [🟡 Minor] 最終検証がCIのKMP経路を再現していない

**該当箇所**: `tasks.md:39`

**問題点**: 最終確認は `allTests --rerun-tasks` だけです。現行CIはこれに加えて `compileCommonMainKotlinMetadata` と `compileIosMainKotlinMetadata` を実行しています。過去にtargetコンパイルと`allTests`が通りながら階層化source setのmetadata compileだけが失敗した実績があります。

**推奨修正**: 最終検証をCIと同じ3タスクの同時実行にし、`testAndroidHostTest`・`iosSimulatorArm64Test`の実行件数も記録してください。

### [🟡 Minor] 空白入りversionのScenarioがtasksに落ちている

**該当箇所**: `specs/kmp-maven-distribution/spec.md:19`、`tasks.md:8`

**問題点**: specは空白を含む値の拒否を要求しますが、taskの否定入力は空文字・`v1.0.0`・`1.0.0-pre`だけです。

**推奨修正**: `-Pversion=' 1.0.0 '` または末尾空白付き入力を追加し、許容形式を示す同じ診断で設定失敗することを確認してください。

## 総合判定

**CHANGES_REQUESTED**

Critical 0 / Major 7 / Minor 5 / Suggestion 0。方向性はphase-7の決定と整合していますが、KGPの実際のmetadata配置との不一致と、主要な安全・発行検査が偽陽性になり得る穴が残っているため、このままの実装着手は承認できません。

## 突き合わせ結果 (2026-09-08)

ホスト側の自己レビュー (ksn-propose Step 8 のチェックリスト 2 周) は指摘なしで通過していたため、以下はすべて「相方のみ」の指摘。根拠の強さで採否を決めた。

| # | 指摘 | 採否 | 反映先 |
|---|---|---|---|
| 1 | SwiftPM メタデータは root publication に付く (iOS ターゲットではない) | **採用** — KGP 2.4.10 の class (`SerializeSwiftPMDependenciesMetadataKt` が `KotlinMultiplatformExtension.adhocSoftwareComponent` に classifier `swiftpm-metadata` で登録) でホスト側が裏取り。ホスト側の見逃し | design Decision 1 (配置表)・spec 発行物 / Swift 参照 / 署名・tasks 3.x / 6.1 |
| 2 | SNAPSHOT ガードの Scenario がガード未実装でも通る | **採用** — 実害シナリオ明確 (認証不足やネットワーク不通でも同じタスクが失敗する) | spec version Req (診断文言・`--offline`・drop の除外)・design Decision 2・tasks 1.1 / 1.3 |
| 3 | `.module` の内容を検証していない | **採用** — PoC は素の maven-publish の実測で、vanniktech を加えた実物は保証しない | spec 発行物 Req (`.module` の期待) + Scenario 追加・design Decision 6・tasks 6.2 |
| 4 | `iosMinimumDeploymentTarget=17.0` が受け入れ条件に無い | **採用** — cross/ADR-0009 が配布物の最低 OS 担保の正としている値で、分岐の組み替えで落ちても検出できなかった | spec Swift 参照 Req と 3 Scenario・design Decision 3・tasks 3.1〜3.4 |
| 5 | 同一 GAV の mavenLocal 検証が前回成果物に汚染される | **採用** — 偽の存在確認・偽の署名検出の実害シナリオあり | design Decision 6 (ケースごとに空の `-Dmaven.repo.local`)・spec 全 Scenario・tasks 冒頭 |
| 6 | `@Throws` 否定側の範囲が不定、否定側の mutation 確認が無い | **採用** — spec と design の範囲の不一致は事実 | spec kmp-api-surface (3 interface の宣言メソッドに限定、否定 mutation Scenario 追加)・design Decision 4・tasks 4.2 |
| 7 | vanniktech 設定の配置とルート plugin classpath が未設計 | **採用** — ルートで型付き参照すると plugin classpath 宣言が要る指摘は正確 | design Decision 1 (module 側で完結、ルートは version とタスク名ガードのみ、代替案 C 追加)・tasks 1.1 / 2.1 |
| 8 | `androidVariantsToPublish` の「省略」の説明が既定値と異なる | **降格 (文言のみ修正)** — 設計への影響なし (新 AGP プラグインでは variant 指定が使われない)。誤った根拠の記述は削除 | design Decision 1・tasks 2.1 |
| 9 | POM の共通項目と sources jar の中身を 5 publication で判定していない | **採用** — 安価で受け入れ条件の穴 | spec 発行物 Req / Scenario・tasks 6.1 |
| 10 | Sample の置換確認が観測不能 | **採用** — 再ビルド成功を置換確認と取り違える余地は事実 | spec Sample Scenario (コンパイルエラー注入)・design Decision 5・tasks 5.2 |
| 11 | `samples/kmp/settings.gradle.kts` のコメントが発行後に虚偽になる | **採用** | spec Sample Req・design Decision 5・tasks 5.1 |
| 12 | 最終検証が CI の 3 タスクを再現していない | **採用** | tasks 7.1 |
| 13 | 空白入り version の否定入力が tasks に無い | **採用** | spec Scenario・tasks 1.2 |

採用 12 / 降格 1 / 未解決 0。判定は反映後に自己レビューで再確認済み。
