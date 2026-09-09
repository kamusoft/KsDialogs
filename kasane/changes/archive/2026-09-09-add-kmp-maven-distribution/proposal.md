# Proposal: add-kmp-maven-distribution

## Why

KMP 形態の配布経路は決定済み (cross/ADR-0008: Maven 1 点 `jp.kamusoft:ksdialogs-kmp` + iOS アプリ側の SwiftPM 1 点、Swift 参照は配信リポジトリ `KsDialogs-SPM` を `exact(同版)` で指す) だが、`kmp/` には発行の配線が無く、version は直書き `0.1.0`、Swift 参照は開発用の `localSwiftPackage(../ios)` 固定である。`localSwiftPackage` のまま発行すると発行者マシンの絶対パスが metadata に乗って消費者ビルドが壊れる (PoC 実証済み、警告も出ない) ため、発行には dev / publish の切り替えが必須になる。姉妹ライブラリ KsSettingsView に KMP 形態は無く、この部分は本ロードマップで唯一の本格的な設計対象としてフェーズ議論 (phase-7) で確定した。その結論を実装し、消費者検証 (phase-8) と release workflow (phase-9) が乗る土台を作る。

あわせて、KMP 公開面の `@Throws` 宣言 (Swift 境界で失敗を `throws` として届ける契約) が落ちても Kotlin 側のビルド・テストは緑のままで、利用者の Swift コードが abort して初めて分かる退行型の穴を、公開 API を固めるこの段階で塞ぐ。

## What Changes

- **発行設定**: `:ksdialogs-kmp` に `com.vanniktech.maven.publish` 0.37.0 を適用 (`KotlinMultiplatform` 構成、AGP 9 の KMP ライブラリプラグイン向けに `androidVariantsToPublish` は省略)。`publishToMavenCentral()` + `signAllPublications()`、空 javadoc jar、署名の必須/任意は鍵の有無に連動、SNAPSHOT は Central へ発行しないガード。POM の共通部 (url / MIT license / developers / scm / inceptionYear) と name / description を `kmp/build.gradle.kts` (新設) に置く。`:api-surface-check` は発行しない。`includeBuild("../android")` は維持する (composite build の置換は解決時にだけ効き、POM には宣言どおりの `jp.kamusoft:ksdialogs-core:<版>` が載る)
- **version の単一ソース**: phase-5 の導出式 (`-Pversion=` の注入値があればそれ、無ければカタログ `ksdialogs` の `0.1.0-SNAPSHOT`。注入値の形式検査つき) を `kmp/build.gradle.kts` にも書き、`:ksdialogs-kmp` の version と `jp.kamusoft:ksdialogs-core` への依存版の共通の入力にする。直書き `"0.1.0"` を廃止する
- **Swift 参照の version 導出**: version が `-SNAPSHOT` なら `localSwiftPackage(../ios)`、それ以外なら `swiftPackage(url, exact(<version>))` を宣言する。URL は Gradle プロパティ 1 つで上書きでき (既定 `https://github.com/kamusoft/KsDialogs-SPM`)、exact は上書きしない。専用のモード切替スイッチは持たない
- **`@Throws` の回帰検査**: `androidHostTest` に反射テストを 1 本追加し、失敗しうる 4 経路 (`KsDialog.show(viewModel)`・`KsLoading.show(viewModel)`・`KsLoading.start(viewModel)`・`KsToast.show(viewModel)`) の throws 節に `DialogException` があること、それ以外の公開関数に throws 節が無いことを検査する
- **Sample の追随**: `samples/kmp/shared` の依存版 `ksdialogs-kmp:0.1.0` の直書きをカタログ値に揃え (置換されるため挙動は変わらない)、`samples/kmp/settings.gradle.kts` の「KMP facade は Maven publication を生成しない」という置換の説明を発行後も成立する内容に改める
- **発行検証**: `-Pversion=` なし / あり、URL 上書きなし / あり (`file://` のローカル clone) で、ケースごとに空の一時 Maven local repository へ `publishToMavenLocal` し、発行物の配置 (root の `swiftpm-metadata.json`・各ターゲットの klib / cinterop klib / aar・sources jar・空 javadoc jar・5 つの POM) と `.module` の内容 (ターゲット委譲 variant・SwiftPM 連携 variant・依存)・SwiftPM 連携メタデータの参照種別と URL・exact・deployment target `17.0` を検算して証跡を evidence/ に残す。android/ と kmp/ を同じ `-Pversion=` で発行し、kmp POM の依存版が android の artifact version と同一文字列であることを確認する
- **規範の追随**: `kasane/handbook/cross/test-execution.md` の kmp テスト件数の記録を更新する

影響する能力: KMP の配布 (Maven 座標と発行経路・Swift 参照の metadata)、KMP ビルド入口 (version と Swift 参照の導出)、KMP 公開面の Swift 境界契約 (`@Throws`)、Sample のソース参照

## Non-Goals

- Maven Central への実発行・secrets 登録・release workflow への組み込み — phase-9 の責務。phase-9 への申し送り (package 段の成果物・Central deployment 2 件・dry-run 段での再発行・publish 順序・smoke の反映待ち) は phase-7 agenda 決定事項 C3 が正
- KMP 消費者プロジェクト (`verification/kmp/`) と dry-run / smoke の消費者検証 — phase-8 の責務。dry-run が使う `file://` URL 上書きと Maven の `mavenLocal()` 割り当ての形は agenda 決定事項 C1・C2 が正で、本変更はその前提となる URL 上書きプロパティを提供する。公開座標での実解決 (Maven Central + https) の確認も phase-8 (smoke)
- KMP Skill (`skills/*/ksdialogs-kmp/`) と README の追従 (依存スコープを `api` に正す・Kotlin サポート範囲 (同 minor 2.4.x、確認済み版はカタログ値) の宣言・「予定している公開 coordinate」の状態表記) — docs-refresh の明示依頼で更新する (CLAUDE.md の運用宣言)。concepts (kmp/api/ios-host-integration.md の発行時参照の記述) の追随は蒸留時の定型作業
- version 導出式の android/ との共有化 (`apply(from=)` や build-logic) — フェーズ議論で却下済み (agenda 決定事項 A5)
- Gradle プロパティによる local / remote の明示切替、dry-run で配信リポジトリへ一時 tag を push する運用 — フェーズ議論で却下済み (agenda 決定事項 A2、cross/ADR-0008 に追記済み)
- `ObjCApiSurfaceTests` や `api-surface-check` への `@Throws` 検査の組み込み — フェーズ議論で却下済み (agenda 決定事項 B)
- SNAPSHOT を `publishToMavenLocal` した成果物の local パス問題の解消 — 既知の限界として受け入れる (SNAPSHOT の消費はリポジトリ内 Sample の composite build が担う)。文書での明記は docs-refresh

## Impact

- 破壊的変更: なし (未リリース)。開発ループは version が SNAPSHOT のあいだ `localSwiftPackage` のままで、`../ios` のライブ編集も `samples/kmp/iosApp/KotlinMultiplatformLinkedPackage/` の中身も変わらない
- 前提: phase-5 の change (add-native-distribution) の実装 — カタログの `0.1.0-SNAPSHOT` 化・`android/build.gradle.kts` の導出式・`ksdialogs-core` への座標リネームと `kmp/` の依存座標追随 — が先に完了していること。本変更はその上に積む
- リスク: ① vanniktech 経由で KGP の SwiftPM 連携メタデータ (`swiftpm-metadata.json` と `.module` の専用バリアント) が発行物に載ることは公式に明記がない — `publishToMavenLocal` の検算を受け入れ条件にし、欠ければ素の `maven-publish` へ戻す判断を仰ぐ。② vanniktech 0.37.0 の検証済み範囲は Kotlin 2.4.0 / AGP 9.2.1 までで、手元の 2.4.10 / 9.3.0 は少し超える (phase-5 と同じ)。③ `swiftPackage(url(...))` に `file://` URL を渡せることは PoC (KGP 2.4.10) で実測済みだが、公式記述は無い — 発行検証で再確認する
- 外部リソース: なし (実発行も配信リポジトリへの push もしない)

## 級: L

公開レジストリへの発行経路と Swift 参照の切替という覆しにくい決定の実装で、kmp ビルド入口・Sample・検証手順を横断し、phase-8 / 9 が前提にする機構 (URL 上書き・version 導出) を提供するため L。設計判断はフェーズ議論 (agenda 決定事項 A1〜A6・B) と ADR (cross/0008 (2026-09-08 追記) / 0009) で確定済みで、design.md は決定事項の Decision 形式への転記 + 実装方式の確定 (導出の配線・プロパティ名・検査の形) に絞る。

domain: kmp
roadmap: package-distribution/phase-7-kmp-packaging
