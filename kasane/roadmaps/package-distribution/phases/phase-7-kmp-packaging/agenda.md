# KMP パッケージング (kmp-packaging)

KsSettingsView に実績のない KMP 形態の配信 (Maven Central へのマルチターゲット publication・dev / publish の Swift 参照切替・Kotlin サポート範囲・公開面の機械検査) と、KMP 消費者検証の形を議論して確定し実装する change フェーズ。本ロードマップで唯一の本格的な議論フェーズ。

## 論点

### publication の形

- Maven Central への KMP マルチターゲット publication (klib + metadata + swiftpm-metadata) を vanniktech plugin で発行する構成。`ksdialogs-kmp` → `ksdialogs` の同版厳密指定 Maven 依存 (cross/ADR-0009) を composite build (`includeBuild("../android")`) と両立させる方法
- dev / publish の Swift 参照切替機構 (library-foundation phase-10 からの申し送り): 開発は `localSwiftPackage(../ios)`、発行時は `swiftPackage(url("https://github.com/kamusoft/KsDialogs-SPM"), exact(x.y.z))`。`localSwiftPackage` のまま発行すると発行者マシンの絶対パスが伝播して消費者ビルドが壊れる (PoC 実証済みの必須要件)。切替は Gradle プロパティか version の SNAPSHOT 判定か
- リモート参照への切替で `samples/kmp/iosApp/KotlinMultiplatformLinkedPackage/` (JetBrains 公式指示どおりコミット) の中身が変わる差分の扱い (phase-10 論点 F の申し送り)
- サポートする Kotlin バージョン範囲の宣言 (SwiftPM import が Alpha, `@ExperimentalKotlinGradlePluginApi` のため。cross/ADR-0008 の見直し条件) と integrateLinkagePackage の利用者手順 (phase-2 の Skill に載せる内容の確定)
- 版の注入: KMP の version も単一ソース (phase-5 の結論) に乗せ、release CI が `-Pversion=` で注入する
- KMP Skill に書く依存宣言 `api("jp.kamusoft:ksdialogs-kmp:<ver>")` が公開座標で解決できる形かの確定 (phase-1 からの申し送り、2026-09-04。Sample は `samples/kmp/settings.gradle.kts` の includeBuild + dependencySubstitution でローカル解決している)

### 公開面の固定

- KMP 公開面の `@Throws` 宣言を回帰から守る検査 (add-kmp-loading-toast-throws からの申し送り、2026-09-02): commonMain 契約は失敗しうる経路 (`KsDialogs.show`・`KsLoading.show` / `start(viewModel)`・`KsToast.show(viewModel)`) だけに `@Throws` を宣言する線引き (concepts core/api/result-notification-semantics.md ルール 5)。宣言が落ちても Kotlin 側のビルド・テストは壊れず、Swift から直接呼んだときだけ abort する退行型で、現状の担保は archive の evidence のみ
- 候補: `androidHostTest` (JVM) のリフレクションで対象関数の throws 節を検査するテスト、または `kmp/api-surface-check` への組み込み。要否から判断する (先例なし。出典: add-kmp-loading-toast-throws review-001 / review-002 Suggestion)

### 消費者検証の形 (実装は phase-8)

- KMP 消費者プロジェクト: Android app (klib を Maven から解決) + iOS app (消費者が自分の framework をビルドし、KGP が生成する linkage package 経由で `KsDialogs-SPM` をリンク)。dry-run では Maven は `mavenLocal()` (`exclusiveContent`)、Swift 参照は配信リポジトリのスナップショットへの `path:` で成立するか
- 検証範囲: KsSettingsView と同じ「解決 + Release ビルド」まで (起動・実機は含めない) で足りるか。KMP は「消費者の framework ビルド + Xcode でのリンク」が通ることが検証の主眼
- release の package / dry-run / smoke 各段の 4 本目 (phase-9 への入力)

## 決定事項

(議論で確定したらここに移動)

前提として確定済みのもの: 配布単位は Maven 1 点 + iOS アプリ側 SwiftPM 1 点 (cross/ADR-0008)、Swift 向け登録 API は Swift パッケージ側 (kmp/ADR-0003・0004)、KMP → Swift は `exact(同版)` (cross/ADR-0009)、Swift 参照先は配信リポジトリ (cross/ADR-0008 (2026-09-04 改訂))。PoC (library-foundation phase-10 `artifacts/poc-swiftpm-remote-distribution.md`) でリモート参照時の Kotlin SwiftPM 連携は全 4 項目成立、スパイクブランチ `spike/phase-10-packaging-poc` は参考実装 (本実装は書き直す)。

## TODO

- [ ] 論点の解消 (publication の形・参照切替・Kotlin 範囲・`@Throws` 検査・消費者検証の形)
- [ ] ksn-propose で変更提案を起こす
