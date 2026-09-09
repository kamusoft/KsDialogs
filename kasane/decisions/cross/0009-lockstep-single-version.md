---
id: 0009
title: 全形態は lockstep 単一バージョンで一斉リリースし、版間互換を提供しない
status: accepted
date: 2026-08-17
---

## Context

4形態の配布物は単一モノレポから生まれ (cross/ADR-0004)、KMP は static リンクで Swift 実体・Android 実体へ委譲し純 Native 利用者と同一レジストリを共有する (kmp/ADR-0002)。この構造では KMP と Native 実体の版がズレると実行時の契約不整合に直結する。

配布モデル (cross/ADR-0008) でモノレポの semver tag が SwiftPM バージョンを兼ねることが決まり、PoC で「KMP 発行 metadata の `exact("x.y.z")` が消費者の Package.resolved まで機械的に届いて固定される」ことを実測済み。バージョン整合をポリシー (お願いベース) でなく仕組みで強制できる土台がある。

版の値を各ビルドルート (android/ / kmp/) がどこから取るかも決めておく必要がある。Android の各 module がカタログから、KMP がビルドファイルの直書きから取る形では、値の置き場が 2 つになり、リリース CI が版を注入する点も形態ごとに分かれる。

前提: 全形態の配布物を 1 つの release workflow が同時に発行する (cross/ADR-0008)。開発中の版は SNAPSHOT で表し、リリース版の値はリリース時に外から与える。

## Decision

- 全 artifact (SwiftPM tag / Maven `jp.kamusoft:ksdialogs-core`・`jp.kamusoft:ksdialogs`・`jp.kamusoft:ksdialogs-kmp` / MAUI NuGet) を**同一バージョン x.y.z で一斉リリース**し、モノレポの git tag と一致させる
- KMP→Swift パッケージは `swiftPackage(url(...), exact(x.y.z))`、KMP→Android Native は同版の厳密指定 Maven 依存で、版ズレを機械的に排除する
- 版が異なる組み合わせは非サポートとし、互換表 (バージョンマトリクス) は作らない。消費者への案内は「全部同じ番号を入れる」の1行で済ませる

**deployment target の担保**: iOS 17 (cross/ADR-0002) の配布物での担保は「Swift パッケージの `platforms: .iOS(.v17)` 宣言 + KMP metadata の `iosMinimumDeploymentTarget=17.0`」を正とする。`-Xoverride-konan-properties` は klib (IR) には効かず、リポジトリ内で自らリンクする binary (テスト・Sample) 用の内部ビルド詳細であり、配布物の担保手段には数えない。

**版の単一ソースと注入**: 開発用の既定値はバージョンカタログ `android/gradle/libs.versions.toml` の `ksdialogs` (`X.Y.Z-SNAPSHOT`) だけが持つ。Gradle のビルドルート (android/ と kmp/) は同じ導出式 — Gradle プロパティ `version` (`-Pversion=`) の注入値があればそれ、無ければカタログ値 — で自分の `version` と本体への依存版を決め、module ごとの直書きは持たない。注入値は `X.Y.Z` または `X.Y.Z-{alpha|beta|rc}.N` に限り、空文字・空白を含む値・形式外の値はビルド設定で失敗させる。version が `-SNAPSHOT` のあいだは Maven Central へ向く発行タスクを失敗させ、ローカル発行は妨げない。リリース時は dispatch 入力 = `-Pversion=` の注入値 = tag = 各レジストリの version が同一文字列で流れる。

## Alternatives Considered

- **形態ごとの独立バージョン + 互換 range**: 却下。static リンク + レジストリ共有の前提で版ズレ事故の余地を残し、互換マトリクスの維持コストが常時かかる。形態単位でリリースできる柔軟性は tag 1本のモノレポでは活きない
- **KMP↔Swift のみ exact、他は range の折衷**: 却下。ルールが2種類混在して複雑さだけ増える
- **カタログの値を CI が書き換えてリリース版にする**: 却下。作業木を汚し、tag との一致が書き換え工程の正しさに依存する
- **version 専用ファイルの新設**: 却下。カタログが既に同じ役を持ち冗長になる
- **各 module に version を直書きする (改訂前の形)**: 却下。group のような全体事項を module ごとに繰り返し、kmp/ の直書きとずれる余地が残る
- **注入値の形式を検査しない**: 却下。空文字が「注入あり」と解釈されて SNAPSHOT ガードを迂回し、開発版が発行版の顔で流れ得る。release workflow の入力検査とは独立に、Gradle を直接実行した場合も守る

## Consequences

- 正: 消費者の理解が「同じ番号を揃える」だけになり、互換表・組み合わせテストが不要になる
- 正: `exact` + 厳密指定により、版ズレはビルド時に機械的に検出される (実行時事故にならない)
- 正: モノレポ tag 1本がそのまま全形態のリリース単位になり、cross/ADR-0008 と整合する
- 正: リリース CI の注入点が「Gradle ビルドに `-Pversion=` を渡す」の 1 種類で済み、KMP → 本体の厳密同版が同じ式から導出されて機械的に揃う
- 正: Sample の composite build は座標で置換するため、カタログ値が SNAPSHOT になっても影響を受けない
- 負: 1形態だけの修正でも全形態のバージョンが上がる (変更のない形態も再発行する)。リリース CI は常に全形態一斉が前提になる (発行実装・運用整備への入力)
- 負: 「変更がないのに版が上がる」ことへの説明をリリースノート運用で受け持つ
- 負: 導出式は別ビルドルートである android/ と kmp/ に同じものを 2 度書くことになり (cross/ADR-0004 はルートに共通ビルドファイルを置かない)、ずれは発行物の版の比較で検出するほかない

## Revisit When

- 前提 (Context) が崩れたとき。特に形態ごとに別々の release workflow で発行するようになったとき、または Gradle 以外のビルドルートが版の値を必要とするようになったとき

---
出典: kasane/roadmaps/archive/2026-09-04-library-foundation/phases/phase-10-packaging-model/history.md (2026-08-17: 論点B) / kasane/roadmaps/package-distribution/phases/phase-5-native-packaging/agenda.md (決定事項: バージョンの単一ソースとリリース version の注入) / kasane/changes/archive/2026-09-08-add-native-distribution/design.md (Decision 3) / kasane/roadmaps/package-distribution/phases/phase-7-kmp-packaging/agenda.md (決定事項 A5) / kasane/changes/archive/2026-09-09-add-kmp-maven-distribution/design.md (Decision 2)
現行照合: 2026-09-09 確認。`android/build.gradle.kts` と `kmp/build.gradle.kts` が同じ導出式 (`-Pversion=` 優先・形式検査・カタログ既定値) と SNAPSHOT ガード (名前に `MavenCentral` を含むタスクを設定段階の要求タスク名照合と task graph 確定時の 2 段で失敗させ、`dropMavenCentralDeployment` と `publishToMavenLocal` は通す) を持つ。kmp の android publication の POM の `ksdialogs-core` 依存版は同じ `-Pversion=` で発行した android artifact の version と同一文字列 (add-kmp-maven-distribution evidence/android-version-alignment.txt)。集約タスク `publish` 経由だけは Gradle の認証情報検査が同じ時点で走り、SNAPSHOT 診断と認証情報未解決が同時に報告される (診断は必ず出て実行前に止まる)。deployment target の伝搬 (Swift パッケージの宣言と KMP metadata の 17.0) は PoC で消費者側まで確認済み。判定: 維持
関連: cross/ADR-0008 (配布モデル。SwiftPM tag と KMP の Swift 参照を version から導出する切り替え) / cross/ADR-0019 (Android の Maven 座標名)
