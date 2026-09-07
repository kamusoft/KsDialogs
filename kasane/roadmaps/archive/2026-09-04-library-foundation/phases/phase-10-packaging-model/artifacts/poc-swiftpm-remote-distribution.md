# PoC: Kotlin SwiftPM 連携の配布時成立性 (論点C)

2026-08-16 実施。スパイクブランチ `spike/phase-10-packaging-poc` + scratchpad の消費者プロジェクトで実測した。
判定: **成立** (検証項目4つすべて確認)。

## 検証構成

- **発行側**: スパイクブランチ上で android/:ksdialogs と kmp/:ksdialogs-kmp に `maven-publish` を配線し mavenLocal へ発行。ルート Package.swift (ソースは `path:` で ios/ 参照) を試作し、モノレポの bare clone + tag `0.1.0` を file:// の擬似リモートとした
- **kmp の Swift 参照**: `localSwiftPackage(../ios)` → `swiftPackage(url(擬似リモート), exact("0.1.0"), listOf(product("KsDialogs")))` へ切り替えて発行
- **消費者側**: リポジトリ外に別の KMP プロジェクト (composite build なし・mavenLocal + 標準リポジトリのみ) と iOS アプリ (Sample iosApp 雛形) を構築

## 検証結果

| # | 検証項目 | 結果 |
|---|---|---|
| 1 | リポジトリ外の消費者が Maven 依存1点で解決できるか | ✅ `jp.kamusoft:ksdialogs-kmp:0.1.0` 1点で klib + cinterop klib (`-cinterop-swiftPMImport.klib`) + Android Native (推移的依存) が揃い、commonMain の契約でコンパイル・framework リンク成功 |
| 2 | Swift パッケージ参照のリモート成立性と消費者側の追加設定 | ✅ 発行 metadata (`swiftpm-metadata.json` + Gradle Module Metadata の専用バリアント) に `SwiftPMDependency.Remote` (URL + Exact 0.1.0) が乗り、**消費者の build.gradle.kts への swiftPMDependencies 再宣言は不要**。消費者ビルドに `integrateLinkagePackage` タスクが推移的に生え、合成パッケージがリモート URL + exact 0.1.0 を参照して再生成された |
| 3 | 消費者 iOS アプリでの通し (レジストリ共有) | ✅ アプリは (a) ConsumerShared framework、(b) 合成パッケージ `KotlinMultiplatformLinkedPackage`、(c) 登録 API 用の KsDialogs リモート SwiftPM 参照の3点をリンクし、Basic Dialog の表示 → OK タップ → `完了: true` の型付き結果還流まで動作 ([スクリーンショット](poc-result-completed-true.png))。消費者定義の ViewModel クラスがレジストリキーとして機能 |
| 4 | 生成物のローカルパス残留 / Swift 実体の重複 | ✅ アプリの Package.resolved は identity `ksdialogs-remote` の **1 pin のみ** (直接参照と合成パッケージ経由が同一 identity にデデュープ、checkout も1つ = レジストリ1系統)。消費者側生成物に発行者マシンの絶対パスは残らない |

## 判明した重要事実

1. **`localSwiftPackage` のまま publish すると壊れる**: 発行 metadata に `SwiftPMDependency.Local` + 発行者マシンの絶対パスがそのまま乗る (実測 + KGP v2.4.10 ソース裏取り)。消費者ビルドはそのパスの Package.swift を読みに行き失敗する。未文書化の落とし穴で、publish 時の警告診断もない。**発行時はリモート `swiftPackage(url(...), ...)` 参照が必須**
2. **バージョン固定は KMP 側 metadata が持つ**: `exact("0.1.0")` / `from(...)` がそのまま消費者へ伝わり、Package.resolved に pin される。モノレポ tag = SwiftPM バージョン (cross/0008) と合わせると、KMP artifact x.y.z → Swift パッケージ tag x.y.z の lockstep を `exact` で機械的に強制できる (論点Bへの入力)
3. **消費者は macOS + Xcode 必須で Swift パッケージが再ビルドされる**: リンク時のマシンコードは klib に入っておらず、消費者ビルドで checkout・ビルドが再実行される
4. **消費者アプリの初回セットアップに `integrateLinkagePackage` 実行が必要** (`XCODEPROJ_PATH` 指定)。以後は自動更新。アプリがリンクするのは合成パッケージであり、個別 Swift パッケージの手動リンクは登録 API 用の1点のみ
5. **機能の安定度は Alpha** (`@ExperimentalKotlinGradlePluginApi`)。static framework 前提なら既知の不安定要素 (dynamic での症状) を回避できる。現構成は static (kmp/ADR-0002) で整合
6. **開発ループとの両立が必要**: リモート参照へ恒久的に切り替えると `../ios` のライブ編集が利かなくなる。ローカル開発は `localSwiftPackage`、発行時のみリモートに切り替えるスイッチ (Gradle プロパティ等) が phase-11 の実装課題

## 検証の限界

- 擬似リモートは file:// の bare リポジトリ。https の実リモート (GitHub) 固有の挙動 (認証・シャロークローン等) は未検証だが、SwiftPM の解決経路としては同一
- mavenLocal 発行のため、Maven Central 固有の要件 (署名・pom 検証) は phase-11 の範囲
- MAUI NuGet (論点D) はこの PoC の範囲外

出典: kasane/roadmaps/library-foundation/phases/phase-10-packaging-model/history.md (2026-08-16: 論点C PoC)、スパイクブランチ `spike/phase-10-packaging-poc`
